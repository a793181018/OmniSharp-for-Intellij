package com.github.a793181018.omnisharpforintellij.server.exceptions;

/**
 * OmniSharp服务器配置异常
 */
public class OmniSharpConfigurationException extends OmniSharpException {
    
    /**
     * 创建配置异常
     * @param message 异常消息
     */
    public OmniSharpConfigurationException(String message) {
        super(message);
    }
    
    /**
     * 创建配置异常
     * @param message 异常消息
     * @param cause 原因异常
     */
    public OmniSharpConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}