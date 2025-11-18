package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.impl;

import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.service.FileStructureService;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.PsiStructureViewFactory;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * OmniSharp文件结构视图工厂
 * 为C#文件创建结构视图构建器
 */
public class OmniSharpStructureViewFactory implements PsiStructureViewFactory {
    
    @Override
    @Nullable
    public StructureViewBuilder getStructureViewBuilder(@NotNull PsiFile psiFile) {
        return new TreeBasedStructureViewBuilder() {
            @Override
            @NotNull
            public StructureViewModel createStructureViewModel(@Nullable Editor editor) {
                return new OmniSharpStructureViewModel(psiFile, editor);
            }
            
            @Override
            public boolean isRootNodeShown() {
                return false;
            }
        };
    }
    
    /**
     * 检查是否应该为指定的文件创建结构视图
     * 
     * @param psiFile 要检查的文件
     * @return 如果是C#文件则返回true，否则返回false
     */
    public static boolean isApplicable(@NotNull PsiFile psiFile) {
        String fileName = psiFile.getName();
        return fileName.endsWith(".cs") || fileName.endsWith(".CS");
    }
}