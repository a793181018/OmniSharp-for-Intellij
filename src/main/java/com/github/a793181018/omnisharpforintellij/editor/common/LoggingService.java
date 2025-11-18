package com.github.a793181018.omnisharpforintellij.editor.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 日志服务接口，提供统一的日志记录功能。
 * 支持不同级别的日志记录，以及格式化和上下文信息。
 */
public interface LoggingService {
    /**
     * 日志级别枚举
     */
    enum Level {
        TRACE,
        DEBUG,
        INFO,
        WARN,
        ERROR
    }
    
    /**
     * 获取或创建一个指定名称的Logger
     * @param name Logger名称
     * @return Logger实例
     */
    @NotNull
    Logger getLogger(@NotNull String name);
    
    /**
     * 获取或创建一个指定类的Logger
     * @param clazz 类对象
     * @return Logger实例
     */
    @NotNull
    Logger getLogger(@NotNull Class<?> clazz);
    
    /**
     * 设置全局日志级别
     * @param level 日志级别
     */
    void setGlobalLevel(@NotNull Level level);
    
    /**
     * 获取全局日志级别
     * @return 当前全局日志级别
     */
    @NotNull
    Level getGlobalLevel();
    
    /**
     * 设置指定Logger的日志级别
     * @param loggerName Logger名称
     * @param level 日志级别
     */
    void setLoggerLevel(@NotNull String loggerName, @NotNull Level level);
    
    /**
     * Logger接口，提供日志记录方法
     */
    interface Logger {
        /**
         * 获取Logger名称
         * @return Logger名称
         */
        @NotNull
        String getName();
        
        /**
         * 设置Logger的日志级别
         * @param level 日志级别
         */
        void setLevel(@NotNull Level level);
        
        /**
         * 获取Logger的日志级别
         * @return 当前日志级别
         */
        @NotNull
        Level getLevel();
        
        /**
         * 是否启用了TRACE级别
         * @return 如果启用则返回true
         */
        boolean isTraceEnabled();
        
        /**
         * 是否启用了DEBUG级别
         * @return 如果启用则返回true
         */
        boolean isDebugEnabled();
        
        /**
         * 是否启用了INFO级别
         * @return 如果启用则返回true
         */
        boolean isInfoEnabled();
        
        /**
         * 是否启用了WARN级别
         * @return 如果启用则返回true
         */
        boolean isWarnEnabled();
        
        /**
         * 是否启用了ERROR级别
         * @return 如果启用则返回true
         */
        boolean isErrorEnabled();
        
        /**
         * 记录TRACE级别的消息
         * @param message 消息内容
         */
        void trace(@NotNull String message);
        
        /**
         * 记录TRACE级别的消息和异常
         * @param message 消息内容
         * @param throwable 异常
         */
        void trace(@NotNull String message, @NotNull Throwable throwable);
        
        /**
         * 记录TRACE级别的格式化消息
         * @param format 格式字符串
         * @param args 参数
         */
        void trace(@NotNull String format, @Nullable Object... args);
        
        /**
         * 记录DEBUG级别的消息
         * @param message 消息内容
         */
        void debug(@NotNull String message);
        
        /**
         * 记录DEBUG级别的消息和异常
         * @param message 消息内容
         * @param throwable 异常
         */
        void debug(@NotNull String message, @NotNull Throwable throwable);
        
        /**
         * 记录DEBUG级别的格式化消息
         * @param format 格式字符串
         * @param args 参数
         */
        void debug(@NotNull String format, @Nullable Object... args);
        
        /**
         * 记录INFO级别的消息
         * @param message 消息内容
         */
        void info(@NotNull String message);
        
        /**
         * 记录INFO级别的消息和异常
         * @param message 消息内容
         * @param throwable 异常
         */
        void info(@NotNull String message, @NotNull Throwable throwable);
        
        /**
         * 记录INFO级别的格式化消息
         * @param format 格式字符串
         * @param args 参数
         */
        void info(@NotNull String format, @Nullable Object... args);
        
        /**
         * 记录WARN级别的消息
         * @param message 消息内容
         */
        void warn(@NotNull String message);
        
        /**
         * 记录WARN级别的消息和异常
         * @param message 消息内容
         * @param throwable 异常
         */
        void warn(@NotNull String message, @NotNull Throwable throwable);
        
        /**
         * 记录WARN级别的格式化消息
         * @param format 格式字符串
         * @param args 参数
         */
        void warn(@NotNull String format, @Nullable Object... args);
        
        /**
         * 记录ERROR级别的消息
         * @param message 消息内容
         */
        void error(@NotNull String message);
        
        /**
         * 记录ERROR级别的消息和异常
         * @param message 消息内容
         * @param throwable 异常
         */
        void error(@NotNull String message, @NotNull Throwable throwable);
        
        /**
         * 记录ERROR级别的格式化消息
         * @param format 格式字符串
         * @param args 参数
         */
        void error(@NotNull String format, @Nullable Object... args);
    }
}