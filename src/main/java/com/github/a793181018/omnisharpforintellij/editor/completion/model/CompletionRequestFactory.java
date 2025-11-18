package com.github.a793181018.omnisharpforintellij.editor.completion.model;


import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

/**
 * 补全请求工厂类，用于创建和管理代码补全请求
 */
public class CompletionRequestFactory {

    /**
     * 从编辑器上下文创建补全请求
     * @param editor 编辑器实例
     * @param project 项目实例
     * @param psiFile PSI文件
     * @param offset 光标位置
     * @return 补全请求对象
     */
    @NotNull
    public static CompletionRequest createFromEditor(
            @NotNull Editor editor, 
            @NotNull Project project, 
            @NotNull PsiFile psiFile, 
            int offset) {
        
        // 获取文件内容
        String buffer = editor.getDocument().getText();
        
        // 获取行列信息
        int lineNumber = editor.getDocument().getLineNumber(offset) + 1; // OmniSharp使用1-based行号
        int columnNumber = offset - editor.getDocument().getLineStartOffset(lineNumber - 1) + 1; // OmniSharp使用1-based列号
        
        // 获取文件路径
        String filePath = psiFile.getVirtualFile() != null ? 
                psiFile.getVirtualFile().getPath() : 
                psiFile.getName();
        
        // 创建请求，避免使用不存在的类型
        return new CompletionRequest.Builder(
                filePath,
                lineNumber,
                columnNumber,
                project,
                buffer,
                null // 上下文参数设为null，在使用时会有默认处理
        )
         .withAdditionalData("requestId", generateRequestId())
         .build();
    }
    
    /**
     * 创建默认的补全请求
     * @param project 项目实例
     * @param filePath 文件路径
     * @param buffer 文件内容
     * @param line 行号
     * @param column 列号
     * @return 默认补全请求
     */
    @NotNull
    public static CompletionRequest createDefaultRequest(
            @NotNull Project project,
            @NotNull String filePath,
            @NotNull String buffer,
            int line,
            int column) {
        
        // 直接创建请求，避免使用不存在的实现类
        return new CompletionRequest.Builder(
                filePath,
                line,
                column,
                project,
                buffer,
                null // 上下文参数设为null，在使用时会有默认处理
        )
        .withAdditionalData("requestId", generateRequestId())
        .build();
    }
    
    /**
     * 根据行列位置计算偏移量
     * @param text 文件内容
     * @param line 行号（1-based）
     * @param column 列号（1-based）
     * @return 偏移量（0-based）
     */
    private static int offsetForPosition(@NotNull String text, int line, int column) {
        int currentLine = 1;
        int offset = 0;
        
        while (currentLine < line && offset < text.length()) {
            if (text.charAt(offset) == '\n') {
                currentLine++;
            }
            offset++;
        }
        
        // 确保不越界
        return Math.min(offset + column - 1, text.length());
    }
    
    /**
     * 生成唯一请求ID
     * @return 请求ID
     */
    @NotNull
    private static String generateRequestId() {
        return "completion_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000);
    }
    
    /**
     * 获取补全请求的调试信息
     * @param request 补全请求
     * @return 调试信息字符串
     */
    @NotNull
    public static String getDebugInfo(@NotNull CompletionRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("CompletionRequest Debug Info:\n");
        builder.append("- File: ").append(request.getFileName()).append("\n");
        builder.append("- Position: Line ").append(request.getLine())
               .append(", Column ").append(request.getColumn()).append("\n");
        
        // 检查上下文是否为null，避免空指针异常
        if (request.getContext() != null) {
            try {
                builder.append("- Context Type: ").append(request.getContext().getContextType()).append("\n");
                builder.append("- Prefix: '").append(request.getContext().getPrefix()).append("'\n");
            } catch (Exception e) {
                // 忽略可能的异常，继续输出其他信息
                builder.append("- Context: Error accessing context properties\n");
            }
        } else {
            builder.append("- Context: null\n");
        }
        
        // 简化选项输出，避免访问不存在的方法
        builder.append("- Options: Default options\n");
        
        return builder.toString();
    }
}