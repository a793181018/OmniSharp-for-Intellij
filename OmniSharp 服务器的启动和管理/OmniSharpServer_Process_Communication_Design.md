# OmniSharp服务器启动和管理 - 进程管理和通信模块设计

## 1. 概述

本文档详细设计了OmniSharp服务器的进程管理和通信模块，这是OmniSharp-for-Intellij插件的核心组件。进程管理模块负责OmniSharp Roslyn服务器进程的生命周期管理，通信模块则负责通过Stdio协议与服务器进行双向通信，实现代码补全、导航等功能。

## 2. 进程管理模块设计

### 2.1 进程启动流程

#### 2.1.1 启动前检查

```kotlin
/**
 * 启动OmniSharp服务器前的检查
 */
private fun preStartupCheck(): PreStartupCheckResult {
    // 检查OmniSharp可执行文件是否存在
    val serverPath = configuration.serverPath
    val serverFile = File(serverPath)
    if (!serverFile.exists() || !serverFile.canExecute()) {
        logger.error("OmniSharp server executable not found or not executable: $serverPath")
        return PreStartupCheckResult(false, "OmniSharp server executable not found or not executable: $serverPath")
    }
    
    // 检查工作目录是否存在
    val workingDir = configuration.workingDirectory
    if (!workingDir.exists() || !workingDir.isDirectory) {
        logger.error("Working directory not found or not a directory: ${workingDir.absolutePath}")
        return PreStartupCheckResult(false, "Working directory not found or not a directory: ${workingDir.absolutePath}")
    }
    
    // 检查是否已有服务器进程在运行
    if (serverProcess != null && processManager.isProcessRunning(serverProcess!!)) {
        logger.warn("OmniSharp server is already running")
        return PreStartupCheckResult(false, "OmniSharp server is already running")
    }
    
    return PreStartupCheckResult(true)
}

data class PreStartupCheckResult(val success: Boolean, val errorMessage: String? = null)
```

#### 2.1.2 进程启动实现

```kotlin
/**
 * 启动OmniSharp服务器进程
 */
override fun startProcess(
    serverPath: String,
    workingDirectory: File,
    arguments: List<String>
): CompletableFuture<Process> {
    val future = CompletableFuture<Process>()
    
    try {
        logger.info("Starting OmniSharp server process: $serverPath with arguments: $arguments")
        
        // 构建进程启动器
        val processBuilder = ProcessBuilder()
            .command(serverPath, *arguments.toTypedArray())
            .directory(workingDirectory)
            .redirectErrorStream(true) // 将标准错误合并到标准输出
        
        // 设置环境变量
        val env = processBuilder.environment()
        // 添加必要的环境变量
        env["OMNISHARP_LOG_LEVEL"] = "Information"
        
        // 启动进程
        val process = processBuilder.start()
        
        logger.info("OmniSharp server process started with PID: ${getProcessId(process)}")
        
        // 监控进程退出
        startProcessExitMonitor(process)
        
        // 返回启动的进程
        future.complete(process)
    } catch (e: Exception) {
        logger.error("Failed to start OmniSharp server process", e)
        future.completeExceptionally(OmniSharpStartupException("Failed to start OmniSharp server process: ${e.message}", e))
    }
    
    return future
}

/**
 * 启动进程退出监控
 */
private fun startProcessExitMonitor(process: Process) {
    val thread = Thread {
        try {
            val exitCode = process.waitFor()
            logger.info("OmniSharp server process exited with code: $exitCode")
            
            // 通知所有监听器
            processListeners[process]?.onProcessTerminated(exitCode)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.error("Process exit monitor interrupted", e)
        } catch (e: Exception) {
            logger.error("Error in process exit monitor", e)
            processListeners[process]?.onProcessError(e)
        } finally {
            // 清理监听器
            processListeners.remove(process)
        }
    }
    
    thread.isDaemon = true
    thread.name = "OmniSharp-Process-Exit-Monitor"
    thread.start()
}
```

### 2.2 进程监控机制

#### 2.2.1 进程状态监控

```kotlin
/**
 * 检查进程是否正在运行
 */
override fun isProcessRunning(process: Process): Boolean {
    try {
        // 通过exitValue()判断，如果进程已终止则不会抛出异常
        process.exitValue()
        return false
    } catch (e: IllegalThreadStateException) {
        // 进程仍在运行
        return true
    }
}

/**
 * 获取进程ID
 */
override fun getProcessId(process: Process): Long {
    try {
        // Java 9+ 有直接获取PID的方法，这里使用反射兼容Java 8
        val pidField = process.javaClass.getDeclaredField("pid")
        pidField.isAccessible = true
        return pidField.getLong(process)
    } catch (e: Exception) {
        // 降级处理，返回-1表示无法获取PID
        logger.warn("Failed to get process ID", e)
        return -1
    }
}
```

#### 2.2.2 进程输出重定向和日志记录

```kotlin
/**
 * 设置进程输出重定向到日志
 */
private fun redirectProcessOutput(process: Process) {
    // 重定向标准输出
    val inputStream = process.inputStream
    val stdoutThread = Thread {
        try {
            BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                lines.forEach { line ->
                    // 根据日志级别分类记录
                    if (line.contains("[ERROR]", ignoreCase = true)) {
                        logger.error("[OmniSharp-Stdout] $line")
                    } else if (line.contains("[WARNING]", ignoreCase = true)) {
                        logger.warn("[OmniSharp-Stdout] $line")
                    } else if (line.contains("[INFO]", ignoreCase = true)) {
                        logger.info("[OmniSharp-Stdout] $line")
                    } else {
                        logger.debug("[OmniSharp-Stdout] $line")
                    }
                    
                    // 解析可能的OmniSharp协议消息
                    tryParseOmniSharpMessage(line)
                }
            }
        } catch (e: Exception) {
            logger.error("Error reading process output", e)
        }
    }
    stdoutThread.isDaemon = true
    stdoutThread.name = "OmniSharp-Stdout-Reader"
    stdoutThread.start()
}

/**
 * 尝试解析OmniSharp协议消息
 */
private fun tryParseOmniSharpMessage(line: String) {
    // 简化的协议消息检测逻辑
    if (line.trimStart().startsWith('{') && line.trimEnd().endsWith('}')) {
        try {
            // 可以在这里添加消息解析和处理逻辑
            logger.trace("Potential OmniSharp protocol message detected")
        } catch (e: Exception) {
            // 不是有效的JSON或OmniSharp协议消息
        }
    }
}
```

### 2.3 进程终止策略

#### 2.3.1 优雅终止

```kotlin
/**
 * 优雅停止OmniSharp服务器进程
 */
override fun stopProcess(process: Process): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    
    // 检查进程是否已终止
    if (!isProcessRunning(process)) {
        logger.info("OmniSharp server process is already terminated")
        future.complete(true)
        return future
    }
    
    Thread {
        try {
            logger.info("Stopping OmniSharp server process: ${getProcessId(process)}")
            
            // 发送终止命令（如果通信通道仍可用）
            sendShutdownCommandIfPossible()
            
            // 尝试优雅终止
            process.destroy()
            
            // 等待进程终止，最多等待5秒
            val terminated = process.waitFor(5, TimeUnit.SECONDS)
            
            if (terminated) {
                logger.info("OmniSharp server process terminated gracefully with exit code: ${process.exitValue()}")
                future.complete(true)
            } else {
                // 强制终止
                logger.warn("OmniSharp server process did not terminate gracefully, forcing termination")
                process.destroyForcibly()
                
                // 再次等待，最多等待3秒
                val forciblyTerminated = process.waitFor(3, TimeUnit.SECONDS)
                
                if (forciblyTerminated) {
                    logger.info("OmniSharp server process forcibly terminated with exit code: ${process.exitValue()}")
                    future.complete(true)
                } else {
                    logger.error("Failed to terminate OmniSharp server process")
                    future.complete(false)
                }
            }
        } catch (e: Exception) {
            logger.error("Error stopping OmniSharp server process", e)
            future.completeExceptionally(e)
        }
    }.start()
    
    return future
}

/**
 * 如果可能，发送关闭命令给OmniSharp服务器
 */
private fun sendShutdownCommandIfPossible() {
    // 实现发送关闭命令的逻辑
    try {
        val shutdownRequest = OmniSharpRequest<Any>(
            command = "/shutdown",
            arguments = emptyMap(),
            responseType = ObjectMapper().typeFactory.constructType(Any::class.java)
        )
        
        communication.sendRequest(shutdownRequest)
            .exceptionally { e ->
                logger.warn("Failed to send shutdown command to OmniSharp server", e)
                null
            }
    } catch (e: Exception) {
        logger.warn("Failed to construct or send shutdown command", e)
    }
}
```

## 3. 通信模块设计

### 3.1 Stdio协议实现

#### 3.1.1 通信初始化

```kotlin
/**
 * 初始化与OmniSharp服务器的通信
 */
override fun initialize(inputStream: InputStream, outputStream: OutputStream) {
    if (isClosed.get()) {
        throw IllegalStateException("Communication channel is already closed")
    }
    
    try {
        // 初始化输入输出流
        this.inputStream = BufferedReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        this.outputStream = BufferedWriter(OutputStreamWriter(outputStream, StandardCharsets.UTF_8))
        
        // 启动消息读取线程
        startMessageReader()
        
        logger.info("OmniSharp communication channel initialized successfully")
    } catch (e: Exception) {
        logger.error("Failed to initialize OmniSharp communication channel", e)
        // 清理已初始化的资源
        close()
        throw OmniSharpCommunicationException("Failed to initialize OmniSharp communication channel: ${e.message}", e)
    }
}

/**
 * 启动消息读取线程
 */
private fun startMessageReader() {
    readerThread = Thread {
        try {
            readMessages()
        } catch (e: Exception) {
            logger.error("Error in message reader thread", e)
        } finally {
            // 当读取线程结束时，关闭通信通道
            if (!isClosed.getAndSet(true)) {
                closeResources()
            }
        }
    }
    
    readerThread!!.isDaemon = true
    readerThread!!.name = "OmniSharp-Message-Reader"
    readerThread!!.start()
}
```

#### 3.1.2 消息读取与解析

```kotlin
/**
 * 从OmniSharp服务器读取消息
 * 实现OmniSharp的Stdio协议：Content-Length: {length}\r\n\r\n{content}
 */
private fun readMessages() {
    val inputStream = this.inputStream ?: throw IllegalStateException("Input stream not initialized")
    
    while (!isClosed.get()) {
        try {
            // 读取Content-Length头
            val headerLine = inputStream.readLine() ?: break
            
            if (!headerLine.startsWith("Content-Length:")) {
                logger.warn("Invalid message header: $headerLine")
                continue
            }
            
            // 解析内容长度
            val length = headerLine.substring("Content-Length:".length).trim().toIntOrNull()
                ?: run {
                    logger.warn("Invalid content length: $headerLine")
                    continue
                }
            
            // 读取空行
            val emptyLine = inputStream.readLine() ?: break
            if (emptyLine.isNotEmpty()) {
                logger.warn("Expected empty line after Content-Length header, got: $emptyLine")
                continue
            }
            
            // 读取消息内容
            val content = CharArray(length)
            var bytesRead = 0
            
            while (bytesRead < length) {
                val read = inputStream.read(content, bytesRead, length - bytesRead)
                if (read == -1) {
                    throw EOFException("End of stream reached while reading message content")
                }
                bytesRead += read
            }
            
            // 处理接收到的消息
            processMessage(String(content))
        } catch (e: IOException) {
            if (!isClosed.get()) {
                logger.error("IO error reading messages from OmniSharp server", e)
            }
            break
        } catch (e: Exception) {
            logger.error("Error processing message from OmniSharp server", e)
        }
    }
}

/**
 * 处理接收到的OmniSharp消息
 */
private fun processMessage(message: String) {
    try {
        // 解析JSON消息
        val jsonNode = objectMapper.readTree(message)
        
        // 判断消息类型
        if (jsonNode.has("type")) {
            val type = jsonNode.get("type").asText()
            
            when (type) {
                "response" -> {
                    // 处理响应消息
                    processResponse(jsonNode)
                }
                "event" -> {
                    // 处理事件消息
                    processEvent(jsonNode)
                }
                else -> {
                    logger.warn("Unknown message type: $type")
                }
            }
        } else {
            logger.warn("Message does not have a 'type' field: $message")
        }
    } catch (e: Exception) {
        logger.error("Failed to process OmniSharp message", e)
    }
}
```

#### 3.1.3 消息发送实现

```kotlin
/**
 * 发送请求到OmniSharp服务器
 */
override fun <T> sendRequest(request: OmniSharpRequest<T>): CompletableFuture<T> {
    val future = CompletableFuture<T>()
    
    if (isClosed.get()) {
        future.completeExceptionally(OmniSharpCommunicationException("Communication channel is closed"))
        return future
    }
    
    try {
        // 构建请求消息
        val requestJson = buildRequestJson(request)
        
        // 存储响应future
        responseFutures[request.sequence] = future as CompletableFuture<Any>
        
        // 发送消息
        sendRawMessage(requestJson)
            .exceptionally { e ->
                // 发送失败，移除future并完成异常
                responseFutures.remove(request.sequence)
                future.completeExceptionally(e)
                false
            }
    } catch (e: Exception) {
        future.completeExceptionally(OmniSharpCommunicationException("Failed to prepare request: ${e.message}", e))
    }
    
    return future
}

/**
 * 构建请求JSON字符串
 */
private fun <T> buildRequestJson(request: OmniSharpRequest<T>): String {
    val requestMap = mutableMapOf<String, Any>(
        "type" to "request",
        "seq" to request.sequence,
        "command" to request.command,
        "arguments" to request.arguments
    )
    
    return objectMapper.writeValueAsString(requestMap)
}

/**
 * 发送原始消息到OmniSharp服务器
 * 实现OmniSharp的Stdio协议：Content-Length: {length}\r\n\r\n{content}
 */
override fun sendRawMessage(message: String): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    
    if (isClosed.get()) {
        future.completeExceptionally(OmniSharpCommunicationException("Communication channel is closed"))
        return future
    }
    
    // 使用锁确保写入的原子性
    writeLock.lock()
    try {
        val outputStream = this.outputStream ?: throw IllegalStateException("Output stream not initialized")
        
        // 计算内容长度
        val contentBytes = message.toByteArray(StandardCharsets.UTF_8)
        val contentLength = contentBytes.size
        
        // 写入头部
        outputStream.write("Content-Length: $contentLength\r\n".toByteArray(StandardCharsets.UTF_8))
        outputStream.write("\r\n".toByteArray(StandardCharsets.UTF_8))
        
        // 写入内容
        outputStream.write(contentBytes)
        outputStream.flush()
        
        logger.debug("Sent message to OmniSharp server: $message")
        future.complete(true)
    } catch (e: Exception) {
        logger.error("Failed to send message to OmniSharp server", e)
        future.completeExceptionally(OmniSharpCommunicationException("Failed to send message: ${e.message}", e))
    } finally {
        writeLock.unlock()
    }
    
    return future
}
```

### 3.2 响应处理

```kotlin
/**
 * 处理OmniSharp服务器响应
 */
private fun processResponse(jsonNode: JsonNode) {
    try {
        // 获取响应序列号
        val seq = jsonNode.get("seq").asLong()
        val requestSeq = jsonNode.get("request_seq").asLong()
        val command = jsonNode.get("command").asText()
        val success = jsonNode.get("success").asBoolean()
        
        // 查找对应的future
        val future = responseFutures.remove(requestSeq)
        if (future == null) {
            logger.warn("Received response for unknown request sequence: $requestSeq")
            return
        }
        
        // 处理响应结果
        if (success) {
            // 响应成功，解析响应体
            val responseType = getResponseTypeForCommand(command)
            if (responseType != null) {
                val body = jsonNode.get("body")
                val responseObject = objectMapper.treeToValue(body, responseType)
                future.complete(responseObject)
            } else {
                // 未知的响应类型，返回原始JSON
                future.complete(body.toString())
            }
        } else {
            // 响应失败，获取错误信息
            val message = if (jsonNode.has("message")) jsonNode.get("message").asText() else "Unknown error"
            future.completeExceptionally(OmniSharpResponseException(message))
        }
    } catch (e: Exception) {
        logger.error("Failed to process OmniSharp response", e)
    }
}

/**
 * 根据命令获取响应类型
 * 这里可以维护一个命令到响应类型的映射表
 */
private fun getResponseTypeForCommand(command: String): Class<*>? {
    // 简化实现，实际项目中应维护一个完整的命令到响应类型的映射
    val typeMap = mapOf(
        "/workspace" to WorkspaceResponse::class.java,
        "/quickinfo" to QuickInfoResponse::class.java,
        "/autocomplete" to CompletionResponse::class.java,
        "/definition" to DefinitionResponse::class.java
        // 添加其他命令对应的响应类型
    )
    
    return typeMap[command]
}
```

### 3.3 事件处理

```kotlin
/**
 * 处理OmniSharp服务器事件
 */
private fun processEvent(jsonNode: JsonNode) {
    try {
        val eventName = jsonNode.get("event").asText()
        val body = jsonNode.get("body")
        
        logger.debug("Received OmniSharp event: $eventName")
        
        // 创建事件对象
        val event = OmniSharpEvent<Any>(eventName, body.toString())
        
        // 发送到事件通道
        runBlocking { eventChannel.send(event) }
        
        // 调用事件订阅者的回调函数
        val subscriptions = eventSubscriptions[eventName] ?: return
        
        for ((_, callback) in subscriptions) {
            try {
                callback(body.toString())
            } catch (e: Exception) {
                logger.error("Error in event callback for $eventName", e)
            }
        }
    } catch (e: Exception) {
        logger.error("Failed to process OmniSharp event", e)
    }
}

/**
 * 订阅OmniSharp服务器事件
 */
override fun <E> subscribeToEvent(eventType: String, callback: (E) -> Unit): String {
    if (isClosed.get()) {
        throw IllegalStateException("Communication channel is closed")
    }
    
    val subscriptionId = "sub-${subscriptionCounter++}"
    
    // 获取或创建该事件类型的订阅列表
    val subscriptions = eventSubscriptions.computeIfAbsent(eventType) { ConcurrentHashMap() }
    
    // 添加订阅
    subscriptions[subscriptionId] = callback as (Any) -> Unit
    
    logger.debug("Subscribed to event: $eventType with ID: $subscriptionId")
    return subscriptionId
}

/**
 * 取消订阅事件
 */
override fun unsubscribeFromEvent(subscriptionId: String) {
    for ((eventType, subscriptions) in eventSubscriptions) {
        if (subscriptions.remove(subscriptionId) != null) {
            logger.debug("Unsubscribed from event: $eventType with ID: $subscriptionId")
            
            // 如果没有订阅者了，清理订阅列表
            if (subscriptions.isEmpty()) {
                eventSubscriptions.remove(eventType)
            }
            return
        }
    }
    
    logger.warn("Failed to unsubscribe: subscription ID not found: $subscriptionId")
}
```

## 4. 通信和进程管理的集成

### 4.1 启动服务器的完整流程

```kotlin
/**
 * 启动OmniSharp服务器的完整流程
 */
override fun startServer(): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    
    // 检查当前状态
    if (serverStatus != ServerStatus.NOT_STARTED && serverStatus != ServerStatus.STOPPED) {
        logger.warn("Cannot start server, current status: $serverStatus")
        future.complete(false)
        return future
    }
    
    // 更新状态为启动中
    updateStatus(ServerStatus.STARTING)
    
    // 异步执行启动流程
    CompletableFuture.runAsync {
        try {
            // 1. 进行启动前检查
            val checkResult = preStartupCheck()
            if (!checkResult.success) {
                logger.error("Pre-startup check failed: ${checkResult.errorMessage}")
                updateStatus(ServerStatus.ERROR)
                future.complete(false)
                return@runAsync
            }
            
            // 2. 启动进程
            val processFuture = processManager.startProcess(
                serverPath = configuration.serverPath,
                workingDirectory = configuration.workingDirectory,
                arguments = configuration.arguments
            )
            
            val process = processFuture.get(configuration.maxStartupWaitTime, TimeUnit.MILLISECONDS)
            this.serverProcess = process
            
            // 3. 设置进程监听器
            processManager.setProcessListener(process, object : IOmniSharpProcessManager.ProcessListener {
                override fun onProcessTerminated(exitCode: Int) {
                    handleProcessTerminated(exitCode)
                }
                
                override fun onProcessError(error: Throwable) {
                    handleProcessError(error)
                }
            })
            
            // 4. 重定向进程输出
            redirectProcessOutput(process)
            
            // 5. 初始化通信
            communication.initialize(process.inputStream, process.outputStream)
            
            // 6. 发送初始化请求
            sendInitializeRequest()
                .thenAccept { initializeResponse ->
                    if (initializeResponse != null) {
                        logger.info("OmniSharp server initialized successfully")
                        updateStatus(ServerStatus.RUNNING)
                        future.complete(true)
                    } else {
                        logger.error("Failed to initialize OmniSharp server")
                        cleanupAfterFailedStartup()
                        future.complete(false)
                    }
                }
                .exceptionally { e ->
                    logger.error("Error initializing OmniSharp server", e)
                    cleanupAfterFailedStartup()
                    future.completeExceptionally(e)
                    null
                }
        } catch (e: TimeoutException) {
            logger.error("OmniSharp server startup timed out after ${configuration.maxStartupWaitTime}ms")
            cleanupAfterFailedStartup()
            future.completeExceptionally(OmniSharpStartupException("Server startup timed out", e))
        } catch (e: Exception) {
            logger.error("Failed to start OmniSharp server", e)
            cleanupAfterFailedStartup()
            future.completeExceptionally(e)
        }
    }
    
    return future
}

/**
 * 发送初始化请求到OmniSharp服务器
 */
private fun sendInitializeRequest(): CompletableFuture<Any> {
    val request = OmniSharpRequest<Any>(
        command = "/initialize",
        arguments = mapOf(
            "processId" to ProcessHandle.current().pid(),
            "rootPath" to configuration.workingDirectory.absolutePath,
            "rootUri" to "file://${configuration.workingDirectory.absolutePath.replace('\\', '/')}",
            "capabilities" to mapOf(
                "textDocument" to mapOf(
                    "completion" to mapOf(
                        "completionItem" to mapOf(
                            "snippetSupport" to true
                        )
                    )
                )
            )
        ),
        responseType = ObjectMapper().typeFactory.constructType(Any::class.java)
    )
    
    return communication.sendRequest(request)
}

/**
 * 启动失败后的清理工作
 */
private fun cleanupAfterFailedStartup() {
    try {
        if (serverProcess != null) {
            processManager.stopProcess(serverProcess!!)
            serverProcess = null
        }
        communication.close()
        updateStatus(ServerStatus.STOPPED)
    } catch (e: Exception) {
        logger.error("Error cleaning up after failed startup", e)
        updateStatus(ServerStatus.ERROR)
    }
}
```

### 4.2 进程终止的处理

```kotlin
/**
 * 处理进程终止事件
 */
private fun handleProcessTerminated(exitCode: Int) {
    logger.info("OmniSharp server process terminated with exit code: $exitCode")
    
    // 更新服务器状态
    val previousStatus = serverStatus
    updateStatus(ServerStatus.STOPPED)
    
    // 清理通信通道
    communication.close()
    serverProcess = null
    
    // 检查是否需要自动重启
    if (configuration.autoRestart && 
        previousStatus != ServerStatus.STOPPING && 
        restartCount < configuration.maxRestartAttempts) {
        
        restartCount++
        logger.info("Attempting to restart OmniSharp server (attempt $restartCount of ${configuration.maxRestartAttempts})")
        
        // 延迟重启，避免频繁重启
        CompletableFuture.runAsync {
            try {
                Thread.sleep(2000)
                startServer()
            } catch (e: Exception) {
                logger.error("Failed to restart OmniSharp server", e)
            }
        }
    } else {
        // 重置重启计数
        restartCount = 0
    }
}

/**
 * 处理进程错误
 */
private fun handleProcessError(error: Throwable) {
    logger.error("OmniSharp server process error", error)
    updateStatus(ServerStatus.ERROR)
}
```

## 5. 线程安全考虑

### 5.1 关键区域保护

```kotlin
/**
 * 更新服务器状态（线程安全）
 */
private fun updateStatus(newStatus: ServerStatus) {
    val oldStatus = serverStatus
    _serverStatus.value = newStatus
    logger.info("OmniSharp server status changed: $oldStatus -> $newStatus")
}

/**
 * 获取服务器状态（线程安全）
 */
override val serverStatus: ServerStatus
    get() = _serverStatus.value
```

### 5.2 并发集合使用

```kotlin
// 使用并发安全的集合存储响应future和事件订阅
private val responseFutures = ConcurrentHashMap<Long, CompletableFuture<Any>>()
private val eventSubscriptions = ConcurrentHashMap<String, MutableMap<String, (Any) -> Unit>>()
private val processListeners = ConcurrentHashMap<Process, IOmniSharpProcessManager.ProcessListener>()
```

## 6. 超时和重试机制

### 6.1 请求超时处理

```kotlin
/**
 * 带超时的请求发送
 */
fun <T> sendRequestWithTimeout(request: OmniSharpRequest<T>, timeoutMs: Long): CompletableFuture<T> {
    val future = sendRequest(request)
    
    // 添加超时处理
    return future.orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
        .exceptionally { e ->
            if (e is TimeoutException) {
                throw OmniSharpCommunicationException("Request timed out after $timeoutMs ms", e)
            }
            throw e
        }
}
```

### 6.2 失败重试策略

```kotlin
/**
 * 带重试的请求发送
 */
fun <T> sendRequestWithRetry(
    request: OmniSharpRequest<T>,
    maxRetries: Int = 3,
    initialDelayMs: Long = 500
): CompletableFuture<T> {
    return sendRequestInternal(request, 0, maxRetries, initialDelayMs)
}

/**
 * 内部重试方法
 */
private fun <T> sendRequestInternal(
    request: OmniSharpRequest<T>,
    currentAttempt: Int,
    maxRetries: Int,
    delayMs: Long
): CompletableFuture<T> {
    return sendRequest(request)
        .exceptionally { e ->
            if (currentAttempt < maxRetries && 
                (e is OmniSharpCommunicationException || 
                 e.cause is IOException)) {
                
                logger.warn("Request failed, retrying (attempt ${currentAttempt + 1} of $maxRetries)...", e)
                
                // 延迟后重试，指数退避
                Thread.sleep(delayMs * Math.pow(2.0, currentAttempt.toDouble()).toLong())
                
                // 创建新的请求，保持相同的命令和参数但更新序列号
                val newRequest = OmniSharpRequest<T>(
                    command = request.command,
                    arguments = request.arguments,
                    responseType = request.responseType
                )
                
                return@exceptionally sendRequestInternal(
                    newRequest,
                    currentAttempt + 1,
                    maxRetries,
                    delayMs
                ).get() // 阻塞以链式处理异常
            }
            throw e
        }
}
```

## 7. 通信协议扩展点

### 7.1 消息拦截器

```kotlin
/**
 * 消息拦截器接口，用于扩展消息处理
 */
interface MessageInterceptor {
    /**
     * 发送消息前拦截
     */
    fun interceptOutgoing(message: String): String
    
    /**
     * 接收消息后拦截
     */
    fun interceptIncoming(message: String): String
}

/**
 * 在通信管理器中集成消息拦截器
 */
fun addMessageInterceptor(interceptor: MessageInterceptor) {
    messageInterceptors.add(interceptor)
}

/**
 * 应用拦截器到出站消息
 */
private fun applyOutgoingInterceptors(message: String): String {
    var result = message
    for (interceptor in messageInterceptors) {
        result = interceptor.interceptOutgoing(result)
    }
    return result
}

/**
 * 应用拦截器到入站消息
 */
private fun applyIncomingInterceptors(message: String): String {
    var result = message
    for (interceptor in messageInterceptors) {
        result = interceptor.interceptIncoming(result)
    }
    return result
}
```

## 8. 性能优化考虑

### 8.1 消息批处理

```kotlin
/**
 * 批量消息发送器，可用于优化频繁发送的小消息
 */
class MessageBatcher(
    private val communication: IOmniSharpCommunication,
    private val batchSize: Int = 10,
    private val maxWaitTimeMs: Long = 100
) {
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()
    private val processingThread = Thread(this::processBatch).apply {
        isDaemon = true
        name = "OmniSharp-Message-Batcher"
    }
    private var isRunning = true
    
    init {
        processingThread.start()
    }
    
    /**
     * 添加消息到批处理队列
     */
    fun addMessage(message: String) {
        messageQueue.add(message)
        lock.lock()
        try {
            condition.signal()
        } finally {
            lock.unlock()
        }
    }
    
    /**
     * 处理批量消息
     */
    private fun processBatch() {
        while (isRunning) {
            val batch = mutableListOf<String>()
            
            // 收集批量消息
            lock.lock()
            try {
                // 等待直到有消息或超时
                if (messageQueue.isEmpty()) {
                    condition.await(maxWaitTimeMs, TimeUnit.MILLISECONDS)
                }
                
                // 收集最多batchSize条消息
                var count = 0
                while (count < batchSize && messageQueue.isNotEmpty()) {
                    batch.add(messageQueue.poll())
                    count++
                }
            } finally {
                lock.unlock()
            }
            
            // 发送批量消息
            if (batch.isNotEmpty()) {
                try {
                    for (message in batch) {
                        communication.sendRawMessage(message)
                    }
                } catch (e: Exception) {
                    logger.error("Error sending batch messages", e)
                }
            }
        }
    }
    
    /**
     * 关闭批处理器
     */
    fun shutdown() {
        isRunning = false
        lock.lock()
        try {
            condition.signal()
        } finally {
            lock.unlock()
        }
        
        try {
            processingThread.join(1000)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}
```

### 8.2 JSON序列化优化

```kotlin
/**
 * 优化的JSON序列化器配置
 */
private fun createOptimizedObjectMapper(): ObjectMapper {
    val mapper = ObjectMapper()
    
    // 禁用不必要的功能
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    mapper.disable(SerializationFeature.INDENT_OUTPUT)
    
    // 启用性能优化
    mapper.enable(SerializationFeature.CLOSE_CLOSEABLE)
    mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    
    // 使用更快的序列化策略
    mapper.setSerializationInclusion(Include.NON_NULL)
    
    return mapper
}
```

## 9. 小结

本文档详细设计了OmniSharp服务器的进程管理和通信模块。进程管理模块负责服务器进程的启动、监控和终止，通信模块实现了Stdio协议的双向通信功能。两个模块紧密协作，确保了OmniSharp服务器的稳定运行和高效通信。设计中考虑了线程安全、错误处理、性能优化等多方面因素，为OmniSharp-for-Intellij插件提供了可靠的后端支持。