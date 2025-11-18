package com.github.a793181018.omnisharpforintellij.editor.completion.registration;

import com.github.a793181018.omnisharpforintellij.editor.completion.contributor.OmniSharpCompletionContributor;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

/**
 * 补全贡献者注册表，负责管理和初始化OmniSharp代码补全相关组件
 */
@Service(Service.Level.PROJECT)
public class CompletionContributorRegistry {
    private static final Logger LOG = Logger.getInstance(CompletionContributorRegistry.class);
    
    private final Project project;
    private boolean isInitialized = false;

    public CompletionContributorRegistry(@NotNull Project project) {
        this.project = project;
    }

    /**
     * 获取CompletionContributorRegistry实例
     */
    @NotNull
    public static CompletionContributorRegistry getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, CompletionContributorRegistry.class);
    }

    /**
     * 初始化补全贡献者组件
     */
    public void initialize() {
        if (isInitialized) {
            return;
        }

        try {
            // 注册补全贡献者（通过plugin.xml自动注册）
            // 这里可以进行一些初始化工作
            LOG.info("OmniSharp completion components initialized for project: " + project.getName());
            isInitialized = true;
        } catch (Exception e) {
            LOG.error("Failed to initialize OmniSharp completion components: " + e.getMessage(), e);
        }
    }

    /**
     * 检查补全功能是否可用
     */
    public boolean isCompletionAvailable() {
        return isInitialized && !project.isDisposed();
    }

    /**
     * 获取补全贡献者信息
     */
    @NotNull
    public String getContributorInfo() {
        if (!isInitialized) {
            return "OmniSharpCompletionContributor not initialized";
        }
        return "OmniSharpCompletionContributor initialized for project: " + project.getName();
    }

    /**
     * 刷新补全贡献者配置
     */
    public void refresh() {
        // 重新初始化补全组件
        LOG.info("Refreshing OmniSharp completion components");
        isInitialized = false;
        initialize();
    }
}