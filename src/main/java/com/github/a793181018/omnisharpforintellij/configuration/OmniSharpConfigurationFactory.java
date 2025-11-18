package com.github.a793181018.omnisharpforintellij.configuration;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 配置工厂，用于获取合适的配置实例
 */
public final class OmniSharpConfigurationFactory {
    
    private OmniSharpConfigurationFactory() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 获取配置实例
     * 如果提供了Project，则获取项目级配置，否则获取全局配置
     */
    @NotNull
    public static OmniSharpConfiguration getConfiguration(@Nullable Project project) {
        if (project != null && !project.isDisposed()) {
            OmniSharpConfiguration projectConfig = project.getService(OmniSharpConfiguration.class);
            if (projectConfig != null) {
                return projectConfig;
            }
        }
        // 如果没有项目或项目已被销毁，返回全局配置
        return OmniSharpSettings.getInstance();
    }
    
    /**
     * 获取项目级配置
     */
    @NotNull
    public static OmniSharpConfiguration getProjectConfiguration(@NotNull Project project) {
        OmniSharpConfiguration config = project.getService(OmniSharpConfiguration.class);
        if (config != null) {
            return config;
        }
        // 如果项目配置不存在，返回全局配置作为备选
        return OmniSharpSettings.getInstance();
    }
    
    /**
     * 获取全局配置
     */
    @NotNull
    public static OmniSharpConfiguration getGlobalConfiguration() {
        return OmniSharpSettings.getInstance();
    }
    
    /**
     * 创建配置的副本
     */
    @NotNull
    public static OmniSharpConfiguration createConfigurationCopy(@NotNull OmniSharpConfiguration source) {
        return new ConfigurationCopy(source);
    }
    
    /**
     * 配置副本类，用于创建配置的临时副本
     */
    private static class ConfigurationCopy implements OmniSharpConfiguration {
        private String serverPath;
        private String serverArguments;
        private int serverTimeoutMs;
        private int codeCompletionMaxResults;
        private boolean autoStartServer;
        private boolean debugMode;
        
        public ConfigurationCopy(@NotNull OmniSharpConfiguration source) {
            this.serverPath = source.getServerPath();
            this.serverArguments = source.getServerArguments();
            this.serverTimeoutMs = source.getServerTimeoutMs();
            this.codeCompletionMaxResults = source.getCodeCompletionMaxResults();
            this.autoStartServer = source.isAutoStartServer();
            this.debugMode = source.isDebugMode();
        }
        
        @Nullable
        @Override
        public String getServerPath() {
            return serverPath;
        }
        
        @Override
        public void setServerPath(@NotNull String serverPath) {
            this.serverPath = serverPath;
        }
        
        @NotNull
        @Override
        public String getServerArguments() {
            return serverArguments;
        }
        
        @Override
        public void setServerArguments(@NotNull String arguments) {
            this.serverArguments = arguments;
        }
        
        @Override
        public int getServerTimeoutMs() {
            return serverTimeoutMs;
        }
        
        @Override
        public void setServerTimeoutMs(int timeoutMs) {
            this.serverTimeoutMs = timeoutMs;
        }
        
        @Override
        public int getCodeCompletionMaxResults() {
            return codeCompletionMaxResults;
        }
        
        @Override
        public void setCodeCompletionMaxResults(int maxResults) {
            this.codeCompletionMaxResults = maxResults;
        }
        
        @Override
        public boolean isAutoStartServer() {
            return autoStartServer;
        }
        
        @Override
        public void setAutoStartServer(boolean autoStart) {
            this.autoStartServer = autoStart;
        }
        
        @Override
        public boolean isDebugMode() {
            return debugMode;
        }
        
        @Override
        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
        
        @Override
        public void save() {
            // 副本不需要保存
        }
        
        @Override
        public void resetToDefaults() {
            // 重置为默认值
            this.serverPath = null;
            this.serverArguments = "-s . --host stdio --encoding utf-8";
            this.serverTimeoutMs = 30000;
            this.codeCompletionMaxResults = 50;
            this.autoStartServer = true;
            this.debugMode = false;
        }
    }
}