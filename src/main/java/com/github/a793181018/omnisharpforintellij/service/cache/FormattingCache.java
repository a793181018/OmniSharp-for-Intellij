/**
 * 格式化缓存服务
 */
package com.github.a793181018.omnisharpforintellij.service.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FormattingCache {
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    
    private static class CacheEntry {
        final int contentHash;
        final String formattedContent;
        final long timestamp;
        
        CacheEntry(int contentHash, String formattedContent) {
            this.contentHash = contentHash;
            this.formattedContent = formattedContent;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * 获取缓存的格式化内容
     * @param filePath 文件路径
     * @param contentHash 内容哈希值
     * @return 格式化内容，如果不存在则返回null
     */
    public String getFormattedContent(String filePath, int contentHash) {
        CacheEntry entry = cache.get(filePath);
        if (entry != null && entry.contentHash == contentHash) {
            // 检查缓存是否过期（5分钟）
            if (System.currentTimeMillis() - entry.timestamp < 5 * 60 * 1000) {
                return entry.formattedContent;
            } else {
                // 过期则移除
                cache.remove(filePath);
            }
        }
        return null;
    }
    
    /**
     * 存储格式化内容到缓存
     * @param filePath 文件路径
     * @param contentHash 内容哈希值
     * @param formattedContent 格式化内容
     */
    public void putFormattedContent(String filePath, int contentHash, String formattedContent) {
        // 如果缓存已满，移除最旧的条目
        if (cache.size() >= MAX_CACHE_SIZE) {
            removeOldestEntry();
        }
        
        cache.put(filePath, new CacheEntry(contentHash, formattedContent));
    }
    
    /**
     * 清除缓存
     */
    public void clear() {
        cache.clear();
    }
    
    /**
     * 移除最旧的缓存条目
     */
    private void removeOldestEntry() {
        String oldestKey = null;
        long oldestTimestamp = Long.MAX_VALUE;
        
        for (Map.Entry<String, CacheEntry> entry : cache.entrySet()) {
            if (entry.getValue().timestamp < oldestTimestamp) {
                oldestTimestamp = entry.getValue().timestamp;
                oldestKey = entry.getKey();
            }
        }
        
        if (oldestKey != null) {
            cache.remove(oldestKey);
        }
    }
}