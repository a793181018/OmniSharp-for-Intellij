package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OmniSharp诊断过滤器类
 * 根据配置过滤诊断结果
 */
public class OmniSharpDiagnosticsFilter {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsFilter.class);
    
    private final OmniSharpDiagnosticsConfig config;
    
    public OmniSharpDiagnosticsFilter(@NotNull OmniSharpDiagnosticsConfig config) {
        this.config = config;
        LOG.info("OmniSharpDiagnosticsFilter initialized");
    }
    
    /**
     * 过滤诊断结果
     * @param diagnostics 原始诊断列表
     * @return 过滤后的诊断列表
     */
    @NotNull
    public List<Diagnostic> filterDiagnostics(@NotNull List<Diagnostic> diagnostics) {
        if (!config.isEnabled()) {
            LOG.debug("Diagnostics disabled, returning empty list");
            return Collections.emptyList();
        }
        
        List<Diagnostic> filtered = diagnostics.stream()
            .filter(Objects::nonNull)
            .filter(this::filterByEnabled)
            .filter(this::filterBySeverity)
            .filter(this::filterByCategory)
            .filter(this::filterByCode)
            .filter(this::filterByCustomRules)
            .filter(this::filterByMaxCount)
            .collect(Collectors.toList());
        
        LOG.debug("Filtered " + diagnostics.size() + " diagnostics to " + filtered.size() + " diagnostics");
        return filtered;
    }
    
    /**
     * 根据启用状态过滤
     */
    private boolean filterByEnabled(@NotNull Diagnostic diagnostic) {
        // 检查语法诊断
        if (isSyntaxDiagnostic(diagnostic) && !config.isSyntaxDiagnosticsEnabled()) {
            LOG.trace("Filtered out syntax diagnostic: " + diagnostic.getId());
            return false;
        }
        
        // 检查语义诊断
        if (isSemanticDiagnostic(diagnostic) && !config.isSemanticDiagnosticsEnabled()) {
            LOG.trace("Filtered out semantic diagnostic: " + diagnostic.getId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据严重级别过滤
     */
    private boolean filterBySeverity(@NotNull Diagnostic diagnostic) {
        Diagnostic.DiagnosticSeverity severity = diagnostic.getSeverity();
        
        switch (severity) {
            case ERROR:
                if (!config.isShowErrors()) {
                    LOG.trace("Filtered out error diagnostic: " + diagnostic.getId());
                    return false;
                }
                break;
            case WARNING:
                if (!config.isShowWarnings()) {
                    LOG.trace("Filtered out warning diagnostic: " + diagnostic.getId());
                    return false;
                }
                break;
            case INFO:
                if (!config.isShowInfo()) {
                    LOG.trace("Filtered out info diagnostic: " + diagnostic.getId());
                    return false;
                }
                break;
            case HINT:
                if (!config.isShowHints()) {
                    LOG.trace("Filtered out hint diagnostic: " + diagnostic.getId());
                    return false;
                }
                break;
        }
        
        return true;
    }
    
    /**
     * 根据分类过滤
     */
    private boolean filterByCategory(@NotNull Diagnostic diagnostic) {
        Set<String> enabledCategories = config.getEnabledCategories();
        if (enabledCategories.isEmpty()) {
            return true;
        }
        
        String category = getDiagnosticCategory(diagnostic);
        if (category != null && !enabledCategories.contains(category)) {
            LOG.trace("Filtered out diagnostic by category: " + category + " - " + diagnostic.getId());
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据诊断代码过滤
     */
    private boolean filterByCode(@NotNull Diagnostic diagnostic) {
        String code = diagnostic.getId();
        if (code == null) {
            return true;
        }
        
        // 检查排除列表
        Set<String> excludedCodes = config.getExcludedDiagnosticCodes();
        if (excludedCodes.contains(code)) {
            LOG.trace("Filtered out diagnostic by excluded code: " + code);
            return false;
        }
        
        // 检查包含列表（如果非空）
        Set<String> includedCodes = config.getIncludedDiagnosticCodes();
        if (!includedCodes.isEmpty() && !includedCodes.contains(code)) {
            LOG.trace("Filtered out diagnostic by included code: " + code);
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据自定义规则过滤
     */
    private boolean filterByCustomRules(@NotNull Diagnostic diagnostic) {
        String code = diagnostic.getId();
        if (code == null) {
            return true;
        }
        
        OmniSharpDiagnosticsConfig.State.DiagnosticRule rule = config.getCustomRule(code);
        if (rule != null) {
            if (!rule.enabled) {
                LOG.trace("Filtered out diagnostic by custom rule (disabled): " + code);
                return false;
            }
            
            // 如果规则指定了严重级别，检查是否匹配
            if (rule.severity != null) {
                String ruleSeverity = rule.severity.toUpperCase();
                String diagnosticSeverity = diagnostic.getSeverity().name();
                if (!ruleSeverity.equals(diagnosticSeverity)) {
                    LOG.trace("Filtered out diagnostic by custom rule severity: " + code + " (rule: " + ruleSeverity + ", actual: " + diagnosticSeverity + ")");
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 根据最大数量过滤
     */
    private boolean filterByMaxCount(@NotNull Diagnostic diagnostic) {
        // 这个过滤器在应用其他过滤器后使用，由调用者控制总数
        return true;
    }
    
    /**
     * 检查是否为语法诊断
     */
    private boolean isSyntaxDiagnostic(@NotNull Diagnostic diagnostic) {
        String code = diagnostic.getId();
        if (code == null) {
            return false;
        }
        
        // 根据诊断代码判断是否为语法诊断
        return code.startsWith("CS1") || // C# 语法错误通常以CS1开头
               code.startsWith("CS0") || // 一些语法警告
               code.contains("syntax") ||
               code.contains("parse");
    }
    
    /**
     * 检查是否为语义诊断
     */
    private boolean isSemanticDiagnostic(@NotNull Diagnostic diagnostic) {
        String code = diagnostic.getId();
        if (code == null) {
            return false;
        }
        
        // 根据诊断代码判断是否为语义诊断
        return code.startsWith("CS") && !isSyntaxDiagnostic(diagnostic) ||
               code.contains("semantic") ||
               code.contains("type") ||
               code.contains("reference");
    }
    
    /**
     * 获取诊断分类
     */
    @Nullable
    private String getDiagnosticCategory(@NotNull Diagnostic diagnostic) {
        String code = diagnostic.getId();
        if (code == null) {
            return null;
        }
        
        // 根据诊断代码判断分类
        if (code.startsWith("CS1")) {
            return "SYNTAX";
        } else if (code.startsWith("CS")) {
            return "SEMANTIC";
        } else if (code.contains("declaration")) {
            return "DECLARATION";
        } else if (code.contains("unnecessary")) {
            return "UNNECESSARY";
        } else if (code.contains("deprecated")) {
            return "DEPRECATION";
        }
        
        return null;
    }
    
    /**
     * 根据最大数量限制过滤诊断列表
     * @param diagnostics 诊断列表
     * @param maxCount 最大数量
     * @return 限制后的诊断列表
     */
    @NotNull
    public List<Diagnostic> limitDiagnosticsByCount(@NotNull List<Diagnostic> diagnostics, int maxCount) {
        if (diagnostics.size() <= maxCount) {
            return diagnostics;
        }
        
        // 优先显示错误，然后是警告，最后是信息和提示
        List<Diagnostic> limited = new ArrayList<>();
        
        // 先添加错误
        List<Diagnostic> errors = diagnostics.stream()
            .filter(d -> d.getSeverity() == Diagnostic.DiagnosticSeverity.ERROR)
            .limit(maxCount)
            .collect(Collectors.toList());
        limited.addAll(errors);
        
        if (limited.size() < maxCount) {
            // 然后添加警告
            List<Diagnostic> warnings = diagnostics.stream()
                .filter(d -> d.getSeverity() == Diagnostic.DiagnosticSeverity.WARNING)
                .limit(maxCount - limited.size())
                .collect(Collectors.toList());
            limited.addAll(warnings);
        }
        
        if (limited.size() < maxCount) {
            // 最后添加信息和提示
            List<Diagnostic> infoAndHints = diagnostics.stream()
                .filter(d -> d.getSeverity() == Diagnostic.DiagnosticSeverity.INFO || d.getSeverity() == Diagnostic.DiagnosticSeverity.HINT)
                .limit(maxCount - limited.size())
                .collect(Collectors.toList());
            limited.addAll(infoAndHints);
        }
        
        LOG.debug("Limited diagnostics from " + diagnostics.size() + " to " + limited.size() + " items");
        return limited;
    }
    
    /**
     * 应用最大数量限制到诊断列表
     * @param diagnostics 诊断列表
     * @return 应用限制后的诊断列表
     */
    @NotNull
    public List<Diagnostic> applyMaxCountLimit(@NotNull List<Diagnostic> diagnostics) {
        int maxCount = config.getMaxDiagnosticsPerFile();
        return limitDiagnosticsByCount(diagnostics, maxCount);
    }
    
    /**
     * 检查诊断是否应该被过滤
     * @param diagnostic 诊断
     * @return 是否应该被过滤
     */
    public boolean shouldFilterDiagnostic(@NotNull Diagnostic diagnostic) {
        List<Diagnostic> singleDiagnostic = Collections.singletonList(diagnostic);
        List<Diagnostic> filtered = filterDiagnostics(singleDiagnostic);
        return filtered.isEmpty();
    }
    
    /**
     * 获取过滤器统计信息
     * @param originalCount 原始诊断数量
     * @param filteredCount 过滤后诊断数量
     * @return 统计信息映射
     */
    @NotNull
    public Map<String, Object> getFilterStatistics(int originalCount, int filteredCount) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("originalCount", originalCount);
        stats.put("filteredCount", filteredCount);
        stats.put("filteredOutCount", originalCount - filteredCount);
        stats.put("filterRate", originalCount > 0 ? (double) (originalCount - filteredCount) / originalCount : 0.0);
        return stats;
    }
    
    /**
     * 重置过滤器配置
     */
    public void reset() {
        LOG.info("Resetting diagnostics filter configuration");
    }
}