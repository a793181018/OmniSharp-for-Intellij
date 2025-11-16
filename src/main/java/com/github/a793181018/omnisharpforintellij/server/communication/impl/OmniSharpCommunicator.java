package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.github.a793181018.omnisharpforintellij.server.communication.*;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OmniSharp通信管理器实现，整合所有通信组件
 */
public class OmniSharpCommunicator implements IOmniSharpCommunication, IStdioMessageListener {
    private static final Logger LOGGER = Logger.getLogger(OmniSharpCommunicator.class.getName());
    private static final String CONTENT_LENGTH_HEADER = "Content-Length: ";
    private static final int DEFAULT_REQUEST_TIMEOUT_MS = 30000;
    
    private final IStdioChannel stdioChannel;
    private final IMessageSerializer messageSerializer;
    private final IRequestTracker requestTracker;
    private final IEventDispatcher eventDispatcher;
    private final Map<String, Subscription<?>> subscriptions = new ConcurrentHashMap<>();
    private final CircuitBreaker circuitBreaker;
    private final RetryPolicy retryPolicy;
    
    private IOmniSharpProcessManager processManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    /**
     * 创建默认配置的OmniSharp通信管理器
     */
    public OmniSharpCommunicator() {
        this(new StdioChannel(), new JacksonMessageSerializer(), 
             new RequestTracker(), new EventDispatcher(),
             new CircuitBreaker(), new RetryPolicy());
    }
    
    /**
     * 创建使用自定义组件的OmniSharp通信管理器
     * @param stdioChannel Stdio通道
     * @param messageSerializer 消息序列化器
     * @param requestTracker 请求跟踪器
     * @param eventDispatcher 事件分发器
     */
    public OmniSharpCommunicator(IStdioChannel stdioChannel, IMessageSerializer messageSerializer, 
                               IRequestTracker requestTracker, IEventDispatcher eventDispatcher) {
        this.stdioChannel = stdioChannel;
        this.messageSerializer = messageSerializer;
        this.requestTracker = requestTracker;
        this.eventDispatcher = eventDispatcher;
        this.circuitBreaker = new CircuitBreaker();
        this.retryPolicy = new RetryPolicy();
    }
    
    /**
     * 创建使用自定义组件和策略的OmniSharp通信管理器
     * @param stdioChannel Stdio通道
     * @param messageSerializer 消息序列化器
     * @param requestTracker 请求跟踪器
     * @param eventDispatcher 事件分发器
     * @param circuitBreaker 断路器
     * @param retryPolicy 重试策略
     */
    public OmniSharpCommunicator(IStdioChannel stdioChannel, IMessageSerializer messageSerializer, 
                               IRequestTracker requestTracker, IEventDispatcher eventDispatcher,
                               CircuitBreaker circuitBreaker, RetryPolicy retryPolicy) {
        this.stdioChannel = stdioChannel;
        this.messageSerializer = messageSerializer;
        this.requestTracker = requestTracker;
        this.eventDispatcher = eventDispatcher;
        this.circuitBreaker = circuitBreaker;
        this.retryPolicy = retryPolicy;
    }
    
    @Override
    public synchronized void initialize(IOmniSharpProcessManager processManager) {
        if (initialized.get()) {
            throw new IllegalStateException("Already initialized");
        }
        if (shutdown.get()) {
            throw new IllegalStateException("Already shutdown");
        }
        
        this.processManager = processManager;
        
        // 初始化Stdio通道
        Process process = processManager.getProcess();
        if (process == null) {
            throw new IllegalStateException("OmniSharp process is not running");
        }
        
        stdioChannel.initialize(process);
        stdioChannel.registerListener(this);
        
        initialized.set(true);
        LOGGER.info("OmniSharpCommunicator initialized successfully");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<OmniSharpResponse<T>> sendRequest(OmniSharpRequest<T> request) {
        checkInitialized();
        
        // 使用断路器和重试策略包装请求
        return retryPolicy.executeWithRetry(() -> {
            return circuitBreaker.executeWithFallback(() -> {
                LOGGER.fine("Sending request: " + request.getCommand() + " (seq: " + request.getSeq() + ")");
                
                // 跟踪请求
                CompletableFuture<OmniSharpResponse<?>> future = requestTracker.trackRequest(request);
                
                try {
                    // 序列化请求
                    String json = messageSerializer.serialize(request);
                    
                    // 构建HTTP-like消息格式
                    String message = buildMessage(json);
                    
                    // 写入通道
                    stdioChannel.write(message.getBytes(StandardCharsets.UTF_8));
                    
                    // 安全地进行类型转换
                    @SuppressWarnings("unchecked")
                    CompletableFuture<OmniSharpResponse<T>> typedFuture = future.thenApply(response -> (OmniSharpResponse<T>) response);
                    return typedFuture;
                } catch (Exception e) {
                    // 如果发送失败，取消请求跟踪
                    requestTracker.cancelRequest(request.getSeq());
                    CompletableFuture<OmniSharpResponse<T>> failedFuture = new CompletableFuture<>();
                    failedFuture.completeExceptionally(e);
                    return failedFuture;
                }
            });
        });
    }
    
    /**
     * 获取断路器实例
     */
    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }
    
    /**
     * 获取重试策略实例
     */
    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }
    
    @Override
    public <T> Mono<OmniSharpResponse<T>> sendRequestReactive(OmniSharpRequest<T> request) {
        // 检查断路器状态
        if (!circuitBreaker.allowRequest()) {
            return Mono.error(new CircuitBreaker.CircuitOpenException("Circuit breaker is open"));
        }
        
        return Mono.fromFuture(() -> sendRequest(request))
            .doOnSuccess(response -> {
                // 记录成功，更新断路器状态
                circuitBreaker.recordSuccess();
            })
            .retryWhen(Retry.from(attempts -> attempts
                .take(retryPolicy.getMaxRetries())
                .flatMap(retrySignal -> {
                    Throwable error = retrySignal.failure();
                    int attempt = (int) retrySignal.totalRetriesInARow() + 1;
                    
                    // 记录失败，更新断路器状态
                    circuitBreaker.recordFailure();
                    
                    // 检查是否应该重试
                    if (shouldRetry(error, attempt, retryPolicy)) {
                        long backoffTime = retryPolicy.getBackoffTime(attempt);
                        LOGGER.log(Level.WARNING, "Reactive operation failed, retrying in " + backoffTime + "ms (attempt " + 
                                attempt + "/" + retryPolicy.getMaxRetries() + ")", error);
                        return Mono.delay(java.time.Duration.ofMillis(backoffTime));
                    }
                    return Mono.error(error);
                })
            ));
    }
    
    private boolean shouldRetry(Throwable error, int attempt, RetryPolicy retryPolicy) {
        // 检查是否是可重试的异常类型
        return attempt <= retryPolicy.getMaxRetries() && 
               !(error instanceof CircuitBreaker.CircuitOpenException) &&
               !(error instanceof IllegalStateException) &&
               retryPolicy.isRetryableException(error);
    }
    
    @Override
    public void sendMessage(String message) {
        checkInitialized();
        
        try {
            String formattedMessage = buildMessage(message);
            stdioChannel.write(formattedMessage.getBytes(StandardCharsets.UTF_8));
            LOGGER.fine("Message sent directly to OmniSharp server");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to send message to OmniSharp server", e);
            throw new RuntimeException("Failed to send message", e);
        }
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> String subscribeToEvent(String eventName, Class<T> eventType, Consumer<OmniSharpEvent<T>> listener) {
        checkInitialized();
        
        if (eventName == null || listener == null) {
            throw new IllegalArgumentException("Event name and listener cannot be null");
        }
        
        // 创建订阅ID
        String subscriptionId = UUID.randomUUID().toString();
        
        // 创建事件监听器适配器
        IEventDispatcher.OmniSharpEventListener adapter = event -> {
            try {
                // 尝试转换事件数据类型
                T data = null;
                if (event.getBody() != null && eventType.isInstance(event.getBody())) {
                    data = eventType.cast(event.getBody());
                }
                
                OmniSharpEvent<T> typedEvent = new OmniSharpEvent<>(event.getEvent(), data);
                typedEvent.setSeq(event.getSeq());
                
                listener.accept(typedEvent);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in event listener for " + eventName, e);
            }
        };
        
        // 注册监听器
        eventDispatcher.registerListener(eventName, adapter);
        subscriptions.put(subscriptionId, new Subscription<>(eventName, adapter));
        
        LOGGER.fine("Subscribed to event: " + eventName + " with ID: " + subscriptionId);
        return subscriptionId;
    }
    
    @Override
    public void unsubscribeFromEvent(String subscriptionId) {
        if (subscriptionId == null) {
            return;
        }
        
        Subscription<?> subscription = subscriptions.remove(subscriptionId);
        if (subscription != null) {
            eventDispatcher.unregisterListener(subscription.eventName, subscription.listener);
            LOGGER.fine("Unsubscribed from event: " + subscription.eventName + " with ID: " + subscriptionId);
        }
    }
    
    @Override
    public synchronized void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            // 取消所有事件订阅
            subscriptions.clear();
            
            // 关闭各个组件
            try {
                requestTracker.shutdown();
                eventDispatcher.clearAllListeners();
                stdioChannel.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during shutdown", e);
            }
            
            initialized.set(false);
            LOGGER.info("OmniSharpCommunicator shutdown");
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized.get() && !shutdown.get();
    }
    
    @Override
    public void onMessageReceived(byte[] message) {
        try {
            String messageStr = new String(message, StandardCharsets.UTF_8);
            
            // 解析HTTP-like格式的消息内容
            String content = extractContent(messageStr);
            
            // 尝试解析为响应或事件
            processIncomingMessage(content);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error processing incoming message", e);
        }
    }
    
    @Override
    public void onError(Throwable error) {
        LOGGER.log(Level.SEVERE, "Stdio channel error", error);
        // 可以在这里添加错误处理逻辑
    }
    
    private void processIncomingMessage(String content) {
        try {
            // 简单的启发式方法：检查是否包含"event"字段
            if (content.contains("\"event\":")) {
                // 处理事件
                OmniSharpEvent<?> event = messageSerializer.deserializeEvent(content);
                LOGGER.fine("Received event: " + event.getEvent());
                eventDispatcher.dispatchEvent(event);
            } else if (content.contains("\"command\":")) {
                // 处理响应
                OmniSharpResponse<?> response = messageSerializer.deserializeResponse(content);
                LOGGER.fine("Received response: " + response.getCommand() + " (seq: " + response.getRequest_seq() + ")");
                requestTracker.completeRequest(response);
            } else {
                LOGGER.warning("Unknown message format: " + content);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to process message: " + content, e);
        }
    }
    
    private String buildMessage(String content) {
        StringBuilder message = new StringBuilder();
        message.append(CONTENT_LENGTH_HEADER).append(content.length()).append("\r\n");
        message.append("\r\n");
        message.append(content);
        return message.toString();
    }
    
    private String extractContent(String message) {
        // 查找Content-Length头
        int contentLengthStart = message.indexOf(CONTENT_LENGTH_HEADER);
        if (contentLengthStart != -1) {
            int contentLengthEnd = message.indexOf("\r\n", contentLengthStart);
            if (contentLengthEnd != -1) {
                String lengthStr = message.substring(contentLengthStart + CONTENT_LENGTH_HEADER.length(), contentLengthEnd).trim();
                try {
                    int contentLength = Integer.parseInt(lengthStr);
                    // 查找两个连续的\r\n，即空行
                    int emptyLineEnd = message.indexOf("\r\n\r\n", contentLengthEnd);
                    if (emptyLineEnd != -1) {
                        int contentStart = emptyLineEnd + 4; // 跳过\r\n\r\n
                        // 计算内容结束位置
                        int contentEnd = Math.min(contentStart + contentLength, message.length());
                        return message.substring(contentStart, contentEnd);
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid Content-Length format: " + lengthStr);
                }
            }
        }
        
        // 如果无法解析HTTP-like格式，返回整个消息
        return message;
    }
    
    private void checkInitialized() {
        if (!initialized.get()) {
            throw new IllegalStateException("Not initialized");
        }
        if (shutdown.get()) {
            throw new IllegalStateException("Already shutdown");
        }
    }
    
    /**
     * 获取内部使用的Stdio通道
     * @return Stdio通道
     */
    @Nullable
    public IStdioChannel getStdioChannel() {
        return stdioChannel;
    }
    
    /**
     * 获取内部使用的消息序列化器
     * @return 消息序列化器
     */
    public IMessageSerializer getMessageSerializer() {
        return messageSerializer;
    }
    
    /**
     * 获取内部使用的请求跟踪器
     * @return 请求跟踪器
     */
    public IRequestTracker getRequestTracker() {
        return requestTracker;
    }
    
    /**
     * 获取内部使用的事件分发器
     * @return 事件分发器
     */
    public IEventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }
    
    /**
     * 内部类：订阅信息
     */
    private static class Subscription<T> {
        final String eventName;
        final IEventDispatcher.OmniSharpEventListener listener;
        
        Subscription(String eventName, IEventDispatcher.OmniSharpEventListener listener) {
            this.eventName = eventName;
            this.listener = listener;
        }
    }
}