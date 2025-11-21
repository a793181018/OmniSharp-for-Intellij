package com.omnisharp.intellij.projectstructure.model;

import com.intellij.openapi.vfs.VirtualFile;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示项目结构树中的节点
 */
public class ProjectStructureNode {
    private final String name;
    private final NodeType type;
    private final VirtualFile virtualFile;
    private final String projectId;
    private final ProjectStructureNode parent;
    private final List<ProjectStructureNode> children;

    public ProjectStructureNode(String name, NodeType type, VirtualFile virtualFile, String projectId) {
        this(name, type, virtualFile, projectId, null);
    }

    public ProjectStructureNode(String name, NodeType type, VirtualFile virtualFile, String projectId, ProjectStructureNode parent) {
        this.name = name;
        this.type = type;
        this.virtualFile = virtualFile;
        this.projectId = projectId;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public NodeType getType() {
        return type;
    }

    public VirtualFile getVirtualFile() {
        return virtualFile;
    }

    public String getProjectId() {
        return projectId;
    }
    
    /**
     * 获取节点ID
     * @return 节点ID
     */
    public String getId() {
        // 简单实现：使用路径作为ID
        return getPath();
    }

    public ProjectStructureNode getParent() {
        return parent;
    }

    public List<ProjectStructureNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public void addChild(ProjectStructureNode child) {
        children.add(child);
    }

    public void removeChild(ProjectStructureNode child) {
        children.remove(child);
    }

    public boolean hasChildren() {
        return !children.isEmpty();
    }

    public int getChildCount() {
        return children.size();
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isSolution() {
        return type == NodeType.SOLUTION;
    }

    public boolean isProject() {
        return type == NodeType.PROJECT;
    }

    public boolean isFile() {
        return type == NodeType.FILE;
    }

    public boolean isFolder() {
        return type == NodeType.FOLDER;
    }

    public String getPath() {
        if (parent == null) {
            return name;
        }
        return parent.getPath() + "/" + name;
    }
    
    /**
     * 获取节点的结构信息
     * @return 节点结构标识符
     */
    public String getStructure() {
        // 返回节点的路径作为结构标识符
        return getPath();
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }
}