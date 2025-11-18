package com.github.a793181018.omnisharpforintellij.editor.navigation.handler;

import com.github.a793181018.omnisharpforintellij.editor.navigation.model.Reference;
import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget;
import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.find.findUsages.FindUsagesHandlerFactory;
import com.intellij.find.findUsages.FindUsagesOptions;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 查找引用处理器
 * 处理用户请求查找符号引用的操作
 */
public class OmniSharpFindUsagesHandler extends FindUsagesHandler {
    
    private static final Logger LOG = Logger.getInstance(FindUsagesHandler.class);
    private static final int USAGES_TIMEOUT_MS = 5000; // 5秒超时
    
    private final PsiElement psiElement;
    private final Project project;
    
    /**
     * 构造函数
     * @param psiElement PSI元素
     * @param project 项目实例
     */
    public OmniSharpFindUsagesHandler(@NotNull PsiElement psiElement, @NotNull Project project) {
        super(psiElement);
        this.psiElement = psiElement;
        this.project = project;
    }
    
    @NotNull
    @Override
    public PsiElement[] getPrimaryElements() {
        return new PsiElement[]{psiElement};
    }
    
    @NotNull
    @Override
    public PsiElement[] getSecondaryElements() {
        return PsiElement.EMPTY_ARRAY;
    }
    
    @NotNull
    @Override
    public FindUsagesOptions getFindUsagesOptions() {
        FindUsagesOptions options = super.getFindUsagesOptions();
        // 可以在这里设置默认的查找选项
        return options;
    }
    
    /**
     * 异步查找引用
     * 注意：这不是重写方法，而是自定义的辅助方法
     * @return 包含UsageInfo的CompletableFuture
     */
    @NotNull
    public CompletableFuture<UsageInfo[]> findUsagesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // 获取当前文件
                PsiFile psiFile = psiElement.getContainingFile();
                if (psiFile == null) {
                    return new UsageInfo[0];
                }
                
                VirtualFile virtualFile = psiFile.getVirtualFile();
                if (virtualFile == null) {
                    return new UsageInfo[0];
                }
                
                // 获取元素在文件中的位置
                int offset = psiElement.getTextOffset();
                int line = PsiDocumentManager.getInstance(project).getDocument(psiFile).getLineNumber(offset) + 1;
                int column = offset - PsiDocumentManager.getInstance(project).getDocument(psiFile).getLineStartOffset(line - 1) + 1;
                
                // 模拟查找引用 - 在实际实现中，这里应该调用OmniSharp服务器
                // 这里我们返回一个空的引用列表以避免编译错误
                List<Reference> references = new ArrayList<>();
                
                // 转换为UsageInfo数组
                List<UsageInfo> usageInfos = new ArrayList<>();
                for (Reference reference : references) {
                    // Reference本身就是NavigationTarget，不需要getTarget()
                    if (reference.getFilePath() != null) {
                        // 这里简化处理，实际应该创建适当的UsageInfo
                        // usageInfos.add(new UsageInfo(...));
                    }
                }
                
                return usageInfos.toArray(new UsageInfo[0]);
                
            } catch (Exception e) {
                LOG.error("查找引用时发生错误", e);
                return new UsageInfo[0];
            }
        }, AppExecutorUtil.getAppExecutorService());
    }
    
    /**
     * 创建UsageInfo对象
     * @param reference 引用信息
     * @return UsageInfo对象，如果创建失败则返回null
     */
    @Nullable
    private UsageInfo createUsageInfo(@NotNull Reference reference) {
        try {
            // 获取目标文件
            VirtualFile targetFile = project.getBaseDir().getFileSystem().findFileByPath(reference.getFilePath());
            if (targetFile == null) {
                LOG.warn("找不到目标文件: " + reference.getFilePath());
                return null;
            }
            
            // 打开文件并获取PSI文件
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(
                com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(targetFile)
            );
            
            if (psiFile == null) {
                LOG.warn("无法获取PSI文件: " + reference.getFilePath());
                return null;
            }
            
            // 查找指定位置的PSI元素
            int line = reference.getLine();
            int column = reference.getColumn();
            int offset = calculateOffset(psiFile, line, column);
            
            if (offset >= 0 && offset < psiFile.getTextLength()) {
                PsiElement element = psiFile.findElementAt(offset);
                if (element != null) {
                    return new UsageInfo(element);
                }
            }
            
        } catch (Exception e) {
            LOG.error("创建UsageInfo时发生错误", e);
        }
        
        return null;
    }
    
    /**
     * 计算文本偏移量
     * @param psiFile PSI文件
     * @param line 行号（1基）
     * @param column 列号（0基）
     * @return 文本偏移量
     */
    private int calculateOffset(@NotNull PsiFile psiFile, int line, int column) {
        try {
            String text = psiFile.getText();
            String[] lines = text.split("\n");
            
            if (line <= 0 || line > lines.length) {
                return -1;
            }
            
            // 计算到目标行的偏移量
            int offset = 0;
            for (int i = 0; i < line - 1; i++) {
                offset += lines[i].length() + 1; // +1 for newline
            }
            
            // 添加列偏移量
            offset += Math.min(column, lines[line - 1].length());
            
            return offset;
            
        } catch (Exception e) {
            LOG.error("计算文本偏移量时发生错误", e);
            return -1;
        }
    }
    
    /**
     * 获取当前编辑器
     * @return 当前编辑器实例，如果获取失败则返回null
     */
    @Nullable
    private Editor getEditor() {
        try {
            // 获取当前活动的编辑器
            return com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).getSelectedTextEditor();
        } catch (Exception e) {
            LOG.error("获取编辑器时发生错误", e);
            return null;
        }
    }
    
    /**
     * 查找引用处理器工厂
     */
    public static class Factory extends FindUsagesHandlerFactory {
        
        private final Project project;
        
        /**
         * 构造函数
         * @param project 项目实例
         */
        public Factory(@NotNull Project project) {
            this.project = project;
        }
        
        @Override
        public boolean canFindUsages(@NotNull PsiElement element) {
            // 检查元素是否支持查找引用
            // 这里可以根据需要添加更复杂的逻辑
            return element.getContainingFile() != null && 
                   element.getContainingFile().getVirtualFile() != null;
        }
        
        @Override
        public FindUsagesHandler createFindUsagesHandler(@NotNull PsiElement element, boolean forHighlightUsages) {
            return new OmniSharpFindUsagesHandler(element, project);
        }
    }
}