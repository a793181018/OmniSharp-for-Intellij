package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import com.omnisharp.intellij.projectstructure.model.ProjectStructureNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 导航节点类，用于表示项目结构中的导航节点
 */
public class NavigationNode {
    private final String id;
    private final String name;
    private final NodeType nodeType;
    private final String path;
    private final NavigationNode parent;
    private final List<NavigationNode> children;
    private final Object data;
    
    public NavigationNode(@NotNull String name, @NotNull NodeType nodeType, @NotNull String path) {
        this(UUID.randomUUID().toString(), name, nodeType, path, null, null);
    }
    
    public NavigationNode(@NotNull String id, @NotNull String name, @NotNull NodeType nodeType, @NotNull String path,
                         @Nullable NavigationNode parent, @Nullable Object data) {
        this.id = id;
        this.name = name;
        this.nodeType = nodeType;
        this.path = path;
        this.parent = parent;
        this.children = new ArrayList<>();
        this.data = data;
    }
    
    /**
     * 获取节点ID
     * @return 节点ID
     */
    @NotNull
    public String getId() {
        return id;
    }
    
    /**
     * 获取节点名称
     * @return 节点名称
     */
    @NotNull
    public String getName() {
        return name;
    }
    
    /**
     * 获取节点类型
     * @return 节点类型
     */
    @NotNull
    public NodeType getNodeType() {
        return nodeType;
    }
    
    /**
     * 获取节点路径
     * @return 节点路径
     */
    @NotNull
    public String getPath() {
        return path;
    }
    
    /**
     * 获取父节点
     * @return 父节点，如果是根节点则返回null
     */
    @Nullable
    public NavigationNode getParent() {
        return parent;
    }
    
    /**
     * 获取子节点列表
     * @return 子节点列表
     */
    @NotNull
    public List<NavigationNode> getChildren() {
        return Collections.unmodifiableList(children);
    }
    
    /**
     * 添加子节点
     * @param child 子节点
     */
    public void addChild(@NotNull NavigationNode child) {
        children.add(child);
    }
    
    /**
     * 移除子节点
     * @param child 子节点
     * @return 是否移除成功
     */
    public boolean removeChild(@NotNull NavigationNode child) {
        return children.remove(child);
    }
    
    /**
     * 获取节点数据
     * @return 节点关联的数据
     */
    @Nullable
    public Object getData() {
        return data;
    }
    
    /**
     * 检查是否为叶子节点
     * @return 如果没有子节点则返回true
     */
    public boolean isLeaf() {
        return children.isEmpty();
    }
    
    /**
     * 检查是否为根节点
     * @return 如果没有父节点则返回true
     */
    public boolean isRoot() {
        return parent == null;
    }
    
    /**
     * 获取节点深度
     * @return 节点深度
     */
    public int getDepth() {
        if (isRoot()) {
            return 0;
        }
        return parent.getDepth() + 1;
    }
    
    /**
     * 查找子节点
     * @param name 子节点名称
     * @return 找到的子节点，如果不存在则返回null
     */
    @Nullable
    public NavigationNode findChildByName(@NotNull String name) {
        return children.stream()
                .filter(child -> child.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 查找子节点
     * @param id 子节点ID
     * @return 找到的子节点，如果不存在则返回null
     */
    @Nullable
    public NavigationNode findChildById(@NotNull String id) {
        return children.stream()
                .filter(child -> child.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 从项目结构节点创建导航节点
     * @param structureNode 项目结构节点
     * @param parent 父导航节点
     * @return 导航节点
     */
    public static NavigationNode fromProjectStructureNode(
            @NotNull com.omnisharp.intellij.projectstructure.model.ProjectStructureNode structureNode, 
            @Nullable NavigationNode parent) {
        NavigationNode node = new NavigationNode(
                structureNode.getId(),
                structureNode.getName(),
                structureNode.getType(),
                structureNode.getPath(),
                parent,
                structureNode
        );
        
        // 递归创建子节点
        for (com.omnisharp.intellij.projectstructure.model.ProjectStructureNode child : structureNode.getChildren()) {
            node.addChild(fromProjectStructureNode(child, node));
        }
        
        return node;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        NavigationNode that = (NavigationNode) o;
        return id.equals(that.id);
    }
    
    @Override
    public int hashCode() {
        return id.hashCode();
    }
    
    @Override
    public String toString() {
        return "NavigationNode{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + nodeType +
                ", path='" + path + '\'' +
                ", children=" + children.size() +
                '}';
    }
}