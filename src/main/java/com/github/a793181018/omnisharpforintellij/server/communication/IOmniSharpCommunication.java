package com.github.a793181018.omnisharpforintellij.server.communication;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OmniSharp通信接口，负责与OmniSharp服务器进行通信
 */
public interface IOmniSharpCommunication {
    /**
     * 初始化通信通道
     * @param processManager 进程管理器
     */
    void initialize(IOmniSharpProcessManager processManager);
    
    /**
     * 发送请求到OmniSharp服务器
     * @param request 请求对象
     * @param <T> 响应数据类型
     * @return 响应结果
     */
    <T> CompletableFuture<OmniSharpResponse<T>> sendRequest(OmniSharpRequest<T> request);
    
    /**
     * 使用Reactor发送请求
     * @param request 请求对象
     * @param <T> 响应数据类型
     * @return Mono响应
     */
    <T> Mono<OmniSharpResponse<T>> sendRequestReactive(OmniSharpRequest<T> request);
    
    /**
     * 发送消息到OmniSharp服务器（不等待响应）
     * @param message 要发送的消息
     */
    void sendMessage(String message);
    
    /**
     * 订阅OmniSharp服务器事件
     * @param eventName 事件名称
     * @param eventType 事件数据类型
     * @param listener 事件监听器
     * @param <T> 事件数据类型
     * @return 订阅ID，用于取消订阅
     */
    <T> String subscribeToEvent(String eventName, Class<T> eventType, Consumer<OmniSharpEvent<T>> listener);
    
    /**
     * 取消事件订阅
     * @param subscriptionId 订阅ID
     */
    void unsubscribeFromEvent(String subscriptionId);
    
    /**
     * 关闭通信通道
     */
    void shutdown();
    
    /**
     * 检查通信通道是否已初始化
     * @return 是否已初始化
     */
    boolean isInitialized();
}