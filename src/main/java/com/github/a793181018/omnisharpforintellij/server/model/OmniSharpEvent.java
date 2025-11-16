package com.github.a793181018.omnisharpforintellij.server.model;

import com.google.gson.annotations.SerializedName;
import java.util.concurrent.atomic.AtomicLong;

/**
 * OmniSharp服务器事件模型
 * @param <T> 事件数据类型
 */
public class OmniSharpEvent<T> {
    private static final AtomicLong EVENT_SEQ_COUNTER = new AtomicLong(1);
    
    @SerializedName("event")
    private final String event;
    
    @SerializedName("seq")
    private long seq;
    
    @SerializedName("type")
    private final String type = "event";
    
    @SerializedName("body")
    private final T body;
    
    /**
     * 创建OmniSharp事件
     * @param event 事件名称
     * @param body 事件数据
     */
    public OmniSharpEvent(String event, T body) {
        this.event = event;
        this.body = body;
        this.seq = EVENT_SEQ_COUNTER.getAndIncrement();
    }
    
    public String getEvent() {
        return event;
    }
    
    public long getSeq() {
        return seq;
    }
    
    public void setSeq(long seq) {
        this.seq = seq;
    }
    
    public String getType() {
        return type;
    }
    
    public T getBody() {
        return body;
    }
}