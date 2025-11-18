package com.github.a793181018.omnisharpforintellij.communicator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 基于Stdio通信协议的OmniSharp服务器客户端
 */
public class StdioOmniSharpClient implements OmniSharpServerClient {
    private final String serverPath;
    private final String projectPath;
    private Process serverProcess;
    private BufferedWriter writer;
    private BufferedReader reader;
    private int connectTimeout = 10000; // 默认10秒
    private int readTimeout = 30000; // 默认30秒
    
    public StdioOmniSharpClient(@NotNull String serverPath, @NotNull String projectPath) {
        this.serverPath = serverPath;
        this.projectPath = projectPath;
    }
    
    @Override
    public void connect() throws IOException {
        if (isConnected()) {
            disconnect();
        }
        
        try {
            // 启动OmniSharp服务器进程
            ProcessBuilder processBuilder = new ProcessBuilder(serverPath);
            processBuilder.directory(new File(projectPath));
            processBuilder.redirectErrorStream(true); // 合并标准错误到标准输出
            
            // 设置环境变量
            processBuilder.environment().put("OMNISHARP_PROTOCOL", "stdio");
            
            // 启动进程
            serverProcess = processBuilder.start();
            
            // 获取输入输出流
            writer = new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8));
            reader = new BufferedReader(new InputStreamReader(serverProcess.getInputStream(), StandardCharsets.UTF_8));
            
            // 等待服务器启动就绪（读取初始输出）
            waitForServerReady();
            
        } catch (IOException | InterruptedException | TimeoutException e) {
            disconnect();
            throw new IOException("Failed to connect to OmniSharp server: " + e.getMessage(), e);
        }
    }
    
    @Override
    public void disconnect() {
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
            writer = null;
        }
        
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {}
            reader = null;
        }
        
        if (serverProcess != null) {
            serverProcess.destroyForcibly();
            try {
                serverProcess.waitFor(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            serverProcess = null;
        }
    }
    
    @Override
    public void send(@NotNull String message) throws IOException {
        if (!isConnected() || writer == null) {
            throw new IOException("Not connected to server");
        }
        
        writer.write(message);
        writer.flush();
    }
    
    @Override
    @Nullable
    public String receive() throws IOException {
        if (!isConnected() || reader == null) {
            throw new IOException("Not connected to server");
        }
        
        // 读取Content-Length头
        StringBuilder responseBuilder = new StringBuilder();
        String line;
        int contentLength = -1;
        
        // 跳过空行，读取Content-Length头
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                continue; // 跳过空行
            }
            if (line.startsWith("Content-Length: ")) {
                try {
                    contentLength = Integer.parseInt(line.substring("Content-Length: ".length()));
                } catch (NumberFormatException e) {
                    throw new IOException("Invalid Content-Length format: " + line);
                }
                break;
            }
        }
        
        // 如果没有找到Content-Length头，可能是流式输出
        if (contentLength == -1) {
            return line; // 返回最后读取的行
        }
        
        // 读取响应体
        char[] buffer = new char[contentLength];
        int totalRead = 0;
        
        while (totalRead < contentLength) {
            int read = reader.read(buffer, totalRead, contentLength - totalRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += read;
        }
        
        return new String(buffer, 0, totalRead);
    }
    
    @Override
    public boolean isConnected() {
        return serverProcess != null && writer != null && reader != null && serverProcess.isAlive();
    }
    
    @Override
    public boolean isReconnectable() {
        return true; // Stdio客户端可以重新连接
    }
    
    @Override
    public void setConnectTimeout(int timeoutMs) {
        this.connectTimeout = timeoutMs;
    }
    
    @Override
    public void setReadTimeout(int timeoutMs) {
        this.readTimeout = timeoutMs;
    }
    
    /**
     * 等待服务器准备就绪
     */
    private void waitForServerReady() throws InterruptedException, IOException, TimeoutException {
        long startTime = System.currentTimeMillis();
        StringBuilder initialOutput = new StringBuilder();
        
        // 设置超时
        long timeoutTime = startTime + connectTimeout;
        
        // 读取初始输出，直到找到服务器就绪的消息
        while (System.currentTimeMillis() < timeoutTime) {
            if (reader.ready()) {
                String line = reader.readLine();
                if (line == null) {
                    throw new IOException("Server closed connection");
                }
                
                initialOutput.append(line).append("\n");
                
                // 检查是否包含服务器就绪的标识
                if (line.contains("Started")) {
                    return; // 服务器已就绪
                }
                
                // 检查是否有错误信息
                if (line.contains("error") || line.contains("exception")) {
                    throw new IOException("Server startup error: " + initialOutput.toString());
                }
            } else {
                Thread.sleep(100); // 短暂休眠避免CPU占用过高
            }
        }
        
        throw new TimeoutException("Server startup timeout. Initial output: " + initialOutput.toString());
    }
}