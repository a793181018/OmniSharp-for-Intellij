package com.omnisharp.intellij.symbol.indexing;

import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 符号搜索引擎
 * 提供丰富的符号搜索功能，支持精确搜索、模糊搜索、前缀匹配等
 */
public class OmniSharpSymbolSearcher {
    private static final Logger LOG = Logger.getInstance(OmniSharpSymbolSearcher.class);

    // 搜索结果排序策略枚举
    public enum SortBy {
        NAME,
        RELEVANCE,
        LOCATION,
        KIND,
        PROJECT
    }

    // 搜索结果类
    public static class SearchResult {
        private final OmniSharpSymbol symbol;
        private final double relevanceScore;

        public SearchResult(OmniSharpSymbol symbol, double relevanceScore) {
            this.symbol = symbol;
            this.relevanceScore = relevanceScore;
        }

        public OmniSharpSymbol getSymbol() {
            return symbol;
        }

        public double getRelevanceScore() {
            return relevanceScore;
        }
    }

    // 搜索选项类
    public static class SearchOptions {
        private boolean caseSensitive = false;
        private boolean exactMatch = false;
        private boolean includeDocumentation = false;
        private int maxResults = 100;
        private SortBy sortBy = SortBy.RELEVANCE;
        private Set<OmniSharpSymbolKind> allowedKinds = new HashSet<>();
        private Set<String> allowedProjects = new HashSet<>();

        // Getters and setters
        public boolean isCaseSensitive() { return caseSensitive; }
        public void setCaseSensitive(boolean caseSensitive) { this.caseSensitive = caseSensitive; }

        public boolean isExactMatch() { return exactMatch; }
        public void setExactMatch(boolean exactMatch) { this.exactMatch = exactMatch; }

        public boolean isIncludeDocumentation() { return includeDocumentation; }
        public void setIncludeDocumentation(boolean includeDocumentation) { this.includeDocumentation = includeDocumentation; }

        public int getMaxResults() { return maxResults; }
        public void setMaxResults(int maxResults) { this.maxResults = maxResults;
            if (this.maxResults < 0) this.maxResults = 0;
        }

        public SortBy getSortBy() { return sortBy; }
        public void setSortBy(SortBy sortBy) { this.sortBy = sortBy != null ? sortBy : SortBy.RELEVANCE; }

        public Set<OmniSharpSymbolKind> getAllowedKinds() { return allowedKinds; }
        public void setAllowedKinds(Collection<OmniSharpSymbolKind> allowedKinds) {
            this.allowedKinds.clear();
            if (allowedKinds != null) {
                this.allowedKinds.addAll(allowedKinds);
            }
        }

        public Set<String> getAllowedProjects() { return allowedProjects; }
        public void setAllowedProjects(Collection<String> allowedProjects) {
            this.allowedProjects.clear();
            if (allowedProjects != null) {
                this.allowedProjects.addAll(allowedProjects);
            }
        }
    }

    // 符号索引器引用
    private final OmniSharpSymbolIndexer symbolIndexer;
    
    // 搜索结果缓存
    private final Map<String, List<SearchResult>> searchResultCache;
    
    // 搜索结果比较器映射
    private final Map<SortBy, Comparator<SearchResult>> comparators;

    public OmniSharpSymbolSearcher(OmniSharpSymbolIndexer indexer) {
        if (indexer == null) {
            throw new IllegalArgumentException("Symbol indexer cannot be null");
        }
        this.symbolIndexer = indexer;
        this.searchResultCache = new ConcurrentHashMap<>(16, 0.75f, Runtime.getRuntime().availableProcessors());
        this.comparators = createComparators();
    }

    /**
     * 创建各种排序比较器
     */
    private Map<SortBy, Comparator<SearchResult>> createComparators() {
        Map<SortBy, Comparator<SearchResult>> map = new HashMap<>();

        // 按相关性排序（降序）
        map.put(SortBy.RELEVANCE, Comparator.comparingDouble(SearchResult::getRelevanceScore).reversed());

        // 按名称排序（升序）
        map.put(SortBy.NAME, Comparator.comparing(result -> result.getSymbol().getName()));

        // 按位置排序（文件路径和行号）
        map.put(SortBy.LOCATION, Comparator.comparing(result -> {
            OmniSharpSymbol symbol = result.getSymbol();
            return symbol.getFilePath().toString() + ":" + symbol.getStartLine();
        }));

        // 按符号类型排序
        map.put(SortBy.KIND, Comparator.comparing(result -> result.getSymbol().getKind()));

        // 按项目名称排序
        map.put(SortBy.PROJECT, Comparator.comparing(result -> result.getSymbol().getProjectName()));

        return map;
    }

    /**
     * 搜索符号
     * @param query 搜索查询
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query) {
        return search(query, new SearchOptions());
    }

    /**
     * 搜索符号（带选项）
     * @param query 搜索查询
     * @param options 搜索选项
     * @return 搜索结果列表
     */
    public List<SearchResult> search(String query, SearchOptions options) {
        if (query == null || query.trim().isEmpty()) {
            return new ArrayList<>();
        }

        if (options == null) {
            options = new SearchOptions();
        }

        // 尝试从缓存获取结果
        String cacheKey = generateCacheKey(query, options);
        List<SearchResult> cachedResults = searchResultCache.get(cacheKey);
        if (cachedResults != null) {
            return cachedResults;
        }

        long startTime = System.currentTimeMillis();
        List<SearchResult> results = new ArrayList<>();

        // 根据选项选择搜索策略
        if (options.isExactMatch()) {
            // 精确匹配搜索
            results.addAll(exactMatchSearch(query, options));
        } else {
            // 尝试多种搜索策略并合并结果
            Set<OmniSharpSymbol> uniqueSymbols = new HashSet<>();

            // 1. 前缀搜索
            List<SearchResult> prefixResults = prefixSearch(query, options);
            results.addAll(prefixResults);
            prefixResults.forEach(result -> uniqueSymbols.add(result.getSymbol()));

            // 2. 模糊搜索（如果前缀搜索结果不足）
            if (results.size() < options.getMaxResults()) {
                List<SearchResult> fuzzyResults = fuzzySearch(query, options);
                for (SearchResult result : fuzzyResults) {
                    if (!uniqueSymbols.contains(result.getSymbol())) {
                        results.add(result);
                        uniqueSymbols.add(result.getSymbol());
                    }
                }
            }

            // 3. 正则表达式搜索（如果仍需更多结果）
            if (results.size() < options.getMaxResults()) {
                List<SearchResult> regexResults = regexSearch(query, options);
                for (SearchResult result : regexResults) {
                    if (!uniqueSymbols.contains(result.getSymbol())) {
                        results.add(result);
                        uniqueSymbols.add(result.getSymbol());
                    }
                }
            }
        }

        // 应用过滤器
        results = applyFilters(results, options);

        // 排序结果
        results = sortResults(results, options.getSortBy());

        // 限制结果数量
        if (results.size() > options.getMaxResults()) {
            results = results.subList(0, options.getMaxResults());
        }

        // 缓存结果
        searchResultCache.put(cacheKey, results);

        long endTime = System.currentTimeMillis();
        LOG.debug(String.format("Search for '%s' completed in %dms, found %d results",
                query, (endTime - startTime), results.size()));

        return results;
    }

    /**
     * 精确匹配搜索
     */
    private List<SearchResult> exactMatchSearch(String query, SearchOptions options) {
        List<SearchResult> results = new ArrayList<>();
        String searchQuery = options.isCaseSensitive() ? query : query.toLowerCase();

        // 先尝试完全限定名匹配
        OmniSharpSymbol symbol = symbolIndexer.findSymbolByFullyQualifiedName(searchQuery);
        if (symbol != null) {
            results.add(new SearchResult(symbol, 1.0)); // 完全匹配相关性最高
        }

        // 再尝试名称匹配
        List<OmniSharpSymbol> nameMatches = symbolIndexer.findSymbolsByName(searchQuery);
        for (OmniSharpSymbol s : nameMatches) {
            // 避免重复添加
            if (symbol == null || !s.getFullyQualifiedName().equals(symbol.getFullyQualifiedName())) {
                results.add(new SearchResult(s, 0.9)); // 名称匹配相关性稍低
            }
        }

        return results;
    }

    /**
     * 前缀搜索
     */
    private List<SearchResult> prefixSearch(String query, SearchOptions options) {
        String prefix = options.isCaseSensitive() ? query : query.toLowerCase();
        List<OmniSharpSymbol> symbols = symbolIndexer.findSymbolsByPrefix(prefix);
        
        return symbols.stream()
                .map(symbol -> new SearchResult(symbol, calculatePrefixRelevance(symbol, prefix, options)))
                .collect(Collectors.toList());
    }

    /**
     * 模糊搜索
     */
    private List<SearchResult> fuzzySearch(String query, SearchOptions options) {
        String fuzzyQuery = options.isCaseSensitive() ? query : query.toLowerCase();
        List<SearchResult> results = new ArrayList<>();

        // 简化实现：使用正则表达式进行模糊匹配
        // 实际应用中可以使用更复杂的模糊匹配算法，如Levenshtein距离或BM25
        String regexPattern = fuzzyQuery.chars()
                .mapToObj(c -> Pattern.quote(String.valueOf((char) c)))
                .collect(Collectors.joining(".*"));

        OmniSharpSymbolIndexer.SearchQuery indexQuery = new OmniSharpSymbolIndexer.SearchQuery();
        indexQuery.setNameRegex(regexPattern);
        
        List<OmniSharpSymbol> symbols = symbolIndexer.searchSymbols(indexQuery);
        
        for (OmniSharpSymbol symbol : symbols) {
            double relevance = calculateFuzzyRelevance(symbol, fuzzyQuery, options);
            if (relevance > 0.1) { // 设置相关性阈值
                results.add(new SearchResult(symbol, relevance));
            }
        }

        return results;
    }

    /**
     * 正则表达式搜索
     */
    private List<SearchResult> regexSearch(String query, SearchOptions options) {
        try {
            String regex = options.isCaseSensitive() ? query : "(?i)" + query;
            Pattern pattern = Pattern.compile(regex);

            OmniSharpSymbolIndexer.SearchQuery indexQuery = new OmniSharpSymbolIndexer.SearchQuery();
            indexQuery.setNameRegex(regex);
            
            List<OmniSharpSymbol> symbols = symbolIndexer.searchSymbols(indexQuery);
            
            return symbols.stream()
                    .map(symbol -> new SearchResult(symbol, 0.5)) // 正则匹配相关性中等
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.warn("Invalid regex pattern: " + query, e);
            return new ArrayList<>();
        }
    }

    /**
     * 应用过滤器
     */
    private List<SearchResult> applyFilters(List<SearchResult> results, SearchOptions options) {
        return results.stream()
                .filter(result -> {
                    OmniSharpSymbol symbol = result.getSymbol();
                    
                    // 过滤符号类型
                    if (!options.getAllowedKinds().isEmpty() && !options.getAllowedKinds().contains(symbol.getKind())) {
                        return false;
                    }
                    
                    // 过滤项目
                    if (!options.getAllowedProjects().isEmpty() && !options.getAllowedProjects().contains(symbol.getProjectName())) {
                        return false;
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 排序搜索结果
     */
    private List<SearchResult> sortResults(List<SearchResult> results, SortBy sortBy) {
        Comparator<SearchResult> comparator = comparators.get(sortBy);
        if (comparator != null) {
            results.sort(comparator);
        }
        return results;
    }

    /**
     * 计算前缀匹配的相关性分数
     */
    private double calculatePrefixRelevance(OmniSharpSymbol symbol, String prefix, SearchOptions options) {
        String symbolName = options.isCaseSensitive() ? symbol.getName() : symbol.getName().toLowerCase();
        String qualifiedName = symbol.getFullyQualifiedName() != null ? 
                (options.isCaseSensitive() ? symbol.getFullyQualifiedName() : symbol.getFullyQualifiedName().toLowerCase()) : "";

        double score = 0.0;
        
        // 精确匹配名称
        if (symbolName.equals(prefix)) {
            score = 1.0;
        }
        // 前缀匹配名称
        else if (symbolName.startsWith(prefix)) {
            score = 0.9;
        }
        // 前缀匹配完全限定名的最后一部分
        else if (!qualifiedName.isEmpty()) {
            int lastDotIndex = qualifiedName.lastIndexOf('.');
            if (lastDotIndex >= 0 && lastDotIndex + 1 < qualifiedName.length()) {
                String shortName = qualifiedName.substring(lastDotIndex + 1);
                if (shortName.startsWith(prefix)) {
                    score = 0.8;
                }
            }
        }
        // 前缀匹配完全限定名的其他部分
        else if (!qualifiedName.isEmpty() && qualifiedName.contains("." + prefix)) {
            score = 0.7;
        }

        return score;
    }

    /**
     * 计算模糊匹配的相关性分数
     */
    private double calculateFuzzyRelevance(OmniSharpSymbol symbol, String query, SearchOptions options) {
        String symbolName = options.isCaseSensitive() ? symbol.getName() : symbol.getName().toLowerCase();
        String qualifiedName = symbol.getFullyQualifiedName() != null ? 
                (options.isCaseSensitive() ? symbol.getFullyQualifiedName() : symbol.getFullyQualifiedName().toLowerCase()) : "";

        double score = 0.0;

        // 计算Levenshtein距离（简化版）
        double nameDistance = calculateLevenshteinDistance(symbolName, query);
        double nameScore = 1.0 - (nameDistance / Math.max(symbolName.length(), query.length()));
        
        // 如果查询词是符号名的子串，给更高分数
        if (symbolName.contains(query)) {
            score += 0.5;
        }

        // 考虑完全限定名
        if (!qualifiedName.isEmpty() && qualifiedName.contains(query)) {
            score += 0.3;
        }

        // 结合距离分数
        score += nameScore * 0.2;
        score = Math.min(score, 1.0); // 确保不超过1.0

        return score;
    }

    /**
     * 计算Levenshtein距离（简化版）
     */
    private int calculateLevenshteinDistance(String s1, String s2) {
        if (s1 == null || s2 == null) {
            return Integer.MAX_VALUE;
        }

        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= s2.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= s1.length(); i++) {
            for (int j = 1; j <= s2.length(); j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                        dp[i - 1][j] + 1,      // 删除
                        dp[i][j - 1] + 1),     // 插入
                        dp[i - 1][j - 1] + cost // 替换
                );
            }
        }

        return dp[s1.length()][s2.length()];
    }

    /**
     * 生成搜索缓存键
     */
    private String generateCacheKey(String query, SearchOptions options) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(query)
                .append("|")
                .append(options.isCaseSensitive() ? "1" : "0")
                .append("|")
                .append(options.isExactMatch() ? "1" : "0")
                .append("|")
                .append(options.getSortBy())
                .append("|")
                .append(options.getMaxResults());

        // 添加类型过滤器
        if (!options.getAllowedKinds().isEmpty()) {
            keyBuilder.append("|kinds:").append(options.getAllowedKinds().stream()
                    .map(Enum::name)
                    .sorted()
                    .collect(Collectors.joining(",")));
        }

        // 添加项目过滤器
        if (!options.getAllowedProjects().isEmpty()) {
            keyBuilder.append("|projects:").append(options.getAllowedProjects().stream()
                    .sorted()
                    .collect(Collectors.joining(",")));
        }

        return keyBuilder.toString();
    }

    /**
     * 清空搜索结果缓存
     */
    public void clearCache() {
        searchResultCache.clear();
        LOG.debug("Search result cache cleared");
    }

    /**
     * 获取搜索缓存大小
     */
    public int getCacheSize() {
        return searchResultCache.size();
    }

    /**
     * 按类型搜索符号
     */
    public List<SearchResult> searchByKind(OmniSharpSymbolKind kind) {
        List<OmniSharpSymbol> symbols = symbolIndexer.findSymbolsByKind(kind);
        return symbols.stream()
                .map(symbol -> new SearchResult(symbol, 1.0))
                .collect(Collectors.toList());
    }

    /**
     * 按项目搜索符号
     */
    public List<SearchResult> searchByProject(String projectName) {
        List<OmniSharpSymbol> symbols = symbolIndexer.findSymbolsByProject(projectName);
        return symbols.stream()
                .map(symbol -> new SearchResult(symbol, 1.0))
                .collect(Collectors.toList());
    }

    /**
     * 按文件搜索符号
     */
    public List<SearchResult> searchByFile(String filePath) {
        List<OmniSharpSymbol> symbols = symbolIndexer.findSymbolsByFile(filePath);
        return symbols.stream()
                .map(symbol -> new SearchResult(symbol, 1.0))
                .collect(Collectors.toList());
    }

    /**
     * 高级搜索：组合多种条件
     */
    public List<SearchResult> advancedSearch(OmniSharpSymbolIndexer.SearchQuery query) {
        if (query == null) {
            return new ArrayList<>();
        }

        List<OmniSharpSymbol> symbols = symbolIndexer.searchSymbols(query);
        return symbols.stream()
                .map(symbol -> new SearchResult(symbol, 1.0))
                .collect(Collectors.toList());
    }
}