package com.omnisharp.intellij.projectstructure.service;

import com.omnisharp.intellij.projectstructure.api.SymbolIndexService;
import com.omnisharp.intellij.projectstructure.model.SymbolInfo;
import com.omnisharp.intellij.projectstructure.model.SymbolKind;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 符号索引服务的实现类
 */
public class SymbolIndexServiceImpl implements SymbolIndexService {
    private final Map<String, List<SymbolInfo>> symbolIndex = new ConcurrentHashMap<>(); // 解决方案ID -> 符号列表
    private final Map<String, SymbolInfo> symbolMap = new ConcurrentHashMap<>(); // 符号ID -> 符号信息
    private final Map<String, List<SymbolInfo>> fqnIndex = new ConcurrentHashMap<>(); // 全限定名 -> 符号列表
    private final Map<String, List<SymbolInfo>> fileIndex = new ConcurrentHashMap<>(); // 文件路径 -> 符号列表
    private final Map<String, List<SymbolInfo>> projectIndex = new ConcurrentHashMap<>(); // 项目ID -> 符号列表
    private final List<IndexProgressListener> progressListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService indexingExecutor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors());

    @Override
    public boolean indexSolution(String solutionId) {
        try {
            notifyProgress(0, "Starting solution indexing");
            
            // 在实际实现中，这里应该遍历解决方案中的所有项目和文件
            // 为了演示，我们模拟索引过程
            List<SymbolInfo> symbols = new ArrayList<>();
            // 模拟添加一些符号
            symbols.addAll(createSampleSymbols(solutionId));
            
            symbolIndex.put(solutionId, symbols);
            // 构建其他索引
            buildSecondaryIndexes(symbols);
            
            notifyProgress(100, "Solution indexing completed");
            notifyComplete(true, "Solution indexed successfully");
            return true;
        } catch (Exception e) {
            notifyComplete(false, "Failed to index solution: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean indexProject(String projectId) {
        try {
            notifyProgress(0, "Starting project indexing: " + projectId);
            
            // 模拟索引单个项目
            List<SymbolInfo> symbols = createSampleSymbolsForProject(projectId);
            projectIndex.put(projectId, symbols);
            buildSecondaryIndexes(symbols);
            
            notifyProgress(100, "Project indexing completed: " + projectId);
            notifyComplete(true, "Project indexed successfully");
            return true;
        } catch (Exception e) {
            notifyComplete(false, "Failed to index project: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean indexFile(String projectId, String filePath) {
        try {
            notifyProgress(0, "Starting file indexing: " + filePath);
            
            // 模拟索引单个文件
            List<SymbolInfo> symbols = createSampleSymbolsForFile(projectId, filePath);
            fileIndex.put(filePath, symbols);
            buildSecondaryIndexes(symbols);
            
            notifyProgress(100, "File indexing completed: " + filePath);
            notifyComplete(true, "File indexed successfully");
            return true;
        } catch (Exception e) {
            notifyComplete(false, "Failed to index file: " + e.getMessage());
            return false;
        }
    }

    @Override
    public List<SymbolInfo> searchSymbols(String query) {
        return searchSymbols(query, null, null, false, false);
    }



    @Override
    public List<SymbolInfo> searchSymbols(String query, Set<SymbolKind> symbolKinds,
                                        Set<String> projectIds, boolean caseSensitive, boolean exactMatch) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String searchQuery = caseSensitive ? query : query.toLowerCase();
        List<SymbolInfo> allSymbols = new ArrayList<>();
        
        // 收集所有符号
        for (List<SymbolInfo> symbols : symbolIndex.values()) {
            allSymbols.addAll(symbols);
        }

        // 过滤符号
        return allSymbols.stream()
                .filter(symbol -> matchesQuery(symbol, searchQuery, caseSensitive, exactMatch))
                .filter(symbol -> symbolKinds == null || symbolKinds.contains(symbol.getKind()))
                .filter(symbol -> projectIds == null || projectIds.contains(symbol.getProjectId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<SymbolInfo> findSymbolByFullyQualifiedName(String fullyQualifiedName) {
        return fqnIndex.getOrDefault(fullyQualifiedName, Collections.emptyList());
    }

    @Override
    public List<SymbolInfo> getSymbolsInFile(String filePath) {
        return fileIndex.getOrDefault(filePath, Collections.emptyList());
    }

    @Override
    public List<SymbolInfo> getSymbolsInProject(String projectId) {
        return projectIndex.getOrDefault(projectId, Collections.emptyList());
    }

    @Override
    public List<SymbolReference> findReferences(String symbolId) {
        SymbolInfo symbol = symbolMap.get(symbolId);
        if (symbol == null) {
            return Collections.emptyList();
        }

        // 模拟查找引用
        List<SymbolReference> references = new ArrayList<>();
        // 实际实现中需要搜索代码中的引用位置
        references.add(new SymbolReference(
                symbol.getProjectId(),
                "references.cs",
                10,
                5,
                "call"
        ));
        return references;
    }

    @Override
    public boolean updateIndex(List<String> changedFiles) {
        try {
            for (String file : changedFiles) {
                // 重新索引变更的文件
                // 在实际实现中，这里需要获取文件所属的项目
                indexFile("unknown", file);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean clearIndex(String solutionId) {
        List<SymbolInfo> symbols = symbolIndex.remove(solutionId);
        if (symbols != null) {
            // 清除相关的索引
            for (SymbolInfo symbol : symbols) {
                symbolMap.remove(symbol.getId());
                // 清除fqn索引中的引用
                fqnIndex.remove(symbol.getFullyQualifiedName());
                // 清除file索引中的引用
                List<SymbolInfo> fileSymbols = fileIndex.get(symbol.getFilePath());
                if (fileSymbols != null) {
                    fileSymbols.remove(symbol);
                }
                // 清除project索引中的引用
                List<SymbolInfo> projectSymbols = projectIndex.get(symbol.getProjectId());
                if (projectSymbols != null) {
                    projectSymbols.remove(symbol);
                }
            }
        }
        return true;
    }

    @Override
    public IndexStats getIndexStatistics(String solutionId) {
        List<SymbolInfo> symbols = symbolIndex.getOrDefault(solutionId, Collections.emptyList());
        Set<String> indexedFiles = new HashSet<>();
        int[] symbolsByKind = new int[SymbolKind.values().length];

        for (SymbolInfo symbol : symbols) {
            indexedFiles.add(symbol.getFilePath());
            symbolsByKind[symbol.getKind().ordinal()]++;
        }

        return new IndexStats(symbols.size(), indexedFiles.size(), 0, symbolsByKind);
    }

    @Override
    public boolean isIndexAvailable(String solutionId) {
        return symbolIndex.containsKey(solutionId);
    }

    @Override
    public void addIndexProgressListener(IndexProgressListener listener) {
        if (listener != null && !progressListeners.contains(listener)) {
            progressListeners.add(listener);
        }
    }

    @Override
    public void removeIndexProgressListener(IndexProgressListener listener) {
        progressListeners.remove(listener);
    }

    // 辅助方法：检查符号是否匹配查询
    private boolean matchesQuery(SymbolInfo symbol, String query, boolean caseSensitive, boolean exactMatch) {
        String name = caseSensitive ? symbol.getName() : symbol.getName().toLowerCase();
        String fqn = caseSensitive ? symbol.getFullyQualifiedName() : symbol.getFullyQualifiedName().toLowerCase();

        if (exactMatch) {
            return name.equals(query) || fqn.equals(query);
        } else {
            return name.contains(query) || fqn.contains(query);
        }
    }

    // 辅助方法：构建二级索引
    private void buildSecondaryIndexes(List<SymbolInfo> symbols) {
        for (SymbolInfo symbol : symbols) {
            // 符号ID索引
            symbolMap.put(symbol.getId(), symbol);
            
            // 全限定名索引
            fqnIndex.computeIfAbsent(symbol.getFullyQualifiedName(), k -> new ArrayList<>()).add(symbol);
            
            // 文件索引
            fileIndex.computeIfAbsent(symbol.getFilePath(), k -> new ArrayList<>()).add(symbol);
            
            // 项目索引
            projectIndex.computeIfAbsent(symbol.getProjectId(), k -> new ArrayList<>()).add(symbol);
        }
    }

    // 通知进度
    private void notifyProgress(int progress, String status) {
        progressListeners.forEach(listener -> listener.onProgress(progress, status));
    }

    // 通知完成
    private void notifyComplete(boolean success, String message) {
        progressListeners.forEach(listener -> listener.onComplete(success, message));
    }

    // 模拟创建一些示例符号
    private List<SymbolInfo> createSampleSymbols(String solutionId) {
        List<SymbolInfo> symbols = new ArrayList<>();
        symbols.add(new SymbolInfo(
                "1", "Program", SymbolKind.CLASS, "ConsoleApp.Program", "project1", 
                "Program.cs", 10, 5,
                "Main program class", Arrays.asList("public", "partial")
        ));
        symbols.add(new SymbolInfo(
                "2", "Main", SymbolKind.METHOD, "ConsoleApp.Program.Main", "project1",
                "Program.cs", 20, 10,
                "Main entry point", Arrays.asList("public", "static")
        ));
        return symbols;
    }

    private List<SymbolInfo> createSampleSymbolsForProject(String projectId) {
        List<SymbolInfo> symbols = new ArrayList<>();
        symbols.add(new SymbolInfo(
                "project-" + projectId + "-1", "ProjectClass", SymbolKind.CLASS,
                "Namespace.ProjectClass", projectId, "ProjectClass.cs", 1, 1,
                "Sample class", Arrays.asList("public")
        ));
        return symbols;
    }

    private List<SymbolInfo> createSampleSymbolsForFile(String projectId, String filePath) {
        List<SymbolInfo> symbols = new ArrayList<>();
        symbols.add(new SymbolInfo(
                "file-" + filePath + "-1", "FileClass", SymbolKind.CLASS,
                "Namespace.FileClass", projectId, filePath, 1, 1,
                "Class in file", Arrays.asList("public")
        ));
        return symbols;
    }
}