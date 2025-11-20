package com.omnisharp.intellij.projectstructure.watcher;

import com.omnisharp.intellij.projectstructure.parser.ProjectParserFacade;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * OmniSharp项目文件监控器实现
 * 使用Java NIO的WatchService实现文件系统监控
 */
public class OmniSharpProjectFileWatcher implements ProjectFileWatcher {
    private static final Logger LOGGER = Logger.getLogger(OmniSharpProjectFileWatcher.class.getName());
    
    // 监控服务
    private WatchService watchService;
    // 执行监控的线程池
    private ExecutorService executorService;
    // 监控线程
    private Thread watchThread;
    // 当前监控的项目文件路径
    private Path projectPath;
    // 项目文件的父目录
    private Path projectDirectory;
    // 变更监听器列表
    private final List<ProjectFileChangeListener> listeners = new CopyOnWriteArrayList<>();
    // 监控状态
    private volatile boolean isWatching = false;
    // 停止标记
    private volatile boolean isStopping = false;
    // 监控的文件类型列表
    private final Set<String> watchedFileExtensions = new HashSet<>(
            Arrays.asList(".csproj", ".fsproj", ".vbproj", ".sln", "packages.config"));
    
    @Override
    public synchronized void startWatching(Path projectPath) {
        if (isWatching) {
            LOGGER.warning("Watcher is already running");
            return;
        }
        
        if (projectPath == null || !Files.exists(projectPath)) {
            throw new IllegalArgumentException("Invalid project file path: " + projectPath);
        }
        
        this.projectPath = projectPath;
        this.projectDirectory = projectPath.getParent();
        this.isWatching = true;
        this.isStopping = false;
        
        try {
            // 初始化WatchService
            watchService = FileSystems.getDefault().newWatchService();
            
            // 注册项目文件所在目录进行监控
            projectDirectory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            
            // 监控packages.config文件所在目录（通常是项目根目录）
            Path packagesConfigPath = projectDirectory.resolve("packages.config");
            if (Files.exists(packagesConfigPath)) {
                projectDirectory.register(watchService, ENTRY_MODIFY, ENTRY_DELETE);
            }
            
            // 监控项目引用文件
            monitorProjectReferences();
            
            // 启动监控线程
            executorService = Executors.newSingleThreadExecutor();
            watchThread = new Thread(this::watch);
            watchThread.setDaemon(true);
            watchThread.setName("ProjectFileWatcher-");
            executorService.submit(watchThread);
            
            LOGGER.info("Started watching project file: " + projectPath);
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to start watching project file", e);
            notifyError(e);
            isWatching = false;
        }
    }
    
    @Override
    public synchronized void stopWatching() {
        if (!isWatching || isStopping) {
            return;
        }
        
        isStopping = true;
        isWatching = false;
        
        try {
            // 关闭WatchService
            if (watchService != null) {
                watchService.close();
            }
            
            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                executorService.awaitTermination(5, TimeUnit.SECONDS);
                executorService.shutdownNow();
            }
            
            // 中断监控线程
            if (watchThread != null && watchThread.isAlive()) {
                watchThread.interrupt();
            }
            
            LOGGER.info("Stopped watching project file: " + projectPath);
            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error stopping watcher", e);
        } finally {
            watchService = null;
            executorService = null;
            watchThread = null;
            projectPath = null;
            projectDirectory = null;
            isStopping = false;
        }
    }
    
    @Override
    public void addProjectFileChangeListener(ProjectFileChangeListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeProjectFileChangeListener(ProjectFileChangeListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    @Override
    public boolean isWatching() {
        return isWatching;
    }
    
    /**
     * 监控项目引用的文件
     */
    private void monitorProjectReferences() {
        try {
            // 这里可以通过ProjectParserFacade解析项目引用并监控这些引用文件
            // 为简化实现，这里不进行实际解析
            LOGGER.info("Monitoring project references not fully implemented in simplified version");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to monitor project references", e);
        }
    }
    
    /**
     * 监控循环
     */
    private void watch() {
        try {
            while (isWatching && !Thread.currentThread().isInterrupted()) {
                WatchKey key;
                try {
                    // 等待文件变更事件，超时设为1秒，以便定期检查isWatching状态
                    key = watchService.poll(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                
                if (key == null) {
                    // 超时，继续循环检查状态
                    continue;
                }
                
                // 处理所有变更事件
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    
                    // 忽略OVERFLOW事件
                    if (kind == OVERFLOW) {
                        continue;
                    }
                    
                    // 获取变更的文件路径
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path fileName = ev.context();
                    Path fullPath = projectDirectory.resolve(fileName);
                    
                    // 检查是否是需要监控的文件类型
                    String filePathStr = fullPath.toString();
                    boolean isWatchedFile = isWatchedFile(filePathStr);
                    
                    if (isWatchedFile) {
                        handleFileEvent(kind, fullPath);
                    }
                }
                
                // 重置WatchKey
                boolean valid = key.reset();
                if (!valid) {
                    // 目录不再可访问，停止监控
                    LOGGER.warning("Directory no longer accessible, stopping watch");
                    break;
                }
            }
        } catch (Exception e) {
            if (!isStopping) {
                LOGGER.log(Level.SEVERE, "Error in watch loop", e);
                notifyError(e);
            }
        } finally {
            isWatching = false;
        }
    }
    
    /**
     * 处理文件事件
     */
    private void handleFileEvent(WatchEvent.Kind<?> kind, Path filePath) {
        if (kind == ENTRY_CREATE) {
            LOGGER.info("Project file created: " + filePath);
            notifyFileCreated(filePath);
        } else if (kind == ENTRY_MODIFY) {
            LOGGER.info("Project file modified: " + filePath);
            notifyFileModified(filePath);
        } else if (kind == ENTRY_DELETE) {
            LOGGER.info("Project file deleted: " + filePath);
            notifyFileDeleted(filePath);
        }
    }
    
    /**
     * 检查是否是需要监控的文件类型
     */
    private boolean isWatchedFile(String filePath) {
        // 检查文件扩展名
        for (String ext : watchedFileExtensions) {
            if (filePath.toLowerCase().endsWith(ext)) {
                return true;
            }
        }
        
        // 检查是否是当前监控的项目文件
        if (projectPath != null && filePath.equals(projectPath.toString())) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 通知所有监听器文件被创建
     */
    private void notifyFileCreated(Path filePath) {
        for (ProjectFileChangeListener listener : listeners) {
            try {
                listener.projectFileCreated(filePath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in file created listener", e);
            }
        }
    }
    
    /**
     * 通知所有监听器文件被修改
     */
    private void notifyFileModified(Path filePath) {
        for (ProjectFileChangeListener listener : listeners) {
            try {
                listener.projectFileModified(filePath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in file modified listener", e);
            }
        }
    }
    
    /**
     * 通知所有监听器文件被删除
     */
    private void notifyFileDeleted(Path filePath) {
        for (ProjectFileChangeListener listener : listeners) {
            try {
                listener.projectFileDeleted(filePath);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in file deleted listener", e);
            }
        }
    }
    
    /**
     * 通知所有监听器发生错误
     */
    private void notifyError(Exception error) {
        for (ProjectFileChangeListener listener : listeners) {
            try {
                listener.watcherError(error);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in error listener", e);
            }
        }
    }
    
    /**
     * 添加需要监控的文件扩展名
     */
    public void addWatchedFileExtension(String extension) {
        if (extension != null && extension.startsWith(".")) {
            watchedFileExtensions.add(extension);
        }
    }
    
    /**
     * 移除需要监控的文件扩展名
     */
    public void removeWatchedFileExtension(String extension) {
        if (extension != null) {
            watchedFileExtensions.remove(extension);
        }
    }
}