package com.github.a793181018.omnisharpforintellij.editor.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口，用于管理编辑器功能的缓存。
 * 提供通用的缓存操作方法，支持不同类型的数据缓存。
 */
public interface CacheService {
    /**
     * 获取缓存项
     * @param key 缓存键
     * @param <T> 缓存值的类型
     * @return 缓存的对象，如果不存在则返回null
     */
    @Nullable
    <T> T get(@NotNull CacheKey<T> key);
    
    /**
     * 存储缓存项
     * @param key 缓存键
     * @param value 要缓存的对象
     * @param <T> 缓存值的类型
     */
    <T> void put(@NotNull CacheKey<T> key, @NotNull T value);
    
    /**
     * 存储带有过期时间的缓存项
     * @param key 缓存键
     * @param value 要缓存的对象
     * @param expirationTime 过期时间
     * @param timeUnit 时间单位
     * @param <T> 缓存值的类型
     */
    <T> void put(@NotNull CacheKey<T> key, @NotNull T value, long expirationTime, @NotNull TimeUnit timeUnit);
    
    /**
     * 移除缓存项
     * @param key 缓存键
     * @param <T> 缓存值的类型
     */
    <T> void remove(@NotNull CacheKey<T> key);
    
    /**
     * 清理所有缓存
     */
    void clear();
    
    /**
     * 根据前缀清理缓存
     * @param prefix 缓存键前缀
     */
    void clear(@NotNull String prefix);
    
    /**
     * 检查缓存是否包含指定键
     * @param key 缓存键
     * @param <T> 缓存值的类型
     * @return 如果缓存包含该键则返回true
     */
    <T> boolean contains(@NotNull CacheKey<T> key);
    
    /**
     * 获取缓存大小
     * @return 缓存中的项目数量
     */
    int size();
    
    /**
     * 获取缓存大小
     * @param prefix 缓存键前缀
     * @return 指定前缀的缓存项目数量
     */
    int size(@NotNull String prefix);
    
    /**
     * 缓存键接口，用于标识缓存项
     * @param <T> 缓存值的类型
     */
    interface CacheKey<T> extends Serializable {
        /**
         * 获取缓存键的字符串表示
         * @return 缓存键字符串
         */
        @NotNull
        String getKey();
        
        /**
         * 获取缓存键的前缀
         * @return 缓存键前缀
         */
        @NotNull
        String getPrefix();
        
        /**
         * 获取缓存值的类型
         * @return 缓存值的类对象
         */
        @NotNull
        Class<T> getValueType();
    }
}