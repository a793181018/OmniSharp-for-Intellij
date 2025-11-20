package com.omnisharp.intellij.projectstructure.parser;

/**
 * 解决方案文件解析异常
 * 当解析解决方案文件过程中遇到错误时抛出
 */
public class ParseException extends Exception {
    
    /**
     * 使用指定的错误消息构造异常
     * @param message 详细的错误消息
     */
    public ParseException(String message) {
        super(message);
    }
    
    /**
     * 使用指定的错误消息和原因构造异常
     * @param message 详细的错误消息
     * @param cause 导致此异常的根本原因
     */
    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }
}