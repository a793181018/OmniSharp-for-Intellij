package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.util.List;

/**
 * 依赖分析器接口
 * 提供依赖分析的核心功能
 */
public interface DependencyAnalyzer {
    /**
     * 分析解决方案的依赖关系
     */
    DependencyAnalysisResult analyzeSolutionDependencies(Path solutionPath);
    
    /**
     * 分析单个项目的依赖关系
     */
    DependencyAnalysisResult analyzeProjectDependencies(Path projectPath);
    
    /**
     * 检测循环依赖
     */
    List<Cycle> detectCycles(Path solutionPath);
    
    /**
     * 检查版本冲突
     */
    List<PackageVersionConflict> checkVersionConflicts(Path solutionPath);
    
    /**
     * 清理缓存
     */
    void clearCache();
}