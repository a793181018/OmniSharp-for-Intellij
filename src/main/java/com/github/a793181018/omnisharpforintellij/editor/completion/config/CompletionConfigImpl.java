package com.github.a793181018.omnisharpforintellij.editor.completion.config;

import com.intellij.openapi.components.*;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;

/**
 * 代码补全配置的实现类，使用IntelliJ的持久化机制
 */
@State(
    name = "OmniSharpCompletionConfig",
    storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class CompletionConfigImpl implements CompletionConfig, PersistentStateComponent<CompletionConfigImpl> {
    
    // 默认配置值
    private static final boolean DEFAULT_COMPLETION_ENABLED = true;
    private static final boolean DEFAULT_SMART_COMPLETION_ENABLED = true;
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 1000;
    private static final int DEFAULT_MAX_COMPLETION_ITEMS = 200;
    private static final boolean DEFAULT_CACHE_ENABLED = true;
    private static final int DEFAULT_CACHE_MAX_SIZE = 1000;
    private static final long DEFAULT_CACHE_EXPIRATION_TIME_MS = 5 * 60 * 1000; // 5分钟
    private static final boolean DEFAULT_SHOW_TYPE_INFO = true;
    private static final boolean DEFAULT_AUTO_POPUP_ENABLED = true;
    private static final int DEFAULT_AUTO_POPUP_DELAY_MS = 100;
    private static final boolean DEFAULT_COMPLETION_SORTING_ENABLED = true;
    
    // 配置字段
    private boolean completionEnabled = DEFAULT_COMPLETION_ENABLED;
    private boolean smartCompletionEnabled = DEFAULT_SMART_COMPLETION_ENABLED;
    private int requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    private int maxCompletionItems = DEFAULT_MAX_COMPLETION_ITEMS;
    private boolean cacheEnabled = DEFAULT_CACHE_ENABLED;
    private int cacheMaxSize = DEFAULT_CACHE_MAX_SIZE;
    private long cacheExpirationTimeMs = DEFAULT_CACHE_EXPIRATION_TIME_MS;
    private boolean showTypeInfo = DEFAULT_SHOW_TYPE_INFO;
    private boolean autoPopupEnabled = DEFAULT_AUTO_POPUP_ENABLED;
    private int autoPopupDelayMs = DEFAULT_AUTO_POPUP_DELAY_MS;
    private boolean completionSortingEnabled = DEFAULT_COMPLETION_SORTING_ENABLED;
    
    @Override
    public boolean isCompletionEnabled() {
        return completionEnabled;
    }
    
    @Override
    public void setCompletionEnabled(boolean enabled) {
        this.completionEnabled = enabled;
    }
    
    @Override
    public boolean isSmartCompletionEnabled() {
        return smartCompletionEnabled;
    }
    
    @Override
    public void setSmartCompletionEnabled(boolean enabled) {
        this.smartCompletionEnabled = enabled;
    }
    
    @Override
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
    
    @Override
    public void setRequestTimeoutMs(int timeoutMs) {
        // 确保超时时间在合理范围内
        this.requestTimeoutMs = Math.max(500, Math.min(timeoutMs, 10000));
    }
    
    @Override
    public int getMaxCompletionItems() {
        return maxCompletionItems;
    }
    
    @Override
    public void setMaxCompletionItems(int maxItems) {
        // 确保最大补全项数量在合理范围内
        this.maxCompletionItems = Math.max(50, Math.min(maxItems, 1000));
    }
    
    @Override
    public boolean isCacheEnabled() {
        return cacheEnabled;
    }
    
    @Override
    public void setCacheEnabled(boolean enabled) {
        this.cacheEnabled = enabled;
    }
    
    @Override
    public int getCacheMaxSize() {
        return cacheMaxSize;
    }
    
    @Override
    public void setCacheMaxSize(int maxSize) {
        // 确保缓存大小在合理范围内
        this.cacheMaxSize = Math.max(100, Math.min(maxSize, 5000));
    }
    
    @Override
    public long getCacheExpirationTimeMs() {
        return cacheExpirationTimeMs;
    }
    
    @Override
    public void setCacheExpirationTimeMs(long expirationTimeMs) {
        // 确保过期时间在合理范围内
        this.cacheExpirationTimeMs = Math.max(60000, Math.min(expirationTimeMs, 30 * 60 * 1000));
    }
    
    @Override
    public boolean isShowTypeInfo() {
        return showTypeInfo;
    }
    
    @Override
    public void setShowTypeInfo(boolean show) {
        this.showTypeInfo = show;
    }
    
    @Override
    public boolean isAutoPopupEnabled() {
        return autoPopupEnabled;
    }
    
    @Override
    public void setAutoPopupEnabled(boolean enabled) {
        this.autoPopupEnabled = enabled;
    }
    
    @Override
    public int getAutoPopupDelayMs() {
        return autoPopupDelayMs;
    }
    
    @Override
    public void setAutoPopupDelayMs(int delayMs) {
        // 确保延迟时间在合理范围内
        this.autoPopupDelayMs = Math.max(0, Math.min(delayMs, 1000));
    }
    
    @Override
    public boolean isCompletionSortingEnabled() {
        return completionSortingEnabled;
    }
    
    @Override
    public void setCompletionSortingEnabled(boolean enabled) {
        this.completionSortingEnabled = enabled;
    }
    
    @Override
    public void resetToDefaults() {
        this.completionEnabled = DEFAULT_COMPLETION_ENABLED;
        this.smartCompletionEnabled = DEFAULT_SMART_COMPLETION_ENABLED;
        this.requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
        this.maxCompletionItems = DEFAULT_MAX_COMPLETION_ITEMS;
        this.cacheEnabled = DEFAULT_CACHE_ENABLED;
        this.cacheMaxSize = DEFAULT_CACHE_MAX_SIZE;
        this.cacheExpirationTimeMs = DEFAULT_CACHE_EXPIRATION_TIME_MS;
        this.showTypeInfo = DEFAULT_SHOW_TYPE_INFO;
        this.autoPopupEnabled = DEFAULT_AUTO_POPUP_ENABLED;
        this.autoPopupDelayMs = DEFAULT_AUTO_POPUP_DELAY_MS;
        this.completionSortingEnabled = DEFAULT_COMPLETION_SORTING_ENABLED;
    }
    
    @NotNull
    @Override
    public CompletionConfig copy() {
        CompletionConfigImpl copy = new CompletionConfigImpl();
        copy.completionEnabled = this.completionEnabled;
        copy.smartCompletionEnabled = this.smartCompletionEnabled;
        copy.requestTimeoutMs = this.requestTimeoutMs;
        copy.maxCompletionItems = this.maxCompletionItems;
        copy.cacheEnabled = this.cacheEnabled;
        copy.cacheMaxSize = this.cacheMaxSize;
        copy.cacheExpirationTimeMs = this.cacheExpirationTimeMs;
        copy.showTypeInfo = this.showTypeInfo;
        copy.autoPopupEnabled = this.autoPopupEnabled;
        copy.autoPopupDelayMs = this.autoPopupDelayMs;
        copy.completionSortingEnabled = this.completionSortingEnabled;
        return copy;
    }
    
    @Override
    public CompletionConfigImpl getState() {
        return this;
    }
    
    @Override
    public void loadState(@NotNull CompletionConfigImpl state) {
        XmlSerializerUtil.copyBean(state, this);
    }
    
    /**
     * 获取默认配置
     */
    @NotNull
    public static CompletionConfig getDefault() {
        CompletionConfigImpl config = new CompletionConfigImpl();
        config.resetToDefaults();
        return config;
    }
    
    @Override
    public String toString() {
        return "CompletionConfig{" +
               "completionEnabled=" + completionEnabled +
               ", smartCompletionEnabled=" + smartCompletionEnabled +
               ", requestTimeoutMs=" + requestTimeoutMs +
               ", maxCompletionItems=" + maxCompletionItems +
               ", cacheEnabled=" + cacheEnabled +
               ", cacheMaxSize=" + cacheMaxSize +
               ", cacheExpirationTimeMs=" + cacheExpirationTimeMs +
               ", showTypeInfo=" + showTypeInfo +
               ", autoPopupEnabled=" + autoPopupEnabled +
               ", autoPopupDelayMs=" + autoPopupDelayMs +
               ", completionSortingEnabled=" + completionSortingEnabled +
               '}';
    }
}