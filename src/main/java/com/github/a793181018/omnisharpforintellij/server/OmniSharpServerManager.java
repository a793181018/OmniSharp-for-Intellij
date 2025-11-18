package com.github.a793181018.omnisharpforintellij.server;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OmniSharp服务器管理器，负责创建和管理服务器实例
 */
@Service(Service.Level.PROJECT)
public final class OmniSharpServerManager {
    private final Project project;
    private final Map<String, OmniSharpServer> servers = new ConcurrentHashMap<>();
    private OmniSharpServer activeServer;
    
    public OmniSharpServerManager(@NotNull Project project) {
        this.project = project;
    }
    
    /**
     * 获取或创建服务器实例
     */
    @NotNull
    public OmniSharpServer getOrCreateServer(@NotNull String serverPath, @NotNull String projectPath) {
        String serverKey = serverPath + ":" + projectPath;
        
        synchronized (this) {
            OmniSharpServer server = servers.get(serverKey);
            if (server == null) {
                server = new OmniSharpServerImpl(serverPath, projectPath);
                servers.put(serverKey, server);
                
                // 默认将第一个创建的服务器设为活动服务器
                if (activeServer == null) {
                    activeServer = server;
                }
            }
            return server;
        }
    }
    
    /**
     * 获取当前活动的服务器
     */
    @Nullable
    public OmniSharpServer getActiveServer() {
        return activeServer;
    }
    
    /**
     * 设置活动服务器
     */
    public void setActiveServer(@NotNull OmniSharpServer server) {
        this.activeServer = server;
    }
    
    /**
     * 获取指定路径的服务器
     */
    @Nullable
    public OmniSharpServer getServer(@NotNull String serverPath, @NotNull String projectPath) {
        String serverKey = serverPath + ":" + projectPath;
        return servers.get(serverKey);
    }
    
    /**
     * 启动指定的服务器
     */
    @NotNull
    public OmniSharpServer startServer(@NotNull String serverPath, @NotNull String projectPath) {
        OmniSharpServer server = getOrCreateServer(serverPath, projectPath);
        if (!server.isRunning()) {
            server.start();
        }
        return server;
    }
    
    /**
     * 停止指定的服务器
     */
    public void stopServer(@NotNull String serverPath, @NotNull String projectPath) {
        String serverKey = serverPath + ":" + projectPath;
        OmniSharpServer server = servers.remove(serverKey);
        if (server != null) {
            server.stop();
            if (server == activeServer) {
                activeServer = null;
            }
            // 释放资源
            if (server instanceof OmniSharpServerImpl) {
                ((OmniSharpServerImpl) server).dispose();
            }
        }
    }
    
    /**
     * 停止所有服务器
     */
    public void stopAllServers() {
        for (OmniSharpServer server : servers.values()) {
            server.stop();
            // 释放资源
            if (server instanceof OmniSharpServerImpl) {
                ((OmniSharpServerImpl) server).dispose();
            }
        }
        servers.clear();
        activeServer = null;
    }
    
    /**
     * 获取服务器数量
     */
    public int getServerCount() {
        return servers.size();
    }
    
    /**
     * 检查是否有运行中的服务器
     */
    public boolean hasRunningServer() {
        for (OmniSharpServer server : servers.values()) {
            if (server.isRunning()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 项目关闭时停止所有服务器
     */
    public static class ProjectCloseListener implements com.intellij.openapi.project.ProjectManagerListener {
        @Override
        public void projectClosing(@NotNull Project project) {
            OmniSharpServerManager manager = project.getService(OmniSharpServerManager.class);
            if (manager != null) {
                manager.stopAllServers();
            }
        }
    }
}