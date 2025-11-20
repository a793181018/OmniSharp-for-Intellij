package com.omnisharp.intellij.projectstructure.manager;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.parser.ProjectParserFacade;
import com.omnisharp.intellij.projectstructure.parser.OmniSharpProjectParserFacade;
import com.omnisharp.intellij.projectstructure.watcher.ProjectFileWatcher;
import com.omnisharp.intellij.projectstructure.watcher.OmniSharpProjectFileWatcher;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OmniSharp项目文件管理器实现
 * 集成了解析器和监控器，提供完整的项目管理功能
 */
public class OmniSharpProjectFileManager implements ProjectFileManager {
    private static final Logger LOGGER = Logger.getLogger(OmniSharpProjectFileManager.class.getName());
    
    // 项目解析器
    private final ProjectParserFacade parserFacade;
    
    // 项目模型缓存
    private final Map<Path, ProjectModel> projectModels = new ConcurrentHashMap<>();
    
    // 项目监控器映射
    private final Map<Path, ProjectFileWatcher> projectWatchers = new ConcurrentHashMap<>();
    
    // 项目变更监听器列表
    private final List<ProjectChangeListener> listeners = new CopyOnWriteArrayList<>();
    
    // 异步任务执行器
    private final ExecutorService executorService;
    
    // 缓存读写锁
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    // 单例实例
    private static final OmniSharpProjectFileManager INSTANCE = new OmniSharpProjectFileManager();
    
    /**
     * 私有构造函数
     */
    private OmniSharpProjectFileManager() {
        this.parserFacade = new OmniSharpProjectParserFacade();
        this.executorService = Executors.newFixedThreadPool(
                Math.max(2, Runtime.getRuntime().availableProcessors()),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(1);
                    
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "ProjectFileManager-Worker-" + counter.getAndIncrement());
                        thread.setDaemon(true);
                        return thread;
                    }
                }
        );
    }
    
    /**
     * 获取单例实例
     */
    public static OmniSharpProjectFileManager getInstance() {
        return INSTANCE;
    }
    
    @Override
    public ProjectModel loadProject(Path projectPath) throws ProjectManagerException {
        if (projectPath == null) {
            throw new IllegalArgumentException("Project path cannot be null");
        }
        
        try {
            // 转换为绝对路径
            Path absolutePath = projectPath.toAbsolutePath().normalize();
            
            // 检查是否已加载
            cacheLock.readLock().lock();
            try {
                ProjectModel cachedModel = projectModels.get(absolutePath);
                if (cachedModel != null) {
                    LOGGER.info("Returning cached project model for: " + absolutePath);
                    return cachedModel;
                }
            } finally {
                cacheLock.readLock().unlock();
            }
            
            // 解析项目文件
            LOGGER.info("Loading project file: " + absolutePath);
            ProjectModel model = parserFacade.parse(absolutePath);
            
            // 存储到缓存
            cacheLock.writeLock().lock();
            try {
                projectModels.put(absolutePath, model);
            } finally {
                cacheLock.writeLock().unlock();
            }
            
            // 通知监听器
            notifyProjectLoaded(model);
            
            return model;
        } catch (Exception e) {
            String errorMessage = "Failed to load project: " + projectPath;
            LOGGER.log(Level.SEVERE, errorMessage, e);
            notifyError(new ProjectManagerException(errorMessage, e), projectPath);
            throw new ProjectManagerException(errorMessage, e);
        }
    }
    
    @Override
    public CompletableFuture<ProjectModel> loadProjectAsync(Path projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadProject(projectPath);
            } catch (ProjectManagerException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    @Override
    public ProjectModel getProjectModel(Path projectPath) {
        if (projectPath == null) {
            return null;
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        cacheLock.readLock().lock();
        try {
            return projectModels.get(absolutePath);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public List<Path> getLoadedProjects() {
        cacheLock.readLock().lock();
        try {
            return new ArrayList<>(projectModels.keySet());
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public ProjectModel refreshProject(Path projectPath) throws ProjectManagerException {
        if (projectPath == null) {
            throw new IllegalArgumentException("Project path cannot be null");
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        
        // 检查项目是否已加载
        if (!isProjectLoaded(absolutePath)) {
            throw new ProjectManagerException("Project not loaded: " + absolutePath);
        }
        
        try {
            LOGGER.info("Refreshing project: " + absolutePath);
            
            // 重新解析项目文件
            ProjectModel model = parserFacade.reload(absolutePath);
            
            // 更新缓存
            cacheLock.writeLock().lock();
            try {
                projectModels.put(absolutePath, model);
            } finally {
                cacheLock.writeLock().unlock();
            }
            
            // 通知监听器
            notifyProjectRefreshed(model);
            
            return model;
        } catch (Exception e) {
            String errorMessage = "Failed to refresh project: " + absolutePath;
            LOGGER.log(Level.SEVERE, errorMessage, e);
            notifyError(new ProjectManagerException(errorMessage, e), absolutePath);
            throw new ProjectManagerException(errorMessage, e);
        }
    }
    
    @Override
    public CompletableFuture<ProjectModel> refreshProjectAsync(Path projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return refreshProject(projectPath);
            } catch (ProjectManagerException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    @Override
    public void closeProject(Path projectPath) {
        if (projectPath == null) {
            return;
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        
        // 停止监控
        ProjectFileWatcher watcher = projectWatchers.remove(absolutePath);
        if (watcher != null) {
            watcher.stopWatching();
        }
        
        // 移除缓存的项目模型
        cacheLock.writeLock().lock();
        try {
            projectModels.remove(absolutePath);
        } finally {
            cacheLock.writeLock().unlock();
        }
        
        LOGGER.info("Closed project: " + absolutePath);
        
        // 通知监听器
        notifyProjectClosed(absolutePath);
    }
    
    @Override
    public void closeAllProjects() {
        // 停止所有监控
        for (ProjectFileWatcher watcher : projectWatchers.values()) {
            try {
                watcher.stopWatching();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping watcher", e);
            }
        }
        projectWatchers.clear();
        
        // 清除所有缓存
        List<Path> closedProjects;
        cacheLock.writeLock().lock();
        try {
            closedProjects = new ArrayList<>(projectModels.keySet());
            projectModels.clear();
        } finally {
            cacheLock.writeLock().unlock();
        }
        
        LOGGER.info("Closed all projects: " + closedProjects.size() + " projects");
        
        // 通知监听器
        for (Path projectPath : closedProjects) {
            notifyProjectClosed(projectPath);
        }
    }
    
    @Override
    public boolean isProjectLoaded(Path projectPath) {
        if (projectPath == null) {
            return false;
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        cacheLock.readLock().lock();
        try {
            return projectModels.containsKey(absolutePath);
        } finally {
            cacheLock.readLock().unlock();
        }
    }
    
    @Override
    public void startWatching(Path projectPath) {
        if (projectPath == null) {
            return;
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        
        // 检查项目是否已加载
        if (!isProjectLoaded(absolutePath)) {
            LOGGER.warning("Cannot start watching unloaded project: " + absolutePath);
            return;
        }
        
        // 检查是否已经在监控
        if (projectWatchers.containsKey(absolutePath)) {
            LOGGER.warning("Project is already being watched: " + absolutePath);
            return;
        }
        
        try {
            // 创建并配置监控器
            ProjectFileWatcher watcher = new OmniSharpProjectFileWatcher();
            watcher.addProjectFileChangeListener(new ProjectFileWatcher.ProjectFileChangeListener() {
                @Override
                public void projectFileCreated(Path filePath) {
                    // 项目文件创建时不需要自动刷新，等待用户操作
                    LOGGER.info("Project file created: " + filePath);
                }
                
                @Override
                public void projectFileModified(Path filePath) {
                    LOGGER.info("Project file modified, refreshing project: " + filePath);
                    try {
                        // 自动刷新项目
                        ProjectModel updatedModel = refreshProject(absolutePath);
                        notifyProjectChanged(absolutePath, updatedModel);
                    } catch (ProjectManagerException e) {
                        notifyError(e, absolutePath);
                    }
                }
                
                @Override
                public void projectFileDeleted(Path filePath) {
                    LOGGER.warning("Project file deleted: " + filePath);
                    // 如果是项目文件本身被删除，则关闭项目
                    if (filePath.equals(absolutePath)) {
                        closeProject(absolutePath);
                    }
                }
                
                @Override
                public void watcherError(Exception error) {
                    LOGGER.log(Level.SEVERE, "Watcher error for project: " + absolutePath, error);
                    notifyError(new ProjectManagerException("Watcher error", error), absolutePath);
                }
            });
            
            // 启动监控
            watcher.startWatching(absolutePath);
            projectWatchers.put(absolutePath, watcher);
            
            LOGGER.info("Started watching project file: " + absolutePath);
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start watching project: " + absolutePath, e);
            notifyError(new ProjectManagerException("Failed to start watching project", e), absolutePath);
        }
    }
    
    @Override
    public void stopWatching(Path projectPath) {
        if (projectPath == null) {
            return;
        }
        
        Path absolutePath = projectPath.toAbsolutePath().normalize();
        ProjectFileWatcher watcher = projectWatchers.remove(absolutePath);
        
        if (watcher != null) {
            try {
                watcher.stopWatching();
                LOGGER.info("Stopped watching project file: " + absolutePath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error stopping watcher for project: " + absolutePath, e);
            }
        }
    }
    
    @Override
    public void addProjectChangeListener(ProjectChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeProjectChangeListener(ProjectChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 通知项目已加载
     */
    private void notifyProjectLoaded(ProjectModel projectModel) {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.projectLoaded(projectModel);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in project loaded listener", e);
            }
        }
    }
    
    /**
     * 通知项目已刷新
     */
    private void notifyProjectRefreshed(ProjectModel projectModel) {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.projectRefreshed(projectModel);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in project refreshed listener", e);
            }
        }
    }
    
    /**
     * 通知项目已关闭
     */
    private void notifyProjectClosed(Path projectPath) {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.projectClosed(projectPath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in project closed listener", e);
            }
        }
    }
    
    /**
     * 通知项目已变更
     */
    private void notifyProjectChanged(Path projectPath, ProjectModel projectModel) {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.projectChanged(projectPath, projectModel);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in project changed listener", e);
            }
        }
    }
    
    /**
     * 通知错误
     */
    private void notifyError(Exception error, Path projectPath) {
        for (ProjectChangeListener listener : listeners) {
            try {
                listener.projectError(error, projectPath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in error listener", e);
            }
        }
    }
    
    /**
     * 关闭管理器，释放资源
     */
    public void shutdown() {
        closeAllProjects();
        
        try {
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
        
        LOGGER.info("Project file manager shut down");
    }
}