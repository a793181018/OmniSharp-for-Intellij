package com.github.a793181018.omnisharpforintellij.communicator.message;

import com.fasterxml.jackson.databind.JsonNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * OmniSharp请求消息
 */
public class OmniSharpRequest extends BaseOmniSharpMessage {
    private final String command;
    private final Map<String, Object> parameters = new HashMap<>();
    
    public OmniSharpRequest(@NotNull String command) {
        super("request");
        this.command = command;
    }
    
    public OmniSharpRequest(@NotNull String command, @NotNull String messageId) {
        super("request", messageId);
        this.command = command;
    }
    
    /**
     * 获取命令名称
     */
    public String getCommand() {
        return command;
    }
    
    /**
     * 添加参数
     */
    public OmniSharpRequest addParameter(@NotNull String key, @Nullable Object value) {
        if (value != null) {
            parameters.put(key, value);
        }
        return this;
    }
    
    /**
     * 获取参数
     */
    public Map<String, Object> getParameters() {
        return new HashMap<>(parameters);
    }
    
    /**
     * 获取指定参数
     */
    @Nullable
    public <T> T getParameter(@NotNull String key) {
        @SuppressWarnings("unchecked")
        T value = (T) parameters.get(key);
        return value;
    }
    
    @Override
    public void fromJson(JsonNode jsonNode) {
        super.fromJson(jsonNode);
        if (jsonNode.has("Arguments")) {
            JsonNode argumentsNode = jsonNode.get("Arguments");
            argumentsNode.fields().forEachRemaining(entry -> {
                parameters.put(entry.getKey(), entry.getValue());
            });
        }
    }
    
    @Override
    public String toString() {
        return "OmniSharpRequest{" +
                "command='" + command + "'" +
                ", parameters=" + parameters + "'" +
                ", messageId='" + getMessageId() + "'" +
                '}';
    }
}