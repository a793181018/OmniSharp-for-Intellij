package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.List;
import java.util.Set;

/**
 * 依赖图接口
 * 定义依赖图的核心操作方法
 */
public interface DependencyGraph {
    /**
     * 添加节点到图中
     */
    void addNode(DependencyNode node);
    
    /**
     * 添加边到图中
     */
    void addEdge(DependencyEdge edge);
    
    /**
     * 获取图中的所有节点
     */
    Set<DependencyNode> getAllNodes();
    
    /**
     * 获取图中的所有边
     */
    Set<DependencyEdge> getAllEdges();
    
    /**
     * 根据ID获取节点
     */
    DependencyNode getNodeById(String id);
    
    /**
     * 获取节点的出边
     */
    List<DependencyEdge> getOutgoingEdges(DependencyNode node);
    
    /**
     * 获取节点的入边
     */
    List<DependencyEdge> getIncomingEdges(DependencyNode node);
    
    /**
     * 查找所有循环依赖
     */
    List<Cycle> findCycles();
    
    /**
     * 检查是否包含循环依赖
     */
    boolean hasCycles();
    
    /**
     * 合并另一个依赖图
     */
    void merge(DependencyGraph other);
    
    /**
     * 清除图中的所有节点和边
     */
    void clear();
}