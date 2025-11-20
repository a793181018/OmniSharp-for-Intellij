package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.List;

/**
 * 依赖图构建器接口
 * 负责构建依赖图
 */
public interface DependencyGraphBuilder {
    /**
     * 构建项目依赖图
     */
    DependencyGraph buildProjectDependencyGraph(ProjectResolver.ProjectInfo projectInfo,
                                               List<ProjectDependency> projectDependencies,
                                               List<PackageDependency> packageDependencies);
    
    /**
     * 构建包依赖图
     */
    DependencyGraph buildPackageDependencyGraph(List<PackageDependency> packageDependencies);
    
    /**
     * 合并依赖图
     */
    DependencyGraph mergeDependencyGraphs(List<DependencyGraph> graphs);
}