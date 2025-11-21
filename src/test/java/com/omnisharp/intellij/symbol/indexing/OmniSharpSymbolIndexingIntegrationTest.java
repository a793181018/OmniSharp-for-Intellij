package com.omnisharp.intellij.symbol.indexing;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 符号索引和缓存功能的集成测试类
 * 测试SymbolCollector, SymbolIndexer, SymbolCache, IncrementalUpdater和SymbolSearcher之间的交互
 */
public class OmniSharpSymbolIndexingIntegrationTest {

    private OmniSharpSymbolCollector collector;
    private OmniSharpSymbolIndexer indexer;
    private OmniSharpSymbolCache cache;
    private OmniSharpIncrementalUpdater updater;
    private OmniSharpSymbolSearcher searcher;
    
    // Mock依赖组件
    private OmniSharpSolutionModel solutionModel;
    private OmniSharpProjectModel projectModel;
    private OmniSharpProjectManager projectManager;
    private OmniSharpFileParser fileParser;
    private OmniSharpLogger logger;
    
    // 测试用的临时目录
    private Path tempDir;
    private ExecutorService executorService;

    @BeforeEach
    void setUp() throws IOException {
        // 创建临时目录
        tempDir = Files.createTempDirectory("omnisharp-test");
        
        // 初始化线程池
        executorService = Executors.newFixedThreadPool(4);
        
        // Mock组件
        solutionModel = mock(OmniSharpSolutionModel.class);
        projectModel = mock(OmniSharpProjectModel.class);
        projectManager = mock(OmniSharpProjectManager.class);
        fileParser = mock(OmniSharpFileParser.class);
        logger = mock(OmniSharpLogger.class);
        
        // 创建实际组件，注入mock
        collector = new OmniSharpSymbolCollector(
                solutionModel, 
                projectManager, 
                fileParser, 
                logger,
                executorService
        );
        
        indexer = new OmniSharpSymbolIndexer(logger);
        
        // 创建缓存目录
        Path cacheDir = tempDir.resolve("cache");
        Files.createDirectory(cacheDir);
        
        cache = new OmniSharpSymbolCache(
                cacheDir.toString(),
                1000, // 最大缓存项
                60 * 1000, // 缓存过期时间（毫秒）
                logger
        );
        
        updater = new OmniSharpIncrementalUpdater(indexer, cache, logger);
        searcher = new OmniSharpSymbolSearcher(indexer, cache, logger);
        
        // 设置mock行为
        setupMockBehaviors();
    }
    
    private void setupMockBehaviors() throws IOException {
        // 设置项目信息
        when(solutionModel.getProjects()).thenReturn(Collections.singletonList(projectModel));
        when(projectModel.getName()).thenReturn("TestProject");
        
        // 创建测试文件
        Path testFile = tempDir.resolve("TestFile.cs");
        Files.write(testFile, Collections.singletonList("// Test file content"));
        
        List<File> projectFiles = Collections.singletonList(testFile.toFile());
        when(projectManager.getSourceFiles(projectModel)).thenReturn(projectFiles);
        
        // 模拟文件解析结果
        Symbol testClassSymbol = new Symbol(
                "TestClass",
                "MyNamespace.TestClass",
                SymbolKind.CLASS,
                "TestProject",
                testFile.toString(),
                1, 10,
                new HashMap<>()
        );
        
        Symbol testMethodSymbol = new Symbol(
                "TestMethod",
                "MyNamespace.TestClass.TestMethod",
                SymbolKind.METHOD,
                "TestProject",
                testFile.toString(),
                5, 5,
                new HashMap<>()
        );
        
        List<Symbol> symbols = Arrays.asList(testClassSymbol, testMethodSymbol);
        when(fileParser.parseFile(testFile.toFile())).thenReturn(symbols);
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // 关闭资源
        collector.shutdown();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        // 删除临时目录
        Files.walk(tempDir)
             .map(Path::toFile)
             .sorted(Comparator.reverseOrder())
             .forEach(File::delete);
    }
    
    /**
     * 测试完整的符号索引流程：收集 -> 索引 -> 缓存 -> 搜索
     */
    @Test
    void testCompleteIndexingAndSearchFlow() {
        // 1. 收集符号
        SymbolCollectionResult collectionResult = collector.collectSymbols();
        assertNotNull(collectionResult);
        assertEquals(2, collectionResult.getTotalSymbols());
        
        // 2. 构建索引
        boolean indexResult = indexer.buildIndex(collectionResult);
        assertTrue(indexResult);
        
        // 3. 缓存索引
        String cacheKey = "test-cache-key-123";
        boolean cacheResult = cache.cacheIndex(cacheKey, indexer);
        assertTrue(cacheResult);
        
        // 4. 搜索符号
        List<SymbolSearchResult> searchResults = searcher.searchSymbols(
                "Test", 
                new SymbolSearchOptions(false, false, null, null)
        );
        assertNotNull(searchResults);
        assertEquals(2, searchResults.size());
        
        // 验证搜索结果
        boolean foundClass = searchResults.stream()
                .anyMatch(result -> result.getSymbol().getName().equals("TestClass"));
        boolean foundMethod = searchResults.stream()
                .anyMatch(result -> result.getSymbol().getName().equals("TestMethod"));
        
        assertTrue(foundClass);
        assertTrue(foundMethod);
    }
    
    /**
     * 测试增量更新功能
     */
    @Test
    void testIncrementalUpdate() throws IOException {
        // 初始符号收集和索引构建
        SymbolCollectionResult initialResult = collector.collectSymbols();
        indexer.buildIndex(initialResult);
        
        // 模拟文件变更
        Path updatedFile = tempDir.resolve("UpdatedFile.cs");
        Files.write(updatedFile, Collections.singletonList("// Updated file content"));
        
        Symbol newSymbol = new Symbol(
                "NewClass",
                "MyNamespace.NewClass",
                SymbolKind.CLASS,
                "TestProject",
                updatedFile.toString(),
                1, 10,
                new HashMap<>()
        );
        
        when(projectManager.getSourceFiles(projectModel))
                .thenReturn(Arrays.asList(tempDir.resolve("TestFile.cs").toFile(), updatedFile.toFile()));
        when(fileParser.parseFile(updatedFile.toFile())).thenReturn(Collections.singletonList(newSymbol));
        
        // 执行增量更新
        FileChangeEvent changeEvent = new FileChangeEvent(
                updatedFile.toString(), 
                FileChangeType.CREATED
        );
        
        boolean updateResult = updater.processFileChange(changeEvent);
        assertTrue(updateResult);
        
        // 验证更新后的搜索结果
        List<SymbolSearchResult> searchResults = searcher.searchSymbols(
                "Class", 
                new SymbolSearchOptions(false, false, null, null)
        );
        assertNotNull(searchResults);
        assertEquals(2, searchResults.size());
        
        boolean foundNewClass = searchResults.stream()
                .anyMatch(result -> result.getSymbol().getName().equals("NewClass"));
        assertTrue(foundNewClass);
    }
    
    /**
     * 测试缓存加载功能
     */
    @Test
    void testCacheLoading() {
        // 构建索引并缓存
        SymbolCollectionResult result = collector.collectSymbols();
        indexer.buildIndex(result);
        
        String cacheKey = "test-cache-key-456";
        cache.cacheIndex(cacheKey, indexer);
        
        // 创建新的索引器
        OmniSharpSymbolIndexer newIndexer = new OmniSharpSymbolIndexer(logger);
        
        // 从缓存加载
        boolean loadResult = cache.loadIndex(cacheKey, newIndexer);
        assertTrue(loadResult);
        
        // 验证索引已加载
        List<Symbol> allSymbols = newIndexer.getAllSymbols();
        assertNotNull(allSymbols);
        assertEquals(2, allSymbols.size());
    }
    
    /**
     * 测试搜索结果排序功能
     */
    @Test
    void testSearchResultSorting() {
        // 构建索引
        SymbolCollectionResult result = collector.collectSymbols();
        indexer.buildIndex(result);
        
        // 使用精确匹配搜索，应该先返回完全匹配的结果
        List<SymbolSearchResult> searchResults = searcher.searchSymbols(
                "TestClass", 
                new SymbolSearchOptions(true, false, null, null)
        );
        
        assertNotNull(searchResults);
        assertFalse(searchResults.isEmpty());
        
        // 第一个结果应该是TestClass
        assertEquals("TestClass", searchResults.get(0).getSymbol().getName());
    }
    
    /**
     * 测试符号过滤功能
     */
    @Test
    void testSymbolFiltering() {
        // 构建索引
        SymbolCollectionResult result = collector.collectSymbols();
        indexer.buildIndex(result);
        
        // 只搜索类类型的符号
        List<SymbolKind> filterKinds = Collections.singletonList(SymbolKind.CLASS);
        List<SymbolSearchResult> searchResults = searcher.searchSymbols(
                "Test", 
                new SymbolSearchOptions(false, false, filterKinds, null)
        );
        
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());
        assertEquals(SymbolKind.CLASS, searchResults.get(0).getSymbol().getKind());
    }
}