package com.github.a793181018.omnisharpforintellij.editor.completion.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 代码补全项模型，表示单个补全建议。
 */
public class CompletionItem {
    private final String label;
    private final String insertText;
    private final String sortText;
    private final String filterText;
    private final CompletionKind kind;
    private final String detail;
    private final String documentation;
    private final boolean insertTextIsSnippet;
    private final List<TextEdit> additionalTextEdits;
    private final int priority;
    private final String source;
    
    private CompletionItem(Builder builder) {
        this.label = builder.label;
        this.insertText = builder.insertText;
        this.sortText = builder.sortText;
        this.filterText = builder.filterText;
        this.kind = builder.kind;
        this.detail = builder.detail;
        this.documentation = builder.documentation;
        this.insertTextIsSnippet = builder.insertTextIsSnippet;
        this.additionalTextEdits = builder.additionalTextEdits;
        this.priority = builder.priority;
        this.source = builder.source;
    }
    
    /**
     * 获取补全项的标签（显示文本）
     */
    @NotNull
    public String getLabel() {
        return label;
    }
    
    /**
     * 获取插入到文档中的文本
     */
    @NotNull
    public String getInsertText() {
        return insertText;
    }
    
    /**
     * 获取用于排序的文本
     */
    @Nullable
    public String getSortText() {
        return sortText;
    }
    
    /**
     * 获取用于过滤的文本
     */
    @Nullable
    public String getFilterText() {
        return filterText;
    }
    
    /**
     * 获取补全项的类型
     */
    @NotNull
    public CompletionKind getKind() {
        return kind;
    }
    
    /**
     * 获取补全项的详细信息
     */
    @Nullable
    public String getDetail() {
        return detail;
    }
    
    /**
     * 获取补全项的文档注释
     */
    @Nullable
    public String getDocumentation() {
        return documentation;
    }
    
    /**
     * 判断插入文本是否为代码片段
     */
    public boolean isInsertTextIsSnippet() {
        return insertTextIsSnippet;
    }
    
    /**
     * 获取附加的文本编辑操作
     */
    @Nullable
    public List<TextEdit> getAdditionalTextEdits() {
        return additionalTextEdits;
    }
    
    /**
     * 获取补全项的优先级
     */
    public int getPriority() {
        return priority;
    }
    
    /**
     * 获取补全项的来源
     */
    @Nullable
    public String getSource() {
        return source;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CompletionItem that = (CompletionItem) o;
        return insertTextIsSnippet == that.insertTextIsSnippet && 
               priority == that.priority && 
               label.equals(that.label) && 
               insertText.equals(that.insertText) && 
               Objects.equals(sortText, that.sortText) && 
               Objects.equals(filterText, that.filterText) && 
               kind == that.kind && 
               Objects.equals(detail, that.detail) && 
               Objects.equals(documentation, that.documentation) && 
               Objects.equals(additionalTextEdits, that.additionalTextEdits) && 
               Objects.equals(source, that.source);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(label, insertText, sortText, filterText, kind, detail, 
                          documentation, insertTextIsSnippet, additionalTextEdits, 
                          priority, source);
    }
    
    @Override
    public String toString() {
        return "CompletionItem{" +
               "label='" + label + '\'' +
               ", kind=" + kind +
               ", priority=" + priority +
               ", source='" + source + '\'' +
               '}';
    }
    
    /**
     * 补全项类型枚举
     */
    public enum CompletionKind {
        TEXT,
        METHOD,
        FUNCTION,
        CONSTRUCTOR,
        FIELD,
        VARIABLE,
        CLASS,
        INTERFACE,
        MODULE,
        PROPERTY,
        UNIT,
        VALUE,
        ENUM,
        KEYWORD,
        SNIPPET,
        COLOR,
        FILE,
        REFERENCE,
        CUSTOM_COLOR,
        STRUCT,
        EVENT,
        OPERATOR,
        TYPE_PARAMETER
    }
    
    /**
     * 文本编辑操作类，表示对文档的修改
     */
    public static class TextEdit {
        private final int startOffset;
        private final int endOffset;
        private final String newText;
        
        public TextEdit(int startOffset, int endOffset, @NotNull String newText) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.newText = newText;
        }
        
        public int getStartOffset() {
            return startOffset;
        }
        
        public int getEndOffset() {
            return endOffset;
        }
        
        @NotNull
        public String getNewText() {
            return newText;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextEdit textEdit = (TextEdit) o;
            return startOffset == textEdit.startOffset && 
                   endOffset == textEdit.endOffset && 
                   newText.equals(textEdit.newText);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(startOffset, endOffset, newText);
        }
        
        @Override
        public String toString() {
            return "TextEdit{" +
                   "startOffset=" + startOffset +
                   ", endOffset=" + endOffset +
                   ", newText='" + newText + '\'' +
                   '}';
        }
    }
    
    /**
     * 构建器类，用于创建CompletionItem实例
     */
    public static class Builder {
        private final String label;
        private String insertText;
        private String sortText;
        private String filterText;
        private CompletionKind kind = CompletionKind.TEXT;
        private String detail;
        private String documentation;
        private boolean insertTextIsSnippet = false;
        private List<TextEdit> additionalTextEdits;
        private int priority = 0;
        private String source;
        
        public Builder(@NotNull String label) {
            this.label = label;
            this.insertText = label; // 默认使用标签作为插入文本
        }
        
        @NotNull
        public Builder withInsertText(@NotNull String insertText) {
            this.insertText = insertText;
            return this;
        }
        
        @NotNull
        public Builder withSortText(@NotNull String sortText) {
            this.sortText = sortText;
            return this;
        }
        
        @NotNull
        public Builder withFilterText(@NotNull String filterText) {
            this.filterText = filterText;
            return this;
        }
        
        @NotNull
        public Builder withKind(@NotNull CompletionKind kind) {
            this.kind = kind;
            return this;
        }
        
        @NotNull
        public Builder withDetail(@NotNull String detail) {
            this.detail = detail;
            return this;
        }
        
        @NotNull
        public Builder withDocumentation(@NotNull String documentation) {
            this.documentation = documentation;
            return this;
        }
        
        @NotNull
        public Builder withInsertTextIsSnippet(boolean insertTextIsSnippet) {
            this.insertTextIsSnippet = insertTextIsSnippet;
            return this;
        }
        
        @NotNull
        public Builder withAdditionalTextEdits(@NotNull List<TextEdit> additionalTextEdits) {
            this.additionalTextEdits = additionalTextEdits;
            return this;
        }
        
        @NotNull
        public Builder withPriority(int priority) {
            this.priority = priority;
            return this;
        }
        
        @NotNull
        public Builder withSource(@NotNull String source) {
            this.source = source;
            return this;
        }
        
        @NotNull
        public CompletionItem build() {
            return new CompletionItem(this);
        }
    }
}