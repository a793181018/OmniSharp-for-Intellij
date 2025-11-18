package com.github.a793181018.omnisharpforintellij.session;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * OmniSharp会话的默认实现
 */
public class OmniSharpSessionImpl implements OmniSharpSession {
    private final Project project;
    private final String sessionId;
    private volatile SessionStatus status = SessionStatus.DISCONNECTED;
    private final CopyOnWriteArrayList<OmniSharpSessionListener> listeners = new CopyOnWriteArrayList<>();
    
    public OmniSharpSessionImpl(@NotNull Project project) {
        this.project = project;
        this.sessionId = UUID.randomUUID().toString();
    }
    
    @Override
    public Project getProject() {
        return project;
    }
    
    @Override
    public void initialize() {
        updateStatus(SessionStatus.INITIALIZING);
        // 初始化会话的逻辑
        // 例如：连接到OmniSharp服务器
        updateStatus(SessionStatus.CONNECTED);
    }
    
    @Override
    public void dispose() {
        updateStatus(SessionStatus.DISCONNECTING);
        // 清理资源的逻辑
        updateStatus(SessionStatus.DISCONNECTED);
        listeners.clear();
    }
    
    @Override
    public boolean isActive() {
        return status == SessionStatus.CONNECTED;
    }
    
    @Override
    public String getSessionId() {
        return sessionId;
    }
    
    @Override
    public void updateStatus(SessionStatus newStatus) {
        if (this.status != newStatus) {
            SessionStatus oldStatus = this.status;
            this.status = newStatus;
            notifyStatusChanged(oldStatus, newStatus);
        }
    }
    
    @Override
    public SessionStatus getStatus() {
        return status;
    }
    
    /**
     * 添加会话监听器
     */
    public void addListener(@NotNull OmniSharpSessionListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除会话监听器
     */
    public void removeListener(@NotNull OmniSharpSessionListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 通知状态变更
     */
    private void notifyStatusChanged(SessionStatus oldStatus, SessionStatus newStatus) {
        for (OmniSharpSessionListener listener : listeners) {
            listener.onStatusChanged(this, oldStatus, newStatus);
        }
    }
}