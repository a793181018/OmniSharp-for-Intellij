package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 项目节点
 * 表示解决方案中的一个项目
 */
public class ProjectNode implements DependencyNode {
    private final String projectPath;
    private final String name;
    private final String projectFile;
    
    public ProjectNode(String projectPath, String name, String projectFile) {
        this.projectPath = projectPath;
        this.name = name;
        this.projectFile = projectFile;
    }
    
    @Override
    public String getId() {
        return projectPath;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public DependencyType getType() {
        return DependencyType.PROJECT;
    }
    
    /**
     * 获取项目文件路径
     */
    public String getProjectFile() {
        return projectFile;
    }
    
    /**
     * 获取项目路径
     */
    public String getProjectPath() {
        return projectPath;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        ProjectNode that = (ProjectNode) o;
        return projectPath.equals(that.projectPath);
    }
    
    @Override
    public int hashCode() {
        return projectPath.hashCode();
    }
    
    @Override
    public String toString() {
        return "ProjectNode{name='" + name + "', path='" + projectPath + "'}";
    }
}