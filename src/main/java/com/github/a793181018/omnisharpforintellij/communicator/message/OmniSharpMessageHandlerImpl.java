package com.github.a793181018.omnisharpforintellij.communicator.message;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.a793181018.omnisharpforintellij.communicator.client.OmniSharpServerClient;
import com.github.a793181018.omnisharpforintellij.session.OmniSharpSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * OmniSharp消息处理器的默认实现
 */
public class OmniSharpMessageHandlerImpl implements OmniSharpMessageHandler {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final OmniSharpSession session;
    private final Map<String, CompletableFuture<OmniSharpResponse>> pendingRequests = new ConcurrentHashMap<>();
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final OmniSharpServerClient serverClient;
    
    public OmniSharpMessageHandlerImpl(@NotNull OmniSharpSession session, @NotNull OmniSharpServerClient serverClient) {
        this.session = session;
        this.serverClient = serverClient;
        this.executorService = Executors.newCachedThreadPool(r -> {
            Thread thread = new Thread(r, "OmniSharp-Message-Handler");
            thread.setDaemon(true);
            return thread;
        });
        // 启动响应监听线程
        startResponseListener();
    }
    
    @Override
    public CompletableFuture<OmniSharpResponse> handleRequest(@NotNull OmniSharpRequest request) {
        if (!isRunning.get()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Handler is not running"));
        }
        
        CompletableFuture<OmniSharpResponse> future = new CompletableFuture<>();
        pendingRequests.put(request.getMessageId(), future);
        
        executorService.submit(() -> {
            try {
                String jsonRequest = request.toJson();
                // 添加消息头，格式为：Content-Length: {length}\r\n\r\n{content}
                String fullRequest = "Content-Length: " + jsonRequest.getBytes().length + "\r\n\r\n" + jsonRequest;
                serverClient.send(fullRequest);
            } catch (Exception e) {
                future.completeExceptionally(e);
                pendingRequests.remove(request.getMessageId());
                handleError(request, e);
            }
        });
        
        return future;
    }
    
    @Override
    public void handleResponse(@NotNull OmniSharpResponse response) {
        CompletableFuture<OmniSharpResponse> future = pendingRequests.remove(response.getRequestId());
        if (future != null) {
            future.complete(response);
        }
        // 可以在这里添加额外的响应处理逻辑
    }
    
    @Override
    public void handleError(@NotNull OmniSharpRequest request, @NotNull Throwable error) {
        CompletableFuture<OmniSharpResponse> future = pendingRequests.remove(request.getMessageId());
        if (future != null) {
            future.completeExceptionally(error);
        }
        // 记录错误日志
        System.err.println("Error handling request: " + request.getCommand() + ", messageId: " + request.getMessageId());
        error.printStackTrace();
    }
    
    @Override
    public void shutdown() {
        if (isRunning.compareAndSet(true, false)) {
            executorService.shutdown();
            try {
                executorService.awaitTermination(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // 完成所有待处理的请求
            for (Map.Entry<String, CompletableFuture<OmniSharpResponse>> entry : pendingRequests.entrySet()) {
                entry.getValue().cancel(true);
            }
            pendingRequests.clear();
        }
    }
    
    /**
     * 启动响应监听线程
     */
    private void startResponseListener() {
        executorService.submit(() -> {
            while (isRunning.get()) {
                try {
                    String response = serverClient.receive();
                    if (response != null) {
                        processRawResponse(response);
                    }
                } catch (Exception e) {
                    if (isRunning.get()) {
                        System.err.println("Error receiving response:");
                        e.printStackTrace();
                    }
                }
            }
        });
    }
    
    /**
     * 处理原始响应字符串
     */
    private void processRawResponse(@NotNull String rawResponse) {
        try {
            // 解析JSON响应
            JsonNode jsonNode = OBJECT_MAPPER.readTree(rawResponse);
            String requestId = jsonNode.has("Request_seq") ? jsonNode.get("Request_seq").asText() : null;
            boolean success = !jsonNode.has("Success") || jsonNode.get("Success").asBoolean(true);
            String message = jsonNode.has("Message") ? jsonNode.get("Message").asText() : null;
            JsonNode body = jsonNode.has("Body") ? jsonNode.get("Body") : null;
            String command = jsonNode.has("Command") ? jsonNode.get("Command").asText() : "unknown";
            
            if (requestId != null) {
                OmniSharpResponse response = new OmniSharpResponse(requestId, success, message, body, command);
                handleResponse(response);
            }
        } catch (IOException e) {
            System.err.println("Failed to parse response: " + rawResponse);
            e.printStackTrace();
        }
    }
}