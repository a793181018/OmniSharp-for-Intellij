package com.github.a793181018.omnisharpforintellij.session;

import org.jetbrains.annotations.NotNull;

/**
 * OmniSharp会话监听器，用于监听会话状态的变化
 */
public interface OmniSharpSessionListener {
    /**
     * 当会话状态发生变化时调用
     * @param session 会话实例
     * @param oldStatus 旧状态
     * @param newStatus 新状态
     */
    void onStatusChanged(@NotNull OmniSharpSession session, @NotNull SessionStatus oldStatus, @NotNull SessionStatus newStatus);
    
    /**
     * 当会话发生错误时调用
     * @param session 会话实例
     * @param error 错误信息
     */
    default void onError(@NotNull OmniSharpSession session, @NotNull Throwable error) {
        // 默认实现，子类可以覆盖
    }
}