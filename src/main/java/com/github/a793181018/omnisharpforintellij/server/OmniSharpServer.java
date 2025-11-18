package com.github.a793181018.omnisharpforintellij.server;

import com.github.a793181018.omnisharpforintellij.communicator.OmniSharpServerClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp服务器接口
 */
public interface OmniSharpServer {
    /**
     * 启动服务器
     */
    CompletableFuture<Boolean> start();
    
    /**
     * 停止服务器
     */
    CompletableFuture<Boolean> stop();
    
    /**
     * 重启服务器
     */
    CompletableFuture<Boolean> restart();
    
    /**
     * 获取服务器状态
     */
    @NotNull
    ServerStatus getStatus();
    
    /**
     * 检查服务器是否运行中
     */
    boolean isRunning();
    
    /**
     * 获取通信客户端
     */
    @Nullable
    OmniSharpServerClient getClient();
    
    /**
     * 获取服务器路径
     */
    @NotNull
    String getServerPath();
    
    /**
     * 获取项目路径
     */
    @NotNull
    String getProjectPath();
    
    /**
     * 获取最后一次错误信息
     */
    @Nullable
    Throwable getLastError();
    
    /**
     * 添加服务器状态监听器
     */
    void addServerListener(@NotNull OmniSharpServerListener listener);
    
    /**
     * 移除服务器状态监听器
     */
    void removeServerListener(@NotNull OmniSharpServerListener listener);
}