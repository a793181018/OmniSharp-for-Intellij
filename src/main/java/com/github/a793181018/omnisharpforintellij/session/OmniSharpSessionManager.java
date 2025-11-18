package com.github.a793181018.omnisharpforintellij.session;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OmniSharp会话管理器，负责创建和管理项目的OmniSharp会话
 */
@Service(Service.Level.PROJECT)
public final class OmniSharpSessionManager {
    private final Project project;
    private final Map<String, OmniSharpSession> sessions = new ConcurrentHashMap<>();
    private OmniSharpSession activeSession;
    
    public OmniSharpSessionManager(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 获取或创建会话
     */
    @NotNull
    public OmniSharpSession getOrCreateSession() {
        if (activeSession == null) {
            synchronized (this) {
                if (activeSession == null) {
                    activeSession = createNewSession();
                }
            }
        }
        return activeSession;
    }
    
    /**
     * 创建新的会话
     */
    @NotNull
    private OmniSharpSession createNewSession() {
        OmniSharpSession session = new OmniSharpSessionImpl(project);
        sessions.put(session.getSessionId(), session);
        session.initialize();
        return session;
    }
    
    /**
     * 获取当前活动的会话
     */
    @Nullable
    public OmniSharpSession getActiveSession() {
        return activeSession;
    }
    
    /**
     * 根据ID获取会话
     */
    @Nullable
    public OmniSharpSession getSession(@NotNull String sessionId) {
        return sessions.get(sessionId);
    }
    
    /**
     * 关闭会话
     */
    public void closeSession(@NotNull String sessionId) {
        OmniSharpSession session = sessions.remove(sessionId);
        if (session != null) {
            session.dispose();
            if (session == activeSession) {
                activeSession = null;
            }
        }
    }
    
    /**
     * 关闭所有会话
     */
    public void closeAllSessions() {
        for (OmniSharpSession session : sessions.values()) {
            session.dispose();
        }
        sessions.clear();
        activeSession = null;
    }
    
    /**
     * 项目关闭时清理会话
     */
    public static class ProjectCloseListener implements ProjectManagerListener {
        @Override
        public void projectClosing(@NotNull Project project) {
            OmniSharpSessionManager manager = project.getService(OmniSharpSessionManager.class);
            if (manager != null) {
                manager.closeAllSessions();
            }
        }
    }
}