package com.omnisharp.intellij.projectstructure.utils;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import com.omnisharp.intellij.projectstructure.utils.ProjectLogger;

/**
 * 配置管理器，用于管理和持久化项目结构分析模块的配置
 */
@State(
    name = "OmniSharpProjectStructureConfig",
    storages = @Storage("omnisharpProjectStructure.xml")
)
public class ConfigurationManager implements PersistentStateComponent<ConfigurationManager.State> {
    
    private final State state = new State();
    private final ProjectLogger logger = ProjectLogger.getInstance(ConfigurationManager.class);
    
    /**
     * 获取配置管理器实例
     * @return 配置管理器实例
     */
    public static ConfigurationManager getInstance() {
        return ApplicationManager.getApplication().getService(ConfigurationManager.class);
    }
    
    @Nullable
    @Override
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
    
    /**
     * 配置状态类，用于持久化存储
     */
    public static class State {
        public boolean enableIndexing = true;
        public boolean enableRealTimeMonitoring = true;
        public boolean enableDependencyAnalysis = true;
        public int maxIndexingDepth = 10;
        public int indexingBatchSize = 100;
        public boolean enableCaching = true;
        public int cacheExpirationMinutes = 60;
        public boolean showHiddenFiles = false;
        public boolean followSymlinks = false;
        public Map<String, String> customSettings = new HashMap<>();
    }
    
    /**
     * 获取索引开关状态
     * @return 是否启用索引
     */
    public boolean isIndexingEnabled() {
        return state.enableIndexing;
    }
    
    /**
     * 设置索引开关状态
     * @param enabled 是否启用索引
     */
    public void setIndexingEnabled(boolean enabled) {
        state.enableIndexing = enabled;
        logger.debug("Indexing enabled: " + enabled);
    }
    
    /**
     * 获取实时监控开关状态
     * @return 是否启用实时监控
     */
    public boolean isRealTimeMonitoringEnabled() {
        return state.enableRealTimeMonitoring;
    }
    
    /**
     * 设置实时监控开关状态
     * @param enabled 是否启用实时监控
     */
    public void setRealTimeMonitoringEnabled(boolean enabled) {
        state.enableRealTimeMonitoring = enabled;
        logger.debug("Real-time monitoring enabled: " + enabled);
    }
    
    /**
     * 获取依赖分析开关状态
     * @return 是否启用依赖分析
     */
    public boolean isDependencyAnalysisEnabled() {
        return state.enableDependencyAnalysis;
    }
    
    /**
     * 设置依赖分析开关状态
     * @param enabled 是否启用依赖分析
     */
    public void setDependencyAnalysisEnabled(boolean enabled) {
        state.enableDependencyAnalysis = enabled;
        logger.debug("Dependency analysis enabled: " + enabled);
    }
    
    /**
     * 获取最大索引深度
     * @return 最大索引深度
     */
    public int getMaxIndexingDepth() {
        return state.maxIndexingDepth;
    }
    
    /**
     * 设置最大索引深度
     * @param depth 最大索引深度
     */
    public void setMaxIndexingDepth(int depth) {
        state.maxIndexingDepth = Math.max(1, depth);
        logger.debug("Max indexing depth set to: " + state.maxIndexingDepth);
    }
    
    /**
     * 获取索引批处理大小
     * @return 索引批处理大小
     */
    public int getIndexingBatchSize() {
        return state.indexingBatchSize;
    }
    
    /**
     * 设置索引批处理大小
     * @param size 索引批处理大小
     */
    public void setIndexingBatchSize(int size) {
        state.indexingBatchSize = Math.max(10, size);
        logger.debug("Indexing batch size set to: " + state.indexingBatchSize);
    }
    
    /**
     * 获取缓存开关状态
     * @return 是否启用缓存
     */
    public boolean isCachingEnabled() {
        return state.enableCaching;
    }
    
    /**
     * 设置缓存开关状态
     * @param enabled 是否启用缓存
     */
    public void setCachingEnabled(boolean enabled) {
        state.enableCaching = enabled;
        logger.debug("Caching enabled: " + enabled);
    }
    
    /**
     * 获取缓存过期时间（分钟）
     * @return 缓存过期时间
     */
    public int getCacheExpirationMinutes() {
        return state.cacheExpirationMinutes;
    }
    
    /**
     * 设置缓存过期时间（分钟）
     * @param minutes 缓存过期时间
     */
    public void setCacheExpirationMinutes(int minutes) {
        state.cacheExpirationMinutes = Math.max(5, minutes);
        logger.debug("Cache expiration set to: " + state.cacheExpirationMinutes + " minutes");
    }
    
    /**
     * 获取显示隐藏文件开关状态
     * @return 是否显示隐藏文件
     */
    public boolean isShowHiddenFiles() {
        return state.showHiddenFiles;
    }
    
    /**
     * 设置显示隐藏文件开关状态
     * @param show 是否显示隐藏文件
     */
    public void setShowHiddenFiles(boolean show) {
        state.showHiddenFiles = show;
        logger.debug("Show hidden files: " + show);
    }
    
    /**
     * 获取是否跟随符号链接
     * @return 是否跟随符号链接
     */
    public boolean isFollowSymlinks() {
        return state.followSymlinks;
    }
    
    /**
     * 设置是否跟随符号链接
     * @param follow 是否跟随符号链接
     */
    public void setFollowSymlinks(boolean follow) {
        state.followSymlinks = follow;
        logger.debug("Follow symlinks: " + follow);
    }
    
    /**
     * 获取自定义设置
     * @param key 设置键
     * @return 设置值，如果不存在则返回null
     */
    @Nullable
    public String getCustomSetting(@NotNull String key) {
        return state.customSettings.get(key);
    }
    
    /**
     * 设置自定义设置
     * @param key 设置键
     * @param value 设置值
     */
    public void setCustomSetting(@NotNull String key, @NotNull String value) {
        state.customSettings.put(key, value);
        logger.debug("Custom setting set: " + key + " = " + value);
    }
    
    /**
     * 移除自定义设置
     * @param key 设置键
     * @return 被移除的值，如果不存在则返回null
     */
    @Nullable
    public String removeCustomSetting(@NotNull String key) {
        String removed = state.customSettings.remove(key);
        if (removed != null) {
            logger.debug("Custom setting removed: " + key);
        }
        return removed;
    }
    
    /**
     * 获取所有自定义设置
     * @return 自定义设置映射
     */
    @NotNull
    public Map<String, String> getAllCustomSettings() {
        return new HashMap<>(state.customSettings);
    }
    
    /**
     * 重置所有配置为默认值
     */
    public void resetToDefaults() {
        state.enableIndexing = true;
        state.enableRealTimeMonitoring = true;
        state.enableDependencyAnalysis = true;
        state.maxIndexingDepth = 10;
        state.indexingBatchSize = 100;
        state.enableCaching = true;
        state.cacheExpirationMinutes = 60;
        state.showHiddenFiles = false;
        state.followSymlinks = false;
        state.customSettings.clear();
        logger.info("Configuration reset to defaults");
    }
}