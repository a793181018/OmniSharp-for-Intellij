package com.github.a793181018.omnisharpforintellij.communicator.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp响应消息
 */
public class OmniSharpResponse extends BaseOmniSharpMessage {
    private final String requestId;
    private final boolean success;
    private final String message;
    private final JsonNode body;
    private final String command;
    
    public OmniSharpResponse(@NotNull String requestId, boolean success, @Nullable String message, 
                            @Nullable JsonNode body, @NotNull String command) {
        super("response", requestId + "_response");
        this.requestId = requestId;
        this.success = success;
        this.message = message;
        this.body = body;
        this.command = command;
    }
    
    /**
     * 获取对应的请求ID
     */
    public String getRequestId() {
        return requestId;
    }
    
    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取响应消息
     */
    @Nullable
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取响应体
     */
    @Nullable
    public JsonNode getBody() {
        return body;
    }
    
    /**
     * 获取命令名称
     */
    @NotNull
    public String getCommand() {
        return command;
    }
    
    @Override
    public void fromJson(JsonNode jsonNode) {
        super.fromJson(jsonNode);
        // 可以在这里添加额外的解析逻辑
    }
    
    @Override
    public String toString() {
        return "OmniSharpResponse{" +
                "requestId='" + requestId + "'" +
                ", success=" + success +
                ", command='" + command + "'" +
                (message != null ? ", message='" + message + "'" : "") +
                '}';
    }
}