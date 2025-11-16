# OmniSharp通信协议 - 请求-响应模式和事件处理机制实现

## 目录

1. [概述](#概述)
2. [请求-响应模式设计](#请求-响应模式设计)
3. [事件处理机制设计](#事件处理机制设计)
4. [核心组件实现](#核心组件实现)
5. [请求跟踪和响应匹配](#请求跟踪和响应匹配)
6. [超时处理](#超时处理)
7. [错误处理](#错误处理)
8. [性能优化](#性能优化)
9. [使用示例](#使用示例)
10. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp通信协议中请求-响应模式和事件处理机制的实现方案。这两个机制是与OmniSharp服务器进行有效通信的关键组件，负责管理请求的发送、响应的接收以及事件的处理。

### 实现目标

- 提供统一的请求-响应接口
- 实现请求ID的自动生成和管理
- 支持请求-响应的自动匹配
- 提供异步的请求处理API
- 实现灵活的事件处理机制
- 支持请求超时和取消
- 确保线程安全

### 技术选型

我们将使用Java的CompletableFuture、并发集合和同步原语来实现高效的请求-响应和事件处理机制。

## 请求-响应模式设计

### 核心概念

1. **请求(Request)**：从客户端发送到服务器的消息，包含命令、参数和序列号
2. **响应(Response)**：服务器对请求的回复，包含请求序列号、状态和结果数据
3. **序列号(Sequence)**：用于匹配请求和响应的唯一标识符
4. **请求跟踪器(RequestTracker)**：管理活跃请求和匹配响应的组件
5. **超时处理(Timeout Handling)**：处理长时间未响应的请求

### 请求-响应匹配算法

OmniSharp使用序列号(seq)来匹配请求和响应：

1. 客户端为每个请求生成唯一的序列号
2. 服务器在响应中包含对应的请求序列号
3. 客户端使用序列号将响应与原始请求匹配

## 事件处理机制设计

### 核心概念

1. **事件(Event)**：服务器主动发送给客户端的通知消息
2. **事件类型(Event Types)**：不同种类的事件，如诊断更新、文档变更等
3. **事件监听器(Event Listener)**：注册接收特定类型事件的组件
4. **事件分发器(Event Dispatcher)**：负责将接收到的事件分发给相应的监听器

### 事件分发策略

1. **基于类型的分发**：根据事件类型分发给注册的监听器
2. **层级化的监听器**：支持全局监听器和特定类型监听器
3. **异步事件处理**：在单独的线程中处理事件，避免阻塞通信线程

## 核心组件实现

### 1. 请求跟踪器接口

```kotlin
/**
 * 请求跟踪器接口，负责跟踪活跃请求并匹配响应
 */
interface IRequestTracker {
    /**
     * 注册新请求
     * @param request 请求对象
     * @param timeoutMs 超时时间（毫秒）
     * @return 用于获取响应的CompletableFuture
     */
    fun registerRequest(request: OmniSharpRequest, timeoutMs: Long? = null): CompletableFuture<OmniSharpResponse>
    
    /**
     * 处理接收到的响应
     * @param response 响应对象
     * @return 是否成功匹配到请求
     */
    fun handleResponse(response: OmniSharpResponse): Boolean
    
    /**
     * 取消请求
     * @param requestId 请求ID
     * @return 是否成功取消
     */
    fun cancelRequest(requestId: Int): Boolean
    
    /**
     * 获取活跃请求数量
     */
    fun getActiveRequestCount(): Int
    
    /**
     * 清除所有活跃请求
     */
    fun clear()
}
```

### 2. 事件分发器接口

```kotlin
/**
 * 事件分发器接口，负责将接收到的事件分发给相应的监听器
 */
interface IEventDispatcher {
    /**
     * 注册事件监听器
     * @param eventType 事件类型，null表示监听所有事件
     * @param listener 事件监听器
     */
    fun registerListener(eventType: String?, listener: OmniSharpEventListener)
    
    /**
     * 取消注册事件监听器
     * @param eventType 事件类型，null表示取消所有类型
     * @param listener 事件监听器
     */
    fun unregisterListener(eventType: String?, listener: OmniSharpEventListener)
    
    /**
     * 分发事件
     * @param event 事件对象
     */
    fun dispatchEvent(event: OmniSharpEvent)
    
    /**
     * 获取指定类型的监听器数量
     * @param eventType 事件类型，null表示所有事件
     */
    fun getListenerCount(eventType: String? = null): Int
}
```

### 3. 通信管理器接口

```kotlin
/**
 * 通信管理器接口，整合Stdio通道、请求跟踪和事件处理
 */
interface IOmniSharpCommunicator {
    /**
     * 初始化通信器
     * @param process OmniSharp服务器进程
     */
    fun initialize(process: Process)
    
    /**
     * 关闭通信器
     */
    fun shutdown()
    
    /**
     * 发送请求
     * @param request 请求对象
     * @param timeoutMs 超时时间（毫秒）
     * @return 响应的CompletableFuture
     */
    fun sendRequest(request: OmniSharpRequest, timeoutMs: Long? = null): CompletableFuture<OmniSharpResponse>
    
    /**
     * 发送请求（带命令和参数）
     * @param command 命令名称
     * @param arguments 参数映射
     * @param timeoutMs 超时时间（毫秒）
     * @return 响应的CompletableFuture
     */
    fun sendRequest(command: String, arguments: Map<String, Any>? = null, timeoutMs: Long? = null): CompletableFuture<OmniSharpResponse>
    
    /**
     * 注册事件监听器
     * @param eventType 事件类型，null表示监听所有事件
     * @param listener 事件监听器
     */
    fun registerEventListener(eventType: String?, listener: OmniSharpEventListener)
    
    /**
     * 取消注册事件监听器
     * @param eventType 事件类型，null表示取消所有类型
     * @param listener 事件监听器
     */
    fun unregisterEventListener(eventType: String?, listener: OmniSharpEventListener)
    
    /**
     * 通信器是否已初始化
     */
    val isInitialized: Boolean
}
```

## 请求跟踪和响应匹配

### 请求跟踪器实现

```kotlin
/**
 * 请求跟踪器的实现类
 */
class RequestTracker(private val defaultTimeoutMs: Long = 30000) : IRequestTracker {
    private val activeRequests = ConcurrentHashMap<Int, RequestInfo>()
    private val sequenceGenerator = AtomicInteger(0)
    private val timeoutScheduler = Executors.newSingleThreadScheduledExecutor {\ r ->
        Thread(r, "OmniSharp-Request-Timeout").apply { isDaemon = true }
    }
    
    override fun registerRequest(request: OmniSharpRequest, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
        // 生成序列号
        val seq = if (request.seq > 0) request.seq else sequenceGenerator.incrementAndGet()
        request.seq = seq
        
        val future = CompletableFuture<OmniSharpResponse>()
        val timeout = timeoutMs ?: defaultTimeoutMs
        
        // 设置超时任务
        val timeoutTask = if (timeout > 0) {
            timeoutScheduler.schedule({
                if (!future.isDone) {
                    activeRequests.remove(seq)
                    future.completeExceptionally(RequestTimeoutException("Request timed out after $timeout ms", seq))
                }
            }, timeout, TimeUnit.MILLISECONDS)
        } else null
        
        // 保存请求信息
        val requestInfo = RequestInfo(request, future, timeoutTask)
        activeRequests[seq] = requestInfo
        
        return future
    }
    
    override fun handleResponse(response: OmniSharpResponse): Boolean {
        val seq = response.request_seq
        val requestInfo = activeRequests.remove(seq)
        
        if (requestInfo != null) {
            // 取消超时任务
            requestInfo.timeoutTask?.cancel(false)
            
            // 完成Future
            if (response.success == true) {
                requestInfo.future.complete(response)
            } else {
                requestInfo.future.completeExceptionally(RequestFailedException(
                    "Request failed: ${response.message ?: "Unknown error"}", 
                    response.command ?: "",
                    response.request_seq,
                    response.message
                ))
            }
            return true
        }
        
        return false
    }
    
    override fun cancelRequest(requestId: Int): Boolean {
        val requestInfo = activeRequests.remove(requestId)
        
        if (requestInfo != null) {
            // 取消超时任务
            requestInfo.timeoutTask?.cancel(false)
            
            // 取消Future
            requestInfo.future.cancel(false)
            return true
        }
        
        return false
    }
    
    override fun getActiveRequestCount(): Int {
        return activeRequests.size
    }
    
    override fun clear() {
        // 取消所有活跃请求
        activeRequests.values.forEach {\ info ->
            info.timeoutTask?.cancel(false)
            if (!info.future.isDone) {
                info.future.cancel(true)
            }
        }
        
        activeRequests.clear()
    }
    
    /**
     * 请求信息类
     */
    private data class RequestInfo(
        val request: OmniSharpRequest,
        val future: CompletableFuture<OmniSharpResponse>,
        val timeoutTask: ScheduledFuture<*>?
    )
}
```

## 事件处理机制实现

### 事件监听器接口

```kotlin
/**
 * OmniSharp事件监听器接口
 */
interface OmniSharpEventListener {
    /**
     * 当接收到事件时调用
     * @param event 事件对象
     */
    fun onEvent(event: OmniSharpEvent)
}

/**
 * 通用事件监听器适配器
 */
abstract class OmniSharpEventAdapter : OmniSharpEventListener {
    override fun onEvent(event: OmniSharpEvent) {
        // 默认实现，子类可以选择性覆盖
    }
    
    /**
     * 诊断事件处理
     */
    open fun onDiagnostics(event: OmniSharpEvent) {}
    
    /**
     * 文档变更事件处理
     */
    open fun onDocumentChanged(event: OmniSharpEvent) {}
    
    /**
     * 项目加载事件处理
     */
    open fun onProjectLoaded(event: OmniSharpEvent) {}
}
```

### 事件分发器实现

```kotlin
/**
 * 事件分发器的实现类
 */
class EventDispatcher : IEventDispatcher {
    // 事件类型 -> 监听器列表
    private val listenersByType = ConcurrentHashMap<String, CopyOnWriteArrayList<OmniSharpEventListener>>()
    // 全局监听器（监听所有事件）
    private val globalListeners = CopyOnWriteArrayList<OmniSharpEventListener>()
    // 事件处理线程池
    private val eventExecutor = Executors.newCachedThreadPool {\ r ->
        Thread(r, "OmniSharp-Event-Processor").apply { isDaemon = true }
    }
    
    override fun registerListener(eventType: String?, listener: OmniSharpEventListener) {
        if (eventType == null) {
            // 注册为全局监听器
            globalListeners.add(listener)
        } else {
            // 注册为特定类型监听器
            listenersByType.computeIfAbsent(eventType) { CopyOnWriteArrayList() }.add(listener)
        }
    }
    
    override fun unregisterListener(eventType: String?, listener: OmniSharpEventListener) {
        if (eventType == null) {
            // 从全局监听器中移除
            globalListeners.remove(listener)
            
            // 从所有类型中移除
            listenersByType.values.forEach { it.remove(listener) }
        } else {
            // 从特定类型中移除
            listenersByType[eventType]?.remove(listener)
        }
    }
    
    override fun dispatchEvent(event: OmniSharpEvent) {
        val eventType = event.type
        
        // 获取所有需要通知的监听器
        val listenersToNotify = mutableListOf<OmniSharpEventListener>()
        
        // 添加全局监听器
        listenersToNotify.addAll(globalListeners)
        
        // 添加特定类型监听器
        if (eventType != null) {
            listenersByType[eventType]?.let { listenersToNotify.addAll(it) }
        }
        
        // 异步分发事件
        listenersToNotify.forEach { listener ->
            eventExecutor.submit {
                try {
                    listener.onEvent(event)
                    
                    // 如果是适配器，调用特定方法
                    if (listener is OmniSharpEventAdapter) {
                        when (eventType) {
                            "diagnostics" -> listener.onDiagnostics(event)
                            "documentChanged" -> listener.onDocumentChanged(event)
                            "projectLoaded" -> listener.onProjectLoaded(event)
                            // 可以添加更多事件类型
                        }
                    }
                } catch (e: Exception) {
                    Logger.getInstance(javaClass).warn("Error in event listener", e)
                }
            }
        }
    }
    
    override fun getListenerCount(eventType: String?): Int {
        return if (eventType == null) {
            globalListeners.size + listenersByType.values.sumBy { it.size }
        } else {
            listenersByType[eventType]?.size ?: 0
        }
    }
}
```

## 通信管理器实现

```kotlin
/**
 * OmniSharp通信管理器的实现类
 */
class OmniSharpCommunicator(
    private val stdioChannel: IStdioChannel = StdioChannel(),
    private val messageSerializer: IMessageSerializer = JacksonMessageSerializer(),
    private val requestTracker: IRequestTracker = RequestTracker(),
    private val eventDispatcher: IEventDispatcher = EventDispatcher()
) : IOmniSharpCommunicator {
    private var isInitialized = false
    private val initializationLock = ReentrantLock()
    
    override fun initialize(process: Process) {
        initializationLock.lock()
        try {
            if (isInitialized) {
                throw IllegalStateException("Communicator already initialized")
            }
            
            // 打开Stdio通道
            stdioChannel.open(process)
            
            // 添加消息读取监听器
            stdioChannel.addReadListener(object : ReadListener {
                override fun onMessageReceived(message: String) {
                    processIncomingMessage(message)
                }
                
                override fun onChannelClosed() {
                    // 处理通道关闭
                    handleChannelClosed()
                }
            })
            
            // 添加错误监听器
            stdioChannel.addErrorListener(object : ErrorListener {
                override fun onError(error: Throwable) {
                    Logger.getInstance(javaClass).error("Communication error", error)
                }
                
                override fun onProcessTerminated(exitCode: Int?) {
                    Logger.getInstance(javaClass).info("OmniSharp process terminated with exit code: $exitCode")
                    handleProcessTerminated()
                }
            })
            
            isInitialized = true
        } finally {
            initializationLock.unlock()
        }
    }
    
    override fun shutdown() {
        initializationLock.lock()
        try {
            if (!isInitialized) {
                return
            }
            
            // 取消所有活跃请求
            requestTracker.clear()
            
            // 关闭Stdio通道
            stdioChannel.close()
            
            isInitialized = false
        } finally {
            initializationLock.unlock()
        }
    }
    
    override fun sendRequest(request: OmniSharpRequest, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
        checkInitialized()
        
        // 注册请求到跟踪器
        val future = requestTracker.registerRequest(request, timeoutMs)
        
        try {
            // 序列化请求
            val jsonRequest = messageSerializer.serializeRequest(request)
            
            // 通过Stdio通道发送
            stdioChannel.write(jsonRequest)
        } catch (e: Exception) {
            // 如果发送失败，取消请求
            requestTracker.cancelRequest(request.seq)
            val failedFuture = CompletableFuture<OmniSharpResponse>()
            failedFuture.completeExceptionally(SendRequestException("Failed to send request", e))
            return failedFuture
        }
        
        return future
    }
    
    override fun sendRequest(command: String, arguments: Map<String, Any>?, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
        // 创建请求对象
        val request = OmniSharpRequest(
            command = command,
            arguments = arguments ?: emptyMap()
        )
        
        return sendRequest(request, timeoutMs)
    }
    
    override fun registerEventListener(eventType: String?, listener: OmniSharpEventListener) {
        eventDispatcher.registerListener(eventType, listener)
    }
    
    override fun unregisterEventListener(eventType: String?, listener: OmniSharpEventListener) {
        eventDispatcher.unregisterListener(eventType, listener)
    }
    
    override val isInitialized: Boolean
        get() = this.isInitialized
    
    // 处理收到的消息
    private fun processIncomingMessage(message: String) {
        try {
            // 尝试解析为响应或事件
            val jsonNode = messageSerializer.parseJson(message)
            val messageType = jsonNode.get("type")?.asText()
            
            when (messageType) {
                "response" -> {
                    // 处理响应
                    val response = messageSerializer.deserializeResponse(message)
                    requestTracker.handleResponse(response)
                }
                "event" -> {
                    // 处理事件
                    val event = messageSerializer.deserializeEvent(message)
                    eventDispatcher.dispatchEvent(event)
                }
                else -> {
                    // 未知消息类型
                    Logger.getInstance(javaClass).warn("Unknown message type: $messageType")
                }
            }
        } catch (e: Exception) {
            Logger.getInstance(javaClass).error("Error processing incoming message", e)
        }
    }
    
    // 处理通道关闭
    private fun handleChannelClosed() {
        Logger.getInstance(javaClass).warn("Stdio channel closed")
        // 取消所有活跃请求
        requestTracker.clear()
    }
    
    // 处理进程终止
    private fun handleProcessTerminated() {
        Logger.getInstance(javaClass).warn("OmniSharp process terminated")
        // 取消所有活跃请求
        requestTracker.clear()
        // 可以在这里触发重启逻辑或通知上层组件
    }
    
    // 检查是否已初始化
    private fun checkInitialized() {
        if (!isInitialized) {
            throw IllegalStateException("Communicator not initialized")
        }
    }
}
```

## 超时处理

### 超时策略

1. **请求级别超时**：为每个请求单独设置超时时间
2. **默认超时**：提供全局默认超时设置
3. **动态超时调整**：根据请求类型自动调整超时时间

### 超时实现

```kotlin
// 请求超时配置类
class TimeoutConfig {
    // 默认超时时间
    var defaultTimeoutMs: Long = 30000
    
    // 命令特定超时映射
    private val commandTimeouts = mutableMapOf<String, Long>()
    
    // 设置命令特定超时
    fun setCommandTimeout(command: String, timeoutMs: Long) {
        commandTimeouts[command] = timeoutMs
    }
    
    // 获取命令的超时时间
    fun getTimeoutForCommand(command: String): Long {
        return commandTimeouts.getOrDefault(command, defaultTimeoutMs)
    }
}

// 使用示例
val timeoutConfig = TimeoutConfig()
timeoutConfig.setCommandTimeout("workspace/projectInformation", 60000) // 项目信息请求60秒超时
timeoutConfig.setCommandTimeout("completion", 5000) // 补全请求5秒超时

// 在通信器中使用
override fun sendRequest(request: OmniSharpRequest, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
    val effectiveTimeout = timeoutMs ?: timeoutConfig.getTimeoutForCommand(request.command)
    // ... 其余代码
}
```

## 错误处理

### 异常类设计

```kotlin
/**
 * 请求异常基类
 */
open class RequestException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    val errorCode: String = "OMNI_REQUEST_ERROR"
}

/**
 * 请求超时异常
 */
class RequestTimeoutException(message: String, val requestSeq: Int, cause: Throwable? = null) : RequestException(message, cause) {
    override val errorCode: String = "OMNI_REQUEST_TIMEOUT"
}

/**
 * 请求失败异常
 */
class RequestFailedException(message: String, val command: String, val requestSeq: Int, val serverMessage: String? = null, cause: Throwable? = null) : RequestException(message, cause) {
    override val errorCode: String = "OMNI_REQUEST_FAILED"
}

/**
 * 发送请求异常
 */
class SendRequestException(message: String, cause: Throwable? = null) : RequestException(message, cause) {
    override val errorCode: String = "OMNI_SEND_REQUEST_ERROR"
}

/**
 * 消息解析异常
 */
class MessageParseException(message: String, cause: Throwable? = null) : RequestException(message, cause) {
    override val errorCode: String = "OMNI_MESSAGE_PARSE_ERROR"
}
```

### 重试机制

对于可重试的错误，可以实现重试逻辑：

```kotlin
/**
 * 带重试机制的请求发送方法
 */
fun sendRequestWithRetry(
    communicator: IOmniSharpCommunicator,
    request: OmniSharpRequest,
    maxRetries: Int = 3,
    initialDelayMs: Long = 1000,
    backoffMultiplier: Double = 2.0
): CompletableFuture<OmniSharpResponse> {
    val future = CompletableFuture<OmniSharpResponse>()
    
    fun attempt(attemptNumber: Int, delayMs: Long) {
        if (attemptNumber > maxRetries) {
            future.completeExceptionally(RetryExhaustedException("Maximum retry attempts reached", maxRetries))
            return
        }
        
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute {
            communicator.sendRequest(request)
                .thenAccept { future.complete(it) }
                .exceptionally { e ->
                    if (isRetryableError(e)) {
                        val nextDelay = (delayMs * backoffMultiplier).toLong()
                        Logger.getInstance(javaClass).info("Retrying request (attempt $attemptNumber/$maxRetries), next delay: ${nextDelay}ms")
                        attempt(attemptNumber + 1, nextDelay)
                    } else {
                        future.completeExceptionally(e)
                    }
                    null
                }
        }
    }
    
    // 开始第一次尝试
    attempt(1, 0) // 第一次尝试不需要延迟
    
    return future
}

/**
 * 检查错误是否可重试
 */
fun isRetryableError(error: Throwable): Boolean {
    return when (error) {
        is RequestTimeoutException -> true
        is IOException -> true
        is ConnectException -> true
        is SocketTimeoutException -> true
        is ChannelReadException -> true
        is ChannelWriteException -> true
        else -> error.cause?.let { isRetryableError(it) } ?: false
    }
}
```

## 性能优化

### 1. 批量请求处理

对于需要发送多个相关请求的场景，可以实现批量请求处理：

```kotlin
/**
 * 批量发送请求
 * @param requests 请求列表
 * @param parallel 是否并行发送
 * @param timeoutMs 每个请求的超时时间
 * @return 响应列表的CompletableFuture
 */
fun sendBatchRequests(
    communicator: IOmniSharpCommunicator,
    requests: List<OmniSharpRequest>,
    parallel: Boolean = true,
    timeoutMs: Long? = null
): CompletableFuture<List<OmniSharpResponse>> {
    if (parallel) {
        // 并行发送
        val futures = requests.map { communicator.sendRequest(it, timeoutMs) }
        return CompletableFuture.allOf(*futures.toTypedArray())
            .thenApply { futures.map { it.join() } }
    } else {
        // 顺序发送
        val responses = mutableListOf<OmniSharpResponse>()
        var future = CompletableFuture.completedFuture(Unit)
        
        for (request in requests) {
            future = future.thenCompose {
                communicator.sendRequest(request, timeoutMs)
                    .thenAccept { responses.add(it) }
            }
        }
        
        return future.thenApply { responses }
    }
}
```

### 2. 请求合并

对于某些频繁的相似请求，可以实现请求合并：

```kotlin
/**
 * 请求合并器
 */
class RequestBatcher<T>(
    private val batchTimeoutMs: Long = 100,
    private val maxBatchSize: Int = 10
) {
    private val pendingRequests = ConcurrentLinkedQueue<T>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private var isProcessing = false
    private val processingStart = AtomicLong(0)
    
    /**
     * 添加请求到批处理队列
     */
    fun addRequest(request: T): CompletableFuture<List<T>> {
        val future = CompletableFuture<List<T>>()
        
        pendingRequests.add(request)
        
        // 尝试启动批处理
        startBatchProcessing()
        
        return future
    }
    
    // 启动批处理
    private fun startBatchProcessing() {
        lock.lock()
        try {
            if (isProcessing) {
                // 已经在处理中，检查是否超过最大批量或超时
                val currentBatchSize = pendingRequests.size
                val currentTime = System.currentTimeMillis()
                val processingTime = currentTime - processingStart.get()
                
                if (currentBatchSize >= maxBatchSize || processingTime >= batchTimeoutMs) {
                    // 通知处理线程处理当前批次
                    condition.signalAll()
                }
                return
            }
            
            isProcessing = true
            processingStart.set(System.currentTimeMillis())
            
            // 启动处理线程
            Thread { processBatch() }.start()
        } finally {
            lock.unlock()
        }
    }
    
    // 处理批次
    private fun processBatch() {
        try {
            while (true) {
                val batch = lock.withLock {
                    // 检查是否有请求需要处理
                    if (pendingRequests.isEmpty()) {
                        isProcessing = false
                        return@withLock null
                    }
                    
                    // 等待直到达到批处理条件
                    if (pendingRequests.size < maxBatchSize) {
                        try {
                            // 等待超时或通知
                            condition.await(batchTimeoutMs, TimeUnit.MILLISECONDS)
                        } catch (e: InterruptedException) {
                            Thread.currentThread().interrupt()
                            return
                        }
                    }
                    
                    // 提取批次
                    val batch = ArrayList<T>()
                    for (i in 0 until maxBatchSize) {
                        val request = pendingRequests.poll() ?: break
                        batch.add(request)
                    }
                    
                    // 更新处理开始时间
                    processingStart.set(System.currentTimeMillis())
                    
                    batch
                }
                
                if (batch == null || batch.isEmpty()) {
                    break
                }
                
                // 处理批次（这里需要实现具体的批次处理逻辑）
                process(batch)
            }
        } finally {
            lock.lock()
            isProcessing = false
            lock.unlock()
        }
    }
    
    // 批次处理方法（需要子类实现）
    protected open fun process(batch: List<T>) {
        // 子类实现具体的处理逻辑
    }
}
```

### 3. 响应缓存

对于频繁的只读请求，可以实现响应缓存：

```kotlin
/**
 * 响应缓存
 */
class ResponseCache<K, V>(
    private val cacheCapacity: Int = 100,
    private val ttlMs: Long = 30000
) {
    private val cache = LinkedHashMap<K, CacheEntry<V>>(cacheCapacity, 0.75f, true)
    private val lock = ReentrantLock()
    
    /**
     * 获取缓存项
     */
    fun get(key: K): V? {
        lock.lock()
        try {
            val entry = cache[key]
            if (entry != null) {
                // 检查是否过期
                if (System.currentTimeMillis() - entry.timestamp < ttlMs) {
                    return entry.value
                } else {
                    // 过期，移除
                    cache.remove(key)
                }
            }
            return null
        } finally {
            lock.unlock()
        }
    }
    
    /**
     * 添加缓存项
     */
    fun put(key: K, value: V) {
        lock.lock()
        try {
            // 检查容量
            if (cache.size >= cacheCapacity && !cache.containsKey(key)) {
                // 移除最老的项
                val oldestKey = cache.keys.iterator().next()
                cache.remove(oldestKey)
            }
            
            // 添加新项
            cache[key] = CacheEntry(value)
        } finally {
            lock.unlock()
        }
    }
    
    /**
     * 清除缓存
     */
    fun clear() {
        lock.lock()
        try {
            cache.clear()
        } finally {
            lock.unlock()
        }
    }
    
    /**
     * 缓存项
     */
    private data class CacheEntry<T>(val value: T, val timestamp: Long = System.currentTimeMillis())
}

// 使用示例
val cacheKey = "${command}_${arguments.hashCode()}"
val cachedResponse = responseCache.get(cacheKey)
if (cachedResponse != null) {
    return CompletableFuture.completedFuture(cachedResponse)
}

// 发送请求并缓存响应
return sendRequest(request)
    .thenApply {
        responseCache.put(cacheKey, it)
        it
    }
```

## 使用示例

### 基本使用

```kotlin
// 创建通信管理器
val communicator = OmniSharpCommunicator()

// 启动OmniSharp服务器进程
val process = startOmniSharpServer("path/to/project")

// 初始化通信器
communicator.initialize(process)

// 注册事件监听器
communicator.registerEventListener("diagnostics", object : OmniSharpEventListener {
    override fun onEvent(event: OmniSharpEvent) {
        println("Received diagnostics event: ${event.body}")
        // 处理诊断信息
    }
})

// 注册全局事件监听器
communicator.registerEventListener(null, object : OmniSharpEventAdapter() {
    override fun onProjectLoaded(event: OmniSharpEvent) {
        println("Project loaded: ${event.body}")
        // 处理项目加载完成事件
    }
})

// 发送初始化请求
val initializeRequest = OmniSharpRequest(
    command = "initialize",
    arguments = mapOf(
        "rootPath" to "path/to/project",
        "capabilities" to mapOf(
            "textDocument" to mapOf(
                "completion" to mapOf(
                    "completionItemKind" to true
                )
            )
        )
    )
)

communicator.sendRequest(initializeRequest, 5000) // 5秒超时
    .thenAccept { response ->
        println("Initialize response: ${response.body}")
        // 处理初始化响应
    }
    .exceptionally { e ->
        println("Error sending request: ${e.message}")
        null
    }

// 使用便捷方法发送请求
communicator.sendRequest(
    command = "completion",
    arguments = mapOf(
        "line" to 10,
        "column" to 20,
        "buffer" to "content of the buffer"
    ),
    timeoutMs = 3000
)
    .thenAccept { response ->
        // 处理补全响应
    }
    .exceptionally { e ->
        if (e is RequestTimeoutException) {
            println("Request timed out")
        } else {
            println("Error: ${e.message}")
        }
        null
    }

// 程序结束时关闭通信器
// communicator.shutdown()
```

### 高级用法 - 请求批处理

```kotlin
// 创建请求批处理器
val completionBatcher = object : RequestBatcher<CompletionRequest>() {
    override fun process(batch: List<CompletionRequest>) {
        // 合并多个补全请求
        println("Processing batch of ${batch.size} completion requests")
        
        // 这里可以实现批量补全逻辑
        // ...
    }
}

// 添加补全请求到批处理器
for (i in 1..20) {
    completionBatcher.addRequest(CompletionRequest(i, 10 + i, "buffer"))
}
```

### 高级用法 - 带重试的请求

```kotlin
// 创建重试策略
fun <T> withRetry(action: () -> CompletableFuture<T>, maxRetries: Int = 3): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    
    fun attempt(attemptNumber: Int) {
        if (attemptNumber > maxRetries) {
            future.completeExceptionally(Exception("Maximum retries exceeded"))
            return
        }
        
        action()
            .thenAccept { future.complete(it) }
            .exceptionally { e ->
                println("Attempt $attemptNumber failed, retrying...")
                // 延迟重试
                CompletableFuture.delayedExecutor(1000L * attemptNumber, TimeUnit.MILLISECONDS).execute {
                    attempt(attemptNumber + 1)
                }
                null
            }
    }
    
    attempt(1)
    return future
}

// 使用重试策略
withRetry({
    communicator.sendRequest("workspace/projectInformation", null, 5000)
}, 3)
    .thenAccept { response ->
        println("Project information: ${response.body}")
    }
    .exceptionally { e ->
        println("Failed to get project information: ${e.message}")
        null
    }
```

## 后续步骤

1. 编写通信协议的错误处理和重试机制
2. 创建通信协议的单元测试和集成测试
3. 编写通信协议实现文档，包含代码示例和流程图

请求-响应模式和事件处理机制是OmniSharp通信协议的重要组成部分，它们共同确保了与OmniSharp服务器的高效、可靠通信。通过这些机制，客户端可以方便地发送请求、接收响应，以及处理服务器推送的各种事件。