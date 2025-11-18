package com.github.a793181018.omnisharpforintellij.editor.completion.cache;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 代码补全缓存管理器，负责管理缓存的生命周期和提供缓存访问接口
 */
@Service(Service.Level.PROJECT)
public class CompletionCacheManager {
    private static final Logger LOG = Logger.getInstance(CompletionCacheManager.class);
    
    private final Project project;
    private final CompletionCache cache;
    private final ScheduledExecutorService cleanupExecutor;
    
    public CompletionCacheManager(@NotNull Project project) {
        this.project = project;
        // 使用内存缓存实现
        this.cache = new MemoryCompletionCache();
        
        // 创建定时清理线程池
        this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("OmniSharp-Completion-Cache-Cleanup");
            thread.setDaemon(true);
            return thread;
        });
        
        // 启动定时清理任务
        startCleanupTask();
    }
    
    /**
     * 获取CompletionCacheManager实例
     */
    @NotNull
    public static CompletionCacheManager getInstance(@NotNull Project project) {
        return project.getService(CompletionCacheManager.class);
    }
    
    /**
     * 缓存补全结果
     */
    public void cacheCompletion(@NotNull CompletionRequest request, @NotNull CompletionResponse response) {
        if (response.isSuccessful() && !response.isIncomplete()) {
            cache.put(request, response);
            LOG.debug("Cached completion results for: " + request.getFileName() + 
                     " at position (" + request.getLine() + "," + request.getColumn() + ")");
        }
    }
    
    /**
     * 尝试从缓存获取补全结果
     */
    @Nullable
    public CompletionResponse getCachedCompletion(@NotNull CompletionRequest request) {
        CompletionResponse response = cache.get(request);
        if (response != null) {
            LOG.debug("Retrieved cached completion results for: " + request.getFileName() + 
                     " at position (" + request.getLine() + "," + request.getColumn() + ")");
        }
        return response;
    }
    
    /**
     * 清除指定文件的缓存
     */
    public void invalidateFile(@NotNull String filePath) {
        cache.clearFileCache(filePath);
        LOG.debug("Invalidated cache for file: " + filePath);
    }
    
    /**
     * 清除指定文件的缓存
     */
    public void invalidateFile(@NotNull VirtualFile file) {
        invalidateFile(file.getPath());
    }
    
    /**
     * 清除所有缓存
     */
    public void invalidateAll() {
        cache.clearAll();
        LOG.debug("Invalidated all completion cache");
    }
    
    /**
     * 获取缓存统计信息
     */
    @NotNull
    public CacheStatistics getStatistics() {
        return new CacheStatistics(cache.size());
    }
    
    /**
     * 设置缓存配置
     */
    public void configureCache(int maxSize, long expirationTimeMillis) {
        cache.setMaxSize(maxSize);
        cache.setExpirationTime(expirationTimeMillis);
        LOG.debug("Configured cache: maxSize=" + maxSize + ", expirationTime=" + expirationTimeMillis + "ms");
    }
    
    /**
     * 启动缓存清理任务
     */
    private void startCleanupTask() {
        // 每5分钟运行一次清理任务
        cleanupExecutor.scheduleAtFixedRate(() -> {
            if (project.isDisposed()) {
                cleanupExecutor.shutdown();
                return;
            }
            
            try {
                if (cache instanceof MemoryCompletionCache) {
                    ((MemoryCompletionCache) cache).cleanupExpired();
                }
            } catch (Exception e) {
                LOG.error("Error during cache cleanup: " + e.getMessage(), e);
            }
        }, 5, 5, TimeUnit.MINUTES);
    }
    
    /**
     * 缓存统计信息
     */
    public static class CacheStatistics {
        private final int cachedItems;
        
        public CacheStatistics(int cachedItems) {
            this.cachedItems = cachedItems;
        }
        
        public int getCachedItems() {
            return cachedItems;
        }
        
        @Override
        public String toString() {
            return "CacheStatistics{" +
                   "cachedItems=" + cachedItems +
                   '}';
        }
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        try {
            cleanupExecutor.shutdownNow();
            cleanupExecutor.awaitTermination(1000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        LOG.debug("Completion cache manager disposed");
    }
}