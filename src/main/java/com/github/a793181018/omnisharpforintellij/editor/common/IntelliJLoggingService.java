package com.github.a793181018.omnisharpforintellij.editor.common;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LoggingService的简单实现类，直接输出到标准输出流。
 */
public class IntelliJLoggingService implements LoggingService {
    
    private static final IntelliJLoggingService INSTANCE = new IntelliJLoggingService();
    
    private final Map<String, LoggingService.Logger> loggerCache = new ConcurrentHashMap<>();
    private volatile LoggingService.Level globalLevel = LoggingService.Level.INFO;
    private final Map<String, LoggingService.Level> loggerLevels = new ConcurrentHashMap<>();
    
    // 私有构造函数，单例模式
    private IntelliJLoggingService() {
    }
    
    /**
     * 获取服务实例
     * @return LoggingService实例
     */
    public static LoggingService getInstance() {
        return INSTANCE;
    }
    
    @Override
    @NotNull
    public LoggingService.Logger getLogger(@NotNull String name) {
        return loggerCache.computeIfAbsent(name, IntelliJLogger::new);
    }
    
    @Override
    @NotNull
    public LoggingService.Logger getLogger(@NotNull Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    @Override
    public void setGlobalLevel(@NotNull LoggingService.Level level) {
        this.globalLevel = level;
    }
    
    @Override
    @NotNull
    public LoggingService.Level getGlobalLevel() {
        return globalLevel;
    }
    
    @Override
    public void setLoggerLevel(@NotNull String loggerName, @NotNull LoggingService.Level level) {
        loggerLevels.put(loggerName, level);
    }
    
    /**
     * 获取指定Logger的有效日志级别（考虑全局级别和特定Logger级别）
     * @param loggerName Logger名称
     * @return 有效日志级别
     */
    @NotNull
    private LoggingService.Level getEffectiveLevel(@NotNull String loggerName) {
        LoggingService.Level level = loggerLevels.get(loggerName);
        return level != null ? level : globalLevel;
    }
    
    /**
     * 简单的Logger实现类
     */
    private class IntelliJLogger implements LoggingService.Logger {
        private final String name;
        
        IntelliJLogger(@NotNull String name) {
            this.name = name;
        }
        
        @Override
        @NotNull
        public String getName() {
            return name;
        }
        
        @Override
        public void setLevel(@NotNull LoggingService.Level level) {
            loggerLevels.put(name, level);
        }
        
        @Override
        @NotNull
        public LoggingService.Level getLevel() {
            LoggingService.Level level = loggerLevels.get(name);
            return level != null ? level : globalLevel;
        }
        
        @Override
        public boolean isTraceEnabled() {
            return getEffectiveLevel(name) == LoggingService.Level.TRACE;
        }
        
        @Override
        public boolean isDebugEnabled() {
            LoggingService.Level level = getEffectiveLevel(name);
            return level == LoggingService.Level.TRACE || level == LoggingService.Level.DEBUG;
        }
        
        @Override
        public boolean isInfoEnabled() {
            LoggingService.Level level = getEffectiveLevel(name);
            return level == LoggingService.Level.TRACE || 
                   level == LoggingService.Level.DEBUG || 
                   level == LoggingService.Level.INFO;
        }
        
        @Override
        public boolean isWarnEnabled() {
            LoggingService.Level level = getEffectiveLevel(name);
            return level != LoggingService.Level.ERROR;
        }
        
        @Override
        public boolean isErrorEnabled() {
            return true; // ERROR级别始终启用
        }
        
        @Override
        public void trace(@NotNull String message) {
            if (isTraceEnabled()) {
                System.out.println("TRACE: " + message);
            }
        }
        
        @Override
        public void trace(@NotNull String message, @NotNull Throwable throwable) {
            if (isTraceEnabled()) {
                System.out.println("TRACE: " + message);
                throwable.printStackTrace();
            }
        }
        
        @Override
        public void trace(@NotNull String format, @Nullable Object... args) {
            if (isTraceEnabled()) {
                System.out.println("TRACE: " + format(format, args));
            }
        }
        
        @Override
        public void debug(@NotNull String message) {
            if (isDebugEnabled()) {
                System.out.println("DEBUG: " + message);
            }
        }
        
        @Override
        public void debug(@NotNull String message, @NotNull Throwable throwable) {
            if (isDebugEnabled()) {
                System.out.println("DEBUG: " + message);
                throwable.printStackTrace();
            }
        }
        
        @Override
        public void debug(@NotNull String format, @Nullable Object... args) {
            if (isDebugEnabled()) {
                System.out.println("DEBUG: " + format(format, args));
            }
        }
        
        @Override
        public void info(@NotNull String message) {
            if (isInfoEnabled()) {
                System.out.println("INFO: " + message);
            }
        }
        
        @Override
        public void info(@NotNull String message, @NotNull Throwable throwable) {
            if (isInfoEnabled()) {
                System.out.println("INFO: " + message);
                throwable.printStackTrace();
            }
        }
        
        @Override
        public void info(@NotNull String format, @Nullable Object... args) {
            if (isInfoEnabled()) {
                System.out.println("INFO: " + format(format, args));
            }
        }
        
        @Override
        public void warn(@NotNull String message) {
            if (isWarnEnabled()) {
                System.out.println("WARN: " + message);
            }
        }
        
        @Override
        public void warn(@NotNull String message, @NotNull Throwable throwable) {
            if (isWarnEnabled()) {
                System.out.println("WARN: " + message);
                throwable.printStackTrace();
            }
        }
        
        @Override
        public void warn(@NotNull String format, @Nullable Object... args) {
            if (isWarnEnabled()) {
                System.out.println("WARN: " + format(format, args));
            }
        }
        
        @Override
        public void error(@NotNull String message) {
            System.err.println("ERROR: " + message);
        }
        
        @Override
        public void error(@NotNull String message, @NotNull Throwable throwable) {
            System.err.println("ERROR: " + message);
            throwable.printStackTrace(System.err);
        }
        
        @Override
        public void error(@NotNull String format, @Nullable Object... args) {
            System.err.println("ERROR: " + format(format, args));
        }
        
        /**
         * 格式化消息字符串
         */
        private String format(@NotNull String format, @Nullable Object... args) {
            if (args == null || args.length == 0) {
                return format;
            }
            return String.format(format, args);
        }
    }
}