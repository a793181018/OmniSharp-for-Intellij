package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 循环依赖
 * 表示依赖图中的一个循环
 */
public class Cycle {
    private final List<String> path;
    private final List<DependencyNode> nodes;
    private final List<DependencyEdge> edges;
    
    public Cycle() {
        this.path = new ArrayList<>();
        this.nodes = new ArrayList<>();
        this.edges = new ArrayList<>();
    }
    
    /**
     * 获取循环路径（节点名称列表）
     */
    public List<String> getPath() {
        return Collections.unmodifiableList(path);
    }
    
    /**
     * 获取循环中的节点列表
     */
    public List<DependencyNode> getNodes() {
        return Collections.unmodifiableList(nodes);
    }
    
    /**
     * 获取循环中的边列表
     */
    public List<DependencyEdge> getEdges() {
        return Collections.unmodifiableList(edges);
    }
    
    /**
     * 添加节点到循环中
     */
    public void addNode(DependencyNode node) {
        this.nodes.add(node);
        this.path.add(node.getName());
    }
    
    /**
     * 添加边到循环中
     */
    public void addEdge(DependencyEdge edge) {
        this.edges.add(edge);
    }
    
    /**
     * 获取循环的长度
     */
    public int getLength() {
        return path.size();
    }
    
    @Override
    public String toString() {
        return "Cycle{" + String.join(" -> ", path) + " -> " + path.get(0) + "}";
    }
}