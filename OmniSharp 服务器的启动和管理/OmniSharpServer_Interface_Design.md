# OmniSharp服务器启动和管理 - 核心服务类和接口设计

## 1. 接口概述

本文档定义了OmniSharp服务器启动和管理模块的核心接口和类。这些接口构成了插件与OmniSharp Roslyn服务器交互的基础框架，实现了关注点分离和模块化设计。

## 2. 核心接口设计

### 2.1 IOmniSharpServerManager

```kotlin
package com.github.a793181018.omnisharpforintellij.server

import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.CompletableFuture

/**
 * OmniSharp服务器管理器的主接口
 * 负责协调整个OmniSharp服务器的生命周期和操作
 */
interface IOmniSharpServerManager {
    /**
     * 获取当前服务器状态
     */
    val serverStatus: ServerStatus
    
    /**
     * 服务器状态变更流
     */
    val statusChanges: Flow<ServerStatus>
    
    /**
     * 启动OmniSharp服务器
     * @return CompletableFuture<Boolean> 表示启动是否成功
     */
    fun startServer(): CompletableFuture<Boolean>
    
    /**
     * 停止OmniSharp服务器
     * @return CompletableFuture<Boolean> 表示停止是否成功
     */
    fun stopServer(): CompletableFuture<Boolean>
    
    /**
     * 重启OmniSharp服务器
     * @return CompletableFuture<Boolean> 表示重启是否成功
     */
    fun restartServer(): CompletableFuture<Boolean>
    
    /**
     * 发送请求到OmniSharp服务器
     * @param request 要发送的请求对象
     * @param <T> 响应类型
     * @return CompletableFuture<T> 包含响应的Future
     */
    fun <T> sendRequest(request: OmniSharpRequest<T>): CompletableFuture<T>
    
    /**
     * 订阅OmniSharp服务器事件
     * @param eventType 事件类型
     * @param callback 事件回调函数
     * @param <E> 事件数据类型
     * @return 订阅ID，用于取消订阅
     */
    fun <E> subscribeToEvent(eventType: String, callback: (E) -> Unit): String
    
    /**
     * 取消订阅事件
     * @param subscriptionId 订阅ID
     */
    fun unsubscribeFromEvent(subscriptionId: String)
    
    /**
     * 获取当前项目
     */
    val project: Project
    
    companion object {
        /**
         * 获取指定项目的OmniSharpServerManager实例
         */
        fun getInstance(project: Project): IOmniSharpServerManager {
            return project.getService(IOmniSharpServerManager::class.java)
        }
    }
}

/**
 * 服务器状态枚举
 */
enum class ServerStatus {
    /** 服务器未启动 */
    NOT_STARTED,
    /** 服务器正在启动 */
    STARTING,
    /** 服务器正在运行 */
    RUNNING,
    /** 服务器正在停止 */
    STOPPING,
    /** 服务器已停止 */
    STOPPED,
    /** 服务器出错 */
    ERROR
}
```

### 2.2 IOmniSharpProcessManager

```kotlin
package com.github.a793181018.omnisharpforintellij.server.process

import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * OmniSharp进程管理器接口
 * 负责OmniSharp服务器进程的创建、监控和终止
 */
interface IOmniSharpProcessManager {
    /**
     * 启动OmniSharp服务器进程
     * @param serverPath OmniSharp服务器可执行文件路径
     * @param workingDirectory 工作目录
     * @param arguments 命令行参数
     * @return CompletableFuture<Process> 启动的进程
     */
    fun startProcess(
        serverPath: String,
        workingDirectory: File,
        arguments: List<String>
    ): CompletableFuture<Process>
    
    /**
     * 停止OmniSharp服务器进程
     * @param process 要停止的进程
     * @return CompletableFuture<Boolean> 表示停止是否成功
     */
    fun stopProcess(process: Process): CompletableFuture<Boolean>
    
    /**
     * 获取进程状态
     * @param process 要检查的进程
     * @return 进程是否正在运行
     */
    fun isProcessRunning(process: Process): Boolean
    
    /**
     * 获取进程ID
     * @param process 进程对象
     * @return 进程ID
     */
    fun getProcessId(process: Process): Long
    
    /**
     * 设置进程状态监听器
     * @param process 要监听的进程
     * @param listener 进程状态变更监听器
     */
    fun setProcessListener(process: Process, listener: ProcessListener)
    
    /**
     * 进程状态监听器接口
     */
    interface ProcessListener {
        /**
         * 进程终止时调用
         * @param exitCode 进程退出码
         */
        fun onProcessTerminated(exitCode: Int)
        
        /**
         * 进程发生错误时调用
         * @param error 错误信息
         */
        fun onProcessError(error: Throwable)
    }
}
```

### 2.3 IOmniSharpCommunication

```kotlin
package com.github.a793181018.omnisharpforintellij.server.communication

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import kotlinx.coroutines.flow.Flow

/**
 * OmniSharp通信管理器接口
 * 负责与OmniSharp服务器的双向通信
 */
interface IOmniSharpCommunication {
    /**
     * 初始化通信通道
     * @param inputStream 从OmniSharp服务器读取数据的输入流
     * @param outputStream 向OmniSharp服务器写入数据的输出流
     */
    fun initialize(inputStream: InputStream, outputStream: OutputStream)
    
    /**
     * 发送请求到OmniSharp服务器
     * @param request 要发送的请求对象
     * @param <T> 响应类型
     * @return CompletableFuture<T> 包含响应的Future
     */
    fun <T> sendRequest(request: OmniSharpRequest<T>): CompletableFuture<T>
    
    /**
     * 发送原始消息到OmniSharp服务器
     * @param message 原始消息字符串
     * @return CompletableFuture<Boolean> 表示发送是否成功
     */
    fun sendRawMessage(message: String): CompletableFuture<Boolean>
    
    /**
     * 订阅OmniSharp服务器事件
     * @param eventType 事件类型
     * @param callback 事件回调函数
     * @param <E> 事件数据类型
     * @return 订阅ID，用于取消订阅
     */
    fun <E> subscribeToEvent(eventType: String, callback: (E) -> Unit): String
    
    /**
     * 取消订阅事件
     * @param subscriptionId 订阅ID
     */
    fun unsubscribeFromEvent(subscriptionId: String)
    
    /**
     * 获取所有可用的事件流
     * @return 事件流
     */
    val allEvents: Flow<OmniSharpEvent<*>>
    
    /**
     * 关闭通信通道
     */
    fun close()
    
    /**
     * 检查通信通道是否已关闭
     */
    val isClosed: Boolean
}
```

### 2.4 IOmniSharpConfiguration

```kotlin
package com.github.a793181018.omnisharpforintellij.server.configuration

import com.intellij.openapi.project.Project
import java.io.File

/**
 * OmniSharp服务器配置接口
 * 负责管理OmniSharp服务器的配置选项
 */
interface IOmniSharpConfiguration {
    /**
     * OmniSharp服务器可执行文件路径
     */
    var serverPath: String
    
    /**
     * 工作目录
     */
    var workingDirectory: File
    
    /**
     * 命令行参数
     */
    var arguments: List<String>
    
    /**
     * 最大启动等待时间（毫秒）
     */
    var maxStartupWaitTime: Long
    
    /**
     * 是否启用自动重启
     */
    var autoRestart: Boolean
    
    /**
     * 自动重启最大尝试次数
     */
    var maxRestartAttempts: Int
    
    /**
     * 通信超时时间（毫秒）
     */
    var communicationTimeout: Long
    
    /**
     * 验证配置是否有效
     * @return 验证结果，包含是否有效和错误消息
     */
    fun validate(): ValidationResult
    
    /**
     * 保存配置
     */
    fun save()
    
    /**
     * 从持久化存储加载配置
     */
    fun load()
    
    /**
     * 获取默认配置
     */
    fun getDefaultConfiguration(): IOmniSharpConfiguration
    
    companion object {
        /**
         * 获取指定项目的OmniSharp配置实例
         */
        fun getInstance(project: Project): IOmniSharpConfiguration {
            return project.getService(IOmniSharpConfiguration::class.java)
        }
    }
    
    /**
     * 验证结果类
     */
    data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)
}
```

## 3. 核心模型类

### 3.1 OmniSharpRequest

```kotlin
package com.github.a793181018.omnisharpforintellij.server.model

import com.fasterxml.jackson.databind.JavaType

/**
 * OmniSharp请求模型
 */
class OmniSharpRequest<T>(
    /**
     * 请求命令名称
     */
    val command: String,
    
    /**
     * 请求参数
     */
    val arguments: Map<String, Any>,
    
    /**
     * 响应类型
     */
    val responseType: JavaType
) {
    /**
     * 请求序列号
     */
    val sequence: Long = nextSequence()
    
    companion object {
        private var sequenceCounter: Long = 0
        
        @Synchronized
        private fun nextSequence(): Long {
            return sequenceCounter++
        }
    }
}
```

### 3.2 OmniSharpResponse

```kotlin
package com.github.a793181018.omnisharpforintellij.server.model

/**
 * OmniSharp响应模型
 */
class OmniSharpResponse<T>(
    /**
     * 响应类型
     */
    val type: String,
    
    /**
     * 响应序列号
     */
    val seq: Long,
    
    /**
     * 请求的命令名称
     */
    val command: String,
    
    /**
     * 对应的请求序列号
     */
    val request_seq: Long,
    
    /**
     * 是否仍在运行
     */
    val running: Boolean,
    
    /**
     * 是否成功
     */
    val success: Boolean,
    
    /**
     * 响应体数据
     */
    val body: T
)
```

### 3.3 OmniSharpEvent

```kotlin
package com.github.a793181018.omnisharpforintellij.server.model

/**
 * OmniSharp事件模型
 */
class OmniSharpEvent<T>(
    /**
     * 事件名称
     */
    val event: String,
    
    /**
     * 事件数据
     */
    val body: T
)
```

## 4. 异常类设计

```kotlin
package com.github.a793181018.omnisharpforintellij.server.exceptions

/**
 * OmniSharp服务器异常基类
 */
open class OmniSharpException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * OmniSharp服务器启动异常
 */
class OmniSharpStartupException(message: String, cause: Throwable? = null) : OmniSharpException(message, cause)

/**
 * OmniSharp服务器通信异常
 */
class OmniSharpCommunicationException(message: String, cause: Throwable? = null) : OmniSharpException(message, cause)

/**
 * OmniSharp服务器响应异常
 */
class OmniSharpResponseException(message: String, val responseCode: Int? = null, cause: Throwable? = null) : OmniSharpException(message, cause)

/**
 * OmniSharp服务器配置异常
 */
class OmniSharpConfigurationException(message: String, cause: Throwable? = null) : OmniSharpException(message, cause)
```

## 5. 实现类结构

### 5.1 OmniSharpServerManagerImpl

```kotlin
package com.github.a793181018.omnisharpforintellij.server.impl

import com.github.a793181018.omnisharpforintellij.server.*
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication
import com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration
import com.github.a793181018.omnisharpforintellij.server.exceptions.*
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest
import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * OmniSharp服务器管理器实现类
 */
class OmniSharpServerManagerImpl(
    override val project: Project,
    private val processManager: IOmniSharpProcessManager,
    private val communication: IOmniSharpCommunication,
    private val configuration: IOmniSharpConfiguration
) : IOmniSharpServerManager {
    
    private val logger = thisLogger()
    private val _serverStatus = MutableStateFlow(ServerStatus.NOT_STARTED)
    private var serverProcess: Process? = null
    private val eventSubscriptions = ConcurrentHashMap<String, (Any) -> Unit>()
    private var subscriptionCounter = 0L
    
    override val serverStatus: ServerStatus
        get() = _serverStatus.value
    
    override val statusChanges: StateFlow<ServerStatus>
        get() = _serverStatus.asStateFlow()
    
    // 实现接口方法...
}
```

### 5.2 OmniSharpProcessManagerImpl

```kotlin
package com.github.a793181018.omnisharpforintellij.server.process.impl

import com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpStartupException
import com.intellij.openapi.diagnostic.thisLogger
import java.io.File
import java.lang.management.ManagementFactory
import java.lang.management.RuntimeMXBean
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * OmniSharp进程管理器实现类
 */
class OmniSharpProcessManagerImpl : IOmniSharpProcessManager {
    
    private val logger = thisLogger()
    private val processListeners = ConcurrentHashMap<Process, IOmniSharpProcessManager.ProcessListener>()
    private val runtimeMXBean: RuntimeMXBean = ManagementFactory.getRuntimeMXBean()
    
    // 实现接口方法...
}
```

### 5.3 OmniSharpCommunicationImpl

```kotlin
package com.github.a793181018.omnisharpforintellij.server.communication.impl

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication
import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpCommunicationException
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse
import com.intellij.openapi.diagnostic.thisLogger
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import java.io.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantLock

/**
 * OmniSharp通信管理器实现类
 */
class OmniSharpCommunicationImpl(
    private val objectMapper: ObjectMapper = ObjectMapper()
) : IOmniSharpCommunication {
    
    private val logger = thisLogger()
    private var inputStream: BufferedReader? = null
    private var outputStream: BufferedWriter? = null
    private val responseFutures = ConcurrentHashMap<Long, CompletableFuture<Any>>()
    private val eventSubscriptions = ConcurrentHashMap<String, MutableMap<String, (Any) -> Unit>>()
    private val eventChannel = Channel<OmniSharpEvent<*>>(Channel.UNLIMITED)
    private val isClosed = AtomicBoolean(false)
    private val writeLock = ReentrantLock()
    private var readerThread: Thread? = null
    
    // 实现接口方法...
}
```

### 5.4 OmniSharpConfigurationImpl

```kotlin
package com.github.a793181018.omnisharpforintellij.server.configuration.impl

import com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Paths

/**
 * OmniSharp服务器配置实现类
 */
@State(
    name = "OmniSharpConfiguration",
    storages = [Storage("omnisharp.xml")]
)
class OmniSharpConfigurationImpl(private val project: Project) : 
    IOmniSharpConfiguration, 
    PersistentStateComponent<OmniSharpConfigurationState> {
    
    private var serverPath: String = ""
    private var workingDirectory: File = project.basePath?.let { File(it) } ?: File(System.getProperty("user.dir"))
    private var arguments: List<String> = listOf("--stdio")
    private var maxStartupWaitTime: Long = 30000L // 30 seconds
    private var autoRestart: Boolean = true
    private var maxRestartAttempts: Int = 3
    private var communicationTimeout: Long = 5000L // 5 seconds
    
    // 实现接口方法...
    
    /**
     * 配置持久化状态类
     */
    data class OmniSharpConfigurationState(
        var serverPath: String = "",
        var workingDirectory: String = "",
        var arguments: List<String> = listOf("--stdio"),
        var maxStartupWaitTime: Long = 30000L,
        var autoRestart: Boolean = true,
        var maxRestartAttempts: Int = 3,
        var communicationTimeout: Long = 5000L
    )
}
```

## 6. 服务注册和集成

### 6.1 plugin.xml 服务注册

```xml
<extensions defaultExtensionNs="com.intellij">
    <!-- 现有扩展 -->
    <toolWindow factoryClass="com.github.a793181018.omnisharpforintellij.toolWindow.MyToolWindowFactory" id="MyToolWindow"/>
    <postStartupActivity implementation="com.github.a793181018.omnisharpforintellij.startup.MyProjectActivity" />
    
    <!-- OmniSharp 服务注册 -->
    <applicationService serviceImplementation="com.github.a793181018.omnisharpforintellij.server.process.impl.OmniSharpProcessManagerImpl" serviceInterface="com.github.a793181018.omnisharpforintellij.server.process.IOmniSharpProcessManager" />
    <applicationService serviceImplementation="com.github.a793181018.omnisharpforintellij.server.communication.impl.OmniSharpCommunicationImpl" serviceInterface="com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication" />
    <projectService serviceImplementation="com.github.a793181018.omnisharpforintellij.server.impl.OmniSharpServerManagerImpl" serviceInterface="com.github.a793181018.omnisharpforintellij.server.IOmniSharpServerManager" />
    <projectService serviceImplementation="com.github.a793181018.omnisharpforintellij.server.configuration.impl.OmniSharpConfigurationImpl" serviceInterface="com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration" />
    
    <!-- 项目监听 -->
    <projectManagerListener implementation="com.github.a793181018.omnisharpforintellij.server.OmniSharpProjectManagerListener" />
</extensions>
```

### 6.2 项目监听器实现

```kotlin
package com.github.a793181018.omnisharpforintellij.server

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.diagnostic.thisLogger
import java.util.concurrent.CompletableFuture

/**
 * 项目生命周期监听器，负责在项目打开时启动OmniSharp服务器，关闭时停止服务器
 */
class OmniSharpProjectManagerListener : ProjectManagerListener {
    
    private val logger = thisLogger()
    
    override fun projectOpened(project: Project) {
        logger.info("Project opened: ${project.name}, starting OmniSharp server")
        
        // 获取服务器管理器并启动服务器
        val serverManager = IOmniSharpServerManager.getInstance(project)
        
        serverManager.startServer()
            .thenAccept {
                if (it) {
                    logger.info("OmniSharp server started successfully")
                } else {
                    logger.error("Failed to start OmniSharp server")
                }
            }
            .exceptionally {
                logger.error("Error starting OmniSharp server", it)
                null
            }
    }
    
    override fun projectClosing(project: Project): Boolean {
        logger.info("Project closing: ${project.name}, stopping OmniSharp server")
        
        // 获取服务器管理器并停止服务器
        val serverManager = IOmniSharpServerManager.getInstance(project)
        
        try {
            // 同步等待服务器停止，避免项目关闭时服务器仍在运行
            serverManager.stopServer().get()
            logger.info("OmniSharp server stopped successfully")
        } catch (e: Exception) {
            logger.error("Error stopping OmniSharp server", e)
        }
        
        return true
    }
}
```

## 7. 依赖关系

```
IOmniSharpServerManager --> IOmniSharpProcessManager
                      --> IOmniSharpCommunication
                      --> IOmniSharpConfiguration
                      --> OmniSharpRequest/Response/Event models
                      --> Exception classes

IOmniSharpProcessManager --> ProcessListener

IOmniSharpCommunication --> OmniSharpRequest/Response/Event models
                       --> Exception classes

IOmniSharpConfiguration --> ValidationResult
```

## 8. 接口使用示例

```kotlin
// 示例：如何使用OmniSharp服务器管理器
fun exampleUsage(project: Project) {
    // 获取服务器管理器实例
    val serverManager = IOmniSharpServerManager.getInstance(project)
    
    // 启动服务器
    serverManager.startServer()
        .thenAccept {\ success ->
            if (success) {
                println("服务器启动成功")
                
                // 发送请求示例
                val request = OmniSharpRequest<Any>(
                    command = "/workspace",
                    arguments = emptyMap(),
                    responseType = ObjectMapper().typeFactory.constructType(Any::class.java)
                )
                
                serverManager.sendRequest(request)
                    .thenAccept { response ->
                        println("收到响应: $response")
                    }
                    .exceptionally { e ->
                        println("请求失败: ${e.message}")
                        null
                    }
                
                // 订阅事件示例
                val subscriptionId = serverManager.subscribeToEvent<Any>("projectAdded") { eventData ->
                    println("项目已添加: $eventData")
                }
                
                // 稍后取消订阅
                // serverManager.unsubscribeFromEvent(subscriptionId)
            } else {
                println("服务器启动失败")
            }
        }
        .exceptionally { e ->
            println("启动异常: ${e.message}")
            null
        }
    
    // 监听状态变化
    runBlocking {
        serverManager.statusChanges.collect {\ status ->
            println("服务器状态变更: $status")
        }
    }
}
```

---

本文档提供了OmniSharp服务器启动和管理模块的核心接口和类设计。这些接口定义了清晰的API契约，实现了关注点分离和模块化设计，便于后续开发和测试。