/**
 * OmniSharp格式化服务核心类
 * 负责与OmniSharp服务器通信并处理格式化请求
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting;

import com.github.a793181018.omnisharpforintellij.communicator.OmniSharpServerClientImpl;
import com.github.a793181018.omnisharpforintellij.editor.formatting.model.DocumentFormattingRequest;
import com.github.a793181018.omnisharpforintellij.editor.formatting.model.RangeFormattingRequest;
import com.github.a793181018.omnisharpforintellij.editor.formatting.model.FormattingResponse;
import com.github.a793181018.omnisharpforintellij.service.cache.FormattingCache;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

public class OmniSharpFormattingService implements Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpFormattingService.class);
    private static final Duration FORMATTING_TIMEOUT = Duration.ofSeconds(10);
    
    private final Project project;
    private final OmniSharpServerClientImpl serverClient;
    private final FormattingCache formattingCache;
    private final FileDocumentManager fileDocumentManager;
    
    public OmniSharpFormattingService(Project project) {
        this.project = project;
        this.serverClient = new OmniSharpServerClientImpl();
        this.formattingCache = new FormattingCache();
        this.fileDocumentManager = FileDocumentManager.getInstance();
    }
    
    /**
     * 格式化整个文档
     * @param file 需要格式化的文件
     * @return 格式化后的内容
     */
    public CompletableFuture<String> formatDocument(VirtualFile file) {
        if (file == null || !"cs".equals(file.getExtension())) {
            return CompletableFuture.completedFuture("");
        }
        
        // 检查缓存
        byte[] content;
        try {
            content = file.contentsToByteArray();
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
        int hash = java.util.Arrays.hashCode(content);
        String cachedResult = formattingCache.getFormattedContent(file.getPath(), hash);
        if (cachedResult != null) {
            return CompletableFuture.completedFuture(cachedResult);
        }
        
        // 构建请求
        DocumentFormattingRequest request = new DocumentFormattingRequest(file);
        
        // 发送请求到OmniSharp服务器
        return serverClient.sendRequest(request, FormattingResponse.class)
            .map(response -> {
                String formattedContent = response.getFormattedContent();
                // 更新缓存
                formattingCache.putFormattedContent(file.getPath(), hash, formattedContent);
                return formattedContent;
            })
            .timeout(FORMATTING_TIMEOUT)
            .subscribeOn(Schedulers.boundedElastic())
            .toFuture();
    }
    
    /**
     * 格式化选中的代码片段
     * @param file 文件
     * @param range 选择范围
     * @return 格式化后的内容
     */
    public CompletableFuture<String> formatSelection(VirtualFile file, TextRange range) {
        if (file == null || !"cs".equals(file.getExtension()) || range == null) {
            return CompletableFuture.completedFuture("");
        }
        
        // 构建请求
        RangeFormattingRequest request = new RangeFormattingRequest(file, range);
        
        // 发送请求到OmniSharp服务器
        return serverClient.sendRequest(request, FormattingResponse.class)
            .map(FormattingResponse::getFormattedContent)
            .timeout(FORMATTING_TIMEOUT)
            .subscribeOn(Schedulers.boundedElastic())
            .toFuture();
    }
    
    /**
     * 应用格式化内容到文档
     * @param document 文档
     * @param formattedContent 格式化后的内容
     */
    public void applyFormatting(Document document, String formattedContent) {
        if (document == null || formattedContent == null) {
            return;
        }
        
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                document.setText(formattedContent);
            } catch (Exception e) {
                LOG.error("Failed to apply formatting", e);
            }
        });
    }
    
    /**
     * 应用格式化内容到选择范围
     * @param document 文档
     * @param range 选择范围
     * @param formattedContent 格式化后的内容
     */
    public void applySelectionFormatting(Document document, TextRange range, String formattedContent) {
        if (document == null || range == null || formattedContent == null) {
            return;
        }
        
        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                document.replaceString(range.getStartOffset(), range.getEndOffset(), formattedContent);
            } catch (Exception e) {
                LOG.error("Failed to apply selection formatting", e);
            }
        });
    }
    
    /**
     * 检查是否应该跳过格式化
     * @param file 文件
     * @return 是否应该跳过
     */
    public boolean shouldSkipFormatting(VirtualFile file) {
        if (file == null) {
            return true;
        }
        
        OmniSharpFormattingSettings settings = OmniSharpFormattingSettings.getInstance(project);
        
        // 检查排除的目录
        String filePath = file.getPath();
        for (String excludedDir : settings.getExcludedDirectories()) {
            if (filePath.startsWith(excludedDir)) {
                return true;
            }
        }
        
        // 检查文件大小限制
        if (file.getLength() > settings.getMaxFileSize()) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 获取格式化缓存
     * @return 格式化缓存
     */
    public FormattingCache getFormattingCache() {
        return formattingCache;
    }
    
    @Override
    public void dispose() {
        formattingCache.clear();
    }
}