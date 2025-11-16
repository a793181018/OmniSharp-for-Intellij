package com.github.a793181018.omnisharpforintellij.server.communication;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 标准输入输出通道接口，用于与OmniSharp服务器进行通信
 */
public interface IStdioChannel {
    /**
     * 初始化通道
     * @param process OmniSharp进程
     */
    void initialize(Process process);
    
    /**
     * 向通道写入消息
     * @param message 消息字节数组
     * @throws IOException 如果写入失败
     */
    void write(byte[] message) throws IOException;
    
    /**
     * 从通道读取消息（带超时）
     * @param timeoutMs 超时时间（毫秒）
     * @return 消息字节数组
     * @throws IOException 如果读取失败
     * @throws TimeoutException 如果读取超时
     */
    byte[] read(long timeoutMs) throws IOException, TimeoutException;
    
    /**
     * 关闭通道并释放资源
     */
    void close();
    
    /**
     * 注册消息监听器
     * @param listener 消息监听器
     */
    void registerListener(IStdioMessageListener listener);
    
    /**
     * 取消注册消息监听器
     * @param listener 消息监听器
     */
    void unregisterListener(IStdioMessageListener listener);
    
    /**
     * 检查通道是否已初始化
     * @return 是否已初始化
     */
    boolean isInitialized();
}