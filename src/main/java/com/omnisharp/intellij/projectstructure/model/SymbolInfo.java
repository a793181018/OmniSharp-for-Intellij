package com.omnisharp.intellij.projectstructure.model;

import java.util.Collections;
import java.util.List;

/**
 * 表示代码中的符号信息
 */
public class SymbolInfo {
    private final String id;
    private final String name;
    private final SymbolKind kind;
    private final String fullyQualifiedName;
    private final String projectId;
    private final String filePath;
    private final int line;
    private final int column;
    private final String documentation;
    private final List<String> modifiers;

    public SymbolInfo(
            String id,
            String name,
            SymbolKind kind,
            String fullyQualifiedName,
            String projectId,
            String filePath,
            int line,
            int column) {
        this(id, name, kind, fullyQualifiedName, projectId, filePath, line, column, null, Collections.emptyList());
    }

    public SymbolInfo(
            String id,
            String name,
            SymbolKind kind,
            String fullyQualifiedName,
            String projectId,
            String filePath,
            int line,
            int column,
            String documentation,
            List<String> modifiers) {
        this.id = id;
        this.name = name;
        this.kind = kind;
        this.fullyQualifiedName = fullyQualifiedName;
        this.projectId = projectId;
        this.filePath = filePath;
        this.line = line;
        this.column = column;
        this.documentation = documentation;
        this.modifiers = modifiers != null ? modifiers : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public SymbolKind getKind() {
        return kind;
    }

    public String getFullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getDocumentation() {
        return documentation;
    }

    public List<String> getModifiers() {
        return modifiers;
    }

    @Override
    public String toString() {
        return kind + ": " + fullyQualifiedName;
    }
}