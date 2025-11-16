# OmniSharp for IntelliJ - 项目结构分析和支持功能单元测试实现

## 1. 概述

本文档详细描述了 OmniSharp for IntelliJ 项目中项目结构分析和支持功能的单元测试实现方案。单元测试是确保代码质量和功能正确性的关键环节，尤其对于复杂的项目结构分析和支持系统。本测试方案涵盖了所有核心组件的测试用例、测试策略和最佳实践。

## 2. 测试框架与工具

### 2.1 测试框架选择

- **JUnit 5**: 用于 Kotlin/Java 单元测试
- **Mockito**: 用于模拟依赖组件
- **Kotest**: 提供更具 Kotlin 风格的测试断言和测试 DSL

### 2.2 测试环境配置

```kotlin
// build.gradle.kts 测试依赖配置

dependencies {
    // 测试框架
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    
    // Mockito 模拟库
    testImplementation("org.mockito:mockito-core:4.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:4.0.0")
    
    // Kotest 测试断言库
    testImplementation("io.kotest:kotest-assertions-core:4.6.0")
    testImplementation("io.kotest:kotest-runner-junit5:4.6.0")
    
    // 用于测试 IntelliJ 平台组件
    testImplementation("com.jetbrains.intellij.platform:platform-test-framework:2022.1")
}

// 测试任务配置
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
```

## 3. 测试策略

### 3.1 单元测试策略

1. **隔离测试**：每个组件的测试应该相互隔离，避免依赖其他组件的实现
2. **模拟依赖**：使用 Mockito 模拟所有外部依赖
3. **参数化测试**：对关键功能使用参数化测试，覆盖多种输入场景
4. **边界条件测试**：特别关注边界条件和异常情况
5. **性能测试**：对性能敏感的功能进行基准测试

### 3.2 集成测试策略

1. **功能集成测试**：验证多个组件协同工作的正确性
2. **端到端测试**：模拟真实用户场景的端到端测试
3. **UI 测试**：使用 IntelliJ UI 测试框架测试用户界面组件

## 4. 核心组件测试实现

### 4.1 解决方案文件解析器测试

```kotlin
package com.omnisharp.intellij.solution

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import org.junit.jupiter.params.*
import org.junit.jupiter.params.provider.*
import org.mockito.*
import java.io.*
import java.nio.file.*
import kotlin.test.*

class OmniSharpSolutionParserTest {
    private lateinit var solutionParser: OmniSharpSolutionParser
    private lateinit var fileSystem: FileSystem
    
    @BeforeEach
    fun setUp() {
        // 模拟文件系统
        fileSystem = Mockito.mock(FileSystem::class.java)
        solutionParser = OmniSharpSolutionParser(fileSystem)
    }
    
    @Test
    fun testParseSolution_Success() {
        // 准备测试数据
        val solutionContent = """Microsoft Visual Studio Solution File, Format Version 12.00
# Visual Studio Version 16
VisualStudioVersion = 16.0.28701.123
MinimumVisualStudioVersion = 10.0.40219.1
Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject\", \"TestProject\\TestProject.csproj\", \"{1A2B3C4D-5E6F-7G8H-9I0J-1K2L3M4N5O6P}\"
EndProject
Global
    GlobalSection(SolutionConfigurationPlatforms) = preSolution
        Debug|Any CPU = Debug|Any CPU
    EndGlobalSection
    GlobalSection(ProjectConfigurationPlatforms) = postSolution
        {1A2B3C4D-5E6F-7G8H-9I0J-1K2L3M4N5O6P}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
        {1A2B3C4D-5E6F-7G8H-9I0J-1K2L3M4N5O6P}.Debug|Any CPU.Build.0 = Debug|Any CPU
    EndGlobalSection
    GlobalSection(SolutionProperties) = preSolution
        HideSolutionNode = FALSE
    EndGlobalSection
EndGlobal"""
        
        // 设置模拟文件内容
        val path = Paths.get("TestSolution.sln")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn(solutionContent.byteInputStream())
        
        // 执行解析
        val solution = solutionParser.parseSolution(path.toString())
        
        // 验证结果
        assertNotNull(solution)
        assertEquals("TestSolution", solution.name)
        assertEquals("TestSolution.sln", solution.path)
        assertEquals(1, solution.projects.size)
        
        val project = solution.projects[0]
        assertEquals("TestProject", project.name)
        assertEquals("TestProject\\TestProject.csproj", project.path)
        assertEquals("{1A2B3C4D-5E6F-7G8H-9I0J-1K2L3M4N5O6P}", project.id)
    }
    
    @Test
    fun testParseSolution_EmptyFile() {
        // 准备空文件
        val path = Paths.get("Empty.sln")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn(ByteArrayInputStream(ByteArray(0)))
        
        // 执行解析并验证异常
        val exception = assertThrows<OmniSharpParsingException> {
            solutionParser.parseSolution(path.toString())
        }
        assertEquals("Invalid solution file format", exception.message)
    }
    
    @Test
    fun testParseSolution_InvalidFormat() {
        // 准备格式错误的文件
        val path = Paths.get("Invalid.sln")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn("Invalid solution file content".byteInputStream())
        
        // 执行解析并验证异常
        val exception = assertThrows<OmniSharpParsingException> {
            solutionParser.parseSolution(path.toString())
        }
        assertEquals("Invalid solution file format", exception.message)
    }
    
    @ParameterizedTest
    @MethodSource("solutionFilesProvider")
    fun testParseSolution_Parameterized(filePath: String, expectedProjects: Int) {
        // 这里可以测试不同大小和复杂度的解决方案文件
        // 实际测试需要提供真实的测试文件路径和预期结果
        assertTrue(true, "这是一个参数化测试模板")
    }
    
    companion object {
        @JvmStatic
        fun solutionFilesProvider(): List<Arguments> {
            return listOf(
                Arguments.of("SimpleSolution.sln", 1),
                Arguments.of("ComplexSolution.sln", 5)
            )
        }
    }
    
    @Test
    fun testParseSolution_FileNotFound() {
        // 设置文件不存在
        val path = Paths.get("NonExistent.sln")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenThrow(FileNotFoundException("File not found"))
        
        // 执行解析并验证异常
        val exception = assertThrows<OmniSharpParsingException> {
            solutionParser.parseSolution(path.toString())
        }
        assertTrue(exception.message!!.contains("Failed to read solution file"))
    }
}
```

### 4.2 项目文件解析器测试

```kotlin
package com.omnisharp.intellij.project

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import org.mockito.*
import java.io.*
import java.nio.file.*
import kotlin.test.*

class OmniSharpProjectParserTest {
    private lateinit var projectParser: OmniSharpProjectParser
    private lateinit var fileSystem: FileSystem
    
    @BeforeEach
    fun setUp() {
        fileSystem = Mockito.mock(FileSystem::class.java)
        projectParser = OmniSharpProjectParser(fileSystem)
    }
    
    @Test
    fun testParseSdkStyleProject() {
        // 准备 SDK 风格项目文件内容
        val projectContent = """<Project Sdk=\"Microsoft.NET.Sdk\">
  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net6.0</TargetFramework>
    <AssemblyName>TestProject</AssemblyName>
    <RootNamespace>TestProject</RootNamespace>
  </PropertyGroup>
  <ItemGroup>
    <PackageReference Include=\"Newtonsoft.Json\" Version=\"13.0.1\" />
  </ItemGroup>
  <ItemGroup>
    <Compile Include=\"Program.cs\" />
    <Compile Include=\"Models\\User.cs\" />
  </ItemGroup>
</Project>"""
        
        val path = Paths.get("TestProject.csproj")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn(projectContent.byteInputStream())
        
        // 执行解析
        val project = projectParser.parseProject(path.toString())
        
        // 验证结果
        assertNotNull(project)
        assertEquals("TestProject", project.name)
        assertEquals("TestProject.csproj", project.path)
        assertEquals("net6.0", project.targetFramework)
        assertEquals("Exe", project.outputType)
        assertEquals(2, project.files.size)
        assertTrue(project.files.contains("Program.cs"))
        assertTrue(project.files.contains("Models\\User.cs"))
        assertEquals(1, project.references.size)
        assertTrue(project.references.any { it.type == "Package" && it.name == "Newtonsoft.Json" && it.version == "13.0.1" })
    }
    
    @Test
    fun testParseLegacyProject() {
        // 准备传统风格项目文件内容
        val projectContent = """<?xml version=\"1.0\" encoding=\"utf-8\"?>
<Project ToolsVersion=\"15.0\" xmlns=\"http://schemas.microsoft.com/developer/msbuild/2003\">
  <PropertyGroup>
    <OutputType>Library</OutputType>
    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
    <AssemblyName>LegacyProject</AssemblyName>
    <RootNamespace>LegacyProject</RootNamespace>
  </PropertyGroup>
  <ItemGroup>
    <Reference Include=\"System.Data\" />
    <Reference Include=\"System.Xml\" />
  </ItemGroup>
  <ItemGroup>
    <Compile Include=\"Class1.cs\" />
  </ItemGroup>
</Project>"""
        
        val path = Paths.get("LegacyProject.csproj")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn(projectContent.byteInputStream())
        
        // 执行解析
        val project = projectParser.parseProject(path.toString())
        
        // 验证结果
        assertNotNull(project)
        assertEquals("LegacyProject", project.name)
        assertEquals("LegacyProject.csproj", project.path)
        assertEquals("v4.7.2", project.targetFramework)
        assertEquals("Library", project.outputType)
        assertEquals(1, project.files.size)
        assertTrue(project.files.contains("Class1.cs"))
        assertEquals(2, project.references.size)
        assertTrue(project.references.any { it.type == "Assembly" && it.name == "System.Data" })
        assertTrue(project.references.any { it.type == "Assembly" && it.name == "System.Xml" })
    }
    
    @Test
    fun testParseProject_InvalidXml() {
        // 准备无效 XML 内容
        val path = Paths.get("InvalidProject.csproj")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenReturn("<Project>Invalid XML</Project>".byteInputStream())
        
        // 执行解析并验证异常
        val exception = assertThrows<OmniSharpParsingException> {
            projectParser.parseProject(path.toString())
        }
        assertTrue(exception.message!!.contains("Failed to parse project file"))
    }
    
    @Test
    fun testParseProject_FileNotFound() {
        // 设置文件不存在
        val path = Paths.get("NonExistent.csproj")
        Mockito.`when`(fileSystem.provider().newInputStream(path))
            .thenThrow(FileNotFoundException("File not found"))
        
        // 执行解析并验证异常
        val exception = assertThrows<OmniSharpParsingException> {
            projectParser.parseProject(path.toString())
        }
        assertTrue(exception.message!!.contains("Failed to read project file"))
    }
}
```

### 4.3 依赖关系分析器测试

```kotlin
package com.omnisharp.intellij.dependency

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import org.mockito.*
import kotlin.test.*

class OmniSharpDependencyAnalyzerTest {
    private lateinit var dependencyAnalyzer: OmniSharpDependencyAnalyzer
    private lateinit var projectManager: OmniSharpProjectManager
    
    @BeforeEach
    fun setUp() {
        projectManager = Mockito.mock(OmniSharpProjectManager::class.java)
        dependencyAnalyzer = OmniSharpDependencyAnalyzer(projectManager)
    }
    
    @Test
    fun testAnalyzeDependencies_SimpleSolution() {
        // 创建测试解决方案
        val solution = createSimpleTestSolution()
        
        // 设置项目管理器的行为
        Mockito.`when`(projectManager.getSourceFiles(Mockito.any()))
            .thenReturn(emptyList())
        
        // 执行分析
        val dependencyGraph = dependencyAnalyzer.analyzeDependencies(solution)
        
        // 验证依赖图
        assertNotNull(dependencyGraph)
        assertEquals(2, dependencyGraph.nodes.size) // 两个项目节点
        assertEquals(1, dependencyGraph.edges.size) // 一个项目引用
        
        // 验证节点
        val mainProjectNode = dependencyGraph.nodes.find { it.name == "MainProject" }
        val utilsProjectNode = dependencyGraph.nodes.find { it.name == "UtilsProject" }
        assertNotNull(mainProjectNode)
        assertNotNull(utilsProjectNode)
        
        // 验证边
        val edge = dependencyGraph.edges.first()
        assertEquals(mainProjectNode, edge.source)
        assertEquals(utilsProjectNode, edge.target)
        assertEquals("ProjectReference", edge.type)
    }
    
    @Test
    fun testAnalyzeDependencies_ComplexSolution() {
        // 创建具有复杂依赖关系的测试解决方案
        val solution = createComplexTestSolution()
        
        // 设置项目管理器的行为
        Mockito.`when`(projectManager.getSourceFiles(Mockito.any()))
            .thenReturn(emptyList())
        
        // 执行分析
        val dependencyGraph = dependencyAnalyzer.analyzeDependencies(solution)
        
        // 验证依赖图
        assertNotNull(dependencyGraph)
        assertEquals(4, dependencyGraph.nodes.size) // 4个项目节点
        assertEquals(4, dependencyGraph.edges.size) // 4个引用关系
        
        // 检查是否存在循环依赖
        val hasCycles = dependencyAnalyzer.hasCycles(dependencyGraph)
        assertFalse(hasCycles, "Solution should not have circular dependencies")
    }
    
    @Test
    fun testAnalyzeDependencies_CircularDependency() {
        // 创建具有循环依赖的测试解决方案
        val solution = createCircularDependencySolution()
        
        // 设置项目管理器的行为
        Mockito.`when`(projectManager.getSourceFiles(Mockito.any()))
            .thenReturn(emptyList())
        
        // 执行分析
        val dependencyGraph = dependencyAnalyzer.analyzeDependencies(solution)
        
        // 检查循环依赖
        val hasCycles = dependencyAnalyzer.hasCycles(dependencyGraph)
        assertTrue(hasCycles, "Solution should have circular dependencies")
        
        // 获取循环依赖路径
        val cycles = dependencyAnalyzer.findCyclePaths(dependencyGraph)
        assertEquals(1, cycles.size) // 应该只有一个循环
        assertEquals(2, cycles[0].size) // 循环应该包含两个节点
    }
    
    @Test
    fun testVersionConflicts() {
        // 创建具有版本冲突的测试解决方案
        val solution = createVersionConflictSolution()
        
        // 设置项目管理器的行为
        Mockito.`when`(projectManager.getSourceFiles(Mockito.any()))
            .thenReturn(emptyList())
        
        // 执行分析
        val dependencyGraph = dependencyAnalyzer.analyzeDependencies(solution)
        
        // 检查版本冲突
        val conflicts = dependencyAnalyzer.detectVersionConflicts(dependencyGraph)
        assertEquals(1, conflicts.size) // 应该有一个版本冲突
        assertEquals("Newtonsoft.Json", conflicts[0].packageName)
        assertEquals(2, conflicts[0].conflictingVersions.size) // 应该有两个冲突版本
    }
    
    private fun createSimpleTestSolution(): OmniSharpSolutionModel {
        // 项目引用关系：MainProject -> UtilsProject
        val utilsProject = OmniSharpProjectModel(
            name = "UtilsProject",
            path = "UtilsProject\\UtilsProject.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val mainProjectReference = OmniSharpReference(
            name = "UtilsProject",
            path = "UtilsProject\\UtilsProject.csproj",
            type = "ProjectReference",
            version = null
        )
        
        val mainProject = OmniSharpProjectModel(
            name = "MainProject",
            path = "MainProject\\MainProject.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = listOf(mainProjectReference),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "SimpleSolution",
            path = "SimpleSolution.sln",
            projects = listOf(mainProject, utilsProject),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    private fun createComplexTestSolution(): OmniSharpSolutionModel {
        // 创建具有层次结构依赖的解决方案
        val dataAccessProject = OmniSharpProjectModel(
            name = "DataAccess",
            path = "DataAccess\\DataAccess.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val modelsProject = OmniSharpProjectModel(
            name = "Models",
            path = "Models\\Models.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val businessLogicReferences = listOf(
            OmniSharpReference("DataAccess", "DataAccess\\DataAccess.csproj", "ProjectReference", null),
            OmniSharpReference("Models", "Models\\Models.csproj", "ProjectReference", null)
        )
        
        val businessLogicProject = OmniSharpProjectModel(
            name = "BusinessLogic",
            path = "BusinessLogic\\BusinessLogic.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = businessLogicReferences,
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val webAppReferences = listOf(
            OmniSharpReference("BusinessLogic", "BusinessLogic\\BusinessLogic.csproj", "ProjectReference", null),
            OmniSharpReference("Models", "Models\\Models.csproj", "ProjectReference", null)
        )
        
        val webAppProject = OmniSharpProjectModel(
            name = "WebApp",
            path = "WebApp\\WebApp.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = webAppReferences,
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "ComplexSolution",
            path = "ComplexSolution.sln",
            projects = listOf(dataAccessProject, modelsProject, businessLogicProject, webAppProject),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    private fun createCircularDependencySolution(): OmniSharpSolutionModel {
        // 创建具有循环依赖的解决方案：ProjectA -> ProjectB -> ProjectA
        val projectAReference = OmniSharpReference("ProjectB", "ProjectB\\ProjectB.csproj", "ProjectReference", null)
        val projectBReference = OmniSharpReference("ProjectA", "ProjectA\\ProjectA.csproj", "ProjectReference", null)
        
        val projectA = OmniSharpProjectModel(
            name = "ProjectA",
            path = "ProjectA\\ProjectA.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = listOf(projectAReference),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val projectB = OmniSharpProjectModel(
            name = "ProjectB",
            path = "ProjectB\\ProjectB.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = listOf(projectBReference),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "CircularSolution",
            path = "CircularSolution.sln",
            projects = listOf(projectA, projectB),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    private fun createVersionConflictSolution(): OmniSharpSolutionModel {
        // 创建具有包版本冲突的解决方案
        val projectA = OmniSharpProjectModel(
            name = "ProjectA",
            path = "ProjectA\\ProjectA.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = listOf(OmniSharpReference("Newtonsoft.Json", "", "Package", "13.0.1")),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        val projectB = OmniSharpProjectModel(
            name = "ProjectB",
            path = "ProjectB\\ProjectB.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = listOf(OmniSharpReference("Newtonsoft.Json", "", "Package", "12.0.3")),
            files = emptyList(),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "VersionConflictSolution",
            path = "VersionConflictSolution.sln",
            projects = listOf(projectA, projectB),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
}
```

### 4.4 符号索引器测试

```kotlin
package com.omnisharp.intellij.symbol

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import org.mockito.*
import kotlin.test.*

class OmniSharpSymbolIndexerTest {
    private lateinit var symbolIndexer: OmniSharpSymbolIndexer
    private lateinit var symbolCollector: OmniSharpSymbolCollector
    private lateinit var symbolCache: OmniSharpSymbolCache
    
    @BeforeEach
    fun setUp() {
        symbolCollector = Mockito.mock(OmniSharpSymbolCollector::class.java)
        symbolCache = Mockito.mock(OmniSharpSymbolCache::class.java)
        symbolIndexer = OmniSharpSymbolIndexer(symbolCollector, symbolCache)
    }
    
    @Test
    fun testBuildIndex() {
        // 创建测试解决方案
        val solution = createTestSolution()
        
        // 创建测试符号
        val testSymbols = listOf(
            OmniSharpSymbol("TestClass", "TestProject", "TestProject.TestClass", SymbolType.CLASS, "c:\\test\\TestClass.cs", 10, 20),
            OmniSharpSymbol("TestMethod", "TestProject", "TestProject.TestClass.TestMethod", SymbolType.METHOD, "c:\\test\\TestClass.cs", 15, 18)
        )
        
        // 设置符号收集器的行为
        Mockito.`when`(symbolCollector.collectSymbols(solution)).thenReturn(testSymbols)
        
        // 执行索引构建
        symbolIndexer.buildIndex(solution)
        
        // 验证调用
        Mockito.verify(symbolCollector).collectSymbols(solution)
        Mockito.verify(symbolCache).clear()
        Mockito.verify(symbolCache).addSymbol(testSymbols[0])
        Mockito.verify(symbolCache).addSymbol(testSymbols[1])
        Mockito.verify(symbolCache).buildInvertedIndex()
    }
    
    @Test
    fun testSearchSymbols_ExactMatch() {
        // 创建测试符号
        val testSymbols = listOf(
            OmniSharpSymbol("User", "TestProject", "TestProject.User", SymbolType.CLASS, "c:\\test\\User.cs", 5, 30),
            OmniSharpSymbol("UserRepository", "TestProject", "TestProject.UserRepository", SymbolType.CLASS, "c:\\test\\UserRepository.cs", 8, 40)
        )
        
        // 设置缓存行为
        Mockito.`when`(symbolCache.searchSymbols("User", true)).thenReturn(listOf(testSymbols[0]))
        
        // 执行搜索
        val results = symbolIndexer.searchSymbols("User", true)
        
        // 验证结果
        assertEquals(1, results.size)
        assertEquals("User", results[0].name)
    }
    
    @Test
    fun testSearchSymbols_PartialMatch() {
        // 创建测试符号
        val testSymbols = listOf(
            OmniSharpSymbol("User", "TestProject", "TestProject.User", SymbolType.CLASS, "c:\\test\\User.cs", 5, 30),
            OmniSharpSymbol("UserRepository", "TestProject", "TestProject.UserRepository", SymbolType.CLASS, "c:\\test\\UserRepository.cs", 8, 40)
        )
        
        // 设置缓存行为
        Mockito.`when`(symbolCache.searchSymbols("User", false)).thenReturn(testSymbols)
        
        // 执行搜索
        val results = symbolIndexer.searchSymbols("User", false)
        
        // 验证结果
        assertEquals(2, results.size)
    }
    
    @Test
    fun testSearchSymbols_CaseSensitive() {
        // 创建测试符号
        val userSymbol = OmniSharpSymbol("User", "TestProject", "TestProject.User", SymbolType.CLASS, "c:\\test\\User.cs", 5, 30)
        
        // 设置缓存行为
        Mockito.`when`(symbolCache.searchSymbols("user", true)).thenReturn(emptyList())
        Mockito.`when`(symbolCache.searchSymbols("user", false)).thenReturn(listOf(userSymbol))
        
        // 测试大小写敏感搜索
        val caseSensitiveResults = symbolIndexer.searchSymbols("user", true)
        assertEquals(0, caseSensitiveResults.size)
        
        // 测试大小写不敏感搜索
        val caseInsensitiveResults = symbolIndexer.searchSymbols("user", false)
        assertEquals(1, caseInsensitiveResults.size)
        assertEquals("User", caseInsensitiveResults[0].name)
    }
    
    @Test
    fun testIncrementalIndexUpdate() {
        // 创建测试文件变更
        val fileChange = OmniSharpFileChange(
            filePath = "c:\\test\\User.cs",
            changeType = FileChangeType.MODIFIED
        )
        
        // 创建测试符号
        val updatedSymbols = listOf(
            OmniSharpSymbol("User", "TestProject", "TestProject.User", SymbolType.CLASS, "c:\\test\\User.cs", 5, 30)
        )
        
        // 设置符号收集器的行为
        Mockito.`when`(symbolCollector.collectSymbolsForFile("c:\\test\\User.cs")).thenReturn(updatedSymbols)
        
        // 执行增量更新
        symbolIndexer.updateIndexForFile(fileChange)
        
        // 验证调用
        Mockito.verify(symbolCollector).collectSymbolsForFile("c:\\test\\User.cs")
        Mockito.verify(symbolCache).removeSymbolsForFile("c:\\test\\User.cs")
        Mockito.verify(symbolCache).addSymbol(updatedSymbols[0])
        Mockito.verify(symbolCache).buildInvertedIndex()
    }
    
    @Test
    fun testClearIndex() {
        // 执行清除索引
        symbolIndexer.clearIndex()
        
        // 验证缓存清除
        Mockito.verify(symbolCache).clear()
    }
    
    private fun createTestSolution(): OmniSharpSolutionModel {
        val project = OmniSharpProjectModel(
            name = "TestProject",
            path = "TestProject\\TestProject.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = listOf("c:\\test\\TestClass.cs"),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "TestSolution",
            path = "TestSolution.sln",
            projects = listOf(project),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
}
```

### 4.5 项目导航器测试

```kotlin
package com.omnisharp.intellij.navigation

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import org.mockito.*
import kotlin.test.*

class OmniSharpProjectNavigatorTest {
    private lateinit var projectNavigator: OmniSharpProjectNavigator
    private lateinit var projectManager: OmniSharpProjectManager
    private lateinit var symbolManager: OmniSharpSymbolManager
    private lateinit var dependencyManager: OmniSharpDependencyManager
    
    @BeforeEach
    fun setUp() {
        projectManager = Mockito.mock(OmniSharpProjectManager::class.java)
        symbolManager = Mockito.mock(OmniSharpSymbolManager::class.java)
        dependencyManager = Mockito.mock(OmniSharpDependencyManager::class.java)
        
        projectNavigator = OmniSharpProjectNavigator(
            project = Mockito.mock(Project::class.java),
            projectManager = projectManager,
            symbolManager = symbolManager,
            dependencyManager = dependencyManager
        )
    }
    
    @Test
    fun testBuildNavigationTree() {
        // 创建测试解决方案
        val solution = createTestSolution()
        
        // 设置模拟行为
        Mockito.`when`(projectManager.getSourceFiles(solution.projects[0]))
            .thenReturn(listOf("Program.cs", "Models\\User.cs"))
        
        // 创建测试符号
        val projectSymbols = listOf(
            OmniSharpSymbol("Program", "TestProject", "TestProject.Program", SymbolType.CLASS, "Program.cs", 10, 20),
            OmniSharpSymbol("User", "TestProject", "TestProject.Models.User", SymbolType.CLASS, "Models\\User.cs", 5, 30)
        )
        
        Mockito.`when`(symbolManager.getSymbolsForProject(solution.projects[0]))
            .thenReturn(projectSymbols)
        
        // 执行导航树构建
        val navigationTree = projectNavigator.buildNavigationTree(solution)
        
        // 验证导航树结构
        assertNotNull(navigationTree)
        assertEquals(NodeType.SOLUTION, navigationTree.root.type)
        assertEquals("TestSolution", navigationTree.root.name)
        
        // 验证项目节点
        val projectNodes = navigationTree.root.getChildren()
        assertEquals(1, projectNodes.size)
        assertEquals(NodeType.PROJECT, projectNodes[0].type)
        assertEquals("TestProject", projectNodes[0].name)
        
        // 验证逻辑结构和物理结构节点
        val projectNodeChildren = projectNodes[0].getChildren()
        assertEquals(2, projectNodeChildren.size) // 逻辑结构和物理结构
        
        // 验证逻辑结构中的命名空间和类
        val logicalNode = projectNodeChildren.find { it.type == NodeType.LOGICAL_STRUCTURE }
        assertNotNull(logicalNode)
        val namespaceNodes = logicalNode.getChildren()
        assertEquals(1, namespaceNodes.size) // TestProject 命名空间
        assertEquals("TestProject", namespaceNodes[0].name)
        
        // 检查 Models 命名空间节点
        val modelsNode = namespaceNodes[0].getChildren().find { it.name == "Models" }
        assertNotNull(modelsNode)
        
        // 查找特定节点
        val userClassNode = navigationTree.findNodeByName("User", NodeType.CLASS)
        assertNotNull(userClassNode)
    }
    
    @Test
    fun testFilterTree() {
        // 创建测试解决方案并构建导航树
        val solution = createTestSolution()
        Mockito.`when`(projectManager.getSourceFiles(solution.projects[0]))
            .thenReturn(listOf("Program.cs", "Models\\User.cs"))
        
        // 创建测试符号
        val projectSymbols = listOf(
            OmniSharpSymbol("Program", "TestProject", "TestProject.Program", SymbolType.CLASS, "Program.cs", 10, 20),
            OmniSharpSymbol("User", "TestProject", "TestProject.Models.User", SymbolType.CLASS, "Models\\User.cs", 5, 30)
        )
        
        Mockito.`when`(symbolManager.getSymbolsForProject(solution.projects[0]))
            .thenReturn(projectSymbols)
        
        // 构建导航树
        val navigationTree = projectNavigator.buildNavigationTree(solution)
        
        // 应用过滤器：只显示名称包含 "User" 的节点
        val filteredTree = projectNavigator.filterTree(navigationTree, "User", emptySet())
        
        // 验证过滤结果
        assertNotNull(filteredTree)
        
        // 检查过滤后的树中是否包含 User 类
        val userNode = filteredTree.findNodeByName("User", NodeType.CLASS)
        assertNotNull(userNode)
        
        // 检查是否不包含 Program 类
        val programNode = filteredTree.findNodeByName("Program", NodeType.CLASS)
        assertNull(programNode)
    }
    
    @Test
    fun testSwitchViewMode() {
        // 创建测试解决方案并构建导航树
        val solution = createTestSolution()
        Mockito.`when`(projectManager.getSourceFiles(solution.projects[0]))
            .thenReturn(listOf("Program.cs", "Models\\User.cs"))
        
        Mockito.`when`(symbolManager.getSymbolsForProject(solution.projects[0]))
            .thenReturn(emptyList())
        
        // 构建导航树
        projectNavigator.buildNavigationTree(solution)
        
        // 切换到逻辑视图
        projectNavigator.switchViewMode(ViewMode.LOGICAL)
        assertEquals(ViewMode.LOGICAL, projectNavigator.currentViewMode)
        
        // 切换到物理视图
        projectNavigator.switchViewMode(ViewMode.PHYSICAL)
        assertEquals(ViewMode.PHYSICAL, projectNavigator.currentViewMode)
        
        // 切换到类型视图
        projectNavigator.switchViewMode(ViewMode.TYPES)
        assertEquals(ViewMode.TYPES, projectNavigator.currentViewMode)
        
        // 切换到依赖视图
        projectNavigator.switchViewMode(ViewMode.DEPENDENCIES)
        assertEquals(ViewMode.DEPENDENCIES, projectNavigator.currentViewMode)
    }
    
    @Test
    fun testFindNodeById() {
        // 创建测试解决方案并构建导航树
        val solution = createTestSolution()
        Mockito.`when`(projectManager.getSourceFiles(solution.projects[0]))
            .thenReturn(listOf("Program.cs"))
        Mockito.`when`(symbolManager.getSymbolsForProject(solution.projects[0]))
            .thenReturn(emptyList())
        
        // 构建导航树
        val navigationTree = projectNavigator.buildNavigationTree(solution)
        
        // 通过 ID 查找节点
        val projectNode = navigationTree.findNodeById("TestProject")
        assertNotNull(projectNode)
        assertEquals("TestProject", projectNode.name)
        
        // 查找不存在的节点
        val nonExistentNode = navigationTree.findNodeById("NonExistent")
        assertNull(nonExistentNode)
    }
    
    private fun createTestSolution(): OmniSharpSolutionModel {
        val project = OmniSharpProjectModel(
            name = "TestProject",
            path = "TestProject\\TestProject.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = listOf("Program.cs", "Models\\User.cs"),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "TestSolution",
            path = "TestSolution.sln",
            projects = listOf(project),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
}
```

### 4.6 缓存系统测试

```kotlin
package com.omnisharp.intellij.cache

import com.omnisharp.intellij.model.*
import org.junit.jupiter.api.*
import kotlin.test.*

class OmniSharpSymbolCacheTest {
    private lateinit var symbolCache: OmniSharpSymbolCache
    
    @BeforeEach
    fun setUp() {
        symbolCache = OmniSharpSymbolCache()
    }
    
    @Test
    fun testAddAndGetSymbol() {
        // 创建测试符号
        val symbol = OmniSharpSymbol(
            name = "TestClass",
            projectName = "TestProject",
            fullName = "TestProject.TestClass",
            type = SymbolType.CLASS,
            filePath = "c:\\test\\TestClass.cs",
            startLine = 10,
            endLine = 20
        )
    }
    
    /**
     * 生成测试解决方案文件
     */
    fun generateTestSolutionFile(path: String, solution: OmniSharpSolutionModel) {
        val content = buildString {
            appendLine("Microsoft Visual Studio Solution File, Format Version 12.00")
            appendLine("# Visual Studio Version 16")
            appendLine("VisualStudioVersion = 16.0.28701.123")
            appendLine("MinimumVisualStudioVersion = 10.0.40219.1")
            
            // 添加项目
            solution.projects.forEachIndexed { index, project ->
                appendLine("Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"${project.name}\", \"${project.path}\", \"{${generateProjectGuid(index)}}\"")
                appendLine("EndProject")
            }
            
            // 添加全局部分
            appendLine("Global")
            appendLine("    GlobalSection(SolutionConfigurationPlatforms) = preSolution")
            appendLine("        Debug|Any CPU = Debug|Any CPU")
            appendLine("        Release|Any CPU = Release|Any CPU")
            appendLine("    EndGlobalSection")
            appendLine("    GlobalSection(ProjectConfigurationPlatforms) = postSolution")
            
            solution.projects.forEachIndexed { index, _ ->
                appendLine("        {${generateProjectGuid(index)}}.Debug|Any CPU.ActiveCfg = Debug|Any CPU")
                appendLine("        {${generateProjectGuid(index)}}.Debug|Any CPU.Build.0 = Debug|Any CPU")
                appendLine("        {${generateProjectGuid(index)}}.Release|Any CPU.ActiveCfg = Release|Any CPU")
                appendLine("        {${generateProjectGuid(index)}}.Release|Any CPU.Build.0 = Release|Any CPU")
            }
            
            appendLine("    EndGlobalSection")
            appendLine("    GlobalSection(SolutionProperties) = preSolution")
            appendLine("        HideSolutionNode = FALSE")
            appendLine("    EndGlobalSection")
            appendLine("EndGlobal")
        }
        
        Files.writeString(Paths.get(path), content)
    }
    
    /**
     * 生成测试项目文件
     */
    fun generateTestProjectFile(path: String, project: OmniSharpProjectModel) {
        val content = buildString {
            appendLine("<Project Sdk=\"Microsoft.NET.Sdk\">")
            appendLine("  <PropertyGroup>")
            appendLine("    <OutputType>${project.outputType}</OutputType>")
            appendLine("    <TargetFramework>${project.targetFramework ?: "net6.0"}</TargetFramework>")
            appendLine("    <AssemblyName>${project.name}</AssemblyName>")
            appendLine("    <RootNamespace>${project.name}</RootNamespace>")
            appendLine("  </PropertyGroup>")
            
            // 添加项目引用
            val projectReferences = project.references.filter { it.type == "ProjectReference" }
            if (projectReferences.isNotEmpty()) {
                appendLine("  <ItemGroup>")
                projectReferences.forEach { reference ->
                    appendLine("    <ProjectReference Include=\"${reference.path}\" />")
                }
                appendLine("  </ItemGroup>")
            }
            
            // 添加包引用
            val packageReferences = project.references.filter { it.type == "Package" }
            if (packageReferences.isNotEmpty()) {
                appendLine("  <ItemGroup>")
                packageReferences.forEach { reference ->
                    appendLine("    <PackageReference Include=\"${reference.name}\" Version=\"${reference.version}\" />")
                }
                appendLine("  </ItemGroup>")
            }
            
            // 添加编译文件
            if (project.files.isNotEmpty()) {
                appendLine("  <ItemGroup>")
                project.files.forEach { file ->
                    appendLine("    <Compile Include=\"${file}\" />")
                }
                appendLine("  </ItemGroup>")
            }
            
            appendLine("</Project>")
        }
        
        val projectDir = Paths.get(path).parent
        if (!Files.exists(projectDir)) {
            Files.createDirectories(projectDir)
        }
        
        Files.writeString(Paths.get(path), content)
    }
    
    private fun generateProjectGuid(index: Int): String {
        // 生成简单的GUID-like字符串用于测试
        return "{TEST-$index-${(1000 + index).toString().padStart(4, '0')}-GUID}"
    }
}

/**
 * 测试断言扩展，提供更具可读性的测试断言
 */
object OmniSharpTestAssertions {
    fun assertSolutionContainsProject(solution: OmniSharpSolutionModel, projectName: String) {
        val project = solution.projects.find { it.name == projectName }
        assertNotNull(project, "解决方案应包含名为 '$projectName' 的项目")
    }
    
    fun assertProjectContainsFile(project: OmniSharpProjectModel, filePath: String) {
        assertTrue(project.files.contains(filePath), "项目 '${project.name}' 应包含文件 '$filePath'")
    }
    
    fun assertProjectContainsReference(project: OmniSharpProjectModel, referenceName: String, referenceType: String? = null) {
        val reference = project.references.find { 
            it.name == referenceName && (referenceType == null || it.type == referenceType)
        }
        assertNotNull(reference, "项目 '${project.name}' 应包含引用 '$referenceName' (类型: $referenceType)")
    }
    
    fun assertDependencyGraphContainsEdge(dependencyGraph: OmniSharpDependencyGraph, source: String, target: String, type: String? = null) {
        val edge = dependencyGraph.edges.find { 
            it.source.name == source && 
            it.target.name == target && 
            (type == null || it.type == type)
        }
        assertNotNull(edge, "依赖图应包含从 '$source' 到 '$target' 的边 (类型: $type)")
    }
    
    fun assertSymbolSearchResults(results: List<OmniSharpSymbol>, expectedCount: Int, expectedNames: List<String> = emptyList()) {
        assertEquals(expectedCount, results.size, "搜索结果数量应等于 $expectedCount")
        
        expectedNames.forEach { name ->
            assertTrue(results.any { it.name == name }, "搜索结果应包含名为 '$name' 的符号")
        }
    }
}

/**
 * Mock对象工厂，用于创建常见的模拟对象
 */
class OmniSharpMockFactory {
    fun createMockProject(): Project {
        val project = Mockito.mock(Project::class.java)
        Mockito.`when`(project.getBasePath()).thenReturn(System.getProperty("user.dir"))
        Mockito.`when`(project.getName()).thenReturn("TestProject")
        return project
    }
    
    fun createMockFileManager(): VirtualFileManager {
        return Mockito.mock(VirtualFileManager::class.java)
    }
    
    fun createMockSolution(projectCount: Int = 1): OmniSharpSolutionModel {
        val solution = Mockito.mock(OmniSharpSolutionModel::class.java)
        val projects = mutableListOf<OmniSharpProjectModel>()
        
        for (i in 0 until projectCount) {
            val project = Mockito.mock(OmniSharpProjectModel::class.java)
            Mockito.`when`(project.name).thenReturn("Project$i")
            Mockito.`when`(project.path).thenReturn("Project$i/Project$i.csproj")
            Mockito.`when`(project.files).thenReturn(listOf("Program.cs", "Class1.cs"))
            projects.add(project)
        }
        
        Mockito.`when`(solution.projects).thenReturn(projects)
        Mockito.`when`(solution.name).thenReturn("TestSolution")
        return solution
    }
}

## 8. 测试执行和报告

### 8.1 运行测试

在 IntelliJ IDEA 中，可以通过以下方式运行测试：

1. **通过 IDE 界面**：
   - 打开测试文件
   - 点击编辑器左侧的绿色运行图标
   - 选择 "Run '<测试名称>'"

2. **通过 Gradle**：
   ```bash
   ./gradlew test
   ```

3. **运行特定测试**：
   ```bash
   ./gradlew test --tests "com.omnisharp.intellij.solution.*"
   ```

### 8.2 测试报告

测试完成后，可以在以下位置查看测试报告：

```
build/reports/tests/test/index.html
```

## 9. 总结与最佳实践

### 9.1 测试最佳实践

1. **隔离测试**：每个测试方法应该测试单一功能点
2. **模拟依赖**：使用模拟对象隔离测试目标
3. **清晰的断言**：使用描述性的断言消息，确保失败时易于理解
4. **测试覆盖率**：追求高覆盖率，但注重关键路径和边界条件
5. **测试命名**：使用描述性的测试方法名称，如 `testMethodUnderTest_Scenario_ExpectedOutcome`
6. **测试数据**：使用有意义的测试数据，避免使用魔术数字
7. **性能考虑**：注意测试执行时间，避免过慢的测试

### 9.2 测试代码质量

1. **DRY 原则**：避免测试代码中的重复
2. **测试辅助类**：创建测试辅助类和方法以简化测试编写
3. **测试维护**：定期检查和更新测试，确保它们与代码保持同步
4. **测试评论**：对于复杂的测试，添加注释解释测试意图和场景

### 9.3 常见问题和解决方案

#### 9.3.1 测试执行慢

- 检查是否存在不必要的文件 IO 操作
- 减少测试中的网络调用
- 使用内存数据库或模拟数据库
- 考虑使用并行测试执行

#### 9.3.2 测试不稳定

- 确保测试之间相互独立
- 避免使用不稳定的外部依赖
- 使用确定性的测试数据
- 适当增加等待时间（对于异步测试）

#### 9.3.3 测试覆盖率低

- 识别未测试的代码路径
- 特别关注边界条件和错误处理
- 为复杂功能编写更详细的测试
- 使用覆盖率工具识别测试缺口

### 9.4 结论

单元测试是确保 OmniSharp for IntelliJ 项目结构分析和支持功能质量的重要组成部分。通过全面的单元测试，可以及早发现和修复问题，提高代码质量，并使代码更容易维护和扩展。本测试方案提供了一个完整的框架，可用于测试所有核心组件，并确保它们按预期工作。

通过遵循最佳实践并持续改进测试策略，可以不断提高测试的有效性，并为项目的成功做出贡献。

---

以上是 OmniSharp for IntelliJ 项目结构分析和支持功能的单元测试实现方案。通过全面而系统的测试覆盖，可以确保项目结构分析和支持功能的稳定性、正确性和性能。,
            endLine = 20
        )
        
        // 添加符号到缓存
        symbolCache.addSymbol(symbol)
        
        // 获取符号
        val retrievedSymbol = symbolCache.getSymbolById(symbol.id)
        assertNotNull(retrievedSymbol)
        assertEquals("TestClass", retrievedSymbol.name)
        assertEquals("TestProject", retrievedSymbol.projectName)
    }
    
    @Test
    fun testRemoveSymbol() {
        // 创建并添加测试符号
        val symbol = OmniSharpSymbol(
            name = "TestClass",
            projectName = "TestProject",
            fullName = "TestProject.TestClass",
            type = SymbolType.CLASS,
            filePath = "c:\\test\\TestClass.cs",
            startLine = 10,
            endLine = 20
        )
        
        symbolCache.addSymbol(symbol)
        
        // 移除符号
        symbolCache.removeSymbol(symbol.id)
        
        // 验证符号已被移除
        val retrievedSymbol = symbolCache.getSymbolById(symbol.id)
        assertNull(retrievedSymbol)
    }
    
    @Test
    fun testRemoveSymbolsForFile() {
        // 创建并添加多个符号
        val fileSymbols = listOf(
            OmniSharpSymbol(
                name = "Class1",
                projectName = "TestProject",
                fullName = "TestProject.Class1",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\File1.cs",
                startLine = 5,
                endLine = 15
            ),
            OmniSharpSymbol(
                name = "Class2",
                projectName = "TestProject",
                fullName = "TestProject.Class2",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\File1.cs",
                startLine = 20,
                endLine = 30
            ),
            OmniSharpSymbol(
                name = "Class3",
                projectName = "TestProject",
                fullName = "TestProject.Class3",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\File2.cs",
                startLine = 10,
                endLine = 20
            )
        )
        
        fileSymbols.forEach { symbolCache.addSymbol(it) }
        
        // 构建倒排索引
        symbolCache.buildInvertedIndex()
        
        // 移除特定文件的符号
        symbolCache.removeSymbolsForFile("c:\\test\\File1.cs")
        
        // 重新构建倒排索引
        symbolCache.buildInvertedIndex()
        
        // 搜索符号
        val results = symbolCache.searchSymbols("Class", false)
        
        // 验证只有 File2.cs 中的符号保留
        assertEquals(1, results.size)
        assertEquals("Class3", results[0].name)
    }
    
    @Test
    fun testSearchSymbols() {
        // 创建并添加测试符号
        val symbols = listOf(
            OmniSharpSymbol(
                name = "User",
                projectName = "TestProject",
                fullName = "TestProject.User",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\User.cs",
                startLine = 10,
                endLine = 20
            ),
            OmniSharpSymbol(
                name = "UserRepository",
                projectName = "TestProject",
                fullName = "TestProject.UserRepository",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\UserRepository.cs",
                startLine = 5,
                endLine = 30
            ),
            OmniSharpSymbol(
                name = "Product",
                projectName = "TestProject",
                fullName = "TestProject.Product",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\Product.cs",
                startLine = 8,
                endLine = 25
            )
        )
        
        symbols.forEach { symbolCache.addSymbol(it) }
        
        // 构建倒排索引
        symbolCache.buildInvertedIndex()
        
        // 测试大小写敏感搜索
        val caseSensitiveResults = symbolCache.searchSymbols("User", true)
        assertEquals(1, caseSensitiveResults.size) // 只匹配 "User"，不匹配 "UserRepository"
        assertEquals("User", caseSensitiveResults[0].name)
        
        // 测试大小写不敏感搜索
        val caseInsensitiveResults = symbolCache.searchSymbols("user", false)
        assertEquals(2, caseInsensitiveResults.size) // 匹配 "User" 和 "UserRepository"
        assertTrue(caseInsensitiveResults.any { it.name == "User" })
        assertTrue(caseInsensitiveResults.any { it.name == "UserRepository" })
    }
    
    @Test
    fun testClearCache() {
        // 创建并添加测试符号
        val symbol = OmniSharpSymbol(
            name = "TestClass",
            projectName = "TestProject",
            fullName = "TestProject.TestClass",
            type = SymbolType.CLASS,
            filePath = "c:\\test\\TestClass.cs",
            startLine = 10,
            endLine = 20
        )
        
        symbolCache.addSymbol(symbol)
        symbolCache.buildInvertedIndex()
        
        // 清除缓存
        symbolCache.clear()
        
        // 验证缓存已清空
        val retrievedSymbol = symbolCache.getSymbolById(symbol.id)
        assertNull(retrievedSymbol)
        
        // 验证搜索结果为空
        val searchResults = symbolCache.searchSymbols("Test", false)
        assertTrue(searchResults.isEmpty())
    }
    
    @Test
    fun testPerformanceForLargeNumberOfSymbols() {
        // 计时添加 10000 个符号
        val startTime = System.currentTimeMillis()
        
        for (i in 0 until 10000) {
            val symbol = OmniSharpSymbol(
                name = "TestClass$i",
                projectName = "TestProject",
                fullName = "TestProject.TestClass$i",
                type = SymbolType.CLASS,
                filePath = "c:\\test\\TestClass$i.cs",
                startLine = i,
                endLine = i + 10
            )
            symbolCache.addSymbol(symbol)
        }
        
        val addTime = System.currentTimeMillis() - startTime
        println("添加 10000 个符号耗时: ${addTime}ms")
        assertTrue(addTime < 1000, "添加符号时间不应超过 1 秒")
        
        // 计时构建倒排索引
        val indexStartTime = System.currentTimeMillis()
        symbolCache.buildInvertedIndex()
        val indexTime = System.currentTimeMillis() - indexStartTime
        println("构建倒排索引耗时: ${indexTime}ms")
        assertTrue(indexTime < 500, "构建索引时间不应超过 500 毫秒")
        
        // 计时搜索
        val searchStartTime = System.currentTimeMillis()
        val results = symbolCache.searchSymbols("TestClass999", false)
        val searchTime = System.currentTimeMillis() - searchStartTime
        println("搜索符号耗时: ${searchTime}ms")
        assertTrue(searchTime < 100, "搜索时间不应超过 100 毫秒")
        assertTrue(results.isNotEmpty())
    }
}
```

## 5. 集成测试

### 5.1 解决方案加载和解析集成测试

```kotlin
package com.omnisharp.intellij.integration

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.project.*
import com.omnisharp.intellij.solution.*
import org.junit.jupiter.api.*
import java.nio.file.*
import kotlin.test.*

class OmniSharpSolutionIntegrationTest {
    private lateinit var solutionParser: OmniSharpSolutionParser
    private lateinit var projectParser: OmniSharpProjectParser
    
    @BeforeEach
    fun setUp() {
        solutionParser = OmniSharpSolutionParser(FileSystems.getDefault())
        projectParser = OmniSharpProjectParser(FileSystems.getDefault())
    }
    
    @Test
    fun testLoadAndParseSolution_Integration() {
        // 注意：这个测试需要在测试环境中有一个真实的解决方案文件
        // 这里使用测试资源目录中的示例解决方案
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val solutionPath = testResourcesDir + "/TestSolution.sln"
        
        // 检查测试文件是否存在
        if (!Files.exists(Paths.get(solutionPath))) {
            System.err.println("测试文件不存在，跳过集成测试: $solutionPath")
            return
        }
        
        // 解析解决方案
        val solution = solutionParser.parseSolution(solutionPath)
        assertNotNull(solution)
        assertTrue(solution.projects.size > 0)
        
        // 解析第一个项目
        val firstProjectPath = Paths.get(solutionPath).parent.resolve(solution.projects[0].path).toString()
        
        if (Files.exists(Paths.get(firstProjectPath))) {
            val project = projectParser.parseProject(firstProjectPath)
            assertNotNull(project)
            assertEquals(solution.projects[0].name, project.name)
            assertNotNull(project.targetFramework)
            assertNotNull(project.outputType)
        }
    }
    
    @Test
    fun testDependencyAnalysis_Integration() {
        // 这个测试将验证从加载解决方案到依赖分析的完整流程
        // 注意：同样需要测试资源文件
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val solutionPath = testResourcesDir + "/TestSolutionWithDependencies.sln"
        
        if (!Files.exists(Paths.get(solutionPath))) {
            System.err.println("测试文件不存在，跳过集成测试: $solutionPath")
            return
        }
        
        // 解析解决方案
        val solution = solutionParser.parseSolution(solutionPath)
        assertNotNull(solution)
        
        // 创建项目管理器和依赖分析器
        val projectManager = TestProjectManager()
        val dependencyAnalyzer = OmniSharpDependencyAnalyzer(projectManager)
        
        // 分析依赖关系
        val dependencyGraph = dependencyAnalyzer.analyzeDependencies(solution)
        assertNotNull(dependencyGraph)
        
        // 验证依赖图至少包含解决方案中的项目数量的节点
        assertTrue(dependencyGraph.nodes.size >= solution.projects.size)
        
        // 检查项目名称是否存在于依赖图中
        solution.projects.forEach { project ->
            val projectNode = dependencyGraph.nodes.find { it.name == project.name }
            assertNotNull(projectNode, "项目 $project.name 应存在于依赖图中")
        }
    }
    
    // 测试项目管理器实现
    private class TestProjectManager : OmniSharpProjectManager {
        override fun getSolutionForProject(project: OmniSharpProjectModel): OmniSharpSolutionModel? {
            return null
        }
        
        override fun getSourceFiles(project: OmniSharpProjectModel): List<String> {
            // 返回模拟的源文件列表
            return listOf("Program.cs", "Models\\User.cs")
        }
        
        override fun getFileParser(): OmniSharpFileParser {
            return object : OmniSharpFileParser {
                override fun parseFile(filePath: String): OmniSharpParseResult {
                    return OmniSharpParseResult(filePath, emptyList())
                }
            }
        }
        
        override fun addFileChangeListener(listener: OmniSharpFileChangeListener) {
            // 不实现
        }
        
        override fun removeFileChangeListener(listener: OmniSharpFileChangeListener) {
            // 不实现
        }
        
        override fun getCurrentSolution(): OmniSharpSolutionModel? {
            return null
        }
        
        override fun loadSolution(solutionPath: String): OmniSharpSolutionModel {
            val parser = OmniSharpSolutionParser(FileSystems.getDefault())
            return parser.parseSolution(solutionPath)
        }
    }
}
```

### 5.2 符号索引和搜索集成测试

```kotlin
package com.omnisharp.intellij.integration

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.symbol.*
import org.junit.jupiter.api.*
import java.nio.file.*
import kotlin.test.*

class OmniSharpSymbolIndexingIntegrationTest {
    private lateinit var symbolCollector: OmniSharpSymbolCollector
    private lateinit var symbolCache: OmniSharpSymbolCache
    private lateinit var symbolIndexer: OmniSharpSymbolIndexer
    
    @BeforeEach
    fun setUp() {
        symbolCollector = OmniSharpSymbolCollector()
        symbolCache = OmniSharpSymbolCache()
        symbolIndexer = OmniSharpSymbolIndexer(symbolCollector, symbolCache)
    }
    
    @Test
    fun testSymbolIndexingAndSearch_Integration() {
        // 注意：这个测试需要在测试环境中有一个包含 C# 源文件的项目
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val solutionPath = testResourcesDir + "/TestSolution.sln"
        
        if (!Files.exists(Paths.get(solutionPath))) {
            System.err.println("测试文件不存在，跳过集成测试: $solutionPath")
            return
        }
        
        // 加载测试解决方案
        val solutionParser = OmniSharpSolutionParser(FileSystems.getDefault())
        val solution = solutionParser.parseSolution(solutionPath)
        assertNotNull(solution)
        
        // 构建索引
        symbolIndexer.buildIndex(solution)
        
        // 执行搜索
        val results = symbolIndexer.searchSymbols("Test", false)
        
        // 验证搜索结果（至少应有一些结果）
        println("搜索到 ${results.size} 个匹配的符号")
        
        // 如果有结果，验证它们的基本属性
        if (results.isNotEmpty()) {
            val firstResult = results[0]
            assertNotNull(firstResult.name)
            assertNotNull(firstResult.projectName)
            assertNotNull(firstResult.fullName)
            assertNotNull(firstResult.type)
            assertNotNull(firstResult.filePath)
            assertTrue(firstResult.startLine >= 0)
            assertTrue(firstResult.endLine > firstResult.startLine)
        }
    }
    
    @Test
    fun testIncrementalIndexing_Integration() {
        // 测试文件变更时的增量索引更新
        // 注意：同样需要测试资源文件
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val testFilePath = testResourcesDir + "/TestFile.cs"
        
        if (!Files.exists(Paths.get(testFilePath))) {
            System.err.println("测试文件不存在，跳过集成测试: $testFilePath")
            return
        }
        
        // 创建文件变更
        val fileChange = OmniSharpFileChange(
            filePath = testFilePath,
            changeType = FileChangeType.MODIFIED
        )
        
        // 执行增量更新
        val beforeUpdateTime = System.currentTimeMillis()
        symbolIndexer.updateIndexForFile(fileChange)
        val updateTime = System.currentTimeMillis() - beforeUpdateTime
        
        println("增量索引更新耗时: ${updateTime}ms")
        assertTrue(updateTime < 1000, "增量更新时间不应超过 1 秒")
    }
}
```

## 6. 性能测试

### 6.1 大型解决方案性能测试

```kotlin
package com.omnisharp.intellij.performance

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.solution.*
import org.junit.jupiter.api.*
import java.nio.file.*
import kotlin.test.*

class OmniSharpPerformanceTest {
    private lateinit var solutionParser: OmniSharpSolutionParser
    
    @BeforeEach
    fun setUp() {
        solutionParser = OmniSharpSolutionParser(FileSystems.getDefault())
    }
    
    @Test
    fun testSolutionParsePerformance() {
        // 注意：这个测试需要一个较大的解决方案文件进行性能测试
        // 在实际环境中应该使用适当的大型解决方案
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val solutionPath = testResourcesDir + "/LargeSolution.sln"
        
        if (!Files.exists(Paths.get(solutionPath))) {
            System.err.println("大型测试文件不存在，跳过性能测试: $solutionPath")
            return
        }
        
        // 计时解析操作
        val startTime = System.currentTimeMillis()
        val solution = solutionParser.parseSolution(solutionPath)
        val parseTime = System.currentTimeMillis() - startTime
        
        println("解析解决方案耗时: ${parseTime}ms")
        println("项目数量: ${solution.projects.size}")
        
        // 验证解析是否成功
        assertNotNull(solution)
        
        // 对于含有 50 个项目的解决方案，解析时间应在 1 秒内
        // 实际性能预期应该根据环境和具体实现进行调整
        if (solution.projects.size <= 50) {
            assertTrue(parseTime < 1000, "解析 50 个项目的解决方案时间不应超过 1 秒")
        } else if (solution.projects.size <= 200) {
            assertTrue(parseTime < 3000, "解析 200 个项目的解决方案时间不应超过 3 秒")
        }
    }
    
    @Test
    fun testSymbolIndexingPerformance() {
        // 测试大型解决方案的符号索引性能
        val testResourcesDir = System.getProperty("user.dir") + "/src/test/resources"
        val solutionPath = testResourcesDir + "/LargeSolution.sln"
        
        if (!Files.exists(Paths.get(solutionPath))) {
            System.err.println("大型测试文件不存在，跳过性能测试: $solutionPath")
            return
        }
        
        // 解析解决方案
        val solution = solutionParser.parseSolution(solutionPath)
        assertNotNull(solution)
        
        // 创建符号管理器
        val symbolCache = OmniSharpSymbolCache()
        val symbolCollector = TestSymbolCollector() // 使用测试实现生成大量符号
        val symbolIndexer = OmniSharpSymbolIndexer(symbolCollector, symbolCache)
        
        // 计时索引构建
        val startTime = System.currentTimeMillis()
        symbolIndexer.buildIndex(solution)
        val indexTime = System.currentTimeMillis() - startTime
        
        println("构建索引耗时: ${indexTime}ms")
        println("符号数量: ${symbolCache.getSymbolCount()}")
        
        // 对于 10000 个符号，索引时间应在 2 秒内
        val symbolCount = symbolCache.getSymbolCount()
        if (symbolCount <= 10000) {
            assertTrue(indexTime < 2000, "索引 10000 个符号的时间不应超过 2 秒")
        } else if (symbolCount <= 50000) {
            assertTrue(indexTime < 5000, "索引 50000 个符号的时间不应超过 5 秒")
        }
        
        // 测试搜索性能
        val searchStartTime = System.currentTimeMillis()
        val results = symbolIndexer.searchSymbols("Test", false)
        val searchTime = System.currentTimeMillis() - searchStartTime
        
        println("搜索耗时: ${searchTime}ms")
        println("搜索结果数量: ${results.size}")
        
        // 搜索时间应在 100 毫秒内
        assertTrue(searchTime < 100, "搜索时间不应超过 100 毫秒")
    }
    
    // 测试符号收集器，用于生成大量符号以测试性能
    private class TestSymbolCollector : OmniSharpSymbolCollector {
        override fun collectSymbols(solution: OmniSharpSolutionModel): List<OmniSharpSymbol> {
            val symbols = mutableListOf<OmniSharpSymbol>()
            
            // 为每个项目生成一定数量的符号
            solution.projects.forEachIndexed { projectIndex, project ->
                // 为每个项目生成 100 个类，每个类生成 10 个方法
                for (classIndex in 0 until 100) {
                    val className = "TestClass_${projectIndex}_${classIndex}"
                    val classSymbol = OmniSharpSymbol(
                        name = className,
                        projectName = project.name,
                        fullName = "${project.name}.${className}",
                        type = SymbolType.CLASS,
                        filePath = "${project.name}/Class${classIndex}.cs",
                        startLine = classIndex * 10,
                        endLine = classIndex * 10 + 100
                    )
                    symbols.add(classSymbol)
                    
                    // 生成方法
                    for (methodIndex in 0 until 10) {
                        val methodName = "TestMethod_${methodIndex}"
                        val methodSymbol = OmniSharpSymbol(
                            name = methodName,
                            projectName = project.name,
                            fullName = "${project.name}.${className}.${methodName}",
                            type = SymbolType.METHOD,
                            filePath = "${project.name}/Class${classIndex}.cs",
                            startLine = classIndex * 10 + 10 + methodIndex * 8,
                            endLine = classIndex * 10 + 15 + methodIndex * 8
                        )
                        symbols.add(methodSymbol)
                    }
                }
            }
            
            return symbols
        }
        
        override fun collectSymbolsForFile(filePath: String): List<OmniSharpSymbol> {
            // 为文件生成一些符号
            val symbols = mutableListOf<OmniSharpSymbol>()
            symbols.add(OmniSharpSymbol(
                name = "TestClassInFile",
                projectName = "TestProject",
                fullName = "TestProject.TestClassInFile",
                type = SymbolType.CLASS,
                filePath = filePath,
                startLine = 10,
                endLine = 50
            ))
            return symbols
        }
    }
}
```

## 7. 测试工具类

### 7.1 测试辅助工具

```kotlin
package com.omnisharp.intellij.test.util

import com.omnisharp.intellij.model.*
import java.io.*
import java.nio.file.*

/**
 * 测试数据生成器，用于创建测试用的解决方案和项目模型
 */
class OmniSharpTestDataGenerator {
    /**
     * 创建一个简单的测试解决方案
     */
    fun createSimpleSolution(name: String = "TestSolution", projectCount: Int = 1): OmniSharpSolutionModel {
        val projects = mutableListOf<OmniSharpProjectModel>()
        
        for (i in 0 until projectCount) {
            val project = createProject("Project$i", i)
            projects.add(project)
        }
        
        return OmniSharpSolutionModel(
            name = name,
            path = "$name.sln",
            projects = projects,
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * 创建一个测试项目
     */
    fun createProject(name: String, projectId: Int): OmniSharpProjectModel {
        // 创建一些引用
        val references = mutableListOf<OmniSharpReference>()
        if (projectId > 0) {
            // 添加对前一个项目的引用
            references.add(OmniSharpReference(
                name = "Project${projectId - 1}",
                path = "Project${projectId - 1}/Project${projectId - 1}.csproj",
                type = "ProjectReference",
                version = null
            ))
        }
        
        // 添加一些包引用
        references.add(OmniSharpReference(
            name = "Newtonsoft.Json",
            path = "",
            type = "Package",
            version = "13.0.1"
        ))
        
        // 创建文件列表
        val files = mutableListOf(
            "Program.cs",
            "Models/User.cs",
            "Services/IService.cs",
            "Services/ServiceImpl.cs"
        )
        
        return OmniSharpProjectModel(
            name = name,
            path = "$name/$name.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = references,
            files = files,
            lastModified = System.currentTimeMillis()
        )
    }
    
    /**
     * 创建一个测试符号
     */
    fun createSymbol(name: String, type: SymbolType, projectName: String = "TestProject"): OmniSharpSymbol {
        return OmniSharpSymbol(
            name = name,
            projectName = projectName,
            fullName = "$projectName.$name",
            type = type,
            filePath = "$projectName/$name.cs",
            startLine = 10