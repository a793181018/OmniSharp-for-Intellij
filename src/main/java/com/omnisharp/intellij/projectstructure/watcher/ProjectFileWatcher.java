package com.omnisharp.intellij.projectstructure.watcher;

import java.nio.file.Path;
import java.util.EventListener;

/**
 * 项目文件监控器接口
 * 负责监控项目文件的变化，并在文件变更时触发相应的事件
 */
public interface ProjectFileWatcher {
    
    /**
     * 启动监控器
     * @param projectPath 项目文件路径
     */
    void startWatching(Path projectPath);
    
    /**
     * 停止监控器
     */
    void stopWatching();
    
    /**
     * 添加项目文件变更监听器
     * @param listener 变更监听器
     */
    void addProjectFileChangeListener(ProjectFileChangeListener listener);
    
    /**
     * 移除项目文件变更监听器
     * @param listener 变更监听器
     */
    void removeProjectFileChangeListener(ProjectFileChangeListener listener);
    
    /**
     * 获取当前监控状态
     * @return 是否正在监控
     */
    boolean isWatching();
    
    /**
     * 项目文件变更监听器接口
     */
    interface ProjectFileChangeListener extends EventListener {
        
        /**
         * 当项目文件被创建时触发
         * @param filePath 被创建的文件路径
         */
        void projectFileCreated(Path filePath);
        
        /**
         * 当项目文件被修改时触发
         * @param filePath 被修改的文件路径
         */
        void projectFileModified(Path filePath);
        
        /**
         * 当项目文件被删除时触发
         * @param filePath 被删除的文件路径
         */
        void projectFileDeleted(Path filePath);
        
        /**
         * 当监控发生错误时触发
         * @param error 错误信息
         */
        void watcherError(Exception error);
    }
}