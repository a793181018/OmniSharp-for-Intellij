package com.github.a793181018.omnisharpforintellij.session;

/**
 * 会话状态枚举
 */
public enum SessionStatus {
    /**
     * 会话正在初始化
     */
    INITIALIZING,
    
    /**
     * 会话已连接
     */
    CONNECTED,
    
    /**
     * 会话正在断开连接
     */
    DISCONNECTING,
    
    /**
     * 会话已断开连接
     */
    DISCONNECTED,
    
    /**
     * 会话出现错误
     */
    ERROR
}