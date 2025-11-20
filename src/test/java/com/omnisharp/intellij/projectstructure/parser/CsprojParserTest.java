package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.Project;
import com.omnisharp.intellij.projectstructure.model.ProjectReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSPROJ文件解析器测试类
 */
class CsprojParserTest {
    
    private CsprojParser parser;
    private static final String TEST_PROJECT_NAME = "TestProject";
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        parser = new CsprojParser();
    }
    
    /**
     * 创建一个简单的CSPROJ文件用于测试
     */
    private Path createSimpleCsprojFile() throws IOException {
        Path csprojPath = tempDir.resolve("TestProject.csproj");
        String csprojContent = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
                "<Project ToolsVersion=\"12.0\" DefaultTargets=\"Build\" xmlns=\"http://schemas.microsoft.com/developer/msbuild/2003\">" +
                "  <PropertyGroup>" +
                "    <Configuration Condition=\" '$(Configuration)' == '' \"Debug</Configuration>" +
                "    <Platform Condition=\" '$(Platform)' == '' \"x86</Platform>" +
                "    <ProjectGuid>{12345678-1234-1234-1234-123456789012}</ProjectGuid>" +
                "    <OutputType>Library</OutputType>" +
                "    <AppDesignerFolder>Properties</AppDesignerFolder>" +
                "    <RootNamespace>TestProject</RootNamespace>" +
                "    <AssemblyName>TestProject</AssemblyName>" +
                "    <TargetFrameworkVersion>v4.5</TargetFrameworkVersion>" +
                "    <FileAlignment>512</FileAlignment>" +
                "  </PropertyGroup>" +
                "  <PropertyGroup Condition=\" '$(Configuration)|$(Platform)' == 'Debug|x86' \">" +
                "    <DebugSymbols>true</DebugSymbols>" +
                "    <DebugType>full</DebugType>" +
                "    <Optimize>false</Optimize>" +
                "    <OutputPath>bin\\Debug\\</OutputPath>" +
                "    <DefineConstants>DEBUG;TRACE</DefineConstants>" +
                "  </PropertyGroup>" +
                "  <PropertyGroup Condition=\" '$(Configuration)|$(Platform)' == 'Release|x86' \">" +
                "    <DebugType>pdbonly</DebugType>" +
                "    <Optimize>true</Optimize>" +
                "    <OutputPath>bin\\Release\\</OutputPath>" +
                "    <DefineConstants>TRACE</DefineConstants>" +
                "  </PropertyGroup>" +
                "  <ItemGroup>" +
                "    <Reference Include=\"System\" />" +
                "    <Reference Include=\"System.Core\" />" +
                "    <Reference Include=\"System.Xml.Linq\" />" +
                "    <Reference Include=\"System.Data.DataSetExtensions\" />" +
                "    <Reference Include=\"Microsoft.CSharp\" />" +
                "    <Reference Include=\"System.Data\" />" +
                "    <Reference Include=\"System.Xml\" />" +
                "  </ItemGroup>" +
                "  <ItemGroup>" +
                "    <Compile Include=\"Program.cs\" />" +
                "    <Compile Include=\"Properties\\AssemblyInfo.cs\" />" +
                "  </ItemGroup>" +
                "  <ItemGroup>" +
                "    <None Include=\"App.config\" />" +
                "  </ItemGroup>" +
                "  <ItemGroup>" +
                "    <ProjectReference Include=\"..\\AnotherProject\\AnotherProject.csproj\">" +
                "      <Project>{87654321-4321-4321-4321-210987654321}</Project>" +
                "      <Name>AnotherProject</Name>" +
                "    </ProjectReference>" +
                "  </ItemGroup>" +
                "  <Import Project=\"$(MSBuildToolsPath)\\Microsoft.CSharp.targets\" />" +
                "</Project>";
        
        Files.writeString(csprojPath, csprojContent);
        return csprojPath;
    }
    
    /**
     * 测试解析项目文件
     */
    @Test
    void testParseProject() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        Project project = parser.parse(csprojPath.toString());
        
        assertNotNull(project, "Parsed project should not be null");
        assertEquals(TEST_PROJECT_NAME, project.getName(), "Project name should match");
        assertEquals(csprojPath.toString(), project.getFilePath(), "Project path should match");
        assertEquals("Library", project.getOutputType(), "Output type should be Library");
        assertEquals("v4.5", project.getTargetFrameworkVersion(), "Target framework should be v4.5");
        assertEquals("{12345678-1234-1234-1234-123456789012}", project.getProjectGuid(), "Project GUID should match");
    }
    
    /**
     * 测试解析项目属性
     */
    @Test
    void testParseProjectProperties() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        Map<String, String> properties = parser.parseProjectProperties(csprojPath.toString());
        
        assertNotNull(properties, "Properties map should not be null");
        assertTrue(properties.containsKey("Configuration"), "Should contain Configuration property");
        assertTrue(properties.containsKey("Platform"), "Should contain Platform property");
        assertTrue(properties.containsKey("OutputType"), "Should contain OutputType property");
        assertTrue(properties.containsKey("TargetFrameworkVersion"), "Should contain TargetFrameworkVersion property");
        
        assertEquals("Library", properties.get("OutputType"), "OutputType should be Library");
        assertEquals("v4.5", properties.get("TargetFrameworkVersion"), "Target framework should be v4.5");
    }
    
    /**
     * 测试解析项目配置
     */
    @Test
    void testParseProjectConfigurations() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        Map<String, Map<String, String>> configurations = parser.parseProjectConfigurations(csprojPath.toString());
        
        assertNotNull(configurations, "Configurations map should not be null");
        assertTrue(configurations.size() >= 2, "Should have at least 2 configurations");
        assertTrue(configurations.containsKey("Debug|x86"), "Should contain Debug|x86 configuration");
        assertTrue(configurations.containsKey("Release|x86"), "Should contain Release|x86 configuration");
        
        // 验证Debug配置的属性
        Map<String, String> debugConfig = configurations.get("Debug|x86");
        assertNotNull(debugConfig, "Debug configuration should not be null");
        assertEquals("true", debugConfig.get("DebugSymbols"), "DebugSymbols should be true");
        assertEquals("full", debugConfig.get("DebugType"), "DebugType should be full");
        assertEquals("false", debugConfig.get("Optimize"), "Optimize should be false");
        
        // 验证Release配置的属性
        Map<String, String> releaseConfig = configurations.get("Release|x86");
        assertNotNull(releaseConfig, "Release configuration should not be null");
        assertEquals("pdbonly", releaseConfig.get("DebugType"), "DebugType should be pdbonly");
        assertEquals("true", releaseConfig.get("Optimize"), "Optimize should be true");
    }
    
    /**
     * 测试解析项目引用
     */
    @Test
    void testParseProjectReferences() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        List<ProjectReference> projectReferences = parser.parseProjectReferences(csprojPath.toString());
        
        assertNotNull(projectReferences, "Project references should not be null");
        assertEquals(1, projectReferences.size(), "Should have exactly 1 project reference");
        
        ProjectReference ref = projectReferences.get(0);
        assertEquals("AnotherProject", ref.getName(), "Reference name should match");
        assertEquals("..\\AnotherProject\\AnotherProject.csproj", ref.getPath(), "Reference path should match");
        assertEquals("{87654321-4321-4321-4321-210987654321}", ref.getProjectGuid(), "Reference GUID should match");
    }
    
    /**
     * 测试解析编译文件
     */
    @Test
    void testParseCompileFiles() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        List<String> compileFiles = parser.parseCompileFiles(csprojPath.toString());
        
        assertNotNull(compileFiles, "Compile files list should not be null");
        assertEquals(2, compileFiles.size(), "Should have exactly 2 compile files");
        assertTrue(compileFiles.contains("Program.cs"), "Should contain Program.cs");
        assertTrue(compileFiles.contains("Properties\\AssemblyInfo.cs"), "Should contain Properties\\AssemblyInfo.cs");
    }
    
    /**
     * 测试解析程序集引用
     */
    @Test
    void testParseAssemblyReferences() throws IOException {
        Path csprojPath = createSimpleCsprojFile();
        
        List<String> references = parser.parseAssemblyReferences(csprojPath.toString());
        
        assertNotNull(references, "Assembly references should not be null");
        assertTrue(references.size() >= 7, "Should have at least 7 assembly references");
        assertTrue(references.contains("System"), "Should contain System reference");
        assertTrue(references.contains("System.Core"), "Should contain System.Core reference");
        assertTrue(references.contains("System.Xml"), "Should contain System.Xml reference");
    }
    
    /**
     * 测试解析不存在的文件
     */
    @Test
    void testParseNonExistentFile() {
        String nonExistentPath = tempDir.resolve("NonExistent.csproj").toString();
        
        assertThrows(RuntimeException.class, () -> {
            parser.parse(nonExistentPath);
        }, "Should throw exception when parsing non-existent file");
    }
    
    /**
     * 测试解析无效的CSPROJ文件
     */
    @Test
    void testParseInvalidCsprojFile() throws IOException {
        Path invalidCsprojPath = tempDir.resolve("Invalid.csproj");
        Files.writeString(invalidCsprojPath, "This is not a valid CSPROJ file content");
        
        // 尝试解析无效文件
        Project project = parser.parse(invalidCsprojPath.toString());
        
        // 即使文件无效，也应该返回项目对象，但可能缺少某些信息
        assertNotNull(project, "Parser should return project even for invalid files");
        assertEquals("Invalid", project.getName(), "Project name should be based on filename");
    }
}