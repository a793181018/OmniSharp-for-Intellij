package com.github.a793181018.omnisharpforintellij.configuration;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Paths;

/**
 * OmniSharp配置实现类，使用IntelliJ的持久化机制
 */
@Service(Service.Level.PROJECT)
@State(name = "OmniSharpConfiguration", storages = @Storage("omniSharp.xml"))
public final class OmniSharpConfigurationImpl implements OmniSharpConfiguration, PersistentStateComponent<OmniSharpConfigurationImpl.State> {
    
    private State state = new State();
    private final Project project;
    
    public OmniSharpConfigurationImpl(@NotNull Project project) {
        this.project = project;
        // 初始化默认配置
        resetToDefaults();
    }
    
    @NotNull
    @Override
    public OmniSharpConfigurationImpl.State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull OmniSharpConfigurationImpl.State state) {
        XmlSerializerUtil.copyBean(state, this.state);
    }
    
    @Nullable
    @Override
    public String getServerPath() {
        return state.serverPath;
    }
    
    @Override
    public void setServerPath(@NotNull String serverPath) {
        state.serverPath = serverPath;
    }
    
    @NotNull
    @Override
    public String getServerArguments() {
        return state.serverArguments;
    }
    
    @Override
    public void setServerArguments(@NotNull String arguments) {
        state.serverArguments = arguments;
    }
    
    @Override
    public int getServerTimeoutMs() {
        return state.serverTimeoutMs;
    }
    
    @Override
    public void setServerTimeoutMs(int timeoutMs) {
        state.serverTimeoutMs = timeoutMs;
    }
    
    @Override
    public int getCodeCompletionMaxResults() {
        return state.codeCompletionMaxResults;
    }
    
    @Override
    public void setCodeCompletionMaxResults(int maxResults) {
        state.codeCompletionMaxResults = maxResults;
    }
    
    @Override
    public boolean isAutoStartServer() {
        return state.autoStartServer;
    }
    
    @Override
    public void setAutoStartServer(boolean autoStart) {
        state.autoStartServer = autoStart;
    }
    
    @Override
    public boolean isDebugMode() {
        return state.debugMode;
    }
    
    @Override
    public void setDebugMode(boolean debugMode) {
        state.debugMode = debugMode;
    }
    
    @Override
    public void save() {
        // IntelliJ会自动处理持久化，这里可以添加额外的保存逻辑
        // 例如验证配置有效性等
        validateConfiguration();
    }
    
    @Override
    public void resetToDefaults() {
        // 设置默认配置
        state.serverPath = null; // 留空，让用户配置
        state.serverArguments = "-s . --host stdio --encoding utf-8";
        state.serverTimeoutMs = 30000; // 默认30秒
        state.codeCompletionMaxResults = 50;
        state.autoStartServer = true;
        state.debugMode = false;
    }
    
    /**
     * 验证配置有效性
     */
    private void validateConfiguration() {
        // 验证服务器路径
        if (state.serverPath != null && !state.serverPath.isEmpty()) {
            File serverFile = new File(state.serverPath);
            if (!serverFile.exists() || !serverFile.canExecute()) {
                // 可以在这里添加日志或通知
                // 但不要抛出异常，让用户有机会修复配置
            }
        }
        
        // 验证超时时间
        if (state.serverTimeoutMs < 1000) {
            state.serverTimeoutMs = 1000;
        }
        
        // 验证最大结果数
        if (state.codeCompletionMaxResults < 1) {
            state.codeCompletionMaxResults = 1;
        } else if (state.codeCompletionMaxResults > 1000) {
            state.codeCompletionMaxResults = 1000;
        }
    }
    
    /**
     * 状态类，用于持久化
     */
    public static class State {
        public String serverPath = null;
        public String serverArguments = "-s . --host stdio --encoding utf-8";
        public int serverTimeoutMs = 30000;
        public int codeCompletionMaxResults = 50;
        public boolean autoStartServer = true;
        public boolean debugMode = false;
    }
    
    /**
     * 获取项目级别的配置实例
     */
    @NotNull
    public static OmniSharpConfiguration getInstance(@NotNull Project project) {
        return project.getService(OmniSharpConfiguration.class);
    }
}