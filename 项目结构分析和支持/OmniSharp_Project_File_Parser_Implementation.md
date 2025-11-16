# OmniSharp 项目文件解析和管理功能实现

## 1. 概述

本文档详细描述 OmniSharp 中项目文件解析和管理功能的实现。项目文件解析器负责解析各种类型的 .NET 项目文件（.csproj, .fsproj, .vbproj），提取项目配置、引用、编译文件等信息，为 IntelliJ 平台提供完整的项目模型支持。

### 1.1 功能目标

- 支持多种项目文件格式：
  - SDK 风格项目文件（新版 .NET Core/.NET 5+）
  - 传统项目文件（旧版 .NET Framework）
- 提取项目基本信息：
  - 项目名称、程序集名称
  - 目标框架版本
  - 输出路径和平台设置
- 解析项目引用：
  - 项目间引用
  - NuGet 包引用
  - 文件引用
- 提取编译文件列表：
  - 源代码文件
  - 资源文件
  - 内容文件
- 支持项目文件变更监控和增量更新
- 提供项目模型的查询和管理接口

### 1.2 项目文件格式

#### 1.2.1 SDK 风格项目文件

SDK 风格项目文件是 .NET Core 和 .NET 5+ 项目使用的简洁格式，示例：

```xml
<Project Sdk="Microsoft.NET.Sdk">
  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net6.0</TargetFramework>
    <ImplicitUsings>enable</ImplicitUsings>
    <Nullable>enable</Nullable>
  </PropertyGroup>
  
  <ItemGroup>
    <ProjectReference Include="..\LibraryProject\LibraryProject.csproj" />
  </ItemGroup>
  
  <ItemGroup>
    <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
  </ItemGroup>
</Project>
```

#### 1.2.2 传统项目文件

传统项目文件用于 .NET Framework 项目，示例：

```xml
<?xml version="1.0" encoding="utf-8"?>
<Project ToolsVersion="15.0" xmlns="http://schemas.microsoft.com/developer/msbuild/2003">
  <PropertyGroup>
    <Configuration Condition=" '$(Configuration)' == '' ">Debug</Configuration>
    <Platform Condition=" '$(Platform)' == '' ">AnyCPU</Platform>
    <ProjectGuid>{12345678-1234-1234-1234-123456789012}</ProjectGuid>
    <OutputType>Exe</OutputType>
    <AssemblyName>TraditionalProject</AssemblyName>
    <TargetFrameworkVersion>v4.7.2</TargetFrameworkVersion>
  </PropertyGroup>
  <ItemGroup>
    <Reference Include="System" />
    <Reference Include="System.Core" />
  </ItemGroup>
  <ItemGroup>
    <Compile Include="Program.cs" />
    <Compile Include="Properties\AssemblyInfo.cs" />
  </ItemGroup>
  <ItemGroup>
    <ProjectReference Include="..\LibraryProject\LibraryProject.csproj">
      <Project>{87654321-4321-4321-4321-210987654321}</Project>
      <Name>LibraryProject</Name>
    </ProjectReference>
  </ItemGroup>
</Project>
```

## 2. 架构设计

### 2.1 组件关系图

```
┌─────────────────────────┐
│     ProjectManager      │
└─────────────┬───────────┘
              │
              ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│  ProjectParserFacade    │────▶│  ProjectFileWatcher     │
└─────────────┬───────────┘     └─────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────────────────────┐
│                      Parser Factory                      │
└─────────────┬─────────────────────────┬──────────────────┘
              │                         │
              ▼                         ▼
┌─────────────────────────┐     ┌─────────────────────────┐
│  SdkStyleProjectParser  │     │  LegacyProjectParser    │
└─────────────┬───────────┘     └─────────────┬───────────┘
              │                                │
              └────────────────┬───────────────┘
                               │
                               ▼
                        ┌─────────────────────────┐
                        │  ProjectModelBuilder    │
                        └─────────────┬───────────┘
                                        │
                                        ▼
                        ┌─────────────────────────┐
                        │      ProjectModel       │
                        └─────────────────────────┘
```

### 2.2 核心组件

1. **ProjectParserFacade**：项目文件解析的门面接口，根据文件格式选择合适的解析器
2. **ProjectFileWatcher**：监控项目文件变更，触发重新解析
3. **SdkStyleProjectParser**：解析 SDK 风格项目文件
4. **LegacyProjectParser**：解析传统项目文件
5. **ProjectModelBuilder**：构建和更新 ProjectModel 对象
6. **ProjectModel**：表示解析后的项目数据结构

### 2.3 解析流程

1. 读取项目文件内容
2. 检测项目文件类型（SDK 风格或传统风格）
3. 选择适当的解析器
4. 解析基本配置信息
5. 解析引用信息（项目引用、包引用、文件引用）
6. 解析编译文件列表
7. 构建或更新 ProjectModel 对象
8. 注册文件监听器，监控变更

## 3. 核心组件实现

### 3.1 ProjectParserFacade

ProjectParserFacade 是项目文件解析功能的入口点，根据文件格式选择合适的解析器。

```kotlin
package com.intellij.csharp.omnisharp.project.parsers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.csharp.omnisharp.project.model.ProjectModel
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

class ProjectParserFacade(private val project: Project) {
    private val executorService = Executors.newCachedThreadPool()
    private val fileWatcher = ProjectFileWatcher(project)
    private val sdkParser = SdkStyleProjectParser()
    private val legacyParser = LegacyProjectParser()
    
    /**
     * 同步解析项目文件
     */
    fun parseProject(projectFile: VirtualFile, baseProjectModel: ProjectModel? = null): ProjectModel {
        // 确定项目文件类型
        val content = projectFile.contentsToByteArray().toString(Charsets.UTF_8)
        val parser = if (isSdkStyleProject(content)) {
            sdkParser
        } else {
            legacyParser
        }
        
        // 执行解析
        val projectModel = parser.parse(projectFile, content, baseProjectModel)
        
        // 注册文件监听器
        registerFileWatcher(projectFile, projectModel)
        
        return projectModel
    }
    
    /**
     * 异步解析项目文件
     */
    fun parseProjectAsync(
        projectFile: VirtualFile,
        baseProjectModel: ProjectModel? = null,
        progressListener: ProgressListener? = null
    ): CompletableFuture<ProjectModel> {
        return CompletableFuture.supplyAsync({
            progressListener?.onProgress(0, "开始解析项目文件")
            val content = projectFile.contentsToByteArray().toString(Charsets.UTF_8)
            progressListener?.onProgress(20, "解析文件内容完成")
            
            val parser = if (isSdkStyleProject(content)) {
                progressListener?.onProgress(30, "识别为SDK风格项目")
                sdkParser
            } else {
                progressListener?.onProgress(30, "识别为传统风格项目")
                legacyParser
            }
            
            val projectModel = parser.parse(projectFile, content, baseProjectModel)
            progressListener?.onProgress(90, "项目模型构建完成")
            
            registerFileWatcher(projectFile, projectModel)
            progressListener?.onProgress(100, "项目文件解析完成")
            
            projectModel
        }, executorService)
    }
    
    /**
     * 重新解析项目文件
     */
    fun reparseProject(projectFile: VirtualFile, currentModel: ProjectModel): ProjectModel {
        return parseProject(projectFile, currentModel)
    }
    
    /**
     * 取消文件监控
     */
    fun unsubscribeFile(filePath: String) {
        fileWatcher.unsubscribe(filePath)
    }
    
    /**
     * 添加项目文件变更监听器
     */
    fun addProjectChangeListener(listener: ProjectChangeListener) {
        fileWatcher.addProjectChangeListener(listener)
    }
    
    /**
     * 移除项目文件变更监听器
     */
    fun removeProjectChangeListener(listener: ProjectChangeListener) {
        fileWatcher.removeProjectChangeListener(listener)
    }
    
    /**
     * 关闭资源
     */
    fun dispose() {
        executorService.shutdown()
        fileWatcher.dispose()
    }
    
    /**
     * 检测是否为SDK风格项目
     */
    private fun isSdkStyleProject(content: String): Boolean {
        // 检查Project标签是否包含Sdk属性
        return content.contains("<Project[^>]*Sdk=\") || 
               content.contains("<Project[^>]*Sdk=\'")
    }
    
    /**
     * 注册文件监听器
     */
    private fun registerFileWatcher(projectFile: VirtualFile, projectModel: ProjectModel) {
        fileWatcher.subscribe(projectFile, projectModel) { changedFile ->
            // 文件变更时重新解析
            reparseProject(changedFile, projectModel)
        }
    }
}

/**
 * 进度监听器接口
 */
interface ProgressListener {
    fun onProgress(percent: Int, message: String)
}

/**
 * 项目文件变更监听器接口
 */
interface ProjectChangeListener {
    fun onProjectFileChanged(projectFile: VirtualFile, projectModel: ProjectModel)
    fun onProjectLoaded(projectModel: ProjectModel)
    fun onProjectLoadFailed(projectFile: VirtualFile, error: Throwable)
}
```

### 3.2 ProjectFileWatcher

ProjectFileWatcher 负责监控项目文件变更，触发重新解析。

```kotlin
package com.intellij.csharp.omnisharp.project.parsers

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.csharp.omnisharp.project.model.ProjectModel
import java.util.concurrent.ConcurrentHashMap

class ProjectFileWatcher(private val project: Project) : Disposable {
    private val fileSubscriptions = ConcurrentHashMap<String, Pair<ProjectModel, (VirtualFile) -> Unit>>()
    private val projectChangeListeners = mutableSetOf<ProjectChangeListener>()
    
    init {
        // 注册VFS文件变更监听器
        project.messageBus.connect(this).subscribe(
            com.intellij.openapi.vfs.VirtualFileManager.VFS_CHANGES,
            object : BulkFileListener {
                override fun before(events: MutableList<out VFileEvent>) {}
                
                override fun after(events: MutableList<out VFileEvent>) {
                    for (event in events) {
                        if (event is VFileContentChangeEvent) {
                            handleFileContentChanged(event)
                        }
                    }
                }
            }
        )
    }
    
    /**
     * 订阅文件变更
     */
    fun subscribe(file: VirtualFile, projectModel: ProjectModel, callback: (VirtualFile) -> Unit) {
        fileSubscriptions[file.path] = Pair(projectModel, callback)
    }
    
    /**
     * 取消订阅
     */
    fun unsubscribe(filePath: String) {
        fileSubscriptions.remove(filePath)
    }
    
    /**
     * 添加项目变更监听器
     */
    fun addProjectChangeListener(listener: ProjectChangeListener) {
        projectChangeListeners.add(listener)
    }
    
    /**
     * 移除项目变更监听器
     */
    fun removeProjectChangeListener(listener: ProjectChangeListener) {
        projectChangeListeners.remove(listener)
    }
    
    /**
     * 处理文件内容变更
     */
    private fun handleFileContentChanged(event: VFileContentChangeEvent) {
        val filePath = event.file.path
        val subscription = fileSubscriptions[filePath]
        
        if (subscription != null) {
            val (projectModel, callback) = subscription
            try {
                // 调用回调函数重新解析文件
                val updatedModel = callback(event.file)
                
                // 通知所有监听器
                for (listener in projectChangeListeners) {
                    listener.onProjectFileChanged(event.file, updatedModel)
                }
            } catch (e: Exception) {
                // 通知加载失败
                for (listener in projectChangeListeners) {
                    listener.onProjectLoadFailed(event.file, e)
                }
            }
        }
    }
    
    /**
     * 通知项目已加载
     */
    fun notifyProjectLoaded(projectModel: ProjectModel) {
        for (listener in projectChangeListeners) {
            listener.onProjectLoaded(projectModel)
        }
    }
    
    override fun dispose() {
        fileSubscriptions.clear()
        projectChangeListeners.clear()
    }
}
```

### 3.3 SdkStyleProjectParser

SdkStyleProjectParser 负责解析 SDK 风格的项目文件。

```kotlin
package com.intellij.csharp.omnisharp.project.parsers

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.csharp.omnisharp.project.model.*
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.io.StringReader
import java.nio.file.Files
import java.nio.file.Paths

class SdkStyleProjectParser {
    fun parse(
        projectFile: VirtualFile,
        content: String,
        baseProjectModel: ProjectModel? = null
    ): ProjectModel {
        val projectDir = projectFile.parent.path
        
        // 解析XML内容
        val document = parseXml(content)
        val rootElement = document.rootElement
        
        // 创建或更新项目模型
        val projectModel = baseProjectModel?.let { createUpdatedModel(it) } ?: createNewModel(projectFile, rootElement, projectDir)
        
        // 解析PropertyGroup
        parsePropertyGroups(rootElement, projectModel)
        
        // 解析项目引用
        projectModel.projectReferences = parseProjectReferences(rootElement, projectDir)
        
        // 解析包引用
        projectModel.packageReferences = parsePackageReferences(rootElement)
        
        // 解析文件引用
        projectModel.fileReferences = parseFileReferences(rootElement, projectDir)
        
        // 解析项目文件
        projectModel.projectFiles = parseProjectFiles(rootElement, projectDir, projectModel)
        
        return projectModel
    }
    
    private fun parseXml(content: String): Document {
        val builder = SAXBuilder()
        return builder.build(StringReader(content))
    }
    
    private fun createNewModel(projectFile: VirtualFile, rootElement: Element, projectDir: String): ProjectModel {
        // 获取项目名称（从文件名）
        val projectName = projectFile.nameWithoutExtension
        
        return ProjectModel(
            id = java.util.UUID.randomUUID().toString(), // 临时ID，可能需要从其他地方获取真实ID
            name = projectName,
            path = projectFile.path,
            directory = projectDir,
            outputPath = "bin/Debug/", // 默认值，后续会更新
            assemblyName = projectName, // 默认值，后续会更新
            targetFramework = "netstandard2.0", // 默认值，后续会更新
            configurations = emptyMap(), // 暂时为空，后续可能从解决方案中获取
            projectReferences = emptyList(),
            packageReferences = emptyList(),
            fileReferences = emptyList(),
            projectFiles = emptyList(),
            language = detectProjectLanguage(projectFile.name)
        )
    }
    
    private fun createUpdatedModel(baseModel: ProjectModel): ProjectModel {
        // 创建新模型，保留原始ID和配置信息
        return ProjectModel(
            id = baseModel.id,
            name = baseModel.name,
            path = baseModel.path,
            directory = baseModel.directory,
            outputPath = baseModel.outputPath, // 将在parsePropertyGroups中更新
            assemblyName = baseModel.assemblyName, // 将在parsePropertyGroups中更新
            targetFramework = baseModel.targetFramework, // 将在parsePropertyGroups中更新
            configurations = baseModel.configurations,
            projectReferences = baseModel.projectReferences, // 将在parseProjectReferences中更新
            packageReferences = baseModel.packageReferences, // 将在parsePackageReferences中更新
            fileReferences = baseModel.fileReferences, // 将在parseFileReferences中更新
            projectFiles = baseModel.projectFiles, // 将在parseProjectFiles中更新
            language = baseModel.language
        )
    }
    
    private fun detectProjectLanguage(fileName: String): ProjectModel.ProjectLanguage {
        return when {
            fileName.endsWith(".csproj") -> ProjectModel.ProjectLanguage.CSHARP
            fileName.endsWith(".fsproj") -> ProjectModel.ProjectLanguage.FSHARP
            fileName.endsWith(".vbproj") -> ProjectModel.ProjectLanguage.VISUAL_BASIC
            else -> ProjectModel.ProjectLanguage.CSHARP // 默认
        }
    }
    
    private fun parsePropertyGroups(rootElement: Element, projectModel: ProjectModel) {
        val propertyGroups = rootElement.getChildren("PropertyGroup")
        
        for (group in propertyGroups) {
            // 检查是否有配置条件
            val condition = group.getAttributeValue("Condition")
            val isActiveConfig = condition == null || isActiveCondition(condition)
            
            if (isActiveConfig) {
                // 解析基本属性
                group.getChildTextNormalize("OutputPath")?.let { projectModel.outputPath = it }
                group.getChildTextNormalize("AssemblyName")?.let { projectModel.assemblyName = it }
                group.getChildTextNormalize("TargetFramework")?.let { projectModel.targetFramework = it }
                
                // 检查多目标框架
                group.getChildTextNormalize("TargetFrameworks")?.let { targetFrameworks ->
                    // 如果有多个目标框架，使用第一个作为默认值
                    val frameworks = targetFrameworks.split(';').map { it.trim() }
                    if (frameworks.isNotEmpty()) {
                        projectModel.targetFramework = frameworks[0]
                    }
                }
            }
        }
    }
    
    private fun isActiveCondition(condition: String): Boolean {
        // 简化的条件评估逻辑
        // 在实际实现中，可能需要更复杂的条件解析器
        return condition.contains("'$(Configuration)' == 'Debug'") || 
               condition.contains("'$(Configuration)' == 'Release'")
    }
    
    private fun parseProjectReferences(rootElement: Element, projectDir: String): List<String> {
        val projectReferences = mutableListOf<String>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        for (group in itemGroups) {
            val references = group.getChildren("ProjectReference")
            for (reference in references) {
                val includePath = reference.getAttributeValue("Include")
                if (includePath != null) {
                    // 在实际实现中，这里应该解析引用的项目文件，获取其ID
                    // 暂时返回路径作为标识
                    projectReferences.add(includePath)
                }
            }
        }
        
        return projectReferences
    }
    
    private fun parsePackageReferences(rootElement: Element): List<PackageReference> {
        val packageReferences = mutableListOf<PackageReference>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        for (group in itemGroups) {
            val references = group.getChildren("PackageReference")
            for (reference in references) {
                val includeId = reference.getAttributeValue("Include")
                val version = reference.getAttributeValue("Version") ?: reference.getChildTextNormalize("Version")
                
                if (includeId != null && version != null) {
                    val includeAssets = parseAssets(reference, "IncludeAssets")
                    val excludeAssets = parseAssets(reference, "ExcludeAssets")
                    
                    packageReferences.add(PackageReference(
                        id = includeId,
                        version = version,
                        includeAssets = includeAssets,
                        excludeAssets = excludeAssets
                    ))
                }
            }
        }
        
        return packageReferences
    }
    
    private fun parseAssets(reference: Element, elementName: String): List<String> {
        val assetsElement = reference.getChild(elementName)
        return if (assetsElement != null) {
            assetsElement.textNormalize.split(';').map { it.trim() }
        } else {
            emptyList()
        }
    }
    
    private fun parseFileReferences(rootElement: Element, projectDir: String): List<FileReference> {
        val fileReferences = mutableListOf<FileReference>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        for (group in itemGroups) {
            val references = group.getChildren("Reference")
            for (reference in references) {
                val includePath = reference.getAttributeValue("Include")
                val hintPath = reference.getChildTextNormalize("HintPath")
                val private = reference.getChildTextNormalize("Private")?.toBoolean() ?: true
                val specificVersion = reference.getChildTextNormalize("SpecificVersion")?.toBoolean() ?: false
                
                if (includePath != null) {
                    fileReferences.add(FileReference(
                        path = includePath,
                        hintPath = hintPath ?: "",
                        private = private,
                        specificVersion = specificVersion
                    ))
                }
            }
        }
        
        return fileReferences
    }
    
    private fun parseProjectFiles(rootElement: Element, projectDir: String, projectModel: ProjectModel): List<String> {
        val projectFiles = mutableListOf<String>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        // 收集显式声明的文件
        for (group in itemGroups) {
            // 解析编译文件
            val compileItems = group.getChildren("Compile")
            for (item in compileItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
            
            // 解析资源文件
            val resourceItems = group.getChildren("EmbeddedResource")
            for (item in resourceItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
            
            // 解析内容文件
            val contentItems = group.getChildren("Content")
            for (item in contentItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
        }
        
        // SDK 风格项目默认包含项目目录下的所有源代码文件
        // 除非设置了 <EnableDefaultCompileItems>false</EnableDefaultCompileItems>
        if (!hasDefaultCompileItemsDisabled(rootElement)) {
            val defaultFiles = findDefaultSourceFiles(projectDir, projectModel.language)
            projectFiles.addAll(defaultFiles)
        }
        
        return projectFiles.distinct()
    }
    
    private fun hasDefaultCompileItemsDisabled(rootElement: Element): Boolean {
        val propertyGroups = rootElement.getChildren("PropertyGroup")
        for (group in propertyGroups) {
            val disableDefaultItems = group.getChildTextNormalize("EnableDefaultCompileItems")
            if (disableDefaultItems != null && disableDefaultItems.equals("false", ignoreCase = true)) {
                return true
            }
        }
        return false
    }
    
    private fun findDefaultSourceFiles(projectDir: String, language: ProjectModel.ProjectLanguage): List<String> {
        val sourceFiles = mutableListOf<String>()
        val sourceExtensions = when (language) {
            ProjectModel.ProjectLanguage.CSHARP -> listOf(".cs")
            ProjectModel.ProjectLanguage.FSHARP -> listOf(".fs")
            ProjectModel.ProjectLanguage.VISUAL_BASIC -> listOf(".vb")
        }
        
        // 简单实现：递归查找项目目录下的所有源文件
        try {
            Files.walk(Paths.get(projectDir))
                .filter { path ->
                    val fileName = path.fileName?.toString() ?: ""
                    sourceExtensions.any { fileName.endsWith(it) }
                }
                .forEach { path ->
                    sourceFiles.add(path.toString())
                }
        } catch (e: Exception) {
            // 忽略错误
        }
        
        return sourceFiles
    }
    
    private fun resolvePath(baseDir: String, relativePath: String): String? {
        // 简化的路径解析
        // 在实际实现中，可能需要处理通配符、条件表达式等
        try {
            val basePath = Paths.get(baseDir)
            val relative = Paths.get(relativePath)
            return basePath.resolve(relative).normalize().toString()
        } catch (e: Exception) {
            return null
        }
    }
}
```

### 3.4 LegacyProjectParser

LegacyProjectParser 负责解析传统风格的项目文件。

```kotlin
package com.intellij.csharp.omnisharp.project.parsers

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.csharp.omnisharp.project.model.*
import org.jdom2.Document
import org.jdom2.Element
import org.jdom2.input.SAXBuilder
import java.io.StringReader
import java.nio.file.Paths
import java.util.*

class LegacyProjectParser {
    fun parse(
        projectFile: VirtualFile,
        content: String,
        baseProjectModel: ProjectModel? = null
    ): ProjectModel {
        val projectDir = projectFile.parent.path
        
        // 解析XML内容
        val document = parseXml(content)
        val rootElement = document.rootElement
        
        // 创建或更新项目模型
        val projectModel = baseProjectModel?.let { createUpdatedModel(it) } ?: createNewModel(projectFile, rootElement, projectDir)
        
        // 解析PropertyGroup
        parsePropertyGroups(rootElement, projectModel)
        
        // 解析项目引用
        projectModel.projectReferences = parseProjectReferences(rootElement, projectDir)
        
        // 解析文件引用
        projectModel.fileReferences = parseFileReferences(rootElement, projectDir)
        
        // 解析项目文件
        projectModel.projectFiles = parseProjectFiles(rootElement, projectDir)
        
        return projectModel
    }
    
    private fun parseXml(content: String): Document {
        val builder = SAXBuilder()
        return builder.build(StringReader(content))
    }
    
    private fun createNewModel(projectFile: VirtualFile, rootElement: Element, projectDir: String): ProjectModel {
        // 获取项目名称（从文件名）
        val projectName = projectFile.nameWithoutExtension
        
        // 尝试从ProjectGuid元素获取ID
        val projectGuid = rootElement.getChildTextNormalize("PropertyGroup")?.let {
            val propertyGroup = rootElement.getChild("PropertyGroup")
            propertyGroup?.getChildTextNormalize("ProjectGuid")?.trim('{', '}')
        } ?: java.util.UUID.randomUUID().toString()
        
        return ProjectModel(
            id = projectGuid,
            name = projectName,
            path = projectFile.path,
            directory = projectDir,
            outputPath = "bin/Debug/", // 默认值，后续会更新
            assemblyName = projectName, // 默认值，后续会更新
            targetFramework = "v4.7.2", // 默认值，后续会更新
            configurations = emptyMap(), // 暂时为空，后续可能从解决方案中获取
            projectReferences = emptyList(),
            packageReferences = emptyList(), // 传统项目可能使用packages.config
            fileReferences = emptyList(),
            projectFiles = emptyList(),
            language = detectProjectLanguage(projectFile.name)
        )
    }
    
    private fun createUpdatedModel(baseModel: ProjectModel): ProjectModel {
        // 创建新模型，保留原始ID和配置信息
        return ProjectModel(
            id = baseModel.id,
            name = baseModel.name,
            path = baseModel.path,
            directory = baseModel.directory,
            outputPath = baseModel.outputPath, // 将在parsePropertyGroups中更新
            assemblyName = baseModel.assemblyName, // 将在parsePropertyGroups中更新
            targetFramework = baseModel.targetFramework, // 将在parsePropertyGroups中更新
            configurations = baseModel.configurations,
            projectReferences = baseModel.projectReferences, // 将在parseProjectReferences中更新
            packageReferences = baseModel.packageReferences, // 传统项目可能需要特殊处理
            fileReferences = baseModel.fileReferences, // 将在parseFileReferences中更新
            projectFiles = baseModel.projectFiles, // 将在parseProjectFiles中更新
            language = baseModel.language
        )
    }
    
    private fun detectProjectLanguage(fileName: String): ProjectModel.ProjectLanguage {
        return when {
            fileName.endsWith(".csproj") -> ProjectModel.ProjectLanguage.CSHARP
            fileName.endsWith(".fsproj") -> ProjectModel.ProjectLanguage.FSHARP
            fileName.endsWith(".vbproj") -> ProjectModel.ProjectLanguage.VISUAL_BASIC
            else -> ProjectModel.ProjectLanguage.CSHARP // 默认
        }
    }
    
    private fun parsePropertyGroups(rootElement: Element, projectModel: ProjectModel) {
        val propertyGroups = rootElement.getChildren("PropertyGroup")
        
        for (group in propertyGroups) {
            // 检查是否有配置条件
            val condition = group.getAttributeValue("Condition")
            val isActiveConfig = condition == null || isActiveCondition(condition)
            
            if (isActiveConfig) {
                // 解析基本属性
                group.getChildTextNormalize("OutputPath")?.let { projectModel.outputPath = it }
                group.getChildTextNormalize("AssemblyName")?.let { projectModel.assemblyName = it }
                group.getChildTextNormalize("TargetFrameworkVersion")?.let { projectModel.targetFramework = it }
            }
        }
    }
    
    private fun isActiveCondition(condition: String): Boolean {
        // 简化的条件评估逻辑
        return condition.contains("'$(Configuration)' == 'Debug'") || 
               condition.contains("'$(Configuration)' == 'Release'")
    }
    
    private fun parseProjectReferences(rootElement: Element, projectDir: String): List<String> {
        val projectReferences = mutableListOf<String>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        for (group in itemGroups) {
            val references = group.getChildren("ProjectReference")
            for (reference in references) {
                val projectId = reference.getChildTextNormalize("Project")?.trim('{', '}')
                if (projectId != null) {
                    projectReferences.add(projectId)
                }
            }
        }
        
        return projectReferences
    }
    
    private fun parseFileReferences(rootElement: Element, projectDir: String): List<FileReference> {
        val fileReferences = mutableListOf<FileReference>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        for (group in itemGroups) {
            val references = group.getChildren("Reference")
            for (reference in references) {
                val includePath = reference.getAttributeValue("Include")
                val hintPath = reference.getChildTextNormalize("HintPath")
                val private = reference.getChildTextNormalize("Private")?.toBoolean() ?: true
                val specificVersion = reference.getChildTextNormalize("SpecificVersion")?.toBoolean() ?: false
                
                if (includePath != null) {
                    fileReferences.add(FileReference(
                        path = includePath,
                        hintPath = hintPath ?: "",
                        private = private,
                        specificVersion = specificVersion
                    ))
                }
            }
        }
        
        return fileReferences
    }
    
    private fun parseProjectFiles(rootElement: Element, projectDir: String): List<String> {
        val projectFiles = mutableListOf<String>()
        val itemGroups = rootElement.getChildren("ItemGroup")
        
        // 收集显式声明的文件
        for (group in itemGroups) {
            // 解析编译文件
            val compileItems = group.getChildren("Compile")
            for (item in compileItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
            
            // 解析资源文件
            val resourceItems = group.getChildren("EmbeddedResource")
            for (item in resourceItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
            
            // 解析内容文件
            val contentItems = group.getChildren("Content")
            for (item in contentItems) {
                val includePath = item.getAttributeValue("Include")
                if (includePath != null) {
                    val fullPath = resolvePath(projectDir, includePath)
                    if (fullPath != null) {
                        projectFiles.add(fullPath)
                    }
                }
            }
        }
        
        return projectFiles.distinct()
    }
    
    private fun resolvePath(baseDir: String, relativePath: String): String? {
        try {
            val basePath = Paths.get(baseDir)
            val relative = Paths.get(relativePath)
            return basePath.resolve(relative).normalize().toString()
        } catch (e: Exception) {
            return null
        }
    }
}
```

## 4. 扩展 ProjectModel

为了更好地支持项目文件解析，我们需要扩展 ProjectModel 类：

```kotlin
package com.intellij.csharp.omnisharp.project.model

import java.io.File
import java.util.*

class ProjectModel(
    val id: String,
    val name: String,
    val path: String,
    val directory: String,
    var outputPath: String,
    var assemblyName: String,
    var targetFramework: String,
    val configurations: Map<String, ProjectConfiguration>,
    var projectReferences: List<String>, // 项目 ID 列表
    var packageReferences: List<PackageReference>,
    var fileReferences: List<FileReference>,
    var projectFiles: List<String>, // 文件路径列表
    val language: ProjectLanguage,
    // 新增字段
    var outputType: String = "Library", // 输出类型: Exe, Library, WinExe
    var rootNamespace: String = name, // 根命名空间
    var defaultNamespace: String = name, // 默认命名空间
    var isSdkProject: Boolean = false, // 是否为SDK风格项目
    var projectFileTimestamp: Long = 0, // 项目文件时间戳
    var projectFileVersion: Int = 0, // 项目文件版本号，用于跟踪变更
    var projectTypeGuids: List<String> = emptyList(), // 项目类型GUID列表
    var msbuildToolsVersion: String = "15.0", // MSBuild工具版本
    var defines: List<String> = emptyList(), // 条件编译符号
    var nullableContextOptions: String = "disable", // 可空引用类型选项
    var implicitUsings: String = "disable", // 隐式using指令选项
    var targetFrameworks: List<String> = emptyList() // 多目标框架
) {
    enum class ProjectLanguage {
        CSHARP,
        FSHARP,
        VISUAL_BASIC
    }
    
    // 项目文件类型分类
    enum class ProjectType {
        APPLICATION,
        LIBRARY,
        TEST,
        WEB,
        CONSOLE
    }
    
    /**
     * 获取项目的默认输出目录
     */
    fun getDefaultOutputDirectory(configuration: String = "Debug", platform: String = "AnyCPU"): String {
        val configPath = if (outputPath.contains("$(Configuration)") || outputPath.contains("$(Platform)")) {
            outputPath
                .replace("$(Configuration)", configuration)
                .replace("$(Platform)", platform)
        } else {
            outputPath
        }
        
        return directory + File.separator + configPath
    }
    
    /**
     * 获取项目的输出文件路径
     */
    fun getOutputFilePath(configuration: String = "Debug", platform: String = "AnyCPU"): String {
        val extension = when (outputType) {
            "Exe", "WinExe" -> ".exe"
            else -> ".dll"
        }
        
        val outputDir = getDefaultOutputDirectory(configuration, platform)
        return outputDir + File.separator + assemblyName + extension
    }
    
    /**
     * 获取项目配置
     */
    fun getConfiguration(configurationName: String): ProjectConfiguration? {
        return configurations.values.find { it.name == configurationName }
    }
    
    /**
     * 检查是否是C#项目
     */
    fun isCSharpProject(): Boolean = language == ProjectLanguage.CSHARP
    
    /**
     * 检查是否是F#项目
     */
    fun isFSharpProject(): Boolean = language == ProjectLanguage.FSHARP
    
    /**
     * 检查是否是VB项目
     */
    fun isVisualBasicProject(): Boolean = language == ProjectLanguage.VISUAL_BASIC
    
    /**
     * 检查是否是可执行项目
     */
    fun isExecutableProject(): Boolean = outputType == "Exe" || outputType == "WinExe"
    
    /**
     * 检查是否是库项目
     */
    fun isLibraryProject(): Boolean = outputType == "Library"
    
    /**
     * 检查是否是多目标框架项目
     */
    fun isMultiTargetProject(): Boolean = targetFrameworks.isNotEmpty()
    
    /**
     * 获取所有源文件
     */
    fun getSourceFiles(): List<String> {
        return projectFiles.filter { isSourceFile(it) }
    }
    
    /**
     * 获取所有资源文件
     */
    fun getResourceFiles(): List<String> {
        return projectFiles.filter { isResourceFile(it) }
    }
    
    /**
     * 检查文件是否为源文件
     */
    private fun isSourceFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return when (language) {
            ProjectLanguage.CSHARP -> extension == "cs"
            ProjectLanguage.FSHARP -> extension == "fs"
            ProjectLanguage.VISUAL_BASIC -> extension == "vb"
        }
    }
    
    /**
     * 检查文件是否为资源文件
     */
    private fun isResourceFile(filePath: String): Boolean {
        val extension = filePath.substringAfterLast('.', "").lowercase()
        return listOf("resx", "resources", "ico", "png", "jpg", "jpeg", "gif").contains(extension)
    }
    
    /**
     * 获取项目类型
     */
    fun getProjectType(): ProjectType {
        // 根据输出类型和项目文件类型判断
        return when {
            isExecutableProject() -> ProjectType.CONSOLE
            projectTypeGuids.any { it.equals("{3AC096D0-A1C2-E12C-1390-A8335801FDAB}", ignoreCase = true) } -> ProjectType.WEB
            projectTypeGuids.any { it.equals("{349c5851-65df-11da-bb86-000d5675b067}", ignoreCase = true) } -> ProjectType.WEB
            packageReferences.any { it.id.lowercase().contains("test") } -> ProjectType.TEST
            else -> ProjectType.LIBRARY
        }
    }
    
    /**
     * 获取项目文件的相对路径
     */
    fun getRelativePath(filePath: String): String {
        return try {
            File(filePath).relativeTo(File(directory)).path
        } catch (e: IllegalArgumentException) {
            filePath // 如果不在同一目录下，返回完整路径
        }
    }
    
    /**
     * 增加项目文件版本号
     */
    fun incrementVersion() {
        projectFileVersion++
    }
    
    override fun toString(): String {
        return "ProjectModel(id='$id', name='$name', path='$path', language=$language, outputType='$outputType')"
    }
}
```

## 5. 项目文件管理功能

为了更好地管理项目文件，我们实现 ProjectFileManager 类：

```kotlin
package com.intellij.csharp.omnisharp.project.manager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.csharp.omnisharp.project.model.ProjectModel
import com.intellij.csharp.omnisharp.project.parsers.ProjectParserFacade
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CompletableFuture

class OmniSharpProjectFileManager(private val project: Project) : ProjectFileManager {
    private val parserFacade = ProjectParserFacade(project)
    private val projectModels = ConcurrentHashMap<String, ProjectModel>()
    private val loadingProjects = ConcurrentHashMap<String, CompletableFuture<ProjectModel>>()
    
    init {
        // 添加项目文件变更监听器
        parserFacade.addProjectChangeListener(object : ProjectChangeListener {
            override fun onProjectFileChanged(projectFile: VirtualFile, projectModel: ProjectModel) {
                // 更新缓存中的项目模型
                projectModels[projectFile.path] = projectModel
                projectModel.incrementVersion()
            }
            
            override fun onProjectLoaded(projectModel: ProjectModel) {
                // 项目加载完成，更新缓存
                projectModels[projectModel.path] = projectModel
            }
            
            override fun onProjectLoadFailed(projectFile: VirtualFile, error: Throwable) {
                // 加载失败，移除加载中的标记
                loadingProjects.remove(projectFile.path)
            }
        })
    }
    
    override fun loadProject(projectFile: VirtualFile): CompletableFuture<ProjectModel> {
        val projectPath = projectFile.path
        
        // 检查是否已经加载
        val existingModel = projectModels[projectPath]
        if (existingModel != null) {
            return CompletableFuture.completedFuture(existingModel)
        }
        
        // 检查是否正在加载
        val loadingFuture = loadingProjects[projectPath]
        if (loadingFuture != null) {
            return loadingFuture
        }
        
        // 创建新的加载任务
        val newFuture = CompletableFuture<ProjectModel>()
        loadingProjects[projectPath] = newFuture
        
        try {
            // 异步解析项目文件
            parserFacade.parseProjectAsync(projectFile).thenAccept {
                projectModel ->
                projectModels[projectPath] = projectModel
                loadingProjects.remove(projectPath)
                newFuture.complete(projectModel)
            }.exceptionally {
                error ->
                loadingProjects.remove(projectPath)
                newFuture.completeExceptionally(error)
                null
            }
        } catch (e: Exception) {
            loadingProjects.remove(projectPath)
            newFuture.completeExceptionally(e)
        }
        
        return newFuture
    }
    
    override fun reloadProject(projectFile: VirtualFile): CompletableFuture<ProjectModel> {
        val projectPath = projectFile.path
        
        // 取消旧的加载任务（如果存在）
        loadingProjects.remove(projectPath)
        
        // 创建新的加载任务
        val newFuture = CompletableFuture<ProjectModel>()
        loadingProjects[projectPath] = newFuture
        
        try {
            // 获取当前模型（如果存在）
            val currentModel = projectModels[projectPath]
            
            // 重新解析项目文件
            parserFacade.parseProjectAsync(projectFile, currentModel).thenAccept {
                projectModel ->
                projectModel.incrementVersion()
                projectModels[projectPath] = projectModel
                loadingProjects.remove(projectPath)
                newFuture.complete(projectModel)
            }.exceptionally {
                error ->
                loadingProjects.remove(projectPath)
                newFuture.completeExceptionally(error)
                null
            }
        } catch (e: Exception) {
            loadingProjects.remove(projectPath)
            newFuture.completeExceptionally(e)
        }
        
        return newFuture
    }
    
    override fun getProject(projectFilePath: String): ProjectModel? {
        return projectModels[projectFilePath]
    }
    
    override fun getProjectFiles(): List<String> {
        return projectModels.keys.toList()
    }
    
    override fun getAllProjects(): List<ProjectModel> {
        return projectModels.values.toList()
    }
    
    override fun unloadProject(projectFilePath: String): Boolean {
        val projectModel = projectModels.remove(projectFilePath)
        if (projectModel != null) {
            // 取消文件监控
            parserFacade.unsubscribeFile(projectFilePath)
            return true
        }
        return false
    }
    
    override fun unloadAllProjects() {
        projectModels.keys.forEach {
            parserFacade.unsubscribeFile(it)
        }
        projectModels.clear()
    }
    
    override fun isProjectLoaded(projectFilePath: String): Boolean {
        return projectModels.containsKey(projectFilePath)
    }
    
    override fun findProjectByFile(filePath: String): ProjectModel? {
        return projectModels.values.find { projectModel ->
            projectModel.projectFiles.contains(filePath)
        }
    }
    
    override fun findProjectByName(projectName: String): ProjectModel? {
        return projectModels.values.find { it.name == projectName }
    }
    
    override fun close() {
        unloadAllProjects()
        parserFacade.dispose()
    }
}

interface ProjectFileManager {
    fun loadProject(projectFile: VirtualFile): CompletableFuture<ProjectModel>
    fun reloadProject(projectFile: VirtualFile): CompletableFuture<ProjectModel>
    fun getProject(projectFilePath: String): ProjectModel?
    fun getProjectFiles(): List<String>
    fun getAllProjects(): List<ProjectModel>
    fun unloadProject(projectFilePath: String): Boolean
    fun unloadAllProjects()
    fun isProjectLoaded(projectFilePath: String): Boolean
    fun findProjectByFile(filePath: String): ProjectModel?
    fun findProjectByName(projectName: String): ProjectModel?
    fun close()
}
```

## 6. 使用示例

### 6.1 基本使用

以下是如何使用项目文件解析器的基本示例：

```kotlin
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.csharp.omnisharp.project.parsers.ProjectParserFacade
import com.intellij.csharp.omnisharp.project.model.ProjectModel

class ProjectLoader(private val project: Project) {
    fun loadProject(projectPath: String): ProjectModel? {
        val fileSystem = LocalFileSystem.getInstance()
        val projectFile = fileSystem.findFileByPath(projectPath)
        
        if (projectFile != null && projectFile.isValid && 
            (projectFile.name.endsWith(".csproj") || 
             projectFile.name.endsWith(".fsproj") || 
             projectFile.name.endsWith(".vbproj"))) {
            
            val parserFacade = ProjectParserFacade(project)
            try {
                return parserFacade.parseProject(projectFile)
            } catch (e: Exception) {
                // 处理解析错误
                e.printStackTrace()
            }
        }
        
        return null
    }
    
    fun loadProjectAsync(projectPath: String, onComplete: (ProjectModel?) -> Unit) {
        val fileSystem = LocalFileSystem.getInstance()
        val projectFile = fileSystem.findFileByPath(projectPath)
        
        if (projectFile != null && projectFile.isValid && 
            (projectFile.name.endsWith(".csproj") || 
             projectFile.name.endsWith(".fsproj") || 
             projectFile.name.endsWith(".vbproj"))) {
            
            val parserFacade = ProjectParserFacade(project)
            
            parserFacade.parseProjectAsync(projectFile, object : ProgressListener {
                override fun onProgress(percent: Int, message: String) {
                    // 更新进度UI
                    println("Progress: $percent% - $message")
                }
            }).thenAccept { projectModel ->
                onComplete(projectModel)
            }.exceptionally { e ->
                // 处理异步错误
                e.printStackTrace()
                onComplete(null)
                null
            }
        } else {
            onComplete(null)
        }
    }
}
```

### 6.2 与 ProjectManager 集成

以下是如何将项目文件解析器与 ProjectManager 集成的示例：

```kotlin
package com.intellij.csharp.omnisharp.project.manager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.csharp.omnisharp.project.model.*
import com.intellij.csharp.omnisharp.project.parsers.ProjectParserFacade
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList

class OmniSharpProjectManager(private val project: Project) : ProjectManager {
    private var currentSolution: SolutionModel? = null
    private val projectFileManager = OmniSharpProjectFileManager(project)
    private val projectListeners = CopyOnWriteArrayList<ProjectListener>()
    
    override fun openSolution(solutionFile: VirtualFile): CompletableFuture<SolutionModel> {
        return CompletableFuture.supplyAsync { ->
            try {
                notifyProjectLoading()
                
                // 使用解决方案解析器解析解决方案文件
                val solutionParser = SolutionParserFacade(project)
                val solution = solutionParser.parseSolution(solutionFile)
                currentSolution = solution
                
                // 加载所有项目
                loadAllProjects(solution)
                
                // 通知监听器解决方案已打开
                notifySolutionOpened(solution)
                
                return@supplyAsync solution
            } catch (e: Exception) {
                notifyProjectLoadFailed(e)
                throw e
            }
        }
    }
    
    private fun loadAllProjects(solution: SolutionModel) {
        // 并行加载所有项目
        val loadFutures = solution.getAllProjects().map {
            projectModel ->
            val projectFile = VirtualFileManager.getInstance().findFileByPath(projectModel.path)
            if (projectFile != null) {
                projectFileManager.loadProject(projectFile)
            } else {
                CompletableFuture.completedFuture(projectModel)
            }
        }
        
        // 等待所有项目加载完成
        loadFutures.forEach { it.join() }
    }
    
    override fun getSolution(): SolutionModel? {
        return currentSolution
    }
    
    override fun getProject(projectId: String): ProjectModel? {
        // 首先尝试从解决方案中获取
        currentSolution?.getProject(projectId)?.let { solutionProject ->
            // 然后尝试从项目文件管理器中获取更详细的信息
            return projectFileManager.getProject(solutionProject.path) ?: solutionProject
        }
        
        // 或者直接从项目文件管理器中查找
        return projectFileManager.getAllProjects().find { it.id == projectId }
    }
    
    override fun refreshProject(projectId: String): CompletableFuture<ProjectModel> {
        val projectModel = getProject(projectId)
        if (projectModel == null) {
            return CompletableFuture.failedFuture(IllegalArgumentException("Project not found: $projectId"))
        }
        
        // 重新加载项目文件
        val projectFile = VirtualFileManager.getInstance().findFileByPath(projectModel.path)
        if (projectFile == null) {
            return CompletableFuture.failedFuture(IllegalArgumentException("Project file not found: ${projectModel.path}"))
        }
        
        return projectFileManager.reloadProject(projectFile).thenApply {
            updatedModel ->
            // 通知监听器项目已更新
            notifyProjectUpdated(updatedModel)
            updatedModel
        }
    }
    
    override fun closeSolution() {
        val solution = currentSolution
        if (solution != null) {
            currentSolution = null
            notifySolutionClosed()
        }
        projectFileManager.close()
    }
    
    override fun addProjectChangeListener(listener: ProjectChangeListener) {
        projectListeners.add(listener)
    }
    
    override fun removeProjectChangeListener(listener: ProjectChangeListener) {
        projectListeners.remove(listener)
    }
    
    private fun notifyProjectLoading() {
        projectListeners.forEach { it.onProjectLoading() }
    }
    
    private fun notifySolutionOpened(solution: SolutionModel) {
        projectListeners.forEach { it.onSolutionOpened(solution) }
    }
    
    private fun notifyProjectUpdated(projectModel: ProjectModel) {
        projectListeners.forEach { it.onProjectUpdated(projectModel) }
    }
    
    private fun notifySolutionClosed() {
        projectListeners.forEach { it.onSolutionClosed() }
    }
    
    private fun notifyProjectLoadFailed(e: Exception) {
        projectListeners.forEach { it.onProjectLoadFailed(e) }
    }
}
```

## 7. 性能优化

### 7.1 并行项目加载

对于包含多个项目的解决方案，可以并行加载项目文件：

```kotlin
// 在ProjectManager中实现并行加载
private fun loadProjectsInParallel(solution: SolutionModel): CompletableFuture<Void> {
    val projectFiles = solution.getAllProjects().mapNotNull {
        projectModel ->
        VirtualFileManager.getInstance().findFileByPath(projectModel.path)
    }
    
    val loadFutures = projectFiles.map {
        projectFile ->
        projectFileManager.loadProject(projectFile)
    }
    
    return CompletableFuture.allOf(*loadFutures.toTypedArray())
}
```

### 7.2 增量更新

当项目文件发生变化时，只更新发生变化的部分，而不是重新解析整个文件：

```kotlin
// 在ProjectFileWatcher中实现增量更新
private fun handleIncrementalUpdate(projectFile: VirtualFile, projectModel: ProjectModel) {
    // 比较文件内容变更
    val oldContent = lastKnownContent[projectFile.path]
    val newContent = projectFile.contentsToByteArray().toString(Charsets.UTF_8)
    
    if (oldContent != null) {
        // 分析变更
        val changes = analyzeChanges(oldContent, newContent)
        
        // 只更新变更的部分
        if (changes.hasPropertyChanges()) {
            updateProjectProperties(projectModel, newContent)
        }
        
        if (changes.hasReferenceChanges()) {
            updateProjectReferences(projectModel, newContent)
        }
        
        if (changes.hasFileChanges()) {
            updateProjectFiles(projectModel, newContent)
        }
    } else {
        // 首次加载，全量解析
        val parser = ProjectParserFacade(project)
        parser.reparseProject(projectFile, projectModel)
    }
    
    // 保存新内容
    lastKnownContent[projectFile.path] = newContent
}
```

### 7.3 缓存优化

优化项目模型缓存，减少内存使用：

```kotlin
// 实现LRU缓存
class LRUProjectCache<K, V>(private val capacity: Int) {
    private val cache = LinkedHashMap<K, V>(capacity, 0.75f, true)
    
    @Synchronized
    fun get(key: K): V? {
        return cache[key]
    }
    
    @Synchronized
    fun put(key: K, value: V) {
        if (cache.size >= capacity && !cache.containsKey(key)) {
            // 移除最久未使用的项
            val oldestKey = cache.keys.iterator().next()
            cache.remove(oldestKey)
        }
        cache[key] = value
    }
    
    @Synchronized
    fun remove(key: K) {
        cache.remove(key)
    }
    
    @Synchronized
    fun clear() {
        cache.clear()
    }
    
    @Synchronized
    fun size(): Int {
        return cache.size
    }
}

// 在ProjectFileManager中使用LRU缓存
class OptimizedProjectFileManager(private val project: Project, maxProjects: Int = 50) {
    private val projectCache = LRUProjectCache<String, ProjectModel>(maxProjects)
    // 其他实现...
}
```

## 8. 错误处理

### 8.1 健壮的异常处理

为项目文件解析添加全面的异常处理：

```kotlin
class ProjectParserException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ProjectFileFormatException(message: String) : ProjectParserException(message)

class ProjectFileNotFoundException(projectPath: String) : ProjectParserException("Project file not found: $projectPath")

class XmlParsingException(message: String, cause: Throwable) : ProjectParserException(message, cause)

// 在解析器中添加异常处理
try {
    // 解析XML
    val document = parseXml(content)
    // 处理文档...
} catch (e: JDOMException) {
    throw XmlParsingException("Failed to parse project file XML", e)
} catch (e: IOException) {
    throw ProjectParserException("Failed to read project file", e)
} catch (e: Exception) {
    throw ProjectParserException("Unexpected error parsing project file", e)
}
```

### 8.2 日志记录

添加详细的日志记录，帮助排查问题：

```kotlin
import com.intellij.openapi.diagnostic.Logger

class SdkStyleProjectParser {
    private val logger = Logger.getInstance(SdkStyleProjectParser::class.java)
    
    fun parse(projectFile: VirtualFile, content: String, baseProjectModel: ProjectModel?): ProjectModel {
        logger.info("Starting to parse SDK style project file: ${projectFile.path}")
        
        try {
            val startTime = System.currentTimeMillis()
            
            val document = parseXml(content)
            logger.debug("XML parsing completed for project: ${projectFile.name}")
            
            // 解析各种配置...
            
            val projectModel = buildProjectModel()
            
            val endTime = System.currentTimeMillis()
            logger.info("Project parsing completed in ${endTime - startTime}ms: ${projectModel.name}")
            logger.debug("Found ${projectModel.projectFiles.size} files, ${projectModel.projectReferences.size} project references")
            
            return projectModel
        } catch (e: Exception) {
            logger.error("Error parsing project file: ${projectFile.path}", e)
            throw e
        }
    }
}
```

## 9. 测试策略

### 9.1 单元测试

为项目文件解析器编写单元测试：

```kotlin
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SdkStyleProjectParserTest : BasePlatformTestCase() {
    @Test
    fun testParseSimpleSdkProject() {
        val testProjectContent = """
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <OutputType>Exe</OutputType>
            <TargetFramework>net6.0</TargetFramework>
            <AssemblyName>TestProject</AssemblyName>
            <RootNamespace>TestProject</RootNamespace>
          </PropertyGroup>
          
          <ItemGroup>
            <PackageReference Include="Newtonsoft.Json" Version="13.0.1" />
          </ItemGroup>
          
          <ItemGroup>
            <ProjectReference Include="..\LibraryProject\LibraryProject.csproj" />
          </ItemGroup>
        </Project>
        """.trimIndent()
        
        // 创建虚拟文件
        val virtualFile = createVirtualFile("TestProject.csproj", testProjectContent)
        
        val parser = SdkStyleProjectParser()
        val projectModel = parser.parse(virtualFile, testProjectContent)
        
        // 验证项目基本信息
        assertEquals("TestProject", projectModel.name)
        assertEquals("net6.0", projectModel.targetFramework)
        assertEquals("Exe", projectModel.outputType)
        assertEquals("TestProject", projectModel.assemblyName)
        assertTrue(projectModel.isSdkProject)
        
        // 验证包引用
        assertEquals(1, projectModel.packageReferences.size)
        val packageRef = projectModel.packageReferences[0]
        assertEquals("Newtonsoft.Json", packageRef.id)
        assertEquals("13.0.1", packageRef.version)
        
        // 验证项目引用
        assertEquals(1, projectModel.projectReferences.size)
    }
    
    @Test
    fun testParseMultiTargetProject() {
        val testProjectContent = """
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFrameworks>net6.0;netstandard2.0</TargetFrameworks>
          </PropertyGroup>
        </Project>
        """.trimIndent()
        
        val virtualFile = createVirtualFile("MultiTargetProject.csproj", testProjectContent)
        
        val parser = SdkStyleProjectParser()
        val projectModel = parser.parse(virtualFile, testProjectContent)
        
        // 验证多目标框架
        assertEquals("net6.0", projectModel.targetFramework) // 默认使用第一个框架
        assertEquals(2, projectModel.targetFrameworks.size)
        assertTrue(projectModel.targetFrameworks.contains("net6.0"))
        assertTrue(projectModel.targetFrameworks.contains("netstandard2.0"))
    }
    
    @Test(expected = XmlParsingException::class)
    fun testParseInvalidXml() {
        val invalidXml = "This is not valid XML"
        val virtualFile = createVirtualFile("InvalidProject.csproj", invalidXml)
        
        val parser = SdkStyleProjectParser()
        parser.parse(virtualFile, invalidXml)
    }
    
    private fun createVirtualFile(name: String, content: String): VirtualFile {
        // 在测试环境中创建虚拟文件
        // 具体实现依赖于测试框架
        // 这里仅作为示例
        return TestVirtualFile(name, content)
    }
}
```

### 9.2 集成测试

测试项目解析器与其他组件的集成：

```kotlin
import com.intellij.openapi.project.Project
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ProjectManagerIntegrationTest : BasePlatformTestCase() {
    @Test
    fun testLoadSolutionWithProjects() {
        // 创建测试解决方案和项目文件
        val solutionContent = """
        Microsoft Visual Studio Solution File, Format Version 12.00
        Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "MainProject", "MainProject\MainProject.csproj", "{12345678-1234-1234-1234-123456789012}"
        EndProject
        Project("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}") = "LibraryProject", "LibraryProject\LibraryProject.csproj", "{87654321-4321-4321-4321-210987654321}"
        EndProject
        Global
            GlobalSection(SolutionConfigurationPlatforms) = preSolution
                Debug|Any CPU = Debug|Any CPU
            EndGlobalSection
            GlobalSection(ProjectConfigurationPlatforms) = postSolution
                {12345678-1234-1234-1234-123456789012}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                {12345678-1234-1234-1234-123456789012}.Debug|Any CPU.Build.0 = Debug|Any CPU
                {87654321-4321-4321-4321-210987654321}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                {87654321-4321-4321-4321-210987654321}.Debug|Any CPU.Build.0 = Debug|Any CPU
            EndGlobalSection
        EndGlobal
        """.trimIndent()
        
        val mainProjectContent = """
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <OutputType>Exe</OutputType>
            <TargetFramework>net6.0</TargetFramework>
          </PropertyGroup>
          <ItemGroup>
            <ProjectReference Include="..\LibraryProject\LibraryProject.csproj" />
          </ItemGroup>
        </Project>
        """.trimIndent()
        
        val libraryProjectContent = """
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFramework>net6.0</TargetFramework>
          </PropertyGroup>
        </Project>
        """.trimIndent()
        
        // 创建测试文件结构
        val solutionFile = createVirtualFile("TestSolution.sln", solutionContent)
        val mainProjectFile = createVirtualFile("MainProject\MainProject.csproj", mainProjectContent)
        val libraryProjectFile = createVirtualFile("LibraryProject\LibraryProject.csproj", libraryProjectContent)
        
        // 创建并初始化ProjectManager
        val projectManager = OmniSharpProjectManager(project)
        
        // 加载解决方案
        val solutionModel = projectManager.openSolution(solutionFile).get()
        
        // 验证解决方案信息
        assertNotNull(solutionModel)
        assertEquals(2, solutionModel.getAllProjects().size)
        
        // 验证项目信息
        val mainProject = projectManager.getProject("{12345678-1234-1234-1234-123456789012}")
        assertNotNull(mainProject)
        assertEquals("MainProject", mainProject.name)
        
        val libraryProject = projectManager.getProject("{87654321-4321-4321-4321-210987654321}")
        assertNotNull(libraryProject)
        assertEquals("LibraryProject", libraryProject.name)
        
        // 验证项目引用
        assertEquals(1, mainProject.projectReferences.size)
    }
    
    // 清理资源
    override fun tearDown() {
        // 确保清理所有创建的测试文件
        super.tearDown()
    }
}
```

### 9.3 性能测试

为项目文件解析器编写性能测试：

```kotlin
import com.intellij.openapi.vfs.VirtualFile
import org.junit.Test
import kotlin.system.measureTimeMillis

class ProjectParserPerformanceTest {
    @Test
    fun testParseLargeProjectFilePerformance() {
        // 创建一个大型项目文件（包含大量文件引用和包引用）
        val largeProjectContent = generateLargeProjectContent()
        val virtualFile = createVirtualFile("LargeProject.csproj", largeProjectContent)
        
        val parser = SdkStyleProjectParser()
        
        // 测量解析时间
        val timeTaken = measureTimeMillis {
            val projectModel = parser.parse(virtualFile, largeProjectContent)
            // 验证解析结果
            assertEquals(100, projectModel.packageReferences.size)
            assertEquals(1000, projectModel.projectFiles.size)
        }
        
        // 确保解析时间在可接受范围内
        println("Parsing large project took $timeTaken ms")
        assertTrue(timeTaken < 1000) // 确保在1秒内完成解析
    }
    
    @Test
    fun testParseMultipleProjectsPerformance() {
        // 创建多个项目文件
        val projectCount = 10
        val projects = mutableListOf<Pair<VirtualFile, String>>()
        
        for (i in 1..projectCount) {
            val projectContent = """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net6.0</TargetFramework>
                <AssemblyName>Project$i</AssemblyName>
              </PropertyGroup>
            </Project>
            """.trimIndent()
            
            val virtualFile = createVirtualFile("Project$i.csproj", projectContent)
            projects.add(Pair(virtualFile, projectContent))
        }
        
        val parser = SdkStyleProjectParser()
        
        // 测量解析时间
        val timeTaken = measureTimeMillis {
            for ((file, content) in projects) {
                parser.parse(file, content)
            }
        }
        
        println("Parsing $projectCount projects took $timeTaken ms")
        assertTrue(timeTaken < 2000) // 确保在2秒内完成10个项目的解析
    }
    
    private fun generateLargeProjectContent(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("""
        <Project Sdk="Microsoft.NET.Sdk">
          <PropertyGroup>
            <TargetFramework>net6.0</TargetFramework>
          </PropertyGroup>
        """.trimIndent())
        
        // 添加100个包引用
        stringBuilder.append("  <ItemGroup>\n")
        for (i in 1..100) {
            stringBuilder.append("    <PackageReference Include=\"Package$i\" Version=\"1.0.$i\" />\n")
        }
        stringBuilder.append("  </ItemGroup>\n")
        
        // 添加1000个文件引用
        stringBuilder.append("  <ItemGroup>\n")
        for (i in 1..1000) {
            stringBuilder.append("    <Compile Include=\"File$i.cs\" />\n")
        }
        stringBuilder.append("  </ItemGroup>\n")
        
        stringBuilder.append("</Project>")
        return stringBuilder.toString()
    }
    
    private fun createVirtualFile(name: String, content: String): VirtualFile {
        // 创建虚拟文件的实现
        return TestVirtualFile(name, content)
    }
}
```

## 10. 总结与后续优化

### 10.1 已实现功能

- 支持SDK风格和传统风格的项目文件解析
- 提取项目基本信息（名称、程序集名称、目标框架等）
- 解析项目引用、包引用和文件引用
- 提取编译文件列表
- 项目文件变更监控和自动更新
- 提供丰富的项目模型查询和管理接口

### 10.2 后续优化方向

1. **支持更多项目文件格式**：
   - 增加对 .fsproj 和 .vbproj 文件的特定解析支持
   - 支持 Directory.Build.props 和 Directory.Build.targets 文件

2. **增强性能**：
   - 实现更高效的 XML 解析算法
   - 引入更智能的增量更新机制
   - 添加更精细的缓存策略

3. **功能扩展**：
   - 添加项目文件编辑和生成支持
   - 实现项目文件格式化功能
   - 支持条件编译和配置转换

4. **错误处理和恢复**：
   - 增加更详细的错误诊断和修复建议
   - 实现解析失败的优雅降级策略

5. **与 IntelliJ 平台深度集成**：
   - 提供项目结构可视化组件
   - 集成到 IntelliJ 的项目视图和导航系统
   - 支持与 IntelliJ 的代码分析和重构工具协作

通过这些优化，项目文件解析和管理功能将更加稳定、高效，为 IntelliJ 平台上的 .NET 开发提供更好的支持。

### 10.3 输入输出示例

#### 输入输出示例

输入：
```kotlin
// 创建解析器
val parserFacade = ProjectParserFacade(project)

// 加载项目文件
val projectFile = LocalFileSystem.getInstance().findFileByPath("C:\\Projects\\MyApp\\MyApp.csproj")
val projectModel = parserFacade.parseProject(projectFile)

// 输出项目信息
println("Project: ${projectModel.name}")
println("Target Framework: ${projectModel.targetFramework}")
println("Assembly Name: ${projectModel.assemblyName}")
println("Output Path: ${projectModel.outputPath}")
println("Number of files: ${projectModel.projectFiles.size}")
println("Number of references: ${projectModel.fileReferences.size}")
```

输出：
```
Project: MyApp
Target Framework: net6.0
Assembly Name: MyApp
Output Path: bin/Debug/net6.0/
Number of files: 25
Number of references: 12
```

这个实现提供了一个全面的项目文件解析和管理系统，支持各种类型的 .NET 项目文件，并提供了丰富的查询和管理接口。通过与 IntelliJ 平台的集成，可以为 .NET 开发人员提供更好的开发体验。