package com.github.a793181018.omnisharpforintellij.communicator.message;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp消息处理器接口
 */
public interface OmniSharpMessageHandler {
    /**
     * 处理请求消息
     * @param request 请求消息
     * @return 响应的CompletableFuture
     */
    CompletableFuture<OmniSharpResponse> handleRequest(@NotNull OmniSharpRequest request);
    
    /**
     * 处理响应消息
     * @param response 响应消息
     */
    void handleResponse(@NotNull OmniSharpResponse response);
    
    /**
     * 处理错误
     * @param request 请求消息
     * @param error 错误信息
     */
    void handleError(@NotNull OmniSharpRequest request, @NotNull Throwable error);
    
    /**
     * 关闭处理器
     */
    void shutdown();
}