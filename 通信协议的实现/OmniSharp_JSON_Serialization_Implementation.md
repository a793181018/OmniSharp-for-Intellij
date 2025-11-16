# OmniSharp通信协议 - JSON消息序列化和反序列化实现

## 目录

1. [概述](#概述)
2. [序列化接口设计](#序列化接口设计)
3. [JSON序列化实现](#json序列化实现)
4. [核心消息模型](#核心消息模型)
5. [序列化和反序列化示例](#序列化和反序列化示例)
6. [错误处理](#错误处理)
7. [性能优化](#性能优化)
8. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp通信协议中JSON消息序列化和反序列化的实现方案。该实现负责将请求、响应和事件对象转换为JSON格式字符串，以及将JSON格式字符串解析回对应的对象结构。

### 实现目标

- 提供高效的JSON序列化和反序列化功能
- 支持OmniSharp协议的所有消息类型
- 确保序列化和反序列化的正确性
- 提供友好的错误处理和调试支持
- 支持复杂嵌套数据结构

### 技术选型

我们将使用Kotlin的序列化库或第三方JSON库（如Jackson或Gson）来实现序列化和反序列化功能。考虑到IntelliJ平台的兼容性，我们选择Jackson库作为序列化引擎。

## 序列化接口设计

首先，我们定义序列化和反序列化的接口：

```kotlin
/**
 * 消息序列化器接口，负责OmniSharp消息的序列化和反序列化
 */
interface IMessageSerializer {
    /**
     * 将请求对象序列化为JSON字符串
     * @param request 请求对象
     * @return 序列化后的JSON字符串
     * @throws SerializationException 序列化失败时抛出
     */
    fun serializeRequest(request: OmniSharpRequest): String
    
    /**
     * 将JSON字符串反序列化为响应对象
     * @param responseString JSON字符串
     * @return 反序列化后的响应对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    fun deserializeResponse(responseString: String): OmniSharpResponse
    
    /**
     * 将JSON字符串反序列化为事件对象
     * @param eventString JSON字符串
     * @return 反序列化后的事件对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    fun deserializeEvent(eventString: String): OmniSharpEvent
    
    /**
     * 将对象序列化为JSON字符串（通用方法）
     * @param obj 要序列化的对象
     * @return 序列化后的JSON字符串
     * @throws SerializationException 序列化失败时抛出
     */
    fun <T> serialize(obj: T): String
    
    /**
     * 将JSON字符串反序列化为指定类型的对象（通用方法）
     * @param json JSON字符串
     * @param type 目标类型
     * @return 反序列化后的对象
     * @throws DeserializationException 反序列化失败时抛出
     */
    fun <T> deserialize(json: String, type: Class<T>): T
}
```

## JSON序列化实现

### Jackson实现

下面是使用Jackson库实现的序列化器：

```kotlin
/**
 * 使用Jackson库实现的消息序列化器
 */
class JacksonMessageSerializer : IMessageSerializer {
    private val objectMapper = ObjectMapper().apply {
        // 配置Jackson
        configure(JsonGenerator.Feature.WRITE_NUMBERS_AS_STRINGS, false)
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // 注册Kotlin模块
        registerModule(KotlinModule())
    }
    
    override fun serializeRequest(request: OmniSharpRequest): String {
        return try {
            objectMapper.writeValueAsString(request)
        } catch (e: Exception) {
            throw SerializationException("Failed to serialize request: ${request.command}", e)
        }
    }
    
    override fun deserializeResponse(responseString: String): OmniSharpResponse {
        return try {
            objectMapper.readValue(responseString, OmniSharpResponse::class.java)
        } catch (e: Exception) {
            throw DeserializationException("Failed to deserialize response", e)
        }
    }
    
    override fun deserializeEvent(eventString: String): OmniSharpEvent {
        return try {
            objectMapper.readValue(eventString, OmniSharpEvent::class.java)
        } catch (e: Exception) {
            throw DeserializationException("Failed to deserialize event", e)
        }
    }
    
    override fun <T> serialize(obj: T): String {
        return try {
            objectMapper.writeValueAsString(obj)
        } catch (e: Exception) {
            throw SerializationException("Failed to serialize object", e)
        }
    }
    
    override fun <T> deserialize(json: String, type: Class<T>): T {
        return try {
            objectMapper.readValue(json, type)
        } catch (e: Exception) {
            throw DeserializationException("Failed to deserialize to ${type.simpleName}", e)
        }
    }
}
```

### 实现序列化异常类

```kotlin
/**
 * 序列化异常基类
 */
open class SerializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * 反序列化异常
 */
class DeserializationException(message: String, cause: Throwable? = null) : SerializationException(message, cause)
```

## 核心消息模型

### 1. OmniSharpRequest

```kotlin
/**
 * OmniSharp请求模型
 */
data class OmniSharpRequest(
    /** 请求序列号 */
    val seq: Int,
    /** 消息类型，通常为"request" */
    val type: String = "request",
    /** 命令名称 */
    val command: String,
    /** 命令参数 */
    val arguments: Map<String, Any> = emptyMap()
)
```

### 2. OmniSharpResponse

```kotlin
/**
 * OmniSharp响应模型
 */
data class OmniSharpResponse(
    /** 响应序列号 */
    val seq: Int,
    /** 消息类型，通常为"response" */
    val type: String,
    /** 命令名称 */
    val command: String,
    /** 对应的请求序列号 */
    val request_seq: Int,
    /** 响应体数据 */
    val body: Map<String, Any> = emptyMap(),
    /** 服务器是否仍在运行 */
    val running: Boolean,
    /** 请求是否成功 */
    val success: Boolean,
    /** 错误消息（如果有） */
    val message: String? = null
)
```

### 3. OmniSharpEvent

```kotlin
/**
 * OmniSharp事件模型
 */
data class OmniSharpEvent(
    /** 事件序列号 */
    val seq: Int,
    /** 消息类型，通常为"event" */
    val type: String = "event",
    /** 事件名称 */
    val event: String,
    /** 事件体数据 */
    val body: Map<String, Any> = emptyMap()
)
```

### 4. 特定命令的请求和响应模型

为了方便使用，可以为常见的OmniSharp命令定义特定的请求和响应模型：

```kotlin
/**
 * 代码补全请求
 */
data class CompletionRequest(
    val fileName: String,
    val line: Int,
    val column: Int,
    val wantSnippet: Boolean = false,
    val wantMethodHeader: Boolean = false,
    val wantReturnType: Boolean = false,
    val wantDocumentationForEveryCompletionResult: Boolean = false,
    val triggerCharacter: String? = null,
    val triggerKind: Int? = null
)

/**
 * 代码补全响应
 */
data class CompletionResponse(
    val completionItems: List<CompletionItem>
)

/**
 * 补全项
 */
data class CompletionItem(
    val label: String,
    val kind: Int,
    val sortText: String,
    val insertText: String,
    val insertTextFormat: Int,
    val documentation: String? = null,
    val detail: String? = null,
    val filterText: String? = null
)
```

## 序列化和反序列化示例

### 序列化请求示例

```kotlin
// 创建序列化器实例
val serializer = JacksonMessageSerializer()

// 创建代码补全请求
val completionRequest = CompletionRequest(
    fileName = "c:\\path\\to\\file.cs",
    line = 10,
    column = 15,
    wantSnippet = true
)

// 转换为通用请求格式
val request = OmniSharpRequest(
    seq = 1,
    command = "completion",
    arguments = mapOf(
        "FileName" to completionRequest.fileName,
        "Line" to completionRequest.line,
        "Column" to completionRequest.column,
        "WantSnippet" to completionRequest.wantSnippet
    )
)

// 序列化请求
val jsonRequest = serializer.serializeRequest(request)
println(jsonRequest)
// 输出: {"seq":1,"type":"request","command":"completion","arguments":{"FileName":"c:\\path\\to\\file.cs","Line":10,"Column":15,"WantSnippet":true}}
```

### 反序列化响应示例

```kotlin
// 假设有一个JSON响应字符串
val jsonResponse = "{\"seq\":1,\"type\":\"response\",\"command\":\"completion\",\"request_seq\":1,\"body\":{\"completionItems\":[{\"label\":\"Console\",\"kind\":5}]},\"running\":true,\"success\":true}"

// 反序列化响应
val response = serializer.deserializeResponse(jsonResponse)

// 访问响应数据
println("Command: ${response.command}")
println("Success: ${response.success}")
println("Body: ${response.body}")

// 可以进一步解析响应体
val completionItems = response.body["completionItems"] as? List<Map<String, Any>>
completionItems?.forEach {
    println("Completion: ${it["label"]}")
}
```

### 反序列化事件示例

```kotlin
// 假设有一个JSON事件字符串
val jsonEvent = "{\"seq\":1,\"type\":\"event\",\"event\":\"ProjectLoaded\",\"body\":{\"MsBuildProject\":{\"FilePath\":\"project.csproj\"}}}"

// 反序列化事件
val event = serializer.deserializeEvent(jsonEvent)

// 访问事件数据
println("Event Type: ${event.event}")
println("Body: ${event.body}")
```

## 错误处理

### 异常类型

我们定义了以下异常类型来处理序列化和反序列化过程中的错误：

```kotlin
/**
 * 序列化异常基类
 */
open class SerializationException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    val errorCode: String = "OMNI_SERIALIZATION_ERROR"
}

/**
 * 反序列化异常
 */
class DeserializationException(message: String, cause: Throwable? = null) : SerializationException(message, cause) {
    override val errorCode: String = "OMNI_DESERIALIZATION_ERROR"
}

/**
 * JSON解析异常
 */
class JsonParsingException(message: String, cause: Throwable? = null) : DeserializationException(message, cause) {
    override val errorCode: String = "OMNI_JSON_PARSING_ERROR"
}

/**
 * 无效消息格式异常
 */
class InvalidMessageFormatException(message: String, cause: Throwable? = null) : SerializationException(message, cause) {
    override val errorCode: String = "OMNI_INVALID_MESSAGE_FORMAT"
}
```

### 异常处理策略

在序列化和反序列化过程中，我们采用以下异常处理策略：

1. **详细的错误信息**：提供包含上下文的详细错误信息，帮助调试
2. **错误代码**：为不同类型的错误分配唯一的错误代码
3. **原始异常保留**：保留原始异常作为原因，便于跟踪根本原因
4. **日志记录**：在抛出异常前记录详细的错误日志
5. **恢复机制**：对于某些非致命错误，尝试提供恢复机制

```kotlin
// 在序列化器中增强错误处理
fun deserializeResponse(responseString: String): OmniSharpResponse {
    try {
        // 验证JSON格式
        if (!isValidJson(responseString)) {
            throw InvalidMessageFormatException("Invalid JSON format: $responseString")
        }
        
        // 尝试反序列化
        return objectMapper.readValue(responseString, OmniSharpResponse::class.java)
    } catch (e: InvalidMessageFormatException) {
        // 记录错误日志
        Logger.getInstance(javaClass).error("Invalid message format: ${e.errorCode}", e)
        throw e
    } catch (e: JsonParseException) {
        val parsingException = JsonParsingException("JSON parsing failed", e)
        Logger.getInstance(javaClass).error("JSON parsing error: ${parsingException.errorCode}", parsingException)
        throw parsingException
    } catch (e: Exception) {
        val deserializeException = DeserializationException("Failed to deserialize response", e)
        Logger.getInstance(javaClass).error("Deserialization error: ${deserializeException.errorCode}", deserializeException)
        throw deserializeException
    }
}

// 验证JSON格式的辅助方法
private fun isValidJson(json: String): Boolean {
    return try {
        objectMapper.readTree(json)
        true
    } catch (e: Exception) {
        false
    }
}
```

## 性能优化

### 1. 缓存配置

```kotlin
// 优化objectMapper配置，提高性能
private val objectMapper = ObjectMapper().apply {
    // 禁用不必要的功能
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    
    // 启用快速序列化
    enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
    enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
    
    // 设置序列化包含策略
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    
    // 注册必要的模块
    registerModule(KotlinModule())
}
```

### 2. 预编译JSON Schema

对于频繁使用的请求和响应类型，可以考虑预编译JSON Schema以提高性能：

```kotlin
// 使用Jackson的ObjectReader和ObjectWriter进行预编译优化
private val requestWriter = objectMapper.writerFor(OmniSharpRequest::class.java)
private val responseReader = objectMapper.readerFor(OmniSharpResponse::class.java)
private val eventReader = objectMapper.readerFor(OmniSharpEvent::class.java)

// 在方法中使用预编译的reader和writer
override fun serializeRequest(request: OmniSharpRequest): String {
    return requestWriter.writeValueAsString(request)
}

override fun deserializeResponse(responseString: String): OmniSharpResponse {
    return responseReader.readValue(responseString)
}
```

### 3. 批处理序列化

对于批量请求，可以实现批量序列化功能：

```kotlin
/**
 * 批量序列化请求
 */
fun serializeRequests(requests: List<OmniSharpRequest>): List<String> {
    return requests.map { serializeRequest(it) }
}

/**
 * 批量反序列化响应
 */
fun deserializeResponses(responseStrings: List<String>): List<OmniSharpResponse> {
    return responseStrings.map { deserializeResponse(it) }
}
```

### 4. 内存优化

```kotlin
// 使用StringBuilder优化字符串操作
fun serializeRequestOptimized(request: OmniSharpRequest): String {
    val builder = StringBuilder()
    builder.append("{\"")
    builder.append("seq\":${request.seq},\"")
    builder.append("type\":\"${request.type}\",\"")
    builder.append("command\":\"${request.command}\",\"")
    builder.append("arguments\":{")
    
    // 处理arguments
    val args = request.arguments.entries.joinToString(",") {
        "\"${it.key}\":${formatValue(it.value)}"
    }
    builder.append(args)
    
    builder.append("}}")
    return builder.toString()
}

// 格式化值的辅助方法
private fun formatValue(value: Any): String {
    return when (value) {
        is String -> "\"${escapeJson(value)}\""
        is Number, is Boolean -> value.toString()
        else -> serialize(value)
    }
}

// JSON转义辅助方法
private fun escapeJson(input: String): String {
    return input.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}
```

## 后续步骤

1. 设计和实现Stdio通信管道
2. 实现请求-响应模式和事件处理机制
3. 编写通信协议的错误处理和重试机制
4. 创建通信协议的单元测试和集成测试
5. 编写通信协议实现文档，包含代码示例和流程图

本实现提供了OmniSharp通信协议中JSON消息序列化和反序列化的核心功能，为后续的Stdio通信管道实现奠定了基础。