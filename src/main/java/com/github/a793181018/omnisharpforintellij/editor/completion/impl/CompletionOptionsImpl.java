package com.github.a793181018.omnisharpforintellij.editor.completion.impl;

import com.github.a793181018.omnisharpforintellij.editor.completion.CompletionProvider;
import org.jetbrains.annotations.NotNull;

/**
 * 代码补全选项的实现类
 */
public class CompletionOptionsImpl implements CompletionProvider.CompletionOptions {
    private boolean includeImportStatements = true;
    private boolean includeSnippets = true;
    private boolean includeKeywords = true;
    private boolean includeMemberAccessCompletions = true;
    private boolean includeTypeCompletions = true;
    private int maxResults = 100;
    private boolean caseSensitive = false;
    private boolean useCachedResults = true;
    private boolean sortResults = true;
    private boolean methodParameterHintsEnabled = true;
    private boolean smartCompletionEnabled = true;
    
    public boolean includeImportStatements() {
        return includeImportStatements;
    }
    
    public boolean includeSnippets() {
        return includeSnippets;
    }
    
    public boolean includeKeywords() {
        return includeKeywords;
    }
    
    public boolean includeMemberAccessCompletions() {
        return includeMemberAccessCompletions;
    }
    
    public boolean includeTypeCompletions() {
        return includeTypeCompletions;
    }
    
    public int maxResults() {
        return maxResults;
    }
    
    public boolean caseSensitive() {
        return caseSensitive;
    }
    
    @Override
    public boolean isSortResults() {
        return sortResults;
    }
    
    @Override
    public void setSortResults(boolean sortResults) {
        this.sortResults = sortResults;
    }
    
    @Override
    public boolean isMethodParameterHintsEnabled() {
        return methodParameterHintsEnabled;
    }
    
    @Override
    public void setMethodParameterHintsEnabled(boolean enabled) {
        this.methodParameterHintsEnabled = enabled;
    }
    
    @Override
    public boolean isSmartCompletionEnabled() {
        return smartCompletionEnabled;
    }
    
    @Override
    public void setSmartCompletionEnabled(boolean enabled) {
        this.smartCompletionEnabled = enabled;
    }
    
    @Override
    public int getMaxResults() {
        return maxResults;
    }
    
    @Override
    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }
    public boolean useCachedResults() {
        return useCachedResults;
    }
    
    // Builder模式用于创建CompletionOptionsImpl实例
    public static class Builder {
        private final CompletionOptionsImpl options = new CompletionOptionsImpl();
        
        public Builder includeImportStatements(boolean include) {
            options.includeImportStatements = include;
            return this;
        }
        
        public Builder includeSnippets(boolean include) {
            options.includeSnippets = include;
            return this;
        }
        
        public Builder includeKeywords(boolean include) {
            options.includeKeywords = include;
            return this;
        }
        
        public Builder includeMemberAccessCompletions(boolean include) {
            options.includeMemberAccessCompletions = include;
            return this;
        }
        
        public Builder includeTypeCompletions(boolean include) {
            options.includeTypeCompletions = include;
            return this;
        }
        
        public Builder maxResults(int max) {
            options.maxResults = max;
            return this;
        }
        
        public Builder caseSensitive(boolean sensitive) {
            options.caseSensitive = sensitive;
            return this;
        }
        
        public Builder useCachedResults(boolean useCache) {
            options.useCachedResults = useCache;
            return this;
        }
        
        @NotNull
        public CompletionOptionsImpl build() {
            return options;
        }
    }
    
    /**
     * 获取默认的补全选项
     */
    @NotNull
    public static CompletionOptionsImpl getDefaultOptions() {
        return new Builder().build();
    }
    
    @Override
    public String toString() {
        return "CompletionOptionsImpl{" +
                "includeImportStatements=" + includeImportStatements +
                ", includeSnippets=" + includeSnippets +
                ", includeKeywords=" + includeKeywords +
                ", maxResults=" + maxResults +
                ", caseSensitive=" + caseSensitive +
                ", useCachedResults=" + useCachedResults +
                '}';
    }
}