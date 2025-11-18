package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.impl;

import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget;
import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.response.FileStructureResponse;
import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.service.FileStructureService;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.impl.common.PsiTreeElementBase;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OmniSharp文件结构视图元素
 * 表示文件结构树中的一个节点
 */
public class OmniSharpStructureViewElement extends PsiTreeElementBase<PsiElement> {
    
    private static final Logger LOG = Logger.getInstance(OmniSharpStructureViewElement.class);
    private static final long TIMEOUT_SECONDS = 10;
    
    private final PsiElement psiElement;
    private final NavigationTarget navigationTarget;
    private final FileStructureService fileStructureService;
    private List<OmniSharpStructureViewElement> children;
    
    /**
     * 构造函数 - 用于根节点（文件）
     */
    public OmniSharpStructureViewElement(@NotNull PsiFile psiFile) {
        super(psiFile);
        this.psiElement = psiFile;
        this.navigationTarget = null;
        this.fileStructureService = psiFile.getProject().getService(FileStructureService.class);
        this.children = null; // 延迟加载
    }
    
    /**
     * 构造函数 - 用于成员节点
     */
    public OmniSharpStructureViewElement(@NotNull PsiElement psiElement, @NotNull NavigationTarget navigationTarget) {
        super(psiElement);
        this.psiElement = psiElement;
        this.navigationTarget = navigationTarget;
        this.fileStructureService = psiElement.getProject().getService(FileStructureService.class);
        this.children = new ArrayList<>();
    }
    
    @Override
    @NotNull
    public Collection<StructureViewTreeElement> getChildrenBase() {
        if (children == null) {
            children = loadChildren();
        }
        
        List<StructureViewTreeElement> result = new ArrayList<>();
        for (OmniSharpStructureViewElement child : children) {
            result.add(child);
        }
        return result;
    }
    
    @Override
    @Nullable
    public String getPresentableText() {
        if (navigationTarget != null) {
            return navigationTarget.getName();
        } else if (psiElement instanceof PsiFile) {
            return ((PsiFile) psiElement).getName();
        }
        return "Unknown";
    }
    
    @Override
    @Nullable
    public String getLocationString() {
        if (navigationTarget != null) {
            return String.format("(%d:%d)", navigationTarget.getLine(), navigationTarget.getColumn());
        }
        return null;
    }
    
    @Override
    @Nullable
    public Icon getIcon(boolean open) {
        if (navigationTarget != null) {
            return getIconForTargetType(navigationTarget.getType());
        } else if (psiElement instanceof PsiFile) {
            return com.intellij.icons.AllIcons.FileTypes.Java; // 使用Java图标作为默认
        }
        return null;
    }
    
    @Override
    @Nullable
    public PsiElement getValue() {
        return psiElement;
    }
    
    @Override
    public void navigate(boolean requestFocus) {
        if (psiElement instanceof NavigatablePsiElement) {
            ((NavigatablePsiElement) psiElement).navigate(requestFocus);
        }
    }
    
    @Override
    public boolean canNavigate() {
        return psiElement instanceof NavigatablePsiElement;
    }
    
    @Override
    public boolean canNavigateToSource() {
        return canNavigate();
    }
    
    /**
     * 根据目标类型获取对应的图标
     */
    @Nullable
    private Icon getIconForTargetType(NavigationTarget.TargetType type) {
        switch (type) {
            case CLASS:
                return com.intellij.icons.AllIcons.Nodes.Class;
            case INTERFACE:
                return com.intellij.icons.AllIcons.Nodes.Interface;
            case ENUM:
                return com.intellij.icons.AllIcons.Nodes.Enum;
            case METHOD:
                return com.intellij.icons.AllIcons.Nodes.Method;
            case FIELD:
                return com.intellij.icons.AllIcons.Nodes.Field;
            case PROPERTY:
                return com.intellij.icons.AllIcons.Nodes.Property;
            case LOCAL_VARIABLE:
                return com.intellij.icons.AllIcons.Nodes.Variable;
            case PARAMETER:
                return com.intellij.icons.AllIcons.Nodes.Parameter;
            case CONSTRUCTOR:
                return com.intellij.icons.AllIcons.Nodes.ClassInitializer;
            case EVENT:
                return com.intellij.icons.AllIcons.Nodes.Artifact;
            case NAMESPACE:
                return com.intellij.icons.AllIcons.Nodes.Package;
            default:
                return com.intellij.icons.AllIcons.Nodes.Field;
        }
    }
    
    /**
     * 加载子节点
     */
    @NotNull
    private List<OmniSharpStructureViewElement> loadChildren() {
        List<OmniSharpStructureViewElement> result = new ArrayList<>();
        
        if (psiElement instanceof PsiFile) {
            PsiFile psiFile = (PsiFile) psiElement;
            
            // 检查文件是否支持
            if (!fileStructureService.isSupportedFile(psiFile.getVirtualFile())) {
                return result;
            }
            
            try {
                // 异步获取文件结构，但等待结果
                CompletableFuture<List<NavigationTarget>> future = fileStructureService.getFileStructure(psiFile.getProject(), psiFile.getVirtualFile());
                
                // 等待结果，带超时
                List<NavigationTarget> targets = ProgressManager.getInstance()
                    .runProcessWithProgressSynchronously(() -> {
                        try {
                            return future.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                        } catch (InterruptedException | ExecutionException | TimeoutException e) {
                            LOG.warn("Failed to get file structure", e);
                            return null;
                        }
                    }, "Loading file structure...", true, psiFile.getProject());
                
                if (targets != null && !targets.isEmpty()) {
                    for (NavigationTarget target : targets) {
                        if (target != null) {
                            // 创建对应的PSI元素（这里简化处理，实际应该根据位置找到真实的PSI元素）
                            PsiElement element = findPsiElementAtPosition(psiFile, target.getLine(), target.getColumn());
                            if (element != null) {
                                result.add(new OmniSharpStructureViewElement(element, target));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOG.error("Error loading file structure", e);
            }
        }
        
        return result;
    }
    
    /**
     * 根据成员信息创建导航目标
     */
    @Nullable
    private NavigationTarget createNavigationTarget(FileStructureResponse.Member member, PsiFile psiFile) {
        try {
            NavigationTarget.TargetType targetType = mapMemberTypeToTargetType(member.getKind());
            return new NavigationTarget.Builder(psiFile.getVirtualFile().getPath(), member.getName(), targetType)
                .withLine(member.getLine())
                .withColumn(member.getColumn())
                .build();
        } catch (Exception e) {
            LOG.warn("Failed to create navigation target for member: " + member.getName(), e);
            return null;
        }
    }
    
    /**
     * 将成员类型映射到导航目标类型
     */
    @NotNull
    private NavigationTarget.TargetType mapMemberTypeToTargetType(String memberType) {
        if (memberType == null) {
            return NavigationTarget.TargetType.METHOD;
        }
        
        switch (memberType.toLowerCase()) {
            case "class":
                return NavigationTarget.TargetType.CLASS;
            case "interface":
                return NavigationTarget.TargetType.INTERFACE;
            case "enum":
                return NavigationTarget.TargetType.ENUM;
            case "method":
                return NavigationTarget.TargetType.METHOD;
            case "field":
                return NavigationTarget.TargetType.FIELD;
            case "property":
                return NavigationTarget.TargetType.PROPERTY;
            case "variable":
                return NavigationTarget.TargetType.LOCAL_VARIABLE;
            case "constructor":
                return NavigationTarget.TargetType.CONSTRUCTOR;
            case "event":
                return NavigationTarget.TargetType.EVENT;
            case "namespace":
                return NavigationTarget.TargetType.NAMESPACE;
            default:
                return NavigationTarget.TargetType.METHOD;
        }
    }
    
    /**
     * 根据位置查找PSI元素
     */
    @Nullable
    private PsiElement findPsiElementAtPosition(PsiFile psiFile, int line, int column) {
        try {
            // 使用文档来计算偏移量
            com.intellij.openapi.editor.Document document = PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile);
            if (document == null) {
                return null;
            }
            
            int offset = document.getLineStartOffset(line - 1) + column - 1;
            return psiFile.findElementAt(offset);
        } catch (Exception e) {
            LOG.warn("Failed to find PSI element at position: " + line + ":" + column, e);
            return null;
        }
    }
}