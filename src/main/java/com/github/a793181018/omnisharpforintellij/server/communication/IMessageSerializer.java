package com.github.a793181018.omnisharpforintellij.server.communication;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;

/**
 * OmniSharp消息序列化器接口
 */
public interface IMessageSerializer {
    /**
     * 序列化请求消息
     * @param request 请求对象
     * @return JSON字符串
     */
    String serialize(OmniSharpRequest<?> request);
    
    /**
     * 反序列化响应消息
     * @param json JSON字符串
     * @return 响应对象
     */
    OmniSharpResponse<?> deserializeResponse(String json);
    
    /**
     * 反序列化事件消息
     * @param json JSON字符串
     * @return 事件对象
     */
    OmniSharpEvent<?> deserializeEvent(String json);
}