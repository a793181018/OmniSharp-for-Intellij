package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.function.Supplier;

/**
 * 断路器实现，用于故障隔离和自动恢复
 */
public class CircuitBreaker {
    private static final Logger LOGGER = Logger.getLogger(CircuitBreaker.class.getName());
    
    /**
     * 断路器状态
     */
    public enum State {
        /** 关闭状态：允许请求通过 */
        CLOSED,
        /** 打开状态：拒绝请求 */
        OPEN,
        /** 半开状态：允许有限请求通过以测试服务是否恢复 */
        HALF_OPEN
    }
    
    private final int failureThreshold;
    private final long resetTimeoutMs;
    private final int halfOpenMaxCalls;
    
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicInteger halfOpenSuccessCount = new AtomicInteger(0);
    private final AtomicLong lastFailureTime = new AtomicLong(0);
    private volatile State state = State.CLOSED;
    
    /**
     * 创建默认配置的断路器
     */
    public CircuitBreaker() {
        this(5, 30000, 2); // 默认5次失败触发打开，30秒超时，半开状态最多2个请求
    }
    
    /**
     * 创建自定义配置的断路器
     * @param failureThreshold 失败阈值，超过此值触发断路
     * @param resetTimeoutMs 重置超时时间（毫秒）
     * @param halfOpenMaxCalls 半开状态下允许的最大请求数
     */
    public CircuitBreaker(int failureThreshold, long resetTimeoutMs, int halfOpenMaxCalls) {
        this.failureThreshold = failureThreshold > 0 ? failureThreshold : 5;
        this.resetTimeoutMs = resetTimeoutMs > 0 ? resetTimeoutMs : 30000;
        this.halfOpenMaxCalls = halfOpenMaxCalls > 0 ? halfOpenMaxCalls : 2;
        LOGGER.info("CircuitBreaker initialized with failureThreshold=" + failureThreshold + 
                   ", resetTimeoutMs=" + resetTimeoutMs + 
                   ", halfOpenMaxCalls=" + halfOpenMaxCalls);
    }
    
    /**
     * 执行受断路器保护的操作
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws CircuitBreakerException 当断路器打开或操作失败时抛出
     */
    public <T> T execute(Supplier<T> supplier) throws CircuitBreakerException {
        // 检查断路器状态
        if (!allowRequest()) {
            throw new CircuitOpenException("Circuit breaker is open");
        }
        
        try {
            // 执行操作
            T result = supplier.get();
            // 记录成功
            recordSuccess();
            return result;
        } catch (Exception e) {
            // 记录失败
            recordFailure();
            // 重新抛出异常，包装为CircuitBreakerException
            throw new CircuitBreakerException("Operation failed", e);
        }
    }
    
    /**
     * 执行带回退的断路器保护操作
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     */
    public <T> T executeWithFallback(Supplier<T> supplier) {
        try {
            return execute(supplier);
        } catch (CircuitBreakerException e) {
            LOGGER.log(Level.WARNING, "Circuit breaker prevented operation: " + e.getMessage(), e);
            // 如果断路器打开，尝试等待一段时间后重试一次
            if (e instanceof CircuitOpenException) {
                try {
                    Thread.sleep(1000); // 等待1秒
                    return execute(supplier);
                } catch (Exception ex) {
                    // 再次失败，抛出异常
                    throw new RuntimeException("Operation failed even after retry", ex);
                }
            }
            throw new RuntimeException("Operation failed", e);
        }
    }
    
    /**
     * 异步执行受断路器保护的操作
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 包含结果的Future或异常
     */
    public <T> java.util.concurrent.CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        java.util.concurrent.CompletableFuture<T> future = new java.util.concurrent.CompletableFuture<>();
        
        if (!allowRequest()) {
            future.completeExceptionally(new CircuitOpenException("Circuit breaker is open"));
            return future;
        }
        
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                T result = supplier.get();
                recordSuccess();
                future.complete(result);
            } catch (Exception e) {
                recordFailure();
                future.completeExceptionally(new CircuitBreakerException("Operation failed", e));
            }
        });
        
        return future;
    }
    
    /**
     * 检查是否允许请求通过
     * @return 是否允许请求
     */
    public boolean allowRequest() {
        synchronized (this) {
            switch (state) {
                case CLOSED:
                    return true;
                case OPEN:
                    // 检查是否可以转换到半开状态
                    if (System.currentTimeMillis() - lastFailureTime.get() > resetTimeoutMs) {
                        state = State.HALF_OPEN;
                        halfOpenSuccessCount.set(0);
                        LOGGER.info("Circuit breaker changed from OPEN to HALF_OPEN");
                        return true;
                    }
                    return false;
                case HALF_OPEN:
                    // 半开状态下限制请求数量
                    return halfOpenSuccessCount.get() < halfOpenMaxCalls;
                default:
                    return false;
            }
        }
    }
    
    /**
     * 记录成功操作
     */
    public void recordSuccess() {
        synchronized (this) {
            switch (state) {
                case CLOSED:
                    // 关闭状态下，成功时重置失败计数
                    failureCount.set(0);
                    break;
                case HALF_OPEN:
                    // 半开状态下，记录成功计数
                    int successCount = halfOpenSuccessCount.incrementAndGet();
                    if (successCount >= halfOpenMaxCalls) {
                        // 足够的成功，关闭断路器
                        state = State.CLOSED;
                        failureCount.set(0);
                        LOGGER.info("Circuit breaker changed from HALF_OPEN to CLOSED");
                    }
                    break;
                case OPEN:
                    // 打开状态下的成功不应该发生，忽略
                    break;
            }
        }
    }
    
    /**
     * 记录失败操作
     */
    public void recordFailure() {
        synchronized (this) {
            switch (state) {
                case CLOSED:
                    // 关闭状态下，增加失败计数
                    int currentFailures = failureCount.incrementAndGet();
                    if (currentFailures >= failureThreshold) {
                        // 达到阈值，打开断路器
                        state = State.OPEN;
                        lastFailureTime.set(System.currentTimeMillis());
                        LOGGER.warning("Circuit breaker changed from CLOSED to OPEN after " + currentFailures + " failures");
                    }
                    break;
                case HALF_OPEN:
                    // 半开状态下的失败，立即打开断路器
                    state = State.OPEN;
                    lastFailureTime.set(System.currentTimeMillis());
                    LOGGER.warning("Circuit breaker changed from HALF_OPEN to OPEN");
                    break;
                case OPEN:
                    // 打开状态下的失败，更新最后失败时间
                    lastFailureTime.set(System.currentTimeMillis());
                    break;
            }
        }
    }
    
    /**
     * 重置断路器到关闭状态
     */
    public synchronized void reset() {
        state = State.CLOSED;
        failureCount.set(0);
        halfOpenSuccessCount.set(0);
        LOGGER.info("Circuit breaker reset to CLOSED");
    }
    
    /**
     * 获取当前断路器状态
     * @return 断路器状态
     */
    public State getState() {
        return state;
    }
    
    /**
     * 获取当前失败计数
     * @return 失败计数
     */
    public int getFailureCount() {
        return failureCount.get();
    }
    
    /**
     * 获取最后失败时间
     * @return 最后失败时间戳（毫秒）
     */
    public long getLastFailureTime() {
        return lastFailureTime.get();
    }
    
    /**
     * 断路器异常基类
     */
    public static class CircuitBreakerException extends RuntimeException {
        public CircuitBreakerException(String message) {
            super(message);
        }
        
        public CircuitBreakerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * 断路器打开异常
     */
    public static class CircuitOpenException extends CircuitBreakerException {
        public CircuitOpenException(String message) {
            super(message);
        }
    }
}