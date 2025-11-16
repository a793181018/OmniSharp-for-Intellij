# OmniSharp通信协议 - 错误处理和重试机制实现

## 目录

1. [概述](#概述)
2. [错误类型分析](#错误类型分析)
3. [错误处理架构设计](#错误处理架构设计)
4. [重试机制设计](#重试机制设计)
5. [异常处理策略](#异常处理策略)
6. [容错和恢复机制](#容错和恢复机制)
7. [性能考虑](#性能考虑)
8. [监控和日志](#监控和日志)
9. [实现示例](#实现示例)
10. [最佳实践](#最佳实践)
11. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp通信协议中的错误处理和重试机制的实现方案。在与OmniSharp服务器进行通信的过程中，可能会遇到各种错误和异常情况，如网络中断、进程崩溃、超时等。一个健壮的错误处理和重试机制对于确保通信的可靠性和稳定性至关重要。

### 实现目标

- 提供全面的错误类型分类和处理
- 实现智能的重试策略，区分可重试和不可重试的错误
- 支持不同类型请求的定制化重试策略
- 提供详细的错误信息和日志记录
- 实现自动恢复机制，提高系统容错能力
- 确保错误处理不影响整体系统性能

### 设计原则

1. **错误透明性**：提供清晰的错误信息，帮助开发者诊断问题
2. **恢复优先**：对于可恢复的错误，尽量自动恢复而不是直接失败
3. **智能重试**：根据错误类型和上下文决定是否重试及重试策略
4. **资源保护**：避免重试风暴，保护服务器资源
5. **性能平衡**：错误处理机制不应显著影响正常操作性能

## 错误类型分析

### 通信错误分类

| 错误类别 | 描述 | 可重试性 | 示例 |
|---------|------|---------|------|
| 网络超时 | 请求或响应超时 | 是 | `SocketTimeoutException` |
| 连接错误 | 连接中断或无法建立 | 是 | `IOException`, `ConnectException` |
| 进程错误 | OmniSharp进程崩溃或异常终止 | 条件重试 | 进程退出码非零 |
| 协议错误 | 消息格式错误或协议不匹配 | 否 | `MessageParseException` |
| 服务器错误 | 服务器内部错误 | 部分可重试 | 错误码500系列 |
| 客户端错误 | 客户端请求无效 | 否 | 错误码400系列 |
| 资源错误 | 内存不足或资源耗尽 | 条件重试 | `OutOfMemoryError` |

### 错误码系统

为了标准化错误处理，我们设计一个统一的错误码系统：

```kotlin
/**
 * 错误码系统
 */
object ErrorCodes {
    // 系统级别错误
    const val SYSTEM_ERROR = "OMNI_SYSTEM_ERROR"
    const val RESOURCE_EXHAUSTED = "OMNI_RESOURCE_EXHAUSTED"
    
    // 通信错误
    const val COMMUNICATION_ERROR = "OMNI_COMMUNICATION_ERROR"
    const val CONNECTION_ERROR = "OMNI_CONNECTION_ERROR"
    const val TIMEOUT_ERROR = "OMNI_TIMEOUT_ERROR"
    const val CHANNEL_ERROR = "OMNI_CHANNEL_ERROR"
    const val CHANNEL_READ_ERROR = "OMNI_CHANNEL_READ_ERROR"
    const val CHANNEL_WRITE_ERROR = "OMNI_CHANNEL_WRITE_ERROR"
    
    // 消息处理错误
    const val MESSAGE_ERROR = "OMNI_MESSAGE_ERROR"
    const val MESSAGE_PARSE_ERROR = "OMNI_MESSAGE_PARSE_ERROR"
    const val MESSAGE_FORMAT_ERROR = "OMNI_MESSAGE_FORMAT_ERROR"
    
    // 请求处理错误
    const val REQUEST_ERROR = "OMNI_REQUEST_ERROR"
    const val REQUEST_TIMEOUT = "OMNI_REQUEST_TIMEOUT"
    const val REQUEST_FAILED = "OMNI_REQUEST_FAILED"
    const val SEND_REQUEST_ERROR = "OMNI_SEND_REQUEST_ERROR"
    
    // 服务器错误
    const val SERVER_ERROR = "OMNI_SERVER_ERROR"
    const val SERVER_CRASHED = "OMNI_SERVER_CRASHED"
    const val SERVER_UNAVAILABLE = "OMNI_SERVER_UNAVAILABLE"
    
    // 重试相关错误
    const val RETRY_EXHAUSTED = "OMNI_RETRY_EXHAUSTED"
    const val NON_RETRYABLE_ERROR = "OMNI_NON_RETRYABLE_ERROR"
}
```

## 错误处理架构设计

### 错误处理接口

```kotlin
/**
 * 错误处理器接口
 */
interface IErrorHandler {
    /**
     * 处理错误
     * @param error 错误对象
     * @param context 错误上下文信息
     * @return 是否处理成功
     */
    fun handleError(error: Throwable, context: ErrorContext): Boolean
    
    /**
     * 判断错误是否可重试
     * @param error 错误对象
     * @param context 错误上下文信息
     * @return 是否可重试
     */
    fun isRetryable(error: Throwable, context: ErrorContext): Boolean
    
    /**
     * 获取重试策略
     * @param error 错误对象
     * @param context 错误上下文信息
     * @return 重试策略，如果不可重试则返回null
     */
    fun getRetryStrategy(error: Throwable, context: ErrorContext): RetryStrategy?
    
    /**
     * 记录错误日志
     * @param error 错误对象
     * @param context 错误上下文信息
     */
    fun logError(error: Throwable, context: ErrorContext)
}

/**
 * 错误上下文信息
 */
class ErrorContext(
    val request: OmniSharpRequest? = null,
    val attemptNumber: Int = 1,
    val commandType: String? = null,
    val extraInfo: Map<String, Any> = emptyMap()
) {
    // 添加更多上下文信息的方法
    fun withExtraInfo(key: String, value: Any): ErrorContext {
        val newMap = mutableMapOf<String, Any>()
        newMap.putAll(extraInfo)
        newMap[key] = value
        return ErrorContext(request, attemptNumber, commandType, newMap)
    }
}
```

### 错误处理器实现

```kotlin
/**
 * 默认错误处理器实现
 */
class DefaultErrorHandler(
    private val retryPolicyProvider: IRetryPolicyProvider = DefaultRetryPolicyProvider()
) : IErrorHandler {
    private val logger = Logger.getInstance(javaClass)
    
    override fun handleError(error: Throwable, context: ErrorContext): Boolean {
        try {
            // 记录错误
            logError(error, context)
            
            // 根据错误类型执行不同的处理逻辑
            when (error) {
                is RequestTimeoutException -> handleTimeoutException(error, context)
                is ChannelException -> handleChannelException(error, context)
                is IOException -> handleIOException(error, context)
                is RequestFailedException -> handleRequestFailedException(error, context)
                is MessageParseException -> handleMessageParseException(error, context)
                else -> handleUnknownException(error, context)
            }
            
            return true
        } catch (e: Exception) {
            logger.error("Failed to handle error", e)
            return false
        }
    }
    
    override fun isRetryable(error: Throwable, context: ErrorContext): Boolean {
        // 检查最大重试次数
        if (context.attemptNumber >= 10) { // 设置上限，防止无限重试
            return false
        }
        
        // 根据错误类型判断是否可重试
        when (error) {
            is RequestTimeoutException -> return true
            is ChannelReadException -> return true
            is ChannelWriteException -> return true
            is IOException -> return !isNonRetryableIoException(error)
            is RequestFailedException -> return isRetryableServerError(error)
            is ConnectException -> return true
            is SocketTimeoutException -> return true
            is TimeoutException -> return true
            is RetryExhaustedException -> return false
            else -> return false
        }
    }
    
    override fun getRetryStrategy(error: Throwable, context: ErrorContext): RetryStrategy? {
        if (!isRetryable(error, context)) {
            return null
        }
        
        // 获取重试策略
        val command = context.request?.command ?: context.commandType
        return retryPolicyProvider.getRetryPolicy(command).createStrategy(context.attemptNumber)
    }
    
    override fun logError(error: Throwable, context: ErrorContext) {
        val requestInfo = context.request?.let { "Command: ${it.command}, Seq: ${it.seq}" } ?: ""
        val attemptInfo = "Attempt: ${context.attemptNumber}"
        val errorInfo = "Error: ${error::class.simpleName}, Message: ${error.message}"
        
        val message = "OmniSharp error: $requestInfo $attemptInfo $errorInfo"
        
        // 根据错误类型记录不同级别的日志
        when (error) {
            is RequestTimeoutException -> logger.warn(message, error)
            is RequestFailedException -> logger.warn(message, error)
            else -> logger.error(message, error)
        }
    }
    
    // 处理超时异常
    private fun handleTimeoutException(error: RequestTimeoutException, context: ErrorContext) {
        // 可以实现特定的超时处理逻辑
        logger.warn("Request timed out after multiple attempts: ${error.requestSeq}")
    }
    
    // 处理通道异常
    private fun handleChannelException(error: ChannelException, context: ErrorContext) {
        // 通道异常可能需要重新建立连接
        logger.warn("Channel exception occurred, might need to reconnect: ${error.errorCode}")
    }
    
    // 处理IO异常
    private fun handleIOException(error: IOException, context: ErrorContext) {
        // IO异常处理逻辑
    }
    
    // 处理请求失败异常
    private fun handleRequestFailedException(error: RequestFailedException, context: ErrorContext) {
        // 可以根据服务器返回的错误信息执行特定处理
        logger.warn("Server rejected request: ${error.serverMessage}")
    }
    
    // 处理消息解析异常
    private fun handleMessageParseException(error: MessageParseException, context: ErrorContext) {
        // 记录无效的消息格式，这通常是不可重试的
        logger.error("Failed to parse message from server")
    }
    
    // 处理未知异常
    private fun handleUnknownException(error: Throwable, context: ErrorContext) {
        // 处理未预期的异常
        logger.error("Unexpected error occurred", error)
    }
    
    // 判断是否为不可重试的IO异常
    private fun isNonRetryableIoException(e: IOException): Boolean {
        // 某些IO异常是不可重试的，如文件未找到
        return e is FileNotFoundException || 
               e is EOFException ||
               e is NotSerializableException
    }
    
    // 判断是否为可重试的服务器错误
    private fun isRetryableServerError(e: RequestFailedException): Boolean {
        // 服务器内部错误通常是可重试的
        return e.serverMessage?.contains("500") == true ||
               e.serverMessage?.contains("internal error") == true ||
               e.serverMessage?.contains("server error") == true
    }
}
```

## 重试机制设计

### 重试策略接口

```kotlin
/**
 * 重试策略接口
 */
interface RetryStrategy {
    /**
     * 获取下一次重试的延迟时间（毫秒）
     * @return 延迟时间，如果返回负数表示不再重试
     */
    fun getNextRetryDelay(): Long
    
    /**
     * 是否应该重试
     */
    fun shouldRetry(): Boolean
    
    /**
     * 获取当前重试次数
     */
    fun getCurrentAttempt(): Int
}

/**
 * 重试策略工厂接口
 */
interface IRetryPolicy {
    /**
     * 创建重试策略
     * @param initialAttemptNumber 初始尝试次数
     */
    fun createStrategy(initialAttemptNumber: Int = 1): RetryStrategy
}

/**
 * 重试策略提供器接口
 */
interface IRetryPolicyProvider {
    /**
     * 获取指定命令的重试策略
     * @param command 命令名称，null表示默认策略
     */
    fun getRetryPolicy(command: String?): IRetryPolicy
}
```

### 重试策略实现

```kotlin
/**
 * 指数退避重试策略
 */
class ExponentialBackoffRetryStrategy(
    private val maxRetries: Int,
    private val initialDelayMs: Long,
    private val maxDelayMs: Long,
    private val backoffMultiplier: Double,
    private val jitterFactor: Double = 0.1,
    private var currentAttempt: Int = 1
) : RetryStrategy {
    override fun getNextRetryDelay(): Long {
        if (!shouldRetry()) {
            return -1
        }
        
        // 计算指数退避延迟
        var delay = (initialDelayMs * Math.pow(backoffMultiplier, (currentAttempt - 1).toDouble())).toLong()
        
        // 限制最大延迟
        if (delay > maxDelayMs) {
            delay = maxDelayMs
        }
        
        // 添加随机抖动，避免多个请求同时重试
        if (jitterFactor > 0) {
            val jitter = (delay * jitterFactor).toLong()
            delay = delay + Random().nextLong(-jitter, jitter + 1)
            if (delay < 0) {
                delay = 0
            }
        }
        
        // 增加尝试次数
        currentAttempt++
        
        return delay
    }
    
    override fun shouldRetry(): Boolean {
        return currentAttempt <= maxRetries
    }
    
    override fun getCurrentAttempt(): Int {
        return currentAttempt
    }
}

/**
 * 固定延迟重试策略
 */
class FixedDelayRetryStrategy(
    private val maxRetries: Int,
    private val fixedDelayMs: Long,
    private var currentAttempt: Int = 1
) : RetryStrategy {
    override fun getNextRetryDelay(): Long {
        if (!shouldRetry()) {
            return -1
        }
        
        // 增加尝试次数
        currentAttempt++
        
        return fixedDelayMs
    }
    
    override fun shouldRetry(): Boolean {
        return currentAttempt <= maxRetries
    }
    
    override fun getCurrentAttempt(): Int {
        return currentAttempt
    }
}

/**
 * 线性递增延迟重试策略
 */
class LinearBackoffRetryStrategy(
    private val maxRetries: Int,
    private val initialDelayMs: Long,
    private val incrementMs: Long,
    private val maxDelayMs: Long,
    private var currentAttempt: Int = 1
) : RetryStrategy {
    override fun getNextRetryDelay(): Long {
        if (!shouldRetry()) {
            return -1
        }
        
        // 计算线性递增延迟
        var delay = initialDelayMs + (currentAttempt - 1) * incrementMs
        
        // 限制最大延迟
        if (delay > maxDelayMs) {
            delay = maxDelayMs
        }
        
        // 增加尝试次数
        currentAttempt++
        
        return delay
    }
    
    override fun shouldRetry(): Boolean {
        return currentAttempt <= maxRetries
    }
    
    override fun getCurrentAttempt(): Int {
        return currentAttempt
    }
}

/**
 * 复合重试策略（结合多种策略）
 */
class CompositeRetryStrategy(
    private val strategies: List<RetryStrategy>
) : RetryStrategy {
    override fun getNextRetryDelay(): Long {
        // 所有策略都应该重试
        if (!shouldRetry()) {
            return -1
        }
        
        // 使用第一个策略的延迟
        return strategies.first().getNextRetryDelay()
    }
    
    override fun shouldRetry(): Boolean {
        // 所有策略都应该允许重试
        return strategies.all { it.shouldRetry() }
    }
    
    override fun getCurrentAttempt(): Int {
        // 返回最高的尝试次数
        return strategies.maxOf { it.getCurrentAttempt() }
    }
}
```

### 重试策略工厂实现

```kotlin
/**
 * 默认重试策略工厂
 */
class DefaultRetryPolicy(
    private val maxRetries: Int = 3,
    private val initialDelayMs: Long = 1000,
    private val maxDelayMs: Long = 30000,
    private val backoffMultiplier: Double = 2.0
) : IRetryPolicy {
    override fun createStrategy(initialAttemptNumber: Int): RetryStrategy {
        return ExponentialBackoffRetryStrategy(
            maxRetries,
            initialDelayMs,
            maxDelayMs,
            backoffMultiplier,
            currentAttempt = initialAttemptNumber
        )
    }
}

/**
 * 命令特定的重试策略配置
 */
class CommandRetryConfig(
    val command: String,
    val maxRetries: Int,
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val backoffMultiplier: Double,
    val retryableErrors: Set<String> = emptySet()
)

/**
 * 默认重试策略提供器
 */
class DefaultRetryPolicyProvider : IRetryPolicyProvider {
    private val commandPolicies = mutableMapOf<String, IRetryPolicy>()
    private val defaultPolicy = DefaultRetryPolicy()
    
    init {
        // 初始化命令特定的重试策略
        initializeDefaultCommandPolicies()
    }
    
    override fun getRetryPolicy(command: String?): IRetryPolicy {
        if (command != null && commandPolicies.containsKey(command)) {
            return commandPolicies[command]!!
        }
        return defaultPolicy
    }
    
    // 初始化默认命令策略
    private fun initializeDefaultCommandPolicies() {
        // 高优先级、快速响应的命令，重试次数少，延迟短
        registerCommandPolicy("completion", 2, 200, 1000, 1.5)
        registerCommandPolicy("hover", 2, 200, 1000, 1.5)
        registerCommandPolicy("signatureHelp", 2, 200, 1000, 1.5)
        
        // 中等复杂度命令
        registerCommandPolicy("gotoDefinition", 3, 500, 5000, 2.0)
        registerCommandPolicy("findReferences", 3, 500, 5000, 2.0)
        
        // 耗时较长的命令，重试次数多，延迟大
        registerCommandPolicy("workspace/projectInformation", 5, 2000, 60000, 2.0)
        registerCommandPolicy("runTests", 3, 3000, 30000, 2.0)
        
        // 初始化命令，非常重要
        registerCommandPolicy("initialize", 5, 1000, 30000, 2.0)
    }
    
    // 注册命令策略
    private fun registerCommandPolicy(
        command: String,
        maxRetries: Int,
        initialDelayMs: Long,
        maxDelayMs: Long,
        backoffMultiplier: Double
    ) {
        commandPolicies[command] = DefaultRetryPolicy(
            maxRetries,
            initialDelayMs,
            maxDelayMs,
            backoffMultiplier
        )
    }
    
    // 动态添加或更新命令策略
    fun registerPolicy(command: String, policy: IRetryPolicy) {
        commandPolicies[command] = policy
    }
}
```

## 异常处理策略

### 异常类层次结构

```kotlin
/**
 * OmniSharp异常基类
 */
open class OmniSharpException(message: String, val errorCode: String, cause: Throwable? = null) : Exception(message, cause) {
    val timestamp: Long = System.currentTimeMillis()
    
    override fun toString(): String {
        return "${javaClass.simpleName}[$errorCode]: $message (${timestamp})"
    }
}

/**
 * 通信异常
 */
open class CommunicationException(message: String, errorCode: String, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 通道异常
 */
open class ChannelException(message: String, errorCode: String = ErrorCodes.CHANNEL_ERROR, cause: Throwable? = null) : CommunicationException(message, errorCode, cause)

/**
 * 通道读取异常
 */
class ChannelReadException(message: String, cause: Throwable? = null) : ChannelException(message, ErrorCodes.CHANNEL_READ_ERROR, cause)

/**
 * 通道写入异常
 */
class ChannelWriteException(message: String, cause: Throwable? = null) : ChannelException(message, ErrorCodes.CHANNEL_WRITE_ERROR, cause)

/**
 * 通道关闭异常
 */
class ChannelClosedException(message: String, cause: Throwable? = null) : ChannelException(message, ErrorCodes.CHANNEL_ERROR, cause)

/**
 * 超时异常
 */
open class TimeoutException(message: String, errorCode: String = ErrorCodes.TIMEOUT_ERROR, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 请求超时异常
 */
class RequestTimeoutException(message: String, val requestSeq: Int, cause: Throwable? = null) : TimeoutException(message, ErrorCodes.REQUEST_TIMEOUT, cause)

/**
 * 请求异常
 */
open class RequestException(message: String, errorCode: String = ErrorCodes.REQUEST_ERROR, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 请求失败异常
 */
class RequestFailedException(message: String, val command: String, val requestSeq: Int, val serverMessage: String? = null, cause: Throwable? = null) : RequestException(message, ErrorCodes.REQUEST_FAILED, cause)

/**
 * 发送请求异常
 */
class SendRequestException(message: String, cause: Throwable? = null) : RequestException(message, ErrorCodes.SEND_REQUEST_ERROR, cause)

/**
 * 消息异常
 */
open class MessageException(message: String, errorCode: String = ErrorCodes.MESSAGE_ERROR, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 消息解析异常
 */
class MessageParseException(message: String, cause: Throwable? = null) : MessageException(message, ErrorCodes.MESSAGE_PARSE_ERROR, cause)

/**
 * 服务器异常
 */
open class ServerException(message: String, errorCode: String = ErrorCodes.SERVER_ERROR, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 服务器崩溃异常
 */
class ServerCrashedException(message: String, val exitCode: Int?, cause: Throwable? = null) : ServerException(message, ErrorCodes.SERVER_CRASHED, cause)

/**
 * 服务器不可用异常
 */
class ServerUnavailableException(message: String, cause: Throwable? = null) : ServerException(message, ErrorCodes.SERVER_UNAVAILABLE, cause)

/**
 * 重试异常
 */
open class RetryException(message: String, errorCode: String = ErrorCodes.RETRY_EXHAUSTED, cause: Throwable? = null) : OmniSharpException(message, errorCode, cause)

/**
 * 重试耗尽异常
 */
class RetryExhaustedException(message: String, val maxRetries: Int, cause: Throwable? = null) : RetryException(message, ErrorCodes.RETRY_EXHAUSTED, cause)

/**
 * 不可重试异常
 */
class NonRetryableException(message: String, cause: Throwable? = null) : RetryException(message, ErrorCodes.NON_RETRYABLE_ERROR, cause)
```

### 全局异常处理器

```kotlin
/**
 * 全局异常处理器
 */
class GlobalExceptionHandler {
    private val logger = Logger.getInstance(javaClass)
    private val errorHandlers = mutableListOf<IErrorHandler>()
    
    /**
     * 添加错误处理器
     */
    fun addErrorHandler(handler: IErrorHandler) {
        errorHandlers.add(handler)
    }
    
    /**
     * 移除错误处理器
     */
    fun removeErrorHandler(handler: IErrorHandler) {
        errorHandlers.remove(handler)
    }
    
    /**
     * 处理异常
     * @return 是否成功处理
     */
    fun handleException(e: Throwable, context: ErrorContext = ErrorContext()): Boolean {
        // 转换常见异常为OmniSharp异常
        val omniException = toOmniSharpException(e)
        
        // 尝试使用所有注册的处理器处理异常
        var handled = false
        for (handler in errorHandlers) {
            try {
                if (handler.handleError(omniException, context)) {
                    handled = true
                }
            } catch (handlerError: Exception) {
                logger.error("Error in error handler", handlerError)
            }
        }
        
        // 如果没有处理器处理异常，记录默认日志
        if (!handled) {
            logger.error("Unhandled OmniSharp exception", omniException)
        }
        
        return handled
    }
    
    /**
     * 检查异常是否可重试
     */
    fun isRetryable(e: Throwable, context: ErrorContext = ErrorContext()): Boolean {
        val omniException = toOmniSharpException(e)
        
        // 任何一个处理器认为可重试即为可重试
        return errorHandlers.any { it.isRetryable(omniException, context) }
    }
    
    /**
     * 获取重试策略
     */
    fun getRetryStrategy(e: Throwable, context: ErrorContext = ErrorContext()): RetryStrategy? {
        val omniException = toOmniSharpException(e)
        
        // 从处理器获取重试策略
        for (handler in errorHandlers) {
            val strategy = handler.getRetryStrategy(omniException, context)
            if (strategy != null) {
                return strategy
            }
        }
        
        return null
    }
    
    /**
     * 将通用异常转换为OmniSharp异常
     */
    private fun toOmniSharpException(e: Throwable): OmniSharpException {
        if (e is OmniSharpException) {
            return e
        }
        
        // 根据异常类型转换为对应的OmniSharp异常
        return when (e) {
            is IOException -> when (e) {
                is ConnectException -> ConnectionException("Connection error: ${e.message}", ErrorCodes.CONNECTION_ERROR, e)
                is SocketTimeoutException -> RequestTimeoutException("Socket timeout: ${e.message}", -1, e)
                else -> CommunicationException("IO error: ${e.message}", ErrorCodes.COMMUNICATION_ERROR, e)
            }
            is TimeoutException -> RequestTimeoutException("Timeout: ${e.message}", -1, e)
            is JSONException -> MessageParseException("JSON parse error: ${e.message}", e)
            is ClassCastException -> MessageParseException("Message structure error: ${e.message}", e)
            is NullPointerException -> OmniSharpException("Null reference: ${e.message}", ErrorCodes.SYSTEM_ERROR, e)
            else -> OmniSharpException("Unknown error: ${e.message}", ErrorCodes.SYSTEM_ERROR, e)
        }
    }
    
    companion object {
        // 单例实例
        private val INSTANCE = GlobalExceptionHandler()
        
        fun getInstance(): GlobalExceptionHandler {
            return INSTANCE
        }
    }
}

/**
 * 连接异常
 */
class ConnectionException(message: String, errorCode: String = ErrorCodes.CONNECTION_ERROR, cause: Throwable? = null) : CommunicationException(message, errorCode, cause)
```

## 容错和恢复机制

### 自动恢复机制

```kotlin
/**
 * 自动恢复管理器
 */
class AutoRecoveryManager(
    private val serverManager: IOmniSharpServerManager,
    private val communicator: IOmniSharpCommunicator,
    private val retryPolicyProvider: IRetryPolicyProvider
) {
    private val logger = Logger.getInstance(javaClass)
    private val recoveryLock = ReentrantLock()
    private var isRecoveryInProgress = false
    private val maxRecoveryAttempts = 3
    private var recoveryAttempts = 0
    private val recoveryDelayMs = 2000L
    
    /**
     * 尝试自动恢复通信
     */
    fun attemptRecovery(): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        
        recoveryLock.lock()
        try {
            // 防止并发恢复
            if (isRecoveryInProgress) {
                logger.info("Recovery already in progress, waiting...")
                // 这里可以添加等待恢复完成的逻辑
                future.complete(false)
                return future
            }
            
            // 检查是否达到最大恢复尝试次数
            if (recoveryAttempts >= maxRecoveryAttempts) {
                logger.error("Maximum recovery attempts reached")
                future.complete(false)
                return future
            }
            
            isRecoveryInProgress = true
            recoveryAttempts++
        } finally {
            recoveryLock.unlock()
        }
        
        // 异步执行恢复
        CompletableFuture.runAsync {
            try {
                logger.info("Starting OmniSharp recovery (attempt $recoveryAttempts/$maxRecoveryAttempts)")
                
                // 关闭现有通信器
                if (communicator.isInitialized) {
                    communicator.shutdown()
                }
                
                // 延迟一段时间后重试
                Thread.sleep(recoveryDelayMs)
                
                // 重启服务器
                val serverRestarted = serverManager.restart()
                
                if (serverRestarted) {
                    // 重新初始化通信器
                    val process = serverManager.getServerProcess()
                    if (process != null) {
                        communicator.initialize(process)
                        
                        // 发送初始化请求
                        val initFuture = communicator.sendRequest(
                            "initialize",
                            mapOf(
                                "rootPath" to serverManager.getProjectPath(),
                                "capabilities" to mapOf<String, Any>()
                            ),
                            10000
                        )
                        
                        val initialized = initFuture.get(10, TimeUnit.SECONDS)
                        logger.info("Recovery successful: OmniSharp server restarted and initialized")
                        
                        // 重置恢复尝试次数
                        recoveryLock.lock()
                        try {
                            recoveryAttempts = 0
                        } finally {
                            recoveryLock.unlock()
                        }
                        
                        future.complete(true)
                    } else {
                        logger.error("Recovery failed: No server process available after restart")
                        future.complete(false)
                    }
                } else {
                    logger.error("Recovery failed: Failed to restart OmniSharp server")
                    future.complete(false)
                }
            } catch (e: Exception) {
                logger.error("Recovery failed with exception", e)
                future.complete(false)
            } finally {
                recoveryLock.lock()
                try {
                    isRecoveryInProgress = false
                } finally {
                    recoveryLock.unlock()
                }
            }
        }
        
        return future
    }
    
    /**
     * 重置恢复状态
     */
    fun resetRecoveryState() {
        recoveryLock.lock()
        try {
            recoveryAttempts = 0
            isRecoveryInProgress = false
        } finally {
            recoveryLock.unlock()
        }
    }
    
    /**
     * 检查是否需要恢复
     */
    fun shouldAttemptRecovery(error: Throwable): Boolean {
        // 对于特定错误类型尝试恢复
        return when (error) {
            is ServerCrashedException -> true
            is ChannelException -> true
            is ConnectionException -> true
            is IOException -> true
            else -> false
        }
    }
}
```

### 断路器模式

为了防止在服务器不可用时长时间重试导致的性能问题，实现断路器模式：

```kotlin
/**
 * 断路器状态
 */
enum class CircuitState {
    CLOSED,    // 正常状态，允许请求通过
    OPEN,      // 开路状态，请求直接失败
    HALF_OPEN  // 半开状态，允许有限请求通过以测试系统是否恢复
}

/**
 * 断路器
 */
class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val resetTimeoutMs: Long = 30000,
    private val halfOpenMaxCalls: Int = 3
) {
    private val stateLock = ReentrantLock()
    private var state = CircuitState.CLOSED
    private var failureCount = 0
    private var lastFailureTime = 0L
    private var halfOpenCallCount = 0
    private var halfOpenSuccessCount = 0
    private val logger = Logger.getInstance(javaClass)
    
    /**
     * 执行操作，如果断路器允许
     */
    fun <T> execute(action: () -> T): T {
        // 检查断路器状态
        if (!shouldAllowRequest()) {
            throw CircuitOpenException("Circuit breaker is OPEN")
        }
        
        try {
            // 执行操作
            val result = action()
            
            // 记录成功
            recordSuccess()
            
            return result
        } catch (e: Exception) {
            // 记录失败
            recordFailure()
            
            // 重新抛出异常
            throw e
        }
    }
    
    /**
     * 检查是否允许请求通过
     */
    private fun shouldAllowRequest(): Boolean {
        stateLock.lock()
        try {
            when (state) {
                CircuitState.CLOSED -> return true
                
                CircuitState.OPEN -> {
                    // 检查是否可以切换到HALF_OPEN状态
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFailureTime >= resetTimeoutMs) {
                        logger.info("Circuit breaker state changed: OPEN -> HALF_OPEN")
                        state = CircuitState.HALF_OPEN
                        halfOpenCallCount = 0
                        halfOpenSuccessCount = 0
                        return true
                    }
                    return false
                }
                
                CircuitState.HALF_OPEN -> {
                    // 限制半开状态下的请求数量
                    return halfOpenCallCount < halfOpenMaxCalls
                }
            }
        } finally {
            stateLock.unlock()
        }
    }
    
    /**
     * 记录成功
     */
    private fun recordSuccess() {
        stateLock.lock()
        try {
            when (state) {
                CircuitState.CLOSED -> {
                    // 重置失败计数
                    failureCount = 0
                }
                
                CircuitState.HALF_OPEN -> {
                    // 增加成功计数
                    halfOpenCallCount++
                    halfOpenSuccessCount++
                    
                    // 如果成功次数足够，关闭断路器
                    if (halfOpenSuccessCount >= halfOpenMaxCalls) {
                        logger.info("Circuit breaker state changed: HALF_OPEN -> CLOSED")
                        state = CircuitState.CLOSED
                        failureCount = 0
                    }
                }
                
                CircuitState.OPEN -> {
                    // 开路状态下的成功应该不会发生，因为请求不应该被执行
                }
            }
        } finally {
            stateLock.unlock()
        }
    }
    
    /**
     * 记录失败
     */
    private fun recordFailure() {
        stateLock.lock()
        try {
            when (state) {
                CircuitState.CLOSED -> {
                    // 增加失败计数
                    failureCount++
                    lastFailureTime = System.currentTimeMillis()
                    
                    // 如果失败次数达到阈值，打开断路器
                    if (failureCount >= failureThreshold) {
                        logger.warn("Circuit breaker state changed: CLOSED -> OPEN (failure count: $failureCount)")
                        state = CircuitState.OPEN
                    }
                }
                
                CircuitState.HALF_OPEN -> {
                    // 半开状态下的任何失败都会重新打开断路器
                    logger.info("Circuit breaker state changed: HALF_OPEN -> OPEN (test failed)")
                    state = CircuitState.OPEN
                    lastFailureTime = System.currentTimeMillis()
                }
                
                CircuitState.OPEN -> {
                    // 更新最后失败时间
                    lastFailureTime = System.currentTimeMillis()
                }
            }
        } finally {
            stateLock.unlock()
        }
    }
    
    /**
     * 获取当前状态
     */
    fun getState(): CircuitState {
        stateLock.lock()
        try {
            return state
        } finally {
            stateLock.unlock()
        }
    }
    
    /**
     * 手动重置断路器
     */
    fun reset() {
        stateLock.lock()
        try {
            logger.info("Circuit breaker manually reset")
            state = CircuitState.CLOSED
            failureCount = 0
            halfOpenCallCount = 0
            halfOpenSuccessCount = 0
        } finally {
            stateLock.unlock()
        }
    }
}

/**
 * 断路器开路异常
 */
class CircuitOpenException(message: String) : RuntimeException(message)
```

## 性能考虑

### 异步错误处理

为了不阻塞主线程，错误处理应该在单独的线程中进行：

```kotlin
/**
 * 异步错误处理器
 */
class AsyncErrorHandler(private val delegate: IErrorHandler) : IErrorHandler {
    private val executor = Executors.newCachedThreadPool { r ->
        Thread(r, "OmniSharp-Error-Handler").apply { isDaemon = true }
    }
    
    override fun handleError(error: Throwable, context: ErrorContext): Boolean {
        // 异步处理错误
        executor.submit {
            try {
                delegate.handleError(error, context)
            } catch (e: Exception) {
                // 记录处理错误的错误
                Logger.getInstance(javaClass).error("Error in async error handler", e)
            }
        }
        return true
    }
    
    override fun isRetryable(error: Throwable, context: ErrorContext): Boolean {
        // 同步判断是否可重试
        return delegate.isRetryable(error, context)
    }
    
    override fun getRetryStrategy(error: Throwable, context: ErrorContext): RetryStrategy? {
        // 同步获取重试策略
        return delegate.getRetryStrategy(error, context)
    }
    
    override fun logError(error: Throwable, context: ErrorContext) {
        // 异步记录日志
        executor.submit {
            try {
                delegate.logError(error, context)
            } catch (e: Exception) {
                // 记录日志错误的错误
                Logger.getInstance(javaClass).error("Error logging error", e)
            }
        }
    }
}
```

### 高效的重试调度

使用专用的调度器来管理重试任务：

```kotlin
/**
 * 重试调度器
 */
class RetryScheduler {
    private val scheduler = Executors.newScheduledThreadPool(1) { r ->
        Thread(r, "OmniSharp-Retry-Scheduler").apply { isDaemon = true }
    }
    
    /**
     * 调度重试任务
     * @param delayMs 延迟时间（毫秒）
     * @param task 重试任务
     */
    fun scheduleRetry(delayMs: Long, task: Runnable): ScheduledFuture<*> {
        return scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS)
    }
    
    /**
     * 关闭调度器
     */
    fun shutdown() {
        scheduler.shutdown()
    }
}
```

## 监控和日志

### 错误监控

```kotlin
/**
 * 错误监控器
 */
class ErrorMonitor {
    private val errorStats = ConcurrentHashMap<String, ErrorStats>()
    private val logger = Logger.getInstance(javaClass)
    private val maxErrorHistory = 100
    private val errorHistory = LinkedBlockingQueue<ErrorInfo>(maxErrorHistory)
    
    /**
     * 记录错误
     */
    fun recordError(error: Throwable, context: ErrorContext) {
        // 获取错误码
        val errorCode = if (error is OmniSharpException) {
            error.errorCode
        } else {
            ErrorCodes.SYSTEM_ERROR
        }
        
        // 更新错误统计
        errorStats.computeIfAbsent(errorCode) { ErrorStats(errorCode) }.increment()
        
        // 记录错误历史
        val errorInfo = ErrorInfo(
            errorCode = errorCode,
            message = error.message ?: "Unknown error",
            timestamp = System.currentTimeMillis(),
            command = context.request?.command,
            attempt = context.attemptNumber
        )
        
        // 如果队列已满，移除最旧的错误
        if (errorHistory.size >= maxErrorHistory) {
            errorHistory.poll()
        }
        errorHistory.offer(errorInfo)
        
        // 检查是否需要发出警告（例如错误率过高）
        checkErrorRate(errorCode)
    }
    
    /**
     * 获取错误统计
     */
    fun getErrorStats(): Map<String, ErrorStats> {
        return errorStats.toMap()
    }
    
    /**
     * 获取最近的错误历史
     */
    fun getRecentErrors(limit: Int = 10): List<ErrorInfo> {
        return errorHistory.takeLast(limit)
    }
    
    /**
     * 重置错误统计
     */
    fun resetStats() {
        errorStats.clear()
        errorHistory.clear()
    }
    
    /**
     * 检查错误率
     */
    private fun checkErrorRate(errorCode: String) {
        val stats = errorStats[errorCode]
        if (stats != null && stats.errorCount > 10) {
            val recentRate = stats.getRecentErrorRate()
            if (recentRate > 0.5) { // 50%错误率
                logger.warn("High error rate detected for $errorCode: ${(recentRate * 100).toInt()}%")
            }
        }
    }
    
    /**
     * 错误统计信息
     */
    data class ErrorStats(val errorCode: String) {
        private val errorCount = AtomicInteger(0)
        private val recentErrors = CircularBuffer<Long>(100)
        
        fun increment() {
            errorCount.incrementAndGet()
            recentErrors.add(System.currentTimeMillis())
        }
        
        fun getErrorCount(): Int {
            return errorCount.get()
        }
        
        fun getRecentErrorRate(): Double {
            val now = System.currentTimeMillis()
            val oneMinuteAgo = now - 60000
            
            val recentErrorCount = recentErrors.filter { it > oneMinuteAgo }.size
            return if (recentErrors.size > 0) recentErrorCount.toDouble() / recentErrors.size else 0.0
        }
    }
    
    /**
     * 错误信息
     */
    data class ErrorInfo(
        val errorCode: String,
        val message: String,
        val timestamp: Long,
        val command: String? = null,
        val attempt: Int = 1
    )
    
    /**
     * 循环缓冲区实现
     */
    class CircularBuffer<T>(capacity: Int) {
        private val buffer = arrayOfNulls<Any>(capacity)
        private val size = AtomicInteger(0)
        private val head = AtomicInteger(0)
        
        fun add(element: T) {
            val index = head.getAndUpdate { (it + 1) % buffer.size }
            buffer[index] = element
            if (size.get() < buffer.size) {
                size.incrementAndGet()
            }
        }
        
        fun filter(predicate: (T) -> Boolean): List<T> {
            val result = mutableListOf<T>()
            val currentSize = size.get()
            val currentHead = head.get()
            
            for (i in 0 until currentSize) {
                val index = (currentHead - currentSize + i + buffer.size) % buffer.size
                @Suppress("UNCHECKED_CAST")
                val element = buffer[index] as T
                if (predicate(element)) {
                    result.add(element)
                }
            }
            
            return result
        }
    }
}
```

## 实现示例

### 带重试的通信管理器

```kotlin
/**
 * 带重试功能的通信管理器包装类
 */
class RetryableCommunicator(
    private val communicator: IOmniSharpCommunicator,
    private val errorHandler: IErrorHandler = DefaultErrorHandler(),
    private val recoveryManager: AutoRecoveryManager? = null,
    private val circuitBreaker: CircuitBreaker = CircuitBreaker()
) : IOmniSharpCommunicator by communicator {
    private val logger = Logger.getInstance(javaClass)
    private val retryScheduler = RetryScheduler()
    
    override fun sendRequest(request: OmniSharpRequest, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
        val future = CompletableFuture<OmniSharpResponse>()
        
        // 执行带重试的请求
        executeWithRetry(request, future, timeoutMs, 1)
        
        return future
    }
    
    override fun sendRequest(command: String, arguments: Map<String, Any>?, timeoutMs: Long?): CompletableFuture<OmniSharpResponse> {
        val request = OmniSharpRequest(
            command = command,
            arguments = arguments ?: emptyMap()
        )
        
        return sendRequest(request, timeoutMs)
    }
    
    /**
     * 执行带重试的请求
     */
    private fun executeWithRetry(
        request: OmniSharpRequest,
        future: CompletableFuture<OmniSharpResponse>,
        timeoutMs: Long?,
        attemptNumber: Int
    ) {
        val context = ErrorContext(request, attemptNumber, request.command)
        
        try {
            // 使用断路器执行请求
            circuitBreaker.execute {
                // 调用原始通信器发送请求
                communicator.sendRequest(request, timeoutMs)
                    .thenAccept { response ->
                        // 记录成功
                        logger.debug("Request succeeded: ${request.command} (attempt $attemptNumber)")
                        future.complete(response)
                    }
                    .exceptionally { error ->
                        handleRequestError(request, future, timeoutMs, attemptNumber, error)
                        null
                    }
            }
        } catch (e: CircuitOpenException) {
            // 断路器开路，尝试恢复
            handleCircuitOpen(request, future, timeoutMs, attemptNumber, e)
        } catch (e: Exception) {
            // 处理其他异常
            handleRequestError(request, future, timeoutMs, attemptNumber, e)
        }
    }
    
    /**
     * 处理请求错误
     */
    private fun handleRequestError(
        request: OmniSharpRequest,
        future: CompletableFuture<OmniSharpResponse>,
        timeoutMs: Long?,
        attemptNumber: Int,
        error: Throwable
    ) {
        val context = ErrorContext(request, attemptNumber, request.command)
        
        // 记录错误
        errorHandler.logError(error, context)
        
        // 处理错误
        errorHandler.handleError(error, context)
        
        // 检查是否可重试
        if (errorHandler.isRetryable(error, context)) {
            // 获取重试策略
            val retryStrategy = errorHandler.getRetryStrategy(error, context)
            
            if (retryStrategy != null && retryStrategy.shouldRetry()) {
                val delayMs = retryStrategy.getNextRetryDelay()
                
                if (delayMs >= 0) {
                    logger.info("Retrying request ${request.command} in ${delayMs}ms (attempt ${retryStrategy.getCurrentAttempt()})")
                    
                    // 调度重试
                    retryScheduler.scheduleRetry(delayMs) {
                        executeWithRetry(request, future, timeoutMs, retryStrategy.getCurrentAttempt())
                    }
                    
                    return
                }
            }
        }
        
        // 检查是否需要尝试恢复
        if (recoveryManager != null && recoveryManager.shouldAttemptRecovery(error)) {
            logger.info("Attempting to recover from error: ${error.message}")
            
            recoveryManager.attemptRecovery()
                .thenAccept { recovered ->
                    if (recovered) {
                        // 恢复成功，重试请求
                        logger.info("Recovery successful, retrying request")
                        executeWithRetry(request, future, timeoutMs, 1) // 重置尝试次数
                    } else {
                        // 恢复失败，完成future为失败
                        future.completeExceptionally(error)
                    }
                }
                .exceptionally { recoveryError ->
                    logger.error("Recovery attempt failed", recoveryError)
                    future.completeExceptionally(error)
                    null
                }
            
            return
        }
        
        // 如果不可重试且无法恢复，完成future为失败
        future.completeExceptionally(error)
    }
    
    /**
     * 处理断路器开路
     */
    private fun handleCircuitOpen(
        request: OmniSharpRequest,
        future: CompletableFuture<OmniSharpResponse>,
        timeoutMs: Long?,
        attemptNumber: Int,
        error: Throwable
    ) {
        logger.warn("Circuit breaker open, cannot send request: ${request.command}")
        
        // 尝试恢复
        if (recoveryManager != null) {
            recoveryManager.attemptRecovery()
                .thenAccept { recovered ->
                    if (recovered) {
                        // 恢复成功，重置断路器并重试
                        circuitBreaker.reset()
                        logger.info("Circuit breaker reset after recovery")
                        executeWithRetry(request, future, timeoutMs, 1)
                    } else {
                        // 恢复失败
                        future.completeExceptionally(error)
                    }
                }
                .exceptionally { recoveryError ->
                    logger.error("Recovery attempt failed with circuit open", recoveryError)
                    future.completeExceptionally(error)
                    null
                }
        } else {
            // 没有恢复管理器，直接失败
            future.completeExceptionally(error)
        }
    }
    
    /**
     * 关闭通信器，清理资源
     */
    override fun shutdown() {
        communicator.shutdown()
        retryScheduler.shutdown()
    }
}
```

### 使用示例

```kotlin
// 创建基本组件
val serverManager = OmniSharpServerManagerImpl()
val stdioChannel = StdioChannel()
val messageSerializer = JacksonMessageSerializer()
val requestTracker = RequestTracker()
val eventDispatcher = EventDispatcher()

// 创建基本通信器
val baseCommunicator = OmniSharpCommunicator(stdioChannel, messageSerializer, requestTracker, eventDispatcher)

// 创建错误处理相关组件
val errorHandler = DefaultErrorHandler()
val recoveryManager = AutoRecoveryManager(serverManager, baseCommunicator, DefaultRetryPolicyProvider())
val circuitBreaker = CircuitBreaker()

// 创建带重试的通信器
val communicator = RetryableCommunicator(baseCommunicator, errorHandler, recoveryManager, circuitBreaker)

// 注册全局异常处理器
GlobalExceptionHandler.getInstance().addErrorHandler(errorHandler)

// 启动服务器
val projectPath = "path/to/project"
serverManager.start(projectPath)

// 初始化通信器
val process = serverManager.getServerProcess()
if (process != null) {
    communicator.initialize(process)
    
    // 发送请求，会自动处理错误和重试
    communicator.sendRequest("initialize", mapOf(
        "rootPath" to projectPath,
        "capabilities" to mapOf<String, Any>()
    ))
    .thenAccept { response ->
        println("Initialized successfully: ${response.success}")
    }
    .exceptionally { e ->
        println("Failed to initialize: ${e.message}")
        null
    }
}
```

## 最佳实践

1. **错误分类和策略定制**
   - 根据错误类型和严重程度定制不同的处理策略
   - 为不同类型的请求设置不同的重试策略

2. **避免重试风暴**
   - 使用指数退避和随机抖动减少重试冲突
   - 实现断路器模式防止在服务器不可用时持续请求

3. **资源管理**
   - 确保错误处理过程中不泄漏资源
   - 及时关闭失败的连接和流

4. **监控和告警**
   - 监控错误率和模式
   - 对异常情况设置告警阈值

5. **日志级别**
   - 适当设置日志级别，避免日志过多
   - 记录足够的上下文信息以便调试

6. **用户体验**
   - 在错误处理过程中提供适当的用户反馈
   - 避免长时间无响应

## 后续步骤

1. 创建通信协议的单元测试和集成测试
2. 编写通信协议实现文档，包含代码示例和流程图

错误处理和重试机制是确保OmniSharp通信协议可靠性的关键组成部分。通过合理的错误分类、智能的重试策略和健壮的恢复机制，我们可以显著提高与OmniSharp服务器通信的稳定性和用户体验。