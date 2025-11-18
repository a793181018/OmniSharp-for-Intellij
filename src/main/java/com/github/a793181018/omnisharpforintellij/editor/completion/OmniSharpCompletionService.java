package com.github.a793181018.omnisharpforintellij.editor.completion;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp代码补全服务接口
 * 负责与OmniSharp服务器通信，获取代码补全列表
 */
public interface OmniSharpCompletionService {
    
    /**
     * 获取服务实例
     */
    static OmniSharpCompletionService getInstance(@NotNull Project project) {
        return project.getService(OmniSharpCompletionService.class);
    }
    
    /**
     * 异步获取代码补全项
     * @param request 补全请求
     * @return 包含补全响应的CompletableFuture
     */
    @NotNull
    CompletableFuture<CompletionResponse> getCompletionsAsync(@NotNull CompletionRequest request);
    
    /**
     * 同步获取代码补全项（阻塞调用）
     * @param request 补全请求
     * @return 补全响应，如果发生错误则返回null
     */
    @Nullable
    CompletionResponse getCompletions(@NotNull CompletionRequest request);
    
    /**
     * 处理OmniSharp服务器返回的补全响应
     * @param response OmniSharp服务器返回的原始响应
     * @param context 补全上下文
     * @return 处理后的补全项列表
     */
    @NotNull
    List<CompletionItem> processCompletionResponse(@NotNull String response, @NotNull CompletionProvider.CompletionContext context);
    
    /**
     * 取消当前的补全请求
     */
    void cancelRequest();
    
    /**
     * 检查是否有正在进行的补全请求
     * @return 如果有正在进行的请求则返回true
     */
    boolean isRequestInProgress();
    
    /**
     * 设置请求超时时间
     * @param timeout 超时时间（毫秒）
     */
    void setRequestTimeout(int timeout);
    
    /**
     * 获取请求超时时间
     * @return 超时时间（毫秒）
     */
    int getRequestTimeout();
}