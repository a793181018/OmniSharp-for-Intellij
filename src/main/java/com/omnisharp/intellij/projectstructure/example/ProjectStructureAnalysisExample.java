package com.omnisharp.intellij.projectstructure.example;

import com.omnisharp.intellij.projectstructure.api.FileSystemListener;
import com.omnisharp.intellij.projectstructure.api.ProjectStructureChangeListener;
import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.api.ProjectListener;
import com.omnisharp.intellij.projectstructure.service.DependencyService;
import com.omnisharp.intellij.projectstructure.service.ProjectManagerService;
import com.omnisharp.intellij.projectstructure.service.SymbolIndexService;
import com.omnisharp.intellij.projectstructure.utils.ProjectLogger;
import com.omnisharp.intellij.projectstructure.utils.PerformanceMonitor;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 项目结构分析示例代码，展示如何使用项目结构分析模块的各种功能
 */
public class ProjectStructureAnalysisExample {
    
    private static final ProjectLogger logger = ProjectLogger.getInstance(ProjectStructureAnalysisExample.class);
    private final ProjectManagerService projectManagerService;
    private final DependencyService dependencyService;
    private final SymbolIndexService symbolIndexService;
    
    /**
     * 构造函数
     * @param projectManagerService 项目管理服务
     * @param dependencyService 依赖服务
     * @param symbolIndexService 符号索引服务
     */
    public ProjectStructureAnalysisExample(
            @NotNull ProjectManagerService projectManagerService,
            @NotNull DependencyService dependencyService,
            @NotNull SymbolIndexService symbolIndexService) {
        this.projectManagerService = projectManagerService;
        this.dependencyService = dependencyService;
        this.symbolIndexService = symbolIndexService;
    }
    
    /**
     * 运行完整的项目结构分析示例
     * @param solutionPath 解决方案文件路径
     */
    public void runExample(@NotNull String solutionPath) {
        logger.info("Starting project structure analysis example...");
        
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("complete_analysis")) {
            // 1. 打开解决方案
            SolutionModel solution = openSolution(solutionPath);
            if (solution == null) {
                logger.error("Failed to open solution: " + solutionPath);
                return;
            }
            
            // 2. 注册事件监听器
            registerListeners(solution);
            
            // 3. 分析项目依赖
            analyzeDependencies(solution);
            
            // 4. 执行符号索引
            indexSymbols(solution);
            
            // 5. 搜索符号示例
            searchSymbolsExample(solution);
            
            // 6. 关闭解决方案
            closeSolution(solution.getPath());
            
            logger.info("Example completed successfully in " + timer.getElapsedMillis() + " ms");
        } catch (Exception e) {
            logger.error("Error running example", e);
        }
    }
    
    /**
     * 打开解决方案示例
     * @param solutionPath 解决方案文件路径
     * @return 解决方案对象
     */
    private SolutionModel openSolution(@NotNull String solutionPath) {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("open_solution")) {
            logger.infof("Opening solution: %s", solutionPath);
            
            // 检查文件是否存在
            if (!new File(solutionPath).exists()) {
                logger.error("Solution file not found: " + solutionPath);
                return null;
            }
            
            // 打开解决方案
            SolutionModel solution = projectManagerService.openSolution(solutionPath);
            
            if (solution != null) {
                logger.infof("Successfully opened solution: %s with %d projects", 
                        solution.getName(), solution.getProjects().size());
                
                // 打印项目信息
                for (ProjectModel project : solution.getProjects().values()) {
                    logger.infof("  - Project: %s (%s)", project.getName(), project.getPath());
                }
            }
            
            return solution;
        }
    }
    
    /**
     * 注册事件监听器示例
     * @param solution 解决方案对象
     */
    private void registerListeners(@NotNull SolutionModel solution) {
        logger.info("Registering event listeners...");
        
        // 注册项目结构变更监听器
        ProjectListener structureListener = new ProjectListener() {
            public void onSolutionOpened(SolutionModel solution) {
                logger.info("Solution opened: " + solution.getName());
            }
            
            public void onSolutionClosed(SolutionModel solution) {
                logger.info("Solution closed: " + solution.getPath());
            }
            
            public void onProjectAdded(SolutionModel solution, ProjectModel project) {
                logger.info("Project added: " + project.getName());
            }
            
            public void onProjectRemoved(SolutionModel solution, ProjectModel project) {
                logger.info("Project removed: " + project.getName());
            }
            
            public void onProjectRefreshed(ProjectModel project) {
                logger.info("Project refreshed: " + project.getName());
            }
            
            public void onSolutionRefreshed(SolutionModel solution) {
                logger.info("Solution refreshed: " + solution.getName());
            }
            
            public void onProjectConfigurationChanged(ProjectModel project) {
                logger.info("Project configuration changed: " + project.getName());
            }
            
            public void onProjectDependenciesChanged(ProjectModel project) {
                // 空实现
            }
            
            public void onSolutionLoadingStarted(String solutionPath) {
                // 空实现
            }
            
            public void onSolutionLoadingFinished(SolutionModel solution) {
                // 空实现
            }
            
            public void onSolutionLoadingFailed(String solutionPath, Exception error) {
                // 空实现
            }
            
            public void onProjectLoadingStarted(String projectPath) {
                // 空实现
            }
            
            public void onProjectLoadingFinished(ProjectModel project) {
                // 空实现
            }
            
            public void onProjectLoadingFailed(String projectPath, Exception error) {
                // 空实现
            }
        };
        
        projectManagerService.addProjectListener(structureListener);
        
        // 注册文件系统变更监听器
            FileSystemListener fileSystemListener = new FileSystemListener() {
                public void onFileCreated(Path file) {
                    logger.debug("File created: " + file);
                }
                
                public void onFileDeleted(Path file) {
                    logger.debug("File deleted: " + file);
                }
                
                public void onFileModified(Path file) {
                    logger.debug("File modified: " + file);
                }
                
                public void onFileMoved(Path oldPath, Path newPath) {
                    logger.debug("File moved: " + oldPath + " -> " + newPath);
                }
                
                public void onFolderCreated(Path folder) {
                    // 空实现
                }
                
                public void onFolderDeleted(Path folder) {
                    // 空实现
                }
                
                public void onFolderModified(Path folder) {
                    // 空实现
                }
                
                public void onFolderMoved(Path oldPath, Path newPath) {
                    // 空实现
                }
                
                public void onBatchChange(FileSystemListener.FileSystemBatchEvent event) {
                    // 空实现 - 处理批量文件系统变更
                }
            };
        
        projectManagerService.addFileSystemListener(solution.getPath(), fileSystemListener);
    }
    
    /**
     * 分析项目依赖示例
     * @param solution 解决方案对象
     */
    private void analyzeDependencies(@NotNull SolutionModel solution) {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("analyze_dependencies")) {
            logger.info("Analyzing project dependencies...");
            
            for (ProjectModel project : solution.getProjects().values()) {
                logger.infof("Analyzing dependencies for project: %s", project.getName());
                
                // 获取项目引用
                List<ProjectModel> projectReferences = dependencyService.getProjectReferences(project);
                if (!projectReferences.isEmpty()) {
                    logger.infof("  Project references (%d):", projectReferences.size());
                    for (ProjectModel ref : projectReferences) {
                        logger.infof("    - %s", ref.getName());
                    }
                }
                
                // 获取NuGet包引用
                List<String> packageReferences = dependencyService.getPackageReferences(project);
                if (!packageReferences.isEmpty()) {
                    logger.infof("  Package references (%d):", packageReferences.size());
                    for (String pkg : packageReferences.subList(0, Math.min(5, packageReferences.size()))) {
                        logger.infof("    - %s", pkg);
                    }
                    if (packageReferences.size() > 5) {
                        logger.infof("    ... and %d more packages", packageReferences.size() - 5);
                    }
                }
                
                // 检查是否有循环依赖
                boolean hasCircularDependencies = dependencyService.hasCircularDependencies(project);
                logger.infof("  Has circular dependencies: %s", hasCircularDependencies);
            }
            
            // 分析整个解决方案的依赖图
            logger.info("Generating solution dependency graph...");
            dependencyService.buildDependencyGraph(solution);
        }
    }
    
    /**
     * 执行符号索引示例
     * @param solution 解决方案对象
     */
    private void indexSymbols(@NotNull SolutionModel solution) {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("index_symbols")) {
            logger.info("Indexing symbols for solution: " + solution.getName());
            
            // 异步索引整个解决方案
            CompletableFuture<Boolean> indexingFuture = symbolIndexService.indexSolutionAsync(solution);
            
            // 等待索引完成
            boolean success = indexingFuture.join();
            
            if (success) {
                logger.info("Symbol indexing completed successfully");
                
                // 获取索引统计信息
                long symbolCount = symbolIndexService.getSymbolCount(solution.getPath());
                logger.infof("Indexed %d symbols in total", symbolCount);
            } else {
                logger.error("Symbol indexing failed");
            }
        }
    }
    
    /**
     * 符号搜索示例
     * @param solution 解决方案对象
     */
    private void searchSymbolsExample(@NotNull SolutionModel solution) {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("search_symbols")) {
            logger.info("Performing symbol search examples...");
            
            // 搜索类
            List<String> classes = symbolIndexService.searchSymbols(solution.getPath(), "class", "*");
            logger.infof("Found %d classes", classes.size());
            for (String className : classes.subList(0, Math.min(5, classes.size()))) {
                logger.info("  - " + className);
            }
            
            // 搜索方法（示例）
            List<String> methods = symbolIndexService.searchSymbols(solution.getPath(), "method", "*Test*");
            logger.infof("Found %d test methods", methods.size());
            
            // 搜索特定符号的引用
            if (!classes.isEmpty()) {
                String className = classes.get(0);
                List<String> references = symbolIndexService.findReferences(solution.getPath(), className);
                logger.infof("Found %d references to %s", references.size(), className);
            }
        }
    }
    
    /**
     * 关闭解决方案示例
     * @param solutionPath 解决方案文件路径
     */
    private void closeSolution(@NotNull String solutionPath) {
        try (PerformanceMonitor.Timer timer = PerformanceMonitor.startTimer("close_solution")) {
            logger.info("Closing solution: " + solutionPath);
            boolean success = projectManagerService.closeSolution(solutionPath);
            
            if (success) {
                logger.info("Solution closed successfully");
            } else {
                logger.warn("Failed to close solution");
            }
        }
    }
    
    /**
     * 主方法，用于独立运行示例
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 注意：在实际应用中，应该通过依赖注入获取服务实例
        // 这里仅作为示例，实际使用时需要适配IntelliJ平台的服务获取方式
        logger.warn("This example should be run within IntelliJ platform context");
        logger.warn("Use the ProjectStructureAnalysisExample class with proper service injection");
    }
}