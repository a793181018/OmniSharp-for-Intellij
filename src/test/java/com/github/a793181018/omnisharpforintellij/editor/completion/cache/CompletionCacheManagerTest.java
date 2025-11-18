package com.github.a793181018.omnisharpforintellij.editor.completion.cache;

import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfig;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfigManager;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.util.TestUtils;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CompletionCacheManagerTest extends LightPlatformTestCase {
    
    private CompletionCacheManager cacheManager;
    private Project testProject;
    private CompletionConfigManager configManager;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testProject = getProject();
        cacheManager = CompletionCacheManager.getInstance(testProject);
        configManager = CompletionConfigManager.getInstance(testProject);
        
        // 确保测试开始前缓存为空
        cacheManager.clearAllCache();
    }
    
    @Test
    public void testSingleCacheEntry() {
        // 测试单个缓存项的存储和获取
        CompletionRequest request = new CompletionRequest();
        request.setFileName("test.cs");
        request.setLine(10);
        request.setColumn(5);
        request.setText("var test = new Str");
        
        List<CompletionItem> testItems = TestUtils.createTestCompletionItems();
        
        // 存储到缓存
        cacheManager.cacheCompletions(request, testItems);
        
        // 从缓存获取
        List<CompletionItem> cachedItems = cacheManager.getCompletions(request);
        
        assertNotNull("缓存的结果不应该为null", cachedItems);
        assertEquals("缓存的结果数量应该与输入相同", testItems.size(), cachedItems.size());
        
        // 验证内容是否一致
        for (int i = 0; i < testItems.size(); i++) {
            assertEquals("缓存的项目应该匹配", testItems.get(i).getLabel(), cachedItems.get(i).getLabel());
        }
    }
    
    @Test
    public void testDifferentRequestsCache() {
        // 测试不同请求的缓存隔离
        // 第一个请求
        CompletionRequest request1 = new CompletionRequest();
        request1.setFileName("test1.cs");
        request1.setLine(10);
        request1.setColumn(5);
        
        // 第二个请求（不同文件）
        CompletionRequest request2 = new CompletionRequest();
        request2.setFileName("test2.cs");
        request2.setLine(10);
        request2.setColumn(5);
        
        // 第三个请求（不同位置）
        CompletionRequest request3 = new CompletionRequest();
        request3.setFileName("test1.cs");
        request3.setLine(20);
        request3.setColumn(15);
        
        List<CompletionItem> items1 = TestUtils.createTestCompletionItems();
        List<CompletionItem> items2 = TestUtils.createTestCompletionItems(); // 使用相同的测试数据，但应该被视为不同的缓存项
        
        // 存储不同的缓存项
        cacheManager.cacheCompletions(request1, items1);
        cacheManager.cacheCompletions(request2, items2);
        
        // 验证获取
        assertNotNull("应该能够获取第一个请求的缓存", cacheManager.getCompletions(request1));
        assertNotNull("应该能够获取第二个请求的缓存", cacheManager.getCompletions(request2));
        assertNull("第三个请求应该没有缓存", cacheManager.getCompletions(request3));
        
        // 清除第一个请求的缓存
        cacheManager.clearFileCache(request1.getFileName());
        
        // 验证清除效果
        assertNull("清除后第一个请求的缓存应该不存在", cacheManager.getCompletions(request1));
        assertNotNull("清除特定文件的缓存不应该影响其他文件", cacheManager.getCompletions(request2));
    }
    
    @Test
    public void testCacheExpiration() throws InterruptedException {
        // 测试缓存过期功能
        CompletionConfig config = configManager.getConfig();
        long originalExpirationTime = config.getCacheExpirationTime();
        
        try {
            // 设置较短的过期时间（1秒）
            config.setCacheExpirationTime(1);
            configManager.applyConfig(config);
            
            // 创建并缓存测试数据
            CompletionRequest request = new CompletionRequest();
            request.setFileName("expiration.cs");
            request.setLine(1);
            request.setColumn(1);
            
            List<CompletionItem> testItems = TestUtils.createTestCompletionItems();
            cacheManager.cacheCompletions(request, testItems);
            
            // 立即检查，应该存在
            assertNotNull("缓存刚创建时应该存在", cacheManager.getCompletions(request));
            
            // 等待过期
            Thread.sleep(1100);
            
            // 手动触发缓存清理
            cacheManager.clearExpiredCache();
            
            // 检查是否过期
            assertNull("缓存过期后应该不存在", cacheManager.getCompletions(request));
        } finally {
            // 恢复原始配置
            config.setCacheExpirationTime(originalExpirationTime);
            configManager.applyConfig(config);
        }
    }
    
    @Test
    public void testClearAllCache() {
        // 测试清除所有缓存
        // 创建多个缓存项
        CompletionRequest request1 = new CompletionRequest();
        request1.setFileName("file1.cs");
        request1.setLine(1);
        request1.setColumn(1);
        
        CompletionRequest request2 = new CompletionRequest();
        request2.setFileName("file2.cs");
        request2.setLine(2);
        request2.setColumn(2);
        
        List<CompletionItem> items = TestUtils.createTestCompletionItems();
        
        cacheManager.cacheCompletions(request1, items);
        cacheManager.cacheCompletions(request2, items);
        
        // 验证缓存存在
        assertNotNull("第一个缓存项应该存在", cacheManager.getCompletions(request1));
        assertNotNull("第二个缓存项应该存在", cacheManager.getCompletions(request2));
        
        // 清除所有缓存
        cacheManager.clearAllCache();
        
        // 验证清除效果
        assertNull("清除所有缓存后第一个项应该不存在", cacheManager.getCompletions(request1));
        assertNull("清除所有缓存后第二个项应该不存在", cacheManager.getCompletions(request2));
    }
    
    @Test
    public void testCacheStatistics() {
        // 测试缓存统计功能
        // 初始统计应该为0
        assertEquals("初始缓存命中率应该为0", 0.0, cacheManager.getCacheHitRate(), 0.001);
        assertEquals("初始缓存项数量应该为0", 0, cacheManager.getCacheItemCount());
        
        // 添加缓存项
        CompletionRequest request = new CompletionRequest();
        request.setFileName("stats.cs");
        request.setLine(1);
        request.setColumn(1);
        
        List<CompletionItem> items = TestUtils.createTestCompletionItems();
        cacheManager.cacheCompletions(request, items);
        
        // 缓存未命中（第一次获取）
        cacheManager.getCompletions(request);
        
        // 缓存命中
        cacheManager.getCompletions(request);
        
        // 验证统计
        assertEquals("缓存项数量应该为1", 1, cacheManager.getCacheItemCount());
        // 由于第一次是存储，第二次是未命中（实际上是第一次获取），第三次是命中
        // 所以命中率应该是 1/2 = 50%
        assertEquals("缓存命中率应该为50%", 0.5, cacheManager.getCacheHitRate(), 0.001);
    }
    
    @Test
    public void testNullValues() {
        // 测试空值处理
        // 空请求不应导致异常
        assertNull("对空请求的缓存获取应该返回null", cacheManager.getCompletions(null));
        
        // 空结果不应导致异常
        CompletionRequest request = new CompletionRequest();
        request.setFileName("null_test.cs");
        cacheManager.cacheCompletions(request, null);
        assertNull("缓存空结果后应该返回null", cacheManager.getCompletions(request));
    }
}