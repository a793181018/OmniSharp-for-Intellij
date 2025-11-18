package com.github.a793181018.omnisharpforintellij.communicator.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * OmniSharp消息的基础实现
 */
public abstract class BaseOmniSharpMessage implements OmniSharpMessage {
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final String messageId;
    private final String type;
    
    public BaseOmniSharpMessage(@NotNull String type) {
        this.type = type;
        this.messageId = UUID.randomUUID().toString();
    }
    
    public BaseOmniSharpMessage(@NotNull String type, @NotNull String messageId) {
        this.type = type;
        this.messageId = messageId;
    }
    
    @Override
    public String getType() {
        return type;
    }
    
    @Override
    public String getMessageId() {
        return messageId;
    }
    
    @Override
    public String toJson() {
        try {
            return OBJECT_MAPPER.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize message to JSON", e);
        }
    }
    
    @Override
    public void fromJson(JsonNode jsonNode) {
        // 基础实现，子类可以覆盖以处理特定字段
    }
    
    @Override
    public String toString() {
        return "BaseOmniSharpMessage{" +
                "type='" + type + "'" +
                ", messageId='" + messageId + "'" +
                '}';
    }
}