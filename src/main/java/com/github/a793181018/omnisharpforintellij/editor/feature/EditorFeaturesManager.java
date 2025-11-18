package com.github.a793181018.omnisharpforintellij.editor.feature;

import com.github.a793181018.omnisharpforintellij.editor.common.EditorFeatureProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBus;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * EditorFeaturesManager是整个编辑器功能集成的核心管理组件，
 * 负责协调各功能模块的初始化、配置和生命周期管理。
 */
@Service(Service.Level.PROJECT)
public final class EditorFeaturesManager implements Disposable {
    private final Project project;
    private final MessageBus messageBus;
    private final Map<String, EditorFeatureProvider> featureProviders;
    private boolean initialized = false;

    /**
     * 创建EditorFeaturesManager实例
     * @param project IntelliJ项目实例
     * @param messageBus IntelliJ消息总线
     * @param sessionManager OmniSharp会话管理器
     */
    public EditorFeaturesManager(
            @NotNull Project project,
            @NotNull MessageBus messageBus) {
        this.project = project;
        this.messageBus = messageBus;
        this.featureProviders = new ConcurrentHashMap<>();
    }

    /**
     * 初始化EditorFeaturesManager
     */
    public synchronized void initialize() {
        if (initialized) {
            return;
        }

        // 初始化所有已注册的功能提供者
        for (EditorFeatureProvider provider : featureProviders.values()) {
            provider.initialize(project);
        }

        initialized = true;
    }

    /**
     * 注册一个功能提供者
     * @param id 功能提供者的唯一标识符
     * @param provider 功能提供者实例
     */
    public synchronized void registerFeatureProvider(@NotNull String id, @NotNull EditorFeatureProvider provider) {
        featureProviders.put(id, provider);
        
        // 如果已经初始化，则立即初始化新注册的提供者
        if (initialized) {
            provider.initialize(project);
        }
    }

    /**
     * 注销一个功能提供者
     * @param id 功能提供者的唯一标识符
     */
    public synchronized void unregisterFeatureProvider(@NotNull String id) {
        EditorFeatureProvider provider = featureProviders.remove(id);
        if (provider != null) {
            provider.dispose();
        }
    }

    /**
     * 获取一个功能提供者
     * @param id 功能提供者的唯一标识符
     * @return 功能提供者实例，如果不存在则返回null
     */
    public EditorFeatureProvider getFeatureProvider(@NotNull String id) {
        return featureProviders.get(id);
    }

    /**
     * 获取所有功能提供者
     * @return 功能提供者实例的集合
     */
    public Collection<EditorFeatureProvider> getAllFeatureProviders() {
        return Collections.unmodifiableCollection(featureProviders.values());
    }

    /**
     * 配置指定的功能
     * @param featureId 功能ID
     * @param config 配置参数
     */
    public void configureFeature(@NotNull String featureId, @NotNull Map<String, Object> config) {
        EditorFeatureProvider provider = getFeatureProvider(featureId);
        if (provider != null) {
            provider.configure(config);
        }
    }

    /**
     * 启用指定的功能
     * @param featureId 功能ID
     */
    public void enableFeature(@NotNull String featureId) {
        EditorFeatureProvider provider = getFeatureProvider(featureId);
        if (provider != null) {
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", true);
            provider.configure(config);
        }
    }

    /**
     * 禁用指定的功能
     * @param featureId 功能ID
     */
    public void disableFeature(@NotNull String featureId) {
        EditorFeatureProvider provider = getFeatureProvider(featureId);
        if (provider != null) {
            Map<String, Object> config = new HashMap<>();
            config.put("enabled", false);
            provider.configure(config);
        }
    }

    /**
     * 检查指定功能是否已启用
     * @param featureId 功能ID
     * @return 如果功能已启用则返回true
     */
    public boolean isFeatureEnabled(@NotNull String featureId) {
        EditorFeatureProvider provider = getFeatureProvider(featureId);
        return provider != null && provider.isEnabled();
    }

    /**
     * 重新初始化所有功能
     */
    public synchronized void reinitialize() {
        // 先释放所有资源
        disposeFeatureProviders();
        
        // 重新初始化
        initialized = false;
        initialize();
    }

    /**
     * 释放所有功能提供者的资源
     */
    private void disposeFeatureProviders() {
        for (EditorFeatureProvider provider : featureProviders.values()) {
            provider.dispose();
        }
    }

    @Override
    public void dispose() {
        disposeFeatureProviders();
        featureProviders.clear();
        initialized = false;
    }

    /**
     * 获取项目实例
     * @return 项目实例
     */
    public Project getProject() {
        return project;
    }

    // 会话管理相关功能可以在需要时添加

    /**
     * 获取消息总线
     * @return 消息总线实例
     */
    public MessageBus getMessageBus() {
        return messageBus;
    }

    /**
     * 检查是否已初始化
     * @return 如果已初始化则返回true
     */
    public boolean isInitialized() {
        return initialized;
    }
}