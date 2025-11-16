package com.github.a793181018.omnisharpforintellij.server.exceptions;

/**
 * OmniSharp服务器操作的基础异常类
 */
public class OmniSharpException extends RuntimeException {
    
    /**
     * 创建OmniSharp异常
     * @param message 异常消息
     */
    public OmniSharpException(String message) {
        super(message);
    }
    
    /**
     * 创建OmniSharp异常
     * @param message 异常消息
     * @param cause 原因异常
     */
    public OmniSharpException(String message, Throwable cause) {
        super(message, cause);
    }
}