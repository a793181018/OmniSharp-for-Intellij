package com.github.a793181018.omnisharpforintellij.server.model;

/**
 * OmniSharp服务器状态枚举
 */
public enum ServerStatus {
    /**
     * 服务器未启动
     */
    NOT_STARTED,
    
    /**
     * 服务器正在启动
     */
    STARTING,
    
    /**
     * 服务器正在运行
     */
    RUNNING,
    
    /**
     * 服务器正在停止
     */
    STOPPING,
    
    /**
     * 服务器已停止
     */
    STOPPED,
    
    /**
     * 服务器启动失败
     */
    FAILED
}