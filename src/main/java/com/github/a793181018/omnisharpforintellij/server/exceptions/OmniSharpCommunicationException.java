package com.github.a793181018.omnisharpforintellij.server.exceptions;

/**
 * OmniSharp服务器通信失败异常
 */
public class OmniSharpCommunicationException extends OmniSharpException {
    
    /**
     * 创建通信异常
     * @param message 异常消息
     */
    public OmniSharpCommunicationException(String message) {
        super(message);
    }
    
    /**
     * 创建通信异常
     * @param message 异常消息
     * @param cause 原因异常
     */
    public OmniSharpCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
}