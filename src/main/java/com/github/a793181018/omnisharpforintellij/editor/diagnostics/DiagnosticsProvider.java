package com.github.a793181018.omnisharpforintellij.editor.diagnostics;

import com.github.a793181018.omnisharpforintellij.editor.common.EditorFeatureProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 代码诊断提供者接口，负责提供代码分析和错误检查功能。
 * 扩展了基础的EditorFeatureProvider接口。
 */
public interface DiagnosticsProvider extends EditorFeatureProvider {
    
    /**
     * 功能ID
     */
    String FEATURE_ID = "omnisharp.diagnostics";
    
    /**
     * 功能名称
     */
    String FEATURE_NAME = "OmniSharp 代码诊断";
    
    /**
     * 功能描述
     */
    String FEATURE_DESCRIPTION = "提供基于OmniSharp服务器的代码分析、错误检查和建议";
    
    /**
     * 分析单个文件的诊断信息
     * @param file 要分析的PSI文件
     * @return CompletableFuture，完成时返回诊断结果列表
     */
    @NotNull
    CompletableFuture<List<Diagnostic>> analyzeFile(@NotNull PsiFile file);
    
    /**
     * 分析整个项目的诊断信息
     * @param project 项目实例
     * @return CompletableFuture，完成时返回项目范围内的诊断结果列表
     */
    @NotNull
    CompletableFuture<List<Diagnostic>> analyzeProject(@NotNull Project project);
    
    /**
     * 分析文档的诊断信息
     * @param document 文档实例
     * @param project 项目实例
     * @return CompletableFuture，完成时返回诊断结果列表
     */
    @NotNull
    CompletableFuture<List<Diagnostic>> analyzeDocument(
            @NotNull Document document,
            @NotNull Project project);
    
    /**
     * 获取指定文件的代码修复建议
     * @param file PSI文件
     * @param diagnostic 诊断信息
     * @return 修复建议列表
     */
    @NotNull
    List<CodeFix> getCodeFixes(@NotNull PsiFile file, @NotNull Diagnostic diagnostic);
    
    /**
     * 应用代码修复
     * @param file PSI文件
     * @param fix 要应用的修复
     * @return CompletableFuture，完成时表示修复已应用
     */
    @NotNull
    CompletableFuture<Boolean> applyCodeFix(@NotNull PsiFile file, @NotNull CodeFix fix);
    
    /**
     * 配置诊断选项
     * @param options 诊断选项
     */
    void configureDiagnosticOptions(@NotNull DiagnosticOptions options);
    
    /**
     * 获取当前诊断选项
     * @return 当前诊断选项
     */
    @NotNull
    DiagnosticOptions getDiagnosticOptions();
    
    /**
     * 诊断信息接口
     */
    interface Diagnostic {
        /**
         * 获取诊断ID
         * @return 诊断ID
         */
        @NotNull
        String getId();
        
        /**
         * 获取诊断消息
         * @return 诊断消息
         */
        @NotNull
        String getMessage();
        
        /**
         * 获取诊断严重性
         * @return 严重性级别
         */
        @NotNull
        Severity getSeverity();
        
        /**
         * 获取文件路径
         * @return 文件路径
         */
        @NotNull
        String getFilePath();
        
        /**
         * 获取开始行号（从1开始）
         * @return 开始行号
         */
        int getStartLine();
        
        /**
         * 获取开始列号（从0开始）
         * @return 开始列号
         */
        int getStartColumn();
        
        /**
         * 获取结束行号（从1开始）
         * @return 结束行号
         */
        int getEndLine();
        
        /**
         * 获取结束列号（从0开始）
         * @return 结束列号
         */
        int getEndColumn();
        
        /**
         * 获取诊断类别
         * @return 诊断类别
         */
        @NotNull
        String getCategory();
        
        /**
         * 获取相关代码上下文
         * @return 代码上下文
         */
        @NotNull
        String getContext();
        
        /**
         * 严重性级别枚举
         */
        enum Severity {
            ERROR,
            WARNING,
            INFO,
            HINT
        }
    }
    
    /**
     * 代码修复接口
     */
    interface CodeFix {
        /**
         * 获取修复ID
         * @return 修复ID
         */
        @NotNull
        String getId();
        
        /**
         * 获取修复名称
         * @return 修复名称
         */
        @NotNull
        String getName();
        
        /**
         * 获取修复描述
         * @return 修复描述
         */
        @NotNull
        String getDescription();
        
        /**
         * 判断修复是否安全（不会破坏代码）
         * @return 如果安全则返回true
         */
        boolean isSafe();
        
        /**
         * 获取关联的诊断ID
         * @return 诊断ID
         */
        @NotNull
        String getDiagnosticId();
    }
    
    /**
     * 诊断选项接口
     */
    interface DiagnosticOptions {
        /**
         * 是否启用实时诊断
         * @return 如果启用则返回true
         */
        boolean isRealtimeDiagnosticsEnabled();
        
        /**
         * 设置是否启用实时诊断
         * @param enabled 启用状态
         */
        void setRealtimeDiagnosticsEnabled(boolean enabled);
        
        /**
         * 获取最低显示的严重性级别
         * @return 严重性级别
         */
        @NotNull
        Diagnostic.Severity getMinimumSeverityLevel();
        
        /**
         * 设置最低显示的严重性级别
         * @param level 严重性级别
         */
        void setMinimumSeverityLevel(@NotNull Diagnostic.Severity level);
        
        /**
         * 是否启用自动修复提示
         * @return 如果启用则返回true
         */
        boolean isAutoFixSuggestionsEnabled();
        
        /**
         * 设置是否启用自动修复提示
         * @param enabled 启用状态
         */
        void setAutoFixSuggestionsEnabled(boolean enabled);
    }
}