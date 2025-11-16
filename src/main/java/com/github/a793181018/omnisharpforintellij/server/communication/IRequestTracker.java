package com.github.a793181018.omnisharpforintellij.server.communication;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

/**
 * 请求跟踪器接口，用于管理和跟踪OmniSharp请求
 */
public interface IRequestTracker {
    /**
     * 跟踪一个请求，返回响应的CompletableFuture
     * @param request 请求对象
     * @return 响应的CompletableFuture
     */
    CompletableFuture<OmniSharpResponse<?>> trackRequest(OmniSharpRequest<?> request);
    
    /**
     * 获取请求的响应Future
     * @param requestSeq 请求序列号
     * @return 响应的CompletableFuture，如果找不到则返回null
     */
    CompletableFuture<OmniSharpResponse<?>> getResponseFuture(long requestSeq);
    
    /**
     * 完成一个请求，设置响应
     * @param response 响应对象
     * @return 是否成功完成请求
     */
    boolean completeRequest(OmniSharpResponse<?> response);
    
    /**
     * 取消一个请求
     * @param requestSeq 请求序列号
     * @return 是否成功取消请求
     */
    boolean cancelRequest(long requestSeq);
    
    /**
     * 取消所有请求
     */
    void cancelAllRequests();
    
    /**
     * 获取当前跟踪的请求数量
     * @return 请求数量
     */
    int getPendingRequestCount();
    
    /**
     * 清理超时的请求
     */
    void cleanupTimedOutRequests();
    
    /**
     * 设置请求超时时间
     * @param timeoutMs 超时时间（毫秒）
     */
    void setRequestTimeout(long timeoutMs);
    
    /**
     * 关闭请求跟踪器
     */
    void shutdown();
}