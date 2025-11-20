package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 依赖分析缓存
 * 用于缓存依赖分析结果，提高性能
 */
public class DependencyAnalysisCache {
    private static final Logger logger = Logger.getLogger(DependencyAnalysisCache.class.getName());
    
    // 缓存项类
    private static class CacheItem {
        private final DependencyAnalysisResult result;
        private final long timestamp;
        private final long fileLastModified;
        
        CacheItem(DependencyAnalysisResult result, long fileLastModified) {
            this.result = result;
            this.timestamp = System.currentTimeMillis();
            this.fileLastModified = fileLastModified;
        }
        
        boolean isExpired(long currentFileLastModified) {
            // 如果文件修改时间发生变化，缓存过期
            return currentFileLastModified != fileLastModified;
        }
    }
    
    private final Map<String, CacheItem> projectCache;
    private final Map<String, CacheItem> solutionCache;
    private final long cacheTimeoutMs;
    
    public DependencyAnalysisCache() {
        this.projectCache = new ConcurrentHashMap<>();
        this.solutionCache = new ConcurrentHashMap<>();
        this.cacheTimeoutMs = 5 * 60 * 1000; // 5分钟缓存超时
    }
    
    public DependencyAnalysisCache(long cacheTimeoutMs) {
        this.projectCache = new ConcurrentHashMap<>();
        this.solutionCache = new ConcurrentHashMap<>();
        this.cacheTimeoutMs = cacheTimeoutMs;
    }
    
    /**
     * 检查项目缓存是否存在且有效
     */
    public boolean hasProjectCache(Path projectPath) {
        String key = projectPath.toString();
        CacheItem item = projectCache.get(key);
        if (item == null) {
            return false;
        }
        
        // 检查缓存是否过期
        Path projectFile = projectPath.resolve(projectPath.getFileName() + ".csproj");
        long currentLastModified = projectFile.toFile().lastModified();
        
        if (item.isExpired(currentLastModified) || 
            System.currentTimeMillis() - item.timestamp > cacheTimeoutMs) {
            // 缓存过期，移除
            projectCache.remove(key);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取项目缓存
     */
    public DependencyAnalysisResult getProjectCache(Path projectPath) {
        CacheItem item = projectCache.get(projectPath.toString());
        return item != null ? item.result : null;
    }
    
    /**
     * 设置项目缓存
     */
    public void setProjectCache(Path projectPath, DependencyAnalysisResult result) {
        Path projectFile = projectPath.resolve(projectPath.getFileName() + ".csproj");
        long fileLastModified = projectFile.toFile().lastModified();
        projectCache.put(projectPath.toString(), new CacheItem(result, fileLastModified));
    }
    
    /**
     * 检查解决方案缓存是否存在且有效
     */
    public boolean hasSolutionCache(Path solutionPath) {
        String key = solutionPath.toString();
        CacheItem item = solutionCache.get(key);
        if (item == null) {
            return false;
        }
        
        // 检查缓存是否过期
        long currentLastModified = solutionPath.toFile().lastModified();
        
        if (item.isExpired(currentLastModified) || 
            System.currentTimeMillis() - item.timestamp > cacheTimeoutMs) {
            // 缓存过期，移除
            solutionCache.remove(key);
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取解决方案缓存
     */
    public DependencyAnalysisResult getSolutionCache(Path solutionPath) {
        CacheItem item = solutionCache.get(solutionPath.toString());
        return item != null ? item.result : null;
    }
    
    /**
     * 设置解决方案缓存
     */
    public void setSolutionCache(Path solutionPath, DependencyAnalysisResult result) {
        long fileLastModified = solutionPath.toFile().lastModified();
        solutionCache.put(solutionPath.toString(), new CacheItem(result, fileLastModified));
    }
    
    /**
     * 清除项目缓存
     */
    public void clearProjectCache(Path projectPath) {
        projectCache.remove(projectPath.toString());
    }
    
    /**
     * 清除解决方案缓存
     */
    public void clearSolutionCache(Path solutionPath) {
        solutionCache.remove(solutionPath.toString());
    }
    
    /**
     * 清除所有缓存
     */
    public void clear() {
        projectCache.clear();
        solutionCache.clear();
        logger.info("依赖分析缓存已清除");
    }
    
    /**
     * 清理过期缓存
     */
    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        
        // 清理过期的项目缓存
        projectCache.entrySet().removeIf(entry -> {
            CacheItem item = entry.getValue();
            return currentTime - item.timestamp > cacheTimeoutMs;
        });
        
        // 清理过期的解决方案缓存
        solutionCache.entrySet().removeIf(entry -> {
            CacheItem item = entry.getValue();
            return currentTime - item.timestamp > cacheTimeoutMs;
        });
        
        logger.info("过期缓存清理完成");
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getCacheStats() {
        return new CacheStats(
            projectCache.size(),
            solutionCache.size(),
            cacheTimeoutMs
        );
    }
    
    /**
     * 缓存统计信息类
     */
    public static class CacheStats {
        private final int projectCacheCount;
        private final int solutionCacheCount;
        private final long cacheTimeoutMs;
        
        public CacheStats(int projectCacheCount, int solutionCacheCount, long cacheTimeoutMs) {
            this.projectCacheCount = projectCacheCount;
            this.solutionCacheCount = solutionCacheCount;
            this.cacheTimeoutMs = cacheTimeoutMs;
        }
        
        public int getProjectCacheCount() {
            return projectCacheCount;
        }
        
        public int getSolutionCacheCount() {
            return solutionCacheCount;
        }
        
        public int getTotalCacheCount() {
            return projectCacheCount + solutionCacheCount;
        }
        
        public long getCacheTimeoutMs() {
            return cacheTimeoutMs;
        }
        
        @Override
        public String toString() {
            return "CacheStats{projectCacheCount=" + projectCacheCount + 
                   ", solutionCacheCount=" + solutionCacheCount + 
                   ", total=" + getTotalCacheCount() + 
                   ", timeoutMs=" + cacheTimeoutMs + "}";
        }
    }
}