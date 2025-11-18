package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 诊断结果缓存管理类
 * 用于缓存诊断结果，避免重复请求
 */
public class DiagnosticsCache implements Disposable {
    private static final Logger LOG = Logger.getInstance(DiagnosticsCache.class);
    
    // 诊断缓存，key为文件路径
    private final Map<String, List<Diagnostic>> diagnosticsCache = new ConcurrentHashMap<>();
    
    // 缓存过期时间（毫秒）
    private static final long CACHE_EXPIRATION_MS = 30000; // 30秒
    
    // 缓存条目，包含诊断结果和缓存时间
    private static class CacheEntry {
        final List<Diagnostic> diagnostics;
        final long timestamp;
        
        CacheEntry(@NotNull List<Diagnostic> diagnostics) {
            this.diagnostics = Collections.unmodifiableList(new ArrayList<>(diagnostics));
            this.timestamp = System.currentTimeMillis();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_EXPIRATION_MS;
        }
    }
    
    // 使用文件路径作为key的缓存
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    /**
     * 获取缓存的诊断结果
     * @param filePath 文件路径
     * @return 诊断结果列表，如果缓存不存在或已过期则返回null
     */
    @Nullable
    public List<Diagnostic> getDiagnostics(@NotNull String filePath) {
        CacheEntry entry = cache.get(filePath);
        if (entry == null) {
            LOG.debug("No cache found for file: " + filePath);
            return null;
        }
        
        if (entry.isExpired()) {
            LOG.debug("Cache expired for file: " + filePath);
            cache.remove(filePath);
            return null;
        }
        
        LOG.debug("Returning cached diagnostics for file: " + filePath);
        return entry.diagnostics;
    }
    
    /**
     * 存储诊断结果到缓存
     * @param filePath 文件路径
     * @param diagnostics 诊断结果列表
     */
    public void putDiagnostics(@NotNull String filePath, @NotNull List<Diagnostic> diagnostics) {
        cache.put(filePath, new CacheEntry(diagnostics));
        LOG.debug("Cached " + diagnostics.size() + " diagnostics for file: " + filePath);
    }
    
    /**
     * 移除指定文件的缓存
     * @param filePath 文件路径
     */
    public void removeDiagnostics(@NotNull String filePath) {
        CacheEntry removed = cache.remove(filePath);
        if (removed != null) {
            LOG.debug("Removed cache for file: " + filePath);
        }
    }
    
    /**
     * 检查指定文件是否有缓存
     * @param filePath 文件路径
     * @return 是否有缓存
     */
    public boolean hasCache(@NotNull String filePath) {
        CacheEntry entry = cache.get(filePath);
        return entry != null && !entry.isExpired();
    }
    
    /**
     * 获取缓存大小
     * @return 缓存中的文件数量
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * 获取所有缓存的文件路径
     * @return 文件路径集合
     */
    @NotNull
    public Set<String> getCachedFiles() {
        return new HashSet<>(cache.keySet());
    }
    
    /**
     * 清除所有缓存
     */
    public void clear() {
        int size = cache.size();
        cache.clear();
        LOG.info("Cleared diagnostics cache, removed " + size + " entries");
    }
    
    /**
     * 清除过期的缓存
     * @return 清除的缓存数量
     */
    public int clearExpired() {
        int removedCount = 0;
        Iterator<Map.Entry<String, CacheEntry>> iterator = cache.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
                LOG.debug("Removed expired cache for file: " + entry.getKey());
            }
        }
        
        if (removedCount > 0) {
            LOG.info("Cleared " + removedCount + " expired cache entries");
        }
        
        return removedCount;
    }
    
    /**
     * 更新缓存中的诊断结果
     * @param filePath 文件路径
     * @param diagnostics 新的诊断结果列表
     * @return 是否成功更新
     */
    public boolean updateDiagnostics(@NotNull String filePath, @NotNull List<Diagnostic> diagnostics) {
        if (hasCache(filePath)) {
            putDiagnostics(filePath, diagnostics);
            return true;
        }
        return false;
    }
    
    /**
     * 获取缓存统计信息
     * @return 统计信息映射
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalEntries", cache.size());
        stats.put("totalFiles", cache.size());
        
        long totalDiagnostics = cache.values().stream()
            .mapToLong(entry -> entry.diagnostics.size())
            .sum();
        stats.put("totalDiagnostics", totalDiagnostics);
        
        int expiredCount = 0;
        for (CacheEntry entry : cache.values()) {
            if (entry.isExpired()) {
                expiredCount++;
            }
        }
        stats.put("expiredEntries", expiredCount);
        
        return stats;
    }
    
    @Override
    public void dispose() {
        clear();
        LOG.info("DiagnosticsCache disposed");
    }
}