package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.List;
import java.util.Set;

/**
 * 依赖分析结果
 * 包含依赖分析的所有信息
 */
public class DependencyAnalysisResult {
    private final DependencyGraph dependencyGraph;
    private final List<ProjectDependency> projectDependencies;
    private final List<PackageDependency> packageDependencies;
    private final List<Cycle> cycles;
    private final List<PackageVersionConflict> versionConflicts;
    private final boolean success;
    private final String errorMessage;
    
    private DependencyAnalysisResult(Builder builder) {
        this.dependencyGraph = builder.dependencyGraph;
        this.projectDependencies = builder.projectDependencies;
        this.packageDependencies = builder.packageDependencies;
        this.cycles = builder.cycles;
        this.versionConflicts = builder.versionConflicts;
        this.success = builder.success;
        this.errorMessage = builder.errorMessage;
    }
    
    /**
     * 获取依赖图
     */
    public DependencyGraph getDependencyGraph() {
        return dependencyGraph;
    }
    
    /**
     * 获取项目依赖列表
     */
    public List<ProjectDependency> getProjectDependencies() {
        return projectDependencies;
    }
    
    /**
     * 获取包依赖列表
     */
    public List<PackageDependency> getPackageDependencies() {
        return packageDependencies;
    }
    
    /**
     * 获取循环依赖列表
     */
    public List<Cycle> getCycles() {
        return cycles;
    }
    
    /**
     * 获取版本冲突列表
     */
    public List<PackageVersionConflict> getVersionConflicts() {
        return versionConflicts;
    }
    
    /**
     * 分析是否成功
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取错误信息
     */
    public String getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * 是否存在循环依赖
     */
    public boolean hasCycles() {
        return !cycles.isEmpty();
    }
    
    /**
     * 是否存在版本冲突
     */
    public boolean hasVersionConflicts() {
        return !versionConflicts.isEmpty();
    }
    
    /**
     * 创建构建器
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * 构建器类
     */
    public static class Builder {
        private DependencyGraph dependencyGraph = new DependencyGraphImpl();
        private List<ProjectDependency> projectDependencies = List.of();
        private List<PackageDependency> packageDependencies = List.of();
        private List<Cycle> cycles = List.of();
        private List<PackageVersionConflict> versionConflicts = List.of();
        private boolean success = true;
        private String errorMessage = null;
        
        public Builder dependencyGraph(DependencyGraph dependencyGraph) {
            this.dependencyGraph = dependencyGraph;
            return this;
        }
        
        public Builder projectDependencies(List<ProjectDependency> projectDependencies) {
            this.projectDependencies = projectDependencies;
            return this;
        }
        
        public Builder packageDependencies(List<PackageDependency> packageDependencies) {
            this.packageDependencies = packageDependencies;
            return this;
        }
        
        public Builder cycles(List<Cycle> cycles) {
            this.cycles = cycles;
            return this;
        }
        
        public Builder versionConflicts(List<PackageVersionConflict> versionConflicts) {
            this.versionConflicts = versionConflicts;
            return this;
        }
        
        public Builder success(boolean success) {
            this.success = success;
            return this;
        }
        
        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            this.success = false;
            return this;
        }
        
        public DependencyAnalysisResult build() {
            return new DependencyAnalysisResult(this);
        }
    }
}