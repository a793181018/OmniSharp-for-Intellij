package com.github.a793181018.omnisharpforintellij.server.communication;

import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;

/**
 * 事件分发器接口，用于分发OmniSharp事件给相应的监听器
 */
public interface IEventDispatcher {
    /**
     * 注册事件监听器
     * @param eventType 事件类型
     * @param listener 事件监听器
     */
    void registerListener(String eventType, OmniSharpEventListener listener);
    
    /**
     * 注册全局事件监听器，接收所有事件
     * @param listener 事件监听器
     */
    void registerGlobalListener(OmniSharpEventListener listener);
    
    /**
     * 取消注册事件监听器
     * @param eventType 事件类型
     * @param listener 事件监听器
     */
    void unregisterListener(String eventType, OmniSharpEventListener listener);
    
    /**
     * 取消注册全局事件监听器
     * @param listener 事件监听器
     */
    void unregisterGlobalListener(OmniSharpEventListener listener);
    
    /**
     * 分发事件给注册的监听器
     * @param event 事件对象
     */
    void dispatchEvent(OmniSharpEvent<?> event);
    
    /**
     * 获取特定事件类型的监听器数量
     * @param eventType 事件类型
     * @return 监听器数量
     */
    int getListenerCount(String eventType);
    
    /**
     * 获取全局监听器数量
     * @return 全局监听器数量
     */
    int getGlobalListenerCount();
    
    /**
     * 清空所有监听器
     */
    void clearAllListeners();
    
    /**
     * 事件监听器接口
     */
    interface OmniSharpEventListener {
        /**
         * 当事件发生时调用
         * @param event 事件对象
         */
        void onEvent(OmniSharpEvent<?> event);
    }
}