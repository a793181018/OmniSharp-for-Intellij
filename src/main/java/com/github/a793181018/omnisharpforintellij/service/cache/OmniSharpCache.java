package com.github.a793181018.omnisharpforintellij.service.cache;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * OmniSharp缓存接口
 */
public interface OmniSharpCache<K, V> {
    /**
     * 获取缓存值
     */
    @Nullable
    V get(K key);
    
    /**
     * 放入缓存
     */
    void put(K key, V value);
    
    /**
     * 放入缓存并设置过期时间
     */
    void put(K key, V value, long expireTime, TimeUnit timeUnit);
    
    /**
     * 移除缓存
     */
    void remove(K key);
    
    /**
     * 清空缓存
     */
    void clear();
    
    /**
     * 获取缓存大小
     */
    int size();
    
    /**
     * 检查键是否存在
     */
    boolean containsKey(K key);
}