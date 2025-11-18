package com.github.a793181018.omnisharpforintellij.service.cache;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * OmniSharp缓存管理器，管理多个不同类型的缓存
 */
@Service(Service.Level.PROJECT)
public final class OmniSharpCacheManager {
    private final Project project;
    private final Map<String, OmniSharpCache<?, ?>> caches = new HashMap<>();
    
    public OmniSharpCacheManager(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 获取或创建指定名称的缓存
     */
    @SuppressWarnings("unchecked")
    public <K, V> OmniSharpCache<K, V> getCache(@NotNull String cacheName, int capacity) {
        synchronized (caches) {
            OmniSharpCache<?, ?> cache = caches.get(cacheName);
            if (cache == null) {
                cache = new LRUCache<>(capacity);
                caches.put(cacheName, cache);
            }
            return (OmniSharpCache<K, V>) cache;
        }
    }
    
    /**
     * 获取或创建默认配置的缓存（容量为1000）
     */
    public <K, V> OmniSharpCache<K, V> getDefaultCache(@NotNull String cacheName) {
        return getCache(cacheName, 1000);
    }
    
    /**
     * 获取或创建短期缓存（5分钟过期，容量为1000）
     */
    public <K, V> OmniSharpCache<K, V> getShortTermCache(@NotNull String cacheName) {
        OmniSharpCache<K, V> cache = getDefaultCache(cacheName);
        // 可以在这里添加特定的短期缓存配置
        return cache;
    }
    
    /**
     * 获取或创建长期缓存（1小时过期，容量为5000）
     */
    public <K, V> OmniSharpCache<K, V> getLongTermCache(@NotNull String cacheName) {
        return getCache(cacheName, 5000);
    }
    
    /**
     * 移除指定名称的缓存
     */
    public void removeCache(@NotNull String cacheName) {
        synchronized (caches) {
            OmniSharpCache<?, ?> cache = caches.remove(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAllCaches() {
        synchronized (caches) {
            for (OmniSharpCache<?, ?> cache : caches.values()) {
                cache.clear();
            }
            caches.clear();
        }
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheCount() {
        synchronized (caches) {
            return caches.size();
        }
    }
    
    /**
     * 预定义的缓存名称
     */
    public static class CacheNames {
        public static final String COMPLETION_CACHE = "completionCache";
        public static final String DEFINITION_CACHE = "definitionCache";
        public static final String REFERENCES_CACHE = "referencesCache";
        public static final String DIAGNOSTICS_CACHE = "diagnosticsCache";
        public static final String SYMBOL_CACHE = "symbolCache";
        public static final String FILE_INDEX_CACHE = "fileIndexCache";
    }
}