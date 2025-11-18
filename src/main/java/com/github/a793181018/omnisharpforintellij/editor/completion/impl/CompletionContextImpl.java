package com.github.a793181018.omnisharpforintellij.editor.completion.impl;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 代码补全上下文的实现类
 */
public class CompletionContextImpl implements CompletionProvider.CompletionContext {
    private final PsiElement element;
    private final int offset;
    private final String currentLine;
    private final String prefix;
    private final String filePath;
    private final ContextType contextType;
    
    public CompletionContextImpl(@NotNull PsiElement element, int offset) {
        this.element = element;
        this.offset = offset;
        
        // 获取当前行文本
        this.currentLine = getCurrentLine(element, offset);
        
        // 获取光标前的前缀
        this.prefix = extractPrefix(currentLine, offset);
        
        // 获取文件路径
        PsiFile file = element.getContainingFile();
        this.filePath = file != null && file.getVirtualFile() != null 
                ? file.getVirtualFile().getPath() 
                : "";
        
        // 确定上下文类型
        this.contextType = determineContextType(element, offset);
    }
    
    @NotNull
    @Override
    public String getCurrentLine() {
        return currentLine;
    }
    
    @NotNull
    @Override
    public String getPrefix() {
        return prefix;
    }
    
    @Override
    public int getOffset() {
        return offset;
    }
    
    @NotNull
    @Override
    public String getFilePath() {
        return filePath;
    }
    
    @NotNull
    @Override
    public ContextType getContextType() {
        return contextType;
    }
    
    /**
     * 获取当前行的文本
     */
    private String getCurrentLine(@NotNull PsiElement element, int offset) {
        PsiFile file = element.getContainingFile();
        if (file == null || file.getVirtualFile() == null) {
            return "";
        }
        
        Document document = EditorFactory.getInstance().createDocument(file.getText());
        int lineNumber = document.getLineNumber(offset);
        int startOffset = document.getLineStartOffset(lineNumber);
        int endOffset = document.getLineEndOffset(lineNumber);
        
        return document.getText().substring(startOffset, endOffset);
    }
    
    /**
     * 提取光标前的标识符作为前缀
     */
    private String extractPrefix(String currentLine, int documentOffset) {
        int lineStartOffset = documentOffset - currentLine.length();
        int positionInLine = documentOffset - lineStartOffset;
        
        // 从光标位置向前查找非标识符字符
        int prefixStart = positionInLine;
        while (prefixStart > 0) {
            char c = currentLine.charAt(prefixStart - 1);
            if (Character.isLetterOrDigit(c) || c == '_' || c == '@') {
                prefixStart--;
            } else {
                break;
            }
        }
        
        return currentLine.substring(prefixStart, positionInLine);
    }
    
    /**
     * 确定当前的上下文类型
     */
    private ContextType determineContextType(@NotNull PsiElement element, int offset) {
        // 检查是否在导入语句中
        if (isInImportStatement(element)) {
            return ContextType.IMPORT;
        }
        
        // 检查是否在成员访问中（如 obj. 后的位置）
        if (isInMemberAccess(element, offset)) {
            return ContextType.MEMBER_ACCESS;
        }
        
        // 检查是否在方法参数中
        if (isInMethodParameter(element)) {
            return ContextType.METHOD_PARAMETER;
        }
        
        // 检查是否在变量声明中
        if (isInVariableDeclaration(element)) {
            return ContextType.VARIABLE_DECLARATION;
        }
        
        // 检查是否在类型名称中
        if (isInTypeName(element)) {
            return ContextType.TYPE_NAME;
        }
        
        // 默认返回其他类型
        return ContextType.OTHER;
    }
    
    /**
     * 检查是否在导入语句中
     */
    private boolean isInImportStatement(PsiElement element) {
        // 简单实现：检查是否在using语句中
        String text = element.getContainingFile().getText().substring(0, Math.min(offset, 1000)).toLowerCase();
        return text.contains("using ") && isNearKeyword(element, "using");
    }
    
    /**
     * 检查是否在成员访问中
     */
    private boolean isInMemberAccess(PsiElement element, int offset) {
        // 检查光标前是否有点操作符
        String text = element.getContainingFile().getText();
        for (int i = offset - 1; i >= 0 && i >= offset - 10; i--) {
            if (text.charAt(i) == '.') {
                return true;
            } else if (!Character.isWhitespace(text.charAt(i))) {
                break;
            }
        }
        return false;
    }
    
    /**
     * 检查是否在方法参数中
     */
    private boolean isInMethodParameter(PsiElement element) {
        // 检查是否在括号内且前面有方法名和括号
        String text = element.getContainingFile().getText();
        int openParenCount = 0;
        for (int i = offset - 1; i >= 0 && i >= offset - 100; i--) {
            char c = text.charAt(i);
            if (c == ')') {
                openParenCount++;
            } else if (c == '(') {
                if (openParenCount > 0) {
                    openParenCount--;
                } else {
                    // 找到匹配的左括号，检查是否前面是方法调用
                    return isPrecededByIdentifier(text, i - 1);
                }
            }
        }
        return false;
    }
    
    /**
     * 检查是否在变量声明中
     */
    private boolean isInVariableDeclaration(PsiElement element) {
        // 简单实现：检查是否在声明关键字附近
        return isNearKeyword(element, "var") || 
               isNearKeyword(element, "int") || 
               isNearKeyword(element, "string") ||
               isNearKeyword(element, "bool") ||
               isNearKeyword(element, "double") ||
               isNearKeyword(element, "float") ||
               isNearKeyword(element, "char") ||
               isNearKeyword(element, "long") ||
               isNearKeyword(element, "short") ||
               isNearKeyword(element, "byte");
    }
    
    /**
     * 检查是否在类型名称中
     */
    private boolean isInTypeName(PsiElement element) {
        // 检查是否在类、接口、枚举等声明中
        return isNearKeyword(element, "class") || 
               isNearKeyword(element, "interface") ||
               isNearKeyword(element, "enum") ||
               isNearKeyword(element, "struct") ||
               isNearKeyword(element, "namespace");
    }
    
    /**
     * 检查是否在指定关键字附近
     */
    private boolean isNearKeyword(PsiElement element, String keyword) {
        String text = element.getText();
        return text.toLowerCase().contains(keyword);
    }
    
    /**
     * 检查指定位置前是否有标识符
     */
    private boolean isPrecededByIdentifier(String text, int position) {
        // 向前查找标识符
        StringBuilder identifier = new StringBuilder();
        for (int i = position; i >= 0; i--) {
            char c = text.charAt(i);
            if (Character.isLetterOrDigit(c) || c == '_') {
                identifier.append(c);
            } else {
                break;
            }
        }
        
        // 反转并检查是否是有效的标识符
        String reversedIdentifier = identifier.reverse().toString();
        return !reversedIdentifier.isEmpty() && Character.isJavaIdentifierStart(reversedIdentifier.charAt(0));
    }
    
    @Override
    public String toString() {
        return "CompletionContextImpl{" +
                "prefix='" + prefix + '\'' +
                ", contextType=" + contextType +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}