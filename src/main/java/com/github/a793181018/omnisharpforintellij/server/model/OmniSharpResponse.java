package com.github.a793181018.omnisharpforintellij.server.model;

import java.util.concurrent.atomic.AtomicLong;

/**
 * OmniSharp服务器响应模型
 * @param <T> 响应数据类型
 */
public class OmniSharpResponse<T> {
    private static final AtomicLong RESPONSE_SEQ_COUNTER = new AtomicLong(1);
    
    private final String command;
    private final long requestSeq;
    private final long responseSeq;
    private final String type = "response";
    private final T body;
    private final boolean success;
    private final String message;
    
    /**
     * 创建OmniSharp响应
     * @param command 命令路径
     * @param requestSeq 请求序列号
     * @param body 响应数据
     * @param success 是否成功
     * @param message 消息
     */
    public OmniSharpResponse(String command, long requestSeq, T body, boolean success, String message) {
        this.command = command;
        this.requestSeq = requestSeq;
        this.responseSeq = RESPONSE_SEQ_COUNTER.getAndIncrement();
        this.body = body;
        this.success = success;
        this.message = message;
    }
    
    public String getCommand() {
        return command;
    }
    
    public long getRequestSeq() {
        return requestSeq;
    }
    
    public long getResponseSeq() {
        return responseSeq;
    }
    
    public String getType() {
        return type;
    }
    
    public T getBody() {
        return body;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getMessage() {
        return message;
    }
}