package com.github.a793181018.omnisharpforintellij.server.exceptions;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;

/**
 * OmniSharp服务器响应处理异常
 */
public class OmniSharpResponseException extends OmniSharpException {
    
    private final transient OmniSharpResponse<?> response;
    
    /**
     * 创建响应异常
     * @param message 异常消息
     * @param response 原始响应对象
     */
    public OmniSharpResponseException(String message, OmniSharpResponse<?> response) {
        super(message);
        this.response = response;
    }
    
    /**
     * 创建响应异常
     * @param message 异常消息
     * @param cause 原因异常
     * @param response 原始响应对象
     */
    public OmniSharpResponseException(String message, Throwable cause, OmniSharpResponse<?> response) {
        super(message, cause);
        this.response = response;
    }
    
    /**
     * 获取原始响应对象
     * @return 响应对象
     */
    public OmniSharpResponse<?> getResponse() {
        return response;
    }
}