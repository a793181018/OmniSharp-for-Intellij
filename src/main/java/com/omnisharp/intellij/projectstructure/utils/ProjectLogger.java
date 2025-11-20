package com.omnisharp.intellij.projectstructure.utils;

import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 日志辅助类，封装了IntelliJ平台的日志功能，提供统一的日志记录接口
 */
public class ProjectLogger {
    
    private final com.intellij.openapi.diagnostic.Logger logger;
    
    /**
     * 私有构造函数
     * @param logger IntelliJ日志实例
     */
    private ProjectLogger(com.intellij.openapi.diagnostic.Logger logger) {
        this.logger = logger;
    }
    
    /**
     * 获取指定类的日志实例
     * @param clazz 类对象
     * @return 日志实例
     */
    public static ProjectLogger getInstance(@NotNull Class<?> clazz) {
        return new ProjectLogger(com.intellij.openapi.diagnostic.Logger.getInstance(clazz));
    }
    
    /**
     * 获取指定名称的日志实例
     * @param name 日志名称
     * @return 日志实例
     */
    public static ProjectLogger getInstance(@NotNull String name) {
        return new ProjectLogger(com.intellij.openapi.diagnostic.Logger.getInstance(name));
    }
    
    /**
     * 记录调试级别日志
     * @param message 日志消息
     */
    public void debug(@NotNull String message) {
        logger.debug(message);
    }
    
    /**
     * 记录调试级别日志
     * @param message 日志消息
     * @param t 异常对象
     */
    public void debug(@NotNull String message, @NotNull Throwable t) {
        logger.debug(message, t);
    }
    
    /**
     * 记录信息级别日志
     * @param message 日志消息
     */
    public void info(@NotNull String message) {
        logger.info(message);
    }
    
    /**
     * 记录信息级别日志
     * @param message 日志消息
     * @param t 异常对象
     */
    public void info(@NotNull String message, @NotNull Throwable t) {
        logger.info(message, t);
    }
    
    /**
     * 记录警告级别日志
     * @param message 日志消息
     */
    public void warn(@NotNull String message) {
        logger.warn(message);
    }
    
    /**
     * 记录警告级别日志
     * @param message 日志消息
     * @param t 异常对象
     */
    public void warn(@NotNull String message, @NotNull Throwable t) {
        logger.warn(message, t);
    }
    
    /**
     * 记录错误级别日志
     * @param message 日志消息
     */
    public void error(@NotNull String message) {
        logger.error(message);
    }
    
    /**
     * 记录错误级别日志
     * @param message 日志消息
     * @param t 异常对象
     */
    public void error(@NotNull String message, @NotNull Throwable t) {
        logger.error(message, t);
    }
    
    /**
     * 记录严重错误级别日志
     * @param message 日志消息
     */
    public void fatal(@NotNull String message) {
        logger.error(message); // IntelliJ平台没有专门的fatal级别，使用error代替
    }
    
    /**
     * 记录严重错误级别日志
     * @param message 日志消息
     * @param t 异常对象
     */
    public void fatal(@NotNull String message, @NotNull Throwable t) {
        logger.error(message, t); // IntelliJ平台没有专门的fatal级别，使用error代替
    }
    
    /**
     * 检查是否启用了调试级别日志
     * @return 是否启用调试级别
     */
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }
    
    /**
     * 检查是否启用了信息级别日志
     * @return 是否启用信息级别
     */
    public boolean isInfoEnabled() {
        // IntelliJ平台可能没有直接的isInfoEnabled方法，默认为true
        return true;
    }
    
    /**
     * 检查是否启用了警告级别日志
     * @return 是否启用警告级别
     */
    public boolean isWarnEnabled() {
        // IntelliJ平台没有直接的isWarnEnabled方法，默认为true
        return true;
    }
    
    /**
     * 检查是否启用了错误级别日志
     * @return 是否启用错误级别
     */
    public boolean isErrorEnabled() {
        // IntelliJ平台没有直接的isErrorEnabled方法，默认为true
        return true;
    }
    
    /**
     * 记录格式化的调试日志
     * @param format 格式字符串
     * @param args 参数列表
     */
    public void debugf(@NotNull String format, @NotNull Object... args) {
        if (isDebugEnabled()) {
            debug(String.format(format, args));
        }
    }
    
    /**
     * 记录格式化的信息日志
     * @param format 格式字符串
     * @param args 参数列表
     */
    public void infof(@NotNull String format, @NotNull Object... args) {
        if (isInfoEnabled()) {
            info(String.format(format, args));
        }
    }
    
    /**
     * 记录格式化的警告日志
     * @param format 格式字符串
     * @param args 参数列表
     */
    public void warnf(@NotNull String format, @NotNull Object... args) {
        if (isWarnEnabled()) {
            warn(String.format(format, args));
        }
    }
    
    /**
     * 记录格式化的错误日志
     * @param format 格式字符串
     * @param args 参数列表
     */
    public void errorf(@NotNull String format, @NotNull Object... args) {
        if (isErrorEnabled()) {
            error(String.format(format, args));
        }
    }
    
    /**
     * 记录格式化的错误日志（带异常）
     * @param t 异常对象
     * @param format 格式字符串
     * @param args 参数列表
     */
    public void errorf(@NotNull Throwable t, @NotNull String format, @NotNull Object... args) {
        if (isErrorEnabled()) {
            error(String.format(format, args), t);
        }
    }
}