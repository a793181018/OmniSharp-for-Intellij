package com.omnisharp.intellij.symbol.indexing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// 使用String作为键类型替代SymbolKind枚举

/**
 * 符号索引器
 * 负责构建和管理符号索引，提供高效的符号查询功能
 */
public class OmniSharpSymbolIndexer {
    // 按符号名称索引
    private final Map<String, List<OmniSharpSymbol>> nameIndex;
    // 按完全限定名索引
    private final Map<String, OmniSharpSymbol> fullyQualifiedNameIndex;
    // 按符号类型索引（使用字符串替代枚举）
    private final Map<String, List<OmniSharpSymbol>> kindIndex;
    // 按项目名称索引
    private final Map<String, List<OmniSharpSymbol>> projectIndex;
    // 按文件路径索引
    private final Map<String, List<OmniSharpSymbol>> fileIndex;
    // 符号名称的前缀树（用于前缀搜索）
    private final TrieNode nameTrie;
    // 索引状态
    private boolean isIndexed;

    /**
     * 前缀树节点
     */
    private static class TrieNode {
        Map<Character, TrieNode> children;
        Set<OmniSharpSymbol> symbols;

        TrieNode() {
            this.children = new HashMap<>();
            this.symbols = new HashSet<>();
        }
    }

    public OmniSharpSymbolIndexer() {
        this.nameIndex = new ConcurrentHashMap<>();
        this.fullyQualifiedNameIndex = new ConcurrentHashMap<>();
        this.kindIndex = new HashMap<String, List<OmniSharpSymbol>>();
        this.projectIndex = new ConcurrentHashMap<>();
        this.fileIndex = new ConcurrentHashMap<>();
        this.nameTrie = new TrieNode();
        this.isIndexed = false;
    }

    /**
     * 构建符号索引
     * @param result 符号收集结果
     */
    public synchronized void buildIndex(SymbolCollectionResult result) {
        // 清空现有索引
        clearIndex();

        // 遍历所有符号，构建索引
        result.getSymbols().forEach((projectName, symbols) -> {
            for (OmniSharpSymbol symbol : symbols) {
                addSymbolToIndex(symbol);
            }
        });

        isIndexed = true;
    }

    /**
     * 将单个符号添加到索引中
     * @param symbol 要添加的符号
     */
    private void addSymbolToIndex(OmniSharpSymbol symbol) {
        String name = symbol.getName();
        String fullyQualifiedName = symbol.getFullyQualifiedName();
        OmniSharpSymbolKind kind = symbol.getKind();
        String projectName = symbol.getProjectName();
        String filePath = symbol.getFilePath().toString();

        // 添加到名称索引
        nameIndex.computeIfAbsent(name.toLowerCase(), k -> new ArrayList<>()).add(symbol);

        // 添加到完全限定名索引
        if (fullyQualifiedName != null) {
            fullyQualifiedNameIndex.put(fullyQualifiedName, symbol);
        }

        // 添加到类型索引
        kindIndex.computeIfAbsent(kind.toString(), k -> new ArrayList<>()).add(symbol);

        // 添加到项目索引
        projectIndex.computeIfAbsent(projectName, k -> new ArrayList<>()).add(symbol);

        // 添加到文件索引
        fileIndex.computeIfAbsent(filePath, k -> new ArrayList<>()).add(symbol);

        // 添加到前缀树
        addToNameTrie(name.toLowerCase(), symbol);
    }

    /**
     * 将符号添加到名称前缀树
     * @param name 符号名称（小写）
     * @param symbol 符号对象
     */
    private void addToNameTrie(String name, OmniSharpSymbol symbol) {
        TrieNode current = nameTrie;
        for (char c : name.toCharArray()) {
            current = current.children.computeIfAbsent(c, k -> new TrieNode());
        }
        current.symbols.add(symbol);
    }

    /**
     * 清空所有索引
     */
    public synchronized void clearIndex() {
        nameIndex.clear();
        fullyQualifiedNameIndex.clear();
        kindIndex.clear();
        projectIndex.clear();
        fileIndex.clear();
        // 重建前缀树
        nameTrie.children.clear();
        nameTrie.symbols.clear();
        isIndexed = false;
    }

    /**
     * 按名称查找符号
     * @param name 符号名称
     * @return 符号列表
     */
    public List<OmniSharpSymbol> findSymbolsByName(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        return nameIndex.getOrDefault(name.toLowerCase(), Collections.emptyList());
    }

    /**
     * 按完全限定名查找符号
     * @param fullyQualifiedName 完全限定名
     * @return 符号对象，如果不存在则返回null
     */
    public OmniSharpSymbol findSymbolByFullyQualifiedName(String fullyQualifiedName) {
        return fullyQualifiedName != null ? fullyQualifiedNameIndex.get(fullyQualifiedName) : null;
    }

    /**
     * 按类型查找符号
     * @param kind 符号类型
     * @return 符号列表
     */
    public List<OmniSharpSymbol> findSymbolsByKind(OmniSharpSymbolKind kind) {
        return kind != null ? kindIndex.getOrDefault(kind, Collections.emptyList()) : Collections.emptyList();
    }

    /**
     * 按项目查找符号
     * @param projectName 项目名称
     * @return 符号列表
     */
    public List<OmniSharpSymbol> findSymbolsByProject(String projectName) {
        return projectName != null ? projectIndex.getOrDefault(projectName, Collections.emptyList()) : Collections.emptyList();
    }

    /**
     * 按文件查找符号
     * @param filePath 文件路径
     * @return 符号列表
     */
    public List<OmniSharpSymbol> findSymbolsByFile(String filePath) {
        return filePath != null ? fileIndex.getOrDefault(filePath, Collections.emptyList()) : Collections.emptyList();
    }

    /**
     * 按名称前缀查找符号
     * @param prefix 名称前缀
     * @return 符号列表
     */
    public List<OmniSharpSymbol> findSymbolsByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return Collections.emptyList();
        }

        Set<OmniSharpSymbol> resultSet = new HashSet<>();
        findByPrefixRecursive(nameTrie, prefix.toLowerCase(), 0, resultSet);
        return new ArrayList<>(resultSet);
    }

    /**
     * 递归查找前缀匹配的符号
     */
    private void findByPrefixRecursive(TrieNode node, String prefix, int index, Set<OmniSharpSymbol> resultSet) {
        if (index == prefix.length()) {
            // 已找到前缀对应的节点，收集该节点及其所有子节点的符号
            collectAllSymbols(node, resultSet);
            return;
        }

        char c = prefix.charAt(index);
        TrieNode child = node.children.get(c);
        if (child != null) {
            findByPrefixRecursive(child, prefix, index + 1, resultSet);
        }
    }

    /**
     * 收集节点及其所有子节点的符号
     */
    private void collectAllSymbols(TrieNode node, Set<OmniSharpSymbol> resultSet) {
        resultSet.addAll(node.symbols);
        for (TrieNode child : node.children.values()) {
            collectAllSymbols(child, resultSet);
        }
    }

    /**
     * 高级搜索：按多个条件过滤符号
     * @param query 搜索查询条件
     * @return 符合条件的符号列表
     */
    public List<OmniSharpSymbol> searchSymbols(SearchQuery query) {
        List<OmniSharpSymbol> candidates;

        // 根据查询条件选择合适的索引开始搜索
        if (query.getFullyQualifiedName() != null) {
            OmniSharpSymbol symbol = findSymbolByFullyQualifiedName(query.getFullyQualifiedName());
            candidates = symbol != null ? Collections.singletonList(symbol) : Collections.emptyList();
        } else if (query.getName() != null) {
            candidates = findSymbolsByName(query.getName());
        } else if (query.getPrefix() != null) {
            candidates = findSymbolsByPrefix(query.getPrefix());
        } else if (query.getKind() != null) {
            candidates = findSymbolsByKind(query.getKind());
        } else if (query.getProjectName() != null) {
            candidates = findSymbolsByProject(query.getProjectName());
        } else if (query.getFilePath() != null) {
            candidates = findSymbolsByFile(query.getFilePath());
        } else {
            // 如果没有指定任何条件，返回空列表
            return Collections.emptyList();
        }

        // 应用其他过滤条件
        return candidates.stream()
                .filter(symbol -> matchesQuery(symbol, query))
                .collect(Collectors.toList());
    }

    /**
     * 检查符号是否匹配查询条件
     */
    private boolean matchesQuery(OmniSharpSymbol symbol, SearchQuery query) {
        // 按类型过滤
        if (query.getKind() != null && symbol.getKind() != query.getKind()) {
            return false;
        }

        // 按项目过滤
        if (query.getProjectName() != null && !query.getProjectName().equals(symbol.getProjectName())) {
            return false;
        }

        // 按正则表达式匹配名称
        if (query.getNameRegex() != null) {
            Pattern pattern = Pattern.compile(query.getNameRegex(), Pattern.CASE_INSENSITIVE);
            if (!pattern.matcher(symbol.getName()).find()) {
                return false;
            }
        }

        return true;
    }

    /**
     * 获取索引中的符号总数
     * @return 符号总数
     */
    public int getTotalSymbols() {
        return fullyQualifiedNameIndex.size();
    }

    /**
     * 检查索引是否已构建
     * @return 如果索引已构建则返回true
     */
    public boolean isIndexed() {
        return isIndexed;
    }

    /**
     * 搜索查询类
     * 用于高级搜索的条件封装
     */
    public static class SearchQuery {
        private String name;
        private String fullyQualifiedName;
        private String prefix;
        private String nameRegex;
        private OmniSharpSymbolKind kind;
        private String projectName;
        private String filePath;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFullyQualifiedName() { return fullyQualifiedName; }
        public void setFullyQualifiedName(String fullyQualifiedName) { this.fullyQualifiedName = fullyQualifiedName; }

        public String getPrefix() { return prefix; }
        public void setPrefix(String prefix) { this.prefix = prefix; }

        public String getNameRegex() { return nameRegex; }
        public void setNameRegex(String nameRegex) { this.nameRegex = nameRegex; }

        public OmniSharpSymbolKind getKind() { return kind; }
        public void setKind(OmniSharpSymbolKind kind) { this.kind = kind; }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
    }
}