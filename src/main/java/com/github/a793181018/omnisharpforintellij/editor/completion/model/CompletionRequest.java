package com.github.a793181018.omnisharpforintellij.editor.completion.model;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 代码补全请求类，封装向OmniSharp服务器发送的补全请求参数
 */
public class CompletionRequest {
    private final String fileName;
    private final int line;
    private final int column;
    private final Project project;
    private final String buffer;
    private final CompletionProvider.CompletionContext context;
    private final CompletionProvider.CompletionOptions options;
    private final Map<String, Object> additionalData;
    
    private CompletionRequest(@NotNull Builder builder) {
        this.fileName = builder.fileName;
        this.line = builder.line;
        this.column = builder.column;
        this.project = builder.project;
        this.buffer = builder.buffer;
        this.context = builder.context;
        this.options = builder.options;
        this.additionalData = new HashMap<>(builder.additionalData);
    }
    
    @NotNull
    public String getFileName() {
        return fileName;
    }
    
    public int getLine() {
        return line;
    }
    
    public int getColumn() {
        return column;
    }
    
    @NotNull
    public Project getProject() {
        return project;
    }
    
    @NotNull
    public String getBuffer() {
        return buffer;
    }
    
    @NotNull
    public CompletionProvider.CompletionContext getContext() {
        return context;
    }
    
    @NotNull
    public CompletionProvider.CompletionOptions getOptions() {
        return options;
    }
    
    @NotNull
    public Map<String, Object> getAdditionalData() {
        return new HashMap<>(additionalData);
    }
    
    @Nullable
    public Object getAdditionalData(@NotNull String key) {
        return additionalData.get(key);
    }
    
    /**
     * 构建器类，用于创建CompletionRequest实例
     */
    public static class Builder {
        private final String fileName;
        private final int line;
        private final int column;
        private final Project project;
        private final String buffer;
        private final CompletionProvider.CompletionContext context;
        private CompletionProvider.CompletionOptions options;
        private final Map<String, Object> additionalData = new HashMap<>();
        
        /**
         * 创建构建器实例
         * @param fileName 文件路径
         * @param line 行号（从1开始）
         * @param column 列号（从1开始）
         * @param project 项目实例
         * @param buffer 文件内容
         * @param context 补全上下文
         */
        public Builder(
                @NotNull String fileName,
                int line,
                int column,
                @NotNull Project project,
                @NotNull String buffer,
                @NotNull CompletionProvider.CompletionContext context) {
            this.fileName = fileName;
            this.line = line;
            this.column = column;
            this.project = project;
            this.buffer = buffer;
            this.context = context;
        }
        
        @NotNull
        public Builder withOptions(@NotNull CompletionProvider.CompletionOptions options) {
            this.options = options;
            return this;
        }
        
        @NotNull
        public Builder withAdditionalData(@NotNull String key, @NotNull Object value) {
            this.additionalData.put(key, value);
            return this;
        }
        
        @NotNull
        public CompletionRequest build() {
            // 如果没有设置选项，使用默认选项
            if (this.options == null) {
                this.options = new CompletionOptionsImpl.Builder().build();
            }
            return new CompletionRequest(this);
        }
    }
    
    @Override
    public String toString() {
        return "CompletionRequest{" +
                "fileName='" + fileName + '\'' +
                ", line=" + line +
                ", column=" + column +
                ", context=" + context +
                ", options=" + options +
                '}';
    }
    
    // 内部类，用于提供默认的CompletionOptions实现
      private static class CompletionOptionsImpl implements CompletionProvider.CompletionOptions {
        private boolean sortResults = true;
          private boolean methodParameterHintsEnabled = true;
          private boolean smartCompletionEnabled = true;
          private int maxResults = 100;
        
        @Override
          public boolean isMethodParameterHintsEnabled() {
              return methodParameterHintsEnabled;
          }

          @Override
          public void setMethodParameterHintsEnabled(boolean enabled) {
              this.methodParameterHintsEnabled = enabled;
          }

          @Override
          public boolean isSmartCompletionEnabled() {
              return smartCompletionEnabled;
          }

          @Override
          public void setSmartCompletionEnabled(boolean enabled) {
              this.smartCompletionEnabled = enabled;
          }

          @Override
          public int getMaxResults() {
              return maxResults;
          }

          @Override
          public void setMaxResults(int maxResults) {
              this.maxResults = maxResults;
          }

        @Override
        public boolean isSortResults() {
            return sortResults;
        }
        
        @Override
        public void setSortResults(boolean sortResults) {
            this.sortResults = sortResults;
        }
        
        public static class Builder {
            @NotNull
            public CompletionOptionsImpl build() {
                return new CompletionOptionsImpl();
            }
        }
    }
}