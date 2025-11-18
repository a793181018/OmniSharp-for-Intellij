package com.github.a793181018.omnisharpforintellij.editor.completion.config;

import org.jetbrains.annotations.NotNull;

/**
 * 代码补全配置接口，定义补全功能的配置选项
 */
public interface CompletionConfig {
    
    /**
     * 是否启用代码补全
     */
    boolean isCompletionEnabled();
    
    /**
     * 设置是否启用代码补全
     */
    void setCompletionEnabled(boolean enabled);
    
    /**
     * 是否启用智能补全（上下文感知）
     */
    boolean isSmartCompletionEnabled();
    
    /**
     * 设置是否启用智能补全
     */
    void setSmartCompletionEnabled(boolean enabled);
    
    /**
     * 获取补全请求超时时间（毫秒）
     */
    int getRequestTimeoutMs();
    
    /**
     * 设置补全请求超时时间（毫秒）
     */
    void setRequestTimeoutMs(int timeoutMs);
    
    /**
     * 获取最大补全项数量
     */
    int getMaxCompletionItems();
    
    /**
     * 设置最大补全项数量
     */
    void setMaxCompletionItems(int maxItems);
    
    /**
     * 是否启用补全缓存
     */
    boolean isCacheEnabled();
    
    /**
     * 设置是否启用补全缓存
     */
    void setCacheEnabled(boolean enabled);
    
    /**
     * 获取缓存最大大小
     */
    int getCacheMaxSize();
    
    /**
     * 设置缓存最大大小
     */
    void setCacheMaxSize(int maxSize);
    
    /**
     * 获取缓存过期时间（毫秒）
     */
    long getCacheExpirationTimeMs();
    
    /**
     * 设置缓存过期时间（毫秒）
     */
    void setCacheExpirationTimeMs(long expirationTimeMs);
    
    /**
     * 是否显示补全项的类型信息
     */
    boolean isShowTypeInfo();
    
    /**
     * 设置是否显示补全项的类型信息
     */
    void setShowTypeInfo(boolean show);
    
    /**
     * 是否启用自动弹出补全列表
     */
    boolean isAutoPopupEnabled();
    
    /**
     * 设置是否启用自动弹出补全列表
     */
    void setAutoPopupEnabled(boolean enabled);
    
    /**
     * 获取自动弹出延迟时间（毫秒）
     */
    int getAutoPopupDelayMs();
    
    /**
     * 设置自动弹出延迟时间（毫秒）
     */
    void setAutoPopupDelayMs(int delayMs);
    
    /**
     * 是否启用补全项排序优化
     */
    boolean isCompletionSortingEnabled();
    
    /**
     * 设置是否启用补全项排序优化
     */
    void setCompletionSortingEnabled(boolean enabled);
    
    /**
     * 重置为默认配置
     */
    void resetToDefaults();
    
    /**
     * 复制当前配置
     */
    @NotNull
    CompletionConfig copy();
}