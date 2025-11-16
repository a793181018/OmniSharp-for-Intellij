# OmniSharp通信协议 - Stdio通信管道实现

## 目录

1. [概述](#概述)
2. [Stdio通道接口设计](#stdio通道接口设计)
3. [Stdio通道实现](#stdio通道实现)
4. [消息读取机制](#消息读取机制)
5. [消息写入机制](#消息写入机制)
6. [线程安全设计](#线程安全设计)
7. [错误处理](#错误处理)
8. [性能优化](#性能优化)
9. [使用示例](#使用示例)
10. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp通信协议中Stdio通信管道的实现方案。Stdio通道是与OmniSharp服务器进行通信的核心组件，负责管理进程的标准输入/输出流，实现消息的发送和接收。

### 实现目标

- 提供高效、可靠的Stdio通信机制
- 实现非阻塞的消息读写操作
- 支持消息的异步处理
- 确保线程安全
- 提供友好的错误处理和恢复机制
- 支持消息边界识别

### 技术选型

我们将使用Java的Process API和Java NIO来实现Stdio通信管道，确保高效的进程间通信。

## Stdio通道接口设计

首先，我们定义Stdio通道的接口：

```kotlin
/**
 * Stdio通信通道接口，负责与OmniSharp服务器进程的标准输入/输出流通信
 */
interface IStdioChannel {
    /**
     * 通道是否打开
     */
    val isOpen: Boolean
    
    /**
     * 打开Stdio通道
     * @param process OmniSharp服务器进程
     * @return 是否成功打开
     * @throws ChannelException 通道操作异常
     */
    fun open(process: Process): Boolean
    
    /**
     * 关闭Stdio通道
     * @throws ChannelException 通道操作异常
     */
    fun close()
    
    /**
     * 写入消息
     * @param message 要写入的消息
     * @throws ChannelWriteException 写入异常
     */
    fun write(message: String)
    
    /**
     * 异步写入消息
     * @param message 要写入的消息
     * @return 写入完成的CompletableFuture
     */
    fun writeAsync(message: String): CompletableFuture<Unit>
    
    /**
     * 读取消息
     * @return 读取的消息，如果没有可用消息则返回null
     * @throws ChannelReadException 读取异常
     */
    fun read(): String?
    
    /**
     * 阻塞读取消息
     * @param timeoutMs 超时时间（毫秒），null表示无限等待
     * @return 读取的消息，如果超时则返回null
     * @throws ChannelReadException 读取异常
     */
    fun readBlocking(timeoutMs: Long? = null): String?
    
    /**
     * 添加读取监听器
     * @param listener 读取监听器
     */
    fun addReadListener(listener: ReadListener)
    
    /**
     * 移除读取监听器
     * @param listener 读取监听器
     */
    fun removeReadListener(listener: ReadListener)
    
    /**
     * 添加错误监听器
     * @param listener 错误监听器
     */
    fun addErrorListener(listener: ErrorListener)
    
    /**
     * 移除错误监听器
     * @param listener 错误监听器
     */
    fun removeErrorListener(listener: ErrorListener)
    
    /**
     * 检查是否有可用的输入
     */
    fun hasAvailableInput(): Boolean
    
    /**
     * 获取底层进程
     */
    fun getProcess(): Process?
}
```

### 监听器接口

```kotlin
/**
 * 读取监听器，用于异步处理接收到的消息
 */
interface ReadListener {
    /**
     * 当读取到消息时调用
     * @param message 读取的消息
     */
    fun onMessageReceived(message: String)
    
    /**
     * 当通道关闭时调用
     */
    fun onChannelClosed()
}

/**
 * 错误监听器，用于处理通道错误
 */
interface ErrorListener {
    /**
     * 当发生错误时调用
     * @param error 错误信息
     */
    fun onError(error: Throwable)
    
    /**
     * 当进程异常终止时调用
     * @param exitCode 进程退出码
     */
    fun onProcessTerminated(exitCode: Int?)
}
```

## Stdio通道实现

下面是Stdio通道的具体实现：

```kotlin
/**
 * Stdio通信通道的实现类
 */
class StdioChannel : IStdioChannel {
    private var process: Process? = null
    private var outputStream: BufferedWriter? = null
    private var inputStream: BufferedReader? = null
    private var errorStream: BufferedReader? = null
    private var readThread: Thread? = null
    private var errorThread: Thread? = null
    private val readListeners = CopyOnWriteArrayList<ReadListener>()
    private val errorListeners = CopyOnWriteArrayList<ErrorListener>()
    private var isRunning = false
    private val messageQueue = ConcurrentLinkedQueue<String>()
    private val readLock = ReentrantLock()
    private val writeLock = ReentrantLock()
    private val processMonitor = ScheduledExecutorService
    
    override val isOpen: Boolean
        get() = synchronized(this) {
            process != null && isRunning
        }
    
    override fun open(process: Process): Boolean {
        synchronized(this) {
            if (isOpen) {
                return false
            }
            
            try {
                this.process = process
                this.outputStream = BufferedWriter(OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8))
                this.inputStream = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8))
                this.errorStream = BufferedReader(InputStreamReader(process.errorStream, StandardCharsets.UTF_8))
                this.isRunning = true
                
                // 启动读取线程
                startReadThread()
                startErrorThread()
                
                // 启动进程监控
                startProcessMonitor()
                
                return true
            } catch (e: Exception) {
                close()
                throw ChannelException("Failed to open Stdio channel", e)
            }
        }
    }
    
    override fun close() {
        synchronized(this) {
            isRunning = false
            
            // 关闭线程
            readThread?.interrupt()
            errorThread?.interrupt()
            
            // 关闭流
            try {
                outputStream?.close()
                inputStream?.close()
                errorStream?.close()
            } catch (e: Exception) {
                // 记录错误但不抛出
                Logger.getInstance(javaClass).warn("Error closing streams", e)
            }
            
            // 清空引用
            outputStream = null
            inputStream = null
            errorStream = null
            
            // 通知监听器
            readListeners.forEach { 
                try {
                    it.onChannelClosed()
                } catch (e: Exception) {
                    Logger.getInstance(javaClass).warn("Error in read listener", e)
                }
            }
            
            process = null
        }
    }
    
    override fun write(message: String) {
        writeLock.lock()
        try {
            if (!isOpen) {
                throw ChannelWriteException("Cannot write to closed channel")
            }
            
            val writer = outputStream ?: throw ChannelWriteException("Output stream is not available")
            
            // 添加消息头并写入
            val messageWithLength = "Content-Length: ${message.length}\r\n\r\n$message"
            writer.write(messageWithLength)
            writer.flush()
        } catch (e: IOException) {
            throw ChannelWriteException("Failed to write to channel", e)
        } finally {
            writeLock.unlock()
        }
    }
    
    override fun writeAsync(message: String): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        
        CompletableFuture.runAsync {
            try {
                write(message)
                future.complete(Unit)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        
        return future
    }
    
    override fun read(): String? {
        return messageQueue.poll()
    }
    
    override fun readBlocking(timeoutMs: Long?): String? {
        if (timeoutMs == null || timeoutMs <= 0) {
            // 无限等待
            while (isOpen) {
                val message = read()
                if (message != null) {
                    return message
                }
                Thread.sleep(10)
            }
            return null
        } else {
            // 有限等待
            val endTime = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < endTime && isOpen) {
                val message = read()
                if (message != null) {
                    return message
                }
                Thread.sleep(10)
            }
            return null
        }
    }
    
    override fun addReadListener(listener: ReadListener) {
        readListeners.add(listener)
    }
    
    override fun removeReadListener(listener: ReadListener) {
        readListeners.remove(listener)
    }
    
    override fun addErrorListener(listener: ErrorListener) {
        errorListeners.add(listener)
    }
    
    override fun removeErrorListener(listener: ErrorListener) {
        errorListeners.remove(listener)
    }
    
    override fun hasAvailableInput(): Boolean {
        return !messageQueue.isEmpty()
    }
    
    override fun getProcess(): Process? {
        return process
    }
    
    // 启动读取线程
    private fun startReadThread() {
        readThread = Thread { 
            try {
                val reader = inputStream ?: return@Thread
                val contentLengthPattern = Pattern.compile("Content-Length: (\\d+)")
                
                while (isRunning) {
                    try {
                        // 读取消息头
                        val contentLength = readContentLength(reader, contentLengthPattern)
                        if (contentLength == null) {
                            if (!isRunning) break
                            continue
                        }
                        
                        // 跳过空行
                        readEmptyLine(reader)
                        
                        // 读取消息体
                        val messageBody = readMessageBody(reader, contentLength)
                        if (messageBody != null) {
                            // 添加到队列
                            messageQueue.offer(messageBody)
                            
                            // 通知监听器
                            readListeners.forEach { 
                                try {
                                    it.onMessageReceived(messageBody)
                                } catch (e: Exception) {
                                    Logger.getInstance(javaClass).warn("Error in read listener", e)
                                }
                            }
                        }
                    } catch (e: IOException) {
                        if (isRunning) {
                            notifyError(e)
                        }
                        break
                    }
                }
            } finally {
                // 确保通道关闭
                if (isRunning) {
                    close()
                }
            }
        }
        readThread?.name = "OmniSharp-Stdio-Read"
        readThread?.isDaemon = true
        readThread?.start()
    }
    
    // 启动错误线程
    private fun startErrorThread() {
        errorThread = Thread { 
            try {
                val reader = errorStream ?: return@Thread
                var line: String?
                
                while (isRunning && reader.readLine().also { line = it } != null) {
                    if (line != null) {
                        Logger.getInstance(javaClass).info("OmniSharp stderr: $line")
                    }
                }
            } catch (e: IOException) {
                if (isRunning) {
                    notifyError(e)
                }
            }
        }
        errorThread?.name = "OmniSharp-Stdio-Error"
        errorThread?.isDaemon = true
        errorThread?.start()
    }
    
    // 启动进程监控
    private fun startProcessMonitor() {
        // 定期检查进程状态
        // 这里可以使用ScheduledExecutorService实现
    }
    
    // 读取内容长度
    private fun readContentLength(reader: BufferedReader, pattern: Pattern): Int? {
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            if (line.isNullOrEmpty()) {
                continue
            }
            
            val matcher = pattern.matcher(line)
            if (matcher.matches()) {
                return matcher.group(1).toInt()
            }
        }
        return null
    }
    
    // 跳过空行
    private fun readEmptyLine(reader: BufferedReader) {
        var line: String?
        do {
            line = reader.readLine()
        } while (line != null && !line.isNullOrEmpty())
    }
    
    // 读取消息体
    private fun readMessageBody(reader: BufferedReader, length: Int): String? {
        val buffer = CharArray(length)
        var totalRead = 0
        
        while (totalRead < length) {
            val read = reader.read(buffer, totalRead, length - totalRead)
            if (read == -1) {
                return null // 流已关闭
            }
            totalRead += read
        }
        
        return String(buffer)
    }
    
    // 通知错误
    private fun notifyError(error: Throwable) {
        Logger.getInstance(javaClass).error("Stdio channel error", error)
        errorListeners.forEach { 
            try {
                it.onError(error)
            } catch (e: Exception) {
                Logger.getInstance(javaClass).warn("Error in error listener", e)
            }
        }
    }
    
    // 通知进程终止
    private fun notifyProcessTerminated(exitCode: Int?) {
        errorListeners.forEach { 
            try {
                it.onProcessTerminated(exitCode)
            } catch (e: Exception) {
                Logger.getInstance(javaClass).warn("Error in error listener", e)
            }
        }
    }
}
```

## 消息读取机制

### 基于HTTP风格的消息格式

OmniSharp使用基于HTTP风格的消息格式，每个消息包含一个头部和一个正文：

```
Content-Length: <length>

<message-body>
```

其中：
- `Content-Length` 头部指定消息体的长度（字节数）
- 头部和消息体之间用一个空行（`

`）分隔
- 消息体是JSON格式的字符串

### 读取线程实现

Stdio通道使用单独的线程来读取服务器输出，确保非阻塞操作。读取线程的主要流程：

1. 读取并解析`Content-Length`头部
2. 跳过头部和消息体之间的空行
3. 根据指定的长度读取消息体
4. 将消息添加到队列并通知监听器

## 消息写入机制

### 写入格式

写入到服务器的消息也需要遵循相同的格式：

1. 计算消息体的长度
2. 添加`Content-Length`头部
3. 添加空行分隔符
4. 添加消息体JSON
5. 刷新输出流

### 异步写入

除了同步写入方法外，还提供了异步写入方法，使用`CompletableFuture`实现非阻塞操作。

## 线程安全设计

### 关键线程安全措施

1. **锁机制**：使用`ReentrantLock`保护读写操作
2. **线程安全集合**：使用`ConcurrentLinkedQueue`和`CopyOnWriteArrayList`确保线程安全
3. **同步块**：使用`synchronized`保护关键状态更新
4. **原子操作**：使用原子变量进行状态管理

### 线程模型

Stdio通道使用以下线程：

1. **主线程**：调用通道的公共方法
2. **读取线程**：专门读取服务器输出
3. **错误线程**：专门读取服务器错误输出
4. **写入线程**：异步写入时使用的线程
5. **监控线程**：监控进程状态

## 错误处理

### 异常类设计

```kotlin
/**
 * 通道异常基类
 */
open class ChannelException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    val errorCode: String = "OMNI_CHANNEL_ERROR"
}

/**
 * 通道读取异常
 */
class ChannelReadException(message: String, cause: Throwable? = null) : ChannelException(message, cause) {
    override val errorCode: String = "OMNI_CHANNEL_READ_ERROR"
}

/**
 * 通道写入异常
 */
class ChannelWriteException(message: String, cause: Throwable? = null) : ChannelException(message, cause) {
    override val errorCode: String = "OMNI_CHANNEL_WRITE_ERROR"
}

/**
 * 通道关闭异常
 */
class ChannelClosedException(message: String, cause: Throwable? = null) : ChannelException(message, cause) {
    override val errorCode: String = "OMNI_CHANNEL_CLOSED"
}

/**
 * 通道超时异常
 */
class ChannelTimeoutException(message: String, cause: Throwable? = null) : ChannelException(message, cause) {
    override val errorCode: String = "OMNI_CHANNEL_TIMEOUT"
}
```

### 错误恢复策略

1. **自动重试**：对于临时性错误，实现自动重试机制
2. **错误通知**：通过错误监听器通知上层组件
3. **优雅关闭**：发生致命错误时，确保资源正确释放
4. **进程监控**：监控服务器进程状态，及时发现异常

## 性能优化

### 1. 缓冲区优化

```kotlin
// 使用合适大小的缓冲区
private val outputStream: BufferedWriter? = null
private val inputStream: BufferedReader? = null

// 在构造函数中初始化时指定缓冲区大小
this.outputStream = BufferedWriter(OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8), 8192)
this.inputStream = BufferedReader(InputStreamReader(process.inputStream, StandardCharsets.UTF_8), 8192)
```

### 2. 批量读取

对于频繁的读取操作，可以实现批量读取机制：

```kotlin
/**
 * 批量读取消息
 * @param maxMessages 最大读取消息数
 * @return 读取的消息列表
 */
fun readBatch(maxMessages: Int = 10): List<String> {
    val messages = mutableListOf<String>()
    var count = 0
    
    while (count < maxMessages) {
        val message = read() ?: break
        messages.add(message)
        count++
    }
    
    return messages
}
```

### 3. 消息预取

实现消息预取机制，减少等待时间：

```kotlin
// 在读取线程中，可以一次预取多个消息
private val prefetchQueue = ArrayBlockingQueue<String>(100)

// 修改读取方法，从预取队列获取消息
override fun read(): String? {
    return prefetchQueue.poll()
}
```

### 4. 非阻塞IO

考虑使用Java NIO的非阻塞IO来进一步提高性能：

```kotlin
// 使用NIO的Channel和Selector实现非阻塞IO
private var inputChannel: ReadableByteChannel? = null
private var outputChannel: WritableByteChannel? = null
private val selector: Selector = Selector.open()

// 在读取线程中使用Selector监听可读事件
inputChannel?.register(selector, SelectionKey.OP_READ)
```

## 使用示例

### 基本使用

```kotlin
// 创建Stdio通道实例
val channel = StdioChannel()

// 假设已经有一个OmniSharp进程
val process = // ... 获取OmniSharp进程

// 打开通道
channel.open(process)

// 添加读取监听器
channel.addReadListener(object : ReadListener {
    override fun onMessageReceived(message: String) {
        println("Received message: $message")
        // 处理接收到的消息
    }
    
    override fun onChannelClosed() {
        println("Channel closed")
    }
})

// 添加错误监听器
channel.addErrorListener(object : ErrorListener {
    override fun onError(error: Throwable) {
        println("Channel error: ${error.message}")
    }
    
    override fun onProcessTerminated(exitCode: Int?) {
        println("Process terminated with exit code: $exitCode")
    }
})

// 发送消息
val requestJson = "{\"seq\":1,\"type\":\"request\",\"command\":\"initialize\",\"arguments\":{}}"
channel.write(requestJson)

// 异步发送消息
channel.writeAsync("{\"seq\":2,\"type\":\"request\",\"command\":\"workspace/projectInformation\",\"arguments\":{}}")
    .thenAccept { println("Message sent successfully") }
    .exceptionally { e -> 
        println("Failed to send message: ${e.message}")
        null
    }

// 读取消息（同步）
val message = channel.read()
if (message != null) {
    println("Read message: $message")
}

// 阻塞读取（带超时）
val blockingMessage = channel.readBlocking(5000) // 5秒超时

// 关闭通道
channel.close()
```

### 与序列化器结合使用

```kotlin
// 创建序列化器和通道
val serializer = JacksonMessageSerializer()
val channel = StdioChannel()

// 打开通道
channel.open(omniSharpProcess)

// 发送请求
fun sendRequest(request: OmniSharpRequest): CompletableFuture<String> {
    val future = CompletableFuture<String>()
    
    try {
        // 序列化请求
        val jsonRequest = serializer.serializeRequest(request)
        
        // 异步写入
        channel.writeAsync(jsonRequest)
            .thenAccept {
                // 这里可以实现请求-响应匹配逻辑
                // 例如使用Future和监听器来匹配响应
                future.complete("Request sent successfully")
            }
            .exceptionally { e ->
                future.completeExceptionally(e)
                null
            }
    } catch (e: Exception) {
        future.completeExceptionally(e)
    }
    
    return future
}

// 使用示例
val request = OmniSharpRequest(
    seq = 1,
    command = "initialize",
    arguments = mapOf("rootPath" to projectPath)
)

sendRequest(request)
    .thenAccept { println(it) }
    .exceptionally { e -> 
        println("Error: ${e.message}")
        null
    }
```

## 后续步骤

1. 实现请求-响应模式和事件处理机制
2. 编写通信协议的错误处理和重试机制
3. 创建通信协议的单元测试和集成测试
4. 编写通信协议实现文档，包含代码示例和流程图

Stdio通信管道的实现是OmniSharp通信协议的核心部分，为与OmniSharp服务器的高效通信提供了基础。