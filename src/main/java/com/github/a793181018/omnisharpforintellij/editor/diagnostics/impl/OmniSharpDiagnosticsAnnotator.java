package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.List;
import java.util.*;

/**
 * OmniSharp诊断标记显示类
 * 负责在编辑器中显示诊断结果
 */
public class OmniSharpDiagnosticsAnnotator implements Annotator {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsAnnotator.class);
    
    // 诊断标记缓存
    private final Map<VirtualFile, List<DiagnosticAnnotation>> annotationCache = new WeakHashMap<>();
    
    // 诊断标记类
    private static class DiagnosticAnnotation {
        final Diagnostic diagnostic;
        final TextRange range;
        final HighlightSeverity severity;
        final TextAttributesKey textAttributes;
        
        DiagnosticAnnotation(@NotNull Diagnostic diagnostic, @NotNull TextRange range, 
                           @NotNull HighlightSeverity severity, @NotNull TextAttributesKey textAttributes) {
            this.diagnostic = diagnostic;
            this.range = range;
            this.severity = severity;
            this.textAttributes = textAttributes;
        }
    }
    
    @Override
    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        // 确保元素是PsiFile
        if (!(element instanceof PsiFile)) {
            return;
        }
        
        PsiFile file = (PsiFile) element;
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile == null) {
            return;
        }
        
        // 获取诊断服务
        Project project = file.getProject();
        OmniSharpDiagnosticsService diagnosticsService = project.getService(OmniSharpDiagnosticsService.class);
        if (diagnosticsService == null) {
            LOG.warn("OmniSharpDiagnosticsService not found");
            return;
        }
        
        // 检查是否启用编辑器诊断显示
        if (!diagnosticsService.getConfig().isShowDiagnosticsInEditor()) {
            return;
        }
        
        // 获取诊断结果
        List<Diagnostic> diagnostics = getCachedDiagnostics(virtualFile, diagnosticsService);
        if (diagnostics == null || diagnostics.isEmpty()) {
            return;
        }
        
        // 创建诊断标记
        createDiagnosticAnnotations(file, holder, diagnostics);
    }
    
    /**
     * 获取缓存的诊断结果
     */
    @Nullable
    private List<Diagnostic> getCachedDiagnostics(@NotNull VirtualFile file, @NotNull OmniSharpDiagnosticsService diagnosticsService) {
        // 尝试从缓存获取
        List<DiagnosticAnnotation> cachedAnnotations = annotationCache.get(file);
        if (cachedAnnotations != null) {
            return cachedAnnotations.stream().map(a -> a.diagnostic).collect(java.util.stream.Collectors.toList());
        }
        
        // 如果缓存不存在，异步获取诊断结果
        diagnosticsService.updateDiagnostics(file)
            .whenComplete((diagnostics, throwable) -> {
                if (throwable != null) {
                    LOG.error("Failed to get diagnostics for file: " + file.getPath(), throwable);
                } else if (diagnostics != null && !diagnostics.isEmpty()) {
                    // 重新触发注解
                    ApplicationManager.getApplication().invokeLater(() -> {
                        Project project = diagnosticsService.getProject();
                        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
                        if (psiFile != null) {
                            DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
                        }
                    });
                }
            });
        
        return null;
    }
    
    /**
     * 创建诊断标记
     */
    private void createDiagnosticAnnotations(@NotNull PsiFile file, @NotNull AnnotationHolder holder, @NotNull List<Diagnostic> diagnostics) {
        List<DiagnosticAnnotation> annotations = new ArrayList<>();
        
        for (Diagnostic diagnostic : diagnostics) {
            try {
                // 计算文本范围
                TextRange range = calculateTextRange(file, diagnostic);
                if (range == null || range.isEmpty()) {
                    continue;
                }
                
                // 获取严重级别和文本属性
                HighlightSeverity severity = getHighlightSeverity(diagnostic.getSeverity());
                TextAttributesKey textAttributes = getTextAttributesKey(diagnostic.getSeverity());
                
                // 创建注解
                holder.newAnnotation(severity, diagnostic.getMessage())
                    .range(range)
                    .textAttributes(textAttributes)
                    .tooltip(createTooltipText(diagnostic))
                    .create();
                
                // 添加代码修复（如果可用）
                addCodeFixes(holder, diagnostic, range);
                
                // 缓存注解信息
                annotations.add(new DiagnosticAnnotation(diagnostic, range, severity, textAttributes));
                
                LOG.debug("Created diagnostic annotation: " + diagnostic.getId() + " at " + range);
                
            } catch (Exception e) {
                LOG.error("Error creating diagnostic annotation for: " + diagnostic.getId(), e);
            }
        }
        
        // 更新缓存
        if (!annotations.isEmpty()) {
            annotationCache.put(file.getVirtualFile(), annotations);
        }
    }
    
    /**
     * 计算诊断的文本范围
     */
    @Nullable
    private TextRange calculateTextRange(@NotNull PsiFile file, @NotNull Diagnostic diagnostic) {
        try {
            int startLine = diagnostic.getStartLine();
            int startColumn = diagnostic.getStartColumn();
            int endLine = diagnostic.getEndLine();
            int endColumn = diagnostic.getEndColumn();
            
            // 如果只有起始位置，尝试计算结束位置
            if (endLine == -1 || endColumn == -1) {
                endLine = startLine;
                endColumn = startColumn + 1; // 默认长度为1
            }
            
            // 将行号列号转换为文档偏移量
            int startOffset = calculateOffset(file, startLine, startColumn);
            int endOffset = calculateOffset(file, endLine, endColumn);
            
            if (startOffset >= 0 && endOffset >= 0 && startOffset <= endOffset) {
                return new TextRange(startOffset, endOffset);
            }
            
        } catch (Exception e) {
            LOG.warn("Error calculating text range for diagnostic: " + diagnostic.getId(), e);
        }
        
        return null;
    }
    
    /**
     * 计算偏移量
     */
    private int calculateOffset(@NotNull PsiFile file, int line, int column) {
        try {
            String text = file.getText();
            String[] lines = text.split("\n", -1);
            
            if (line < 0 || line >= lines.length) {
                return -1;
            }
            
            int offset = 0;
            for (int i = 0; i < line; i++) {
                offset += lines[i].length() + 1; // +1 for newline
            }
            
            if (column < 0 || column > lines[line].length()) {
                column = 0;
            }
            
            return offset + column;
            
        } catch (Exception e) {
            LOG.error("Error calculating offset", e);
            return -1;
        }
    }
    
    /**
     * 获取高亮严重级别
     */
    @NotNull
    private HighlightSeverity getHighlightSeverity(@NotNull Diagnostic.DiagnosticSeverity severity) {
        switch (severity) {
            case ERROR:
                return HighlightSeverity.ERROR;
            case WARNING:
                return HighlightSeverity.WARNING;
            case INFO:
                return HighlightSeverity.INFORMATION;
            case HINT:
                return HighlightSeverity.WEAK_WARNING;
            default:
                return HighlightSeverity.INFORMATION;
        }
    }
    
    /**
     * 获取文本属性键
     */
    @NotNull
    private TextAttributesKey getTextAttributesKey(@NotNull Diagnostic.DiagnosticSeverity severity) {
        switch (severity) {
            case ERROR:
                return CodeInsightColors.ERRORS_ATTRIBUTES;
            case WARNING:
                return CodeInsightColors.WARNINGS_ATTRIBUTES;
            case INFO:
                return CodeInsightColors.INFO_ATTRIBUTES;
            case HINT:
                return CodeInsightColors.INFO_ATTRIBUTES; // 使用INFO_ATTRIBUTES替代BLOCK_COMMENT_ATTRIBUTES
            default:
                return CodeInsightColors.INFO_ATTRIBUTES;
        }
    }
    
    /**
     * 创建工具提示文本
     */
    @NotNull
    private String createTooltipText(@NotNull Diagnostic diagnostic) {
        StringBuilder tooltip = new StringBuilder();
        
        // 严重级别
        tooltip.append("<b>").append(diagnostic.getSeverity()).append("</b><br/>");
        
        // 诊断代码
        if (diagnostic.getId() != null) {
            tooltip.append("<b>Code:</b> ").append(diagnostic.getId()).append("<br/>");
        }
        
        // 消息
        tooltip.append("<b>Message:</b> ").append(diagnostic.getMessage()).append("<br/>");
        
        // 位置信息
        if (diagnostic.getStartLine() >= 0 && diagnostic.getStartColumn() >= 0) {
            tooltip.append("<b>Location:</b> Line ").append(diagnostic.getStartLine() + 1)
                   .append(", Column ").append(diagnostic.getStartColumn() + 1);
        }
        
        return tooltip.toString();
    }
    
    /**
     * 添加代码修复
     */
    private void addCodeFixes(@NotNull AnnotationHolder holder, @NotNull Diagnostic diagnostic, @NotNull TextRange range) {
        // 获取诊断服务
        // 注意：这里需要从上下文中获取项目，然后获取诊断服务
        // 由于这是一个私有方法，我们假设调用者已经验证了配置
        // if (!diagnosticsService.getConfig().isShowCodeFixes()) {
        //     return;
        // }
        
        // 这里可以添加代码修复逻辑
        // 例如：从diagnostic中获取CodeFix信息并创建IntentionAction
        
        // 示例：添加快速修复意图
        // if (diagnostic.getCodeFixes() != null && !diagnostic.getCodeFixes().isEmpty()) {
        //     for (CodeFix codeFix : diagnostic.getCodeFixes()) {
        //         holder.newFix(new OmniSharpCodeFixIntention(codeFix))
        //             .range(range)
        //             .registerFix();
        //     }
        // }
    }
    
    /**
     * 更新文件的诊断标记
     */
    public void updateDiagnosticAnnotations(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics, @NotNull Project project) {
        // 清除缓存
        annotationCache.remove(file);
        
        // 重新触发注解
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
            }
        });
    }
    
    /**
     * 清除指定文件的诊断标记
     */
    public void clearDiagnosticAnnotations(@NotNull VirtualFile file, @NotNull Project project) {
        annotationCache.remove(file);
        
        // 重新触发注解以清除标记
        ApplicationManager.getApplication().invokeLater(() -> {
            PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).restart(psiFile);
            }
        });
    }
    
    /**
     * 清除所有诊断标记缓存
     */
    public void clearAllAnnotations() {
        annotationCache.clear();
    }
    
    /**
     * 获取文件的高亮显示设置
     */
    @NotNull
    public Map<String, Object> getHighlightingSettings(@NotNull VirtualFile file) {
        Map<String, Object> settings = new HashMap<>();
        
        List<DiagnosticAnnotation> annotations = annotationCache.get(file);
        if (annotations != null) {
            settings.put("annotationCount", annotations.size());
            
            Map<Diagnostic.DiagnosticSeverity, Integer> severityCount = new HashMap<>();
        for (DiagnosticAnnotation annotation : annotations) {
            Diagnostic.DiagnosticSeverity severity = annotation.diagnostic.getSeverity();
                severityCount.put(severity, severityCount.getOrDefault(severity, 0) + 1);
            }
            settings.put("severityCount", severityCount);
        } else {
            settings.put("annotationCount", 0);
            settings.put("severityCount", Collections.emptyMap());
        }
        
        return settings;
    }
    
    /**
     * 创建自定义文本属性
     */
    @NotNull
    public TextAttributes createCustomTextAttributes(@NotNull Diagnostic.DiagnosticSeverity severity, @Nullable Color backgroundColor) {
        TextAttributes attributes = new TextAttributes();
        
        // 设置前景色
        switch (severity) {
            case ERROR:
                attributes.setForegroundColor(Color.RED);
                attributes.setEffectColor(Color.RED);
                attributes.setEffectType(EffectType.WAVE_UNDERSCORE);
                break;
            case WARNING:
                attributes.setForegroundColor(Color.ORANGE);
                attributes.setEffectColor(Color.ORANGE);
                attributes.setEffectType(EffectType.WAVE_UNDERSCORE);
                break;
            case INFO:
                attributes.setForegroundColor(Color.BLUE);
                attributes.setEffectColor(Color.BLUE);
                attributes.setEffectType(EffectType.BOLD_DOTTED_LINE);
                break;
            case HINT:
                attributes.setForegroundColor(Color.GRAY);
                attributes.setEffectColor(Color.GRAY);
                attributes.setEffectType(EffectType.BOLD_DOTTED_LINE);
                break;
        }
        
        // 设置背景色
        if (backgroundColor != null) {
            attributes.setBackgroundColor(backgroundColor);
        }
        
        return attributes;
    }
}