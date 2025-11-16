package com.github.a793181018.omnisharpforintellij.server.model;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OmniSharp服务器请求模型
 * @param <T> 响应数据类型
 */
public class OmniSharpRequest<T> {
    private static final AtomicLong REQUEST_SEQ_COUNTER = new AtomicLong(1);
    
    private final String command;
    private final Map<String, Object> arguments;
    private final Class<T> responseType;
    private final long requestSeq;
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
        this.requestSeq = REQUEST_SEQ_COUNTER.getAndIncrement();
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
    
    public long getRequestSeq() {
        return requestSeq;
    }
    
    public String getType() {
        return type;
    }
}