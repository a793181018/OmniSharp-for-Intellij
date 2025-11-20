package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解决方案文件解析器测试类
 * 测试解析器的基本功能和缓存机制
 */
class SolutionParserTest {

    @TempDir
    Path tempDir;

    private SolutionParserFacade defaultParser;
    private CachingSolutionParserFacade cachingParser;
    private Path testSolutionFile;

    @BeforeEach
    void setUp() throws IOException {
        // 创建测试解析器
        defaultParser = new DefaultSolutionParserFacade();
        cachingParser = new CachingSolutionParserFacade(5, 5000); // 小缓存大小，短缓存时间，方便测试

        // 创建测试解决方案文件
        testSolutionFile = createTestSolutionFile();
    }

    @AfterEach
    void tearDown() {
        // 关闭解析器
        defaultParser.shutdown();
        cachingParser.shutdown();
    }

    /**
     * 创建测试用的解决方案文件
     */
    private Path createTestSolutionFile() throws IOException {
        Path solutionPath = tempDir.resolve("TestSolution.sln");
        
        String solutionContent = """
Microsoft Visual Studio Solution File, Format Version 12.00
# Visual Studio Version 16
VisualStudioVersion = 16.0.28701.123
MinimumVisualStudioVersion = 10.0.40219.1
Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject1\", \"TestProject1\\TestProject1.csproj\", \"{11111111-1111-1111-1111-111111111111}\"
EndProject
Project(\"{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}\") = \"TestProject2\", \"TestProject2\\TestProject2.csproj\", \"{22222222-2222-2222-2222-222222222222}\"
EndProject
Global
	GlobalSection(SolutionConfigurationPlatforms) = preSolution
		Debug|Any CPU = Debug|Any CPU
		Release|Any CPU = Release|Any CPU
	EndGlobalSection
	GlobalSection(ProjectConfigurationPlatforms) = postSolution
		{11111111-1111-1111-1111-111111111111}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
		{11111111-1111-1111-1111-111111111111}.Debug|Any CPU.Build.0 = Debug|Any CPU
		{11111111-1111-1111-1111-111111111111}.Release|Any CPU.ActiveCfg = Release|Any CPU
		{11111111-1111-1111-1111-111111111111}.Release|Any CPU.Build.0 = Release|Any CPU
		{22222222-2222-2222-2222-222222222222}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
		{22222222-2222-2222-2222-222222222222}.Debug|Any CPU.Build.0 = Debug|Any CPU
		{22222222-2222-2222-2222-222222222222}.Release|Any CPU.ActiveCfg = Release|Any CPU
		{22222222-2222-2222-2222-222222222222}.Release|Any CPU.Build.0 = Release|Any CPU
	EndGlobalSection
	GlobalSection(SolutionProperties) = preSolution
		HideSolutionNode = FALSE
	EndGlobalSection
EndGlobal
""";

        Files.writeString(solutionPath, solutionContent);
        return solutionPath;
    }

    /**
     * 测试文件验证功能
     */
    @Test
    void testIsValidSolutionFile() throws IOException {
        // 有效文件测试
        assertTrue(defaultParser.isValidSolutionFile(testSolutionFile), "测试解决方案文件应该有效");

        // 无效文件测试
        Path invalidFile = tempDir.resolve("invalid.txt");
        Files.writeString(invalidFile, "This is not a solution file");
        assertFalse(defaultParser.isValidSolutionFile(invalidFile), "非解决方案文件应该无效");

        // 空文件测试
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);
        assertFalse(defaultParser.isValidSolutionFile(emptyFile), "空文件应该无效");
    }

    /**
     * 测试同步解析功能
     */
    @Test
    void testParseSolution() throws ParseException, IOException {
        SolutionModel solutionModel = defaultParser.parseSolution(testSolutionFile);

        // 验证解决方案基本信息
        assertEquals("TestSolution", solutionModel.getName(), "解决方案名称应该正确");
        assertEquals(testSolutionFile, solutionModel.getPath(), "解决方案路径应该正确");
        assertEquals("12.00", solutionModel.getVersion(), "解决方案版本应该正确");

        // 验证项目信息
        List<ProjectModel> projects = solutionModel.getProjects();
        assertEquals(2, projects.size(), "应该有两个项目");

        // 验证第一个项目
        ProjectModel project1 = projects.get(0);
        assertEquals("{11111111-1111-1111-1111-111111111111}", project1.getId(), "项目ID应该正确");
        assertEquals("TestProject1", project1.getName(), "项目名称应该正确");
        assertEquals(tempDir.resolve("TestProject1/TestProject1.csproj"), project1.getPath(), "项目路径应该正确");
        assertEquals("{FAE04EC0-301F-11D3-BF4B-00C04F79EFBC}", project1.getTypeGuid(), "项目类型GUID应该正确");

        // 验证全局配置
        Map<String, Map<String, String>> globalSections = solutionModel.getGlobalSections();
        assertTrue(globalSections.containsKey("SolutionConfigurationPlatforms"), "应该包含解决方案配置平台部分");
        assertTrue(globalSections.containsKey("ProjectConfigurationPlatforms"), "应该包含项目配置平台部分");
        assertTrue(globalSections.containsKey("SolutionProperties"), "应该包含解决方案属性部分");
    }

    /**
     * 测试异步解析功能
     */
    @Test
    void testParseSolutionAsync() throws ExecutionException, InterruptedException {
        CompletableFuture<SolutionModel> future = defaultParser.parseSolutionAsync(testSolutionFile);
        SolutionModel solutionModel = future.get();

        // 验证异步解析结果
        assertNotNull(solutionModel, "异步解析结果不应该为null");
        assertEquals("TestSolution", solutionModel.getName(), "解决方案名称应该正确");
        assertEquals(2, solutionModel.getProjects().size(), "应该有两个项目");
    }

    /**
     * 测试并行解析功能
     */
    @Test
    void testParseSolutionsParallel() throws ExecutionException, InterruptedException {
        List<Path> solutionPaths = List.of(testSolutionFile, testSolutionFile); // 使用同一个文件两次作为示例
        
        CompletableFuture<Map<Path, SolutionModel>> future = defaultParser.parseSolutionsParallel(solutionPaths);
        Map<Path, SolutionModel> results = future.get();

        // 验证并行解析结果
        assertNotNull(results, "并行解析结果不应该为null");
        assertEquals(1, results.size(), "应该有一个唯一的结果（因为两个路径相同）");
        
        SolutionModel solutionModel = results.get(testSolutionFile);
        assertNotNull(solutionModel, "解决方案模型不应该为null");
        assertEquals(2, solutionModel.getProjects().size(), "应该有两个项目");
    }

    /**
     * 测试缓存功能
     */
    @Test
    void testCachingFunctionality() throws ParseException, IOException {
        // 首次解析
        SolutionModel model1 = cachingParser.parseSolution(testSolutionFile);
        assertEquals(1, cachingParser.getCacheSize(), "首次解析后缓存大小应该为1");

        // 再次解析（使用缓存）
        SolutionModel model2 = cachingParser.parseSolution(testSolutionFile);
        assertSame(model1, model2, "两次解析应该返回同一个对象引用（缓存）");

        // 清除缓存
        cachingParser.clearCache(testSolutionFile);
        assertEquals(0, cachingParser.getCacheSize(), "清除缓存后大小应该为0");

        // 再次解析（无缓存）
        SolutionModel model3 = cachingParser.parseSolution(testSolutionFile);
        assertNotSame(model1, model3, "清除缓存后再次解析应该返回不同的对象");

        // 清除所有缓存
        cachingParser.clearAllCache();
        assertEquals(0, cachingParser.getCacheSize(), "清除所有缓存后大小应该为0");
    }

    /**
     * 测试缓存过期功能
     */
    @Test
    void testCacheExpiration() throws ParseException, IOException, InterruptedException {
        // 首次解析
        SolutionModel model1 = cachingParser.parseSolution(testSolutionFile);
        assertEquals(1, cachingParser.getCacheSize(), "首次解析后缓存大小应该为1");

        // 修改文件，使缓存过期
        Files.writeString(testSolutionFile, Files.readString(testSolutionFile) + "\n# Modified");

        // 再次解析（应该检测到文件变化）
        SolutionModel model2 = cachingParser.parseSolution(testSolutionFile);
        assertNotSame(model1, model2, "文件修改后应该返回不同的对象");

        // 测试缓存时间过期（需要修改测试等待时间，取决于缓存配置）
        Thread.sleep(6000); // 等待缓存过期（5秒+1秒保险）
        SolutionModel model3 = cachingParser.parseSolution(testSolutionFile);
        assertNotSame(model2, model3, "缓存过期后应该返回不同的对象");
    }

    /**
     * 测试异常处理
     */
    @Test
    void testExceptionHandling() {
        // 测试空路径
        assertThrows(IllegalArgumentException.class, () -> {
            defaultParser.parseSolution(null);
        }, "空路径应该抛出IllegalArgumentException");

        // 测试不存在的文件
        Path nonExistentFile = tempDir.resolve("non_existent.sln");
        assertThrows(IllegalArgumentException.class, () -> {
            defaultParser.parseSolution(nonExistentFile);
        }, "不存在的文件应该抛出IllegalArgumentException");
    }
}