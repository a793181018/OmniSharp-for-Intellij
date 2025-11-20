package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectReference;
import com.omnisharp.intellij.projectstructure.model.Solution;
import com.omnisharp.intellij.projectstructure.model.SolutionConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解决方案文件解析器测试类
 */
class SlnParserTest {
    
    private SlnParser parser;
    private static final String TEST_SOLUTION_NAME = "TestSolution";
    private static final String TEST_SOLUTION_FORMAT = "Microsoft Visual Studio Solution File, Format Version 12.00\n# Visual Studio 2013\n";
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        parser = new SlnParser();
    }
    
    /**
     * 创建一个简单的SLN文件用于测试
     */
    private Path createSimpleSlnFile() throws IOException {
        Path slnPath = tempDir.resolve("TestSolution.sln");
        String slnContent = TEST_SOLUTION_FORMAT +
                "Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject\", \"TestProject/TestProject.csproj\", \"{12345678-1234-1234-1234-123456789012}\"\n" +
                "EndProject\n" +
                "Global\n" +
                "\tGlobalSection(SolutionConfigurationPlatforms) = preSolution\n" +
                "\t\tDebug|x86 = Debug|x86\n" +
                "\t\tRelease|x86 = Release|x86\n" +
                "\tEndGlobalSection\n" +
                "\tGlobalSection(ProjectConfigurationPlatforms) = postSolution\n" +
                "\t\t{12345678-1234-1234-1234-123456789012}.Debug|x86.ActiveCfg = Debug|x86\n" +
                "\t\t{12345678-1234-1234-1234-123456789012}.Debug|x86.Build.0 = Debug|x86\n" +
                "\t\t{12345678-1234-1234-1234-123456789012}.Release|x86.ActiveCfg = Release|x86\n" +
                "\t\t{12345678-1234-1234-1234-123456789012}.Release|x86.Build.0 = Release|x86\n" +
                "\tEndGlobalSection\n" +
                "EndGlobal\n";
        
        Files.writeString(slnPath, slnContent);
        return slnPath;
    }
    
    /**
     * 测试解析解决方案文件
     */
    @Test
    void testParseSolution() throws IOException {
        Path slnPath = createSimpleSlnFile();
        
        Solution solution = parser.parse(slnPath.toString());
        
        assertNotNull(solution, "Parsed solution should not be null");
        assertEquals(TEST_SOLUTION_NAME, solution.getName(), "Solution name should match");
        assertEquals(slnPath.toString(), solution.getPath(), "Solution path should match");
    }
    
    /**
     * 测试解析解决方案版本
     */
    @Test
    void testParseSolutionVersion() throws IOException {
        Path slnPath = createSimpleSlnFile();
        
        String version = parser.parseSolutionVersion(slnPath.toString());
        
        assertNotNull(version, "Solution version should not be null");
        assertTrue(version.contains("12.00"), "Version should contain expected format");
    }
    
    /**
     * 测试解析解决方案配置
     */
    @Test
    void testParseSolutionConfigurations() throws IOException {
        Path slnPath = createSimpleSlnFile();
        
        Map<String, SolutionConfiguration> configurations = parser.parseSolutionConfigurations(slnPath.toString());
        
        assertNotNull(configurations, "Configurations map should not be null");
        assertTrue(configurations.size() >= 2, "Should have at least 2 configurations");
        assertTrue(configurations.containsKey("Debug|x86"), "Should contain Debug|x86 configuration");
        assertTrue(configurations.containsKey("Release|x86"), "Should contain Release|x86 configuration");
    }
    
    /**
     * 测试解析项目引用
     */
    @Test
    void testParseProjectReferences() throws IOException {
        Path slnPath = createSimpleSlnFile();
        
        List<ProjectReference> projectReferences = parser.parseProjectReferences(slnPath.toString());
        
        assertNotNull(projectReferences, "Project references should not be null");
        assertEquals(1, projectReferences.size(), "Should have exactly 1 project reference");
        
        ProjectReference projectRef = projectReferences.get(0);
        assertEquals("TestProject", projectRef.getName(), "Project name should match");
        assertEquals("TestProject\\TestProject.csproj", projectRef.getPath(), "Project path should match");
        assertEquals("{12345678-1234-1234-1234-123456789012}", projectRef.getProjectGuid(), "Project GUID should match");
    }
    
    /**
     * 测试解析不存在的文件
     */
    @Test
    void testParseNonExistentFile() {
        String nonExistentPath = tempDir.resolve("NonExistent.sln").toString();
        
        assertThrows(RuntimeException.class, () -> {
            parser.parse(nonExistentPath);
        }, "Should throw exception when parsing non-existent file");
    }
    
    /**
     * 测试解析无效的SLN文件
     */
    @Test
    void testParseInvalidSlnFile() throws IOException {
        Path invalidSlnPath = tempDir.resolve("Invalid.sln");
        Files.writeString(invalidSlnPath, "This is not a valid SLN file content");
        
        // 尝试解析无效文件
        Solution solution = parser.parse(invalidSlnPath.toString());
        
        // 即使文件无效，也应该返回解决方案对象，但可能缺少某些信息
        assertNotNull(solution, "Parser should return solution even for invalid files");
        assertEquals("Invalid", solution.getName(), "Solution name should be based on filename");
        // 但不应包含任何项目引用
        assertTrue(solution.getProjects().isEmpty(), "Invalid solution should have no projects");
    }
}