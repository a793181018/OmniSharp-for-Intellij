package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 具有缓存功能的解决方案文件解析器外观
 */
public class CachingSolutionParserFacade implements SolutionParserFacade {
    private final SolutionParserFacade delegate;
    private final Map<String, CacheEntry> cache;
    private final ReadWriteLock lock;
    private final long cacheExpirationTimeMs;

    public CachingSolutionParserFacade(SolutionParserFacade delegate) {
        this(delegate, 5, TimeUnit.MINUTES);
    }

    public CachingSolutionParserFacade(SolutionParserFacade delegate, long duration, TimeUnit unit) {
        this.delegate = delegate;
        this.cache = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
        this.cacheExpirationTimeMs = unit.toMillis(duration);
    }

    @Override
    public SolutionModel parse(Path solutionPath) throws ParseException {
        String key = solutionPath.toString();

        // 尝试从缓存读取
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return entry.getValue();
            }
        } finally {
            lock.readLock().unlock();
        }

        // 缓存未命中或已过期，解析新内容
        SolutionModel model = delegate.parse(solutionPath);

        // 更新缓存
        lock.writeLock().lock();
        try {
            cache.put(key, new CacheEntry(model));
        } finally {
            lock.writeLock().unlock();
        }

        return model;
    }

    @Override
    public CompletableFuture<SolutionModel> parseAsync(Path solutionPath) {
        String key = solutionPath.toString();

        // 尝试从缓存读取
        lock.readLock().lock();
        try {
            CacheEntry entry = cache.get(key);
            if (entry != null && !entry.isExpired()) {
                return CompletableFuture.completedFuture(entry.getValue());
            }
        } finally {
            lock.readLock().unlock();
        }

        // 缓存未命中或已过期，异步解析
        return delegate.parseAsync(solutionPath).thenApply(model -> {
            // 更新缓存
            lock.writeLock().lock();
            try {
                cache.put(key, new CacheEntry(model));
            } finally {
                lock.writeLock().unlock();
            }
            return model;
        });
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 获取当前缓存大小
     * @return 缓存中的条目数
     */
    public int getCacheSize() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 缓存条目类
     */
    private class CacheEntry {
        private final SolutionModel value;
        private final long timestamp;

        public CacheEntry(SolutionModel value) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
        }

        public SolutionModel getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > cacheExpirationTimeMs;
        }
    }
}