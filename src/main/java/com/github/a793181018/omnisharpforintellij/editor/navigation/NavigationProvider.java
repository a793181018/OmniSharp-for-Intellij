package com.github.a793181018.omnisharpforintellij.editor.navigation;

import com.github.a793181018.omnisharpforintellij.editor.common.EditorFeatureProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 代码导航提供者接口，负责提供代码导航功能。
 * 扩展了基础的EditorFeatureProvider接口。
 */
public interface NavigationProvider extends EditorFeatureProvider {
    
    /**
     * 功能ID
     */
    String FEATURE_ID = "omnisharp.navigation";
    
    /**
     * 功能名称
     */
    String FEATURE_NAME = "OmniSharp 代码导航";
    
    /**
     * 功能描述
     */
    String FEATURE_DESCRIPTION = "提供基于OmniSharp服务器的代码导航功能，如跳转到定义、查找引用等";
    
    /**
     * 导航到定义
     * @param editor 编辑器实例
     * @param file 当前文件
     * @param offset 光标偏移量
     * @return CompletableFuture，完成时返回导航目标位置
     */
    @NotNull
    CompletableFuture<List<NavigationTarget>> navigateToDefinition(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset);
    
    /**
     * 查找引用
     * @param editor 编辑器实例
     * @param file 当前文件
     * @param offset 光标偏移量
     * @return CompletableFuture，完成时返回引用列表
     */
    @NotNull
    CompletableFuture<List<Reference>> findReferences(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset);
    
    /**
     * 查找实现
     * @param editor 编辑器实例
     * @param file 当前文件
     * @param offset 光标偏移量
     * @return CompletableFuture，完成时返回实现列表
     */
    @NotNull
    CompletableFuture<List<NavigationTarget>> findImplementations(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset);
    
    /**
     * 查找类型定义
     * @param editor 编辑器实例
     * @param file 当前文件
     * @param offset 光标偏移量
     * @return CompletableFuture，完成时返回类型定义位置
     */
    @NotNull
    CompletableFuture<List<NavigationTarget>> navigateToTypeDefinition(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset);
    
    /**
     * 检查是否应该处理给定位置的导航请求
     * @param editor 编辑器实例
     * @param file 当前文件
     * @param offset 光标偏移量
     * @return 如果应该处理则返回true
     */
    boolean shouldHandleNavigation(
            @NotNull Editor editor,
            @NotNull PsiFile file,
            int offset);
    
    /**
     * 获取元素的快速导航信息
     * @param element PSI元素
     * @return 导航信息，如果元素不支持导航则返回null
     */
    @Nullable
    NavigationInfo getNavigationInfo(@NotNull PsiElement element);
    
    /**
     * 导航目标接口
     */
    interface NavigationTarget {
        /**
         * 获取文件路径
         * @return 文件路径
         */
        @NotNull
        String getFilePath();
        
        /**
         * 获取行号（从1开始）
         * @return 行号
         */
        int getLine();
        
        /**
         * 获取列号（从0开始）
         * @return 列号
         */
        int getColumn();
        
        /**
         * 获取目标名称
         * @return 目标名称
         */
        @NotNull
        String getName();
        
        /**
         * 获取目标类型
         * @return 目标类型
         */
        @NotNull
        TargetType getType();
        
        /**
         * 目标类型枚举
         */
        enum TargetType {
            METHOD,
            CLASS,
            INTERFACE,
            PROPERTY,
            FIELD,
            ENUM,
            ENUM_VALUE,
            NAMESPACE,
            OTHER
        }
    }
    
    /**
     * 引用接口
     */
    interface Reference extends NavigationTarget {
        /**
         * 判断是否为写入引用
         * @return 如果是写入引用则返回true
         */
        boolean isWriteAccess();
        
        /**
         * 获取引用上下文（代码片段）
         * @return 引用上下文
         */
        @NotNull
        String getContext();
    }
    
    /**
     * 导航信息接口
     */
    interface NavigationInfo {
        /**
         * 获取元素名称
         * @return 元素名称
         */
        @NotNull
        String getElementName();
        
        /**
         * 获取元素类型
         * @return 元素类型
         */
        @NotNull
        String getElementType();
        
        /**
         * 判断元素是否可导航到定义
         * @return 如果可导航则返回true
         */
        boolean canNavigateToDefinition();
        
        /**
         * 判断元素是否可查找引用
         * @return 如果可查找则返回true
         */
        boolean canFindReferences();
        
        /**
         * 判断元素是否可查找实现
         * @return 如果可查找则返回true
         */
        boolean canFindImplementations();
    }
}