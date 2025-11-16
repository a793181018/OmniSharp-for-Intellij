package com.github.a793181018.omnisharpforintellij.server.exceptions;

/**
 * OmniSharp服务器启动失败异常
 */
public class OmniSharpServerStartupException extends OmniSharpException {
    
    /**
     * 创建服务器启动异常
     * @param message 异常消息
     */
    public OmniSharpServerStartupException(String message) {
        super(message);
    }
    
    /**
     * 创建服务器启动异常
     * @param message 异常消息
     * @param cause 原因异常
     */
    public OmniSharpServerStartupException(String message, Throwable cause) {
        super(message, cause);
    }
}