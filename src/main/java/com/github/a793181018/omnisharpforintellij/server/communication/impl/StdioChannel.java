package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.github.a793181018.omnisharpforintellij.server.communication.IStdioChannel;
import com.github.a793181018.omnisharpforintellij.server.communication.IStdioMessageListener;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 标准输入输出通道实现，用于与OmniSharp服务器进行通信
 */
public class StdioChannel implements IStdioChannel {
    private static final Logger LOGGER = Logger.getLogger(StdioChannel.class.getName());
    private static final int DEFAULT_BUFFER_SIZE = 8192;
    private static final long DEFAULT_READ_TIMEOUT_MS = 30000; // 30秒默认超时
    
    private Process process;
    private OutputStream outputStream;
    private InputStream inputStream;
    private InputStream errorStream;
    private boolean initialized = false;
    private boolean closed = false;
    private final List<IStdioMessageListener> listeners = Collections.synchronizedList(new ArrayList<>());
    private final BlockingQueue<byte[]> messageQueue = new LinkedBlockingQueue<>();
    private Thread readThread;
    private Thread errorThread;
    
    @Override
    public synchronized void initialize(Process process) {
        if (initialized) {
            throw new IllegalStateException("Channel already initialized");
        }
        
        this.process = process;
        this.outputStream = process.getOutputStream();
        this.inputStream = process.getInputStream();
        this.errorStream = process.getErrorStream();
        
        // 启动读取线程
        startReadThreads();
        
        initialized = true;
        LOGGER.info("StdioChannel initialized successfully");
    }
    
    @Override
    public void write(byte[] message) throws IOException {
        checkInitialized();
        
        synchronized (outputStream) {
            outputStream.write(message);
            outputStream.flush();
            LOGGER.fine("Message written to OmniSharp server: " + new String(message, StandardCharsets.UTF_8));
        }
    }
    
    @Override
    public byte[] read(long timeoutMs) throws IOException, TimeoutException {
        checkInitialized();
        
        try {
            byte[] message = messageQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
            if (message == null) {
                throw new TimeoutException("Read operation timed out after " + timeoutMs + "ms");
            }
            return message;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Read operation interrupted", e);
        }
    }
    
    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        
        // 停止线程
        if (readThread != null) {
            readThread.interrupt();
        }
        if (errorThread != null) {
            errorThread.interrupt();
        }
        
        // 关闭流
        try {
            if (outputStream != null) {
                outputStream.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (errorStream != null) {
                errorStream.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error closing streams", e);
        }
        
        // 清空队列和监听器
        messageQueue.clear();
        listeners.clear();
        
        closed = true;
        initialized = false;
        LOGGER.info("StdioChannel closed");
    }
    
    @Override
    public void registerListener(IStdioMessageListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void unregisterListener(IStdioMessageListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Channel not initialized");
        }
        if (closed) {
            throw new IllegalStateException("Channel is closed");
        }
    }
    
    private void startReadThreads() {
        // 启动标准输出读取线程
        readThread = new Thread(this::readFromInputStream, "OmniSharp-Stdio-Reader");
        readThread.setDaemon(true);
        readThread.start();
        
        // 启动标准错误读取线程
        errorThread = new Thread(this::readFromErrorStream, "OmniSharp-Stdio-Error-Reader");
        errorThread.setDaemon(true);
        errorThread.start();
    }
    
    private void readFromInputStream() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder messageBuilder = new StringBuilder();
            String line;
            
            while (!Thread.currentThread().isInterrupted()) {
                line = reader.readLine();
                if (line == null) {
                    break; // 流已关闭
                }
                
                messageBuilder.append(line).append("\n");
                
                // 检查是否是完整的消息（根据OmniSharp协议，每条消息以Content-Length开头并包含空行）
                if (isCompleteMessage(messageBuilder.toString())) {
                    byte[] messageBytes = messageBuilder.toString().getBytes(StandardCharsets.UTF_8);
                    messageQueue.offer(messageBytes);
                    notifyMessageListeners(messageBytes);
                    messageBuilder.setLength(0);
                }
            }
        } catch (IOException e) {
            if (!closed) {
                LOGGER.log(Level.SEVERE, "Error reading from OmniSharp server", e);
                notifyErrorListeners(e);
            }
        }
        
        // 如果不是因为关闭而退出，关闭通道
        if (!closed) {
            close();
        }
    }
    
    private void readFromErrorStream() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream, StandardCharsets.UTF_8))) {
            String line;
            while (!Thread.currentThread().isInterrupted() && (line = reader.readLine()) != null) {
                LOGGER.warning("OmniSharp server error: " + line);
            }
        } catch (IOException e) {
            if (!closed) {
                LOGGER.log(Level.SEVERE, "Error reading from OmniSharp error stream", e);
            }
        }
    }
    
    private boolean isCompleteMessage(String message) {
        // 简单实现：检查是否包含完整的HTTP头格式（OmniSharp使用HTTP-like格式）
        // 查找Content-Length头和两个连续的换行符
        return message.contains("Content-Length:") && message.contains("\n\n");
    }
    
    private void notifyMessageListeners(byte[] message) {
        List<IStdioMessageListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (IStdioMessageListener listener : listenersCopy) {
            try {
                listener.onMessageReceived(message);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in message listener", e);
            }
        }
    }
    
    private void notifyErrorListeners(Throwable error) {
        List<IStdioMessageListener> listenersCopy;
        synchronized (listeners) {
            listenersCopy = new ArrayList<>(listeners);
        }
        
        for (IStdioMessageListener listener : listenersCopy) {
            try {
                listener.onError(error);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in error listener", e);
            }
        }
    }
    
    /**
     * 检查通道是否已关闭
     * @return 是否已关闭
     */
    public boolean isClosed() {
        return closed;
    }
    
    /**
     * 获取当前通道关联的进程
     * @return 进程对象，如果通道未初始化则返回null
     */
    @Nullable
    public Process getProcess() {
        return process;
    }
}