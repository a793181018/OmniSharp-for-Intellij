package com.github.a793181018.omnisharpforintellij.server;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.server.model.ServerStatus;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OmniSharp服务器管理器接口，负责服务器的生命周期管理和通信
 */
public interface IOmniSharpServerManager {
    /**
     * 服务器状态变更通知主题
     */
    Topic<ServerStatusChangeListener> SERVER_STATUS_TOPIC = Topic.create(
            "OmniSharp.Server.Status",
            ServerStatusChangeListener.class
    );
    
    /**
     * 获取指定项目的服务器管理器实例
     * @param project 项目对象
     * @return 服务器管理器实例
     */
    static IOmniSharpServerManager getInstance(Project project) {
        return project.getService(IOmniSharpServerManager.class);
    }
    
    /**
     * 启动OmniSharp服务器
     * @return 启动结果，true表示成功
     */
    CompletableFuture<Boolean> startServer();
    
    /**
     * 停止OmniSharp服务器
     * @return 停止结果，true表示成功
     */
    CompletableFuture<Boolean> stopServer();
    
    /**
     * 重启OmniSharp服务器
     * @return 重启结果，true表示成功
     */
    CompletableFuture<Boolean> restartServer();
    
    /**
     * 检查服务器是否正在运行
     * @return 是否运行中
     */
    boolean isServerRunning();
    
    /**
     * 获取当前服务器状态
     * @return 服务器状态
     */
    ServerStatus getServerStatus();
    
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
     * 获取服务器状态变更的Flux流
     * @return 状态变更流
     */
    Flux<ServerStatus> statusChanges();
    
    /**
     * 服务器状态变更监听器接口
     */
    interface ServerStatusChangeListener {
        /**
         * 当服务器状态变更时调用
         * @param status 新的服务器状态
         */
        void onStatusChange(ServerStatus status);
    }
}