package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 依赖节点接口
 * 表示依赖图中的一个节点，可以是项目、包或其他依赖项
 */
public interface DependencyNode {
    /**
     * 获取节点的唯一标识符
     */
    String getId();
    
    /**
     * 获取节点名称
     */
    String getName();
    
    /**
     * 获取节点类型
     */
    DependencyType getType();
}