package com.github.a793181018.omnisharpforintellij;

import com.github.a793181018.omnisharpforintellij.configuration.OmniSharpSettings;
import com.github.a793181018.omnisharpforintellij.server.OmniSharpServerManager;
import com.github.a793181018.omnisharpforintellij.session.OmniSharpSessionManager;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * OmniSharp插件的主入口类
 */
public final class OmniSharpPlugin implements ApplicationComponent {
    
    private static final Logger LOGGER = Logger.getInstance(OmniSharpPlugin.class);
    public static final String PLUGIN_NAME = "OmniSharp for IntelliJ";
    public static final String PLUGIN_ID = "com.github.a793181018.omnisharpforintellij";
    
    private static OmniSharpPlugin instance;
    
    public OmniSharpPlugin() {
        instance = this;
    }
    
    @NotNull
    @Override
    public String getComponentName() {
        return PLUGIN_NAME;
    }
    
    @Override
    public void initComponent() {
        LOGGER.info("Initializing " + PLUGIN_NAME);
        
        try {
            // 初始化插件组件
            Application application = ApplicationManager.getApplication();
            
            // 确保全局设置已初始化
            OmniSharpSettings settings = OmniSharpSettings.getInstance();
            if (settings.isDebugMode()) {
                LOGGER.info("Debug mode enabled");
            }
            
            // 注册全局服务
            // 注意：我们已经在各个服务类中使用了@Service注解，IntelliJ会自动管理它们
            
            LOGGER.info("" + PLUGIN_NAME + " initialized successfully");
        } catch (Exception e) {
            LOGGER.error("Failed to initialize " + PLUGIN_NAME, e);
        }
    }
    
    @Override
    public void disposeComponent() {
        LOGGER.info("Disposing " + PLUGIN_NAME);
        
        try {
            // 清理插件资源
            // 这里可以添加需要在应用关闭时执行的清理操作
            
            LOGGER.info("" + PLUGIN_NAME + " disposed successfully");
        } catch (Exception e) {
            LOGGER.error("Error during disposal of " + PLUGIN_NAME, e);
        }
        
        instance = null;
    }
    
    /**
     * 获取插件实例
     */
    @NotNull
    public static OmniSharpPlugin getInstance() {
        if (instance == null) {
            instance = ApplicationManager.getApplication().getComponent(OmniSharpPlugin.class);
        }
        return instance;
    }
    
    /**
     * 检查插件是否启用调试模式
     */
    public boolean isDebugMode() {
        return OmniSharpSettings.getInstance().isDebugMode();
    }
    
    /**
     * 记录调试日志（仅在调试模式下）
     */
    public void debug(String message) {
        if (isDebugMode()) {
            LOGGER.info("[DEBUG] " + message);
        }
    }
    
    /**
     * 记录错误日志
     */
    public void error(String message, Throwable throwable) {
        LOGGER.error(message, throwable);
    }
}