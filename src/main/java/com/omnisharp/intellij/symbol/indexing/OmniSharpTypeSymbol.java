package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 类型符号类
 * 表示类、接口、结构、枚举等类型相关的符号
 */
public class OmniSharpTypeSymbol extends OmniSharpSymbol {
    /** 类型的基类/接口列表 */
    private final List<String> baseTypes;
    /** 嵌套在该类型中的成员符号 */
    private final List<OmniSharpSymbol> members;
    /** 该类型是否是抽象的 */
    private final boolean isAbstract;
    /** 该类型是否是密封的 */
    private final boolean isSealed;
    /** 该类型是否是静态的 */
    private final boolean isStatic;
    /** 类型的可见性 */
    private final Visibility visibility;

    /**
     * 可见性枚举
     */
    public enum Visibility {
        PUBLIC,
        PRIVATE,
        PROTECTED,
        INTERNAL,
        PROTECTED_INTERNAL,
        PRIVATE_PROTECTED
    }

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
     * @param baseTypes 类型的基类/接口列表
     * @param isAbstract 该类型是否是抽象的
     * @param isSealed 该类型是否是密封的
     * @param isStatic 该类型是否是静态的
     * @param visibility 类型的可见性
     */
    public OmniSharpTypeSymbol(
            String name,
            String fullyQualifiedName,
            OmniSharpSymbolKind kind,
            Path filePath,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            String projectName,
            List<String> baseTypes,
            boolean isAbstract,
            boolean isSealed,
            boolean isStatic,
            Visibility visibility) {
        super(name, fullyQualifiedName, kind, filePath, startLine, startColumn, endLine, endColumn, projectName);
        this.baseTypes = baseTypes != null ? new ArrayList<>(baseTypes) : new ArrayList<>();
        this.members = new ArrayList<>();
        this.isAbstract = isAbstract;
        this.isSealed = isSealed;
        this.isStatic = isStatic;
        this.visibility = visibility;
    }

    public List<String> getBaseTypes() {
        return Collections.unmodifiableList(baseTypes);
    }

    public List<OmniSharpSymbol> getMembers() {
        return Collections.unmodifiableList(members);
    }

    /**
     * 添加成员符号
     * @param member 要添加的成员符号
     */
    public void addMember(OmniSharpSymbol member) {
        members.add(member);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isSealed() {
        return isSealed;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        return "OmniSharpTypeSymbol{" +
                "name='" + getName() + '\'' +
                ", kind=" + getKind() +
                ", visibility=" + visibility +
                ", abstract=" + isAbstract +
                ", sealed=" + isSealed +
                ", static=" + isStatic +
                ", baseTypes=" + baseTypes +
                ", membersCount=" + members.size() +
                "}";
    }
}