package com.github.a793181018.omnisharpforintellij.editor.common;

import com.github.a793181018.omnisharpforintellij.editor.feature.EditorFeaturesManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * 编辑器功能提供者接口，所有OmniSharp编辑器功能模块都应实现此接口。
 * 该接口定义了功能模块的生命周期管理方法。
 */
public interface EditorFeatureProvider {
    /**
     * 初始化功能提供者
     * @param project IntelliJ项目实例
     */
    void initialize(@NotNull Project project);
    
    /**
     * 是否启用该功能
     * @return 如果功能已启用则返回true
     */
    boolean isEnabled();
    
    /**
     * 配置功能
     * @param config 配置参数映射
     */
    void configure(@NotNull Map<String, Object> config);
    
    /**
     * 清理资源
     */
    void dispose();
    
    /**
     * 获取功能的唯一标识符
     * @return 功能ID
     */
    @NotNull
    String getFeatureId();
    
    /**
     * 获取功能的名称
     * @return 功能名称
     */
    @NotNull
    String getFeatureName();
    
    /**
     * 获取功能的描述
     * @return 功能描述
     */
    @NotNull
    String getFeatureDescription();
    
    /**
     * 通知功能管理器已注册
     * @param manager EditorFeaturesManager实例
     */
    default void onRegistered(@NotNull EditorFeaturesManager manager) {
        // 默认实现为空，子类可以覆盖以提供特定行为
    }
    
    /**
     * 通知功能管理器已注销
     * @param manager EditorFeaturesManager实例
     */
    default void onUnregistered(@NotNull EditorFeaturesManager manager) {
        // 默认实现为空，子类可以覆盖以提供特定行为
    }
}