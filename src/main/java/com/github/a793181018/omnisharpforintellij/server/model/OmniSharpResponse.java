package com.github.a793181018.omnisharpforintellij.server.model;

import com.google.gson.annotations.SerializedName;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OmniSharp服务器响应模型
 * @param <T> 响应数据类型
 */
public class OmniSharpResponse<T> {
    private static final AtomicLong RESPONSE_SEQ_COUNTER = new AtomicLong(1);
    
    @SerializedName("command")
    private final String command;
    
    @SerializedName("request_seq")
    private final long request_seq;
    
    @SerializedName("seq")
    private final long seq;
    
    @SerializedName("type")
    private final String type = "response";
    
    @SerializedName("body")
    private final T body;
    
    @SerializedName("success")
    private final boolean success;
    
    @SerializedName("message")
    private final String message;
    
    /**
     * 创建OmniSharp响应
     * @param command 命令路径
     * @param request_seq 请求序列号
     * @param body 响应数据
     * @param success 是否成功
     * @param message 消息
     */
    public OmniSharpResponse(String command, long request_seq, T body, boolean success, String message) {
        this.command = command;
        this.request_seq = request_seq;
        this.seq = RESPONSE_SEQ_COUNTER.getAndIncrement();
        this.body = body;
        this.success = success;
        this.message = message;
    }
    
    public String getCommand() {
        return command;
    }
    
    public long getRequest_seq() {
        return request_seq;
    }
    
    public long getSeq() {
        return seq;
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