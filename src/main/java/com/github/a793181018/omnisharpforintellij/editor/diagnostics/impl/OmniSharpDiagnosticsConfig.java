package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.Disposable;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * OmniSharp诊断配置类
 * 管理诊断相关的配置选项
 */
@State(
    name = "OmniSharpDiagnosticsConfig",
    storages = {@Storage("omnisharp-diagnostics.xml")}
)
public class OmniSharpDiagnosticsConfig implements PersistentStateComponent<OmniSharpDiagnosticsConfig.State>, Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsConfig.class);
    
    private final Project project;
    private State state = new State();
    
    public OmniSharpDiagnosticsConfig(@NotNull Project project) {
        this.project = project;
        LOG.info("OmniSharpDiagnosticsConfig initialized for project: " + project.getName());
    }
    
    /**
     * 配置状态类
     */
    public static class State {
        // 基本诊断设置
        public boolean enabled = true;
        public boolean syntaxDiagnosticsEnabled = true;
        public boolean semanticDiagnosticsEnabled = true;
        public boolean realTimeDiagnosticsEnabled = true;
        
        // 诊断级别过滤
        public boolean showErrors = true;
        public boolean showWarnings = true;
        public boolean showInfo = false;
        public boolean showHints = false;
        
        // 诊断分类过滤
        public Set<String> enabledCategories = new HashSet<>(Arrays.asList(
            "SYNTAX", "SEMANTIC", "DECLARATION", "UNNECESSARY", "DEPRECATION"
        ));
        
        // 诊断代码过滤
        public Set<String> excludedDiagnosticCodes = new HashSet<>();
        public Set<String> includedDiagnosticCodes = new HashSet<>();
        
        // 诊断显示设置
        public boolean showDiagnosticsInEditor = true;
        public boolean showDiagnosticsInProblemsView = true;
        public boolean highlightDiagnostics = true;
        public boolean showCodeFixes = true;
        
        // 性能设置
        public int maxDiagnosticsPerFile = 1000;
        public int diagnosticUpdateDelayMs = 500;
        public boolean enableDiagnosticsCache = true;
        public long diagnosticsCacheExpirationMs = 30000;
        
        // 高级设置
        public boolean analyzeOpenFilesOnly = false;
        public boolean analyzeOnSave = true;
        public boolean analyzeOnType = false;
        public boolean analyzeOnFileChange = true;
        
        // 自定义规则
        public Map<String, DiagnosticRule> customRules = new HashMap<>();
        
        // 诊断规则类
        public static class DiagnosticRule {
            public String code;
            public String message;
            public String severity; // ERROR, WARNING, INFO, HINT
            public boolean enabled = true;
            public String category;
            public Map<String, String> options = new HashMap<>();
            
            public DiagnosticRule() {}
            
            public DiagnosticRule(String code, String severity, boolean enabled) {
                this.code = code;
                this.severity = severity;
                this.enabled = enabled;
            }
        }
    }
    
    @Override
    @Nullable
    public State getState() {
        return state;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, this.state);
        LOG.info("Loaded diagnostics configuration state");
    }
    
    /**
     * 获取项目
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    /**
     * 是否启用诊断
     */
    public boolean isEnabled() {
        return state.enabled;
    }
    
    /**
     * 设置是否启用诊断
     */
    public void setEnabled(boolean enabled) {
        state.enabled = enabled;
        LOG.info("Diagnostics enabled: " + enabled);
    }
    
    /**
     * 是否启用语法诊断
     */
    public boolean isSyntaxDiagnosticsEnabled() {
        return state.syntaxDiagnosticsEnabled;
    }
    
    /**
     * 设置是否启用语法诊断
     */
    public void setSyntaxDiagnosticsEnabled(boolean enabled) {
        state.syntaxDiagnosticsEnabled = enabled;
        LOG.info("Syntax diagnostics enabled: " + enabled);
    }
    
    /**
     * 是否启用语义诊断
     */
    public boolean isSemanticDiagnosticsEnabled() {
        return state.semanticDiagnosticsEnabled;
    }
    
    /**
     * 设置是否启用语义诊断
     */
    public void setSemanticDiagnosticsEnabled(boolean enabled) {
        state.semanticDiagnosticsEnabled = enabled;
        LOG.info("Semantic diagnostics enabled: " + enabled);
    }
    
    /**
     * 是否启用实时诊断
     */
    public boolean isRealTimeDiagnosticsEnabled() {
        return state.realTimeDiagnosticsEnabled;
    }
    
    /**
     * 设置是否启用实时诊断
     */
    public void setRealTimeDiagnosticsEnabled(boolean enabled) {
        state.realTimeDiagnosticsEnabled = enabled;
        LOG.info("Real-time diagnostics enabled: " + enabled);
    }
    
    /**
     * 是否显示错误
     */
    public boolean isShowErrors() {
        return state.showErrors;
    }
    
    /**
     * 设置是否显示错误
     */
    public void setShowErrors(boolean showErrors) {
        state.showErrors = showErrors;
        LOG.info("Show errors: " + showErrors);
    }
    
    /**
     * 是否显示警告
     */
    public boolean isShowWarnings() {
        return state.showWarnings;
    }
    
    /**
     * 设置是否显示警告
     */
    public void setShowWarnings(boolean showWarnings) {
        state.showWarnings = showWarnings;
        LOG.info("Show warnings: " + showWarnings);
    }
    
    /**
     * 是否显示信息
     */
    public boolean isShowInfo() {
        return state.showInfo;
    }
    
    /**
     * 设置是否显示信息
     */
    public void setShowInfo(boolean showInfo) {
        state.showInfo = showInfo;
        LOG.info("Show info: " + showInfo);
    }
    
    /**
     * 是否显示提示
     */
    public boolean isShowHints() {
        return state.showHints;
    }
    
    /**
     * 设置是否显示提示
     */
    public void setShowHints(boolean showHints) {
        state.showHints = showHints;
        LOG.info("Show hints: " + showHints);
    }
    
    /**
     * 获取启用的诊断分类
     */
    @NotNull
    public Set<String> getEnabledCategories() {
        return new HashSet<>(state.enabledCategories);
    }
    
    /**
     * 设置启用的诊断分类
     */
    public void setEnabledCategories(@NotNull Set<String> categories) {
        state.enabledCategories = new HashSet<>(categories);
        LOG.info("Enabled categories: " + categories);
    }
    
    /**
     * 添加启用的诊断分类
     */
    public void addEnabledCategory(@NotNull String category) {
        state.enabledCategories.add(category);
        LOG.info("Added enabled category: " + category);
    }
    
    /**
     * 移除启用的诊断分类
     */
    public void removeEnabledCategory(@NotNull String category) {
        state.enabledCategories.remove(category);
        LOG.info("Removed enabled category: " + category);
    }
    
    /**
     * 获取排除的诊断代码
     */
    @NotNull
    public Set<String> getExcludedDiagnosticCodes() {
        return new HashSet<>(state.excludedDiagnosticCodes);
    }
    
    /**
     * 设置排除的诊断代码
     */
    public void setExcludedDiagnosticCodes(@NotNull Set<String> codes) {
        state.excludedDiagnosticCodes = new HashSet<>(codes);
        LOG.info("Excluded diagnostic codes: " + codes.size() + " codes");
    }
    
    /**
     * 添加排除的诊断代码
     */
    public void addExcludedDiagnosticCode(@NotNull String code) {
        state.excludedDiagnosticCodes.add(code);
        LOG.info("Added excluded diagnostic code: " + code);
    }
    
    /**
     * 移除排除的诊断代码
     */
    public void removeExcludedDiagnosticCode(@NotNull String code) {
        state.excludedDiagnosticCodes.remove(code);
        LOG.info("Removed excluded diagnostic code: " + code);
    }
    
    /**
     * 获取包含的诊断代码
     */
    @NotNull
    public Set<String> getIncludedDiagnosticCodes() {
        return new HashSet<>(state.includedDiagnosticCodes);
    }
    
    /**
     * 设置包含的诊断代码
     */
    public void setIncludedDiagnosticCodes(@NotNull Set<String> codes) {
        state.includedDiagnosticCodes = new HashSet<>(codes);
        LOG.info("Included diagnostic codes: " + codes.size() + " codes");
    }
    
    /**
     * 是否在编辑器中显示诊断
     */
    public boolean isShowDiagnosticsInEditor() {
        return state.showDiagnosticsInEditor;
    }
    
    /**
     * 设置是否在编辑器中显示诊断
     */
    public void setShowDiagnosticsInEditor(boolean show) {
        state.showDiagnosticsInEditor = show;
        LOG.info("Show diagnostics in editor: " + show);
    }
    
    /**
     * 是否在问题视图中显示诊断
     */
    public boolean isShowDiagnosticsInProblemsView() {
        return state.showDiagnosticsInProblemsView;
    }
    
    /**
     * 设置是否在问题视图中显示诊断
     */
    public void setShowDiagnosticsInProblemsView(boolean show) {
        state.showDiagnosticsInProblemsView = show;
        LOG.info("Show diagnostics in problems view: " + show);
    }
    
    /**
     * 是否高亮诊断
     */
    public boolean isHighlightDiagnostics() {
        return state.highlightDiagnostics;
    }
    
    /**
     * 设置是否高亮诊断
     */
    public void setHighlightDiagnostics(boolean highlight) {
        state.highlightDiagnostics = highlight;
        LOG.info("Highlight diagnostics: " + highlight);
    }
    
    /**
     * 是否显示代码修复
     */
    public boolean isShowCodeFixes() {
        return state.showCodeFixes;
    }
    
    /**
     * 设置是否显示代码修复
     */
    public void setShowCodeFixes(boolean show) {
        state.showCodeFixes = show;
        LOG.info("Show code fixes: " + show);
    }
    
    /**
     * 获取每文件最大诊断数
     */
    public int getMaxDiagnosticsPerFile() {
        return state.maxDiagnosticsPerFile;
    }
    
    /**
     * 设置每文件最大诊断数
     */
    public void setMaxDiagnosticsPerFile(int max) {
        state.maxDiagnosticsPerFile = Math.max(1, max);
        LOG.info("Max diagnostics per file: " + state.maxDiagnosticsPerFile);
    }
    
    /**
     * 获取诊断更新延迟（毫秒）
     */
    public int getDiagnosticUpdateDelayMs() {
        return state.diagnosticUpdateDelayMs;
    }
    
    /**
     * 设置诊断更新延迟（毫秒）
     */
    public void setDiagnosticUpdateDelayMs(int delay) {
        state.diagnosticUpdateDelayMs = Math.max(100, delay);
        LOG.info("Diagnostic update delay: " + state.diagnosticUpdateDelayMs + "ms");
    }
    
    /**
     * 是否启用诊断缓存
     */
    public boolean isEnableDiagnosticsCache() {
        return state.enableDiagnosticsCache;
    }
    
    /**
     * 设置是否启用诊断缓存
     */
    public void setEnableDiagnosticsCache(boolean enable) {
        state.enableDiagnosticsCache = enable;
        LOG.info("Enable diagnostics cache: " + enable);
    }
    
    /**
     * 获取诊断缓存过期时间（毫秒）
     */
    public long getDiagnosticsCacheExpirationMs() {
        return state.diagnosticsCacheExpirationMs;
    }
    
    /**
     * 设置诊断缓存过期时间（毫秒）
     */
    public void setDiagnosticsCacheExpirationMs(long expiration) {
        state.diagnosticsCacheExpirationMs = Math.max(5000, expiration);
        LOG.info("Diagnostics cache expiration: " + state.diagnosticsCacheExpirationMs + "ms");
    }
    
    /**
     * 是否只分析打开的文件
     */
    public boolean isAnalyzeOpenFilesOnly() {
        return state.analyzeOpenFilesOnly;
    }
    
    /**
     * 设置是否只分析打开的文件
     */
    public void setAnalyzeOpenFilesOnly(boolean onlyOpenFiles) {
        state.analyzeOpenFilesOnly = onlyOpenFiles;
        LOG.info("Analyze open files only: " + onlyOpenFiles);
    }
    
    /**
     * 是否在保存时分析
     */
    public boolean isAnalyzeOnSave() {
        return state.analyzeOnSave;
    }
    
    /**
     * 设置是否在保存时分析
     */
    public void setAnalyzeOnSave(boolean analyze) {
        state.analyzeOnSave = analyze;
        LOG.info("Analyze on save: " + analyze);
    }
    
    /**
     * 是否在输入时分析
     */
    public boolean isAnalyzeOnType() {
        return state.analyzeOnType;
    }
    
    /**
     * 设置是否在输入时分析
     */
    public void setAnalyzeOnType(boolean analyze) {
        state.analyzeOnType = analyze;
        LOG.info("Analyze on type: " + analyze);
    }
    
    /**
     * 是否在文件改变时分析
     */
    public boolean isAnalyzeOnFileChange() {
        return state.analyzeOnFileChange;
    }
    
    /**
     * 设置是否在文件改变时分析
     */
    public void setAnalyzeOnFileChange(boolean analyze) {
        state.analyzeOnFileChange = analyze;
        LOG.info("Analyze on file change: " + analyze);
    }
    
    /**
     * 获取自定义诊断规则
     */
    @NotNull
    public Map<String, State.DiagnosticRule> getCustomRules() {
        return new HashMap<>(state.customRules);
    }
    
    /**
     * 设置自定义诊断规则
     */
    public void setCustomRules(@NotNull Map<String, State.DiagnosticRule> rules) {
        state.customRules = new HashMap<>(rules);
        LOG.info("Set " + rules.size() + " custom diagnostic rules");
    }
    
    /**
     * 添加自定义诊断规则
     */
    public void addCustomRule(@NotNull String code, @NotNull State.DiagnosticRule rule) {
        state.customRules.put(code, rule);
        LOG.info("Added custom diagnostic rule: " + code);
    }
    
    /**
     * 移除自定义诊断规则
     */
    public void removeCustomRule(@NotNull String code) {
        state.customRules.remove(code);
        LOG.info("Removed custom diagnostic rule: " + code);
    }
    
    /**
     * 获取指定代码的自定义规则
     */
    @Nullable
    public State.DiagnosticRule getCustomRule(@NotNull String code) {
        return state.customRules.get(code);
    }
    
    /**
     * 重置为默认配置
     */
    public void resetToDefaults() {
        state = new State();
        LOG.info("Reset diagnostics configuration to defaults");
    }
    
    /**
     * 获取配置摘要
     */
    @NotNull
    public Map<String, Object> getConfigurationSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("enabled", state.enabled);
        summary.put("syntaxDiagnosticsEnabled", state.syntaxDiagnosticsEnabled);
        summary.put("semanticDiagnosticsEnabled", state.semanticDiagnosticsEnabled);
        summary.put("realTimeDiagnosticsEnabled", state.realTimeDiagnosticsEnabled);
        summary.put("showErrors", state.showErrors);
        summary.put("showWarnings", state.showWarnings);
        summary.put("showInfo", state.showInfo);
        summary.put("showHints", state.showHints);
        summary.put("enabledCategoriesCount", state.enabledCategories.size());
        summary.put("excludedCodesCount", state.excludedDiagnosticCodes.size());
        summary.put("includedCodesCount", state.includedDiagnosticCodes.size());
        summary.put("maxDiagnosticsPerFile", state.maxDiagnosticsPerFile);
        summary.put("diagnosticUpdateDelayMs", state.diagnosticUpdateDelayMs);
        summary.put("enableDiagnosticsCache", state.enableDiagnosticsCache);
        summary.put("customRulesCount", state.customRules.size());
        return summary;
    }
    
    @Override
    public void dispose() {
        LOG.info("OmniSharpDiagnosticsConfig disposed");
    }
}