package com.github.a793181018.omnisharpforintellij.service.cache;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于LRU（最近最少使用）策略的缓存实现
 */
public class LRUCache<K, V> implements OmniSharpCache<K, V> {
    private final int capacity;
    private final LinkedHashMap<K, CacheEntry<V>> cacheMap;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public LRUCache(int capacity) {
        this.capacity = capacity;
        // 第三个参数true表示按访问顺序排序
        this.cacheMap = new LinkedHashMap<K, CacheEntry<V>>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, CacheEntry<V>> eldest) {
                return size() > LRUCache.this.capacity;
            }
        };
    }
    
    @Override
    @Nullable
    public V get(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cacheMap.get(key);
            if (entry == null) {
                return null;
            }
            
            // 检查是否过期
            if (entry.isExpired()) {
                // 释放读锁，获取写锁来移除过期项
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    // 双重检查
                    if (cacheMap.containsKey(key) && cacheMap.get(key).isExpired()) {
                        cacheMap.remove(key);
                    }
                    return null;
                } finally {
                    // 降级锁：获取读锁，然后释放写锁
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return entry.getValue();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public void put(K key, V value) {
        put(key, value, 0, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void put(K key, V value, long expireTime, TimeUnit timeUnit) {
        lock.writeLock().lock();
        try {
            long expireTimestamp = expireTime > 0 ? System.currentTimeMillis() + timeUnit.toMillis(expireTime) : 0;
            cacheMap.put(key, new CacheEntry<>(value, expireTimestamp));
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void remove(K key) {
        lock.writeLock().lock();
        try {
            cacheMap.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            cacheMap.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public int size() {
        lock.readLock().lock();
        try {
            // 清理过期项并返回有效大小
            int validSize = 0;
            for (Map.Entry<K, CacheEntry<V>> entry : cacheMap.entrySet()) {
                if (!entry.getValue().isExpired()) {
                    validSize++;
                }
            }
            return validSize;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = cacheMap.get(key);
            if (entry == null) {
                return false;
            }
            
            if (entry.isExpired()) {
                // 过期了就移除
                lock.readLock().unlock();
                lock.writeLock().lock();
                try {
                    if (cacheMap.containsKey(key) && cacheMap.get(key).isExpired()) {
                        cacheMap.remove(key);
                    }
                    return false;
                } finally {
                    lock.readLock().lock();
                    lock.writeLock().unlock();
                }
            }
            return true;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 缓存条目，包含值和过期时间
     */
    private static class CacheEntry<V> {
        private final V value;
        private final long expireTimestamp;
        
        public CacheEntry(V value, long expireTimestamp) {
            this.value = value;
            this.expireTimestamp = expireTimestamp;
        }
        
        public V getValue() {
            return value;
        }
        
        public boolean isExpired() {
            return expireTimestamp > 0 && System.currentTimeMillis() > expireTimestamp;
        }
    }
}