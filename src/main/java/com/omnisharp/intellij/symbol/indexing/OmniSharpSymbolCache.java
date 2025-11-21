package com.omnisharp.intellij.symbol.indexing;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.util.io.DataExternalizer;
import com.intellij.util.io.EnumeratorStringDescriptor;
import com.intellij.util.io.PersistentHashMap;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.zip.CRC32;

/**
 * 符号缓存管理器
 * 负责缓存符号索引结果，提供内存缓存和磁盘持久化功能
 */
public class OmniSharpSymbolCache {
    private static final Logger LOG = Logger.getInstance(OmniSharpSymbolCache.class);
    
    // 内存缓存项
    private static class CacheEntry <T> {
        private final T value;
        private final long timestamp;
        private final long expirationTime;
        private final String version;

        CacheEntry(T value, long expirationTime, String version) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.expirationTime = expirationTime;
            this.version = version;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        T getValue() {
            return value;
        }

        String getVersion() {
            return version;
        }
    }

    // 内存缓存
    private final Map<String, CacheEntry<?>> memoryCache;
    
    // 符号索引的持久化缓存
    private PersistentHashMap<String, SymbolCollectionResult> symbolCache;
    
    // 缓存配置
    private final CacheConfig config;
    
    // 缓存统计信息
    private final CacheStats stats;
    
    // 缓存清理调度器
    private final ScheduledCleanupTask cleanupTask;

    /**
     * 缓存配置类
     */
    public static class CacheConfig {
        private long defaultExpiration = TimeUnit.HOURS.toMillis(1);
        private long maxMemorySize = 100 * 1024 * 1024; // 100MB
        private int maxEntries = 10000;
        private String cacheDirectory = ".symbolcache";
        private boolean diskCacheEnabled = true;
        private long cleanupInterval = TimeUnit.MINUTES.toMillis(30);
        private double evictionRatio = 0.2; // 每次清理20%的缓存

        // Getters and setters
        public long getDefaultExpiration() { return defaultExpiration; }
        public void setDefaultExpiration(long defaultExpiration) { this.defaultExpiration = defaultExpiration; }

        public long getMaxMemorySize() { return maxMemorySize; }
        public void setMaxMemorySize(long maxMemorySize) { this.maxMemorySize = maxMemorySize; }

        public int getMaxEntries() { return maxEntries; }
        public void setMaxEntries(int maxEntries) { this.maxEntries = maxEntries; }

        public String getCacheDirectory() { return cacheDirectory; }
        public void setCacheDirectory(String cacheDirectory) { this.cacheDirectory = cacheDirectory; }

        public boolean isDiskCacheEnabled() { return diskCacheEnabled; }
        public void setDiskCacheEnabled(boolean diskCacheEnabled) { this.diskCacheEnabled = diskCacheEnabled; }

        public long getCleanupInterval() { return cleanupInterval; }
        public void setCleanupInterval(long cleanupInterval) { this.cleanupInterval = cleanupInterval; }

        public double getEvictionRatio() { return evictionRatio; }
        public void setEvictionRatio(double evictionRatio) { this.evictionRatio = evictionRatio; }
    }

    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private final AtomicLong hits = new AtomicLong(0);
        private final AtomicLong misses = new AtomicLong(0);
        private final AtomicLong evictions = new AtomicLong(0);
        private final AtomicLong expirations = new AtomicLong(0);

        public long getHits() { return hits.get(); }
        public long getMisses() { return misses.get(); }
        public long getEvictions() { return evictions.get(); }
        public long getExpirations() { return expirations.get(); }
        
        public double getHitRate() {
            long total = hits.get() + misses.get();
            return total > 0 ? (double) hits.get() / total : 0.0;
        }

        public void recordHit() { hits.incrementAndGet(); }
        public void recordMiss() { misses.incrementAndGet(); }
        public void recordEviction() { evictions.incrementAndGet(); }
        public void recordExpiration() { expirations.incrementAndGet(); }
    }

    /**
     * 符号收集结果的外部化器
     */
    private static class SymbolCollectionResultExternalizer implements DataExternalizer<SymbolCollectionResult> {
        @Override
        public void save(java.io.DataOutput out, SymbolCollectionResult value) throws IOException {
            // 保存项目数量
            out.writeInt(value.getSymbols().size());
            
            // 保存每个项目的符号
            for (Map.Entry<String, List<OmniSharpSymbol>> entry : value.getSymbols().entrySet()) {
                out.writeUTF(entry.getKey());
                
                List<OmniSharpSymbol> symbols = entry.getValue();
                out.writeInt(symbols.size());
                
                for (OmniSharpSymbol symbol : symbols) {
                    // 这里简化实现，实际应用中需要完善符号的序列化
                    out.writeUTF(symbol.getClass().getName());
                    out.writeUTF(symbol.getName());
                    out.writeUTF(symbol.getFullyQualifiedName() != null ? symbol.getFullyQualifiedName() : "");
                    out.writeInt(symbol.getKind().ordinal());
                    out.writeUTF(symbol.getFilePath().toString());
                    out.writeUTF(symbol.getProjectName());
                    // 写入其他属性...
                }
            }
            
            // 保存错误文件列表
            out.writeInt(value.getErrors().size());
            for (String errorFile : value.getErrors()) {
                out.writeUTF(errorFile);
            }
            
            // 保存符号总数
            out.writeInt(value.getTotalSymbols());
        }

        @Override
        public SymbolCollectionResult read(java.io.DataInput in) throws IOException {
            Map<String, List<OmniSharpSymbol>> symbols = new HashMap<>();
            List<String> errorFiles = new ArrayList<>();
            int totalSymbols = 0;
            
            // 读取项目数量
            int projectCount = in.readInt();
            
            // 读取每个项目的符号
            for (int i = 0; i < projectCount; i++) {
                String projectName = in.readUTF();
                int symbolCount = in.readInt();
                List<OmniSharpSymbol> projectSymbols = new ArrayList<>();
                
                for (int j = 0; j < symbolCount; j++) {
                    // 这里简化实现，实际应用中需要完善符号的反序列化
                    String className = in.readUTF();
                    String name = in.readUTF();
                    String fullyQualifiedName = in.readUTF();
                    if (fullyQualifiedName.isEmpty()) fullyQualifiedName = null;
                    OmniSharpSymbolKind kind = OmniSharpSymbolKind.values()[in.readInt()];
                    Path filePath = Paths.get(in.readUTF());
                    String project = in.readUTF();
                    
                    // 创建符号实例（简化版）
                    OmniSharpSymbol symbol = new OmniSharpSymbol(name, fullyQualifiedName, kind, filePath, 0, 0, 0, 0, project) { };
                    projectSymbols.add(symbol);
                }
                
                symbols.put(projectName, projectSymbols);
                totalSymbols += symbolCount;
            }
            
            // 读取错误文件列表
            int errorCount = in.readInt();
            for (int i = 0; i < errorCount; i++) {
                errorFiles.add(in.readUTF()); // 直接使用字符串，不转换为Path
            }
            
            // 读取符号总数
            totalSymbols = in.readInt();
            
            return new SymbolCollectionResult(symbols, errorFiles);
        }
    }

    /**
     * 定时清理任务
     */
    private class ScheduledCleanupTask extends Thread {
        private volatile boolean running = true;
        private final long interval;

        ScheduledCleanupTask(long interval) {
            super("SymbolCache-Cleanup");
            this.interval = interval;
            setDaemon(true);
        }

        @Override
        public void run() {
            while (running) {
                try {
                    Thread.sleep(interval);
                    cleanupExpiredEntries();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        public void stopTask() {
            running = false;
            interrupt();
        }
    }

    public OmniSharpSymbolCache() {
        this(new CacheConfig());
    }

    public OmniSharpSymbolCache(CacheConfig config) {
        this.config = config != null ? config : new CacheConfig();
        this.memoryCache = new ConcurrentHashMap<>();
        this.stats = new CacheStats();
        this.cleanupTask = new ScheduledCleanupTask(config.getCleanupInterval());
        
        // 初始化磁盘缓存
        if (config.isDiskCacheEnabled()) {
            try {
                initDiskCache();
            } catch (IOException e) {
                LOG.warn("Failed to initialize disk cache", e);
            }
        }
        
        // 启动清理任务
        cleanupTask.start();
    }

    /**
     * 初始化磁盘缓存
     */
    private void initDiskCache() throws IOException {
        Path cacheDir = Paths.get(config.getCacheDirectory());
        if (!Files.exists(cacheDir)) {
            Files.createDirectories(cacheDir);
        }
        
        File cacheFile = cacheDir.resolve("symbol_index_cache").toFile();
        symbolCache = new PersistentHashMap<>(
                cacheFile,
                new EnumeratorStringDescriptor(),
                new SymbolCollectionResultExternalizer()
        );
    }

    /**
     * 从缓存中获取数据，如果不存在则执行提供的加载函数
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Supplier<T> loader, long expirationMillis) {
        return getOrCompute(key, loader, expirationMillis, null);
    }

    /**
     * 从缓存中获取数据，如果不存在则执行提供的加载函数，并支持版本控制
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, Supplier<T> loader, long expirationMillis, String version) {
        // 检查内存缓存
        CacheEntry<T> entry = (CacheEntry<T>) memoryCache.get(key);
        
        if (entry != null) {
            if (entry.isExpired()) {
                // 缓存已过期，移除并重新加载
                memoryCache.remove(key);
                stats.recordExpiration();
            } else if (version == null || version.equals(entry.getVersion())) {
                // 缓存有效且版本匹配
                stats.recordHit();
                return entry.getValue();
            }
        }
        
        // 缓存未命中，加载数据
        stats.recordMiss();
        T value = loader.get();
        
        if (value != null) {
            // 添加到内存缓存
            long expirationTime = expirationMillis > 0 ? 
                    System.currentTimeMillis() + expirationMillis : 
                    System.currentTimeMillis() + config.getDefaultExpiration();
            
            memoryCache.put(key, new CacheEntry<>(value, expirationTime, version));
            
            // 检查是否需要清理缓存
            if (memoryCache.size() > config.getMaxEntries()) {
                cleanupCacheEntries();
            }
        }
        
        return value;
    }

    /**
     * 存储符号索引结果到缓存
     */
    public void cacheSymbolResult(String key, SymbolCollectionResult result, String solutionVersion) {
        if (result == null) return;
        
        // 存储到内存缓存
        long expirationTime = System.currentTimeMillis() + config.getDefaultExpiration();
        memoryCache.put(key, new CacheEntry<>(result, expirationTime, solutionVersion));
        
        // 存储到磁盘缓存
        if (config.isDiskCacheEnabled() && symbolCache != null) {
            try {
                symbolCache.put(key, result);
            } catch (IOException e) {
                LOG.warn("Failed to cache symbols to disk", e);
            }
        }
    }

    /**
     * 从缓存中获取符号索引结果
     */
    @SuppressWarnings("unchecked")
    public SymbolCollectionResult getSymbolResult(String key, String solutionVersion) {
        // 首先检查内存缓存
        CacheEntry<SymbolCollectionResult> entry = (CacheEntry<SymbolCollectionResult>) memoryCache.get(key);
        
        if (entry != null) {
            if (entry.isExpired()) {
                // 缓存已过期
                memoryCache.remove(key);
                stats.recordExpiration();
            } else if (solutionVersion == null || solutionVersion.equals(entry.getVersion())) {
                // 缓存有效且版本匹配
                stats.recordHit();
                return entry.getValue();
            }
        }
        
        // 检查磁盘缓存
        if (config.isDiskCacheEnabled() && symbolCache != null) {
            try {
                SymbolCollectionResult result = symbolCache.get(key);
                if (result != null) {
                    // 将磁盘缓存加载到内存缓存
                    long expirationTime = System.currentTimeMillis() + config.getDefaultExpiration();
                    memoryCache.put(key, new CacheEntry<>(result, expirationTime, solutionVersion));
                    stats.recordHit();
                    return result;
                }
            } catch (IOException e) {
                LOG.warn("Failed to read symbols from disk cache", e);
            }
        }
        
        stats.recordMiss();
        return null;
    }

    /**
     * 清理过期的缓存项
     */
    public void cleanupExpiredEntries() {
        List<String> expiredKeys = new ArrayList<>();
        
        for (Map.Entry<String, CacheEntry<?>> entry : memoryCache.entrySet()) {
            if (entry.getValue().isExpired()) {
                expiredKeys.add(entry.getKey());
            }
        }
        
        // 移除过期的缓存项
        for (String key : expiredKeys) {
            memoryCache.remove(key);
            stats.recordExpiration();
        }
        
        LOG.debug("Cleaned up " + expiredKeys.size() + " expired cache entries");
    }

    /**
     * 清理部分缓存项以控制缓存大小
     */
    private void cleanupCacheEntries() {
        if (memoryCache.size() <= config.getMaxEntries()) {
            return;
        }
        
        // 计算需要移除的条目数量
        int evictionCount = (int) (memoryCache.size() * config.getEvictionRatio());
        if (evictionCount <= 0) evictionCount = 1;
        
        // 简单实现：移除最早添加的条目
        // 实际应用中可以使用LRU或其他缓存替换策略
        List<String> keys = new ArrayList<>(memoryCache.keySet());
        for (int i = 0; i < Math.min(evictionCount, keys.size()); i++) {
            memoryCache.remove(keys.get(i));
            stats.recordEviction();
        }
        
        LOG.debug("Evicted " + evictionCount + " cache entries to control size");
    }

    /**
     * 清除所有缓存
     */
    public void clear() {
        memoryCache.clear();
        
        if (config.isDiskCacheEnabled() && symbolCache != null) {
            try {
                // 关闭并重新创建symbolCache来清除磁盘缓存
                symbolCache.close();
                initDiskCache();
            } catch (IOException e) {
                LOG.warn("Failed to clear disk cache", e);
            }
        }
        
        LOG.debug("Cache cleared");
    }

    /**
     * 关闭缓存，释放资源
     */
    public void close() {
        // 停止清理任务
        cleanupTask.stopTask();
        
        // 关闭磁盘缓存
        if (symbolCache != null) {
            try {
                symbolCache.close();
            } catch (IOException e) {
                LOG.warn("Failed to close disk cache properly", e);
            }
        }
        
        // 清除内存缓存
        memoryCache.clear();
        
        LOG.debug("Cache closed and resources released");
    }

    /**
     * 生成基于内容的缓存键
     */
    public static String generateCacheKey(String baseKey, String content) {
        CRC32 crc = new CRC32();
        crc.update(content.getBytes());
        return baseKey + ":" + crc.getValue();
    }

    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats() {
        return stats;
    }

    /**
     * 获取当前缓存大小
     */
    public int getSize() {
        return memoryCache.size();
    }
}