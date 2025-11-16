# OmniSharp通信协议实现 - 架构设计

## 目录

1. [概述](#概述)
2. [通信协议架构](#通信协议架构)
3. [核心组件设计](#核心组件设计)
4. [消息格式](#消息格式)
5. [数据流](#数据流)
6. [扩展性考虑](#扩展性考虑)
7. [性能优化](#性能优化)
8. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp-for-Intellij插件中通信协议实现的架构设计。OmniSharp服务器使用基于JSON的Stdio协议进行通信，本架构设计旨在实现高效、可靠的通信机制，支持请求-响应模式和事件通知机制。

### 设计目标

- 实现与OmniSharp服务器的高效通信
- 支持异步请求处理和响应
- 提供灵活的消息序列化和反序列化机制
- 实现健壮的错误处理和重试逻辑
- 确保通信的线程安全
- 支持事件驱动的消息处理

### 技术栈

- Kotlin - 主要开发语言
- JSON - 消息格式
- Stdio - 通信通道
- IntelliJ Platform SDK - 插件开发框架

## 通信协议架构

### 高层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           通信协议架构                                   │
├─────────┬─────────────────────────────────────┬─────────────────────────┤
│ 消息序列化层 │                              │  事件处理层              │
└─────────┴───────────┬─────────────────────────┴─────────┬───────────────┘
                      │                                   │
                      ▼                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           通信核心层                                     │
├────────────────────┬─────────────────────────────────────┬──────────────┤
│ 请求-响应管理器 │ Stdio通信管道        │ 消息路由器         │
└────────────────────┴─────────────────┬─────────────────┴──────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           错误处理层                                     │
├────────────────────┬─────────────────────────────────────┬──────────────┤
│ 错误处理器     │ 重试机制           │ 超时管理           │
└────────────────────┴─────────────────────────────────────┴──────────────┘
```

### 组件层次关系

通信协议实现采用分层架构，主要包括以下几层：

1. **消息序列化层**：负责消息的序列化和反序列化
2. **通信核心层**：处理请求发送、响应接收和消息路由
3. **事件处理层**：管理服务器发送的事件通知
4. **错误处理层**：处理通信过程中的错误和异常情况

### 主要数据流

- **请求发送流程**：请求构建 → 序列化 → 通过Stdio通道发送
- **响应接收流程**：从Stdio通道接收 → 反序列化 → 路由到对应的请求处理器
- **事件处理流程**：从Stdio通道接收 → 反序列化 → 分发到注册的事件监听器

## 核心组件设计

### 1. IOmniSharpCommunicator

通信协议的核心接口，负责与OmniSharp服务器的通信。

```kotlin
interface IOmniSharpCommunicator {
    val isConnected: Boolean
    
    /**
     * 连接到OmniSharp服务器进程
     */
    fun connect(process: Process): CompletableFuture<Boolean>
    
    /**
     * 断开连接
     */
    fun disconnect()
    
    /**
     * 发送原始消息
     */
    fun sendMessage(message: String): CompletableFuture<String>
    
    /**
     * 发送结构化请求
     */
    fun sendRequest(request: OmniSharpRequest): CompletableFuture<OmniSharpResponse>
    
    /**
     * 添加消息监听器
     */
    fun addMessageListener(listener: MessageListener)
    
    /**
     * 移除消息监听器
     */
    fun removeMessageListener(listener: MessageListener)
    
    /**
     * 添加事件监听器
     */
    fun addEventListener(eventType: String, listener: EventListener)
    
    /**
     * 移除事件监听器
     */
    fun removeEventListener(eventType: String, listener: EventListener)
}
```

### 2. IMessageSerializer

负责消息的序列化和反序列化。

```kotlin
interface IMessageSerializer {
    /**
     * 序列化请求对象为JSON字符串
     */
    fun serializeRequest(request: OmniSharpRequest): String
    
    /**
     * 反序列化JSON字符串为响应对象
     */
    fun deserializeResponse(responseString: String): OmniSharpResponse
    
    /**
     * 反序列化JSON字符串为事件对象
     */
    fun deserializeEvent(eventString: String): OmniSharpEvent
}
```

### 3. IStdioChannel

管理Stdio通信通道。

```kotlin
interface IStdioChannel {
    val isOpen: Boolean
    
    /**
     * 打开Stdio通道
     */
    fun open(process: Process): Boolean
    
    /**
     * 关闭Stdio通道
     */
    fun close()
    
    /**
     * 写入消息
     */
    fun write(message: String)
    
    /**
     * 读取消息
     */
    fun read(): String?
    
    /**
     * 添加读取监听器
     */
    fun addReadListener(listener: ReadListener)
    
    /**
     * 移除读取监听器
     */
    fun removeReadListener(listener: ReadListener)
}
```

### 4. IRequestResponseManager

管理请求-响应映射和超时处理。

```kotlin
interface IRequestResponseManager {
    /**
     * 添加请求，返回请求ID
     */
    fun addRequest(request: OmniSharpRequest): Int
    
    /**
     * 完成请求
     */
    fun completeRequest(requestId: Int, response: OmniSharpResponse)
    
    /**
     * 取消请求
     */
    fun cancelRequest(requestId: Int, exception: Exception? = null)
    
    /**
     * 检查请求是否存在
     */
    fun hasRequest(requestId: Int): Boolean
    
    /**
     * 设置请求超时时间
     */
    fun setRequestTimeout(requestId: Int, timeoutMs: Long)
}
```

### 5. IEventHandler

管理事件分发。

```kotlin
interface IEventHandler {
    /**
     * 注册事件监听器
     */
    fun registerListener(eventType: String, listener: EventListener)
    
    /**
     * 注销事件监听器
     */
    fun unregisterListener(eventType: String, listener: EventListener)
    
    /**
     * 触发事件
     */
    fun triggerEvent(event: OmniSharpEvent)
    
    /**
     * 清理所有监听器
     */
    fun clearAllListeners()
}
```

### 6. IErrorHandler

处理通信错误。

```kotlin
interface IErrorHandler {
    /**
     * 处理通信错误
     */
    fun handleError(error: Throwable, context: ErrorContext? = null): ErrorResolution
    
    /**
     * 记录错误
     */
    fun logError(error: Throwable, message: String? = null)
    
    /**
     * 是否应该重试
     */
    fun shouldRetry(error: Throwable): Boolean
    
    /**
     * 获取重试延迟时间
     */
    fun getRetryDelay(error: Throwable, attempt: Int): Long
}
```

## 消息格式

### 1. OmniSharpRequest

```kotlin
data class OmniSharpRequest(
    val seq: Int = 0,
    val type: String = "request",
    val command: String,
    val arguments: Map<String, Any> = emptyMap()
)
```

### 2. OmniSharpResponse

```kotlin
data class OmniSharpResponse(
    val seq: Int,
    val type: String,
    val command: String,
    val request_seq: Int,
    val body: Map<String, Any> = emptyMap(),
    val running: Boolean,
    val success: Boolean,
    val message: String? = null
)
```

### 3. OmniSharpEvent

```kotlin
data class OmniSharpEvent(
    val seq: Int,
    val type: String = "event",
    val event: String,
    val body: Map<String, Any> = emptyMap()
)
```

## 数据流

### 请求发送流程

```
sendRequest(request)
    │
    ▼
checkConnection()
    │
    ├─┐ not connected
    │ ▼
    │ throw ConnectionException()
    │
    ▼ connected
assignRequestId()
    │
    ▼
registerRequest()
    │
    ▼
serializeRequest()
    │
    ▼
writeToStdioChannel()
    │
    ▼
return CompletableFuture()
```

### 响应处理流程

```
readFromStdioChannel()
    │
    ▼
parseMessageType()
    │
    ├─┐ response
    │ ▼
    │ deserializeResponse()
    │    │
    │    ▼
    │ findRequest(requestId)
    │    │
    │    ├─┐ found
    │    │ ▼
    │    │ completeFuture()
    │    │
    │    ▼ not found
    │    logWarning()
    │
    ▼ event
    deserializeEvent()
       │
       ▼
    triggerEvent()
```

## 扩展性考虑

### 1. 支持多种序列化格式

通过IMessageSerializer接口，可以轻松扩展支持其他序列化格式，如Protocol Buffers或MessagePack。

### 2. 支持多种通信通道

除了Stdio，还可以扩展支持WebSocket或HTTP等其他通信通道。

### 3. 支持自定义消息处理器

通过消息路由器和事件处理器，可以注册自定义的消息处理器。

### 4. 支持拦截器模式

可以添加请求和响应拦截器，用于日志记录、性能监控等。

## 性能优化

1. **异步处理**：使用CompletableFuture进行异步操作
2. **线程池**：使用线程池管理读取和写入操作
3. **缓冲区管理**：优化缓冲区大小和消息解析效率
4. **连接池**：考虑添加连接池机制，提高连接复用率
5. **批处理**：支持批量请求处理

## 后续步骤

1. 实现JSON消息的序列化和反序列化机制
2. 设计和实现Stdio通信管道
3. 实现请求-响应模式和事件处理机制
4. 编写通信协议的错误处理和重试机制
5. 创建通信协议的单元测试和集成测试
6. 编写通信协议实现文档，包含代码示例和流程图

本架构设计提供了OmniSharp通信协议实现的基础框架，后续将根据这个设计进行具体实现。