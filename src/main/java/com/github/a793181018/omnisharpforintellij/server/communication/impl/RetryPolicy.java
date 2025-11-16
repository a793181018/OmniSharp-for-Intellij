package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.github.a793181018.omnisharpforintellij.server.communication.CircuitOpenException;

/**
 * 重试策略实现，支持可配置的重试次数、退避策略和异常过滤
 */
public class RetryPolicy {
    private static final Logger LOGGER = Logger.getLogger(RetryPolicy.class.getName());
    
    private final int maxRetries;
    private final long initialBackoffMs;
    private final double backoffMultiplier;
    private final long maxBackoffMs;
    private final Set<Class<? extends Throwable>> retryableExceptions;
    private final Predicate<Throwable> exceptionFilter;
    
    /**
     * 创建默认配置的重试策略
     */
    public RetryPolicy() {
        this(3, 1000, 1.5, 30000);
    }
    
    /**
     * 创建自定义配置的重试策略
     * @param maxRetries 最大重试次数
     * @param initialBackoffMs 初始退避时间（毫秒）
     * @param backoffMultiplier 退避时间乘数
     * @param maxBackoffMs 最大退避时间（毫秒）
     */
    public RetryPolicy(int maxRetries, long initialBackoffMs, double backoffMultiplier, long maxBackoffMs) {
        this.maxRetries = maxRetries > 0 ? maxRetries : 3;
        this.initialBackoffMs = initialBackoffMs > 0 ? initialBackoffMs : 1000;
        this.backoffMultiplier = backoffMultiplier > 1.0 ? backoffMultiplier : 1.5;
        this.maxBackoffMs = maxBackoffMs > initialBackoffMs ? maxBackoffMs : 30000;
        this.retryableExceptions = new HashSet<>();
        this.exceptionFilter = null;
        
        LOGGER.info("RetryPolicy initialized with maxRetries=" + maxRetries +
                   ", initialBackoffMs=" + initialBackoffMs +
                   ", backoffMultiplier=" + backoffMultiplier +
                   ", maxBackoffMs=" + maxBackoffMs);
    }
    
    /**
     * 私有构造函数，用于构建器模式
     */
    private RetryPolicy(Builder builder) {
        this.maxRetries = builder.maxRetries;
        this.initialBackoffMs = builder.initialBackoffMs;
        this.backoffMultiplier = builder.backoffMultiplier;
        this.maxBackoffMs = builder.maxBackoffMs;
        this.retryableExceptions = builder.retryableExceptions;
        this.exceptionFilter = builder.exceptionFilter;
    }
    
    /**
     * 获取最大重试次数
     * @return 最大重试次数
     */
    public int getMaxRetries() {
        return maxRetries;
    }
    
    /**
     * 获取指定尝试次数的退避时间
     * @param attempt 尝试次数（从1开始）
     * @return 退避时间（毫秒）
     */
    public long getBackoffTime(int attempt) {
        return calculateBackoffTime(attempt - 1); // 内部计算从0开始
    }
    
    /**
     * 检查异常是否可重试
     * @param exception 要检查的异常
     * @return 是否可重试
     */
    public boolean isRetryableException(Throwable exception) {
        return shouldRetry(exception);
    }
    
    /**
     * 执行带重试的操作（异步版本）
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果的CompletableFuture
     */
    public <T> CompletableFuture<T> executeWithRetry(Supplier<CompletableFuture<T>> supplier) {
        return executeWithRetryAsync(supplier, 0);
    }
    
    /**
     * 异步重试执行逻辑
     */
    private <T> CompletableFuture<T> executeWithRetryAsync(Supplier<CompletableFuture<T>> supplier, int attempt) {
        CompletableFuture<T> resultFuture = new CompletableFuture<>();
        
        try {
            if (attempt > 0) {
                LOGGER.info("Retry attempt " + attempt + " for operation");
            }
            
            CompletableFuture<T> future = supplier.get();
            future.thenAccept(resultFuture::complete)
                  .exceptionally(ex -> {
                      Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                      Exception e = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
                      
                      if (!shouldRetry(e)) {
                          LOGGER.log(Level.WARNING, "Exception not retryable, aborting: " + e.getMessage(), e);
                          resultFuture.completeExceptionally(e);
                          return null;
                      }
                      
                      if (attempt >= maxRetries) {
                          LOGGER.log(Level.SEVERE, "Operation failed after " + maxRetries + " retries", e);
                          resultFuture.completeExceptionally(e);
                          return null;
                      }
                      
                      long backoffTime = calculateBackoffTime(attempt);
                      LOGGER.log(Level.WARNING, "Operation failed, retrying in " + backoffTime + "ms (attempt " + 
                                 (attempt + 1) + "/" + maxRetries + ")", e);
                      
                      // 使用定时器延迟执行下一次重试
                      java.util.Timer timer = new java.util.Timer(true);
                      timer.schedule(new java.util.TimerTask() {
                          @Override
                          public void run() {
                              try {
                                  CompletableFuture<T> nextAttempt = executeWithRetryAsync(supplier, attempt + 1);
                                  nextAttempt.thenAccept(resultFuture::complete)
                                            .exceptionally(ex -> {
                                                resultFuture.completeExceptionally(ex);
                                                return null;
                                            });
                              } catch (Exception timerEx) {
                                  resultFuture.completeExceptionally(timerEx);
                              }
                          }
                      }, backoffTime);
                      
                      return null;
                  });
        } catch (Exception e) {
            resultFuture.completeExceptionally(e);
        }
        
        return resultFuture;
    }
    
    /**
     * 执行带重试的操作
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 操作结果
     * @throws Exception 当所有重试都失败时抛出最后一个异常
     */
    public <T> T execute(Supplier<T> supplier) throws Exception {
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                if (attempt > 0) {
                    LOGGER.info("Retry attempt " + attempt + " for operation");
                }
                return supplier.get();
            } catch (Exception e) {
                lastException = e;
                
                // 检查异常是否可重试
                if (!shouldRetry(e)) {
                    LOGGER.log(Level.WARNING, "Exception not retryable, aborting: " + e.getMessage(), e);
                    throw e;
                }
                
                // 如果是最后一次尝试，直接抛出
                if (attempt >= maxRetries) {
                    LOGGER.log(Level.SEVERE, "Operation failed after " + maxRetries + " retries", e);
                    throw e;
                }
                
                // 计算退避时间
                long backoffTime = calculateBackoffTime(attempt);
                LOGGER.log(Level.WARNING, "Operation failed, retrying in " + backoffTime + "ms (attempt " + 
                           (attempt + 1) + "/" + maxRetries + ")", e);
                
                // 执行退避
                try {
                    TimeUnit.MILLISECONDS.sleep(backoffTime);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    LOGGER.log(Level.SEVERE, "Retry interrupted", ie);
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }
        
        // 不应该到达这里，但为了安全，抛出最后一个异常
        throw lastException != null ? lastException : new RuntimeException("Operation failed without exception");
    }
    
    /**
     * 异步执行带重试的操作
     * @param supplier 要执行的操作
     * @param <T> 返回类型
     * @return 包含结果的CompletableFuture
     */
    public <T> java.util.concurrent.CompletableFuture<T> executeAsync(Supplier<T> supplier) {
        java.util.concurrent.CompletableFuture<T> resultFuture = new java.util.concurrent.CompletableFuture<>();
        
        executeWithRetryAsync(supplier, resultFuture, 0);
        
        return resultFuture;
    }
    
    /**
     * 异步重试逻辑实现
     */
    private <T> void executeWithRetryAsync(Supplier<T> supplier, 
                                         java.util.concurrent.CompletableFuture<T> resultFuture,
                                         int attempt) {
        if (attempt > 0) {
            LOGGER.info("Async retry attempt " + attempt + " for operation");
        }
        
        java.util.concurrent.CompletableFuture.supplyAsync(supplier)
            .thenAccept(resultFuture::complete)
            .exceptionally(ex -> {
                Throwable cause = ex instanceof java.util.concurrent.CompletionException ? ex.getCause() : ex;
                Exception e = cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
                
                if (!shouldRetry(e)) {
                    LOGGER.log(Level.WARNING, "Async exception not retryable, aborting: " + e.getMessage(), e);
                    resultFuture.completeExceptionally(e);
                    return null;
                }
                
                if (attempt >= maxRetries) {
                    LOGGER.log(Level.SEVERE, "Async operation failed after " + maxRetries + " retries", e);
                    resultFuture.completeExceptionally(e);
                    return null;
                }
                
                long backoffTime = calculateBackoffTime(attempt);
                LOGGER.log(Level.WARNING, "Async operation failed, retrying in " + backoffTime + "ms (attempt " + 
                           (attempt + 1) + "/" + maxRetries + ")", e);
                
                // 使用定时器延迟执行下一次重试
                java.util.Timer timer = new java.util.Timer(true);
                timer.schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        executeWithRetryAsync(supplier, resultFuture, attempt + 1);
                    }
                }, backoffTime);
                
                return null;
            });
    }
    
    /**
     * 计算退避时间
     * @param attempt 当前尝试次数（从0开始）
     * @return 退避时间（毫秒）
     */
    private long calculateBackoffTime(int attempt) {
        // 指数退避：initialBackoff * (multiplier ^ attempt)
        double backoff = initialBackoffMs * Math.pow(backoffMultiplier, attempt);
        
        // 添加一些随机抖动以避免雪崩效应
        double jitter = 0.8 + Math.random() * 0.4; // 80%到120%之间的随机因子
        
        // 确保不超过最大退避时间
        return Math.min((long)(backoff * jitter), maxBackoffMs);
    }
    
    /**
     * 检查异常是否应该重试
     * @param exception 发生的异常
     * @return 是否应该重试
     */
    private boolean shouldRetry(Throwable exception) {
        // 如果有自定义异常过滤器，使用它
        if (exceptionFilter != null) {
            return exceptionFilter.test(exception);
        }
        
        // 如果有明确指定的可重试异常列表，检查异常是否在列表中
        if (!retryableExceptions.isEmpty()) {
            Class<?> exceptionClass = exception.getClass();
            for (Class<? extends Throwable> retryableClass : retryableExceptions) {
                if (retryableClass.isAssignableFrom(exceptionClass)) {
                    return true;
                }
            }
            return false;
        }
        
        // 默认重试逻辑：网络相关异常、超时异常等应该重试
        return exception instanceof java.io.IOException ||
               exception instanceof java.net.SocketException ||
               exception instanceof java.net.SocketTimeoutException ||
               exception instanceof java.net.ConnectException ||
               exception instanceof java.net.UnknownHostException ||
               exception instanceof java.util.concurrent.TimeoutException ||
               exception instanceof CircuitBreaker.CircuitBreakerException && 
               !(exception instanceof CircuitOpenException);
    }
    
    /**
     * 构建器模式实现
     */
    public static class Builder {
        private int maxRetries = 3;
        private long initialBackoffMs = 1000;
        private double backoffMultiplier = 1.5;
        private long maxBackoffMs = 30000;
        private Set<Class<? extends Throwable>> retryableExceptions = new HashSet<>();
        private Predicate<Throwable> exceptionFilter = null;
        
        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }
        
        public Builder initialBackoffMs(long initialBackoffMs) {
            this.initialBackoffMs = initialBackoffMs;
            return this;
        }
        
        public Builder backoffMultiplier(double backoffMultiplier) {
            this.backoffMultiplier = backoffMultiplier;
            return this;
        }
        
        public Builder maxBackoffMs(long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
            return this;
        }
        
        public Builder retryableExceptions(Collection<Class<? extends Throwable>> exceptions) {
            this.retryableExceptions.addAll(exceptions);
            return this;
        }
        
        public Builder retryableExceptions(Class<? extends Throwable>... exceptions) {
            this.retryableExceptions.addAll(Arrays.asList(exceptions));
            return this;
        }
        
        public Builder exceptionFilter(Predicate<Throwable> filter) {
            this.exceptionFilter = filter;
            return this;
        }
        
        public RetryPolicy build() {
            return new RetryPolicy(this);
        }
    }
    
    /**
     * 创建构建器
     * @return 新的构建器实例
     */
    public static Builder builder() {
        return new Builder();
    }
}