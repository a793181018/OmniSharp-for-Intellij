package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication;
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpCommunicationException;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager;
import com.github.a793181018.omnisharpforintellij.server.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import reactor.core.publisher.Mono;
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * OmniSharp通信实现类，负责JSON-RPC通信
 */
public class OmniSharpCommunicationImpl implements IOmniSharpCommunication, ProcessListener {
    private static final Logger LOG = Logger.getInstance(OmniSharpCommunicationImpl.class);
    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    
    private final Map<Long, CompletableFuture<?>> pendingRequests = new ConcurrentHashMap<>();
    private final Map<String, EventSubscription<?>> eventSubscriptions = new ConcurrentHashMap<>();
    private final AtomicLong subscriptionCounter = new AtomicLong(1);
    
    private IOmniSharpProcessManager processManager;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final StringBuilder outputBuffer = new StringBuilder();
    
    @Override
    public void initialize(IOmniSharpProcessManager processManager) {
        if (initialized.compareAndSet(false, true)) {
            this.processManager = processManager;
            processManager.addProcessListener(this);
            LOG.info("OmniSharp communication initialized");
        }
    }
    
    @Override
    public <T> CompletableFuture<OmniSharpResponse<T>> sendRequest(OmniSharpRequest<T> request) {
        if (!initialized.get()) {
            throw new IllegalStateException("Communication not initialized");
        }
        
        CompletableFuture<OmniSharpResponse<T>> future = new CompletableFuture<>();
        long requestSeq = request.getRequestSeq();
        
        // 存储待处理请求
        pendingRequests.put(requestSeq, future);
        
        try {
            // 转换请求为JSON
            Map<String, Object> jsonRequest = new HashMap<>();
            jsonRequest.put("command", request.getCommand());
            jsonRequest.put("arguments", request.getArguments());
            jsonRequest.put("request_seq", requestSeq);
            jsonRequest.put("type", "request");
            
            String json = objectMapper.writeValueAsString(jsonRequest);
            LOG.debug("Sending request: " + json);
            
            // 发送请求
            if (processManager.getProcessInputWriter() != null) {
                processManager.getProcessInputWriter().write(json + "\n");
            } else {
                future.completeExceptionally(new OmniSharpCommunicationException("Process input writer is not available"));
                pendingRequests.remove(requestSeq);
            }
            
        } catch (Exception e) {
            pendingRequests.remove(requestSeq);
            future.completeExceptionally(new OmniSharpCommunicationException("Failed to send request", e));
        }
        
        return future;
    }
    
    @Override
    public <T> Mono<OmniSharpResponse<T>> sendRequestReactive(OmniSharpRequest<T> request) {
        return Mono.fromFuture(() -> sendRequest(request))
                .subscribeOn(Schedulers.fromExecutor(executorService));
    }
    
    @Override
    public void sendMessage(String message) {
        if (!initialized.get()) {
            throw new IllegalStateException("Communication not initialized");
        }
        
        try {
            LOG.debug("Sending message: " + message);
            if (processManager.getProcessInputWriter() != null) {
                processManager.getProcessInputWriter().write(message + "\n");
            }
        } catch (Exception e) {
            LOG.error("Failed to send message", e);
            throw new OmniSharpCommunicationException("Failed to send message", e);
        }
    }
    
    @Override
    public <T> String subscribeToEvent(String eventName, Class<T> eventType, Consumer<OmniSharpEvent<T>> listener) {
        String subscriptionId = "sub_" + subscriptionCounter.getAndIncrement();
        eventSubscriptions.put(subscriptionId, new EventSubscription<>(eventName, eventType, listener));
        LOG.info("Subscribed to event: " + eventName + " with ID: " + subscriptionId);
        return subscriptionId;
    }
    
    @Override
    public void unsubscribeFromEvent(String subscriptionId) {
        eventSubscriptions.remove(subscriptionId);
        LOG.info("Unsubscribed from event with ID: " + subscriptionId);
    }
    
    @Override
    public void shutdown() {
        if (initialized.compareAndSet(true, false)) {
            if (processManager != null) {
                processManager.removeProcessListener(this);
            }
            
            // 取消所有待处理请求
            for (CompletableFuture<?> future : pendingRequests.values()) {
                future.cancel(false);
            }
            pendingRequests.clear();
            eventSubscriptions.clear();
            
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            LOG.info("OmniSharp communication shutdown");
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized.get();
    }
    
    @Override
    public void onProcessStarted() {
        LOG.info("Process started, communication ready");
    }
    
    @Override
    public void onProcessOutput(String output) {
        processOutput(output);
    }
    
    @Override
    public void onProcessError(String error) {
        LOG.warn("Process error output: " + error);
    }
    
    @Override
    public void onProcessTerminated(int exitCode) {
        LOG.info("Process terminated, cancelling all pending requests");
        for (CompletableFuture<?> future : pendingRequests.values()) {
            future.completeExceptionally(new OmniSharpCommunicationException("Process terminated with exit code: " + exitCode));
        }
        pendingRequests.clear();
    }
    
    @Override
    public void onProcessStartFailed(Throwable throwable) {
        LOG.error("Process start failed", throwable);
    }
    
    /**
     * 处理进程输出，解析JSON-RPC消息
     */
    private void processOutput(String output) {
        outputBuffer.append(output);
        
        // 尝试解析完整的JSON消息
        try {
            parseJsonMessages(outputBuffer.toString());
        } catch (Exception e) {
            LOG.warn("Error parsing JSON messages", e);
        }
    }
    
    /**
     * 解析JSON消息
     */
    private void parseJsonMessages(String input) throws Exception {
        // 简化的JSON解析，实际可能需要更复杂的处理来处理多行JSON
        int braceCount = 0;
        int startIndex = 0;
        boolean inString = false;
        char prevChar = 0;
        
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            // 处理字符串中的特殊字符
            if (c == '"' && prevChar != '\\') {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{') {
                    if (braceCount == 0) {
                        startIndex = i;
                    }
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        // 找到了完整的JSON对象
                        String jsonMessage = input.substring(startIndex, i + 1);
                        processMessage(jsonMessage);
                        
                        // 移除已处理的消息
                        outputBuffer.delete(0, i + 1);
                        
                        // 重新开始解析剩余内容
                        if (outputBuffer.length() > 0) {
                            parseJsonMessages(outputBuffer.toString());
                        }
                        return;
                    }
                }
            }
            
            prevChar = c;
        }
    }
    
    /**
     * 处理单个消息
     */
    private void processMessage(String jsonMessage) {
        try {
            // 解析基础消息结构
            Map<String, Object> message = objectMapper.readValue(jsonMessage, Map.class);
            String type = (String) message.get("type");
            
            if ("response".equals(type)) {
                processResponse(message);
            } else if ("event".equals(type)) {
                processEvent(message);
            }
        } catch (Exception e) {
            LOG.error("Error processing message: " + jsonMessage, e);
        }
    }
    
    /**
     * 处理响应消息
     */
    @SuppressWarnings("unchecked")
    private void processResponse(Map<String, Object> response) {
        try {
            Number requestSeqNum = (Number) response.get("request_seq");
            if (requestSeqNum == null) {
                LOG.warn("Response missing request_seq: " + response);
                return;
            }
            
            long requestSeq = requestSeqNum.longValue();
            CompletableFuture<?> future = pendingRequests.remove(requestSeq);
            
            if (future != null && !future.isDone()) {
                // 查找对应的请求类型来确定响应类型
                // 这里简化处理，实际应该在请求时保存更多信息
                LOG.debug("Received response for requestSeq: " + requestSeq);
                
                // 转换为响应对象
                OmniSharpResponse<Object> responseObj = new OmniSharpResponse<>(
                        (String) response.get("command"),
                        requestSeq,
                        response.get("body"),
                        Boolean.TRUE.equals(response.get("success")),
                        (String) response.get("message")
                );
                
                ((CompletableFuture<OmniSharpResponse<Object>>) future).complete(responseObj);
            }
        } catch (Exception e) {
            LOG.error("Error processing response", e);
        }
    }
    
    /**
     * 处理事件消息
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void processEvent(Map<String, Object> event) {
        try {
            String eventName = (String) event.get("event");
            if (eventName == null) {
                LOG.warn("Event missing event name: " + event);
                return;
            }
            
            Object body = event.get("body");
            
            // 通知所有订阅该事件的监听器
            for (EventSubscription<?> subscription : eventSubscriptions.values()) {
                if (subscription.getEventName().equals(eventName)) {
                    try {
                        // 转换事件数据为正确的类型
                        Object typedBody = body;
                        if (body instanceof Map && subscription.getEventType() != Object.class) {
                            // 如果需要类型转换
                            String bodyJson = objectMapper.writeValueAsString(body);
                            typedBody = objectMapper.readValue(bodyJson, subscription.getEventType());
                        }
                        
                        OmniSharpEvent eventObj = new OmniSharpEvent<>(eventName, typedBody);
                        subscription.getListener().accept(eventObj);
                    } catch (Exception e) {
                        LOG.error("Error notifying event listener", e);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Error processing event", e);
        }
    }
    
    /**
     * 事件订阅类
     */
    private static class EventSubscription<T> {
        private final String eventName;
        private final Class<T> eventType;
        private final Consumer<OmniSharpEvent<T>> listener;
        
        public EventSubscription(String eventName, Class<T> eventType, Consumer<OmniSharpEvent<T>> listener) {
            this.eventName = eventName;
            this.eventType = eventType;
            this.listener = listener;
        }
        
        public String getEventName() {
            return eventName;
        }
        
        public Class<T> getEventType() {
            return eventType;
        }
        
        public Consumer<OmniSharpEvent<T>> getListener() {
            return listener;
        }
    }
}