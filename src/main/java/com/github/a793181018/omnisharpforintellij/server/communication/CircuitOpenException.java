package com.github.a793181018.omnisharpforintellij.server.communication;

/**
 * 当断路器处于打开状态时抛出的异常
 */
public class CircuitOpenException extends RuntimeException {
    public CircuitOpenException(String message) {
        super(message);
    }
}