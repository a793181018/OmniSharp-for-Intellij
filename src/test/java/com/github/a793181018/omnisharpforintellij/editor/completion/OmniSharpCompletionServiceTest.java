package com.github.a793181018.omnisharpforintellij.editor.completion;

import com.github.a793181018.omnisharpforintellij.editor.completion.cache.CompletionCacheManager;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfig;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfigManager;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.performance.CompletionPerformanceService;
import com.github.a793181018.omnisharpforintellij.util.TestUtils;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class OmniSharpCompletionServiceTest extends LightPlatformTestCase {
    
    private OmniSharpCompletionService completionService;
    private Project testProject;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testProject = getProject();
        completionService = OmniSharpCompletionServiceImpl.getInstance(testProject);
    }
    
    @Test
    public void testConfigIntegration() {
        // 测试配置集成
        CompletionConfigManager configManager = CompletionConfigManager.getInstance(testProject);
        CompletionConfig config = configManager.getConfig();
        
        assertNotNull("配置管理器应该返回有效的配置", config);
        assertTrue("智能补全默认应该启用", config.isSmartCompletionEnabled());
        assertTrue("缓存默认应该启用", config.isCacheEnabled());
    }
    
    @Test
    public void testCacheFunctionality() {
        // 测试缓存功能
        CompletionCacheManager cacheManager = CompletionCacheManager.getInstance(testProject);
        
        // 创建一个测试请求
        CompletionRequest request = new CompletionRequest();
        request.setFileName("test.cs");
        request.setLine(1);
        request.setColumn(1);
        
        // 创建一些测试补全项
        List<CompletionItem> testItems = TestUtils.createTestCompletionItems();
        
        // 存储到缓存
        cacheManager.cacheCompletions(request, testItems);
        
        // 从缓存获取
        List<CompletionItem> cachedItems = cacheManager.getCompletions(request);
        
        assertNotNull("缓存应该返回有效的补全项列表", cachedItems);
        assertFalse("缓存的补全项列表不应该为空", cachedItems.isEmpty());
        assertEquals("缓存的补全项数量应该与存储的相同", testItems.size(), cachedItems.size());
        
        // 清除缓存并验证
        cacheManager.clearAllCache();
        assertNull("清除缓存后应该返回null", cacheManager.getCompletions(request));
    }
    
    @Test
    public void testPerformanceServiceIntegration() {
        // 测试性能优化服务集成
        CompletionPerformanceService performanceService = getService(CompletionPerformanceService.class);
        assertNotNull("性能优化服务应该可用", performanceService);
    }
    
    @Test
    public void testRequestCancellation() {
        // 测试请求取消功能
        OmniSharpCompletionServiceImpl impl = (OmniSharpCompletionServiceImpl) completionService;
        
        // 验证初始状态
        assertFalse("初始状态下不应该有进行中的请求", impl.isRequestInProgress());
        
        // 模拟设置进行中请求
        impl.setRequestInProgress(true);
        assertTrue("设置后应该有进行中的请求", impl.isRequestInProgress());
        
        // 取消请求
        impl.cancelRequest();
        assertFalse("取消后不应该有进行中的请求", impl.isRequestInProgress());
    }
    
    @Test
    public void testAsyncCompletionFlow() throws Exception {
        // 测试异步补全流程
        // 在实际测试中，这里应该模拟OmniSharp服务器的响应
        // 由于这是轻量级测试，我们主要验证基本流程
        
        CompletionRequest request = new CompletionRequest();
        request.setFileName("test.cs");
        request.setLine(1);
        request.setColumn(1);
        request.setText("using System;\n\npublic class Test {\n    public void Method() {\n        Str");
        
        // 测试异步方法不会抛出异常
        CompletableFuture<List<CompletionItem>> future = completionService.getCompletionsAsync(request);
        assertNotNull("异步方法应该返回有效的CompletableFuture", future);
        
        // 由于没有实际的OmniSharp服务器，我们不等待完成，只是验证流程
    }
    
    @Test
    public void testConfigChangeNotification() {
        // 测试配置变更通知
        CompletionConfigManager configManager = CompletionConfigManager.getInstance(testProject);
        CompletionConfig config = configManager.getConfig();
        
        // 保存原始状态
        boolean originalSmartCompletion = config.isSmartCompletionEnabled();
        
        try {
            // 修改配置
            config.setSmartCompletionEnabled(!originalSmartCompletion);
            configManager.applyConfig(config);
            
            // 验证配置已更新
            CompletionConfig updatedConfig = configManager.getConfig();
            assertEquals("配置应该已更新", !originalSmartCompletion, updatedConfig.isSmartCompletionEnabled());
        } finally {
            // 恢复原始配置
            config.setSmartCompletionEnabled(originalSmartCompletion);
            configManager.applyConfig(config);
        }
    }
    
    @Test
    public void testCacheExpiration() throws Exception {
        // 测试缓存过期
        CompletionConfigManager configManager = CompletionConfigManager.getInstance(testProject);
        CompletionConfig config = configManager.getConfig();
        CompletionCacheManager cacheManager = CompletionCacheManager.getInstance(testProject);
        
        // 保存原始缓存过期时间
        long originalExpirationTime = config.getCacheExpirationTime();
        
        try {
            // 设置短的缓存过期时间（1秒）
            config.setCacheExpirationTime(1);
            configManager.applyConfig(config);
            
            // 创建并缓存测试数据
            CompletionRequest request = new CompletionRequest();
            request.setFileName("test_expiration.cs");
            request.setLine(1);
            request.setColumn(1);
            
            List<CompletionItem> testItems = TestUtils.createTestCompletionItems();
            cacheManager.cacheCompletions(request, testItems);
            
            // 验证缓存有效
            assertNotNull("缓存应该返回有效数据", cacheManager.getCompletions(request));
            
            // 等待缓存过期
            Thread.sleep(1100);
            
            // 手动触发缓存清理（通常由定时任务完成）
            cacheManager.clearExpiredCache();
            
            // 验证缓存已过期
            assertNull("缓存过期后应该返回null", cacheManager.getCompletions(request));
        } finally {
            // 恢复原始配置
            config.setCacheExpirationTime(originalExpirationTime);
            configManager.applyConfig(config);
        }
    }
}