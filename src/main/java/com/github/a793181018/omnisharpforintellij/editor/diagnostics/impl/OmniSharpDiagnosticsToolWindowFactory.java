package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import org.jetbrains.annotations.NotNull;
import java.util.List;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp诊断工具窗口工厂
 * 创建和管理诊断工具窗口
 */
public class OmniSharpDiagnosticsToolWindowFactory implements ToolWindowFactory, Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsToolWindowFactory.class);
    
    private Project project;
    private OmniSharpDiagnosticsProblemsView problemsView;
    private ToolWindow toolWindow;
    
    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        this.project = project;
        this.toolWindow = toolWindow;
        
        LOG.info("Creating OmniSharp diagnostics tool window for project: " + project.getName());
        
        // 创建问题视图
        problemsView = new OmniSharpDiagnosticsProblemsView(project);
        
        // 创建内容面板
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true, true);
        panel.setContent(problemsView.getMainPanel());
        
        // 添加到工具窗口
        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "OmniSharp Diagnostics", false);
        toolWindow.getContentManager().addContent(content);
        
        // 设置工具窗口属性
        toolWindow.setStripeTitle("OmniSharp Diagnostics");
        toolWindow.setAvailable(true);
        toolWindow.setToHideOnEmptyContent(false);
        
        // 注册监听器
        setupListeners();
        
        LOG.info("OmniSharp diagnostics tool window created successfully");
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 监听文件编辑器事件
        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (isCSharpFile(file)) {
                    LOG.debug("C# file opened: " + file.getPath());
                    // 可以在这里触发诊断更新
                }
            }
            
            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (isCSharpFile(file)) {
                    LOG.debug("C# file closed: " + file.getPath());
                    // 可以在这里清理诊断缓存
                }
            }
        });
        
        // 监听诊断服务事件
        OmniSharpDiagnosticsService diagnosticsService = project.getService(OmniSharpDiagnosticsService.class);
        if (diagnosticsService != null) {
            diagnosticsService.addGlobalDiagnosticListener(new OmniSharpDiagnosticsService.DiagnosticListener() {
                @Override
                public void onDiagnosticsUpdated(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (toolWindow != null && !toolWindow.isVisible() && !diagnostics.isEmpty()) {
                            // 如果有新的诊断，可以显示工具窗口
                            toolWindow.show(null);
                        }
                    });
                }
            });
        }
    }
    
    /**
     * 检查是否为C#文件
     */
    private boolean isCSharpFile(@NotNull VirtualFile file) {
        return "cs".equalsIgnoreCase(file.getExtension());
    }
    
    /**
     * 获取工具窗口
     */
    @NotNull
    public ToolWindow getToolWindow() {
        return toolWindow;
    }
    
    /**
     * 获取问题视图
     */
    @NotNull
    public OmniSharpDiagnosticsProblemsView getProblemsView() {
        return problemsView;
    }
    
    /**
     * 刷新诊断
     */
    public CompletableFuture<Void> refreshDiagnostics() {
        if (problemsView != null) {
            // 这里可以调用问题视图的刷新方法
            return CompletableFuture.completedFuture(null);
        }
        return CompletableFuture.completedFuture(null);
    }
    
    /**
     * 显示工具窗口
     */
    public void showToolWindow() {
        if (toolWindow != null) {
            toolWindow.show(null);
        }
    }
    
    /**
     * 隐藏工具窗口
     */
    public void hideToolWindow() {
        if (toolWindow != null) {
            toolWindow.hide(null);
        }
    }
    
    /**
     * 检查工具窗口是否可见
     */
    public boolean isToolWindowVisible() {
        return toolWindow != null && toolWindow.isVisible();
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OmniSharpDiagnosticsToolWindowFactory");
        
        if (problemsView != null) {
            Disposer.dispose(problemsView);
            problemsView = null;
        }
        
        project = null;
        toolWindow = null;
    }
    
    /**
     * 获取实例
     */
    @NotNull
    public static OmniSharpDiagnosticsToolWindowFactory getInstance(@NotNull Project project) {
        ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow("OmniSharp Diagnostics");
        if (toolWindow != null) {
            // 这里可以通过工具窗口获取工厂实例
            // 实际实现可能需要通过服务或其他方式获取
        }
        
        // 如果没有找到现有实例，创建新的
        return new OmniSharpDiagnosticsToolWindowFactory();
    }
    
    /**
     * 注册工具窗口
     */
    public static void registerToolWindow(@NotNull Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        ToolWindow toolWindow = toolWindowManager.getToolWindow("OmniSharp Diagnostics");
        
        if (toolWindow == null) {
            // 工具窗口不存在，需要通过plugin.xml注册
            LOG.info("OmniSharp Diagnostics tool window not found, it should be registered in plugin.xml");
        } else {
            LOG.info("OmniSharp Diagnostics tool window already registered");
        }
    }
    
    /**
     * 工具窗口是否应该可用
     */
    @Override
    public boolean shouldBeAvailable(@NotNull Project project) {
        // 检查项目是否包含C#文件
        return hasCSharpFiles(project);
    }
    
    /**
     * 检查项目是否包含C#文件
     */
    private boolean hasCSharpFiles(@NotNull Project project) {
        // 这里可以实现检查项目是否包含C#文件的逻辑
        // 简化实现，总是返回true
        return true;
    }
    
    /**
     * 初始化工具窗口
     */
    public static void initialize(@NotNull Project project) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
                ToolWindow toolWindow = toolWindowManager.getToolWindow("OmniSharp Diagnostics");
                
                if (toolWindow != null) {
                    LOG.info("OmniSharp Diagnostics tool window initialized");
                    
                    // 可以在这里进行额外的初始化
                    // 例如：设置图标、标题等
                    
                } else {
                    LOG.warn("OmniSharp Diagnostics tool window not found");
                }
            } catch (Exception e) {
                LOG.error("Failed to initialize OmniSharp Diagnostics tool window", e);
            }
        });
    }
}