package com.github.a793181018.omnisharpforintellij.server.configuration.impl;

import com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OmniSharp服务器配置实现类，使用IntelliJ的持久化机制
 */
@State(
    name = "OmniSharpConfiguration",
    storages = @Storage("omnisharp.xml")
)
public class OmniSharpConfigurationImpl implements 
        IOmniSharpConfiguration, 
        PersistentStateComponent<OmniSharpConfigurationImpl.OmniSharpConfigurationState> {
    
    private final Project project;
    private String serverPath = "";
    private File workingDirectory;
    private List<String> arguments = Arrays.asList("--stdio");
    private long maxStartupWaitTime = 30000L; // 30 seconds
    private boolean autoRestart = true;
    private int maxRestartAttempts = 3;
    private long communicationTimeout = 5000L; // 5 seconds
    
    /**
     * 创建配置实现
     * @param project 项目对象
     */
    public OmniSharpConfigurationImpl(Project project) {
        this.project = project;
        this.workingDirectory = project.getBasePath() != null 
                ? new File(project.getBasePath()) 
                : new File(System.getProperty("user.dir"));
    }
    
    @Override
    public String getServerPath() {
        return serverPath;
    }
    
    @Override
    public void setServerPath(String serverPath) {
        this.serverPath = serverPath;
    }
    
    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }
    
    @Override
    public void setWorkingDirectory(File workingDirectory) {
        this.workingDirectory = workingDirectory;
    }
    
    @Override
    public List<String> getArguments() {
        return arguments;
    }
    
    @Override
    public void setArguments(List<String> arguments) {
        this.arguments = arguments;
    }
    
    @Override
    public long getMaxStartupWaitTime() {
        return maxStartupWaitTime;
    }
    
    @Override
    public void setMaxStartupWaitTime(long maxStartupWaitTime) {
        this.maxStartupWaitTime = maxStartupWaitTime;
    }
    
    @Override
    public boolean isAutoRestart() {
        return autoRestart;
    }
    
    @Override
    public void setAutoRestart(boolean autoRestart) {
        this.autoRestart = autoRestart;
    }
    
    @Override
    public int getMaxRestartAttempts() {
        return maxRestartAttempts;
    }
    
    @Override
    public void setMaxRestartAttempts(int maxRestartAttempts) {
        this.maxRestartAttempts = maxRestartAttempts;
    }
    
    @Override
    public long getCommunicationTimeout() {
        return communicationTimeout;
    }
    
    @Override
    public void setCommunicationTimeout(long communicationTimeout) {
        this.communicationTimeout = communicationTimeout;
    }
    
    @Override
    public ValidationResult validate() {
        // 验证服务器路径
        if (serverPath == null || serverPath.trim().isEmpty()) {
            return ValidationResult.failure("OmniSharp server path is not configured");
        }
        
        File serverFile = new File(serverPath);
        if (!serverFile.exists() || !serverFile.canExecute()) {
            return ValidationResult.failure("OmniSharp server path is invalid or not executable");
        }
        
        // 验证工作目录
        if (workingDirectory == null || !workingDirectory.exists() || !workingDirectory.isDirectory()) {
            return ValidationResult.failure("Working directory is invalid");
        }
        
        // 验证时间设置
        if (maxStartupWaitTime <= 0) {
            return ValidationResult.failure("Max startup wait time must be positive");
        }
        
        if (communicationTimeout <= 0) {
            return ValidationResult.failure("Communication timeout must be positive");
        }
        
        if (maxRestartAttempts < 0) {
            return ValidationResult.failure("Max restart attempts cannot be negative");
        }
        
        return ValidationResult.success();
    }
    
    @Nullable
    @Override
    public OmniSharpConfigurationState getState() {
        OmniSharpConfigurationState state = new OmniSharpConfigurationState();
        state.serverPath = this.serverPath;
        state.workingDirectory = this.workingDirectory != null ? this.workingDirectory.getAbsolutePath() : "";
        state.arguments = new ArrayList<>(this.arguments);
        state.maxStartupWaitTime = this.maxStartupWaitTime;
        state.autoRestart = this.autoRestart;
        state.maxRestartAttempts = this.maxRestartAttempts;
        state.communicationTimeout = this.communicationTimeout;
        return state;
    }
    
    @Override
    public void loadState(@NotNull OmniSharpConfigurationState state) {
        this.serverPath = state.serverPath;
        this.workingDirectory = !state.workingDirectory.isEmpty() 
                ? new File(state.workingDirectory) 
                : project.getBasePath() != null 
                    ? new File(project.getBasePath()) 
                    : new File(System.getProperty("user.dir"));
        this.arguments = new ArrayList<>(state.arguments);
        this.maxStartupWaitTime = state.maxStartupWaitTime;
        this.autoRestart = state.autoRestart;
        this.maxRestartAttempts = state.maxRestartAttempts;
        this.communicationTimeout = state.communicationTimeout;
    }
    
    /**
     * 配置持久化状态类
     */
    public static class OmniSharpConfigurationState {
        public String serverPath = "";
        public String workingDirectory = "";
        public List<String> arguments = Arrays.asList("--stdio");
        public long maxStartupWaitTime = 30000L;
        public boolean autoRestart = true;
        public int maxRestartAttempts = 3;
        public long communicationTimeout = 5000L;
    }
}