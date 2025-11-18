package com.github.a793181018.omnisharpforintellij.editor.completion.performance;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 代码补全性能优化服务接口
 * 负责实现智能预取、增量更新等性能优化功能
 */
public interface CompletionPerformanceService {
    
    /**
     * 智能预测并预取补全项
     * @param editor 编辑器实例
     * @param psiFile PSI文件
     * @param caretOffset 光标偏移量
     */
    void prefetchCompletions(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset);
    
    /**
     * 获取预取的补全结果
     * @param request 补全请求
     * @return 预取的补全响应，如果没有则返回null
     */
    @Nullable
    CompletionResponse getPrefetchedCompletions(@NotNull CompletionRequest request);
    
    /**
     * 清除预取缓存
     */
    void clearPrefetchCache();
    
    /**
     * 并行处理多个补全请求
     * @param requests 补全请求列表
     * @return 补全响应的CompletableFuture数组
     */
    CompletableFuture<CompletionResponse>[] processRequestsInParallel(@NotNull CompletionRequest... requests);
    
    /**
     * 计算请求的优先级
     * @param request 补全请求
     * @return 优先级值，越大优先级越高
     */
    int calculateRequestPriority(@NotNull CompletionRequest request);
    
    /**
     * 检查是否应该执行补全请求
     * 基于编辑器上下文、用户行为等因素进行智能判断
     * @param editor 编辑器实例
     * @param psiFile PSI文件
     * @param caretOffset 光标偏移量
     * @return 是否应该执行补全
     */
    boolean shouldPerformCompletion(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset);
    
    /**
     * 记录性能指标
     * @param metricName 指标名称
     * @param value 指标值
     */
    void recordPerformanceMetric(@NotNull String metricName, long value);
    
    /**
     * 获取性能统计信息
     * @return 性能统计信息
     */
    @NotNull
    PerformanceStatistics getPerformanceStatistics();
    
    /**
     * 性能统计信息接口
     */
    interface PerformanceStatistics {
        long getTotalCompletionRequests();
        long getCachedCompletionRequests();
        long getPrefetchedCompletionRequests();
        long getAverageResponseTimeMs();
        long getMaximumResponseTimeMs();
        long getMinimumResponseTimeMs();
    }
}