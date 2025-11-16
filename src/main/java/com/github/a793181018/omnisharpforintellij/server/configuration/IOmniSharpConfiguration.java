package com.github.a793181018.omnisharpforintellij.server.configuration;

import java.io.File;
import java.util.List;

/**
 * OmniSharp服务器配置接口，负责管理OmniSharp服务器的配置选项
 */
public interface IOmniSharpConfiguration {
    /**
     * 获取OmniSharp服务器可执行文件路径
     * @return 服务器路径
     */
    String getServerPath();
    
    /**
     * 设置OmniSharp服务器可执行文件路径
     * @param serverPath 服务器路径
     */
    void setServerPath(String serverPath);
    
    /**
     * 获取工作目录
     * @return 工作目录
     */
    File getWorkingDirectory();
    
    /**
     * 设置工作目录
     * @param workingDirectory 工作目录
     */
    void setWorkingDirectory(File workingDirectory);
    
    /**
     * 获取启动参数
     * @return 启动参数列表
     */
    List<String> getArguments();
    
    /**
     * 设置启动参数
     * @param arguments 启动参数列表
     */
    void setArguments(List<String> arguments);
    
    /**
     * 获取服务器启动最大等待时间（毫秒）
     * @return 最大等待时间
     */
    long getMaxStartupWaitTime();
    
    /**
     * 设置服务器启动最大等待时间（毫秒）
     * @param maxStartupWaitTime 最大等待时间
     */
    void setMaxStartupWaitTime(long maxStartupWaitTime);
    
    /**
     * 获取是否自动重启
     * @return 是否自动重启
     */
    boolean isAutoRestart();
    
    /**
     * 设置是否自动重启
     * @param autoRestart 是否自动重启
     */
    void setAutoRestart(boolean autoRestart);
    
    /**
     * 获取最大重启次数
     * @return 最大重启次数
     */
    int getMaxRestartAttempts();
    
    /**
     * 设置最大重启次数
     * @param maxRestartAttempts 最大重启次数
     */
    void setMaxRestartAttempts(int maxRestartAttempts);
    
    /**
     * 获取通信超时时间（毫秒）
     * @return 通信超时时间
     */
    long getCommunicationTimeout();
    
    /**
     * 设置通信超时时间（毫秒）
     * @param communicationTimeout 通信超时时间
     */
    void setCommunicationTimeout(long communicationTimeout);
    
    /**
     * 验证配置是否有效
     * @return 验证结果
     */
    ValidationResult validate();
    
    /**
     * 配置验证结果类
     */
    class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        
        /**
         * 创建验证结果
         * @param isValid 是否有效
         * @param errorMessage 错误消息
         */
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        /**
         * 创建成功的验证结果
         * @return 成功的验证结果
         */
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        /**
         * 创建失败的验证结果
         * @param errorMessage 错误消息
         * @return 失败的验证结果
         */
        public static ValidationResult failure(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}