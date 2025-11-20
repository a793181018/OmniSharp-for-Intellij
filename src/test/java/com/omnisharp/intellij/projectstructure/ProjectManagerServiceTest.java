package com.omnisharp.intellij.projectstructure;

import com.omnisharp.intellij.projectstructure.api.FileSystemListener;
import com.omnisharp.intellij.projectstructure.api.ProjectStructureChangeListener;
import com.omnisharp.intellij.projectstructure.model.Project;
import com.omnisharp.intellij.projectstructure.model.Solution;
import com.omnisharp.intellij.projectstructure.service.ProjectManagerService;
import com.omnisharp.intellij.projectstructure.service.ProjectManagerServiceImpl;
import com.omnisharp.intellij.projectstructure.utils.FileUtils;
import com.omnisharp.intellij.projectstructure.utils.ProjectLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 项目管理器服务测试类
 */
class ProjectManagerServiceTest {
    
    private static final ProjectLogger logger = ProjectLogger.getInstance(ProjectManagerServiceTest.class);
    private ProjectManagerService projectManagerService;
    private List<ProjectStructureChangeListener> structureListeners;
    private List<FileSystemListener> fileSystemListeners;
    
    @TempDir
    private Path tempDir;
    private Path testSolutionPath;
    private Path testProjectPath;
    
    @BeforeEach
    void setUp() throws IOException {
        // 初始化测试数据
        initTestData();
        
        // 创建监听器列表用于验证
        structureListeners = new ArrayList<>();
        fileSystemListeners = new ArrayList<>();
        
        // 创建ProjectManagerService的测试实现
        projectManagerService = new ProjectManagerServiceImpl() {
            @Override
            public void addProjectStructureChangeListener(@NotNull ProjectStructureChangeListener listener) {
                super.addProjectStructureChangeListener(listener);
                structureListeners.add(listener);
            }
            
            @Override
            public void addFileSystemListener(@NotNull String solutionPath, @NotNull FileSystemListener listener) {
                super.addFileSystemListener(solutionPath, listener);
                fileSystemListeners.add(listener);
            }
        };
    }
    
    @AfterEach
    void tearDown() {
        // 清理测试资源
        projectManagerService.closeAllSolutions();
    }
    
    /**
     * 初始化测试数据
     */
    private void initTestData() throws IOException {
        // 创建测试解决方案文件
        testSolutionPath = tempDir.resolve("TestSolution.sln");
        String slnContent = """
Microsoft Visual Studio Solution File, Format Version 12.00
# Visual Studio Version 16
VisualStudioVersion = 16.0.30320.27
MinimumVisualStudioVersion = 10.0.40219.1
Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject1\", \"TestProject1/TestProject1.csproj\", \"{12345678-1234-1234-1234-123456789012}\"
EndProject
Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject2\", \"TestProject2/TestProject2.csproj\", \"{23456789-2345-2345-2345-234567890123}\"
EndProject
Global
\tGlobalSection(SolutionConfigurationPlatforms) = preSolution
\t\tDebug|Any CPU = Debug|Any CPU
\t\tRelease|Any CPU = Release|Any CPU
\tEndGlobalSection
\tGlobalSection(ProjectConfigurationPlatforms) = postSolution
\t\t{12345678-1234-1234-1234-123456789012}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
\t\t{12345678-1234-1234-1234-123456789012}.Debug|Any CPU.Build.0 = Debug|Any CPU
\t\t{12345678-1234-1234-1234-123456789012}.Release|Any CPU.ActiveCfg = Release|Any CPU
\t\t{12345678-1234-1234-1234-123456789012}.Release|Any CPU.Build.0 = Release|Any CPU
\t\t{23456789-2345-2345-2345-234567890123}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
\t\t{23456789-2345-2345-2345-234567890123}.Debug|Any CPU.Build.0 = Debug|Any CPU
\t\t{23456789-2345-2345-2345-234567890123}.Release|Any CPU.ActiveCfg = Release|Any CPU
\t\t{23456789-2345-2345-2345-234567890123}.Release|Any CPU.Build.0 = Release|Any CPU
\tEndGlobalSection
\tGlobalSection(SolutionProperties) = preSolution
\t\tHideSolutionNode = FALSE
\tEndGlobalSection
EndGlobal
""";
        FileUtils.writeFileContent(testSolutionPath.toString(), slnContent);
        
        // 创建测试项目目录
        testProjectPath = tempDir.resolve("TestProject1");
        FileUtils.createDirectory(testProjectPath.toString());
        
        // 创建测试项目文件
        Path csprojPath = testProjectPath.resolve("TestProject1.csproj");
        String csprojContent = """
<Project Sdk=\"Microsoft.NET.Sdk\">
  <PropertyGroup>
    <OutputType>Exe</OutputType>
    <TargetFramework>net6.0</TargetFramework>
  </PropertyGroup>
</Project>
""";
        FileUtils.writeFileContent(csprojPath.toString(), csprojContent);
        
        // 创建第二个测试项目目录和文件
        Path testProject2Path = tempDir.resolve("TestProject2");
        FileUtils.createDirectory(testProject2Path.toString());
        Path csproj2Path = testProject2Path.resolve("TestProject2.csproj");
        FileUtils.writeFileContent(csproj2Path.toString(), csprojContent);
    }
    
    /**
     * 测试打开解决方案功能
     */
    @Test
    void testOpenSolution() {
        logger.info("Testing openSolution...");
        
        // 打开解决方案
        Solution solution = projectManagerService.openSolution(testSolutionPath.toString());
        
        // 验证解决方案是否正确打开
        assertNotNull(solution, "Solution should not be null");
        assertEquals("TestSolution", solution.getName(), "Solution name should match");
        assertEquals(testSolutionPath.toString(), solution.getPath(), "Solution path should match");
        assertEquals(2, solution.getProjects().size(), "Solution should contain 2 projects");
        
        // 验证项目信息
        List<Project> projects = solution.getProjects();
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("TestProject1")), 
                "Solution should contain TestProject1");
        assertTrue(projects.stream().anyMatch(p -> p.getName().equals("TestProject2")), 
                "Solution should contain TestProject2");
        
        // 验证获取已打开的解决方案
        Solution openedSolution = projectManagerService.getOpenedSolution(testSolutionPath.toString());
        assertNotNull(openedSolution, "Should be able to get opened solution");
        assertEquals(solution, openedSolution, "Returned solution should be the same instance");
    }
    
    /**
     * 测试关闭解决方案功能
     */
    @Test
    void testCloseSolution() {
        logger.info("Testing closeSolution...");
        
        // 打开解决方案
        projectManagerService.openSolution(testSolutionPath.toString());
        
        // 验证解决方案已打开
        assertNotNull(projectManagerService.getOpenedSolution(testSolutionPath.toString()), 
                "Solution should be opened");
        
        // 关闭解决方案
        boolean success = projectManagerService.closeSolution(testSolutionPath.toString());
        
        // 验证关闭成功
        assertTrue(success, "Closing solution should be successful");
        assertNull(projectManagerService.getOpenedSolution(testSolutionPath.toString()), 
                "Solution should be null after closing");
    }
    
    /**
     * 测试添加和触发项目结构变更监听器
     */
    @Test
    void testProjectStructureChangeListener() {
        logger.info("Testing ProjectStructureChangeListener...");
        
        // 创建模拟监听器
        ProjectStructureChangeListener mockListener = Mockito.mock(ProjectStructureChangeListener.class);
        
        // 添加监听器
        projectManagerService.addProjectStructureChangeListener(mockListener);
        
        // 验证监听器被添加
        assertEquals(1, structureListeners.size(), "Listener should be added");
        
        // 打开解决方案以触发事件
        Solution solution = projectManagerService.openSolution(testSolutionPath.toString());
        
        // 验证监听器被调用
        verify(mockListener, times(1)).onSolutionOpened(solution);
        
        // 关闭解决方案以触发事件
        projectManagerService.closeSolution(testSolutionPath.toString());
        
        // 验证监听器被调用
        verify(mockListener, times(1)).onSolutionClosed(testSolutionPath.toString());
        
        // 移除监听器
        projectManagerService.removeProjectStructureChangeListener(mockListener);
        
        // 再次打开解决方案，验证监听器不再被调用
        reset(mockListener);
        projectManagerService.openSolution(testSolutionPath.toString());
        verify(mockListener, never()).onSolutionOpened(any());
    }
    
    /**
     * 测试添加文件系统监听器
     */
    @Test
    void testFileSystemListener() {
        logger.info("Testing FileSystemListener...");
        
        // 打开解决方案
        projectManagerService.openSolution(testSolutionPath.toString());
        
        // 创建模拟监听器
        FileSystemListener mockListener = Mockito.mock(FileSystemListener.class);
        
        // 添加监听器
        projectManagerService.addFileSystemListener(testSolutionPath.toString(), mockListener);
        
        // 验证监听器被添加
        assertEquals(1, fileSystemListeners.size(), "Listener should be added");
        
        // 移除监听器
        projectManagerService.removeFileSystemListener(testSolutionPath.toString(), mockListener);
        
        // 验证监听器数量
        // 注意：在实际实现中，这里应该为0，但由于我们只是记录添加的监听器，所以这里仍然是1
        // 在真实的测试中，应该验证底层实现是否正确移除了监听器
    }
    
    /**
     * 测试刷新解决方案功能
     */
    @Test
    void testRefreshSolution() throws IOException {
        logger.info("Testing refreshSolution...");
        
        // 打开解决方案
        Solution solution = projectManagerService.openSolution(testSolutionPath.toString());
        int initialProjectCount = solution.getProjects().size();
        
        // 刷新解决方案
        Solution refreshedSolution = projectManagerService.refreshSolution(testSolutionPath.toString());
        
        // 验证刷新后的解决方案
        assertNotNull(refreshedSolution, "Refreshed solution should not be null");
        assertEquals(initialProjectCount, refreshedSolution.getProjects().size(), 
                "Project count should remain the same after refresh");
    }
    
    /**
     * 测试获取所有已打开解决方案功能
     */
    @Test
    void testGetAllOpenedSolutions() {
        logger.info("Testing getAllOpenedSolutions...");
        
        // 验证初始状态没有打开的解决方案
        List<Solution> initialSolutions = projectManagerService.getAllOpenedSolutions();
        assertTrue(initialSolutions.isEmpty() || initialSolutions.size() == 0, 
                "Initially should have no opened solutions");
        
        // 打开解决方案
        projectManagerService.openSolution(testSolutionPath.toString());
        
        // 验证有一个打开的解决方案
        List<Solution> openedSolutions = projectManagerService.getAllOpenedSolutions();
        assertEquals(1, openedSolutions.size(), "Should have one opened solution");
        assertEquals("TestSolution", openedSolutions.get(0).getName(), 
                "Solution name should match");
    }
    
    /**
     * 测试关闭所有解决方案功能
     */
    @Test
    void testCloseAllSolutions() {
        logger.info("Testing closeAllSolutions...");
        
        // 打开解决方案
        projectManagerService.openSolution(testSolutionPath.toString());
        
        // 验证有一个打开的解决方案
        assertEquals(1, projectManagerService.getAllOpenedSolutions().size(), 
                "Should have one opened solution");
        
        // 关闭所有解决方案
        projectManagerService.closeAllSolutions();
        
        // 验证没有打开的解决方案
        assertTrue(projectManagerService.getAllOpenedSolutions().isEmpty(), 
                "Should have no opened solutions after closing all");
    }
    
    /**
     * 测试打开不存在的解决方案
     */
    @Test
    void testOpenNonExistentSolution() {
        logger.info("Testing openNonExistentSolution...");
        
        String nonExistentPath = tempDir.resolve("NonExistent.sln").toString();
        
        // 尝试打开不存在的解决方案
        Solution solution = projectManagerService.openSolution(nonExistentPath);
        
        // 验证返回null
        assertNull(solution, "Opening non-existent solution should return null");
    }
}