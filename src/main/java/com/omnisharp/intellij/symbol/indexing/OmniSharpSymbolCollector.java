package com.omnisharp.intellij.symbol.indexing;

import com.omnisharp.intellij.symbol.indexing.OmniSharpProjectManager;
import com.omnisharp.intellij.symbol.indexing.OmniSharpFileParser;
import com.omnisharp.intellij.symbol.indexing.OmniSharpLogger;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 符号收集器
 * 负责从项目文件中收集所有符号信息
 */
public class OmniSharpSymbolCollector {
    private final OmniSharpProjectManager projectManager;
    private final OmniSharpFileParser fileParser;
    private final OmniSharpLogger logger;
    private final ExecutorService executorService;

    /**
     * 构造函数
     * @param projectManager 项目管理器
     * @param fileParser 文件解析器
     * @param logger 日志记录器
     */
    public OmniSharpSymbolCollector(
            OmniSharpProjectManager projectManager,
            OmniSharpFileParser fileParser,
            OmniSharpLogger logger) {
        this.projectManager = projectManager;
        this.fileParser = fileParser;
        this.logger = logger;
        // 创建线程池用于并行收集符号
        this.executorService = Executors.newWorkStealingPool();
    }

    /**
     * 收集解决方案中的所有符号
     * @param solution 解决方案模型
     * @return 包含符号收集结果的CompletableFuture
     */
    public CompletableFuture<SymbolCollectionResult> collectSymbols(OmniSharpSolutionModel solution) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("开始收集解决方案 " + solution.getName() + " 中的符号...");
                
                ConcurrentMap<String, List<OmniSharpSymbol>> symbolMap = new ConcurrentHashMap<>();
                ConcurrentLinkedQueue<String> errorFiles = new ConcurrentLinkedQueue<>();
                
                // 并行收集每个项目的符号
                List<CompletableFuture<Void>> futures = new ArrayList<>();
                
                for (OmniSharpProjectModel project : solution.getProjects()) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            logger.info("收集项目 " + project.getName() + " 中的符号...");
                            List<OmniSharpSymbol> projectSymbols = collectProjectSymbols(project);
                            symbolMap.put(project.getName(), projectSymbols);
                            logger.info("项目 " + project.getName() + " 符号收集完成，共收集 " + projectSymbols.size() + " 个符号");
                        } catch (Exception e) {
                            logger.error("收集项目 " + project.getName() + " 符号失败: " + e.getMessage());
                            errorFiles.add(project.getName());
                        }
                    }, executorService);
                    futures.add(future);
                }
                
                // 等待所有项目符号收集完成
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                
                // 计算总符号数
                int totalSymbols = symbolMap.values().stream()
                        .mapToInt(List::size)
                        .sum();
                
                logger.info("解决方案符号收集完成，共收集 " + totalSymbols + " 个符号，" + 
                        "错误文件数: " + errorFiles.size());
                
                return new SymbolCollectionResult(symbolMap, new ArrayList<>(errorFiles));
            } catch (Exception e) {
                logger.error("符号收集过程中发生异常: " + e.getMessage());
                throw new RuntimeException("符号收集失败", e);
            }
        });
    }

    /**
     * 收集单个项目中的符号
     * @param project 项目模型
     * @return 项目中的符号列表
     */
    private List<OmniSharpSymbol> collectProjectSymbols(OmniSharpProjectModel project) {
        List<OmniSharpSymbol> symbols = new ArrayList<>();
        
        try {
            // 获取项目中的所有C#文件
            List<Path> csFiles = projectManager.getSourceFiles(project);
            
            for (Path filePath : csFiles) {
                try {
                    // 解析文件并收集符号
                    List<OmniSharpSymbol> fileSymbols = fileParser.parseFile(filePath, project.getName());
                    symbols.addAll(fileSymbols);
                } catch (Exception e) {
                    logger.warn("解析文件 " + filePath + " 失败: " + e.getMessage());
                    // 继续处理其他文件，不中断整个项目的符号收集
                }
            }
        } catch (Exception e) {
            logger.error("收集项目文件列表失败: " + e.getMessage());
            throw e;
        }
        
        return symbols;
    }

    /**
     * 关闭符号收集器，释放资源
     */
    public void shutdown() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}