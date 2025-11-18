package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.communicator.message.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.editor.diagnostics.DiagnosticsProvider;
import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * OmniSharp代码诊断服务核心实现类
 * 负责与OmniSharp服务器通信并处理诊断请求
 */
public class OmniSharpDiagnosticsService implements Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsService.class);
    
    private final Project project;
    private final IOmniSharpCommunication communicator;
    private final DiagnosticsCache diagnosticsCache;
    private final VirtualFileManager virtualFileManager;
    private final OmniSharpDiagnosticsConfig config;
    private final OmniSharpDiagnosticsFilter filter;
    
    // 诊断结果监听器
    private final Map<VirtualFile, List<DiagnosticListener>> diagnosticListeners = new ConcurrentHashMap<>();
    
    // 诊断更新防抖机制
    private final Map<VirtualFile, Long> lastDiagnosticUpdateTime = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_TIME_MS = 500;
    
    public OmniSharpDiagnosticsService(@NotNull Project project) {
        this.project = project;
        this.communicator = project.getService(IOmniSharpCommunication.class);
        this.diagnosticsCache = new DiagnosticsCache();
        this.virtualFileManager = VirtualFileManager.getInstance();
        this.config = new OmniSharpDiagnosticsConfig(project);
        this.filter = new OmniSharpDiagnosticsFilter(config);
        
        LOG.info("OmniSharpDiagnosticsService initialized for project: " + project.getName());
    }
    
    /**
     * 获取文件的诊断结果
     * @param file 需要诊断的文件
     * @return 诊断结果列表
     */
    @NotNull
    public Mono<List<Diagnostic>> getDiagnostics(@NotNull VirtualFile file) {
        // 检查缓存
        List<Diagnostic> cachedResult = diagnosticsCache.getDiagnostics(file.getPath());
        if (cachedResult != null) {
            LOG.debug("Returning cached diagnostics for file: " + file.getPath());
            return Mono.just(cachedResult);
        }
        
        // 构建请求
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("FileName", file.getPath());
        arguments.put("WantSemanticDiagnostics", true);
        arguments.put("WantSyntaxDiagnostics", true);
        
        OmniSharpRequest<List<Diagnostic>> request = new OmniSharpRequest<>(
            "/diagnostics",
            arguments,
            (Class<List<Diagnostic>>) (Class<?>) List.class
        );
        
        // 发送请求到OmniSharp服务器
        return communicator.sendRequestReactive(request)
            .map(response -> {
                if (!response.isSuccess()) {
                    LOG.warn("OmniSharp server returned error for diagnostics request: " + response.getMessage());
                    return Collections.<Diagnostic>emptyList();
                }
                
                // 处理响应，转换为Diagnostic对象
                List<Diagnostic> diagnostics = convertResponseToDiagnostics(response);
                
                // 应用过滤
                diagnostics = filter.filterDiagnostics(diagnostics);
                
                // 更新缓存
                diagnosticsCache.putDiagnostics(file.getPath(), diagnostics);
                
                LOG.debug("Retrieved " + diagnostics.size() + " diagnostics for file: " + file.getPath());
                return diagnostics;
            })
            .onErrorResume(throwable -> {
                LOG.error("Failed to get diagnostics for file: " + file.getPath(), throwable);
                return Mono.just(Collections.emptyList());
            });
    }
    
    /**
     * 触发文件的诊断更新
     * @param file 需要更新诊断的文件
     */
    @NotNull
    public CompletableFuture<List<Diagnostic>> updateDiagnostics(@NotNull VirtualFile file) {
        // 防抖检查
        Long lastUpdate = lastDiagnosticUpdateTime.get(file);
        long currentTime = System.currentTimeMillis();
        if (lastUpdate != null && (currentTime - lastUpdate) < DEBOUNCE_TIME_MS) {
            LOG.debug("Skipping diagnostic update due to debounce for file: " + file.getPath());
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        
        lastDiagnosticUpdateTime.put(file, currentTime);
        
        // 清除缓存
        diagnosticsCache.removeDiagnostics(file.getPath());
        
        return getDiagnostics(file)
            .doOnSuccess(diagnostics -> {
                LOG.debug("Diagnostics updated for file: " + file.getPath() + ", count: " + diagnostics.size());
                // 通知监听器
                notifyDiagnosticsUpdated(file, diagnostics);
            })
            .toFuture();
    }
    
    /**
     * 批量更新多个文件的诊断
     * @param files 需要更新诊断的文件列表
     */
    @NotNull
    public CompletableFuture<Map<VirtualFile, List<Diagnostic>>> updateDiagnosticsBatch(@NotNull List<VirtualFile> files) {
        Map<VirtualFile, CompletableFuture<List<Diagnostic>>> futures = new HashMap<>();
        
        for (VirtualFile file : files) {
            futures.put(file, updateDiagnostics(file));
        }
        
        return CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                Map<VirtualFile, List<Diagnostic>> results = new HashMap<>();
                for (Map.Entry<VirtualFile, CompletableFuture<List<Diagnostic>>> entry : futures.entrySet()) {
                    try {
                        results.put(entry.getKey(), entry.getValue().get());
                    } catch (Exception e) {
                        LOG.error("Failed to get diagnostics for file: " + entry.getKey().getPath(), e);
                        results.put(entry.getKey(), Collections.emptyList());
                    }
                }
                return results;
            });
    }
    
    /**
     * 添加诊断监听器
     * @param file 监听的文件
     * @param listener 监听器
     */
    public void addDiagnosticListener(@NotNull VirtualFile file, @NotNull DiagnosticListener listener) {
        diagnosticListeners.computeIfAbsent(file, k -> new CopyOnWriteArrayList<>()).add(listener);
        LOG.debug("Added diagnostic listener for file: " + file.getPath());
    }
    
    /**
     * 添加全局诊断监听器（监听所有文件的诊断更新）
     * @param listener 监听器
     */
    public void addGlobalDiagnosticListener(@NotNull DiagnosticListener listener) {
        // 使用特殊键值表示全局监听器
        diagnosticListeners.computeIfAbsent(null, k -> new CopyOnWriteArrayList<>()).add(listener);
        LOG.debug("Added global diagnostic listener");
    }
    
    /**
     * 移除诊断监听器
     * @param file 监听的文件
     * @param listener 监听器
     */
    public void removeDiagnosticListener(@NotNull VirtualFile file, @NotNull DiagnosticListener listener) {
        List<DiagnosticListener> listeners = diagnosticListeners.get(file);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                diagnosticListeners.remove(file);
            }
        }
        LOG.debug("Removed diagnostic listener for file: " + file.getPath());
    }
    
    /**
     * 移除全局诊断监听器
     * @param listener 监听器
     */
    public void removeGlobalDiagnosticListener(@NotNull DiagnosticListener listener) {
        List<DiagnosticListener> listeners = diagnosticListeners.get(null);
        if (listeners != null) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                diagnosticListeners.remove(null);
            }
        }
        LOG.debug("Removed global diagnostic listener");
    }
    
    /**
     * 清除指定文件的诊断缓存
     * @param file 文件
     */
    public void clearDiagnosticsCache(@NotNull VirtualFile file) {
        diagnosticsCache.removeDiagnostics(file.getPath());
        lastDiagnosticUpdateTime.remove(file);
        LOG.debug("Cleared diagnostics cache for file: " + file.getPath());
    }
    
    /**
     * 清除所有诊断缓存
     */
    public void clearAllDiagnosticsCache() {
        diagnosticsCache.clear();
        lastDiagnosticUpdateTime.clear();
        LOG.info("Cleared all diagnostics cache");
    }
    
    /**
     * 获取诊断配置
     */
    @NotNull
    public OmniSharpDiagnosticsConfig getConfig() {
        return config;
    }
    
    /**
     * 获取诊断过滤器
     */
    @NotNull
    public OmniSharpDiagnosticsFilter getFilter() {
        return filter;
    }
    
    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    /**
     * 将OmniSharp响应转换为诊断列表
     */
    @NotNull
    private List<Diagnostic> convertResponseToDiagnostics(@NotNull com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse<List<Diagnostic>> response) {
        List<Diagnostic> body = response.getBody();
        if (body == null) {
            return Collections.emptyList();
        }
        
        // 过滤掉空值并返回
        return body.stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
    

    
    /**
     * 通知诊断更新
     */
    private void notifyDiagnosticsUpdated(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
        // 通知文件特定的监听器
        List<DiagnosticListener> listeners = diagnosticListeners.get(file);
        if (listeners != null && !listeners.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                for (DiagnosticListener listener : listeners) {
                    try {
                        listener.onDiagnosticsUpdated(file, diagnostics);
                    } catch (Exception e) {
                        LOG.error("Error notifying diagnostic listener", e);
                    }
                }
            });
        }
        
        // 通知全局监听器
        List<DiagnosticListener> globalListeners = diagnosticListeners.get(null);
        if (globalListeners != null && !globalListeners.isEmpty()) {
            ApplicationManager.getApplication().invokeLater(() -> {
                for (DiagnosticListener listener : globalListeners) {
                    try {
                        listener.onDiagnosticsUpdated(file, diagnostics);
                    } catch (Exception e) {
                        LOG.error("Error notifying global diagnostic listener", e);
                    }
                }
            });
        }
    }
    
    /**
     * 获取指定PSI文件的诊断
     */
    @NotNull
    public CompletableFuture<List<Diagnostic>> getDiagnosticsForPsiFile(@NotNull PsiFile file) {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return updateDiagnostics(virtualFile);
    }
    
    /**
     * 获取指定文档的诊断
     */
    @NotNull
    public CompletableFuture<List<Diagnostic>> getDiagnosticsForDocument(@NotNull Document document, @NotNull Project project) {
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(document);
        if (virtualFile == null) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }
        return updateDiagnostics(virtualFile);
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OmniSharpDiagnosticsService for project: " + project.getName());
        
        // 清除缓存
        diagnosticsCache.clear();
        
        // 清除监听器
        diagnosticListeners.clear();
        lastDiagnosticUpdateTime.clear();
        
        // 清理配置
        if (config != null) {
            Disposer.dispose(config);
        }
    }
    
    /**
     * 诊断监听器接口
     */
    public interface DiagnosticListener {
        /**
         * 诊断更新时调用
         * @param file 文件
         * @param diagnostics 诊断列表
         */
        void onDiagnosticsUpdated(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics);
    }
}