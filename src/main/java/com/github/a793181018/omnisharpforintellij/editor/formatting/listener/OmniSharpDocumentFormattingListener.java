/**
 * OmniSharp保存时自动格式化监听器
 * 在文件保存时自动执行代码格式化
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting.listener;

import com.github.a793181018.omnisharpforintellij.editor.formatting.OmniSharpFormattingService;
import com.github.a793181018.omnisharpforintellij.editor.formatting.OmniSharpFormattingSettings;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class OmniSharpDocumentFormattingListener implements FileDocumentManagerListener {
    private static final Logger LOG = Logger.getInstance(OmniSharpDocumentFormattingListener.class);
    private static final long FORMATTING_TIMEOUT_SECONDS = 3;
    
    @Override
    public void beforeDocumentSaving(@NotNull Document document) {
        // 获取当前项目
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        if (projects.length == 0) {
            return;
        }
        
        Project project = projects[0]; // 获取第一个打开的项目
        if (project.isDisposed()) {
            return;
        }
        
        // 获取格式化设置
        OmniSharpFormattingSettings settings = OmniSharpFormattingSettings.getInstance(project);
        if (!settings.isFormatOnSaveEnabled()) {
            return;
        }
        
        // 获取文件
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null || !"cs".equals(file.getExtension())) {
            return;
        }
        
        // 获取格式化服务
        OmniSharpFormattingService formattingService = project.getService(OmniSharpFormattingService.class);
        if (formattingService == null) {
            LOG.warn("Formatting service not available for project: " + project.getName());
            return;
        }
        
        // 检查是否应该跳过格式化
        if (formattingService.shouldSkipFormatting(file)) {
            LOG.debug("Skipping formatting for file: " + file.getPath());
            return;
        }
        
        // 执行格式化
        try {
            String formattedContent = formattingService.formatDocument(file)
                .get(FORMATTING_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            
            if (formattedContent != null && !formattedContent.isEmpty()) {
                // 应用格式化内容
                ApplicationManager.getApplication().invokeLater(() -> {
                    try {
                        formattingService.applyFormatting(document, formattedContent);
                        LOG.debug("Applied formatting to file: " + file.getPath());
                    } catch (Exception e) {
                        LOG.warn("Failed to apply formatting to file: " + file.getPath(), e);
                    }
                });
            }
            
        } catch (Exception ex) {
            LOG.warn("Failed to format file on save: " + file.getPath(), ex);
        }
    }
    
    @Override
    public void beforeAllDocumentsSaving() {
        // 在所有文档保存前的操作
    }
    
    @Override
    public void unsavedDocumentsDropped() {
        // 当未保存的文档被丢弃时的操作
    }
    
    /**
     * 获取指定文档对应的项目
     * @param document 文档
     * @return 项目，如果找不到则返回null
     */
    private Project getProjectForDocument(Document document) {
        VirtualFile file = FileDocumentManager.getInstance().getFile(document);
        if (file == null) {
            return null;
        }
        
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        for (Project project : projects) {
            if (!project.isDisposed() && project.getBasePath() != null && 
                file.getPath().startsWith(project.getBasePath())) {
                return project;
            }
        }
        
        return projects.length > 0 ? projects[0] : null;
    }
}