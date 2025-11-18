package com.github.a793181018.omnisharpforintellij.editor.completion.impl;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.server.communication.impl.OmniSharpCommunicator;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp代码补全提供者的实现类
 */
public class OmniSharpCompletionProviderImpl implements CompletionProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OmniSharpCompletionProviderImpl.class);
    
    private Project project;
    private OmniSharpCommunicator communicator;
    private CompletionOptionsImpl completionOptions;
    private boolean enabled = true;
    
    @Override
    public void initialize(@NotNull Project project) {
        this.project = project;
        this.communicator = new OmniSharpCommunicator();
        this.completionOptions = new CompletionOptionsImpl();
        LOG.info("OmniSharpCompletionProvider initialized");
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void configure(@NotNull Map<String, Object> config) {
        // 配置功能
        if (config.containsKey("enabled")) {
            this.enabled = (Boolean) config.get("enabled");
        }
        
        if (config.containsKey("smartCompletion")) {
            completionOptions.setSmartCompletionEnabled((Boolean) config.get("smartCompletion"));
        }
        
        if (config.containsKey("maxResults")) {
            completionOptions.setMaxResults((Integer) config.get("maxResults"));
        }
    }
    
    @Override
    public void dispose() {
        // 清理资源
        LOG.info("OmniSharpCompletionProvider disposed");
    }
    
    @NotNull
    @Override
    public String getFeatureId() {
        return FEATURE_ID;
    }
    
    @NotNull
    @Override
    public String getFeatureName() {
        return FEATURE_NAME;
    }
    
    @NotNull
    @Override
    public String getFeatureDescription() {
        return FEATURE_DESCRIPTION;
    }
    
    @NotNull
    @Override
    public CompletableFuture<Void> provideCompletions(
            @NotNull CompletionParameters parameters, 
            @NotNull CompletionResultSet resultSet) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                PsiElement position = parameters.getPosition();
                PsiFile file = position.getContainingFile().getOriginalFile();
                VirtualFile virtualFile = file.getVirtualFile();
                
                if (virtualFile == null || !"cs".equals(virtualFile.getExtension())) {
                    return;
                }
                
                int offset = parameters.getOffset();
                CompletionContext context = createCompletionContext(position, offset);
                
                // 构建补全请求
                String filePath = context.getFilePath();
                
                // 计算行号和列号（简化实现，实际可能需要更精确的计算）
                int line = 1;
                int column = 1;
                
                // 获取文件内容
                String buffer = "";
                try {
                    if (filePath != null && !filePath.isEmpty()) {
                        VirtualFile localFile = LocalFileSystem.getInstance().findFileByPath(filePath);
                        if (localFile != null) {
                            buffer = new String(localFile.contentsToByteArray());
                            
                            // 计算行号和列号
                            int currentOffset = 0;
                            for (int i = 0; i < buffer.length() && i < offset; i++) {
                                if (buffer.charAt(i) == '\n') {
                                    line++;
                                    column = 1;
                                } else {
                                    column++;
                                }
                                currentOffset++;
                                if (currentOffset >= offset) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Failed to read file content", e);
                }
                
                // 创建OmniSharp请求参数
                Map<String, Object> arguments = new HashMap<>();
                arguments.put("FileName", filePath != null ? filePath : "");
                arguments.put("Line", line);
                arguments.put("Column", column);
                arguments.put("Buffer", buffer);
                
                // 添加补全选项
                arguments.put("WantImportable", completionOptions.includeImportStatements());
                arguments.put("WantSnippet", completionOptions.includeSnippets());
                arguments.put("WantMethodHeader", completionOptions.isMethodParameterHintsEnabled());
                arguments.put("WantSmartCompletion", completionOptions.isSmartCompletionEnabled());
                arguments.put("MaxItems", completionOptions.getMaxResults());
                arguments.put("WantSortText", completionOptions.isSortResults());
                
                // 创建OmniSharp请求
                OmniSharpRequest<CompletionResponse> omniSharpRequest = 
                        new OmniSharpRequest<>("completion", arguments, CompletionResponse.class);
                
                // 发送请求到OmniSharp服务器
                CompletableFuture<OmniSharpResponse<CompletionResponse>> responseFuture = 
                        communicator.sendRequest(omniSharpRequest);
                
                // 处理响应
                responseFuture.thenAccept(omniSharpResponse -> {
                    if (omniSharpResponse != null && omniSharpResponse.getBody() != null) {
                        CompletionResponse response = omniSharpResponse.getBody();
                        if (response != null && response.getItems() != null) {
                            // 将OmniSharp补全项转换为IntelliJ补全项
                            response.getItems().forEach(item -> {
                                LookupElementBuilder lookupElement = createLookupElement(item);
                                resultSet.addElement(lookupElement);
                            });
                        }
                    }
                    resultSet.stopHere();
                }).exceptionally(e -> {
                    LOG.error("Error processing completion response", e);
                    resultSet.stopHere();
                    return null;
                });
                
            } catch (Exception e) {
                LOG.error("Error providing completions", e);
                resultSet.stopHere();
            }
        });
    }
    
    @Override
    public boolean shouldHandleCompletion(@NotNull CompletionParameters parameters) {
        // 检查是否为C#文件
        PsiElement position = parameters.getPosition();
        PsiFile file = position.getContainingFile().getOriginalFile();
        VirtualFile virtualFile = file.getVirtualFile();
        
        return virtualFile != null && "cs".equals(virtualFile.getExtension());
    }
    
    @NotNull
    @Override
    public CompletionContext createCompletionContext(@NotNull PsiElement element, int offset) {
        return new CompletionContextImpl(element, offset);
    }
    
    @Override
    public void configureCompletionOptions(@NotNull CompletionOptions options) {
        if (options instanceof CompletionOptionsImpl) {
            this.completionOptions = (CompletionOptionsImpl) options;
        } else {
            // 转换配置
            this.completionOptions.setSmartCompletionEnabled(options.isSmartCompletionEnabled());
            this.completionOptions.setMethodParameterHintsEnabled(options.isMethodParameterHintsEnabled());
            this.completionOptions.setMaxResults(options.getMaxResults());
            this.completionOptions.setSortResults(options.isSortResults());
        }
    }
    
    @NotNull
    @Override
    public CompletionOptions getCompletionOptions() {
        return completionOptions;
    }
    
    /**
     * 将OmniSharp补全项转换为IntelliJ的LookupElement
     */
    private LookupElementBuilder createLookupElement(CompletionItem item) {
        LookupElementBuilder builder = LookupElementBuilder.create(item.getInsertText() != null ? item.getInsertText() : item.getLabel())
            .withPresentableText(item.getLabel());
        
        // 设置类型文本
        if (item.getDetail() != null) {
            builder = builder.withTypeText(item.getDetail(), true);
        }
        
        // 目前无法从CompletionKind获取图标，暂时移除图标设置
        // if (item.getKind() != null) {
        //     builder = builder.withIcon(...); // 需要实现映射逻辑
        // }
        
        // 设置描述
        if (item.getDocumentation() != null) {
            builder = builder.withTailText(" " + item.getDocumentation(), true);
        }
        
        return builder;
    }
}