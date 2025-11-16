package com.github.a793181018.omnisharpforintellij.server.communication.impl;

import com.github.a793181018.omnisharpforintellij.server.communication.IEventDispatcher;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 事件分发器实现，用于分发OmniSharp事件给相应的监听器
 */
public class EventDispatcher implements IEventDispatcher {
    private static final Logger LOGGER = Logger.getLogger(EventDispatcher.class.getName());
    private static final int DEFAULT_THREAD_POOL_SIZE = 4;
    
    private final Map<String, List<OmniSharpEventListener>> eventListeners = new ConcurrentHashMap<>();
    private final List<OmniSharpEventListener> globalListeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executorService;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    
    /**
     * 创建默认配置的事件分发器
     */
    public EventDispatcher() {
        this(DEFAULT_THREAD_POOL_SIZE);
    }
    
    /**
     * 创建指定线程池大小的事件分发器
     * @param threadPoolSize 线程池大小
     */
    public EventDispatcher(int threadPoolSize) {
        int size = Math.max(1, threadPoolSize);
        this.executorService = new ThreadPoolExecutor(
                size,
                size,
                0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000),
                r -> {
                    Thread thread = new Thread(r, "OmniSharp-Event-Dispatcher");
                    thread.setDaemon(true);
                    return thread;
                },
                new ThreadPoolExecutor.CallerRunsPolicy() // 当队列满时，调用者线程执行任务
        );
    }
    
    @Override
    public void registerListener(String eventType, OmniSharpEventListener listener) {
        if (shutdown.get()) {
            throw new IllegalStateException("EventDispatcher is shutdown");
        }
        
        if (eventType == null || listener == null) {
            throw new IllegalArgumentException("Event type and listener cannot be null");
        }
        
        List<OmniSharpEventListener> listeners = eventListeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>());
          if (!listeners.contains(listener)) {
              listeners.add(listener);
          }
        LOGGER.fine("Registered listener for event type: " + eventType);
    }
    
    @Override
    public void registerGlobalListener(OmniSharpEventListener listener) {
        if (shutdown.get()) {
            throw new IllegalStateException("EventDispatcher is shutdown");
        }
        
        if (listener == null) {
            throw new IllegalArgumentException("Listener cannot be null");
        }
        
        if (!globalListeners.contains(listener)) {
              globalListeners.add(listener);
          }
        LOGGER.fine("Registered global event listener");
    }
    
    @Override
    public void unregisterListener(String eventType, OmniSharpEventListener listener) {
        if (eventType == null || listener == null) {
            return;
        }
        
        List<OmniSharpEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            boolean removed = listeners.remove(listener);
            if (removed) {
                LOGGER.fine("Unregistered listener for event type: " + eventType);
                // 如果没有监听器了，移除事件类型
                if (listeners.isEmpty()) {
                    eventListeners.remove(eventType);
                }
            }
        }
    }
    
    @Override
    public void unregisterGlobalListener(OmniSharpEventListener listener) {
        if (listener == null) {
            return;
        }
        
        boolean removed = globalListeners.remove(listener);
        if (removed) {
            LOGGER.fine("Unregistered global event listener");
        }
    }
    
    @Override
    public void dispatchEvent(OmniSharpEvent<?> event) {
        if (shutdown.get() || event == null) {
            return;
        }
        
        String eventType = event.getEvent();
        if (eventType == null) {
            LOGGER.warning("Event type is null");
            return;
        }
        
        LOGGER.fine("Dispatching event: " + eventType);
        
        // 异步分发事件
        executorService.execute(() -> {
            try {
                // 分发到特定事件类型的监听器
                List<OmniSharpEventListener> listeners = eventListeners.get(eventType);
                if (listeners != null) {
                    for (OmniSharpEventListener listener : listeners) {
                        try {
                            listener.onEvent(event);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Error in event listener for type: " + eventType, e);
                        }
                    }
                }
                
                // 分发到全局监听器
                for (OmniSharpEventListener listener : globalListeners) {
                    try {
                        listener.onEvent(event);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Error in global event listener", e);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during event dispatch", e);
            }
        });
    }
    
    @Override
    public int getListenerCount(String eventType) {
        if (eventType == null) {
            return 0;
        }
        List<OmniSharpEventListener> listeners = eventListeners.get(eventType);
        return listeners != null ? listeners.size() : 0;
    }
    
    @Override
    public int getGlobalListenerCount() {
        return globalListeners.size();
    }
    
    @Override
    public void clearAllListeners() {
        eventListeners.clear();
        globalListeners.clear();
        LOGGER.info("Cleared all event listeners");
    }
    
    /**
     * 关闭事件分发器
     */
    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            clearAllListeners();
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                executorService.shutdownNow();
            }
            LOGGER.info("EventDispatcher shutdown");
        }
    }
    
    /**
     * 检查是否已关闭
     * @return 是否已关闭
     */
    public boolean isShutdown() {
        return shutdown.get();
    }
    
    /**
     * 获取当前注册的事件类型数量
     * @return 事件类型数量
     */
    public int getEventTypeCount() {
        return eventListeners.size();
    }
    
    /**
     * 同步分发事件（阻塞当前线程）
     * @param event 事件对象
     */
    public void dispatchEventSync(OmniSharpEvent<?> event) {
        if (shutdown.get() || event == null) {
            return;
        }
        
        String eventType = event.getEvent();
        if (eventType == null) {
            LOGGER.warning("Event type is null");
            return;
        }
        
        LOGGER.fine("Sync dispatching event: " + eventType);
        
        // 分发到特定事件类型的监听器
        List<OmniSharpEventListener> listeners = eventListeners.get(eventType);
        if (listeners != null) {
            for (OmniSharpEventListener listener : listeners) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error in event listener for type: " + eventType, e);
                }
            }
        }
        
        // 分发到全局监听器
        for (OmniSharpEventListener listener : globalListeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in global event listener", e);
            }
        }
    }
}