package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 符号基类
 * 表示代码中的符号，如类、方法、属性等
 */
public abstract class OmniSharpSymbol {
    /** 符号名称 */
    private final String name;
    /** 符号完全限定名 */
    private final String fullyQualifiedName;
    /** 符号类型 */
    private final OmniSharpSymbolKind kind;
    /** 包含符号的文件路径 */
    private final Path filePath;
    /** 符号在文件中的起始行号 */
    private final int startLine;
    /** 符号在文件中的起始列号 */
    private final int startColumn;
    /** 符号在文件中的结束行号 */
    private final int endLine;
    /** 符号在文件中的结束列号 */
    private final int endColumn;
    /** 符号所在的项目名称 */
    private final String projectName;

    /**
     * 构造函数
     * @param name 符号名称
     * @param fullyQualifiedName 符号完全限定名
     * @param kind 符号类型
     * @param filePath 包含符号的文件路径
     * @param startLine 符号在文件中的起始行号
     * @param startColumn 符号在文件中的起始列号
     * @param endLine 符号在文件中的结束行号
     * @param endColumn 符号在文件中的结束列号
     * @param projectName 符号所在的项目名称
     */
    protected OmniSharpSymbol(
            String name,
            String fullyQualifiedName,
            OmniSharpSymbolKind kind,
            Path filePath,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            String projectName) {
        this.name = name;
        this.fullyQualifiedName = fullyQualifiedName;
        this.kind = kind;
        this.filePath = filePath;
        this.startLine = startLine;
        this.startColumn = startColumn;
        this.endLine = endLine;
        this.endColumn = endColumn;
        this.projectName = projectName;
    }

    public String getName() {
        return name;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public OmniSharpSymbolKind getKind() {
        return kind;
    }

    public Path getFilePath() {
        return filePath;
    }

    public int getStartLine() {
        return startLine;
    }

    public int getStartColumn() {
        return startColumn;
    }

    public int getEndLine() {
        return endLine;
    }

    public int getEndColumn() {
        return endColumn;
    }

    public String getProjectName() {
        return projectName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OmniSharpSymbol that = (OmniSharpSymbol) o;
        return Objects.equals(fullyQualifiedName, that.fullyQualifiedName) &&
                kind == that.kind &&
                Objects.equals(filePath, that.filePath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fullyQualifiedName, kind, filePath);
    }

    @Override
    public String toString() {
        return "OmniSharpSymbol{" +
                "name='" + name + '\'' +
                ", kind=" + kind +
                ", file=" + filePath.getFileName() +
                ", project='" + projectName + '\'' +
                "}";
    }
}