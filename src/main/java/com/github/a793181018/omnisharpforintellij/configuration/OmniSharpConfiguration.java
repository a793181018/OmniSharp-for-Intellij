package com.github.a793181018.omnisharpforintellij.configuration;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp配置接口，定义插件配置项
 */
public interface OmniSharpConfiguration {
    
    /**
     * 获取OmniSharp服务器路径
     */
    @Nullable
    String getServerPath();
    
    /**
     * 设置OmniSharp服务器路径
     */
    void setServerPath(@NotNull String serverPath);
    
    /**
     * 获取服务器启动参数
     */
    @NotNull
    String getServerArguments();
    
    /**
     * 设置服务器启动参数
     */
    void setServerArguments(@NotNull String arguments);
    
    /**
     * 获取服务器超时时间（毫秒）
     */
    int getServerTimeoutMs();
    
    /**
     * 设置服务器超时时间（毫秒）
     */
    void setServerTimeoutMs(int timeoutMs);
    
    /**
     * 获取代码补全最大结果数
     */
    int getCodeCompletionMaxResults();
    
    /**
     * 设置代码补全最大结果数
     */
    void setCodeCompletionMaxResults(int maxResults);
    
    /**
     * 是否启用自动启动服务器
     */
    boolean isAutoStartServer();
    
    /**
     * 设置是否自动启动服务器
     */
    void setAutoStartServer(boolean autoStart);
    
    /**
     * 是否启用调试模式
     */
    boolean isDebugMode();
    
    /**
     * 设置是否启用调试模式
     */
    void setDebugMode(boolean debugMode);
    
    /**
     * 保存配置
     */
    void save();
    
    /**
     * 重置为默认配置
     */
    void resetToDefaults();
}