/**
 * OmniSharp代码格式化操作处理器
 * 处理整个文档的格式化请求
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting.action;

import com.github.a793181018.omnisharpforintellij.editor.formatting.OmniSharpFormattingService;
import com.github.a793181018.omnisharpforintellij.editor.formatting.OmniSharpFormattingSettings;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OmniSharpReformatCodeAction extends AnAction {
    private static final String ACTION_NAME = "Reformat Code";
    private static final Duration FORMATTING_TIMEOUT = Duration.ofSeconds(10);
    
    public OmniSharpReformatCodeAction() {
        super(ACTION_NAME);
    }
    
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return;
        }
        
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        
        if (editor == null || psiFile == null) {
            return;
        }
        
        VirtualFile virtualFile = psiFile.getVirtualFile();
        if (virtualFile == null || !"cs".equals(virtualFile.getExtension())) {
            return;
        }
        
        // 获取格式化服务
        OmniSharpFormattingService formattingService = project.getService(OmniSharpFormattingService.class);
        if (formattingService == null) {
            showErrorNotification(project, "Formatting service not available");
            return;
        }
        
        // 检查是否应该跳过格式化
        if (formattingService.shouldSkipFormatting(virtualFile)) {
            LOG.debug("Skipping formatting for file: " + virtualFile.getPath());
            return;
        }
        
        // 显示进度对话框
        ProgressManager.getInstance().runProcessWithProgressSynchronously(() -> {
            try {
                // 执行格式化
                CompletableFuture<String> formattingFuture = formattingService.formatDocument(virtualFile);
                String formattedContent = formattingFuture.get(FORMATTING_TIMEOUT.getSeconds(), TimeUnit.SECONDS);
                
                if (formattedContent == null || formattedContent.isEmpty()) {
                    showWarningNotification(project, "No formatting changes needed");
                    return;
                }
                
                // 应用格式化内容
                ApplicationManager.getApplication().invokeLater(() -> {
                    formattingService.applyFormatting(editor.getDocument(), formattedContent);
                    showSuccessNotification(project, "File formatted successfully");
                });
                
            } catch (Exception ex) {
                LOG.error("Failed to format file: " + virtualFile.getPath(), ex);
                ApplicationManager.getApplication().invokeLater(() -> {
                    showErrorNotification(project, "Failed to format file: " + ex.getMessage());
                });
            }
        }, "Formatting Code", false, project);
    }
    
    @Override
    public void update(AnActionEvent event) {
        Editor editor = event.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = event.getData(CommonDataKeys.PSI_FILE);
        
        boolean enabled = false;
        
        if (editor != null && psiFile != null) {
            VirtualFile virtualFile = psiFile.getVirtualFile();
            if (virtualFile != null && "cs".equals(virtualFile.getExtension())) {
                Project project = event.getProject();
                if (project != null) {
                    OmniSharpFormattingService formattingService = project.getService(OmniSharpFormattingService.class);
                    enabled = formattingService != null && !formattingService.shouldSkipFormatting(virtualFile);
                }
            }
        }
        
        event.getPresentation().setEnabledAndVisible(enabled);
    }
    
    /**
     * 显示成功通知
     */
    private void showSuccessNotification(Project project, String message) {
        Notification notification = new Notification(
            "OmniSharp.Formatting",
            "Formatting Complete",
            message,
            NotificationType.INFORMATION
        );
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * 显示错误通知
     */
    private void showErrorNotification(Project project, String message) {
        Notification notification = new Notification(
            "OmniSharp.Formatting",
            "Formatting Error",
            message,
            NotificationType.ERROR
        );
        Notifications.Bus.notify(notification, project);
    }
    
    /**
     * 显示警告通知
     */
    private void showWarningNotification(Project project, String message) {
        Notification notification = new Notification(
            "OmniSharp.Formatting",
            "Formatting Info",
            message,
            NotificationType.WARNING
        );
        Notifications.Bus.notify(notification, project);
    }
    
    private static final com.intellij.openapi.diagnostic.Logger LOG = com.intellij.openapi.diagnostic.Logger.getInstance(OmniSharpReformatCodeAction.class);
}