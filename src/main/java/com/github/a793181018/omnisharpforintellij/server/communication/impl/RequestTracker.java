package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.github.a793181018.omnisharpforintellij.server.communication.IRequestTracker;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 请求跟踪器实现，用于管理和跟踪OmniSharp请求
 */
public class RequestTracker implements IRequestTracker {
    private static final Logger LOGGER = Logger.getLogger(RequestTracker.class.getName());
    private static final long DEFAULT_REQUEST_TIMEOUT_MS = 30000; // 默认30秒超时
    private static final int DEFAULT_MAX_PENDING_REQUESTS = 1000; // 默认最大待处理请求数
    
    private final ConcurrentHashMap<Long, TrackedRequest> requestMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "OmniSharp-Request-Timeout-Checker");
        thread.setDaemon(true);
        return thread;
    });
    private long requestTimeoutMs = DEFAULT_REQUEST_TIMEOUT_MS;
    private final int maxPendingRequests;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    /**
     * 创建默认配置的请求跟踪器
     */
    public RequestTracker() {
        this(DEFAULT_MAX_PENDING_REQUESTS);
    }
    
    /**
     * 创建指定最大请求数的请求跟踪器
     * @param maxPendingRequests 最大待处理请求数
     */
    public RequestTracker(int maxPendingRequests) {
        this.maxPendingRequests = maxPendingRequests > 0 ? maxPendingRequests : DEFAULT_MAX_PENDING_REQUESTS;
        // 启动定期清理任务
        scheduler.scheduleAtFixedRate(this::cleanupTimedOutRequests, 
                requestTimeoutMs / 2, requestTimeoutMs / 2, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public CompletableFuture<OmniSharpResponse<?>> trackRequest(OmniSharpRequest<?> request) {
        if (shutdown.get()) {
            throw new IllegalStateException("RequestTracker is shutdown");
        }
        
        // 检查是否超过最大请求数
        if (requestMap.size() >= maxPendingRequests) {
            CompletableFuture<OmniSharpResponse<?>> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalStateException("Too many pending requests: " + requestMap.size()));
            return future;
        }
        
        long requestSeq = request.getSeq();
        CompletableFuture<OmniSharpResponse<?>> future = new CompletableFuture<>();
        
        // 设置超时处理
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(() -> {
            TrackedRequest tracked = requestMap.remove(requestSeq);
            if (tracked != null && !future.isDone()) {
                future.completeExceptionally(new TimeoutException("Request timed out after " + requestTimeoutMs + "ms: " + request.getCommand()));
                LOGGER.warning("Request timed out: " + request.getCommand() + " (seq: " + requestSeq + ")");
            }
        }, requestTimeoutMs, TimeUnit.MILLISECONDS);
        
        // 添加取消处理
        future.whenComplete((response, throwable) -> {
            if (throwable instanceof CancellationException) {
                timeoutFuture.cancel(false);
                requestMap.remove(requestSeq);
                LOGGER.fine("Request cancelled: " + request.getCommand() + " (seq: " + requestSeq + ")");
            }
        });
        
        // 存储请求
        TrackedRequest trackedRequest = new TrackedRequest(future, timeoutFuture);
        requestMap.put(requestSeq, trackedRequest);
        
        LOGGER.fine("Tracking request: " + request.getCommand() + " (seq: " + requestSeq + ")");
        return future;
    }
    
    @Override
    public CompletableFuture<OmniSharpResponse<?>> getResponseFuture(long requestSeq) {
        TrackedRequest tracked = requestMap.get(requestSeq);
        return tracked != null ? tracked.future : null;
    }
    
    @Override
    public boolean completeRequest(OmniSharpResponse<?> response) {
        if (response == null) {
            return false;
        }
        
        long requestSeq = response.getRequest_seq();
        TrackedRequest tracked = requestMap.remove(requestSeq);
        
        if (tracked != null) {
            // 取消超时任务
            tracked.timeoutFuture.cancel(false);
            
            // 完成Future
            if (!tracked.future.isDone()) {
                tracked.future.complete(response);
                LOGGER.fine("Request completed: " + response.getCommand() + " (seq: " + requestSeq + ")");
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean cancelRequest(long requestSeq) {
        TrackedRequest tracked = requestMap.remove(requestSeq);
        
        if (tracked != null) {
            tracked.timeoutFuture.cancel(false);
            if (!tracked.future.isDone()) {
                tracked.future.cancel(true);
                LOGGER.fine("Request cancelled: " + requestSeq);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public void cancelAllRequests() {
        for (Map.Entry<Long, TrackedRequest> entry : requestMap.entrySet()) {
            TrackedRequest tracked = entry.getValue();
            tracked.timeoutFuture.cancel(false);
            if (!tracked.future.isDone()) {
                tracked.future.cancel(true);
            }
        }
        
        requestMap.clear();
        LOGGER.info("All pending requests cancelled");
    }
    
    @Override
    public int getPendingRequestCount() {
        return requestMap.size();
    }
    
    @Override
    public void cleanupTimedOutRequests() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<Long, TrackedRequest> entry : requestMap.entrySet()) {
            TrackedRequest tracked = entry.getValue();
            
            // 检查是否超时
            if (currentTime - tracked.creationTime > requestTimeoutMs) {
                long requestSeq = entry.getKey();
                requestMap.remove(requestSeq);
                tracked.timeoutFuture.cancel(false);
                
                if (!tracked.future.isDone()) {
                    tracked.future.completeExceptionally(new TimeoutException("Request timed out: " + requestSeq));
                    LOGGER.warning("Cleaned up timed out request: " + requestSeq);
                }
            }
        }
    }
    
    @Override
    public void setRequestTimeout(long timeoutMs) {
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.requestTimeoutMs = timeoutMs;
        LOGGER.info("Request timeout set to " + timeoutMs + "ms");
    }
    
    @Override
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            // 取消所有请求
            cancelAllRequests();
            
            // 关闭调度器
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                scheduler.shutdownNow();
            }
            
            LOGGER.info("RequestTracker shutdown");
        }
    }
    
    /**
     * 获取最大待处理请求数
     * @return 最大请求数
     */
    public int getMaxPendingRequests() {
        return maxPendingRequests;
    }
    
    /**
     * 获取当前请求超时时间
     * @return 超时时间（毫秒）
     */
    public long getRequestTimeout() {
        return requestTimeoutMs;
    }
    
    /**
     * 检查是否已关闭
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    /**
     * 内部类：跟踪的请求信息
     */
    private static class TrackedRequest {
        final CompletableFuture<OmniSharpResponse<?>> future;
        final ScheduledFuture<?> timeoutFuture;
        final long creationTime;
        
        TrackedRequest(CompletableFuture<OmniSharpResponse<?>> future, ScheduledFuture<?> timeoutFuture) {
            this.future = future;
            this.timeoutFuture = timeoutFuture;
            this.creationTime = System.currentTimeMillis();
        }
    }
}