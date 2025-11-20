package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * OmniSharp依赖分析器实现
 * 实现了依赖分析的核心功能
 */
public class OmniSharpDependencyAnalyzer implements DependencyAnalyzer {
    private static final Logger logger = Logger.getLogger(OmniSharpDependencyAnalyzer.class.getName());
    
    private final ProjectResolver projectResolver;
    private final PackageManager packageManager;
    private final DependencyGraphBuilder graphBuilder;
    private final DependencyAnalysisCache cache;
    private final ExecutorService executorService;
    
    public OmniSharpDependencyAnalyzer(ProjectResolver projectResolver, PackageManager packageManager) {
        this.projectResolver = projectResolver;
        this.packageManager = packageManager;
        this.graphBuilder = new OmniSharpDependencyGraphBuilder();
        this.cache = new DependencyAnalysisCache();
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors() * 2);
        
        // 初始化包管理器
        packageManager.initialize();
    }
    
    @Override
    public DependencyAnalysisResult analyzeSolutionDependencies(Path solutionPath) {
        logger.info("开始分析解决方案依赖关系: " + solutionPath);
        
        // 检查缓存
        if (cache.hasSolutionCache(solutionPath)) {
            logger.info("从缓存获取解决方案依赖分析结果");
            return cache.getSolutionCache(solutionPath);
        }
        
        try {
            // 1. 解析解决方案中的项目引用关系
            Map<Path, List<ProjectDependency>> projectReferences = 
                projectResolver.resolveSolutionProjectReferences(solutionPath);
            
            // 2. 并行分析每个项目的依赖
            List<CompletableFuture<DependencyAnalysisResult>> futures = new ArrayList<>();
            for (Path projectPath : projectReferences.keySet()) {
                CompletableFuture<DependencyAnalysisResult> future = 
                    CompletableFuture.supplyAsync(() -> analyzeProjectDependenciesInternal(projectPath),
                                                executorService);
                futures.add(future);
            }
            
            // 3. 等待所有项目分析完成
            List<DependencyAnalysisResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            // 4. 合并所有结果
            DependencyAnalysisResult mergedResult = mergeAnalysisResults(results);
            
            // 5. 检测循环依赖
            List<Cycle> cycles = mergedResult.getDependencyGraph().findCycles();
            
            // 6. 检查版本冲突
            List<PackageVersionConflict> conflicts = checkVersionConflictsInternal(mergedResult);
            
            // 7. 构建最终结果
            DependencyAnalysisResult finalResult = DependencyAnalysisResult.builder()
                .dependencyGraph(mergedResult.getDependencyGraph())
                .projectDependencies(mergedResult.getProjectDependencies())
                .packageDependencies(mergedResult.getPackageDependencies())
                .cycles(cycles)
                .versionConflicts(conflicts)
                .success(true)
                .build();
            
            // 缓存结果
            cache.setSolutionCache(solutionPath, finalResult);
            
            logger.info("解决方案依赖分析完成: " + solutionPath);
            return finalResult;
        } catch (Exception e) {
            logger.severe("分析解决方案依赖关系失败: " + e.getMessage());
            return DependencyAnalysisResult.builder()
                .success(false)
                .errorMessage("分析失败: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public DependencyAnalysisResult analyzeProjectDependencies(Path projectPath) {
        return analyzeProjectDependenciesInternal(projectPath);
    }
    
    private DependencyAnalysisResult analyzeProjectDependenciesInternal(Path projectPath) {
        logger.info("开始分析项目依赖关系: " + projectPath);
        
        // 检查缓存
        if (cache.hasProjectCache(projectPath)) {
            logger.info("从缓存获取项目依赖分析结果");
            return cache.getProjectCache(projectPath);
        }
        
        try {
            // 1. 解析项目信息
            ProjectResolver.ProjectInfo projectInfo = projectResolver.resolveProject(projectPath);
            
            // 2. 解析项目引用
            List<ProjectDependency> projectDependencies = 
                projectResolver.resolveProjectReferences(projectPath);
            
            // 3. 解析包依赖
            List<PackageDependency> packageDependencies = 
                packageManager.resolveProjectPackageDependencies(projectPath);
            
            // 4. 构建依赖图
            DependencyGraph graph = graphBuilder.buildProjectDependencyGraph(
                projectInfo, projectDependencies, packageDependencies);
            
            // 5. 检测循环依赖
            List<Cycle> cycles = graph.findCycles();
            
            // 6. 构建结果
            DependencyAnalysisResult result = DependencyAnalysisResult.builder()
                .dependencyGraph(graph)
                .projectDependencies(projectDependencies)
                .packageDependencies(packageDependencies)
                .cycles(cycles)
                .versionConflicts(List.of())
                .success(true)
                .build();
            
            // 缓存结果
            cache.setProjectCache(projectPath, result);
            
            logger.info("项目依赖分析完成: " + projectPath);
            return result;
        } catch (Exception e) {
            logger.severe("分析项目依赖关系失败: " + e.getMessage());
            return DependencyAnalysisResult.builder()
                .success(false)
                .errorMessage("分析失败: " + e.getMessage())
                .build();
        }
    }
    
    @Override
    public List<Cycle> detectCycles(Path solutionPath) {
        DependencyAnalysisResult result = analyzeSolutionDependencies(solutionPath);
        return result.getCycles();
    }
    
    @Override
    public List<PackageVersionConflict> checkVersionConflicts(Path solutionPath) {
        DependencyAnalysisResult result = analyzeSolutionDependencies(solutionPath);
        return result.getVersionConflicts();
    }
    
    private List<PackageVersionConflict> checkVersionConflictsInternal(DependencyAnalysisResult result) {
        Map<String, Map<String, List<PackageDependency>>> packageVersionMap = new HashMap<>();
        
        // 按包ID和版本分组
        for (PackageDependency dependency : result.getPackageDependencies()) {
            packageVersionMap.computeIfAbsent(dependency.getPackageId(), k -> new HashMap<>())
                .computeIfAbsent(dependency.getResolvedVersion(), k -> new ArrayList<>())
                .add(dependency);
        }
        
        // 查找冲突
        List<PackageVersionConflict> conflicts = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<PackageDependency>>> entry : packageVersionMap.entrySet()) {
            String packageId = entry.getKey();
            Map<String, List<PackageDependency>> versionMap = entry.getValue();
            
            // 如果同一个包有多个版本，则存在冲突
            if (versionMap.size() > 1) {
                // 收集所有依赖
                List<PackageDependency> allDependencies = new ArrayList<>();
                for (List<PackageDependency> deps : versionMap.values()) {
                    allDependencies.addAll(deps);
                }
                
                conflicts.add(new PackageVersionConflict(
                    packageId, 
                    new HashSet<>(versionMap.keySet()),
                    allDependencies
                ));
            }
        }
        
        return conflicts;
    }
    
    private DependencyAnalysisResult mergeAnalysisResults(List<DependencyAnalysisResult> results) {
        DependencyGraph mergedGraph = new DependencyGraphImpl();
        List<ProjectDependency> allProjectDependencies = new ArrayList<>();
        List<PackageDependency> allPackageDependencies = new ArrayList<>();
        
        for (DependencyAnalysisResult result : results) {
            if (result.isSuccess()) {
                mergedGraph.merge(result.getDependencyGraph());
                allProjectDependencies.addAll(result.getProjectDependencies());
                allPackageDependencies.addAll(result.getPackageDependencies());
            }
        }
        
        return DependencyAnalysisResult.builder()
            .dependencyGraph(mergedGraph)
            .projectDependencies(allProjectDependencies)
            .packageDependencies(allPackageDependencies)
            .success(true)
            .build();
    }
    
    @Override
    public void clearCache() {
        logger.info("清除依赖分析缓存");
        cache.clear();
    }
}