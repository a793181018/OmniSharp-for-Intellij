package com.github.a793181018.omnisharpforintellij.server.communication;

import com.github.a793181018.omnisharpforintellij.server.communication.impl.CircuitBreaker;
import com.github.a793181018.omnisharpforintellij.server.communication.impl.RetryPolicy;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

/**
 * OmniSharp通信配置类
 * 使用构建器模式提供灵活的配置选项
 */
public class OmniSharpConfig {
    // 基本配置
    private final String executablePath;
    private final String workingDirectory;
    private final String[] additionalArgs;
    
    // 通信配置
    private final int requestTimeoutMs;
    private final int maxMessageSize;
    private final String encoding;
    
    // 线程池配置
    private final int dispatcherThreadPoolSize;
    private final int readerThreadPriority;
    
    // 断路器配置
    private final int circuitBreakerFailureThreshold;
    private final long circuitBreakerResetTimeoutMs;
    private final int circuitBreakerHalfOpenMaxCalls;
    
    // 重试策略配置
    private final int retryPolicyMaxRetries;
    private final long retryPolicyInitialBackoffMs;
    private final double retryPolicyBackoffMultiplier;
    private final long retryPolicyMaxBackoffMs;
    
    // 日志配置
    private final Level logLevel;
    private final boolean logRequests;
    private final boolean logResponses;
    
    /**
     * 创建默认配置
     */
    public OmniSharpConfig() {
        this(new Builder());
    }
    
    /**
     * 通过构建器创建配置
     */
    private OmniSharpConfig(Builder builder) {
        this.executablePath = builder.executablePath;
        this.workingDirectory = builder.workingDirectory;
        this.additionalArgs = builder.additionalArgs;
        this.requestTimeoutMs = builder.requestTimeoutMs;
        this.maxMessageSize = builder.maxMessageSize;
        this.encoding = builder.encoding;
        this.dispatcherThreadPoolSize = builder.dispatcherThreadPoolSize;
        this.readerThreadPriority = builder.readerThreadPriority;
        this.circuitBreakerFailureThreshold = builder.circuitBreakerFailureThreshold;
        this.circuitBreakerResetTimeoutMs = builder.circuitBreakerResetTimeoutMs;
        this.circuitBreakerHalfOpenMaxCalls = builder.circuitBreakerHalfOpenMaxCalls;
        this.retryPolicyMaxRetries = builder.retryPolicyMaxRetries;
        this.retryPolicyInitialBackoffMs = builder.retryPolicyInitialBackoffMs;
        this.retryPolicyBackoffMultiplier = builder.retryPolicyBackoffMultiplier;
        this.retryPolicyMaxBackoffMs = builder.retryPolicyMaxBackoffMs;
        this.logLevel = builder.logLevel;
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
    }
    
    /**
     * 创建断路器实例
     */
    public CircuitBreaker createCircuitBreaker() {
        return new CircuitBreaker(
            circuitBreakerFailureThreshold,
            circuitBreakerResetTimeoutMs,
            circuitBreakerHalfOpenMaxCalls
        );
    }
    
    /**
     * 创建重试策略实例
     */
    public RetryPolicy createRetryPolicy() {
        return new RetryPolicy(
            retryPolicyMaxRetries,
            retryPolicyInitialBackoffMs,
            retryPolicyBackoffMultiplier,
            retryPolicyMaxBackoffMs
        );
    }
    
    // Getters
    public String getExecutablePath() {
        return executablePath;
    }
    
    public String getWorkingDirectory() {
        return workingDirectory;
    }
    
    public String[] getAdditionalArgs() {
        return additionalArgs != null ? additionalArgs.clone() : new String[0];
    }
    
    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }
    
    public int getMaxMessageSize() {
        return maxMessageSize;
    }
    
    public String getEncoding() {
        return encoding;
    }
    
    public int getDispatcherThreadPoolSize() {
        return dispatcherThreadPoolSize;
    }
    
    public int getReaderThreadPriority() {
        return readerThreadPriority;
    }
    
    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }
    
    public long getCircuitBreakerResetTimeoutMs() {
        return circuitBreakerResetTimeoutMs;
    }
    
    public int getCircuitBreakerHalfOpenMaxCalls() {
        return circuitBreakerHalfOpenMaxCalls;
    }
    
    public int getRetryPolicyMaxRetries() {
        return retryPolicyMaxRetries;
    }
    
    public long getRetryPolicyInitialBackoffMs() {
        return retryPolicyInitialBackoffMs;
    }
    
    public double getRetryPolicyBackoffMultiplier() {
        return retryPolicyBackoffMultiplier;
    }
    
    public long getRetryPolicyMaxBackoffMs() {
        return retryPolicyMaxBackoffMs;
    }
    
    public Level getLogLevel() {
        return logLevel;
    }
    
    public boolean isLogRequests() {
        return logRequests;
    }
    
    public boolean isLogResponses() {
        return logResponses;
    }
    
    /**
     * 构建器类
     */
    public static class Builder {
        private String executablePath = "omnisharp/OmniSharp.exe";
        private String workingDirectory = System.getProperty("user.dir");
        private String[] additionalArgs = new String[]{};
        private int requestTimeoutMs = 30000;
        private int maxMessageSize = 1024 * 1024; // 1MB
        private String encoding = "UTF-8";
        private int dispatcherThreadPoolSize = Runtime.getRuntime().availableProcessors();
        private int readerThreadPriority = Thread.NORM_PRIORITY;
        private int circuitBreakerFailureThreshold = 5;
        private long circuitBreakerResetTimeoutMs = 30000; // 30秒
        private int circuitBreakerHalfOpenMaxCalls = 2;
        private int retryPolicyMaxRetries = 3;
        private long retryPolicyInitialBackoffMs = 1000; // 1秒
        private double retryPolicyBackoffMultiplier = 1.5;
        private long retryPolicyMaxBackoffMs = 30000; // 30秒
        private Level logLevel = Level.INFO;
        private boolean logRequests = false;
        private boolean logResponses = false;
        
        /**
         * 设置OmniSharp可执行文件路径
         */
        public Builder executablePath(String path) {
            this.executablePath = path;
            return this;
        }
        
        /**
         * 设置工作目录
         */
        public Builder workingDirectory(String directory) {
            this.workingDirectory = directory;
            return this;
        }
        
        /**
         * 设置附加命令行参数
         */
        public Builder additionalArgs(String... args) {
            this.additionalArgs = args;
            return this;
        }
        
        /**
         * 设置请求超时时间
         */
        public Builder requestTimeout(long timeout, TimeUnit unit) {
            this.requestTimeoutMs = Math.toIntExact(unit.toMillis(timeout));
            return this;
        }
        
        /**
         * 设置请求超时时间（毫秒）
         */
        public Builder requestTimeoutMs(int timeoutMs) {
            this.requestTimeoutMs = timeoutMs;
            return this;
        }
        
        /**
         * 设置最大消息大小
         */
        public Builder maxMessageSize(int size) {
            this.maxMessageSize = size;
            return this;
        }
        
        /**
         * 设置编码
         */
        public Builder encoding(String encoding) {
            this.encoding = encoding;
            return this;
        }
        
        /**
         * 设置分发器线程池大小
         */
        public Builder dispatcherThreadPoolSize(int size) {
            this.dispatcherThreadPoolSize = size;
            return this;
        }
        
        /**
         * 设置读取线程优先级
         */
        public Builder readerThreadPriority(int priority) {
            this.readerThreadPriority = priority;
            return this;
        }
        
        /**
         * 设置断路器失败阈值
         */
        public Builder circuitBreakerFailureThreshold(int threshold) {
            this.circuitBreakerFailureThreshold = threshold;
            return this;
        }
        
        /**
         * 设置断路器重置超时时间
         */
        public Builder circuitBreakerResetTimeout(long timeout, TimeUnit unit) {
            this.circuitBreakerResetTimeoutMs = unit.toMillis(timeout);
            return this;
        }
        
        /**
         * 设置断路器半开状态最大调用数
         */
        public Builder circuitBreakerHalfOpenMaxCalls(int calls) {
            this.circuitBreakerHalfOpenMaxCalls = calls;
            return this;
        }
        
        /**
         * 设置重试策略最大重试次数
         */
        public Builder retryPolicyMaxRetries(int retries) {
            this.retryPolicyMaxRetries = retries;
            return this;
        }
        
        /**
         * 设置重试策略初始退避时间
         */
        public Builder retryPolicyInitialBackoff(long backoff, TimeUnit unit) {
            this.retryPolicyInitialBackoffMs = unit.toMillis(backoff);
            return this;
        }
        
        /**
         * 设置重试策略退避乘数
         */
        public Builder retryPolicyBackoffMultiplier(double multiplier) {
            this.retryPolicyBackoffMultiplier = multiplier;
            return this;
        }
        
        /**
         * 设置重试策略最大退避时间
         */
        public Builder retryPolicyMaxBackoff(long backoff, TimeUnit unit) {
            this.retryPolicyMaxBackoffMs = unit.toMillis(backoff);
            return this;
        }
        
        /**
         * 设置日志级别
         */
        public Builder logLevel(Level level) {
            this.logLevel = level;
            return this;
        }
        
        /**
         * 设置是否记录请求
         */
        public Builder logRequests(boolean log) {
            this.logRequests = log;
            return this;
        }
        
        /**
         * 设置是否记录响应
         */
        public Builder logResponses(boolean log) {
            this.logResponses = log;
            return this;
        }
        
        /**
         * 构建配置实例
         */
        public OmniSharpConfig build() {
            return new OmniSharpConfig(this);
        }
    }
    
    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
}