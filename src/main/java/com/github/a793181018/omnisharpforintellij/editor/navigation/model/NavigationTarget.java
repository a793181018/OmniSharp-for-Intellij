package com.github.a793181018.omnisharpforintellij.editor.navigation.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 导航目标模型，表示代码导航的目标位置。
 */
public class NavigationTarget {
    private final String filePath;
    private final int line;
    private final int column;
    private final String name;
    private final TargetType type;
    private final String containingType;
    private final String containingNamespace;
    private final String signature;
    private final String sourceCode;
    
    protected NavigationTarget(Builder builder) {
        this.filePath = builder.filePath;
        this.line = builder.line;
        this.column = builder.column;
        this.name = builder.name;
        this.type = builder.type;
        this.containingType = builder.containingType;
        this.containingNamespace = builder.containingNamespace;
        this.signature = builder.signature;
        this.sourceCode = builder.sourceCode;
    }
    
    /**
     * 获取文件路径
     */
    @NotNull
    public String getFilePath() {
        return filePath;
    }
    
    /**
     * 获取行号（从1开始）
     */
    public int getLine() {
        return line;
    }
    
    /**
     * 获取列号（从0开始）
     */
    public int getColumn() {
        return column;
    }
    
    /**
     * 获取目标名称
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * 获取目标类型
     */
    @NotNull
    public TargetType getType() {
        return type;
    }
    
    /**
     * 获取包含类型名称
     */
    @Nullable
    public String getContainingType() {
        return containingType;
    }
    
    /**
     * 获取包含命名空间
     */
    @Nullable
    public String getContainingNamespace() {
        return containingNamespace;
    }
    
    /**
     * 获取目标的签名
     */
    @Nullable
    public String getSignature() {
        return signature;
    }
    
    /**
     * 获取源代码片段
     */
    @Nullable
    public String getSourceCode() {
        return sourceCode;
    }
    
    /**
     * 获取完整的显示名称（包含命名空间和类型）
     */
    @NotNull
    public String getFullyQualifiedName() {
        StringBuilder sb = new StringBuilder();
        
        if (containingNamespace != null && !containingNamespace.isEmpty()) {
            sb.append(containingNamespace);
            sb.append(".");
        }
        
        if (containingType != null && !containingType.isEmpty()) {
            sb.append(containingType);
            sb.append(".");
        }
        
        sb.append(name);
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NavigationTarget that = (NavigationTarget) o;
        return line == that.line && 
               column == that.column && 
               filePath.equals(that.filePath) && 
               name.equals(that.name) && 
               type == that.type && 
               Objects.equals(containingType, that.containingType) && 
               Objects.equals(containingNamespace, that.containingNamespace) && 
               Objects.equals(signature, that.signature) && 
               Objects.equals(sourceCode, that.sourceCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(filePath, line, column, name, type, containingType, 
                          containingNamespace, signature, sourceCode);
    }
    
    @Override
    public String toString() {
        return "NavigationTarget{" +
               "filePath='" + filePath + '\'' +
               ", line=" + line +
               ", column=" + column +
               ", name='" + name + '\'' +
               ", type=" + type +
               ", fullyQualifiedName='" + getFullyQualifiedName() + '\'' +
               '}';
    }
    
    /**
     * 目标类型枚举
     */
    public enum TargetType {
        METHOD,
        CLASS,
        INTERFACE,
        PROPERTY,
        FIELD,
        ENUM,
        ENUM_VALUE,
        NAMESPACE,
        CONSTRUCTOR,
        DESTRUCTOR,
        EVENT,
        OPERATOR,
        INDEXER,
        TYPE_PARAMETER,
        LOCAL_VARIABLE,
        PARAMETER,
        LABEL,
        OTHER
    }
    
    /**
     * 构建器类，用于创建NavigationTarget实例
     */
    public static class Builder {
        private final String filePath;
        private final String name;
        private final TargetType type;
        private int line = 1;
        private int column = 0;
        private String containingType;
        private String containingNamespace;
        private String signature;
        private String sourceCode;
        
        public Builder(@NotNull String filePath, @NotNull String name, @NotNull TargetType type) {
            this.filePath = filePath;
            this.name = name;
            this.type = type;
        }
        
        @NotNull
        public Builder withLine(int line) {
            this.line = line;
            return this;
        }
        
        @NotNull
        public Builder withColumn(int column) {
            this.column = column;
            return this;
        }
        
        @NotNull
        public Builder withContainingType(@NotNull String containingType) {
            this.containingType = containingType;
            return this;
        }
        
        @NotNull
        public Builder withContainingNamespace(@NotNull String containingNamespace) {
            this.containingNamespace = containingNamespace;
            return this;
        }
        
        @NotNull
        public Builder withSignature(@NotNull String signature) {
            this.signature = signature;
            return this;
        }
        
        @NotNull
        public Builder withSourceCode(@NotNull String sourceCode) {
            this.sourceCode = sourceCode;
            return this;
        }
        
        @NotNull
        public NavigationTarget build() {
            return new NavigationTarget(this);
        }
    }
}