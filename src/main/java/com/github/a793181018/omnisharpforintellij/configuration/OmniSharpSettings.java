package com.github.a793181018.omnisharpforintellij.configuration;

import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 全局OmniSharp设置，应用级别的配置
 */
@Service(Service.Level.APP)
@State(name = "OmniSharpSettings", storages = @Storage("omniSharpSettings.xml"))
public final class OmniSharpSettings implements OmniSharpConfiguration, PersistentStateComponent<OmniSharpSettings.State> {
    
    private static OmniSharpSettings instance;
    private State state = new State();
    
    public OmniSharpSettings() {
        instance = this;
        resetToDefaults();
    }
    
    @NotNull
    @Override
    public OmniSharpSettings.State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull OmniSharpSettings.State state) {
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
        // IntelliJ会自动处理持久化
    }
    
    @Override
    public void resetToDefaults() {
        // 设置应用级别的默认配置
        state.serverPath = null;
        state.serverArguments = "-s . --host stdio --encoding utf-8";
        state.serverTimeoutMs = 30000;
        state.codeCompletionMaxResults = 50;
        state.autoStartServer = true;
        state.debugMode = false;
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
     * 获取全局设置实例
     */
    @NotNull
    public static OmniSharpSettings getInstance() {
        if (instance == null) {
            // 如果实例为null，尝试通过服务管理器获取
            instance = com.intellij.openapi.application.ApplicationManager.getApplication().getService(OmniSharpSettings.class);
        }
        return instance;
    }
}