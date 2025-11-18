package com.github.a793181018.omnisharpforintellij.server;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp服务器监听器
 */
public interface OmniSharpServerListener {
    /**
     * 当服务器状态改变时调用
     * @param server 服务器实例
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void onStatusChanged(@NotNull OmniSharpServer server, 
                        @NotNull ServerStatus oldStatus, 
                        @NotNull ServerStatus newStatus);
    
    /**
     * 当服务器启动成功时调用
     * @param server 服务器实例
     */
    default void onServerStarted(@NotNull OmniSharpServer server) {
        // 默认实现，子类可以覆盖
    }
    
    /**
     * 当服务器停止时调用
     * @param server 服务器实例
     */
    default void onServerStopped(@NotNull OmniSharpServer server) {
        // 默认实现，子类可以覆盖
    }
    
    /**
     * 当服务器发生错误时调用
     * @param server 服务器实例
     * @param error 错误信息
     */
    default void onServerError(@NotNull OmniSharpServer server, @NotNull Throwable error) {
        // 默认实现，子类可以覆盖
    }
    
    /**
     * 当服务器日志输出时调用
     * @param server 服务器实例
     * @param logLevel 日志级别
     * @param message 日志消息
     */
    default void onServerLog(@NotNull OmniSharpServer server, @NotNull String logLevel, @NotNull String message) {
        // 默认实现，子类可以覆盖
    }
}