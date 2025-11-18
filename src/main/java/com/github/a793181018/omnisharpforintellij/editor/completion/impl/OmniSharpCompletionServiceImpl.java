package com.github.a793181018.omnisharpforintellij.editor.completion.impl;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import com.github.a793181018.omnisharpforintellij.editor.completion.OmniSharpCompletionService;
import com.github.a793181018.omnisharpforintellij.editor.completion.cache.CompletionCacheManager;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfig;
import com.github.a793181018.omnisharpforintellij.editor.completion.config.CompletionConfigManager;
import com.github.a793181018.omnisharpforintellij.editor.completion.performance.CompletionPerformanceService;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.github.a793181018.omnisharpforintellij.server.communication.impl.OmniSharpCommunicator;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OmniSharpCompletionService的实现类
 * 负责与OmniSharp服务器通信，获取并处理代码补全数据
 */
public class OmniSharpCompletionServiceImpl implements OmniSharpCompletionService {
    private static final Logger LOG = Logger.getInstance(OmniSharpCompletionServiceImpl.class.getName());
    private static final int DEFAULT_TIMEOUT_MS = 3000; // 默认超时时间3秒
    private final Project project;
    private final ScheduledExecutorService executorService;
    private CompletableFuture<CompletionResponse> currentFuture;
    private int requestTimeout = DEFAULT_TIMEOUT_MS;
    private final CompletionCacheManager cacheManager;
    private final CompletionConfigManager configManager;
    private final CompletionPerformanceService performanceService;
    private final OmniSharpCommunicator communicator;
    
    /**
     * 构造函数
     */
    public OmniSharpCompletionServiceImpl(@NotNull Project project) {
        this.project = project;
        this.cacheManager = CompletionCacheManager.getInstance(project);
        this.configManager = CompletionConfigManager.getInstance(project);
        this.performanceService = ServiceManager.getService(project, CompletionPerformanceService.class);
        this.communicator = new OmniSharpCommunicator();
        
        // 应用初始配置
        applyConfig(configManager.getConfig());
        
        // 注册配置变更监听器
        configManager.addConfigChangeListener(this::applyConfig);
        
        this.executorService = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = Executors.defaultThreadFactory().newThread(r);
            thread.setName("OmniSharp-Completion-Thread");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @NotNull
    @Override
    public CompletableFuture<CompletionResponse> getCompletionsAsync(@NotNull CompletionRequest request) {
        long startTime = System.currentTimeMillis();
        CompletionConfig config = configManager.getConfig();
        
        // 检查补全是否已启用
        if (!config.isCompletionEnabled()) {
            return CompletableFuture.completedFuture(CompletionResponse.createEmptyResponse());
        }
        
        // 首先尝试从预取缓存获取（如果智能补全已启用）
        if (config.isSmartCompletionEnabled()) {
            CompletionResponse prefetchedResponse = performanceService.getPrefetchedCompletions(request);
            if (prefetchedResponse != null) {
                performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
                return CompletableFuture.completedFuture(prefetchedResponse);
            }
        }
        
        // 尝试从缓存获取（如果缓存已启用）
        CompletionResponse cachedResponse = null;
        if (config.isCacheEnabled()) {
            cachedResponse = cacheManager.getCachedCompletion(request);
            if (cachedResponse != null) {
                performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
                return CompletableFuture.completedFuture(cachedResponse);
            }
        }
        
        // 取消之前的请求
        cancelRequest();
        
        // 创建新的CompletableFuture
        CompletableFuture<CompletionResponse> future = CompletableFuture.supplyAsync(() -> {
            try {
                // 构建请求参数
                Map<String, Object> parameters = buildRequestParameters(request, config);
                
                // 创建OmniSharp请求
                OmniSharpRequest<CompletionResponse> omniSharpRequest = 
                        new OmniSharpRequest<>("autocomplete", parameters, CompletionResponse.class);
                
                // 发送请求并获取响应
                CompletableFuture<OmniSharpResponse<CompletionResponse>> responseFuture = 
                        communicator.sendRequest(omniSharpRequest);
                
                try {
                       // 同步获取响应
                       OmniSharpResponse<CompletionResponse> omniSharpResponse = responseFuture.get(requestTimeout, TimeUnit.MILLISECONDS);
                       CompletionResponse completionResponse = omniSharpResponse.getBody();
                        
                       // 缓存成功的响应（如果缓存已启用）
                       if (config.isCacheEnabled() && completionResponse != null) {
                           cacheManager.cacheCompletion(request, completionResponse);
                       }
                        
                       // 记录性能指标
                       performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
                        
                       return completionResponse;
                   } catch (Exception e) {
                  LOG.error("Error processing OmniSharp response", e);
                  return CompletionResponse.createErrorResponse("Error communicating with OmniSharp server: " + e.getMessage());
              }
          } catch (Exception e) {
                return CompletionResponse.createErrorResponse("Error processing completion response: " + e.getMessage());
            }
        }, executorService);
        
        // 设置超时
        future = future.orTimeout(requestTimeout, TimeUnit.MILLISECONDS)
                .exceptionally(ex -> {
                    if (ex instanceof TimeoutException) {
                        return new CompletionResponse.Builder()
                                .withErrorMessage("Completion request timed out")
                                .isIncomplete(true)
                                .withRequestTimeMs(startTime)
                                .withResponseTimeMs(System.currentTimeMillis())
                                .build();
                    }
                    return CompletionResponse.createErrorResponse("Error: " + ex.getMessage());
                });
        
        // 保存当前future
        currentFuture = future;
        
        return future;
    }
    
    @Nullable
    @Override
    public CompletionResponse getCompletions(@NotNull CompletionRequest request) {
        long startTime = System.currentTimeMillis();
        CompletionConfig config = configManager.getConfig();
        
        // 检查补全是否已启用
        if (!config.isCompletionEnabled()) {
              return CompletionResponse.createEmptyResponse();
          }
        
        // 首先尝试从预取缓存获取（如果智能补全已启用）
        if (config.isSmartCompletionEnabled()) {
            CompletionResponse prefetchedResponse = performanceService.getPrefetchedCompletions(request);
            if (prefetchedResponse != null) {
                performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
                return prefetchedResponse;
            }
        }
        
        // 尝试从缓存获取（如果缓存已启用）
        if (config.isCacheEnabled()) {
            CompletionResponse cachedResponse = cacheManager.getCachedCompletion(request);
            if (cachedResponse != null) {
                performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
                return cachedResponse;
            }
        }
        
        try {
            CompletionResponse response = getCompletionsAsync(request).get(requestTimeout, TimeUnit.MILLISECONDS);
            // 缓存成功的响应（如果缓存已启用）
            if (response != null && config.isCacheEnabled()) {
                  cacheManager.cacheCompletion(request, response);
              }
            // 记录性能指标
            performanceService.recordPerformanceMetric("response_time", System.currentTimeMillis() - startTime);
            return response;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            return CompletionResponse.createErrorResponse("Error getting completions: " + e.getMessage());
        }
    }
    
    @NotNull
    @Override
    public List<CompletionItem> processCompletionResponse(@NotNull String response, @NotNull CompletionProvider.CompletionContext context) {
        List<CompletionItem> items = new ArrayList<>();
        
        try {
            // 这里应该使用JSON解析库解析响应
            // 由于暂时没有明确的JSON库引用，这里提供一个简化的实现
            // 实际实现中应该使用Jackson、Gson等库
            items = parseResponse(response, context);
            
            // 过滤和排序补全项
            items = filterCompletions(items, context);
            items = sortCompletions(items);
        } catch (Exception e) {
            // 记录错误但不中断流程
            System.err.println("Error processing completion response: " + e.getMessage());
        }
        
        return items;
    }
    
    @Override
    public void cancelRequest() {
        if (currentFuture != null && !currentFuture.isDone()) {
            currentFuture.cancel(true);
            currentFuture = null;
        }
    }
    
    @Override
    public boolean isRequestInProgress() {
        return currentFuture != null && !currentFuture.isDone();
    }
    
    @Override
    public void setRequestTimeout(int timeout) {
        this.requestTimeout = Math.max(500, timeout); // 最小500毫秒
    }
    
    @Override
    public int getRequestTimeout() {
        return requestTimeout;
    }
    
    /**
     * 应用配置更改
     */
    private void applyConfig(@NotNull CompletionConfig config) {
        // 应用超时设置，使用默认值
    setRequestTimeout(DEFAULT_TIMEOUT_MS);
        
        // 应用缓存设置
        if (config.isCacheEnabled()) {
            // 缓存已启用，不需要特定的配置方法
        }
    }
    
    /**
     * 构建OmniSharp服务器请求参数
     */
    @NotNull
    private Map<String, Object> buildRequestParameters(@NotNull CompletionRequest request, @NotNull CompletionConfig config) {
        Map<String, Object> params = new HashMap<>();
        
        params.put("FileName", request.getFileName());
        params.put("Line", request.getLine());
        params.put("Column", request.getColumn());
        params.put("Buffer", request.getBuffer());
        
        // 添加上下文信息
        params.put("WordToComplete", request.getContext().getPrefix());
        params.put("WantDocumentationForEveryCompletionResult", true);
        params.put("WantMethodHeader", true);
        params.put("WantReturnType", true);
        
        // 添加选项信息 - 优先使用全局配置
        CompletionProvider.CompletionOptions options = request.getOptions();
// 设置最大结果数量，使用合理的默认值
        params.put("MaxItems", 100); // 默认返回100个结果
        params.put("IncludeKeywords", true); // 默认包含关键字
        params.put("IncludeSnippets", true); // 默认包含代码片段
        params.put("IncludeImportableTypes", true); // 默认包含可导入的类型
        
        // 添加智能补全相关设置
        if (config.isSmartCompletionEnabled()) {
            params.put("WantStaleResults", true);
            params.put("WantKind", true);
        }
        
        // 添加文档设置
        params.put("WantDocumentationForEveryCompletionResult", true); // 默认包含文档
        
        // 添加额外数据
        params.putAll(request.getAdditionalData());
        
        return params;
    }
    
    /**
     * 解析OmniSharp服务器返回的响应
     */
    @NotNull
    private List<CompletionItem> parseResponse(@NotNull String response, @NotNull CompletionProvider.CompletionContext context) {
        List<CompletionItem> items = new ArrayList<>();
        
        // 这里提供一个简化的解析实现
        // 实际实现中应该使用JSON解析库
        
        // 提取CompletionItems数组
        int itemsStart = response.indexOf("\"CompletionItems\":[");
        if (itemsStart != -1) {
            itemsStart += "\"CompletionItems\":[".length();
            int itemsEnd = findMatchingBracket(response, itemsStart, '[', ']');
            
            if (itemsEnd != -1) {
                String itemsJson = response.substring(itemsStart, itemsEnd);
                // 简单地按大括号分割来模拟解析
                String[] itemParts = itemsJson.split("\\}", -1);
                
                for (String part : itemParts) {
                    if (!part.contains("{")) continue;
                    
                    part = part.substring(part.indexOf('{') + 1);
                    String finalPart = part;
                    
                    // 提取基本字段
                    String label = extractField(finalPart, "\"Label\":\"");
                    String kind = extractField(finalPart, "\"Kind\":\"");
                    String insertText = extractField(finalPart, "\"InsertText\":\"");
                    String documentation = extractField(finalPart, "\"Documentation\":\"");
                    
                    if (label != null) {
                        // 创建补全项
                        CompletionItem.Builder builder = new CompletionItem.Builder(label);
                        
                        if (insertText != null) {
                            builder.withInsertText(insertText);
                        }
                        
                        if (documentation != null) {
                            builder.withDocumentation(documentation);
                        }
                        
                        // 设置补全项类型
                        if (kind != null) {
                            builder.withKind(mapCompletionKind(kind));
                        }
                        
                        items.add(builder.build());
                    }
                }
            }
        }
        
        return items;
    }
    
    /**
     * 提取JSON字段值
     */
    @Nullable
    private String extractField(@NotNull String jsonPart, @NotNull String fieldPrefix) {
        int start = jsonPart.indexOf(fieldPrefix);
        if (start == -1) return null;
        
        start += fieldPrefix.length();
        int end = jsonPart.indexOf('"', start);
        if (end == -1) return null;
        
        return jsonPart.substring(start, end);
    }
    
    /**
     * 查找匹配的括号位置
     */
    private int findMatchingBracket(@NotNull String text, int startIndex, char openBracket, char closeBracket) {
        int count = 1;
        for (int i = startIndex; i < text.length(); i++) {
            if (text.charAt(i) == openBracket) {
                count++;
            } else if (text.charAt(i) == closeBracket) {
                count--;
                if (count == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
    
    /**
     * 将OmniSharp的补全类型映射到本地类型
     */
    @NotNull
    private CompletionItem.CompletionKind mapCompletionKind(@NotNull String kind) {
        switch (kind.toLowerCase()) {
            case "method":
            case "function":
                return CompletionItem.CompletionKind.METHOD;
            case "field":
                return CompletionItem.CompletionKind.FIELD;
            case "property":
                return CompletionItem.CompletionKind.PROPERTY;
            case "class":
                return CompletionItem.CompletionKind.CLASS;
            case "interface":
                return CompletionItem.CompletionKind.INTERFACE;
            case "enum":
                return CompletionItem.CompletionKind.ENUM;
            case "struct":
                return CompletionItem.CompletionKind.STRUCT;
            case "keyword":
                return CompletionItem.CompletionKind.KEYWORD;
            case "snippet":
                return CompletionItem.CompletionKind.SNIPPET;
            case "variable":
                return CompletionItem.CompletionKind.VARIABLE;
            case "event":
                return CompletionItem.CompletionKind.EVENT;
            default:
                return CompletionItem.CompletionKind.TEXT;
        }
    }
    
    /**
     * 过滤补全项
     */
    @NotNull
    private List<CompletionItem> filterCompletions(@NotNull List<CompletionItem> items, @NotNull CompletionProvider.CompletionContext context) {
        String prefix = context.getPrefix();
        if (prefix.isEmpty()) {
            return items; // 如果没有前缀，不过滤
        }
        
        return ContainerUtil.filter(items, item -> {
            String label = item.getLabel();
            String filterText = item.getFilterText() != null ? item.getFilterText() : label;
            
            // 前缀匹配
            return filterText.toLowerCase().startsWith(prefix.toLowerCase()) ||
                   // 或者包含前缀（模糊匹配）
                   filterText.toLowerCase().contains(prefix.toLowerCase());
        });
    }
    
    /**
     * 对补全项进行排序
     */
    @NotNull
    private List<CompletionItem> sortCompletions(@NotNull List<CompletionItem> items) {
        List<CompletionItem> sortedItems = new ArrayList<>(items);
        
        // 按优先级和标签排序
        sortedItems.sort((a, b) -> {
            // 先按优先级排序
            if (a.getPriority() != b.getPriority()) {
                return Integer.compare(b.getPriority(), a.getPriority());
            }
            
            // 再按标签排序
            return a.getLabel().compareToIgnoreCase(b.getLabel());
        });
        
        return sortedItems;
    }
}