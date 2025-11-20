package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 项目解析异常
 * 当解析项目文件失败时抛出
 */
public class ProjectResolutionException extends Exception {
    private final String projectPath;
    
    public ProjectResolutionException(String projectPath, String message) {
        super(message);
        this.projectPath = projectPath;
    }
    
    public ProjectResolutionException(String projectPath, String message, Throwable cause) {
        super(message, cause);
        this.projectPath = projectPath;
    }
    
    /**
     * 获取项目路径
     */
    public String getProjectPath() {
        return projectPath;
    }
    
    @Override
    public String toString() {
        return "ProjectResolutionException{projectPath='" + projectPath + "', message='" + getMessage() + "'}";
    }
}