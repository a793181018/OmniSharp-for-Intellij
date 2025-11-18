package com.github.a793181018.omnisharpforintellij.editor.diagnostics.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 代码诊断模型，表示代码中的错误、警告或建议等问题。
 */
public class Diagnostic {
    private final String id;
    private final String message;
    private final String filePath;
    private final int startLine;
    private final int startColumn;
    private final int endLine;
    private final int endColumn;
    private final DiagnosticSeverity severity;
    private final DiagnosticCategory category;
    private final String source;
    private final String code;
    private final List<CodeFix> codeFixes;
    private final String toolTip;
    private final List<String> relatedLocations;
    private final String projectName;
    private final boolean isSuppressed;
    private final String suppressionId;
    
    private Diagnostic(Builder builder) {
        this.id = builder.id;
        this.message = builder.message;
        this.filePath = builder.filePath;
        this.startLine = builder.startLine;
        this.startColumn = builder.startColumn;
        this.endLine = builder.endLine;
        this.endColumn = builder.endColumn;
        this.severity = builder.severity;
        this.category = builder.category;
        this.source = builder.source;
        this.code = builder.code;
        this.codeFixes = Collections.unmodifiableList(new ArrayList<>(builder.codeFixes));
        this.toolTip = builder.toolTip;
        this.relatedLocations = Collections.unmodifiableList(new ArrayList<>(builder.relatedLocations));
        this.projectName = builder.projectName;
        this.isSuppressed = builder.isSuppressed;
        this.suppressionId = builder.suppressionId;
    }
    
    /**
     * 获取诊断的唯一标识符
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * 获取诊断消息
     */
    @NotNull
    public String getMessage() {
        return message;
    }
    
    /**
     * 获取诊断所在文件路径
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 获取诊断起始行号（从1开始）
     */
    public int getStartLine() {
        return startLine;
    }
    
    /**
     * 获取诊断起始列号（从1开始）
     */
    public int getStartColumn() {
        return startColumn;
    }
    
    /**
     * 获取诊断结束行号（从1开始）
     */
    public int getEndLine() {
        return endLine;
    }
    
    /**
     * 获取诊断结束列号（从1开始）
     */
    public int getEndColumn() {
        return endColumn;
    }
    
    /**
     * 获取诊断严重级别
     */
    @NotNull
    public DiagnosticSeverity getSeverity() {
        return severity;
    }
    
    /**
     * 获取诊断分类
     */
    @NotNull
    public DiagnosticCategory getCategory() {
        return category;
    }
    
    /**
     * 获取诊断源（产生诊断的工具或分析器）
     */
    @Nullable
    public String getSource() {
        return source;
    }
    
    /**
     * 获取诊断代码
     */
    @Nullable
    public String getCode() {
        return code;
    }
    
    /**
     * 获取可用的代码修复列表
     */
    @NotNull
    public List<CodeFix> getCodeFixes() {
        return codeFixes;
    }
    
    /**
     * 获取提示信息
     */
    @Nullable
    public String getToolTip() {
        return toolTip;
    }
    
    /**
     * 获取相关位置列表
     */
    @NotNull
    public List<String> getRelatedLocations() {
        return relatedLocations;
    }
    
    /**
     * 获取项目名称
     */
    @Nullable
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * 判断诊断是否被抑制
     */
    public boolean isSuppressed() {
        return isSuppressed;
    }
    
    /**
     * 获取抑制ID
     */
    @Nullable
    public String getSuppressionId() {
        return suppressionId;
    }
    
    /**
     * 获取诊断的简短描述
     */
    @NotNull
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(severity.toString().toLowerCase()).append(": ").append(message);
        if (code != null) {
            sb.append(" [").append(code).append("]");
        }
        return sb.toString();
    }
    
    /**
     * 检查是否为错误
     */
    public boolean isError() {
        return severity == DiagnosticSeverity.ERROR;
    }
    
    /**
     * 检查是否为警告
     */
    public boolean isWarning() {
        return severity == DiagnosticSeverity.WARNING;
    }
    
    /**
     * 检查是否为信息
     */
    public boolean isInformation() {
        return severity == DiagnosticSeverity.INFO;
    }
    
    /**
     * 检查是否为提示
     */
    public boolean isHint() {
        return severity == DiagnosticSeverity.HINT;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Diagnostic that = (Diagnostic) o;
        return startLine == that.startLine && 
               startColumn == that.startColumn && 
               endLine == that.endLine && 
               endColumn == that.endColumn && 
               isSuppressed == that.isSuppressed &&
               Objects.equals(id, that.id) &&
               Objects.equals(message, that.message) && 
               Objects.equals(filePath, that.filePath) && 
               severity == that.severity && 
               category == that.category && 
               Objects.equals(source, that.source) && 
               Objects.equals(code, that.code) && 
               Objects.equals(toolTip, that.toolTip) && 
               Objects.equals(projectName, that.projectName) && 
               Objects.equals(suppressionId, that.suppressionId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(id, message, filePath, startLine, startColumn, 
                          endLine, endColumn, severity, category, source, 
                          code, toolTip, projectName, isSuppressed, suppressionId);
    }
    
    @Override
    public String toString() {
        return "Diagnostic{" +
               "id='" + id + '\'' +
               ", severity=" + severity +
               ", message='" + message + '\'' +
               ", filePath='" + filePath + '\'' +
               ", line=" + startLine +
               ", column=" + startColumn +
               ", code='" + code + '\'' +
               '}';
    }
    
    /**
     * 诊断严重级别枚举
     */
    public enum DiagnosticSeverity {
        /**
         * 错误，表示代码无法编译或运行
         */
        ERROR,
        /**
         * 警告，表示可能的问题或最佳实践违规
         */
        WARNING,
        /**
         * 信息，表示提供有用的信息
         */
        INFO,
        /**
         * 提示，表示代码改进的建议
         */
        HINT
    }
    
    /**
     * 诊断分类枚举
     */
    public enum DiagnosticCategory {
        /**
         * 语法错误
         */
        SYNTAX,
        /**
         * 语义错误
         */
        SEMANTIC,
        /**
         * 编译错误
         */
        COMPILER,
        /**
         * 设计规则
         */
        DESIGN,
        /**
         * 命名规则
         */
        NAMING,
        /**
         * 性能规则
         */
        PERFORMANCE,
        /**
         * 安全性规则
         */
        SECURITY,
        /**
         * 代码质量规则
         */
        CODE_QUALITY,
        /**
         * 国际化规则
         */
        INTERNATIONALIZATION,
        /**
         * 文档规则
         */
        DOCUMENTATION,
        /**
         * 可维护性规则
         */
        MAINTAINABILITY,
        /**
         * 其他规则
         */
        OTHER
    }
    
    /**
     * 代码修复模型
     */
    public static class CodeFix {
        private final String id;
        private final String title;
        private final String description;
        private final List<TextEdit> edits;
        private final boolean isPreferred;
        private final boolean isPreviewable;
        private final boolean isSafe;
        
        private CodeFix(Builder fixBuilder) {
            this.id = fixBuilder.id;
            this.title = fixBuilder.title;
            this.description = fixBuilder.description;
            this.edits = Collections.unmodifiableList(new ArrayList<>(fixBuilder.edits));
            this.isPreferred = fixBuilder.isPreferred;
            this.isPreviewable = fixBuilder.isPreviewable;
            this.isSafe = fixBuilder.isSafe;
        }
        
        /**
         * 获取代码修复的唯一标识符
         */
        @NotNull
        public String getId() {
            return id;
        }
        
        /**
         * 获取代码修复标题
         */
        @NotNull
        public String getTitle() {
            return title;
        }
        
        /**
         * 获取代码修复描述
         */
        @Nullable
        public String getDescription() {
            return description;
        }
        
        /**
         * 获取文本编辑列表
         */
        @NotNull
        public List<TextEdit> getEdits() {
            return edits;
        }
        
        /**
         * 判断是否为首选修复
         */
        public boolean isPreferred() {
            return isPreferred;
        }
        
        /**
         * 判断是否可预览
         */
        public boolean isPreviewable() {
            return isPreviewable;
        }
        
        /**
         * 判断是否安全（不会产生副作用）
         */
        public boolean isSafe() {
            return isSafe;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeFix codeFix = (CodeFix) o;
            return isPreferred == codeFix.isPreferred &&
                   isPreviewable == codeFix.isPreviewable &&
                   isSafe == codeFix.isSafe &&
                   Objects.equals(id, codeFix.id) &&
                   Objects.equals(title, codeFix.title) &&
                   Objects.equals(description, codeFix.description) &&
                   Objects.equals(edits, codeFix.edits);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(id, title, description, edits, isPreferred, isPreviewable, isSafe);
        }
        
        @Override
        public String toString() {
            return "CodeFix{" +
                   "id='" + id + '\'' +
                   ", title='" + title + '\'' +
                   ", editsCount=" + edits.size() +
                   "}";
        }
        
        /**
         * 代码修复构建器类
         */
        public static class Builder {
            private final String id;
            private final String title;
            private String description;
            private final List<TextEdit> edits = new ArrayList<>();
            private boolean isPreferred = false;
            private boolean isPreviewable = true;
            private boolean isSafe = true;
            
            public Builder(@NotNull String id, @NotNull String title) {
                this.id = id;
                this.title = title;
            }
            
            @NotNull
            public Builder withDescription(@NotNull String description) {
                this.description = description;
                return this;
            }
            
            @NotNull
            public Builder addEdit(@NotNull TextEdit edit) {
                this.edits.add(edit);
                return this;
            }
            
            @NotNull
            public Builder addEdit(@NotNull String filePath, int startLine, int startColumn, 
                                  int endLine, int endColumn, @NotNull String newText) {
                this.edits.add(new TextEdit(filePath, startLine, startColumn, endLine, endColumn, newText));
                return this;
            }
            
            @NotNull
            public Builder withPreferred(boolean isPreferred) {
                this.isPreferred = isPreferred;
                return this;
            }
            
            @NotNull
            public Builder withPreviewable(boolean isPreviewable) {
                this.isPreviewable = isPreviewable;
                return this;
            }
            
            @NotNull
            public Builder withSafe(boolean isSafe) {
                this.isSafe = isSafe;
                return this;
            }
            
            @NotNull
            public CodeFix build() {
                return new CodeFix(this);
            }
        }
    }
    
    /**
     * 文本编辑模型
     */
    public static class TextEdit {
        private final String filePath;
        private final int startLine;
        private final int startColumn;
        private final int endLine;
        private final int endColumn;
        private final String newText;
        
        public TextEdit(@NotNull String filePath, int startLine, int startColumn, 
                       int endLine, int endColumn, @NotNull String newText) {
            this.filePath = filePath;
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
            this.newText = newText;
        }
        
        /**
         * 获取文件路径
         */
        @NotNull
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * 获取起始行号（从1开始）
         */
        public int getStartLine() {
            return startLine;
        }
        
        /**
         * 获取起始列号（从1开始）
         */
        public int getStartColumn() {
            return startColumn;
        }
        
        /**
         * 获取结束行号（从1开始）
         */
        public int getEndLine() {
            return endLine;
        }
        
        /**
         * 获取结束列号（从1开始）
         */
        public int getEndColumn() {
            return endColumn;
        }
        
        /**
         * 获取替换后的新文本
         */
        @NotNull
        public String getNewText() {
            return newText;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TextEdit textEdit = (TextEdit) o;
            return startLine == textEdit.startLine &&
                   startColumn == textEdit.startColumn &&
                   endLine == textEdit.endLine &&
                   endColumn == textEdit.endColumn &&
                   Objects.equals(filePath, textEdit.filePath) &&
                   Objects.equals(newText, textEdit.newText);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(filePath, startLine, startColumn, endLine, endColumn, newText);
        }
        
        @Override
        public String toString() {
            return "TextEdit{" +
                   "filePath='" + filePath + '\'' +
                   ", startLine=" + startLine +
                   ", startColumn=" + startColumn +
                   ", endLine=" + endLine +
                   ", endColumn=" + endColumn +
                   ", newTextLength=" + newText.length() +
                   "}";
        }
    }
    
    /**
     * 构建器类，用于创建Diagnostic实例
     */
    public static class Builder {
        private final String id;
        private final String message;
        private final String filePath;
        private int startLine = 1;
        private int startColumn = 1;
        private int endLine = 1;
        private int endColumn = 1;
        private DiagnosticSeverity severity = DiagnosticSeverity.WARNING;
        private DiagnosticCategory category = DiagnosticCategory.OTHER;
        private String source;
        private String code;
        private final List<CodeFix> codeFixes = new ArrayList<>();
        private String toolTip;
        private final List<String> relatedLocations = new ArrayList<>();
        private String projectName;
        private boolean isSuppressed = false;
        private String suppressionId;
        
        public Builder(@NotNull String id, @NotNull String message, @NotNull String filePath) {
            this.id = id;
            this.message = message;
            this.filePath = filePath;
        }
        
        @NotNull
        public Builder withLocation(int startLine, int startColumn, int endLine, int endColumn) {
            this.startLine = startLine;
            this.startColumn = startColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
            return this;
        }
        
        @NotNull
        public Builder withSingleLineLocation(int line, int startColumn, int endColumn) {
            this.startLine = line;
            this.startColumn = startColumn;
            this.endLine = line;
            this.endColumn = endColumn;
            return this;
        }
        
        @NotNull
        public Builder withSeverity(@NotNull DiagnosticSeverity severity) {
            this.severity = severity;
            return this;
        }
        
        @NotNull
        public Builder withCategory(@NotNull DiagnosticCategory category) {
            this.category = category;
            return this;
        }
        
        @NotNull
        public Builder withSource(@NotNull String source) {
            this.source = source;
            return this;
        }
        
        @NotNull
        public Builder withCode(@NotNull String code) {
            this.code = code;
            return this;
        }
        
        @NotNull
        public Builder addCodeFix(@NotNull CodeFix codeFix) {
            this.codeFixes.add(codeFix);
            return this;
        }
        
        @NotNull
        public Builder withToolTip(@NotNull String toolTip) {
            this.toolTip = toolTip;
            return this;
        }
        
        @NotNull
        public Builder addRelatedLocation(@NotNull String location) {
            this.relatedLocations.add(location);
            return this;
        }
        
        @NotNull
        public Builder withProjectName(@NotNull String projectName) {
            this.projectName = projectName;
            return this;
        }
        
        @NotNull
        public Builder withSuppressed(boolean isSuppressed) {
            this.isSuppressed = isSuppressed;
            return this;
        }
        
        @NotNull
        public Builder withSuppressionId(@NotNull String suppressionId) {
            this.suppressionId = suppressionId;
            return this;
        }
        
        @NotNull
        public Diagnostic build() {
            return new Diagnostic(this);
        }
    }
}