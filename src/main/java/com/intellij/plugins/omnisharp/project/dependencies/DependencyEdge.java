package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 依赖边
 * 表示两个依赖节点之间的关系
 */
public class DependencyEdge {
    private final DependencyNode source;
    private final DependencyNode target;
    private final EdgeType type;
    
    public DependencyEdge(DependencyNode source, DependencyNode target, EdgeType type) {
        this.source = source;
        this.target = target;
        this.type = type;
    }
    
    /**
     * 获取源节点
     */
    public DependencyNode getSource() {
        return source;
    }
    
    /**
     * 获取目标节点
     */
    public DependencyNode getTarget() {
        return target;
    }
    
    /**
     * 获取边类型
     */
    public EdgeType getType() {
        return type;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        DependencyEdge that = (DependencyEdge) o;
        return source.equals(that.source) && target.equals(that.target);
    }
    
    @Override
    public int hashCode() {
        int result = source.hashCode();
        result = 31 * result + target.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "DependencyEdge{" + source.getName() + " -> " + target.getName() + " (" + type + ")}";
    }
}