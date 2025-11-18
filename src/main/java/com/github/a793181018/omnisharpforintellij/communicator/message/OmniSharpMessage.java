package com.github.a793181018.omnisharpforintellij.communicator.message;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OmniSharp消息接口
 */
public interface OmniSharpMessage {
    /**
     * 获取消息类型
     */
    String getType();
    
    /**
     * 获取消息ID
     */
    String getMessageId();
    
    /**
     * 转换为JSON
     */
    String toJson();
    
    /**
     * 从JSON节点创建消息
     */
    void fromJson(JsonNode jsonNode);
}