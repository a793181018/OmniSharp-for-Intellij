package com.github.a793181018.omnisharpforintellij.editor.completion.config;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class CompletionConfigManagerTest extends LightPlatformTestCase {
    
    private CompletionConfigManager configManager;
    private Project testProject;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testProject = getProject();
        configManager = CompletionConfigManager.getInstance(testProject);
    }
    
    @Test
    public void testGetConfig() {
        // 测试获取配置实例
        CompletionConfig config = configManager.getConfig();
        assertNotNull("配置管理器应该返回非空配置", config);
        
        // 验证默认值
        assertTrue("智能补全默认应该启用", config.isSmartCompletionEnabled());
        assertTrue("缓存默认应该启用", config.isCacheEnabled());
        assertEquals("默认缓存过期时间应该为60秒", 60, config.getCacheExpirationTime());
        assertEquals("默认最大缓存项数应该为1000", 1000, config.getMaxCacheItems());
        assertTrue("默认应该启用排序", config.isSortingEnabled());
    }
    
    @Test
    public void testApplyConfig() {
        // 测试应用配置更改
        CompletionConfig config = configManager.getConfig();
        
        // 修改配置值
        boolean newSmartCompletion = !config.isSmartCompletionEnabled();
        boolean newCacheEnabled = !config.isCacheEnabled();
        long newCacheExpirationTime = 120;
        int newMaxCacheItems = 2000;
        boolean newSortingEnabled = !config.isSortingEnabled();
        
        config.setSmartCompletionEnabled(newSmartCompletion);
        config.setCacheEnabled(newCacheEnabled);
        config.setCacheExpirationTime(newCacheExpirationTime);
        config.setMaxCacheItems(newMaxCacheItems);
        config.setSortingEnabled(newSortingEnabled);
        
        // 应用更改
        configManager.applyConfig(config);
        
        // 验证更改已应用
        CompletionConfig updatedConfig = configManager.getConfig();
        assertEquals("智能补全设置应该已更新", newSmartCompletion, updatedConfig.isSmartCompletionEnabled());
        assertEquals("缓存启用设置应该已更新", newCacheEnabled, updatedConfig.isCacheEnabled());
        assertEquals("缓存过期时间应该已更新", newCacheExpirationTime, updatedConfig.getCacheExpirationTime());
        assertEquals("最大缓存项数应该已更新", newMaxCacheItems, updatedConfig.getMaxCacheItems());
        assertEquals("排序启用设置应该已更新", newSortingEnabled, updatedConfig.isSortingEnabled());
    }
    
    @Test
    public void testResetToDefaults() {
        // 测试重置为默认配置
        CompletionConfig config = configManager.getConfig();
        
        // 修改所有配置值
        config.setSmartCompletionEnabled(false);
        config.setCacheEnabled(false);
        config.setCacheExpirationTime(200);
        config.setMaxCacheItems(5000);
        config.setSortingEnabled(false);
        config.setMaxResults(50);
        config.setTimeout(10000);
        config.setCaseSensitive(false);
        config.setFuzzyMatchingEnabled(true);
        config.setSnippetCompletionEnabled(false);
        config.setPreselectFirstItem(false);
        config.setAutoPopupEnabled(false);
        config.setShowDocumentation(false);
        config.setShowParameterInfo(false);
        config.setPreselectByProbability(true);
        config.setMinimumPrefixLength(3);
        config.setParameterHintsEnabled(false);
        
        // 应用更改
        configManager.applyConfig(config);
        
        // 验证更改已应用
        assertFalse("智能补全应该被禁用", configManager.getConfig().isSmartCompletionEnabled());
        assertFalse("缓存应该被禁用", configManager.getConfig().isCacheEnabled());
        
        // 重置为默认值
        configManager.resetToDefaults();
        
        // 验证已重置为默认值
        CompletionConfig defaultConfig = configManager.getConfig();
        assertTrue("智能补全默认应该启用", defaultConfig.isSmartCompletionEnabled());
        assertTrue("缓存默认应该启用", defaultConfig.isCacheEnabled());
        assertEquals("默认缓存过期时间应该为60秒", 60, defaultConfig.getCacheExpirationTime());
        assertEquals("默认最大缓存项数应该为1000", 1000, defaultConfig.getMaxCacheItems());
        assertTrue("默认应该启用排序", defaultConfig.isSortingEnabled());
    }
    
    @Test
    public void testConfigListener() throws InterruptedException {
        // 测试配置变更监听器
        final CountDownLatch latch = new CountDownLatch(1);
        final List<CompletionConfig> notifiedConfigs = new ArrayList<>();
        
        // 添加监听器
        CompletionConfigManager.ConfigChangeListener listener = new CompletionConfigManager.ConfigChangeListener() {
            @Override
            public void onConfigChanged(CompletionConfig newConfig) {
                notifiedConfigs.add(newConfig);
                latch.countDown();
            }
        };
        
        configManager.addConfigChangeListener(listener);
        
        try {
            // 修改并应用配置
            CompletionConfig config = configManager.getConfig();
            config.setSmartCompletionEnabled(!config.isSmartCompletionEnabled());
            configManager.applyConfig(config);
            
            // 等待通知
            boolean notified = latch.await(1, TimeUnit.SECONDS);
            assertTrue("应该收到配置变更通知", notified);
            assertFalse("通知的配置列表不应为空", notifiedConfigs.isEmpty());
            
            // 验证通知的配置是最新的
            CompletionConfig notifiedConfig = notifiedConfigs.get(0);
            assertEquals("通知的配置应该与当前配置一致", 
                    configManager.getConfig().isSmartCompletionEnabled(), 
                    notifiedConfig.isSmartCompletionEnabled());
        } finally {
            // 移除监听器
            configManager.removeConfigChangeListener(listener);
        }
    }
    
    @Test
    public void testRemoveConfigListener() throws InterruptedException {
        // 测试移除配置变更监听器
        final CountDownLatch latch = new CountDownLatch(1);
        
        CompletionConfigManager.ConfigChangeListener listener = new CompletionConfigManager.ConfigChangeListener() {
            @Override
            public void onConfigChanged(CompletionConfig newConfig) {
                latch.countDown(); // 如果收到通知，就减少计数
            }
        };
        
        // 添加然后立即移除监听器
        configManager.addConfigChangeListener(listener);
        configManager.removeConfigChangeListener(listener);
        
        // 修改并应用配置
        CompletionConfig config = configManager.getConfig();
        config.setSmartCompletionEnabled(!config.isSmartCompletionEnabled());
        configManager.applyConfig(config);
        
        // 等待一小段时间，确保如果有通知的话能收到
        Thread.sleep(100);
        
        // 验证监听器没有被调用
        assertEquals("监听器不应该被调用", 1, latch.getCount());
    }
    
    @Test
    public void testConfigValueValidation() {
        // 测试配置值验证
        CompletionConfig config = configManager.getConfig();
        
        // 测试边界值
        config.setCacheExpirationTime(-1); // 应该被限制为最小值
        assertEquals("缓存过期时间应该被限制为最小值", 1, config.getCacheExpirationTime());
        
        config.setCacheExpirationTime(3601); // 应该被限制为最大值
        assertEquals("缓存过期时间应该被限制为最大值", 3600, config.getCacheExpirationTime());
        
        config.setMaxCacheItems(0); // 应该被限制为最小值
        assertEquals("最大缓存项数应该被限制为最小值", 1, config.getMaxCacheItems());
        
        config.setMaxCacheItems(10001); // 应该被限制为最大值
        assertEquals("最大缓存项数应该被限制为最大值", 10000, config.getMaxCacheItems());
        
        config.setTimeout(0); // 应该被限制为最小值
        assertEquals("超时时间应该被限制为最小值", 1000, config.getTimeout());
        
        config.setTimeout(60001); // 应该被限制为最大值
        assertEquals("超时时间应该被限制为最大值", 60000, config.getTimeout());
        
        config.setMinimumPrefixLength(-1); // 应该被限制为最小值
        assertEquals("最小前缀长度应该被限制为最小值", 0, config.getMinimumPrefixLength());
        
        config.setMinimumPrefixLength(11); // 应该被限制为最大值
        assertEquals("最小前缀长度应该被限制为最大值", 10, config.getMinimumPrefixLength());
    }
    
    @Test
    public void testConfigCopy() {
        // 测试配置复制功能
        CompletionConfig originalConfig = configManager.getConfig();
        CompletionConfig copiedConfig = originalConfig.copy();
        
        // 验证复制的配置与原始配置值相同
        assertEquals("复制的配置应该与原始配置具有相同的智能补全设置", 
                originalConfig.isSmartCompletionEnabled(), copiedConfig.isSmartCompletionEnabled());
        assertEquals("复制的配置应该与原始配置具有相同的缓存启用设置", 
                originalConfig.isCacheEnabled(), copiedConfig.isCacheEnabled());
        assertEquals("复制的配置应该与原始配置具有相同的缓存过期时间", 
                originalConfig.getCacheExpirationTime(), copiedConfig.getCacheExpirationTime());
        
        // 验证是深拷贝（修改复制的配置不影响原始配置）
        copiedConfig.setSmartCompletionEnabled(!originalConfig.isSmartCompletionEnabled());
        assertFalse("修改复制的配置不应该影响原始配置", 
                originalConfig.isSmartCompletionEnabled() == copiedConfig.isSmartCompletionEnabled());
    }
}