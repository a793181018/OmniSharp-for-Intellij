package com.github.a793181018.omnisharpforintellij.editor.format.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 代码格式化结果模型，表示代码格式化操作的结果。
 */
public class FormattingResult {
    private final String formattedContent;
    private final boolean success;
    private final String errorMessage;
    private final List<TextEdit> textEdits;
    private final String originalFilePath;
    private final FormattingOptions appliedOptions;
    private final long executionTimeMs;
    private final boolean isPartialFormatting;
    private final int startOffset;
    private final int endOffset;
    
    private FormattingResult(Builder builder) {
        this.formattedContent = builder.formattedContent;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
        this.textEdits = Collections.unmodifiableList(new ArrayList<>(builder.textEdits));
        this.originalFilePath = builder.originalFilePath;
        this.appliedOptions = builder.appliedOptions;
        this.executionTimeMs = builder.executionTimeMs;
        this.isPartialFormatting = builder.isPartialFormatting;
        this.startOffset = builder.startOffset;
        this.endOffset = builder.endOffset;
    }
    
    /**
     * 获取格式化后的内容
     */
    @NotNull
    public String getFormattedContent() {
        return formattedContent;
    }
    
    /**
     * 判断格式化是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取错误消息（如果格式化失败）
     */
    @Nullable
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 获取格式化产生的文本编辑列表
     */
    @NotNull
    public List<TextEdit> getTextEdits() {
        return textEdits;
    }
    
    /**
     * 获取原始文件路径
     */
    @Nullable
    public String getOriginalFilePath() {
        return originalFilePath;
    }
    
    /**
     * 获取应用的格式化选项
     */
    @Nullable
    public FormattingOptions getAppliedOptions() {
        return appliedOptions;
    }
    
    /**
     * 获取格式化执行时间（毫秒）
     */
    public long getExecutionTimeMs() {
        return executionTimeMs;
    }
    
    /**
     * 判断是否为部分格式化
     */
    public boolean isPartialFormatting() {
        return isPartialFormatting;
    }
    
    /**
     * 获取格式化的起始偏移量
     */
    public int getStartOffset() {
        return startOffset;
    }
    
    /**
     * 获取格式化的结束偏移量
     */
    public int getEndOffset() {
        return endOffset;
    }
    
    /**
     * 获取格式化操作的描述
     */
    @NotNull
    public String getDescription() {
        if (success) {
            StringBuilder sb = new StringBuilder();
            sb.append("格式化成功");
            sb.append(isPartialFormatting ? " (部分)": " (完整)");
            sb.append(", 生成了 ").append(textEdits.size()).append(" 处编辑");
            sb.append(", 耗时: ").append(executionTimeMs).append("ms");
            return sb.toString();
        } else {
            return "格式化失败: " + (errorMessage != null ? errorMessage : "未知错误");
        }
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FormattingResult that = (FormattingResult) o;
        return success == that.success &&
               executionTimeMs == that.executionTimeMs &&
               isPartialFormatting == that.isPartialFormatting &&
               startOffset == that.startOffset &&
               endOffset == that.endOffset &&
               Objects.equals(formattedContent, that.formattedContent) &&
               Objects.equals(errorMessage, that.errorMessage) &&
               Objects.equals(textEdits, that.textEdits) &&
               Objects.equals(originalFilePath, that.originalFilePath) &&
               Objects.equals(appliedOptions, that.appliedOptions);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(formattedContent, success, errorMessage, textEdits, 
                          originalFilePath, appliedOptions, executionTimeMs, 
                          isPartialFormatting, startOffset, endOffset);
    }
    
    @Override
    public String toString() {
        return "FormattingResult{" +
               "success=" + success +
               ", editsCount=" + textEdits.size() +
               ", partial=" + isPartialFormatting +
               ", timeMs=" + executionTimeMs +
               ", errorMessage='" + errorMessage + '\'' +
               '}';
    }
    
    /**
     * 文本编辑模型
     */
    public static class TextEdit {
        private final int startOffset;
        private final int endOffset;
        private final String newText;
        private final String originalText;
        
        public TextEdit(int startOffset, int endOffset, @NotNull String newText) {
            this(startOffset, endOffset, newText, "");
        }
        
        public TextEdit(int startOffset, int endOffset, @NotNull String newText, @NotNull String originalText) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.newText = newText;
            this.originalText = originalText;
        }
        
        /**
         * 获取起始偏移量
         */
        public int getStartOffset() {
            return startOffset;
        }
        
        /**
         * 获取结束偏移量
         */
        public int getEndOffset() {
            return endOffset;
        }
        
        /**
         * 获取替换后的新文本
         */
        @NotNull
        public String getNewText() {
            return newText;
        }
        
        /**
         * 获取替换前的原始文本
         */
        @NotNull
        public String getOriginalText() {
            return originalText;
        }
        
        /**
         * 获取编辑影响的字符数量
         */
        public int getLength() {
            return endOffset - startOffset;
        }
        
        /**
         * 判断是否为空编辑（即没有实际修改）
         */
        public boolean isEmpty() {
            return startOffset == endOffset && newText.isEmpty();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextEdit textEdit = (TextEdit) o;
            return startOffset == textEdit.startOffset &&
                   endOffset == textEdit.endOffset &&
                   Objects.equals(newText, textEdit.newText) &&
                   Objects.equals(originalText, textEdit.originalText);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(startOffset, endOffset, newText, originalText);
        }
        
        @Override
        public String toString() {
            return "TextEdit{" +
                   "offset=" + startOffset +
                   ", length=" + getLength() +
                   ", newTextLength=" + newText.length() +
                   "}";
        }
    }
    
    /**
     * 格式化选项接口
     */
    public interface FormattingOptions {
        /**
         * 获取缩进大小
         */
        int getIndentSize();
        
        /**
         * 获取制表符大小
         */
        int getTabSize();
        
        /**
         * 判断是否使用制表符进行缩进
         */
        boolean useTabs();
        
        /**
         * 获取最大行长度
         */
        int getMaxLineLength();
        
        /**
         * 获取花括号样式
         */
        @NotNull
        String getBracesStyle();
        
        /**
         * 获取空白行数量
         */
        int getBlankLines();
        
        /**
         * 转换为字符串表示
         */
        @Override
        String toString();
    }
    
    /**
     * 默认格式化选项实现
     */
    public static class DefaultFormattingOptions implements FormattingOptions {
        private final int indentSize;
        private final int tabSize;
        private final boolean useTabs;
        private final int maxLineLength;
        private final String bracesStyle;
        private final int blankLines;
        
        public DefaultFormattingOptions(int indentSize, int tabSize, boolean useTabs, 
                                      int maxLineLength, @NotNull String bracesStyle, int blankLines) {
            this.indentSize = indentSize;
            this.tabSize = tabSize;
            this.useTabs = useTabs;
            this.maxLineLength = maxLineLength;
            this.bracesStyle = bracesStyle;
            this.blankLines = blankLines;
        }
        
        @Override
        public int getIndentSize() {
            return indentSize;
        }
        
        @Override
        public int getTabSize() {
            return tabSize;
        }
        
        @Override
        public boolean useTabs() {
            return useTabs;
        }
        
        @Override
        public int getMaxLineLength() {
            return maxLineLength;
        }
        
        @Override
        @NotNull
        public String getBracesStyle() {
            return bracesStyle;
        }
        
        @Override
        public int getBlankLines() {
            return blankLines;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DefaultFormattingOptions that = (DefaultFormattingOptions) o;
            return indentSize == that.indentSize &&
                   tabSize == that.tabSize &&
                   useTabs == that.useTabs &&
                   maxLineLength == that.maxLineLength &&
                   blankLines == that.blankLines &&
                   Objects.equals(bracesStyle, that.bracesStyle);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(indentSize, tabSize, useTabs, maxLineLength, bracesStyle, blankLines);
        }
        
        @Override
        @NotNull
        public String toString() {
            return "FormattingOptions{" +
                   "indentSize=" + indentSize +
                   ", tabSize=" + tabSize +
                   ", useTabs=" + useTabs +
                   ", maxLineLength=" + maxLineLength +
                   ", bracesStyle='" + bracesStyle + '\'' +
                   ", blankLines=" + blankLines +
                   '}';
        }
    }
    
    /**
     * 构建器类，用于创建FormattingResult实例
     */
    public static class Builder {
        private final String formattedContent;
        private boolean success = true;
        private String errorMessage;
        private final List<TextEdit> textEdits = new ArrayList<>();
        private String originalFilePath;
        private FormattingOptions appliedOptions;
        private long executionTimeMs = 0;
        private boolean isPartialFormatting = false;
        private int startOffset = 0;
        private int endOffset = 0;
        
        public Builder(@NotNull String formattedContent) {
            this.formattedContent = formattedContent;
        }
        
        @NotNull
        public Builder withSuccess(boolean success) {
            this.success = success;
            return this;
        }
        
        @NotNull
        public Builder withErrorMessage(@NotNull String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }
        
        @NotNull
        public Builder addTextEdit(@NotNull TextEdit textEdit) {
            this.textEdits.add(textEdit);
            return this;
        }
        
        @NotNull
        public Builder addTextEdit(int startOffset, int endOffset, @NotNull String newText) {
            this.textEdits.add(new TextEdit(startOffset, endOffset, newText));
            return this;
        }
        
        @NotNull
        public Builder addTextEdit(int startOffset, int endOffset, @NotNull String newText, @NotNull String originalText) {
            this.textEdits.add(new TextEdit(startOffset, endOffset, newText, originalText));
            return this;
        }
        
        @NotNull
        public Builder withOriginalFilePath(@NotNull String originalFilePath) {
            this.originalFilePath = originalFilePath;
            return this;
        }
        
        @NotNull
        public Builder withAppliedOptions(@NotNull FormattingOptions appliedOptions) {
            this.appliedOptions = appliedOptions;
            return this;
        }
        
        @NotNull
        public Builder withExecutionTimeMs(long executionTimeMs) {
            this.executionTimeMs = executionTimeMs;
            return this;
        }
        
        @NotNull
        public Builder withPartialFormatting(boolean isPartialFormatting) {
            this.isPartialFormatting = isPartialFormatting;
            return this;
        }
        
        @NotNull
        public Builder withOffsetRange(int startOffset, int endOffset) {
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            return this;
        }
        
        @NotNull
        public FormattingResult build() {
            return new FormattingResult(this);
        }
    }
    
    /**
     * 创建成功的格式化结果
     */
    @NotNull
    public static FormattingResult createSuccess(@NotNull String formattedContent) {
        return new Builder(formattedContent)
                .withSuccess(true)
                .build();
    }
    
    /**
     * 创建失败的格式化结果
     */
    @NotNull
    public static FormattingResult createFailure(@NotNull String errorMessage) {
        return new Builder("")
                .withSuccess(false)
                .withErrorMessage(errorMessage)
                .build();
    }
}