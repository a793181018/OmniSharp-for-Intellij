package com.github.a793181018.omnisharpforintellij.session;

import com.intellij.openapi.project.Project;

/**
 * OmniSharp会话接口，管理与OmniSharp服务器的会话
 */
public interface OmniSharpSession {
    /**
     * 获取项目实例
     */
    Project getProject();
    
    /**
     * 初始化会话
     */
    void initialize();
    
    /**
     * 销毁会话
     */
    void dispose();
    
    /**
     * 检查会话是否活跃
     */
    boolean isActive();
    
    /**
     * 获取会话ID
     */
    String getSessionId();
    
    /**
     * 更新会话状态
     */
    void updateStatus(SessionStatus status);
    
    /**
     * 获取会话状态
     */
    SessionStatus getStatus();
}