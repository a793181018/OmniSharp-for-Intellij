package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.util.Alarm;
import com.intellij.openapi.fileEditor.FileEditorManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 文档诊断监听器
 * 监听文档变化并触发诊断更新
 */
public class OmniSharpDocumentDiagnosticsListener implements DocumentListener, Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpDocumentDiagnosticsListener.class);
    
    private final Project project;
    private final OmniSharpDiagnosticsService diagnosticsService;
    private final Alarm updateAlarm;
    
    // 文档更新防抖机制
    private final Map<Document, Long> lastUpdateTime = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_TIME_MS = 500;
    
    // 待更新的文档队列
    private final Set<Document> pendingUpdates = ConcurrentHashMap.newKeySet();
    
    // 是否启用实时诊断
    private boolean realTimeDiagnosticsEnabled = true;
    
    // 文档监听器注册状态
    private boolean isListening = false;
    
    public OmniSharpDocumentDiagnosticsListener(@NotNull Project project, @NotNull OmniSharpDiagnosticsService diagnosticsService) {
        this.project = project;
        this.diagnosticsService = diagnosticsService;
        this.updateAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);
        
        // 初始化配置
        updateRealTimeDiagnosticsEnabled();
        
        LOG.info("OmniSharpDocumentDiagnosticsListener initialized for project: " + project.getName());
    }
    
    /**
     * 启动文档监听
     */
    public void startListening() {
        if (isListening) {
            LOG.warn("Document listener is already listening");
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            EditorFactory.getInstance().getEventMulticaster().addDocumentListener(this, this);
            isListening = true;
            LOG.info("Started document diagnostics listening");
        });
    }
    
    /**
     * 停止文档监听
     */
    public void stopListening() {
        if (!isListening) {
            LOG.warn("Document listener is not listening");
            return;
        }
        
        ApplicationManager.getApplication().invokeLater(() -> {
            EditorFactory.getInstance().getEventMulticaster().removeDocumentListener(this);
            isListening = false;
            LOG.info("Stopped document diagnostics listening");
        });
    }
    
    /**
     * 更新实时诊断启用状态
     */
    public void updateRealTimeDiagnosticsEnabled() {
        this.realTimeDiagnosticsEnabled = diagnosticsService.getConfig().isRealTimeDiagnosticsEnabled();
        
        if (realTimeDiagnosticsEnabled) {
            startListening();
        } else {
            stopListening();
        }
        
        LOG.info("Real-time diagnostics enabled: " + realTimeDiagnosticsEnabled);
    }
    
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
        if (!realTimeDiagnosticsEnabled) {
            return;
        }
        
        Document document = event.getDocument();
        
        // 检查是否属于当前项目
        if (!isDocumentInProject(document)) {
            return;
        }
        
        // 防抖检查
        Long lastUpdate = lastUpdateTime.get(document);
        long currentTime = System.currentTimeMillis();
        if (lastUpdate != null && (currentTime - lastUpdate) < DEBOUNCE_TIME_MS) {
            LOG.debug("Skipping diagnostic update due to debounce for document");
            return;
        }
        
        lastUpdateTime.put(document, currentTime);
        
        // 添加到待更新队列
        pendingUpdates.add(document);
        
        // 延迟执行诊断更新
        scheduleDiagnosticUpdate(document);
    }
    
    /**
     * 调度诊断更新
     */
    private void scheduleDiagnosticUpdate(@NotNull Document document) {
        updateAlarm.cancelAllRequests();
        
        updateAlarm.addRequest(() -> {
            if (pendingUpdates.isEmpty()) {
                return;
            }
            
            // 批量处理待更新的文档
            Set<Document> documentsToUpdate = new HashSet<>(pendingUpdates);
            pendingUpdates.clear();
            
            for (Document doc : documentsToUpdate) {
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(doc);
                if (virtualFile != null && virtualFile.isValid()) {
                    updateDocumentDiagnostics(doc);
                }
            }
        }, DEBOUNCE_TIME_MS);
    }
    
    /**
     * 更新文档诊断
     */
    private void updateDocumentDiagnostics(@NotNull Document document) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
                if (virtualFile == null || !virtualFile.isValid()) {
                    LOG.debug("No virtual file found for document or file is invalid");
                    return;
                }
                
                // 检查文件类型
                if (!isCSharpFile(virtualFile)) {
                    LOG.debug("File is not a C# file: " + virtualFile.getPath());
                    return;
                }
                
                // 检查是否在分析范围内
                if (!shouldAnalyzeFile(virtualFile)) {
                    LOG.debug("File should not be analyzed: " + virtualFile.getPath());
                    return;
                }
                
                LOG.debug("Updating diagnostics for document: " + virtualFile.getPath());
                
                // 触发诊断更新
                diagnosticsService.updateDiagnostics(virtualFile)
                    .whenComplete((diagnostics, throwable) -> {
                        if (throwable != null) {
                            LOG.error("Failed to update diagnostics for document: " + virtualFile.getPath(), throwable);
                        } else {
                            LOG.debug("Successfully updated diagnostics for document: " + virtualFile.getPath() + ", count: " + diagnostics.size());
                        }
                    });
                
            } catch (Exception e) {
                LOG.error("Error updating document diagnostics", e);
            }
        });
    }
    
    /**
     * 检查文档是否属于当前项目
     */
    private boolean isDocumentInProject(@NotNull Document document) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return false;
        }
        
        return isFileInProject(virtualFile);
    }
    
    /**
     * 检查文件是否属于当前项目
     */
    private boolean isFileInProject(@NotNull VirtualFile file) {
        Project fileProject = ProjectUtil.guessProjectForFile(file);
        return fileProject != null && fileProject.equals(project);
    }
    
    /**
     * 检查是否为C#文件
     */
    private boolean isCSharpFile(@NotNull VirtualFile file) {
        String extension = file.getExtension();
        return "cs".equalsIgnoreCase(extension);
    }
    
    /**
     * 检查是否应该分析文件
     */
    private boolean shouldAnalyzeFile(@NotNull VirtualFile file) {
        // 检查是否只分析打开的文件
        if (diagnosticsService.getConfig().isAnalyzeOpenFilesOnly()) {
            return isFileOpen(file);
        }
        
        return true;
    }
    
    /**
     * 检查文件是否打开
     */
    private boolean isFileOpen(@NotNull VirtualFile file) {
        return FileEditorManager.getInstance(project).isFileOpen(file);
    }
    
    /**
     * 手动触发文档诊断更新
     */
    public void triggerDocumentDiagnostics(@NotNull Document document) {
        // 检查文档是否有效 - 使用VirtualFile进行验证
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null || !virtualFile.isValid()) {
            LOG.warn("Cannot trigger diagnostics for invalid document");
            return;
        }
        
        LOG.debug("Manually triggering document diagnostics");
        updateDocumentDiagnostics(document);
    }
    
    /**
     * 手动触发文件诊断更新
     */
    public void triggerFileDiagnostics(@NotNull VirtualFile file) {
        if (!file.isValid()) {
            LOG.warn("Cannot trigger diagnostics for invalid file: " + file.getPath());
            return;
        }
        
        Document document = FileDocumentManager.getInstance().getDocument(file);
        if (document != null) {
            triggerDocumentDiagnostics(document);
        } else {
            LOG.debug("No document found for file: " + file.getPath());
            // 直接触发文件诊断
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                diagnosticsService.updateDiagnostics(file)
                    .whenComplete((diagnostics, throwable) -> {
                        if (throwable != null) {
                            LOG.error("Failed to update diagnostics for file: " + file.getPath(), throwable);
                        }
                    });
            });
        }
    }
    
    /**
     * 批量触发多个文档的诊断更新
     */
    public void triggerDocumentsDiagnostics(@NotNull List<Document> documents) {
        LOG.debug("Triggering diagnostics for " + documents.size() + " documents");
        
        for (Document document : documents) {
            VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
            if (virtualFile != null && virtualFile.isValid()) {
                pendingUpdates.add(document);
            }
        }
        
        // 批量调度更新
        if (!pendingUpdates.isEmpty()) {
            scheduleDiagnosticUpdate(documents.get(0));
        }
    }
    
    /**
     * 批量触发多个文件的诊断更新
     */
    public void triggerFilesDiagnostics(@NotNull List<VirtualFile> files) {
        LOG.debug("Triggering diagnostics for " + files.size() + " files");
        
        List<Document> documents = new ArrayList<>();
        for (VirtualFile file : files) {
            if (file.isValid() && isCSharpFile(file)) {
                Document document = FileDocumentManager.getInstance().getDocument(file);
                if (document != null) {
                    documents.add(document);
                }
            }
        }
        
        if (!documents.isEmpty()) {
            triggerDocumentsDiagnostics(documents);
        }
    }
    
    /**
     * 清除文档的更新状态
     */
    public void clearDocumentUpdateState(@NotNull Document document) {
        lastUpdateTime.remove(document);
        pendingUpdates.remove(document);
        LOG.debug("Cleared update state for document");
    }
    
    /**
     * 清除所有更新状态
     */
    public void clearAllUpdateStates() {
        lastUpdateTime.clear();
        pendingUpdates.clear();
        LOG.info("Cleared all document update states");
    }
    
    /**
     * 获取监听状态
     */
    public boolean isListening() {
        return isListening;
    }
    
    /**
     * 获取待更新文档数量
     */
    public int getPendingUpdatesCount() {
        return pendingUpdates.size();
    }
    
    /**
     * 获取监听统计信息
     */
    @NotNull
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("isListening", isListening);
        stats.put("realTimeDiagnosticsEnabled", realTimeDiagnosticsEnabled);
        stats.put("pendingUpdatesCount", pendingUpdates.size());
        stats.put("lastUpdateTimeCount", lastUpdateTime.size());
        return stats;
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OmniSharpDocumentDiagnosticsListener");
        
        // 停止监听
        if (isListening) {
            stopListening();
        }
        
        // 清除状态
        clearAllUpdateStates();
        
        // 取消所有调度请求
        updateAlarm.cancelAllRequests();
    }
}