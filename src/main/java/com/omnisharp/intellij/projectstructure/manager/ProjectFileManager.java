package com.omnisharp.intellij.projectstructure.manager;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.watcher.ProjectFileWatcher;

import java.nio.file.Path;
import java.util.EventListener;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 项目文件管理器接口
 * 负责项目文件的加载、解析、监控和缓存管理
 */
public interface ProjectFileManager {
    
    /**
     * 加载项目文件
     * @param projectPath 项目文件路径
     * @return 项目模型
     * @throws ProjectManagerException 项目管理异常
     */
    ProjectModel loadProject(Path projectPath) throws ProjectManagerException;
    
    /**
     * 异步加载项目文件
     * @param projectPath 项目文件路径
     * @return 包含项目模型的CompletableFuture
     */
    CompletableFuture<ProjectModel> loadProjectAsync(Path projectPath);
    
    /**
     * 获取已加载的项目模型
     * @param projectPath 项目文件路径
     * @return 项目模型，如果不存在返回null
     */
    ProjectModel getProjectModel(Path projectPath);
    
    /**
     * 获取所有已加载的项目
     * @return 已加载项目路径列表
     */
    List<Path> getLoadedProjects();
    
    /**
     * 刷新项目
     * @param projectPath 项目文件路径
     * @return 刷新后的项目模型
     * @throws ProjectManagerException 项目管理异常
     */
    ProjectModel refreshProject(Path projectPath) throws ProjectManagerException;
    
    /**
     * 异步刷新项目
     * @param projectPath 项目文件路径
     * @return 包含刷新后项目模型的CompletableFuture
     */
    CompletableFuture<ProjectModel> refreshProjectAsync(Path projectPath);
    
    /**
     * 关闭项目
     * @param projectPath 项目文件路径
     */
    void closeProject(Path projectPath);
    
    /**
     * 关闭所有已加载的项目
     */
    void closeAllProjects();
    
    /**
     * 检查项目是否已加载
     * @param projectPath 项目文件路径
     * @return 是否已加载
     */
    boolean isProjectLoaded(Path projectPath);
    
    /**
     * 启动项目监控
     * @param projectPath 项目文件路径
     */
    void startWatching(Path projectPath);
    
    /**
     * 停止项目监控
     * @param projectPath 项目文件路径
     */
    void stopWatching(Path projectPath);
    
    /**
     * 添加项目变更监听器
     * @param listener 项目变更监听器
     */
    void addProjectChangeListener(ProjectChangeListener listener);
    
    /**
     * 移除项目变更监听器
     * @param listener 项目变更监听器
     */
    void removeProjectChangeListener(ProjectChangeListener listener);
    
    /**
     * 项目变更监听器接口
     */
    interface ProjectChangeListener extends EventListener {
        
        /**
         * 当项目被加载时触发
         * @param projectModel 项目模型
         */
        void projectLoaded(ProjectModel projectModel);
        
        /**
         * 当项目被刷新时触发
         * @param projectModel 刷新后的项目模型
         */
        void projectRefreshed(ProjectModel projectModel);
        
        /**
         * 当项目被关闭时触发
         * @param projectPath 项目文件路径
         */
        void projectClosed(Path projectPath);
        
        /**
         * 当项目文件发生变更时触发
         * @param projectPath 项目文件路径
         * @param projectModel 变更后的项目模型
         */
        void projectChanged(Path projectPath, ProjectModel projectModel);
        
        /**
         * 当发生错误时触发
         * @param error 错误信息
         * @param projectPath 相关的项目文件路径，可能为null
         */
        void projectError(Exception error, Path projectPath);
    }
    
    /**
     * 项目管理异常类
     */
    class ProjectManagerException extends Exception {
        public ProjectManagerException(String message) {
            super(message);
        }
        
        public ProjectManagerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}