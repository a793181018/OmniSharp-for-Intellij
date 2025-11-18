package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.impl;

import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget;
import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.service.FileStructureService;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewModelBase;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * OmniSharp文件结构视图模型
 * 提供文件结构的树形视图模型
 */
public class OmniSharpStructureViewModel extends StructureViewModelBase implements StructureViewModel.ElementInfoProvider {
    
    private final PsiFile psiFile;
    private final FileStructureService fileStructureService;
    
    public OmniSharpStructureViewModel(@NotNull PsiFile psiFile, @Nullable Editor editor) {
        super(psiFile, editor, new OmniSharpStructureViewElement(psiFile));
        this.psiFile = psiFile;
        this.fileStructureService = psiFile.getProject().getService(FileStructureService.class);
    }
    
    @Override
    @NotNull
    public Class<?>[] getSuitableClasses() {
        // 返回通用的PSI元素类，避免依赖特定的Java PSI类
        return new Class<?>[] {
            PsiElement.class
        };
    }
    
    @Override
    public boolean isAlwaysShowsPlus(StructureViewTreeElement element) {
        return false;
    }
    
    @Override
    public boolean isAlwaysLeaf(StructureViewTreeElement element) {
        Object value = element.getValue();
        // 只有具体的成员（方法、字段等）才是叶子节点
        return value instanceof NavigationTarget && 
               ((NavigationTarget) value).getType() != NavigationTarget.TargetType.CLASS &&
               ((NavigationTarget) value).getType() != NavigationTarget.TargetType.INTERFACE &&
               ((NavigationTarget) value).getType() != NavigationTarget.TargetType.ENUM;
    }
    
    /**
     * 获取文件结构服务
     */
    @NotNull
    public FileStructureService getFileStructureService() {
        return fileStructureService;
    }
    
    /**
     * 获取PSI文件
     */
    @NotNull
    public PsiFile getPsiFile() {
        return psiFile;
    }
}