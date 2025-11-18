package com.github.a793181018.omnisharpforintellij.editor.completion.cache;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 内存中的代码补全缓存实现，使用LRU策略管理缓存
 */
public class MemoryCompletionCache implements CompletionCache {
    private static final Logger LOG = Logger.getInstance(MemoryCompletionCache.class);
    
    // 默认最大缓存大小
    private static final int DEFAULT_MAX_SIZE = 1000;
    // 默认过期时间（毫秒）
    private static final long DEFAULT_EXPIRATION_TIME = 5 * 60 * 1000; // 5分钟
    
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final LinkedHashMap<CacheKey, CacheEntry> cache;
    private final Map<String, Set<CacheKey>> fileToKeysMap;
    
    private int maxSize;
    private long expirationTime;
    
    /**
     * 缓存键，包含文件路径和位置信息
     */
    private static class CacheKey {
        private final String filePath;
        private final int line;
        private final int column;
        private final String prefix;
        private final long hashCode;
        
        public CacheKey(@NotNull CompletionRequest request) {
            this.filePath = request.getFileName();
            this.line = request.getLine();
            this.column = request.getColumn();
            this.prefix = request.getContext().getPrefix() != null ? request.getContext().getPrefix() : "";
            
            // 预计算哈希码，提高性能
            int result = filePath.hashCode();
            result = 31 * result + line;
            result = 31 * result + column;
            result = 31 * result + prefix.hashCode();
            this.hashCode = result & 0x7FFFFFFFL;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            CacheKey cacheKey = (CacheKey) o;
            return line == cacheKey.line && 
                   column == cacheKey.column &&
                   Objects.equals(filePath, cacheKey.filePath) &&
                   Objects.equals(prefix, cacheKey.prefix);
        }
        
        @Override
        public int hashCode() {
            return (int) hashCode;
        }
        
        public String getFilePath() {
            return filePath;
        }
    }
    
    /**
     * 缓存条目，包含响应和创建时间
     */
    private static class CacheEntry {
        private final CompletionResponse response;
        private final long creationTime;
        
        public CacheEntry(@NotNull CompletionResponse response) {
            this.response = response;
            this.creationTime = System.currentTimeMillis();
        }
        
        public CompletionResponse getResponse() {
            return response;
        }
        
        public boolean isExpired(long expirationTime) {
            return System.currentTimeMillis() - creationTime > expirationTime;
        }
    }
    
    public MemoryCompletionCache() {
        this(DEFAULT_MAX_SIZE, DEFAULT_EXPIRATION_TIME);
    }
    
    public MemoryCompletionCache(int maxSize, long expirationTime) {
        this.maxSize = Math.max(100, maxSize); // 最小100个缓存项
        this.expirationTime = Math.max(60 * 1000, expirationTime); // 最小1分钟
        
        // 创建LinkedHashMap作为LRU缓存
        this.cache = new LinkedHashMap<CacheKey, CacheEntry>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<CacheKey, CacheEntry> eldest) {
                return size() > MemoryCompletionCache.this.maxSize;
            }
        };
        
        this.fileToKeysMap = new ConcurrentHashMap<>();
    }
    
    @Override
    public void put(@NotNull CompletionRequest request, @NotNull CompletionResponse response) {
        if (request.getFileName() == null) {
            return;
        }
        
        final CacheKey key = new CacheKey(request);
        final CacheEntry entry = new CacheEntry(response);
        
        try {
            lock.writeLock().lock();
            
            // 添加到缓存
            cache.put(key, entry);
            
            // 更新文件到缓存键的映射
            String filePath = request.getFileName();
            fileToKeysMap.computeIfAbsent(filePath, k -> ConcurrentHashMap.newKeySet()).add(key);
        } finally {
            // 确保总是释放写锁
            lock.writeLock().unlock();
        }
    }
    
    @Override
    @Nullable
    public CompletionResponse get(@NotNull CompletionRequest request) {
        if (request.getFileName() == null) {
            return null;
        }
        
        final CacheKey key = new CacheKey(request);
        
        try {
            lock.readLock().lock();
            final CacheEntry entry = cache.get(key);
            
            if (entry == null) {
                return null;
            }
            
            // 检查是否过期
            if (entry.isExpired(expirationTime)) {
                // 过期了，在写锁下删除
                lock.readLock().unlock();
                try {
                    lock.writeLock().lock();
                    // 再次检查，防止在锁切换期间被其他线程修改
                    if (cache.containsKey(key)) {
                        removeEntry(key);
                    }
                    return null;
                } finally {
                    lock.writeLock().unlock();
                }
            }
            
            return entry.getResponse();
        } finally {
            // 直接获取写锁，不再检查读锁状态
                lock.readLock().unlock();
            }
        }
    
    @Override
    public void clearFileCache(@NotNull String filePath) {
        try {
            lock.writeLock().lock();
            
            Set<CacheKey> keys = fileToKeysMap.remove(filePath);
            if (keys != null) {
                for (CacheKey key : keys) {
                    cache.remove(key);
                }
                LOG.debug("Cleared cache for file: " + filePath + ", removed " + keys.size() + " entries");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clearAll() {
        try {
            lock.writeLock().lock();
            cache.clear();
            fileToKeysMap.clear();
            LOG.debug("Cleared all completion cache");
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int size() {
        try {
            lock.readLock().lock();
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean contains(@NotNull CompletionRequest request) {
        final CacheKey key = new CacheKey(request);
        
        try {
            lock.readLock().lock();
            final CacheEntry entry = cache.get(key);
            
            if (entry != null && entry.isExpired(expirationTime)) {
                // 过期了，在写锁下删除
                lock.readLock().unlock();
                try {
                    lock.writeLock().lock();
                    // 再次检查，防止在锁切换期间被其他线程修改
                    if (cache.containsKey(key)) {
                        removeEntry(key);
                    }
                    return false;
                } finally {
                    lock.writeLock().unlock();
                }
            }
            
            return entry != null;
        } finally {
            // 直接释放读锁
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void setMaxSize(int maxSize) {
        this.maxSize = Math.max(100, maxSize);
        LOG.debug("Set max cache size to: " + this.maxSize);
    }
    
    @Override
    public void setExpirationTime(long expirationTime) {
        this.expirationTime = Math.max(60 * 1000, expirationTime);
        LOG.debug("Set cache expiration time to: " + this.expirationTime + "ms");
    }
    
    /**
     * 移除单个缓存条目
     */
    private void removeEntry(@NotNull CacheKey key) {
        cache.remove(key);
        
        // 从文件到缓存键的映射中移除
        Set<CacheKey> keys = fileToKeysMap.get(key.getFilePath());
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                fileToKeysMap.remove(key.getFilePath());
            }
        }
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        try {
            lock.writeLock().lock();
            
            List<CacheKey> expiredKeys = new ArrayList<>();
            
            for (Map.Entry<CacheKey, CacheEntry> entry : cache.entrySet()) {
                if (entry.getValue().isExpired(expirationTime)) {
                    expiredKeys.add(entry.getKey());
                }
            }
            
            for (CacheKey key : expiredKeys) {
                removeEntry(key);
            }
            
            if (!expiredKeys.isEmpty()) {
                LOG.debug("Cleaned up " + expiredKeys.size() + " expired cache entries");
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}