package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 项目依赖
 * 表示一个项目对另一个项目的依赖关系
 */
public class ProjectDependency {
    private final String sourceProjectPath;
    private final String targetProjectPath;
    private final String sourceProjectName;
    private final String targetProjectName;
    
    public ProjectDependency(String sourceProjectPath, String targetProjectPath, 
                            String sourceProjectName, String targetProjectName) {
        this.sourceProjectPath = sourceProjectPath;
        this.targetProjectPath = targetProjectPath;
        this.sourceProjectName = sourceProjectName;
        this.targetProjectName = targetProjectName;
    }
    
    /**
     * 获取源项目路径
     */
    public String getSourceProjectPath() {
        return sourceProjectPath;
    }
    
    /**
     * 获取目标项目路径
     */
    public String getTargetProjectPath() {
        return targetProjectPath;
    }
    
    /**
     * 获取源项目名称
     */
    public String getSourceProjectName() {
        return sourceProjectName;
    }
    
    /**
     * 获取目标项目名称
     */
    public String getTargetProjectName() {
        return targetProjectName;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProjectDependency that = (ProjectDependency) o;
        return sourceProjectPath.equals(that.sourceProjectPath) && 
               targetProjectPath.equals(that.targetProjectPath);
    }
    
    @Override
    public int hashCode() {
        int result = sourceProjectPath.hashCode();
        result = 31 * result + targetProjectPath.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "ProjectDependency{" + sourceProjectName + " -> " + targetProjectName + "}";
    }
}