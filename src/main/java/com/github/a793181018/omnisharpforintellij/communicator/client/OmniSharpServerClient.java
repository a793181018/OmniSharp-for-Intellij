package com.github.a793181018.omnisharpforintellij.communicator.client;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * OmniSharp服务器客户端接口，负责与OmniSharp服务器进行通信
 */
public interface OmniSharpServerClient {
    /**
     * 发送消息到OmniSharp服务器
     * @param message 要发送的消息
     * @throws IOException 如果发送失败
     */
    void send(@NotNull String message) throws IOException;
    
    /**
     * 从OmniSharp服务器接收消息
     * @return 接收到的消息
     * @throws IOException 如果接收失败
     */
    String receive() throws IOException;
    
    /**
     * 关闭连接
     */
    void close();
    
    /**
     * 检查连接是否活动
     * @return 如果连接活动返回true
     */
    boolean isActive();
}