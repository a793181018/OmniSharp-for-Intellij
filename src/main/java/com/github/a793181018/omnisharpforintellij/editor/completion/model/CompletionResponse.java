package com.github.a793181018.omnisharpforintellij.editor.completion.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 代码补全响应类，封装从OmniSharp服务器获取的补全结果
 */
public class CompletionResponse {
    private final List<CompletionItem> items;
    private final boolean isIncomplete;
    private final String errorMessage;
    private final long requestTimeMs;
    private final long responseTimeMs;
    
    private CompletionResponse(@NotNull Builder builder) {
        this.items = builder.items != null ? Collections.unmodifiableList(builder.items) : Collections.emptyList();
        this.isIncomplete = builder.isIncomplete;
        this.errorMessage = builder.errorMessage;
        this.requestTimeMs = builder.requestTimeMs;
        this.responseTimeMs = builder.responseTimeMs;
    }
    
    /**
     * 获取补全项列表
     */
    @NotNull
    public List<CompletionItem> getItems() {
        return items;
    }
    
    /**
     * 判断补全结果是否不完整
     * @return 如果结果不完整（例如超时或达到最大限制）则返回true
     */
    public boolean isIncomplete() {
        return isIncomplete;
    }
    
    /**
     * 获取错误信息
     * @return 如果发生错误则返回错误信息，否则返回null
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 判断响应是否成功
     * @return 如果没有错误且items不为空则返回true
     */
    public boolean isSuccessful() {
        return errorMessage == null && !items.isEmpty();
    }
    
    /**
     * 获取请求发送时间（毫秒时间戳）
     */
    public long getRequestTimeMs() {
        return requestTimeMs;
    }
    
    /**
     * 获取响应接收时间（毫秒时间戳）
     */
    public long getResponseTimeMs() {
        return responseTimeMs;
    }
    
    /**
     * 计算请求响应耗时
     * @return 耗时（毫秒）
     */
    public long getDurationMs() {
        return responseTimeMs - requestTimeMs;
    }
    
    /**
     * 构建器类，用于创建CompletionResponse实例
     */
    public static class Builder {
        private List<CompletionItem> items;
        private boolean isIncomplete = false;
        private String errorMessage;
        private long requestTimeMs = System.currentTimeMillis();
        private long responseTimeMs = System.currentTimeMillis();
        
        @NotNull
        public Builder withItems(@NotNull List<CompletionItem> items) {
            this.items = items;
            return this;
        }
        
        @NotNull
        public Builder isIncomplete(boolean isIncomplete) {
            this.isIncomplete = isIncomplete;
            return this;
        }
        
        @NotNull
        public Builder withErrorMessage(@NotNull String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }
        
        @NotNull
        public Builder withRequestTimeMs(long requestTimeMs) {
            this.requestTimeMs = requestTimeMs;
            return this;
        }
        
        @NotNull
        public Builder withResponseTimeMs(long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }
        
        @NotNull
        public CompletionResponse build() {
            return new CompletionResponse(this);
        }
    }
    
    /**
     * 创建错误响应
     * @param errorMessage 错误信息
     * @return 包含错误信息的响应
     */
    @NotNull
    public static CompletionResponse createErrorResponse(@NotNull String errorMessage) {
        return new Builder()
                .withErrorMessage(errorMessage)
                .build();
    }
    
    /**
     * 创建空响应
     * @return 空响应
     */
    @NotNull
    public static CompletionResponse createEmptyResponse() {
        return new Builder()
                .withItems(Collections.emptyList())
                .build();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletionResponse that = (CompletionResponse) o;
        return isIncomplete == that.isIncomplete && 
               requestTimeMs == that.requestTimeMs && 
               responseTimeMs == that.responseTimeMs && 
               items.equals(that.items) && 
               Objects.equals(errorMessage, that.errorMessage);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(items, isIncomplete, errorMessage, requestTimeMs, responseTimeMs);
    }
    
    @Override
    public String toString() {
        return "CompletionResponse{" +
                "items.size()=" + items.size() +
                ", isIncomplete=" + isIncomplete +
                ", errorMessage='" + errorMessage + '\'' +
                ", durationMs=" + getDurationMs() +
                '}';
    }
}