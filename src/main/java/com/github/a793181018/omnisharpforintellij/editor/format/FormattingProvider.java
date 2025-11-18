package com.github.a793181018.omnisharpforintellij.editor.format;

import com.github.a793181018.omnisharpforintellij.editor.common.EditorFeatureProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * 代码格式化提供者接口，负责提供代码格式化功能。
 * 扩展了基础的EditorFeatureProvider接口。
 */
public interface FormattingProvider extends EditorFeatureProvider {
    
    /**
     * 功能ID
     */
    String FEATURE_ID = "omnisharp.formatting";
    
    /**
     * 功能名称
     */
    String FEATURE_NAME = "OmniSharp 代码格式化";
    
    /**
     * 功能描述
     */
    String FEATURE_DESCRIPTION = "提供基于OmniSharp服务器的代码格式化功能";
    
    /**
     * 格式化整个文件
     * @param file 要格式化的PSI文件
     * @return CompletableFuture，完成时返回格式化后的代码
     */
    @NotNull
    CompletableFuture<String> formatFile(@NotNull PsiFile file);
    
    /**
     * 格式化文档
     * @param document 要格式化的文档
     * @param file 关联的PSI文件
     * @return CompletableFuture，完成时返回格式化后的代码
     */
    @NotNull
    CompletableFuture<String> formatDocument(@NotNull Document document, @NotNull PsiFile file);
    
    /**
     * 格式化文档中的选定区域
     * @param document 文档
     * @param file 关联的PSI文件
     * @param selectionModel 选择模型，包含选定区域信息
     * @return CompletableFuture，完成时返回格式化后的代码
     */
    @NotNull
    CompletableFuture<String> formatSelection(
            @NotNull Document document,
            @NotNull PsiFile file,
            @NotNull SelectionModel selectionModel);
    
    /**
     * 格式化PSI元素
     * @param element 要格式化的PSI元素
     * @return CompletableFuture，完成时返回格式化后的代码
     */
    @NotNull
    CompletableFuture<String> formatElement(@NotNull PsiElement element);
    
    /**
     * 格式化代码段
     * @param code 代码字符串
     * @param languageId 语言ID（如"cs"）
     * @return CompletableFuture，完成时返回格式化后的代码
     */
    @NotNull
    CompletableFuture<String> formatCode(@NotNull String code, @NotNull String languageId);
    
    /**
     * 调整文件缩进
     * @param file PSI文件
     * @param options 格式化选项
     * @return CompletableFuture，完成时返回调整缩进后的代码
     */
    @NotNull
    CompletableFuture<String> adjustIndent(@NotNull PsiFile file, @NotNull FormattingOptions options);
    
    /**
     * 配置格式化选项
     * @param options 格式化选项
     */
    void configureFormattingOptions(@NotNull FormattingOptions options);
    
    /**
     * 获取当前格式化选项
     * @return 当前格式化选项
     */
    @NotNull
    FormattingOptions getFormattingOptions();
    
    /**
     * 格式化选项接口
     */
    interface FormattingOptions {
        /**
         * 获取制表符大小
         * @return 制表符大小
         */
        int getTabSize();
        
        /**
         * 设置制表符大小
         * @param tabSize 制表符大小
         */
        void setTabSize(int tabSize);
        
        /**
         * 是否使用制表符进行缩进
         * @return 如果使用制表符则返回true
         */
        boolean useTabsForIndentation();
        
        /**
         * 设置是否使用制表符进行缩进
         * @param useTabs 使用状态
         */
        void setUseTabsForIndentation(boolean useTabs);
        
        /**
         * 获取缩进大小
         * @return 缩进大小
         */
        int getIndentSize();
        
        /**
         * 设置缩进大小
         * @param indentSize 缩进大小
         */
        void setIndentSize(int indentSize);
        
        /**
         * 获取大括号样式
         * @return 大括号样式
         */
        @NotNull
        BracesStyle getBracesStyle();
        
        /**
         * 设置大括号样式
         * @param style 大括号样式
         */
        void setBracesStyle(@NotNull BracesStyle style);
        
        /**
         * 是否在运算符前后添加空格
         * @return 如果添加则返回true
         */
        boolean insertSpaceAroundOperators();
        
        /**
         * 设置是否在运算符前后添加空格
         * @param insert 是否添加
         */
        void setInsertSpaceAroundOperators(boolean insert);
        
        /**
         * 是否在逗号后添加空格
         * @return 如果添加则返回true
         */
        boolean insertSpaceAfterComma();
        
        /**
         * 设置是否在逗号后添加空格
         * @param insert 是否添加
         */
        void setInsertSpaceAfterComma(boolean insert);
        
        /**
         * 大括号样式枚举
         */
        enum BracesStyle {
            /**
             * 与语句在同一行
             * e.g., if (condition) { ... }
             */
            SAME_LINE,
            
            /**
             * 在下一行
             * e.g., if (condition)
             *       { ... }
             */
            NEXT_LINE,
            
            /**
             * 在下一行，且缩进
             * e.g., if (condition)
             *         { ... }
             */
            NEXT_LINE_INDENTED
        }
    }
}