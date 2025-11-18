package com.github.a793181018.omnisharpforintellij;

import com.github.a793181018.omnisharpforintellij.configuration.OmniSharpConfiguration;
import com.github.a793181018.omnisharpforintellij.configuration.OmniSharpConfigurationFactory;
import com.github.a793181018.omnisharpforintellij.server.OmniSharpServerManager;
import com.github.a793181018.omnisharpforintellij.session.OmniSharpSessionManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * OmniSharp项目组件，管理项目级别的资源和生命周期
 */
public final class OmniSharpProjectComponent implements ProjectComponent {
    
    private static final Logger LOGGER = Logger.getInstance(OmniSharpProjectComponent.class);
    
    private final Project project;
    private final OmniSharpServerManager serverManager;
    private final OmniSharpSessionManager sessionManager;
    private final OmniSharpConfiguration configuration;
    
    public OmniSharpProjectComponent(@NotNull Project project) {
        this.project = project;
        this.serverManager = project.getService(OmniSharpServerManager.class);
        this.sessionManager = project.getService(OmniSharpSessionManager.class);
        this.configuration = OmniSharpConfigurationFactory.getProjectConfiguration(project);
    }
    
    @NotNull
    @Override
    public String getComponentName() {
        return OmniSharpPlugin.PLUGIN_NAME + " Project Component"; 
    }
    
    @Override
    public void initComponent() {
        // 这个方法在项目组件初始化时调用
        LOGGER.info("Initializing " + getComponentName() + " for project: " + project.getName());
    }
    
    @Override
    public void disposeComponent() {
        // 这个方法在项目组件销毁时调用
        LOGGER.info("Disposing " + getComponentName() + " for project: " + project.getName());
    }
    
    @Override
    public void projectOpened() {
        LOGGER.info("Project opened: " + project.getName());
        
        // 当项目打开时，检查是否需要自动启动OmniSharp服务器
        if (configuration.isAutoStartServer()) {
            String serverPath = configuration.getServerPath();
            if (serverPath != null && !serverPath.isEmpty()) {
                File serverFile = new File(serverPath);
                if (serverFile.exists() && serverFile.canExecute()) {
                    String projectPath = project.getBasePath();
                    if (projectPath != null) {
                        LOGGER.info("Auto-starting OmniSharp server for project: " + project.getName());
                        try {
                            serverManager.startServer(serverPath, projectPath);
                        } catch (Exception e) {
                            LOGGER.error("Failed to auto-start OmniSharp server", e);
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void projectClosed() {
        LOGGER.info("Project closed: " + project.getName());
        
        try {
            // 当项目关闭时，清理会话和停止服务器
            sessionManager.closeAllSessions();
            serverManager.stopAllServers();
        } catch (Exception e) {
            LOGGER.error("Error during project closing cleanup", e);
        }
    }
    
    /**
     * 获取项目实例
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    /**
     * 获取服务器管理器
     */
    @NotNull
    public OmniSharpServerManager getServerManager() {
        return serverManager;
    }
    
    /**
     * 获取会话管理器
     */
    @NotNull
    public OmniSharpSessionManager getSessionManager() {
        return sessionManager;
    }
    
    /**
     * 获取项目配置
     */
    @NotNull
    public OmniSharpConfiguration getConfiguration() {
        return configuration;
    }
    
    /**
     * 检查项目是否有运行中的OmniSharp服务器
     */
    public boolean hasRunningServer() {
        return serverManager.hasRunningServer();
    }
    
    /**
     * 获取项目实例的组件
     */
    @NotNull
    public static OmniSharpProjectComponent getInstance(@NotNull Project project) {
        OmniSharpProjectComponent component = project.getComponent(OmniSharpProjectComponent.class);
        if (component == null) {
            throw new IllegalStateException("OmniSharpProjectComponent not found for project: " + project.getName());
        }
        return component;
    }
}