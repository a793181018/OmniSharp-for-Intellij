package com.github.a793181018.omnisharpforintellij.editor.completion.contributor;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import com.github.a793181018.omnisharpforintellij.editor.completion.OmniSharpCompletionService;
import com.github.a793181018.omnisharpforintellij.editor.completion.impl.CompletionContextImpl;
import com.github.a793181018.omnisharpforintellij.editor.completion.impl.CompletionOptionsImpl;
import com.github.a793181018.omnisharpforintellij.editor.completion.impl.OmniSharpCompletionProviderImpl;
import com.github.a793181018.omnisharpforintellij.editor.completion.impl.OmniSharpCompletionServiceImpl;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequestFactory;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionResponse;
import com.github.a793181018.omnisharpforintellij.editor.completion.performance.CompletionPerformanceService;
import com.intellij.openapi.components.ServiceManager;
// import com.github.a793181018.omnisharpforintellij.util.OmniSharpUtils;
import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * OmniSharp代码补全贡献者，负责将OmniSharp服务器的补全结果集成到IntelliJ的代码补全系统中
 */
public class OmniSharpCompletionContributor extends CompletionContributor {
    private static final Logger LOG = Logger.getInstance(OmniSharpCompletionContributor.class);
    private static final int COMPLETION_TIMEOUT_MS = 2000; // 补全请求超时时间
    
    private final CompletionProvider completionProvider;
    private OmniSharpCompletionService completionService;

    public OmniSharpCompletionContributor() {
        this.completionProvider = new OmniSharpCompletionProviderImpl();
        
        // 注册补全提供器
        extend(CompletionType.BASIC, psiElement(), new OmniSharpCompletionProvider());
        extend(CompletionType.SMART, psiElement(), new OmniSharpCompletionProvider());
        
        LOG.info("OmniSharpCompletionContributor initialized");
    }

    /**
     * OmniSharp补全提供器，负责处理补全请求并返回补全项
     */
    private class OmniSharpCompletionProvider extends com.intellij.codeInsight.completion.CompletionProvider<CompletionParameters> {
        private CompletionPerformanceService performanceService;
        @Override
        protected void addCompletions(
                @NotNull CompletionParameters parameters,
                @NotNull ProcessingContext context,
                @NotNull CompletionResultSet resultSet) {
            
            // 检查是否应该处理此补全请求
            if (!shouldHandleCompletion(parameters)) {
                return;
            }

            Project project = parameters.getEditor().getProject();
            if (project == null) {
                return;
            }

            // 获取补全服务实例
            completionService = ServiceManager.getService(project, OmniSharpCompletionService.class);
            performanceService = ServiceManager.getService(project, CompletionPerformanceService.class);
            
            // 取消可能正在进行的请求
            completionService.cancelRequest();

            try {
                // 创建补全请求
                CompletionRequest request = CompletionRequestFactory.createFromEditor(
                        parameters.getEditor(),
                        project,
                        parameters.getPosition().getContainingFile(),
                        parameters.getOffset()
                );

                // 异步获取补全项
                CompletableFuture<CompletionResponse> future = completionService.getCompletionsAsync(request);
                
                // 等待补全结果（带超时）
                CompletionResponse response = future.get(COMPLETION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
                
                // 处理补全结果
                if (response.isSuccessful()) {
                    addCompletionItems(response, resultSet);
                } else if (response.getErrorMessage() != null) {
                    LOG.warn("Completion failed: " + response.getErrorMessage());
                }
                
            } catch (Exception e) {
                LOG.warn("Error during completion: " + e.getMessage());
            }
        }

        /**
         * 检查是否应该处理此补全请求
         */
        private boolean shouldHandleCompletion(@NotNull CompletionParameters parameters) {
            return ((com.github.a793181018.omnisharpforintellij.editor.completion.impl.OmniSharpCompletionProviderImpl)
                    completionProvider).shouldHandleCompletion(parameters);
        }

        /**
         * 创建补全上下文
         */
        @NotNull
        private CompletionProvider.CompletionContext createCompletionContext(@NotNull CompletionParameters parameters) {
            return new CompletionContextImpl(parameters.getPosition(), parameters.getOffset());
        }

        /**
         * 将OmniSharp补全项添加到结果集中
         */
        private void addCompletionItems(
                @NotNull CompletionResponse response,
                @NotNull CompletionResultSet resultSet) {
            
            for (CompletionItem item : response.getItems()) {
                // 创建IntelliJ的补全项
                LookupElementBuilder elementBuilder = createLookupElement(item);
                
                // 设置优先级 (IntelliJ API中不支持withPriority方法)
                
                // 添加补全项
                resultSet.addElement(elementBuilder);
            }
            
            LOG.debug("Added " + response.getItems().size() + " completion items");
        }

        /**
         * 创建IntelliJ的LookupElement
         */
        @NotNull
        private LookupElementBuilder createLookupElement(@NotNull CompletionItem item) {
            // 创建基本的补全项
            LookupElementBuilder builder = LookupElementBuilder.create(item.getInsertText())
                    .withPresentableText(item.getLabel())
                    .withTypeText(item.getDetail(), true);

            // 设置图标
            builder = builder.withIcon(getCompletionItemIcon(item.getKind()));

            // 设置文档
            if (item.getDocumentation() != null) {
                builder = builder.withTailText(" " + item.getDocumentation(), true);
            }

            // 如果是代码片段，设置插入文本
            if (item.isInsertTextIsSnippet()) {
                builder = builder.withInsertHandler(new SnippetInsertHandler(item.getInsertText()));
            }

            return builder;
        }

        /**
         * 获取补全项图标
         */
        @NotNull
        private javax.swing.Icon getCompletionItemIcon(@NotNull CompletionItem.CompletionKind kind) {
            // 根据补全项类型返回相应的图标
            switch (kind) {
                case METHOD:
                case FUNCTION:
                case CONSTRUCTOR:
                    return com.intellij.icons.AllIcons.Nodes.Method;
                case CLASS:
                    return com.intellij.icons.AllIcons.Nodes.Class;
                case INTERFACE:
                    return com.intellij.icons.AllIcons.Nodes.Interface;
                case FIELD:
                    return com.intellij.icons.AllIcons.Nodes.Field;
                case VARIABLE:
                    return com.intellij.icons.AllIcons.Nodes.Variable;
                case ENUM:
                    return com.intellij.icons.AllIcons.Nodes.Enum;
                case PROPERTY:
                    return com.intellij.icons.AllIcons.Nodes.Property;
                case KEYWORD:
                    return com.intellij.icons.AllIcons.FileTypes.Any_type;
                case SNIPPET:
                    return com.intellij.icons.AllIcons.FileTypes.Any_type;
                default:
                    return com.intellij.icons.AllIcons.FileTypes.Any_type;
            }
        }
    }

    /**
     * 代码片段插入处理器
     */
    private static class SnippetInsertHandler implements InsertHandler<LookupElement> {
        private final String snippetText;

        public SnippetInsertHandler(@NotNull String snippetText) {
            this.snippetText = snippetText;
        }

        @Override
        public void handleInsert(
                @NotNull InsertionContext context,
                @NotNull LookupElement item) {
            // 插入代码片段
            Editor editor = context.getEditor();
            int offset = context.getStartOffset();
            
            // 替换当前选中的文本
            editor.getDocument().replaceString(offset, context.getTailOffset(), snippetText);
            
            // 更新光标位置
            context.commitDocument();
        }
    }

    @Override
    public void beforeCompletion(@NotNull CompletionInitializationContext context) {
        // 在补全开始前执行
        LOG.debug("Starting completion");
        
        // 预加载补全服务并执行智能预取
        Project project = context.getProject();
        if (project != null && !project.isDisposed()) {
            OmniSharpCompletionService service = ServiceManager.getService(project, OmniSharpCompletionService.class);
            CompletionPerformanceService performanceService = ServiceManager.getService(project, CompletionPerformanceService.class);
            
            // 执行智能预取
            Editor editor = context.getEditor();
            PsiFile psiFile = context.getFile();
            if (editor != null) {
                int caretOffset = editor.getCaretModel().getOffset();
                performanceService.prefetchCompletions(editor, psiFile, caretOffset);
            }
        }
    }

    @Override
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        // 自动弹出补全菜单的条件
        return shouldTriggerAutoPopup(position, typeChar);
    }

    /**
     * 判断是否应该触发自动弹出补全菜单
     */
    private boolean shouldTriggerAutoPopup(@NotNull PsiElement position, char typeChar) {
        // 在点操作符后自动弹出补全
        if (typeChar == '.') {
            return true;
        }
        
        // 在其他特定字符后触发
        return switch (typeChar) {
            case '(', '[' -> true;
            default -> false;
        };
    }

    public void afterCompletion(@NotNull PsiElement position) {
        // 补全完成后执行
        LOG.debug("Completion completed at: " + position.getTextOffset());
    }
}