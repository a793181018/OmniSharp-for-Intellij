package com.omnisharp.intellij.projectstructure.example;

import com.omnisharp.intellij.projectstructure.manager.OmniSharpProjectFileManager;
import com.omnisharp.intellij.projectstructure.manager.ProjectFileManager;
import com.omnisharp.intellij.projectstructure.model.*;
import com.omnisharp.intellij.projectstructure.parser.OmniSharpProjectParserFacade;
import com.omnisharp.intellij.projectstructure.parser.ProjectParserFacade;
import com.omnisharp.intellij.projectstructure.watcher.OmniSharpProjectFileWatcher;
import com.omnisharp.intellij.projectstructure.watcher.ProjectFileWatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 项目文件解析器示例代码
 * 演示如何使用项目文件解析和管理功能
 */
public class ProjectFileParserExample {
    
    public static void main(String[] args) {
        try {
            // 示例1: 使用ProjectParserFacade直接解析项目文件
            parseProjectDirectlyExample();
            
            // 示例2: 使用ProjectFileManager管理项目生命周期
            manageProjectExample();
            
            // 示例3: 异步加载项目
            asyncLoadProjectExample();
            
            // 示例4: 监控项目文件变更
            watchProjectExample();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 示例1: 直接使用解析器解析项目文件
     */
    private static void parseProjectDirectlyExample() {
        System.out.println("\n=== 示例1: 直接使用解析器解析项目文件 ===");
        
        try {
            // 创建解析器实例
            ProjectParserFacade parserFacade = new OmniSharpProjectParserFacade();
            
            // 这里需要替换为实际的项目文件路径
            Path projectPath = Paths.get("path/to/your/project.csproj");
            
            System.out.println("开始解析项目文件: " + projectPath);
            
            // 解析项目文件
            ProjectModel projectModel = parserFacade.parse(projectPath);
            
            // 显示项目基本信息
            System.out.println("项目名称: " + projectModel.getName());
            System.out.println("项目路径: " + projectModel.getPath());
            System.out.println("项目类型: " + projectModel.getProjectType());
            System.out.println("目标框架: " + projectModel.getTargetFramework());
            System.out.println("是否SDK项目: " + projectModel.isSdkProject());
            
            // 显示项目引用信息
            System.out.println("\n项目引用数量: " + projectModel.getProjectReferences().size());
            for (ProjectReference ref : projectModel.getProjectReferences()) {
                System.out.println("  - 引用项目: " + ref.getName() + " (" + ref.getPath() + ")");
            }
            
            // 显示包引用信息
            System.out.println("\nNuGet包引用数量: " + projectModel.getPackageReferences().size());
            for (PackageReference ref : projectModel.getPackageReferences()) {
                System.out.println("  - 包: " + ref.getId() + " (v" + ref.getVersion() + ")");
            }
            
        } catch (Exception e) {
            System.out.println("解析项目文件失败: " + e.getMessage());
        }
    }
    
    /**
     * 示例2: 使用项目管理器管理项目生命周期
     */
    private static void manageProjectExample() throws ProjectFileManager.ProjectManagerException {
        System.out.println("\n=== 示例2: 使用项目管理器管理项目生命周期 ===");
        
        // 获取项目管理器实例（单例模式）
        OmniSharpProjectFileManager manager = OmniSharpProjectFileManager.getInstance();
        
        // 这里需要替换为实际的项目文件路径
        Path projectPath = Paths.get("path/to/your/project.csproj");
        
        try {
            // 注册项目变更监听器
            manager.addProjectChangeListener(new ProjectFileManager.ProjectChangeListener() {
                @Override
                public void projectLoaded(ProjectModel projectModel) {
                    System.out.println("[监听器] 项目已加载: " + projectModel.getName());
                }
                
                @Override
                public void projectRefreshed(ProjectModel projectModel) {
                    System.out.println("[监听器] 项目已刷新: " + projectModel.getName());
                }
                
                @Override
                public void projectClosed(Path path) {
                    System.out.println("[监听器] 项目已关闭: " + path);
                }
                
                @Override
                public void projectChanged(Path path, ProjectModel projectModel) {
                    System.out.println("[监听器] 项目已变更: " + projectModel.getName());
                }
                
                @Override
                public void projectError(Exception error, Path path) {
                    System.out.println("[监听器] 项目错误: " + error.getMessage());
                }
            });
            
            // 加载项目
            System.out.println("加载项目: " + projectPath);
            ProjectModel model = manager.loadProject(projectPath);
            
            // 检查项目是否已加载
            boolean isLoaded = manager.isProjectLoaded(projectPath);
            System.out.println("项目是否已加载: " + isLoaded);
            
            // 获取所有已加载的项目
            List<Path> loadedProjects = manager.getLoadedProjects();
            System.out.println("已加载的项目数量: " + loadedProjects.size());
            
            // 刷新项目
            System.out.println("刷新项目");
            model = manager.refreshProject(projectPath);
            
            // 显示编译文件信息
            System.out.println("\n编译文件数量: " + model.getCompileFiles().size());
            if (!model.getCompileFiles().isEmpty()) {
                System.out.println("  前5个编译文件:");
                int count = 0;
                for (String file : model.getCompileFiles()) {
                    System.out.println("  - " + file);
                    if (++count >= 5) break;
                }
            }
            
            // 关闭项目
            System.out.println("\n关闭项目");
            manager.closeProject(projectPath);
            
        } catch (ProjectFileManager.ProjectManagerException e) {
            System.out.println("项目管理操作失败: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * 示例3: 异步加载项目
     */
    private static void asyncLoadProjectExample() {
        System.out.println("\n=== 示例3: 异步加载项目 ===");
        
        // 获取项目管理器实例
        OmniSharpProjectFileManager manager = OmniSharpProjectFileManager.getInstance();
        
        // 这里需要替换为实际的项目文件路径
        Path projectPath = Paths.get("path/to/your/project.csproj");
        
        try {
            System.out.println("开始异步加载项目: " + projectPath);
            
            // 异步加载项目
            CompletableFuture<ProjectModel> future = manager.loadProjectAsync(projectPath);
            
            // 注册回调
            future.thenAccept(model -> {
                System.out.println("[异步] 项目加载完成: " + model.getName());
                System.out.println("[异步] 项目目录: " + model.getDirectory());
                System.out.println("[异步] 输出类型: " + model.getOutputType());
            });
            
            future.exceptionally(ex -> {
                System.out.println("[异步] 项目加载失败: " + ex.getMessage());
                return null;
            });
            
            // 等待异步操作完成
            ProjectModel model = future.get(10, TimeUnit.SECONDS);
            
            if (model != null) {
                // 执行其他操作
                System.out.println("\n获取项目属性信息:");
                model.getProperties().forEach((key, value) -> {
                    System.out.println("  " + key + ": " + value);
                });
                
                // 关闭项目
                manager.closeProject(projectPath);
            }
            
        } catch (Exception e) {
            System.out.println("异步操作失败: " + e.getMessage());
        }
    }
    
    /**
     * 示例4: 监控项目文件变更
     */
    private static void watchProjectExample() throws InterruptedException {
        System.out.println("\n=== 示例4: 监控项目文件变更 ===");
        
        // 这里需要替换为实际的项目文件路径
        Path projectPath = Paths.get("path/to/your/project.csproj");
        
        try {
            // 方式1: 使用项目管理器内置的监控功能
            useManagerWatchExample(projectPath);
            
            // 方式2: 直接使用文件监控器
            // useDirectWatchExample(projectPath);
            
        } catch (Exception e) {
            System.out.println("监控示例执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 使用项目管理器内置的监控功能
     */
    private static void useManagerWatchExample(Path projectPath) throws Exception {
        System.out.println("使用项目管理器监控项目文件: " + projectPath);
        
        // 获取项目管理器实例
        OmniSharpProjectFileManager manager = OmniSharpProjectFileManager.getInstance();
        
        // 加载项目
        ProjectModel model = manager.loadProject(projectPath);
        
        // 启动监控
        manager.startWatching(projectPath);
        System.out.println("已启动项目监控，将监控文件变更10秒...");
        
        // 监控10秒后停止
        Thread.sleep(10000);
        
        // 停止监控
        manager.stopWatching(projectPath);
        manager.closeProject(projectPath);
        System.out.println("已停止项目监控并关闭项目");
    }
    
    /**
     * 直接使用文件监控器
     */
    private static void useDirectWatchExample(Path projectPath) throws Exception {
        System.out.println("直接使用文件监控器: " + projectPath);
        
        // 创建文件监控器
        ProjectFileWatcher watcher = new OmniSharpProjectFileWatcher();
        
        // 注册文件变更监听器
        watcher.addProjectFileChangeListener(new ProjectFileWatcher.ProjectFileChangeListener() {
            @Override
            public void projectFileCreated(Path filePath) {
                System.out.println("文件已创建: " + filePath);
            }
            
            @Override
            public void projectFileModified(Path filePath) {
                System.out.println("文件已修改: " + filePath);
                // 可以在这里手动刷新项目
            }
            
            @Override
            public void projectFileDeleted(Path filePath) {
                System.out.println("文件已删除: " + filePath);
            }
            
            @Override
            public void watcherError(Exception error) {
                System.out.println("监控器错误: " + error.getMessage());
            }
        });
        
        // 启动监控
        watcher.startWatching(projectPath);
        System.out.println("已启动直接文件监控，将监控文件变更10秒...");
        
        // 监控10秒后停止
        Thread.sleep(10000);
        
        // 停止监控
        watcher.stopWatching();
        System.out.println("已停止直接文件监控");
    }
    
    /**
     * 示例5: 高级用法 - 同时管理多个项目
     */
    private static void manageMultipleProjectsExample() {
        System.out.println("\n=== 示例5: 高级用法 - 同时管理多个项目 ===");
        
        // 获取项目管理器实例
        OmniSharpProjectFileManager manager = OmniSharpProjectFileManager.getInstance();
        
        try {
            // 这里需要替换为实际的项目文件路径
            Path project1Path = Paths.get("path/to/your/project1.csproj");
            Path project2Path = Paths.get("path/to/your/project2.csproj");
            
            // 异步加载多个项目
            CompletableFuture<ProjectModel> future1 = manager.loadProjectAsync(project1Path);
            CompletableFuture<ProjectModel> future2 = manager.loadProjectAsync(project2Path);
            
            // 等待所有项目加载完成
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(future1, future2);
            allFutures.join();
            
            System.out.println("所有项目加载完成");
            
            // 获取已加载的项目列表
            List<Path> loadedProjects = manager.getLoadedProjects();
            System.out.println("当前已加载的项目数量: " + loadedProjects.size());
            
            // 关闭所有项目
            System.out.println("关闭所有项目");
            manager.closeAllProjects();
            
        } catch (Exception e) {
            System.out.println("多项目管理操作失败: " + e.getMessage());
        }
    }
}