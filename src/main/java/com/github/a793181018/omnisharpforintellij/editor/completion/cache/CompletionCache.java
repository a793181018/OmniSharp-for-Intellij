package com.github.a793181018.omnisharpforintellij.editor.completion.cache;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 代码补全缓存接口，用于存储和管理补全结果缓存
 */
public interface CompletionCache {
    
    /**
     * 缓存补全结果
     * 
     * @param request 补全请求
     * @param response 补全响应
     */
    void put(@NotNull CompletionRequest request, @NotNull CompletionResponse response);
    
    /**
     * 获取缓存的补全结果
     * 
     * @param request 补全请求
     * @return 缓存的响应，如果不存在则返回null
     */
    @Nullable
    CompletionResponse get(@NotNull CompletionRequest request);
    
    /**
     * 清除指定文件的缓存
     * 
     * @param filePath 文件路径
     */
    void clearFileCache(@NotNull String filePath);
    
    /**
     * 清除所有缓存
     */
    void clearAll();
    
    /**
     * 获取缓存大小
     */
    int size();
    
    /**
     * 检查缓存是否包含指定请求的结果
     */
    boolean contains(@NotNull CompletionRequest request);
    
    /**
     * 设置最大缓存大小
     */
    void setMaxSize(int maxSize);
    
    /**
     * 设置缓存项的过期时间（毫秒）
     */
    void setExpirationTime(long expirationTime);
}