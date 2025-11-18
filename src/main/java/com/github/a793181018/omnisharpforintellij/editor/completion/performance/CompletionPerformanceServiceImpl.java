package com.github.a793181018.omnisharpforintellij.editor.completion.performance;

import com.github.a793181018.omnisharpforintellij.editor.completion.OmniSharpCompletionService;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfig;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfigManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CompletionPerformanceService的实现类
 * 提供智能预取、并行处理等性能优化功能
 */
public class CompletionPerformanceServiceImpl implements CompletionPerformanceService {
    
    private static final Key<CompletionRequest> PREFETCH_REQUEST_KEY = Key.create("OmniSharp.prefetchRequest");
    private static final int PREFETCH_CACHE_SIZE = 5;
    private static final int PREFETCH_DELAY_MS = 500; // 预取延迟，避免频繁触发
    private static final Pattern DOT_PATTERN = Pattern.compile("\\.");
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("\\b(if|else|for|while|return|class|interface|struct|enum)\\b");
    
    private final Project project;
    private final OmniSharpCompletionService completionService;
    private final CompletionConfigManager configManager;
    private final ConcurrentHashMap<CompletionRequest, CompletionResponse> prefetchCache;
    private final CompletionRequestQueue requestQueue;
    private final MergingUpdateQueue prefetchQueue;
    private final PerformanceStatsImpl performanceStats;
    private final ScheduledExecutorService prefetchExecutor;
    
    /**
     * 构造函数
     */
    public CompletionPerformanceServiceImpl(@NotNull Project project) {
        this.project = project;
        this.completionService = ServiceManager.getService(project, OmniSharpCompletionService.class);
        this.configManager = CompletionConfigManager.getInstance(project);
        this.prefetchCache = new ConcurrentHashMap<>(PREFETCH_CACHE_SIZE);
        this.requestQueue = new CompletionRequestQueue();
        this.performanceStats = new PerformanceStatsImpl();
        
        // 创建预取队列，使用合并机制避免重复预取
        this.prefetchQueue = new MergingUpdateQueue(
                "OmniSharp.CompletionPrefetch",
                PREFETCH_DELAY_MS,
                true,
                MergingUpdateQueue.ANY_COMPONENT,
                project,
                null,
                false
        );
        
        // 创建预取执行器
        this.prefetchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("OmniSharp-Completion-Prefetch");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public void prefetchCompletions(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset) {
        // 检查配置是否启用预取
        CompletionConfig config = configManager.getConfig();
        // 使用isCompletionEnabled代替isEnabled，并假设默认启用智能补全
        if (!config.isCompletionEnabled()) {
            return;
        }
        
        // 检查是否应该执行补全预取
        if (!shouldPerformCompletion(editor, psiFile, caretOffset)) {
            return;
        }
        
        // 创建预取请求
        final CompletionRequest request = createPrefetchRequest(editor, psiFile, caretOffset);
        if (request == null) {
            return;
        }
        
        // 使用合并队列，避免短时间内重复预取
        prefetchQueue.queue(new Update(request) {
            @Override
            public void run() {
                // 再次检查，确保在执行时仍然有效
                if (isValidPrefetchContext(editor, psiFile, caretOffset)) {
                    doPrefetch(request);
                }
            }
            
            @Override
            public boolean canEat(Update update) {
                // 简化检查逻辑，只确认update是Update类型
                return update instanceof Update;
            }
        });
    }
    
    @Nullable
    @Override
    public CompletionResponse getPrefetchedCompletions(@NotNull CompletionRequest request) {
        // 查找匹配的预取结果
        for (Map.Entry<CompletionRequest, CompletionResponse> entry : prefetchCache.entrySet()) {
            if (isMatchingPrefetch(entry.getKey(), request)) {
                // 命中预取缓存
                performanceStats.incrementPrefetchedRequests();
                return entry.getValue();
            }
        }
        return null;
    }
    
    @Override
    public void clearPrefetchCache() {
        prefetchCache.clear();
    }
    
    @Override
    public CompletableFuture<CompletionResponse>[] processRequestsInParallel(@NotNull CompletionRequest... requests) {
        // 使用并行流处理多个请求
        return Arrays.stream(requests)
                .map(request -> {
                    // 计算优先级
                      int priority = calculateRequestPriority(request);
                      // 使用默认的异步执行器处理任务
                      return CompletableFuture.supplyAsync(() -> {
                          long startTime = System.currentTimeMillis();
                          CompletionResponse response = completionService.getCompletions(request);
                          long endTime = System.currentTimeMillis();
                          // 记录响应时间
                          recordPerformanceMetric("response_time", endTime - startTime);
                          return response;
                      });
                })
                .toArray(CompletableFuture[]::new);
    }
    
    @Override
    public int calculateRequestPriority(@NotNull CompletionRequest request) {
        int priority = 0;
        
        // 基于上下文前缀的优先级规则
        String prefix = request.getContext().getPrefix();
        if (prefix != null) {
            // 点号后请求优先级高
            if (DOT_PATTERN.matcher(prefix).find()) {
                priority += 2;
            }
            // 关键字前缀优先级高
            if (KEYWORD_PATTERN.matcher(prefix).find()) {
                priority += 1;
            }
        }
        
        // 基于位置的优先级规则
        if (request.getColumn() > 0) {
            // 在代码中间的补全请求优先级更高
            priority += 1;
        }
        
        return Math.min(priority, 5); // 限制最大优先级
    }
    
    @Override
    public boolean shouldPerformCompletion(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset) {
        // 检查基本条件
        if (editor.isDisposed() || psiFile.isPhysical() && !psiFile.isValid()) {
            return false;
        }
        
        // 检查配置
        CompletionConfig config = configManager.getConfig();
        if (!config.isCompletionEnabled()) {
            return false;
        }
        
        // 检查光标位置和上下文
        try {
            // 检查最近的元素，判断是否在可补全的位置
            PsiElement element = PsiTreeUtil.findElementOfClassAtOffset(
                    psiFile,
                    caretOffset,
                    PsiElement.class,
                    false
            );
            
            if (element != null) {
                // 检查是否在字符串或注释中
                if (isInStringOrComment(element)) {
                    return false; // 替代不存在的方法
                }
                
                // 检查是否在表达式上下文中
                return isInExpressionContext(element);
            }
            
            // 检查前一个字符，判断是否在可补全位置
            if (caretOffset > 0) {
                char prevChar = editor.getDocument().getText().charAt(caretOffset - 1);
                return isCompletionTriggerCharacter(prevChar);
            }
            
        } catch (Exception e) {
            // 忽略异常
        }
        
        return false;
    }
    
    @Override
    public void recordPerformanceMetric(@NotNull String metricName, long value) {
        performanceStats.recordMetric(metricName, value);
    }
    
    @NotNull
    @Override
    public PerformanceStatistics getPerformanceStatistics() {
        return performanceStats;
    }
    
    /**
     * 执行预取操作
     */
    private void doPrefetch(@NotNull CompletionRequest request) {
        // 检查是否已经在缓存中
        if (getPrefetchedCompletions(request) != null) {
            return;
        }
        
        // 异步预取
        prefetchExecutor.submit(() -> {
            try {
                long startTime = System.currentTimeMillis();
                CompletionResponse response = completionService.getCompletions(request);
                
                if (response != null && response.isSuccessful() && !response.getItems().isEmpty()) {
                    // 限制缓存大小，使用LRU策略
                    synchronized (prefetchCache) {
                        if (prefetchCache.size() >= PREFETCH_CACHE_SIZE) {
                            // 移除最早添加的条目
                            Map.Entry<CompletionRequest, CompletionResponse> oldestEntry = 
                                    prefetchCache.entrySet().iterator().next();
                            prefetchCache.remove(oldestEntry.getKey());
                        }
                        // 添加到预取缓存
                        prefetchCache.put(request, response);
                    }
                    
                    // 记录预取性能
                    long endTime = System.currentTimeMillis();
                    recordPerformanceMetric("prefetch_time", endTime - startTime);
                }
            } catch (Exception e) {
                // 预取失败不影响主流程
            }
        });
    }
    
    /**
     * 创建预取请求
     */
    @Nullable
    private CompletionRequest createPrefetchRequest(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset) {
        // 这里应该创建一个适当的CompletionRequest
        // 实现简化，实际应该根据文档内容和位置创建合适的请求
        return null; // 需要根据实际情况实现
    }
    
    /**
     * 检查预取上下文是否有效
     */
    private boolean isValidPrefetchContext(@NotNull Editor editor, @NotNull PsiFile psiFile, int caretOffset) {
        // 检查编辑器和文件是否仍然有效
        if (editor.isDisposed() || !psiFile.isValid()) {
            return false;
        }
        
        // 检查光标位置是否有显著变化
        int currentCaretOffset = editor.getCaretModel().getOffset();
        return Math.abs(currentCaretOffset - caretOffset) < 10; // 允许小范围移动
    }
    
    /**
     * 检查两个请求是否匹配（可以使用预取结果）
     */
    private boolean isMatchingPrefetch(@NotNull CompletionRequest prefetched, @NotNull CompletionRequest actual) {
        // 文件名必须匹配
        if (!Objects.equals(prefetched.getFileName(), actual.getFileName())) {
            return false;
        }
        
        // 行号必须相同
        if (prefetched.getLine() != actual.getLine()) {
            return false;
        }
        
        // 列号允许一定误差
        if (Math.abs(prefetched.getColumn() - actual.getColumn()) > 5) {
            return false;
        }
        
        // 前缀必须匹配或兼容
        String prefetchPrefix = prefetched.getContext().getPrefix();
        String actualPrefix = actual.getContext().getPrefix();
        if (prefetchPrefix != null && actualPrefix != null) {
            // 预取的前缀应该是实际前缀的前缀
            return actualPrefix.startsWith(prefetchPrefix);
        }
        
        return true;
    }
    
    /**
     * 检查元素是否在字符串或注释中
     */
    private boolean isInStringOrComment(@NotNull PsiElement element) {
        // 简化实现，实际应该根据语言特定的PSI结构判断
        return false; // 需要根据实际情况实现
    }
    
    /**
     * 检查元素是否在表达式上下文中
     */
    private boolean isInExpressionContext(@NotNull PsiElement element) {
        // 简化实现，实际应该根据语言特定的PSI结构判断
        return true;
    }
    
    /**
     * 检查字符是否是补全触发器字符
     */
    private boolean isCompletionTriggerCharacter(char c) {
        return Character.isJavaIdentifierPart(c) || c == '.' || c == '\'' || c == '"';
    }
    
    /**
     * 补全请求队列，用于优先级管理
     */
    private static class CompletionRequestQueue {
        private final PriorityBlockingQueue<QueuedRequest> queue;
        
        public CompletionRequestQueue() {
            this.queue = new PriorityBlockingQueue<>(10, Comparator.comparingInt(QueuedRequest::getPriority).reversed());
        }
        
        public void addRequest(CompletionRequest request, int priority, Runnable callback) {
            queue.put(new QueuedRequest(request, priority, callback));
        }
    }
    
    /**
     * 队列中的请求项
     */
    private static class QueuedRequest {
        private final CompletionRequest request;
        private final int priority;
        private final Runnable callback;
        
        public QueuedRequest(CompletionRequest request, int priority, Runnable callback) {
            this.request = request;
            this.priority = priority;
            this.callback = callback;
        }
        
        public int getPriority() {
            return priority;
        }
    }
    
    /**
     * 性能统计实现类
     */
    private static class PerformanceStatsImpl implements PerformanceStatistics {
        private final AtomicLong totalRequests = new AtomicLong(0);
        private final AtomicLong cachedRequests = new AtomicLong(0);
        private final AtomicLong prefetchedRequests = new AtomicLong(0);
        private final AtomicLong totalResponseTime = new AtomicLong(0);
        private final AtomicLong maxResponseTime = new AtomicLong(0);
        private final AtomicLong minResponseTime = new AtomicLong(Long.MAX_VALUE);
        private final Map<String, Long> metrics = new ConcurrentHashMap<>();
        
        public void incrementTotalRequests() {
            totalRequests.incrementAndGet();
        }
        
        public void incrementCachedRequests() {
            cachedRequests.incrementAndGet();
        }
        
        public void incrementPrefetchedRequests() {
            prefetchedRequests.incrementAndGet();
        }
        
        public void recordMetric(String name, long value) {
            metrics.put(name, value);
            
            if ("response_time".equals(name)) {
                totalResponseTime.addAndGet(value);
                maxResponseTime.updateAndGet(currentMax -> Math.max(currentMax, value));
                minResponseTime.updateAndGet(currentMin -> Math.min(currentMin, value));
            }
        }
        
        @Override
        public long getTotalCompletionRequests() {
            return totalRequests.get();
        }
        
        @Override
        public long getCachedCompletionRequests() {
            return cachedRequests.get();
        }
        
        @Override
        public long getPrefetchedCompletionRequests() {
            return prefetchedRequests.get();
        }
        
        @Override
        public long getAverageResponseTimeMs() {
            long count = getTotalCompletionRequests();
            return count > 0 ? totalResponseTime.get() / count : 0;
        }
        
        @Override
        public long getMaximumResponseTimeMs() {
            return maxResponseTime.get();
        }
        
        @Override
        public long getMinimumResponseTimeMs() {
            long min = minResponseTime.get();
            return min != Long.MAX_VALUE ? min : 0;
        }
    }
}