package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;

/**
 * 属性符号类
 * 表示属性相关的符号
 */
public class OmniSharpPropertySymbol extends OmniSharpSymbol {
    /** 属性的类型 */
    private final String propertyType;
    /** 是否有getter */
    private final boolean hasGetter;
    /** 是否有setter */
    private final boolean hasSetter;
    /** 该属性是否是虚拟的 */
    private final boolean isVirtual;
    /** 该属性是否是重写的 */
    private final boolean isOverride;
    /** 该属性是否是静态的 */
    private final boolean isStatic;
    /** 该属性是否是只读的 */
    private final boolean isReadOnly;
    /** 该属性是否是只写的 */
    private final boolean isWriteOnly;
    /** 属性的可见性 */
    private final OmniSharpTypeSymbol.Visibility visibility;

    /**
     * 构造函数
     * @param name 符号名称
     * @param fullyQualifiedName 符号完全限定名
     * @param filePath 包含符号的文件路径
     * @param startLine 符号在文件中的起始行号
     * @param startColumn 符号在文件中的起始列号
     * @param endLine 符号在文件中的结束行号
     * @param endColumn 符号在文件中的结束列号
     * @param projectName 符号所在的项目名称
     * @param propertyType 属性的类型
     * @param hasGetter 是否有getter
     * @param hasSetter 是否有setter
     * @param isVirtual 该属性是否是虚拟的
     * @param isOverride 该属性是否是重写的
     * @param isStatic 该属性是否是静态的
     * @param isReadOnly 该属性是否是只读的
     * @param isWriteOnly 该属性是否是只写的
     * @param visibility 属性的可见性
     */
    public OmniSharpPropertySymbol(
            String name,
            String fullyQualifiedName,
            Path filePath,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            String projectName,
            String propertyType,
            boolean hasGetter,
            boolean hasSetter,
            boolean isVirtual,
            boolean isOverride,
            boolean isStatic,
            boolean isReadOnly,
            boolean isWriteOnly,
            OmniSharpTypeSymbol.Visibility visibility) {
        super(name, fullyQualifiedName, OmniSharpSymbolKind.PROPERTY, filePath, startLine, startColumn, endLine, endColumn, projectName);
        this.propertyType = propertyType;
        this.hasGetter = hasGetter;
        this.hasSetter = hasSetter;
        this.isVirtual = isVirtual;
        this.isOverride = isOverride;
        this.isStatic = isStatic;
        this.isReadOnly = isReadOnly;
        this.isWriteOnly = isWriteOnly;
        this.visibility = visibility;
    }

    public String getPropertyType() {
        return propertyType;
    }

    public boolean hasGetter() {
        return hasGetter;
    }

    public boolean hasSetter() {
        return hasSetter;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public boolean isOverride() {
        return isOverride;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isWriteOnly() {
        return isWriteOnly;
    }

    public OmniSharpTypeSymbol.Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return "OmniSharpPropertySymbol{" +
                "name='" + getName() + '\'' +
                ", type='" + propertyType + '\'' +
                ", getter=" + hasGetter +
                ", setter=" + hasSetter +
                ", static=" + isStatic +
                ", visibility=" + visibility +
                "}";
    }
}