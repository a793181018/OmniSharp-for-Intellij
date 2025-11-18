package com.github.a793181018.omnisharpforintellij.communicator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * OmniSharp服务器客户端接口
 */
public interface OmniSharpServerClient {
    /**
     * 连接到OmniSharp服务器
     */
    void connect() throws IOException;
    
    /**
     * 断开连接
     */
    void disconnect();
    
    /**
     * 发送消息到服务器
     */
    void send(@NotNull String message) throws IOException;
    
    /**
     * 接收服务器响应
     */
    @Nullable
    String receive() throws IOException;
    
    /**
     * 检查是否已连接
     */
    boolean isConnected();
    
    /**
     * 检查是否可重连
     */
    boolean isReconnectable();
    
    /**
     * 设置连接超时时间
     */
    void setConnectTimeout(int timeoutMs);
    
    /**
     * 设置读取超时时间
     */
    void setReadTimeout(int timeoutMs);
}