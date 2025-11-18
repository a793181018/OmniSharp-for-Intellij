package com.github.a793181018.omnisharpforintellij.server;

import com.github.a793181018.omnisharpforintellij.communicator.OmniSharpServerClient;
import com.github.a793181018.omnisharpforintellij.communicator.StdioOmniSharpClient;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * OmniSharp服务器的默认实现
 */
public class OmniSharpServerImpl implements OmniSharpServer {
    private final String serverPath;
    private final String projectPath;
    private final ExecutorService executorService;
    private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.NOT_STARTED);
    private final List<OmniSharpServerListener> listeners = new CopyOnWriteArrayList<>();
    private OmniSharpServerClient client;
    private Throwable lastError;
    private static final int DEFAULT_START_TIMEOUT = 30; // 默认30秒启动超时
    private final int startTimeoutSeconds;
    
    public OmniSharpServerImpl(@NotNull String serverPath, @NotNull String projectPath) {
        this(serverPath, projectPath, DEFAULT_START_TIMEOUT);
    }
    
    public OmniSharpServerImpl(@NotNull String serverPath, @NotNull String projectPath, int startTimeoutSeconds) {
        this.serverPath = serverPath;
        this.projectPath = projectPath;
        this.startTimeoutSeconds = startTimeoutSeconds;
        this.executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "OmniSharp-Server-Manager");
            thread.setDaemon(true);
            return thread;
        });
    }
    
    @Override
    public CompletableFuture<Boolean> start() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        executorService.submit(() -> {
            try {
                if (status.get() != ServerStatus.NOT_STARTED && status.get() != ServerStatus.STOPPED) {
                    future.complete(false);
                    return;
                }
                
                updateStatus(ServerStatus.STARTING);
                
                // 检查服务器文件是否存在
                File serverFile = new File(serverPath);
                if (!serverFile.exists() || !serverFile.canExecute()) {
                    throw new IllegalStateException("OmniSharp server not found or not executable: " + serverPath);
                }
                
                // 检查项目目录是否存在
                File projectDir = new File(projectPath);
                if (!projectDir.exists() || !projectDir.isDirectory()) {
                    throw new IllegalStateException("Project directory not found: " + projectPath);
                }
                
                // 创建客户端实例
                client = new StdioOmniSharpClient(serverPath, projectPath);
                
                // 连接到服务器
                client.connect();
                
                // 更新状态为初始化中
                updateStatus(ServerStatus.INITIALIZING);
                
                // 等待服务器完全初始化（可以添加额外的初始化检查逻辑）
                boolean initialized = waitForInitialization();
                
                if (initialized) {
                    updateStatus(ServerStatus.RUNNING);
                    future.complete(true);
                    notifyServerStarted();
                } else {
                    updateStatus(ServerStatus.ERROR);
                    future.complete(false);
                }
            } catch (Exception e) {
                lastError = e;
                updateStatus(ServerStatus.ERROR);
                notifyServerError(e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    @Override
    public CompletableFuture<Boolean> stop() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        executorService.submit(() -> {
            try {
                if (status.get() != ServerStatus.STARTING && status.get() != ServerStatus.INITIALIZING && status.get() != ServerStatus.RUNNING) {
                    future.complete(false);
                    return;
                }
                
                updateStatus(ServerStatus.STOPPING);
                
                if (client != null) {
                    client.disconnect();
                    client = null;
                }
                
                updateStatus(ServerStatus.STOPPED);
                future.complete(true);
                notifyServerStopped();
            } catch (Exception e) {
                lastError = e;
                updateStatus(ServerStatus.ERROR);
                notifyServerError(e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }
    
    @Override
    public CompletableFuture<Boolean> restart() {
        return stop().thenCompose(success -> {
            if (success) {
                return start();
            } else {
                return CompletableFuture.completedFuture(false);
            }
        });
    }
    
    @Override
    @NotNull
    public ServerStatus getStatus() {
        return status.get();
    }
    
    @Override
    public boolean isRunning() {
        ServerStatus currentStatus = status.get();
        return currentStatus == ServerStatus.RUNNING;
    }
    
    @Override
    @Nullable
    public OmniSharpServerClient getClient() {
        return client;
    }
    
    @Override
    @NotNull
    public String getServerPath() {
        return serverPath;
    }
    
    @Override
    @NotNull
    public String getProjectPath() {
        return projectPath;
    }
    
    @Override
    @Nullable
    public Throwable getLastError() {
        return lastError;
    }
    
    @Override
    public void addServerListener(@NotNull OmniSharpServerListener listener) {
        listeners.add(listener);
    }
    
    @Override
    public void removeServerListener(@NotNull OmniSharpServerListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 更新服务器状态
     */
    private void updateStatus(@NotNull ServerStatus newStatus) {
        ServerStatus oldStatus = status.getAndSet(newStatus);
        if (oldStatus != newStatus) {
            notifyStatusChanged(oldStatus, newStatus);
        }
    }
    
    /**
     * 通知状态变化
     */
    private void notifyStatusChanged(@NotNull ServerStatus oldStatus, @NotNull ServerStatus newStatus) {
        for (OmniSharpServerListener listener : listeners) {
            try {
                listener.onStatusChanged(this, oldStatus, newStatus);
            } catch (Exception e) {
                // 记录异常但不中断通知链
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 通知服务器已启动
     */
    private void notifyServerStarted() {
        for (OmniSharpServerListener listener : listeners) {
            try {
                listener.onServerStarted(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 通知服务器已停止
     */
    private void notifyServerStopped() {
        for (OmniSharpServerListener listener : listeners) {
            try {
                listener.onServerStopped(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 通知服务器错误
     */
    private void notifyServerError(@NotNull Throwable error) {
        for (OmniSharpServerListener listener : listeners) {
            try {
                listener.onServerError(this, error);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 等待服务器初始化完成
     */
    private boolean waitForInitialization() {
        // 这里可以添加更复杂的初始化检查逻辑
        // 目前简单地等待一段时间
        try {
            Thread.sleep(2000); // 等待2秒
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        stop();
        executorService.shutdown();
        try {
            executorService.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        listeners.clear();
    }
}