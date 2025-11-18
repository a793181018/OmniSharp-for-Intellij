package com.github.a793181018.omnisharpforintellij.editor.completion.config;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 代码补全配置管理器，负责管理配置的访问和通知机制
 */
@Service(Service.Level.PROJECT)
public class CompletionConfigManager {
    
    private final Project project;
    private final CompletionConfig config;
    private final Set<ConfigChangeListener> listeners;
    private final ReadWriteLock lock;
    
    public CompletionConfigManager(@NotNull Project project) {
        this.project = project;
        this.config = project.getService(CompletionConfigImpl.class);
        this.listeners = new HashSet<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    /**
     * 获取CompletionConfigManager实例
     */
    @NotNull
    public static CompletionConfigManager getInstance(@NotNull Project project) {
        return project.getService(CompletionConfigManager.class);
    }
    
    /**
     * 获取当前配置
     */
    @NotNull
    public CompletionConfig getConfig() {
        try {
            lock.readLock().lock();
            return config.copy();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 应用配置更改
     */
    public void applyConfig(@NotNull CompletionConfig newConfig) {
        try {
            lock.writeLock().lock();
            
            // 应用配置更改
            if (config instanceof CompletionConfigImpl) {
                CompletionConfigImpl impl = (CompletionConfigImpl) config;
                impl.setCompletionEnabled(newConfig.isCompletionEnabled());
                impl.setSmartCompletionEnabled(newConfig.isSmartCompletionEnabled());
                impl.setRequestTimeoutMs(newConfig.getRequestTimeoutMs());
                impl.setMaxCompletionItems(newConfig.getMaxCompletionItems());
                impl.setCacheEnabled(newConfig.isCacheEnabled());
                impl.setCacheMaxSize(newConfig.getCacheMaxSize());
                impl.setCacheExpirationTimeMs(newConfig.getCacheExpirationTimeMs());
                impl.setShowTypeInfo(newConfig.isShowTypeInfo());
                impl.setAutoPopupEnabled(newConfig.isAutoPopupEnabled());
                impl.setAutoPopupDelayMs(newConfig.getAutoPopupDelayMs());
                impl.setCompletionSortingEnabled(newConfig.isCompletionSortingEnabled());
            }
            
            // 通知监听器
            notifyConfigChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 重置配置为默认值
     */
    public void resetToDefaults() {
        try {
            lock.writeLock().lock();
            config.resetToDefaults();
            notifyConfigChanged();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 添加配置更改监听器
     */
    public void addConfigChangeListener(@NotNull ConfigChangeListener listener) {
        try {
            lock.writeLock().lock();
            listeners.add(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 移除配置更改监听器
     */
    public void removeConfigChangeListener(@NotNull ConfigChangeListener listener) {
        try {
            lock.writeLock().lock();
            listeners.remove(listener);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 通知配置更改
     */
    private void notifyConfigChanged() {
        // 创建监听器副本，避免在通知过程中修改列表
        Set<ConfigChangeListener> listenersCopy;
        try {
            lock.readLock().lock();
            listenersCopy = new HashSet<>(listeners);
        } finally {
            lock.readLock().unlock();
        }
        
        // 通知所有监听器
        for (ConfigChangeListener listener : listenersCopy) {
            try {
                listener.onConfigChanged(config.copy());
            } catch (Exception e) {
                // 记录异常但不中断通知流程
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 配置更改监听器接口
     */
    public interface ConfigChangeListener {
        /**
         * 当配置更改时调用
         */
        void onConfigChanged(@NotNull CompletionConfig newConfig);
    }
    
    /**
     * 检查配置是否有效
     */
    public boolean isValidConfig() {
        try {
            lock.readLock().lock();
            return config != null;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取配置摘要
     */
    @NotNull
    public String getConfigSummary() {
        try {
            lock.readLock().lock();
            return "CompletionConfig{" +
                   "enabled=" + config.isCompletionEnabled() + ", " +
                   "smart=" + config.isSmartCompletionEnabled() + ", " +
                   "cache=" + config.isCacheEnabled() + ", " +
                   "timeout=" + config.getRequestTimeoutMs() + "ms" +
                   '}';
        } finally {
            lock.readLock().unlock();
        }
    }
}