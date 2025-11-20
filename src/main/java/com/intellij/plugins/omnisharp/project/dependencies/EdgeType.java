package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 边类型枚举
 * 表示依赖关系的类型
 */
public enum EdgeType {
    /**
     * 项目引用
     */
    PROJECT_REFERENCE,
    
    /**
     * 包引用
     */
    PACKAGE_REFERENCE,
    
    /**
     * 传递依赖
     */
    TRANSITIVE_DEPENDENCY,
    
    /**
     * 其他类型的依赖
     */
    OTHER
}