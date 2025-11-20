package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 依赖类型枚举
 */
public enum DependencyType {
    /**
     * 项目依赖
     */
    PROJECT,
    
    /**
     * 包依赖
     */
    PACKAGE,
    
    /**
     * 其他类型的依赖
     */
    OTHER
}