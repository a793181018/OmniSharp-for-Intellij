package com.omnisharp.intellij.symbol.indexing;

/**
 * 日志记录器接口
 * 提供日志记录功能
 */
public interface OmniSharpLogger {
    /**
     * 记录调试信息
     * @param message 日志消息
     */
    void debug(String message);
    
    /**
     * 记录信息
     * @param message 日志消息
     */
    void info(String message);
    
    /**
     * 记录警告信息
     * @param message 日志消息
     */
    void warn(String message);
    
    /**
     * 记录错误信息
     * @param message 日志消息
     */
    void error(String message);
    
    /**
     * 记录带有异常的错误信息
     * @param message 日志消息
     * @param throwable 异常
     */
    void error(String message, Throwable throwable);
    
    /**
     * 检查是否启用了调试级别
     * @return 如果启用了调试级别则返回true
     */
    boolean isDebugEnabled();
    
    /**
     * 检查是否启用了信息级别
     * @return 如果启用了信息级别则返回true
     */
    boolean isInfoEnabled();
}