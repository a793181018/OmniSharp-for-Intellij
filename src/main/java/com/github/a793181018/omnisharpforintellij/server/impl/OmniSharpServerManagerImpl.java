package com.github.a793181018.omnisharpforintellij.server.impl;

import com.github.a793181018.omnisharpforintellij.server.IOmniSharpServerManager;
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication;
import com.github.a793181018.omnisharpforintellij.server.communication.impl.OmniSharpCommunicationImpl;
import com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration;
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpConfigurationException;
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpException;
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpServerStartupException;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.server.model.ServerStatus;
import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager;
import com.github.a793181018.omnisharpforintellij.server.process.ProcessListener;
import com.github.a793181018.omnisharpforintellij.server.process.impl.OmniSharpProcessManagerImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.ConcurrencyUtil;
import org.jetbrains.annotations.NotNull;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * OmniSharp服务器管理器的具体实现，协调配置、进程和通信
 */
@Service
public class OmniSharpServerManagerImpl implements IOmniSharpServerManager, Disposable, ProcessListener {
    private static final Logger LOG = Logger.getInstance(OmniSharpServerManagerImpl.class);
    
    private final IOmniSharpConfiguration configuration;
    private final IOmniSharpProcessManager processManager;
    private final IOmniSharpCommunication communication;
    
    private final AtomicReference<ServerStatus> status = new AtomicReference<>(ServerStatus.NOT_STARTED);
    private final CountDownLatch startupLatch = new CountDownLatch(1);
    private final ScheduledExecutorService scheduler = ConcurrencyUtil.newSingleScheduledThreadExecutor("OmniSharp-Server-Manager");
    private final CopyOnWriteArrayList<Runnable> statusChangeListeners = new CopyOnWriteArrayList<>();
    
    private boolean isDisposed = false;
    private ScheduledFuture<?> startupTimeoutFuture = null;
    
    public OmniSharpServerManagerImpl(IOmniSharpConfiguration configuration) {
        this.configuration = configuration;
        this.processManager = new OmniSharpProcessManagerImpl();
        this.communication = new OmniSharpCommunicationImpl();
        
        // 添加进程监听器
        processManager.addProcessListener(this);
        
        // 初始化通信组件
        communication.initialize(processManager);
    }
    
    /**
     * 内部启动实现
     * @return 是否成功启动
     */
    public boolean start() {
        if (!checkDisposed()) {
            return false;
        }
        
        // 检查状态，只有NOT_STARTED或STOPPED状态可以启动
        if (status.get() != ServerStatus.NOT_STARTED && status.get() != ServerStatus.STOPPED) {
            LOG.warn("Server is already in state: " + status.get() + ", cannot start");
            return false;
        }
        
        LOG.info("Starting OmniSharp server");
        
        try {
            // 验证配置
            IOmniSharpConfiguration.ValidationResult validation = configuration.validate();
            if (!validation.isValid()) {
                throw new OmniSharpConfigurationException("Invalid configuration: " + validation.getErrorMessage());
            }
            
            // 更新状态为启动中
            updateStatus(ServerStatus.STARTING);
            
            // 准备启动参数
            String executablePath = configuration.getServerPath();
            File workingDirectory = configuration.getWorkingDirectory();
            List<String> arguments = configuration.getArguments();
            
            LOG.debug("Starting server with path: " + executablePath);
            LOG.debug("Working directory: " + workingDirectory);
            LOG.debug("Arguments: " + arguments);
            
            // 启动服务器进程
            boolean started = processManager.startProcess(executablePath, workingDirectory, arguments).join();
            
            if (started) {
                // 设置启动超时
                long startupTimeoutMs = configuration.getMaxStartupWaitTime();
                startupTimeoutFuture = scheduler.schedule(() -> {
                    if (status.get() == ServerStatus.STARTING) {
                        LOG.warn("Server startup timed out after " + startupTimeoutMs + "ms");
                        handleServerStartupFailed(new OmniSharpServerStartupException("Server startup timed out"));
                    }
                }, startupTimeoutMs, TimeUnit.MILLISECONDS);
                
                return true;
            } else {
                updateStatus(ServerStatus.FAILED);
                return false;
            }
        } catch (Exception e) {
            handleServerStartupFailed(e);
            return false;
        }
    }
    
    /**
     * 启动服务器
     * @return 是否成功启动的CompletableFuture
     */
    @Override
    public CompletableFuture<Boolean> startServer() {
        LOG.info("Starting OmniSharp server via startServer method");
        return CompletableFuture.supplyAsync(() -> start());
    }
    
    /**
     * 内部停止实现
     * @return 是否成功停止
     */
    public boolean stop() {
        if (!checkDisposed()) {
            return false;
        }
        
        // 取消启动超时任务
        cancelStartupTimeout();
        
        // 检查状态，只有STARTING、RUNNING状态可以停止
        if (status.get() != ServerStatus.STARTING && status.get() != ServerStatus.RUNNING) {
            LOG.warn("Server is not running, cannot stop. Current state: " + status.get());
            return false;
        }
        
        LOG.info("Stopping OmniSharp server");
        
        updateStatus(ServerStatus.STOPPING);
        
        try {
            // 发送退出命令
            sendExitCommand();
            
            // 停止进程
            boolean stopped = processManager.stopProcess().join();
            
            if (stopped) {
                updateStatus(ServerStatus.STOPPED);
                startupLatch.countDown(); // 确保等待启动的线程不会永久阻塞
            } else {
                updateStatus(ServerStatus.FAILED);
            }
            
            return stopped;
        } catch (Exception e) {
            LOG.error("Error stopping server", e);
            updateStatus(ServerStatus.FAILED);
            return false;
        }
    }
    
    /**
     * 停止服务器
     * @return 是否成功停止的CompletableFuture
     */
    @Override
    public CompletableFuture<Boolean> stopServer() {
        LOG.info("Stopping OmniSharp server");
        return CompletableFuture.supplyAsync(() -> stop());
    }
    
    /**
     * 内部重启实现
     * @return 是否成功重启
     */
    public boolean restart() {    
        LOG.info("Restarting OmniSharp server");
        
        // 先停止
        stop();
        
        // 等待进程完全停止
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 再启动
        return start();
    }
    
    /**
     * 重启服务器
     * @return 是否成功重启的CompletableFuture
     */
    @Override
    public CompletableFuture<Boolean> restartServer() {    
        LOG.info("Restarting OmniSharp server via restartServer method");
        return CompletableFuture.supplyAsync(() -> restart());
    }
    
    public ServerStatus getStatus() {
        return status.get();
    }
    
    public boolean isRunning() {
        return status.get() == ServerStatus.RUNNING;
    }
    
    /**
     * 检查服务器是否正在运行
     * @return 是否运行中
     */
    @Override
    public boolean isServerRunning() {
        return isRunning();
    }
    
    /**
     * 获取当前服务器状态
     * @return 服务器状态
     */
    @Override
    public ServerStatus getServerStatus() {
        return status.get();
    }
    
    @Override
    public <T> CompletableFuture<OmniSharpResponse<T>> sendRequest(OmniSharpRequest<T> request) {
        if (!checkDisposed()) {
            return CompletableFuture.completedFuture(null);
        }
        
        if (!isRunning()) {
            CompletableFuture<OmniSharpResponse<T>> failedFuture = new CompletableFuture<>();
            failedFuture.completeExceptionally(new OmniSharpException("Server is not running. Current state: " + status.get()));
            return failedFuture;
        }
        
        return communication.sendRequest(request);
    }
    
    @Override
    public <T> Mono<OmniSharpResponse<T>> sendRequestReactive(OmniSharpRequest<T> request) {
        if (!checkDisposed()) {
            return Mono.empty();
        }
        
        if (!isRunning()) {
            return Mono.error(new OmniSharpException("Server is not running. Current state: " + status.get()));
        }
        
        return communication.sendRequestReactive(request);
    }
    
    @Override
    public <T> String subscribeToEvent(String eventName, Class<T> eventType, Consumer<OmniSharpEvent<T>> listener) {
        if (!checkDisposed()) {
            return null;
        }
        
        return communication.subscribeToEvent(eventName, eventType, listener);
    }
    
    @Override
    public void unsubscribeFromEvent(String subscriptionId) {
        if (!checkDisposed()) {
            return;
        }
        
        communication.unsubscribeFromEvent(subscriptionId);
    }
    
    public void addStatusChangeListener(Runnable listener) {
        if (!checkDisposed()) {
            return;
        }
        
        statusChangeListeners.add(listener);
    }
    
    public void removeStatusChangeListener(Runnable listener) {
        statusChangeListeners.remove(listener);
    }
    
    @Override
    public Flux<ServerStatus> statusChanges() {
        return Flux.create(sink -> {
            // 添加状态变化监听器
            Runnable listener = () -> {
                if (sink.isCancelled()) {
                    return;
                }
                sink.next(getServerStatus());
            };
            
            addStatusChangeListener(listener);
            
            // 初始状态
            sink.next(getServerStatus());
            
            // 当订阅取消时清理监听器
            sink.onCancel(() -> removeStatusChangeListener(listener));
        });
    }
    
    public void waitForServerReady(long timeoutMs) throws TimeoutException, InterruptedException {
        if (!checkDisposed()) {
            throw new TimeoutException("Server manager is disposed");
        }
        
        if (!startupLatch.await(timeoutMs, TimeUnit.MILLISECONDS)) {
            throw new TimeoutException("Timed out waiting for server to be ready");
        }
    }
    
    @Override
    public void dispose() {
        if (isDisposed) {
            return;
        }
        
        isDisposed = true;
        
        try {
            // 停止服务器
            stop();
            
            // 关闭通信
            communication.shutdown();
            
            // 关闭调度器
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
            
            // 清理监听器
            statusChangeListeners.clear();
            
            LOG.info("OmniSharp server manager disposed");
        } catch (Exception e) {
            LOG.error("Error during disposal", e);
        }
    }
    
    @Override
    public void onProcessStarted() {
        LOG.info("Process started, initializing communication");
        // 进程已启动，但服务器可能还未完全就绪
        // 需要等待确认服务器已准备好接收请求
    }
    
    @Override
    public void onProcessOutput(String output) {
        // 检查输出中是否包含服务器就绪的标志
        if (output.contains("ready to accept requests") || output.contains("server started")) {
            LOG.info("Server is ready to accept requests");
            cancelStartupTimeout();
            updateStatus(ServerStatus.RUNNING);
            startupLatch.countDown();
        }
    }
    
    @Override
    public void onProcessError(String error) {
        LOG.warn("Process error output: " + error);
        
        // 检查错误输出是否表明启动失败
        if (status.get() == ServerStatus.STARTING && 
            (error.contains("failed to start") || error.contains("exception") || error.contains("error"))) {
            handleServerStartupFailed(new OmniSharpServerStartupException("Server failed to start: " + error));
        }
    }
    
    @Override
    public void onProcessTerminated(int exitCode) {
        LOG.info("Process terminated with exit code: " + exitCode);
        
        // 如果是意外终止，标记为失败
        if (status.get() != ServerStatus.STOPPING && status.get() != ServerStatus.STOPPED) {
            updateStatus(ServerStatus.FAILED);
        }
        
        startupLatch.countDown(); // 确保等待启动的线程不会永久阻塞
    }
    
    @Override
    public void onProcessStartFailed(Throwable throwable) {
        handleServerStartupFailed(throwable);
    }
    
    /**
     * 发送退出命令给OmniSharp服务器
     */
    private void sendExitCommand() {
        try {
            OmniSharpRequest<Void> exitRequest = new OmniSharpRequest<>("exit", null, null);
            communication.sendRequest(exitRequest);
        } catch (Exception e) {
            LOG.warn("Failed to send exit command", e);
        }
    }
    
    /**
     * 处理服务器启动失败
     */
    private void handleServerStartupFailed(Throwable throwable) {
        LOG.error("Server startup failed", throwable);
        cancelStartupTimeout();
        updateStatus(ServerStatus.FAILED);
        startupLatch.countDown(); // 确保等待启动的线程不会永久阻塞
    }
    
    /**
     * 取消启动超时任务
     */
    private void cancelStartupTimeout() {
        if (startupTimeoutFuture != null) {
            startupTimeoutFuture.cancel(false);
            startupTimeoutFuture = null;
        }
    }
    
    /**
     * 更新服务器状态并通知监听器
     */
    private void updateStatus(ServerStatus newStatus) {
        if (status.compareAndSet(status.get(), newStatus)) {
            LOG.info("Server status changed to: " + newStatus);
            
            // 通知所有状态变更监听器
            for (Runnable listener : statusChangeListeners) {
                ApplicationManager.getApplication().invokeLater(listener);
            }
        }
    }
    
    /**
     * 检查是否已释放
     */
    private boolean checkDisposed() {
        if (isDisposed) {
            LOG.warn("Operation attempted on disposed server manager");
            return false;
        }
        return true;
    }
    
    /**
     * 获取服务器管理器实例（IntelliJ服务）
     */
    public static @NotNull IOmniSharpServerManager getInstance() {
        return ApplicationManager.getApplication().getService(IOmniSharpServerManager.class);
    }
}