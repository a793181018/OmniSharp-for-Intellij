package com.github.a793181018.omnisharpforintellij.server.process;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OmniSharp进程管理器接口，负责启动、停止和监控OmniSharp服务器进程
 */
public interface IOmniSharpProcessManager {
    /**
     * 启动OmniSharp服务器进程
     * @param serverPath 服务器可执行文件路径
     * @param workingDirectory 工作目录
     * @param arguments 启动参数
     * @return 启动结果，true表示成功
     */
    CompletableFuture<Boolean> startProcess(String serverPath, File workingDirectory, List<String> arguments);
    
    /**
     * 停止OmniSharp服务器进程
     * @return 停止结果，true表示成功
     */
    CompletableFuture<Boolean> stopProcess();
    
    /**
     * 重启OmniSharp服务器进程
     * @return 重启结果，true表示成功
     */
    CompletableFuture<Boolean> restartProcess();
    
    /**
     * 检查进程是否正在运行
     * @return 是否运行中
     */
    boolean isProcessRunning();
    
    /**
     * 获取进程的输入流写入器
     * @return 进程输入流写入器
     */
    ProcessInputWriter getProcessInputWriter();
    
    /**
     * 获取进程的输出流读取器
     * @return 进程输出流读取器
     */
    ProcessOutputReader getProcessOutputReader();
    
    /**
     * 添加进程监听器
     * @param listener 进程监听器
     */
    void addProcessListener(ProcessListener listener);
    
    /**
     * 移除进程监听器
     * @param listener 进程监听器
     */
    void removeProcessListener(ProcessListener listener);
    
    /**
     * 进程输入写入器接口
     */
    interface ProcessInputWriter {
        /**
         * 写入数据到进程标准输入
         * @param data 要写入的数据
         */
        void write(String data);
        
        /**
         * 关闭写入器
         */
        void close();
    }
    
    /**
     * 进程输出读取器接口
     */
    interface ProcessOutputReader {
        /**
         * 开始异步读取进程输出
         */
        void startReading();
        
        /**
         * 停止读取
         */
        void stopReading();
    }
}