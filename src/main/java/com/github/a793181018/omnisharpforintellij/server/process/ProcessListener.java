package com.github.a793181018.omnisharpforintellij.server.process;

/**
 * 进程监听器接口，用于监听OmniSharp服务器进程的各种事件
 */
public interface ProcessListener {
    /**
     * 当进程启动成功时调用
     */
    void onProcessStarted();
    
    /**
     * 当进程输出数据时调用
     * @param output 进程输出的数据
     */
    void onProcessOutput(String output);
    
    /**
     * 当进程输出错误时调用
     * @param error 进程输出的错误信息
     */
    void onProcessError(String error);
    
    /**
     * 当进程终止时调用
     * @param exitCode 进程退出码
     */
    void onProcessTerminated(int exitCode);
    
    /**
     * 当进程启动失败时调用
     * @param throwable 失败原因
     */
    void onProcessStartFailed(Throwable throwable);
}