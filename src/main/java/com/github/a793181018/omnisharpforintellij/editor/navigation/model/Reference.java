package com.github.a793181018.omnisharpforintellij.editor.navigation.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 代码引用模型，表示代码中对某个元素的引用。
 * 扩展了NavigationTarget，增加了引用特定的属性。
 */
public class Reference extends NavigationTarget {
    private final boolean writeAccess;
    private final String context;
    private final ReferenceKind referenceKind;
    private final boolean isDefinition;
    private final boolean isDeclaration;
    private final String projectName;
    
    private Reference(Builder builder) {
        super(builder);
        this.writeAccess = builder.writeAccess;
        this.context = builder.context;
        this.referenceKind = builder.referenceKind;
        this.isDefinition = builder.isDefinition;
        this.isDeclaration = builder.isDeclaration;
        this.projectName = builder.projectName;
    }
    
    /**
     * 判断是否为写入引用
     */
    public boolean isWriteAccess() {
        return writeAccess;
    }
    
    /**
     * 获取引用上下文（代码片段）
     */
    @NotNull
    public String getContext() {
        return context;
    }
    
    /**
     * 获取引用类型
     */
    @NotNull
    public ReferenceKind getReferenceKind() {
        return referenceKind;
    }
    
    /**
     * 判断是否为定义
     */
    public boolean isDefinition() {
        return isDefinition;
    }
    
    /**
     * 判断是否为声明
     */
    public boolean isDeclaration() {
        return isDeclaration;
    }
    
    /**
     * 获取项目名称
     */
    @Nullable
    public String getProjectName() {
        return projectName;
    }
    
    /**
     * 获取引用的显示文本
     */
    @NotNull
    public String getDisplayText() {
        StringBuilder sb = new StringBuilder();
        
        // 添加访问类型信息
        if (writeAccess) {
            sb.append("[写入]");
        } else {
            sb.append("[读取]");
        }
        
        // 添加引用类型
        sb.append(" ").append(referenceKind);
        
        // 添加位置信息
        sb.append(" - ").append(getFilePath()).append(":").append(getLine());
        
        // 添加上下文预览
        String contextPreview = context.trim();
        if (contextPreview.length() > 50) {
            contextPreview = contextPreview.substring(0, 47) + "...";
        }
        sb.append(" - \"").append(contextPreview).append("\"");
        
        return sb.toString();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Reference reference = (Reference) o;
        return writeAccess == reference.writeAccess && 
               isDefinition == reference.isDefinition && 
               isDeclaration == reference.isDeclaration && 
               context.equals(reference.context) && 
               referenceKind == reference.referenceKind && 
               Objects.equals(projectName, reference.projectName);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), writeAccess, context, referenceKind, 
                          isDefinition, isDeclaration, projectName);
    }
    
    @Override
    public String toString() {
        return "Reference{" +
               "filePath='" + getFilePath() + '\'' +
               ", name='" + getName() + '\'' +
               ", writeAccess=" + writeAccess +
               ", referenceKind=" + referenceKind +
               ", isDefinition=" + isDefinition +
               ", isDeclaration=" + isDeclaration +
               '}';
    }
    
    /**
     * 引用类型枚举
     */
    public enum ReferenceKind {
        /**
         * 普通引用
         */
        NORMAL,
        /**
         * 方法调用
         */
        METHOD_CALL,
        /**
         * 属性访问
         */
        PROPERTY_ACCESS,
        /**
         * 字段访问
         */
        FIELD_ACCESS,
        /**
         * 继承引用
         */
        INHERITANCE,
        /**
         * 实现引用
         */
        IMPLEMENTATION,
        /**
         * 接口引用
         */
        INTERFACE_IMPLEMENTATION,
        /**
         * 类型参数约束
         */
        TYPE_PARAMETER_CONSTRAINT,
        /**
         * 导入语句
         */
        IMPORT,
        /**
         * 命名空间引用
         */
        NAMESPACE_USING,
        /**
         * 委托引用
         */
        DELEGATE_INVOCATION,
        /**
         * 事件订阅
         */
        EVENT_SUBSCRIPTION,
        /**
         * 其他类型的引用
         */
        OTHER
    }
    
    /**
     * 构建器类，用于创建Reference实例
     */
    public static class Builder extends NavigationTarget.Builder {
        private boolean writeAccess = false;
        private String context = "";
        private ReferenceKind referenceKind = ReferenceKind.NORMAL;
        private boolean isDefinition = false;
        private boolean isDeclaration = false;
        private String projectName;
        
        public Builder(@NotNull String filePath, @NotNull String name, @NotNull TargetType type) {
            super(filePath, name, type);
        }
        
        @NotNull
        public Builder withWriteAccess(boolean writeAccess) {
            this.writeAccess = writeAccess;
            return this;
        }
        
        @NotNull
        public Builder withContext(@NotNull String context) {
            this.context = context;
            return this;
        }
        
        @NotNull
        public Builder withReferenceKind(@NotNull ReferenceKind referenceKind) {
            this.referenceKind = referenceKind;
            return this;
        }
        
        @NotNull
        public Builder withIsDefinition(boolean isDefinition) {
            this.isDefinition = isDefinition;
            return this;
        }
        
        @NotNull
        public Builder withIsDeclaration(boolean isDeclaration) {
            this.isDeclaration = isDeclaration;
            return this;
        }
        
        @NotNull
        public Builder withProjectName(@NotNull String projectName) {
            this.projectName = projectName;
            return this;
        }
        
        @Override
        @NotNull
        public Builder withLine(int line) {
            super.withLine(line);
            return this;
        }
        
        @Override
        @NotNull
        public Builder withColumn(int column) {
            super.withColumn(column);
            return this;
        }
        
        @Override
        @NotNull
        public Builder withContainingType(@NotNull String containingType) {
            super.withContainingType(containingType);
            return this;
        }
        
        @Override
        @NotNull
        public Builder withContainingNamespace(@NotNull String containingNamespace) {
            super.withContainingNamespace(containingNamespace);
            return this;
        }
        
        @Override
        @NotNull
        public Builder withSignature(@NotNull String signature) {
            super.withSignature(signature);
            return this;
        }
        
        @Override
        @NotNull
        public Builder withSourceCode(@NotNull String sourceCode) {
            super.withSourceCode(sourceCode);
            return this;
        }
        
        @Override
        @NotNull
        public Reference build() {
            return new Reference(this);
        }
    }
}