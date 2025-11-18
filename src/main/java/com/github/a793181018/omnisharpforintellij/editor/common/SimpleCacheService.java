package com.github.a793181018.omnisharpforintellij.editor.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * CacheService的简单实现类，提供基本的内存缓存功能。
 */
public class SimpleCacheService implements CacheService {
    
    // 缓存数据存储
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();
    
    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T get(@NotNull CacheKey<T> key) {
        String keyStr = key.getKey();
        CacheEntry<?> entry = cache.get(keyStr);
        
        if (entry == null) {
            return null;
        }
        
        // 检查是否过期
        if (entry.hasExpired()) {
            cache.remove(keyStr);
            return null;
        }
        
        return (T) entry.getValue();
    }
    
    @Override
    public <T> void put(@NotNull CacheKey<T> key, @NotNull T value) {
        put(key, value, 0, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public <T> void put(@NotNull CacheKey<T> key, @NotNull T value, long expirationTime, @NotNull TimeUnit timeUnit) {
        long expireAt = expirationTime > 0 ? 
            System.currentTimeMillis() + timeUnit.toMillis(expirationTime) : 0;
        
        cache.put(key.getKey(), new CacheEntry<>(value, expireAt));
    }
    
    @Override
    public <T> void remove(@NotNull CacheKey<T> key) {
        cache.remove(key.getKey());
    }
    
    @Override
    public void clear() {
        cache.clear();
    }
    
    @Override
    public void clear(@NotNull String prefix) {
        cache.keySet().removeIf(key -> key.startsWith(prefix));
    }
    
    @Override
    public <T> boolean contains(@NotNull CacheKey<T> key) {
        return get(key) != null;
    }
    
    @Override
    public int size() {
        // 先清理过期项
        cleanExpiredEntries();
        return cache.size();
    }
    
    @Override
    public int size(@NotNull String prefix) {
        // 先清理过期项
        cleanExpiredEntries();
        return (int) cache.keySet().stream().filter(key -> key.startsWith(prefix)).count();
    }
    
    /**
     * 获取缓存值，如果不存在则使用supplier生成并缓存
     * @param key 缓存键
     * @param supplier 生成缓存值的supplier
     * @param <T> 缓存值类型
     * @return 缓存值
     */
    @NotNull
    public <T> T getOrPut(@NotNull CacheKey<T> key, @NotNull Supplier<T> supplier) {
        T value = get(key);
        if (value == null) {
            value = supplier.get();
            put(key, value);
        }
        return value;
    }
    
    /**
     * 清理过期的缓存项
     */
    private void cleanExpiredEntries() {
        long now = System.currentTimeMillis();
        cache.entrySet().removeIf(entry -> {
            CacheEntry<?> cacheEntry = entry.getValue();
            return cacheEntry.hasExpired(now);
        });
    }
    
    /**
     * 缓存条目内部类，包含缓存值和过期时间
     */
    private static class CacheEntry<T> {
        private final T value;
        private final long expireAt; // 0表示永不过期
        
        CacheEntry(T value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
        
        T getValue() {
            return value;
        }
        
        boolean hasExpired() {
            return hasExpired(System.currentTimeMillis());
        }
        
        boolean hasExpired(long now) {
            return expireAt > 0 && now > expireAt;
        }
    }
    
    /**
     * 简单的缓存键实现
     */
    public static class SimpleCacheKey<T> implements CacheKey<T> {
        private final String prefix;
        private final String key;
        private final Class<T> valueType;
        
        public SimpleCacheKey(@NotNull String prefix, @NotNull String key, @NotNull Class<T> valueType) {
            this.prefix = prefix;
            this.key = key;
            this.valueType = valueType;
        }
        
        @Override
        @NotNull
        public String getKey() {
            return prefix + ":" + key;
        }
        
        @Override
        @NotNull
        public String getPrefix() {
            return prefix;
        }
        
        @Override
        @NotNull
        public Class<T> getValueType() {
            return valueType;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SimpleCacheKey<?> that = (SimpleCacheKey<?>) o;
            return getKey().equals(that.getKey()) && valueType.equals(that.valueType);
        }
        
        @Override
        public int hashCode() {
            int result = getKey().hashCode();
            result = 31 * result + valueType.hashCode();
            return result;
        }
    }
}