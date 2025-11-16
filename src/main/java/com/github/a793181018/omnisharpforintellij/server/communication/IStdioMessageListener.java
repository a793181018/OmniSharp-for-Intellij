package com.github.a793181018.omnisharpforintellij.server.communication;

/**
 * Stdio通道消息监听器接口
 */
public interface IStdioMessageListener {
    /**
     * 当接收到消息时调用
     * @param message 接收到的消息字节数组
     */
    void onMessageReceived(byte[] message);
    
    /**
     * 当发生错误时调用
     * @param error 错误信息
     */
    void onError(Throwable error);
}