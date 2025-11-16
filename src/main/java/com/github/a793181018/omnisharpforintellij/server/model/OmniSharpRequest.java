package com.github.a793181018.omnisharpforintellij.server.model;

import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OmniSharp服务器请求模型
 * @param <T> 响应数据类型
 */
public class OmniSharpRequest<T> {
    private static final AtomicLong REQUEST_SEQ_COUNTER = new AtomicLong(1);
    
    @SerializedName("command")
    private final String command;
    
    @SerializedName("arguments")
    private final Map<String, Object> arguments;
    
    // 不序列化responseType
    private final Class<T> responseType;
    
    @SerializedName("seq")
    private final long seq;
    
    @SerializedName("type")
    private final String type = "request";
    
    /**
     * 创建OmniSharp请求
     * @param command 命令路径
     * @param arguments 请求参数
     * @param responseType 响应类型
     */
    public OmniSharpRequest(String command, Map<String, Object> arguments, Class<T> responseType) {
        this.command = command;
        this.arguments = arguments != null ? arguments : new HashMap<>();
        this.responseType = responseType;
        this.seq = REQUEST_SEQ_COUNTER.getAndIncrement();
    }
    
    /**
     * 创建OmniSharp请求（无类型参数）
     * @param command 命令路径
     * @param arguments 请求参数
     */
    public OmniSharpRequest(String command, Map<String, Object> arguments) {
        this(command, arguments, null);
    }
    
    public String getCommand() {
        return command;
    }
    
    public Map<String, Object> getArguments() {
        return arguments;
    }
    
    public Class<T> getResponseType() {
        return responseType;
    }
    
    public long getSeq() {
        return seq;
    }
    
    public long getRequestSeq() {
        return getSeq();
    }
    
    public String getType() {
        return type;
    }
}