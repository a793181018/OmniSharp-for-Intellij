package com.omnisharp.intellij.projectstructure.utils;

import org.jetbrains.annotations.NotNull;
import com.omnisharp.intellij.projectstructure.utils.ProjectLogger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控工具类，用于跟踪和分析代码执行性能
 */
public class PerformanceMonitor {
    
    private static final ProjectLogger logger = ProjectLogger.getInstance(PerformanceMonitor.class);
    private static final Map<String, Timer> activeTimers = new ConcurrentHashMap<>();
    private static final Map<String, PerformanceStats> statsMap = new ConcurrentHashMap<>();
    private static final boolean ENABLED = true; // 可配置是否启用性能监控
    
    /**
     * 计时器类，用于测量代码块的执行时间
     */
    public static class Timer implements AutoCloseable {
        private final String name;
        private final long startTime;
        private final ProjectLogger logger;
        
        private Timer(String name) {
            this.name = name;
            this.startTime = System.nanoTime();
            this.logger = ProjectLogger.getInstance(PerformanceMonitor.class);
        }
        
        /**
         * 获取已运行时间（毫秒）
         * @return 已运行时间（毫秒）
         */
        public long getElapsedMillis() {
            return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
        }
        
        /**
         * 获取已运行时间（纳秒）
         * @return 已运行时间（纳秒）
         */
        public long getElapsedNanos() {
            return System.nanoTime() - startTime;
        }
        
        /**
         * 停止计时并记录性能统计
         * @return 执行时间（毫秒）
         */
        public long stop() {
            long elapsed = getElapsedNanos();
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsed);
            
            // 记录性能统计
            statsMap.computeIfAbsent(name, k -> new PerformanceStats(name)).recordExecution(elapsed);
            
            // 移除活动计时器
            activeTimers.remove(name);
            
            // 记录长时间运行的操作
            if (elapsedMillis > 500) { // 超过500毫秒的操作被视为慢操作
                logger.warnf("Slow operation detected: %s took %d ms", name, elapsedMillis);
            } else if (logger.isDebugEnabled()) {
                logger.debugf("Operation completed: %s took %d ms", name, elapsedMillis);
            }
            
            return elapsedMillis;
        }
        
        @Override
        public void close() {
            stop();
        }
    }
    
    /**
     * 性能统计类，用于存储和分析操作的性能数据
     */
    public static class PerformanceStats {
        private final String name;
        private final AtomicLong totalExecutions = new AtomicLong(0);
        private final AtomicLong totalTimeNanos = new AtomicLong(0);
        private final AtomicLong minTimeNanos = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong maxTimeNanos = new AtomicLong(0);
        
        private PerformanceStats(String name) {
            this.name = name;
        }
        
        /**
         * 记录一次执行的性能数据
         * @param timeNanos 执行时间（纳秒）
         */
        public void recordExecution(long timeNanos) {
            totalExecutions.incrementAndGet();
            totalTimeNanos.addAndGet(timeNanos);
            
            // 更新最小时间（使用CAS操作确保线程安全）
            long currentMin;
            do {
                currentMin = minTimeNanos.get();
                if (timeNanos >= currentMin) break;
            } while (!minTimeNanos.compareAndSet(currentMin, timeNanos));
            
            // 更新最大时间（使用CAS操作确保线程安全）
            long currentMax;
            do {
                currentMax = maxTimeNanos.get();
                if (timeNanos <= currentMax) break;
            } while (!maxTimeNanos.compareAndSet(currentMax, timeNanos));
        }
        
        /**
         * 获取操作名称
         * @return 操作名称
         */
        public String getName() {
            return name;
        }
        
        /**
         * 获取总执行次数
         * @return 总执行次数
         */
        public long getTotalExecutions() {
            return totalExecutions.get();
        }
        
        /**
         * 获取总执行时间（纳秒）
         * @return 总执行时间
         */
        public long getTotalTimeNanos() {
            return totalTimeNanos.get();
        }
        
        /**
         * 获取平均执行时间（纳秒）
         * @return 平均执行时间
         */
        public long getAverageTimeNanos() {
            long executions = totalExecutions.get();
            return executions > 0 ? totalTimeNanos.get() / executions : 0;
        }
        
        /**
         * 获取最小执行时间（纳秒）
         * @return 最小执行时间
         */
        public long getMinTimeNanos() {
            return minTimeNanos.get();
        }
        
        /**
         * 获取最大执行时间（纳秒）
         * @return 最大执行时间
         */
        public long getMaxTimeNanos() {
            return maxTimeNanos.get();
        }
        
        /**
         * 获取平均执行时间（毫秒）
         * @return 平均执行时间
         */
        public double getAverageTimeMillis() {
            return TimeUnit.NANOSECONDS.toMillis(getAverageTimeNanos()) + 
                   (getAverageTimeNanos() % 1_000_000) / 1_000_000.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PerformanceStats{name='%s', executions=%d, avg=%.2fms, min=%.2fms, max=%.2fms}",
                name,
                getTotalExecutions(),
                getAverageTimeMillis(),
                TimeUnit.NANOSECONDS.toMillis(getMinTimeNanos()) + 
                    (getMinTimeNanos() % 1_000_000) / 1_000_000.0,
                TimeUnit.NANOSECONDS.toMillis(getMaxTimeNanos()) + 
                    (getMaxTimeNanos() % 1_000_000) / 1_000_000.0
            );
        }
    }
    
    /**
     * 启动一个计时器
     * @param name 操作名称
     * @return 计时器实例
     */
    public static Timer startTimer(@NotNull String name) {
        if (!ENABLED) {
            return new NullTimer();
        }
        
        Timer timer = new Timer(name);
        activeTimers.put(name, timer);
        return timer;
    }
    
    /**
     * 停止指定名称的计时器
     * @param name 操作名称
     * @return 执行时间（毫秒），如果计时器不存在则返回-1
     */
    public static long stopTimer(@NotNull String name) {
        if (!ENABLED) {
            return 0;
        }
        
        Timer timer = activeTimers.remove(name);
        if (timer != null) {
            return timer.stop();
        }
        return -1;
    }
    
    /**
     * 获取指定操作的性能统计信息
     * @param name 操作名称
     * @return 性能统计信息，如果不存在则返回null
     */
    public static PerformanceStats getStats(@NotNull String name) {
        return statsMap.get(name);
    }
    
    /**
     * 获取所有操作的性能统计信息
     * @return 所有操作的性能统计信息映射
     */
    public static Map<String, PerformanceStats> getAllStats() {
        return new HashMap<>(statsMap);
    }
    
    /**
     * 重置指定操作的性能统计信息
     * @param name 操作名称
     */
    public static void resetStats(@NotNull String name) {
        statsMap.remove(name);
    }
    
    /**
     * 重置所有性能统计信息
     */
    public static void resetAllStats() {
        statsMap.clear();
        activeTimers.clear();
    }
    
    /**
     * 输出所有性能统计信息到日志
     */
    public static void logAllStats() {
        if (!ENABLED || logger.isDebugEnabled()) {
            logger.info("Performance statistics summary:");
            for (PerformanceStats stats : statsMap.values()) {
                logger.info("  " + stats.toString());
            }
        }
    }
    
    /**
     * 空计时器实现，用于在性能监控禁用时使用
     */
    private static class NullTimer extends Timer {
        private NullTimer() {
            super("");
        }
        
        @Override
        public long getElapsedMillis() {
            return 0;
        }
        
        @Override
        public long getElapsedNanos() {
            return 0;
        }
        
        @Override
        public long stop() {
            return 0;
        }
        
        @Override
        public void close() {
            // 空实现
        }
    }
    
    /**
     * 检查性能监控是否启用
     * @return 是否启用
     */
    public static boolean isEnabled() {
        return ENABLED;
    }
}