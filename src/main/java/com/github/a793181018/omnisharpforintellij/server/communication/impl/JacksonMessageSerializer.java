package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.a793181018.omnisharpforintellij.server.communication.IMessageSerializer;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;

import java.io.IOException;
import java.util.Map;

/**
 * 使用Jackson库实现的OmniSharp消息序列化器
 */
public class JacksonMessageSerializer implements IMessageSerializer {
    private final ObjectMapper objectMapper;
    
    /**
     * 创建Jackson消息序列化器
     */
    public JacksonMessageSerializer() {
        this.objectMapper = new ObjectMapper();
        // 配置Jackson
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, false); // 不格式化输出以提高性能
    }
    
    /**
     * 创建带自定义配置的Jackson消息序列化器
     * @param prettyPrint 是否美化输出
     * @param failOnUnknownProperties 遇到未知属性是否失败
     */
    public JacksonMessageSerializer(boolean prettyPrint, boolean failOnUnknownProperties) {
        this.objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, failOnUnknownProperties);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, prettyPrint);
    }
    
    @Override
    public String serialize(OmniSharpRequest<?> request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize request: " + e.getMessage(), e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public OmniSharpResponse<?> deserializeResponse(String json) {
        try {
            // 首先解析为Map获取基本信息
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            
            String command = (String) map.get("command");
            long request_seq = ((Number) map.getOrDefault("request_seq", 0)).longValue();
            boolean success = Boolean.TRUE.equals(map.getOrDefault("success", true));
            String message = (String) map.getOrDefault("message", null);
            Object body = map.getOrDefault("body", null);
            
            // 创建响应对象
            return new OmniSharpResponse<>(command, request_seq, body, success, message);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize response: " + e.getMessage(), e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public OmniSharpEvent<?> deserializeEvent(String json) {
        try {
            // 首先解析为Map获取基本信息
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            
            String event = (String) map.get("event");
            Object body = map.getOrDefault("body", null);
            
            // 创建事件对象
            return new OmniSharpEvent<>(event, body);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize event: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取底层的ObjectMapper实例
     * @return ObjectMapper实例
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}