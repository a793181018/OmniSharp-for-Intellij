package com.github.a793181018.omnisharpforintellij.editor.completion;

import com.github.a793181018.omnisharpforintellij.editor.common.EditorFeatureProvider;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 代码补全提供者接口，负责提供代码补全功能。
 * 扩展了基础的EditorFeatureProvider接口。
 */
public interface CompletionProvider extends EditorFeatureProvider {
    
    /**
     * 功能ID
     */
    String FEATURE_ID = "omnisharp.completion";
    
    /**
     * 功能名称
     */
    String FEATURE_NAME = "OmniSharp 代码补全";
    
    /**
     * 功能描述
     */
    String FEATURE_DESCRIPTION = "提供基于OmniSharp服务器的代码补全建议";
    
    /**
     * 处理代码补全请求
     * @param parameters 补全参数
     * @param resultSet 结果集用于添加补全项
     * @return CompletableFuture，完成时表示补全处理完成
     */
    @NotNull
    CompletableFuture<Void> provideCompletions(
            @NotNull CompletionParameters parameters,
            @NotNull CompletionResultSet resultSet);
    
    /**
     * 检查是否应该处理给定位置的补全请求
     * @param parameters 补全参数
     * @return 如果应该处理则返回true
     */
    boolean shouldHandleCompletion(@NotNull CompletionParameters parameters);
    
    /**
     * 获取当前光标位置的上下文信息
     * @param element 当前PSI元素
     * @param offset 偏移量
     * @return 补全上下文对象
     */
    @NotNull
    CompletionContext createCompletionContext(
            @NotNull PsiElement element,
            int offset);
    
    /**
     * 配置补全选项
     * @param options 补全选项
     */
    void configureCompletionOptions(@NotNull CompletionOptions options);
    
    /**
     * 获取当前补全选项
     * @return 当前补全选项
     */
    @NotNull
    CompletionOptions getCompletionOptions();
    
    /**
     * 代码补全上下文接口
     */
    interface CompletionContext {
        /**
         * 获取当前行文本
         * @return 当前行文本
         */
        @NotNull
        String getCurrentLine();
        
        /**
         * 获取光标前的标识符
         * @return 光标前的标识符，如果没有则返回空字符串
         */
        @NotNull
        String getPrefix();
        
        /**
         * 获取光标位置
         * @return 光标在文档中的偏移量
         */
        int getOffset();
        
        /**
         * 获取文件路径
         * @return 文件路径
         */
        @NotNull
        String getFilePath();
        
        /**
         * 获取上下文类型
         * @return 上下文类型枚举值
         */
        @NotNull
        ContextType getContextType();
        
        /**
         * 上下文类型枚举
         */
        enum ContextType {
            /**
             * 在导入语句中
             */
            IMPORT,
            /**
             * 在类型名称中
             */
            TYPE_NAME,
            /**
             * 在成员访问中（如 obj. 后的位置）
             */
            MEMBER_ACCESS,
            /**
             * 在方法调用参数中
             */
            METHOD_PARAMETER,
            /**
             * 在变量声明中
             */
            VARIABLE_DECLARATION,
            /**
             * 在语句中
             */
            STATEMENT,
            /**
             * 其他上下文
             */
            OTHER
        }
    }
    
    /**
     * 补全选项接口
     */
    interface CompletionOptions {
        /**
         * 是否启用方法参数提示
         * @return 如果启用则返回true
         */
        boolean isMethodParameterHintsEnabled();
        
        /**
         * 设置是否启用方法参数提示
         * @param enabled 启用状态
         */
        void setMethodParameterHintsEnabled(boolean enabled);
        
        /**
         * 是否启用智能补全
         * @return 如果启用则返回true
         */
        boolean isSmartCompletionEnabled();
        
        /**
         * 设置是否启用智能补全
         * @param enabled 启用状态
         */
        void setSmartCompletionEnabled(boolean enabled);
        
        /**
         * 获取最大补全结果数量
         * @return 最大结果数量
         */
        int getMaxResults();
        
        /**
         * 设置最大补全结果数量
         * @param maxResults 最大结果数量
         */
        void setMaxResults(int maxResults);
        
        /**
         * 是否对结果进行排序
         * @return 如果启用排序则返回true
         */
        boolean isSortResults();
        
        /**
         * 设置是否对结果进行排序
         * @param sortResults 排序状态
         */
        void setSortResults(boolean sortResults);
    }
}