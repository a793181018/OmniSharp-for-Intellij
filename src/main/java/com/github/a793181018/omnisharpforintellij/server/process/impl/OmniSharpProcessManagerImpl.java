package com.github.a793181018.omnisharpforintellij.server.process.impl;

import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager;
import com.github.a793181018.omnisharpforintellij.server.process.ProcessListener;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OmniSharp进程管理器实现类
 */
public class OmniSharpProcessManagerImpl implements IOmniSharpProcessManager {
    private static final Logger LOG = Logger.getInstance(OmniSharpProcessManagerImpl.class);
    
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final List<ProcessListener> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    
    private ProcessInputWriterImpl inputWriter;
    private ProcessOutputReaderImpl outputReader;
    
    @Override
    public CompletableFuture<Boolean> startProcess(String serverPath, File workingDirectory, List<String> arguments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LOG.info("Starting OmniSharp process with path: " + serverPath + ", working directory: " + workingDirectory);
                
                // 构建进程启动命令
                List<String> command = new ArrayList<>();
                command.add(serverPath);
                command.addAll(arguments);
                
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                processBuilder.directory(workingDirectory);
                processBuilder.redirectErrorStream(false);
                
                // 启动进程
                Process newProcess = processBuilder.start();
                
                // 设置进程
                if (!process.compareAndSet(null, newProcess)) {
                    newProcess.destroy();
                    LOG.warn("Process already running, cannot start new process");
                    return false;
                }
                
                // 创建输入输出处理
                inputWriter = new ProcessInputWriterImpl(newProcess.getOutputStream());
                outputReader = new ProcessOutputReaderImpl(newProcess.getInputStream(), newProcess.getErrorStream());
                
                // 开始读取输出
                outputReader.startReading();
                
                isRunning.set(true);
                
                // 启动进程退出监控
                monitorProcessExit(newProcess);
                
                // 通知监听器
                notifyProcessStarted();
                
                LOG.info("OmniSharp process started successfully with PID: " + newProcess.pid());
                return true;
                
            } catch (IOException e) {
                LOG.error("Failed to start OmniSharp process", e);
                notifyProcessStartFailed(e);
                return false;
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Boolean> stopProcess() {
        return CompletableFuture.supplyAsync(() -> {
            Process currentProcess = process.getAndSet(null);
            if (currentProcess == null) {
                LOG.warn("No process to stop");
                return true;
            }
            
            try {
                LOG.info("Stopping OmniSharp process with PID: " + currentProcess.pid());
                
                // 停止输出读取
                if (outputReader != null) {
                    outputReader.stopReading();
                    outputReader = null;
                }
                
                // 关闭输入写入器
                if (inputWriter != null) {
                    inputWriter.close();
                    inputWriter = null;
                }
                
                // 尝试优雅关闭
                currentProcess.destroy();
                
                // 等待进程退出，但设置超时
                try {
                    boolean exited = currentProcess.waitFor(5, TimeUnit.SECONDS);
                    if (!exited) {
                        LOG.warn("Process did not exit gracefully, forcing termination");
                        currentProcess.destroyForcibly();
                        currentProcess.waitFor(2, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    currentProcess.destroyForcibly();
                }
                
                isRunning.set(false);
                LOG.info("OmniSharp process stopped");
                return true;
                
            } catch (Exception e) {
                LOG.error("Error stopping OmniSharp process", e);
                return false;
            }
        }, executorService);
    }
    
    @Override
    public CompletableFuture<Boolean> restartProcess() {
        return stopProcess().thenCompose(success -> {
            if (!success) {
                LOG.warn("Failed to stop process, cannot restart");
                return CompletableFuture.completedFuture(false);
            }
            
            // 重新启动需要外部提供配置参数，这里返回false
            // 实际重启应该在ServerManager中处理
            LOG.warn("Process restart requested but requires configuration parameters");
            return CompletableFuture.completedFuture(false);
        });
    }
    
    @Override
    public boolean isProcessRunning() {
        return isRunning.get();
    }
    
    @Override
    public Process getProcess() {
        return process.get();
    }
    
    @Override
    public ProcessInputWriter getProcessInputWriter() {
        return inputWriter;
    }
    
    @Override
    public ProcessOutputReader getProcessOutputReader() {
        return outputReader;
    }
    
    @Override
    public void addProcessListener(ProcessListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeProcessListener(ProcessListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    /**
     * 监控进程退出
     */
    private void monitorProcessExit(Process process) {
        executorService.submit(() -> {
            try {
                int exitCode = process.waitFor();
                LOG.info("OmniSharp process exited with code: " + exitCode);
                isRunning.set(false);
                notifyProcessTerminated(exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Process exit monitoring interrupted");
            }
        });
    }
    
    /**
     * 通知进程启动
     */
    private void notifyProcessStarted() {
        for (ProcessListener listener : listeners) {
            try {
                listener.onProcessStarted();
            } catch (Exception e) {
                LOG.error("Error notifying process started", e);
            }
        }
    }
    
    /**
     * 通知进程启动失败
     */
    private void notifyProcessStartFailed(Throwable throwable) {
        for (ProcessListener listener : listeners) {
            try {
                listener.onProcessStartFailed(throwable);
            } catch (Exception e) {
                LOG.error("Error notifying process start failed", e);
            }
        }
    }
    
    /**
     * 通知进程终止
     */
    private void notifyProcessTerminated(int exitCode) {
        for (ProcessListener listener : listeners) {
            try {
                listener.onProcessTerminated(exitCode);
            } catch (Exception e) {
                LOG.error("Error notifying process terminated", e);
            }
        }
    }
    
    /**
     * 通知进程输出
     */
    private void notifyProcessOutput(String output) {
        for (ProcessListener listener : listeners) {
            try {
                listener.onProcessOutput(output);
            } catch (Exception e) {
                LOG.error("Error notifying process output", e);
            }
        }
    }
    
    /**
     * 通知进程错误
     */
    private void notifyProcessError(String error) {
        for (ProcessListener listener : listeners) {
            try {
                listener.onProcessError(error);
            } catch (Exception e) {
                LOG.error("Error notifying process error", e);
            }
        }
    }
    
    /**
     * 进程输入写入器实现
     */
    private class ProcessInputWriterImpl implements ProcessInputWriter {
        private final BufferedWriter writer;
        private final AtomicBoolean closed = new AtomicBoolean(false);
        
        public ProcessInputWriterImpl(OutputStream outputStream) {
            this.writer = new BufferedWriter(new OutputStreamWriter(outputStream));
        }
        
        @Override
        public void write(String data) {
            if (closed.get()) {
                throw new IllegalStateException("Input writer is closed");
            }
            
            try {
                writer.write(data);
                writer.flush();
            } catch (IOException e) {
                LOG.error("Error writing to process input", e);
                throw new RuntimeException("Failed to write to process input", e);
            }
        }
        
        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                try {
                    writer.close();
                } catch (IOException e) {
                    LOG.error("Error closing input writer", e);
                }
            }
        }
    }
    
    /**
     * 进程输出读取器实现
     */
    private class ProcessOutputReaderImpl implements ProcessOutputReader {
        private final BufferedReader outputReader;
        private final BufferedReader errorReader;
        private final AtomicBoolean running = new AtomicBoolean(false);
        private Future<?> outputReadingTask;
        private Future<?> errorReadingTask;
        
        public ProcessOutputReaderImpl(InputStream outputStream, InputStream errorStream) {
            this.outputReader = new BufferedReader(new InputStreamReader(outputStream));
            this.errorReader = new BufferedReader(new InputStreamReader(errorStream));
        }
        
        @Override
        public void startReading() {
            if (running.compareAndSet(false, true)) {
                outputReadingTask = executorService.submit(this::readOutput);
                errorReadingTask = executorService.submit(this::readError);
            }
        }
        
        @Override
        public void stopReading() {
            if (running.compareAndSet(true, false)) {
                if (outputReadingTask != null) {
                    outputReadingTask.cancel(true);
                }
                if (errorReadingTask != null) {
                    errorReadingTask.cancel(true);
                }
                
                try {
                    outputReader.close();
                } catch (IOException e) {
                    LOG.error("Error closing output reader", e);
                }
                
                try {
                    errorReader.close();
                } catch (IOException e) {
                    LOG.error("Error closing error reader", e);
                }
            }
        }
        
        /**
         * 读取标准输出
         */
        private void readOutput() {
            try {
                String line;
                while (running.get() && (line = outputReader.readLine()) != null) {
                    LOG.debug("OmniSharp output: " + line);
                    notifyProcessOutput(line);
                }
            } catch (IOException e) {
                if (running.get()) {
                    LOG.error("Error reading process output", e);
                }
            }
        }
        
        /**
         * 读取错误输出
         */
        private void readError() {
            try {
                String line;
                while (running.get() && (line = errorReader.readLine()) != null) {
                    LOG.debug("OmniSharp error: " + line);
                    notifyProcessError(line);
                }
            } catch (IOException e) {
                if (running.get()) {
                    LOG.error("Error reading process error", e);
                }
            }
        }
    }
    
    /**
     * 关闭资源
     */
    public void dispose() {
        stopProcess();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}