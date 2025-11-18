package com.github.a793181018.omnisharpforintellij.util;

import com.github.a793181018.omnisharpforintellij.configuration.OmniSharpConfigurationFactory;
import com.github.a793181018.omnisharpforintellij.configuration.OmniSharpSettings;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp日志工具类，提供统一的日志记录功能
 */
public final class OmniSharpLogger {
    
    private OmniSharpLogger() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 获取指定类的Logger实例
     */
    @NotNull
    public static Logger getLogger(@NotNull Class<?> clazz) {
        return Logger.getInstance(clazz);
    }
    
    /**
     * 获取指定名称的Logger实例
     */
    @NotNull
    public static Logger getLogger(@NotNull String loggerName) {
        return Logger.getInstance(loggerName);
    }
    
    /**
     * 记录调试日志
     */
    public static void debug(@NotNull Logger logger, @NotNull String message) {
        if (logger.isDebugEnabled() || isDebugMode(null)) {
            logger.debug(message);
        }
    }
    
    /**
     * 记录调试日志（带异常）
     */
    public static void debug(@NotNull Logger logger, @NotNull String message, @NotNull Throwable throwable) {
        if (logger.isDebugEnabled() || isDebugMode(null)) {
            logger.debug(message, throwable);
        }
    }
    
    /**
     * 记录信息日志
     */
    public static void info(@NotNull Logger logger, @NotNull String message) {
        logger.info(message);
    }
    
    /**
     * 记录信息日志（带异常）
     */
    public static void info(@NotNull Logger logger, @NotNull String message, @NotNull Throwable throwable) {
        logger.info(message, throwable);
    }
    
    /**
     * 记录警告日志
     */
    public static void warn(@NotNull Logger logger, @NotNull String message) {
        logger.warn(message);
    }
    
    /**
     * 记录警告日志（带异常）
     */
    public static void warn(@NotNull Logger logger, @NotNull String message, @NotNull Throwable throwable) {
        logger.warn(message, throwable);
    }
    
    /**
     * 记录错误日志
     */
    public static void error(@NotNull Logger logger, @NotNull String message) {
        logger.error(message);
    }
    
    /**
     * 记录错误日志（带异常）
     */
    public static void error(@NotNull Logger logger, @NotNull String message, @NotNull Throwable throwable) {
        logger.error(message, throwable);
    }
    
    /**
     * 检查是否启用调试模式
     */
    public static boolean isDebugMode(@Nullable Project project) {
        return OmniSharpConfigurationFactory.getConfiguration(project).isDebugMode();
    }
    
    /**
     * 记录服务器通信日志
     */
    public static void logServerCommunication(@Nullable Project project, @NotNull String message) {
        Logger logger = Logger.getInstance("OmniSharpServerCommunication");
        if (isDebugMode(project)) {
            logger.info("[SERVER] " + message);
        } else {
            logger.debug("[SERVER] " + message);
        }
    }
    
    /**
     * 记录会话日志
     */
    public static void logSession(@Nullable Project project, @NotNull String sessionId, @NotNull String message) {
        Logger logger = Logger.getInstance("OmniSharpSession");
        String logMessage = "[SESSION] [" + sessionId + "] " + message;
        
        if (isDebugMode(project)) {
            logger.info(logMessage);
        } else {
            logger.debug(logMessage);
        }
    }
    
    /**
     * 记录性能日志
     */
    public static void logPerformance(@NotNull Logger logger, @NotNull String operation, long startTimeMillis) {
        long duration = System.currentTimeMillis() - startTimeMillis;
        logger.info("Operation '" + operation + "' completed in " + duration + " ms");
    }
    
    /**
     * 安全地记录可能包含敏感信息的调试日志
     */
    public static void safeDebug(@NotNull Logger logger, @NotNull String message, @NotNull String sensitiveData) {
        if (logger.isDebugEnabled()) {
            // 在调试模式下，替换敏感信息的一部分
            String maskedData = maskSensitiveData(sensitiveData);
            logger.debug(message + ": " + maskedData);
        }
    }
    
    /**
     * 屏蔽敏感数据
     */
    @NotNull
    private static String maskSensitiveData(@NotNull String data) {
        if (data.length() <= 4) {
            return "***";
        }
        int visibleChars = Math.min(4, data.length() / 2);
        return data.substring(0, visibleChars) + "***" + data.substring(data.length() - visibleChars);
    }
}