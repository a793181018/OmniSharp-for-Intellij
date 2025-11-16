# OmniSharp通信协议 - 单元测试和集成测试设计

## 目录

1. [概述](#概述)
2. [测试策略](#测试策略)
3. [单元测试设计](#单元测试设计)
4. [集成测试设计](#集成测试设计)
5. [测试数据](#测试数据)
6. [测试工具和环境](#测试工具和环境)
7. [测试覆盖率要求](#测试覆盖率要求)
8. [CI/CD集成](#cicd集成)
9. [实现示例](#实现示例)
10. [后续步骤](#后续步骤)

## 概述

本文档详细描述OmniSharp通信协议的测试策略和实现方案。测试是确保通信协议可靠性和稳定性的关键环节，通过全面的单元测试和集成测试，可以验证各个组件的功能正确性、错误处理能力和边界条件处理能力。

### 测试目标

- 验证通信协议各组件的功能正确性
- 测试错误处理和重试机制的有效性
- 验证通信协议在各种边界条件下的行为
- 确保通信协议的性能符合要求
- 提供可重复的测试用例，支持CI/CD流程

## 测试策略

### 测试级别

1. **单元测试**：测试单个组件的功能，隔离其他依赖
2. **集成测试**：测试组件间的交互和协作
3. **端到端测试**：测试完整的通信流程，从请求发送到响应处理

### 测试覆盖范围

- **功能测试**：验证正常情况下的功能正确性
- **异常测试**：验证异常情况和错误处理
- **边界测试**：验证边界条件下的行为
- **性能测试**：验证在高负载情况下的性能表现
- **稳定性测试**：验证长时间运行的稳定性

## 单元测试设计

### 1. 消息序列化/反序列化测试

#### 测试类：`MessageSerializerTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testSerializeOmniSharpRequest` | 测试请求序列化 | 正确序列化请求对象为JSON字符串 |
| `testDeserializeOmniSharpRequest` | 测试请求反序列化 | 正确从JSON字符串反序列化为请求对象 |
| `testSerializeOmniSharpResponse` | 测试响应序列化 | 正确序列化响应对象为JSON字符串 |
| `testDeserializeOmniSharpResponse` | 测试响应反序列化 | 正确从JSON字符串反序列化为响应对象 |
| `testSerializeOmniSharpEvent` | 测试事件序列化 | 正确序列化事件对象为JSON字符串 |
| `testDeserializeOmniSharpEvent` | 测试事件反序列化 | 正确从JSON字符串反序列化为事件对象 |
| `testSerializeComplexArguments` | 测试复杂参数序列化 | 正确序列化包含嵌套对象的参数 |
| `testDeserializeComplexArguments` | 测试复杂参数反序列化 | 正确反序列化包含嵌套对象的参数 |
| `testSerializeWithSpecialCharacters` | 测试特殊字符序列化 | 正确处理包含特殊字符的消息 |
| `testDeserializeInvalidJson` | 测试无效JSON反序列化 | 抛出正确的异常 |
| `testDeserializeMalformedMessage` | 测试格式错误消息反序列化 | 抛出正确的异常 |

### 2. Stdio通道测试

#### 测试类：`StdioChannelTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testWriteMessage` | 测试写入消息 | 消息被正确写入输出流 |
| `testReadMessage` | 测试读取消息 | 能正确从输入流读取消息 |
| `testReadMultipleMessages` | 测试读取多条消息 | 能正确读取连续的多条消息 |
| `testWriteReadIntegration` | 测试写入读取集成 | 写入的消息能被正确读取 |
| `testChannelClose` | 测试通道关闭 | 通道正确关闭并释放资源 |
| `testReadWithTimeout` | 测试读取超时 | 在指定时间后抛出超时异常 |
| `testWriteToClosedChannel` | 测试向已关闭通道写入 | 抛出正确的异常 |
| `testReadFromClosedChannel` | 测试从已关闭通道读取 | 抛出正确的异常 |
| `testChannelErrorHandling` | 测试通道错误处理 | 正确捕获和处理IO异常 |

### 3. 请求-响应模式测试

#### 测试类：`RequestTrackerTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testTrackRequest` | 测试跟踪请求 | 请求被正确添加到跟踪器 |
| `testGetRequest` | 测试获取请求 | 能正确获取已跟踪的请求 |
| `testCompleteRequest` | 测试完成请求 | 请求被正确完成并移除 |
| `testCancelRequest` | 测试取消请求 | 请求被正确取消并移除 |
| `testMultipleRequests` | 测试多个请求 | 能同时跟踪多个请求 |
| `testRequestTimeout` | 测试请求超时 | 请求在超时后被正确处理 |
| `testNonExistentRequest` | 测试不存在的请求 | 对不存在的请求操作返回正确结果 |
| `testRequestLimit` | 测试请求数量限制 | 达到限制时抛出正确的异常 |

#### 测试类：`EventDispatcherTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testRegisterListener` | 测试注册监听器 | 监听器被正确注册 |
| `testUnregisterListener` | 测试取消注册监听器 | 监听器被正确移除 |
| `testDispatchEvent` | 测试分发事件 | 事件被正确分发给所有注册的监听器 |
| `testDispatchUnknownEvent` | 测试分发未知事件 | 没有监听器被调用，不抛出异常 |
| `testMultipleListeners` | 测试多个监听器 | 事件被分发给所有监听器 |
| `testListenerExceptionHandling` | 测试监听器异常处理 | 单个监听器异常不影响其他监听器 |
| `testRegisterNullListener` | 测试注册空监听器 | 抛出正确的异常 |

### 4. 通信管理器测试

#### 测试类：`OmniSharpCommunicatorTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testInitialize` | 测试初始化 | 通信器被正确初始化 |
| `testSendRequest` | 测试发送请求 | 请求被正确发送并返回响应 |
| `testSendRequestWithTimeout` | 测试带超时的请求 | 请求在超时后被取消 |
| `testRegisterEventListener` | 测试注册事件监听器 | 事件监听器被正确注册 |
| `testHandleResponse` | 测试处理响应 | 响应被正确处理并匹配请求 |
| `testHandleEvent` | 测试处理事件 | 事件被正确分发 |
| `testShutdown` | 测试关闭 | 通信器被正确关闭并释放资源 |
| `testSendRequestBeforeInitialize` | 测试初始化前发送请求 | 抛出正确的异常 |
| `testSendRequestAfterShutdown` | 测试关闭后发送请求 | 抛出正确的异常 |

### 5. 错误处理和重试机制测试

#### 测试类：`ErrorHandlerTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testHandleError` | 测试处理错误 | 错误被正确处理 |
| `testIsRetryable` | 测试判断可重试性 | 正确判断不同类型错误的可重试性 |
| `testGetRetryStrategy` | 测试获取重试策略 | 返回正确的重试策略 |
| `testLogError` | 测试记录错误日志 | 错误日志被正确记录 |
| `testHandleTimeoutException` | 测试处理超时异常 | 超时异常被正确处理 |
| `testHandleChannelException` | 测试处理通道异常 | 通道异常被正确处理 |
| `testHandleMessageParseException` | 测试处理消息解析异常 | 消息解析异常被正确处理 |

#### 测试类：`RetryStrategyTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testExponentialBackoff` | 测试指数退避策略 | 返回的延迟时间符合指数增长 |
| `testFixedDelay` | 测试固定延迟策略 | 返回固定的延迟时间 |
| `testLinearBackoff` | 测试线性退避策略 | 返回的延迟时间线性增长 |
| `testJitterEffect` | 测试抖动效果 | 延迟时间有随机变化 |
| `testMaxRetries` | 测试最大重试次数 | 达到最大重试次数后不再重试 |
| `testMaxDelay` | 测试最大延迟时间 | 延迟时间不超过最大值 |
| `testCompositeStrategy` | 测试复合策略 | 复合策略行为符合预期 |

#### 测试类：`CircuitBreakerTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testInitialState` | 测试初始状态 | 断路器初始状态为CLOSED |
| `testSuccessDoesNotTripCircuit` | 测试成功不触发断路器 | 成功请求后断路器保持CLOSED状态 |
| `testFailuresTripCircuit` | 测试失败触发断路器 | 多次失败后断路器变为OPEN状态 |
| `testOpenCircuitRejectsRequests` | 测试开路状态拒绝请求 | 开路状态下请求被拒绝 |
| `testCircuitResetsAfterTimeout` | 测试超时后重置断路器 | 超时后断路器变为HALF_OPEN状态 |
| `testHalfOpenSuccessClosesCircuit` | 测试半开状态成功关闭断路器 | 半开状态下成功请求后断路器变为CLOSED |
| `testHalfOpenFailureOpensCircuit` | 测试半开状态失败打开断路器 | 半开状态下失败请求后断路器变为OPEN |
| `testManualReset` | 测试手动重置 | 手动重置后断路器变为CLOSED状态 |

## 集成测试设计

### 1. 通信流程集成测试

#### 测试类：`CommunicationFlowTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testCompleteCommunicationFlow` | 测试完整通信流程 | 从初始化到请求响应的完整流程正确执行 |
| `testMultipleConcurrentRequests` | 测试多个并发请求 | 能同时处理多个请求并正确返回响应 |
| `testEventHandlingDuringRequests` | 测试请求期间的事件处理 | 在处理请求的同时能正确处理事件 |
| `testLongRunningOperation` | 测试长时间运行的操作 | 能处理长时间运行的请求而不超时 |
| `testCommunicationWithComplexData` | 测试复杂数据通信 | 能正确处理包含复杂数据的请求和响应 |

### 2. 错误恢复集成测试

#### 测试类：`ErrorRecoveryTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testRetryOnTimeout` | 测试超时重试 | 请求超时后正确重试 |
| `testRetryOnConnectionError` | 测试连接错误重试 | 连接错误后正确重试 |
| `testRetryExhaustion` | 测试重试耗尽 | 达到最大重试次数后正确处理 |
| `testCircuitBreakerIntegration` | 测试断路器集成 | 断路器正确与重试机制集成 |
| `testServerRestartRecovery` | 测试服务器重启恢复 | 服务器重启后能自动恢复通信 |
| `testMultipleFailureTypes` | 测试多种失败类型 | 能正确处理不同类型的失败 |

### 3. 端到端测试

#### 测试类：`EndToEndTest`

**测试用例**：

| 测试用例 | 描述 | 预期结果 |
|---------|------|----------|
| `testInitializeAndSimpleCommand` | 测试初始化和简单命令 | 成功初始化并执行简单命令 |
| `testCodeCompletion` | 测试代码补全 | 成功获取代码补全结果 |
| `testGotoDefinition` | 测试转到定义 | 成功获取定义位置 |
| `testFindReferences` | 测试查找引用 | 成功获取引用列表 |
| `testDiagnosticsEvent` | 测试诊断事件 | 正确接收和处理诊断事件 |
| `testMultipleRequestsInSequence` | 测试按序执行多个请求 | 多个请求按顺序正确执行 |
| `testPerformanceUnderLoad` | 测试负载下的性能 | 在多个并发请求下保持性能稳定 |
| `testStabilityOverTime` | 测试长时间稳定性 | 长时间运行不出现资源泄漏或错误 |

## 测试数据

### 测试消息模板

```kotlin
// 请求模板
val requestTemplates = mapOf(
    "initialize" to OmniSharpRequest(
        command = "initialize",
        arguments = mapOf(
            "rootPath" to "/path/to/project",
            "capabilities" to mapOf<String, Any>()
        )
    ),
    "completion" to OmniSharpRequest(
        command = "completion",
        arguments = mapOf(
            "FileName" to "/path/to/file.cs",
            "Line" to 10,
            "Column" to 15
        )
    ),
    "gotoDefinition" to OmniSharpRequest(
        command = "gotoDefinition",
        arguments = mapOf(
            "FileName" to "/path/to/file.cs",
            "Line" to 10,
            "Column" to 15
        )
    )
)

// 响应模板
val responseTemplates = mapOf(
    "initialize" to OmniSharpResponse(
        request_seq = 1,
        success = true,
        command = "initialize",
        body = mapOf(
            "capabilities" to mapOf<String, Any>()
        )
    ),
    "completion" to OmniSharpResponse(
        request_seq = 2,
        success = true,
        command = "completion",
        body = mapOf(
            "CompletionItems" to listOf(
                mapOf(
                    "DisplayText" to "MethodName",
                    "InsertText" to "MethodName()"
                )
            )
        )
    )
)

// 事件模板
val eventTemplates = mapOf(
    "diagnostics" to OmniSharpEvent(
        event = "diagnostics",
        body = mapOf(
            "FileName" to "/path/to/file.cs",
            "Diagnostics" to listOf(
                mapOf(
                    "Severity" to "Warning",
                    "Message" to "Variable not used",
                    "Range" to mapOf(
                        "Start" to mapOf("Line" to 10, "Column" to 5),
                        "End" to mapOf("Line" to 10, "Column" to 10)
                    )
                )
            )
        )
    )
)
```

### 测试场景数据

```kotlin
// 错误场景数据
val errorScenarios = listOf(
    ErrorScenario(
        name = "ConnectionTimeout",
        error = SocketTimeoutException("Connection timed out"),
        isRetryable = true
    ),
    ErrorScenario(
        name = "MessageParseError",
        error = MessageParseException("Invalid JSON"),
        isRetryable = false
    ),
    ErrorScenario(
        name = "ServerError",
        error = RequestFailedException("Server error", "command", 1, "Internal server error"),
        isRetryable = true
    )
)

data class ErrorScenario(
    val name: String,
    val error: Throwable,
    val isRetryable: Boolean
)
```

## 测试工具和环境

### 测试框架

- **JUnit 5**: 单元测试和集成测试框架
- **Mockito**: 用于模拟依赖的框架
- **AssertJ**: 用于增强断言功能
- **TestContainers**: 用于集成测试中的容器化服务

### 测试环境设置

```kotlin
/**
 * 测试基类，提供通用的测试设置和工具方法
 */
open class OmniSharpTestBase {
    protected val logger = Logger.getInstance(javaClass)
    
    @BeforeEach
    fun setup() {
        // 测试前设置
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
    }
    
    @AfterEach
    fun cleanup() {
        // 测试后清理
    }
    
    /**
     * 创建模拟的进程输入输出流
     */
    protected fun createMockProcess(): Pair<Process, PipedInputStream, PipedOutputStream> {
        val testInputStream = PipedInputStream()
        val testOutputStream = PipedOutputStream(testInputStream)
        
        val mockProcess = mock(Process::class.java)
        `when`(mockProcess.inputStream).thenReturn(PipedInputStream(PipedOutputStream()))
        `when`(mockProcess.outputStream).thenReturn(testOutputStream)
        `when`(mockProcess.errorStream).thenReturn(PipedInputStream())
        
        return Triple(mockProcess, testInputStream, testOutputStream)
    }
    
    /**
     * 等待条件满足或超时
     */
    protected fun waitForCondition(condition: () -> Boolean, timeoutMs: Long = 5000): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            if (condition()) {
                return true
            }
            Thread.sleep(10)
        }
        return false
    }
}
```

## 测试覆盖率要求

- **语句覆盖率**：> 90%
- **分支覆盖率**：> 85%
- **方法覆盖率**：> 90%
- **类覆盖率**：> 95%

## CI/CD集成

### GitHub Actions 配置示例

```yaml
name: OmniSharp Communication Protocol Tests

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main, develop ]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
          distribution: 'adopt'
      
      - name: Cache Gradle packages
        uses: actions/cache@v2
        with:
          path: ~/.gradle/caches
          key: ${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}
          restore-keys: ${{ runner.os }}-gradle-
      
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew
      
      - name: Run tests
        run: ./gradlew test
      
      - name: Generate coverage report
        run: ./gradlew jacocoTestReport
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v2
        with:
          file: ./build/reports/jacoco/test/jacocoTestReport.xml
```

## 实现示例

### 单元测试实现示例

#### MessageSerializerTest.kt

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.assertEquals

class MessageSerializerTest : OmniSharpTestBase() {
    private val mapper = jacksonObjectMapper()
    private val serializer = JacksonMessageSerializer(mapper)
    
    @Test
    fun testSerializeOmniSharpRequest() {
        // Arrange
        val request = OmniSharpRequest(
            command = "testCommand",
            arguments = mapOf(
                "param1" to "value1",
                "param2" to 42
            ),
            seq = 1
        )
        
        // Act
        val jsonString = serializer.serialize(request)
        
        // Assert
        assertNotNull(jsonString)
        assertTrue(jsonString.contains("testCommand"))
        assertTrue(jsonString.contains("param1"))
        assertTrue(jsonString.contains("value1"))
        assertTrue(jsonString.contains("param2"))
        assertTrue(jsonString.contains("42"))
        assertTrue(jsonString.contains("\"seq\":1"))
    }
    
    @Test
    fun testDeserializeOmniSharpRequest() {
        // Arrange
        val jsonString = "{\"command\":\"testCommand\",\"arguments\":{\"param1\":\"value1\"},\"seq\":1}"
        
        // Act
        val request = serializer.deserializeRequest(jsonString)
        
        // Assert
        assertNotNull(request)
        assertEquals("testCommand", request.command)
        assertEquals(1, request.seq)
        assertEquals("value1", request.arguments["param1"])
    }
    
    @Test
    fun testDeserializeInvalidJson() {
        // Arrange
        val invalidJson = "{invalid json}"
        
        // Act & Assert
        assertThrows(MessageParseException::class.java) {
            serializer.deserializeRequest(invalidJson)
        }
    }
    
    @Test
    fun testSerializeWithSpecialCharacters() {
        // Arrange
        val request = OmniSharpRequest(
            command = "test",
            arguments = mapOf(
                "text" to "Special chars: \"'\\\n\t\r"
            )
        )
        
        // Act
        val jsonString = serializer.serialize(request)
        
        // Assert
        assertNotNull(jsonString)
        // 确保特殊字符被正确转义
        assertTrue(jsonString.contains("\\\""))  // 双引号应该被转义
        assertTrue(jsonString.contains("\\\\"))  // 反斜杠应该被转义
    }
}
```

#### RequestTrackerTest.kt

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.test.assertEquals
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RequestTrackerTest : OmniSharpTestBase() {
    private val requestTracker = RequestTracker()
    
    @Test
    fun testTrackRequest() {
        // Arrange
        val request = OmniSharpRequest(command = "test", seq = 1)
        
        // Act
        requestTracker.trackRequest(request)
        
        // Assert
        assertTrue(requestTracker.hasRequest(1))
    }
    
    @Test
    fun testGetRequest() {
        // Arrange
        val request = OmniSharpRequest(command = "test", seq = 1)
        requestTracker.trackRequest(request)
        
        // Act
        val retrievedRequest = requestTracker.getRequest(1)
        
        // Assert
        assertNotNull(retrievedRequest)
        assertEquals(request, retrievedRequest)
    }
    
    @Test
    fun testCompleteRequest() {
        // Arrange
        val request = OmniSharpRequest(command = "test", seq = 1)
        requestTracker.trackRequest(request)
        val response = OmniSharpResponse(request_seq = 1, success = true, command = "test")
        
        // Act
        val completedRequest = requestTracker.completeRequest(1, response)
        
        // Assert
        assertNotNull(completedRequest)
        assertEquals(request, completedRequest)
        assertFalse(requestTracker.hasRequest(1))
    }
    
    @Test
    fun testRequestTimeout() {
        // Arrange
        val request = OmniSharpRequest(command = "test", seq = 1)
        requestTracker.trackRequest(request, 100)  // 100ms超时
        
        // Act & Assert
        assertDoesNotThrow {
            waitForCondition({ !requestTracker.hasRequest(1) }, 200)
        }
    }
    
    @Test
    fun testNonExistentRequest() {
        // Act & Assert
        assertNull(requestTracker.getRequest(999))
        assertNull(requestTracker.completeRequest(999, OmniSharpResponse(request_seq = 999, success = true, command = "test")))
    }
}
```

### 集成测试实现示例

#### CommunicationFlowTest.kt

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals

class CommunicationFlowTest : OmniSharpTestBase() {
    @Test
    fun testCompleteCommunicationFlow() {
        // Arrange
        // 创建测试用的流
        val inputPipe = PipedInputStream()
        val outputPipe = PipedOutputStream(inputPipe)
        val responsePipe = PipedInputStream()
        val requestPipe = PipedOutputStream(responsePipe)
        
        // 创建模拟进程
        val mockProcess = mock(Process::class.java)
        `when`(mockProcess.inputStream).thenReturn(responsePipe)
        `when`(mockProcess.outputStream).thenReturn(outputPipe)
        `when`(mockProcess.errorStream).thenReturn(PipedInputStream())
        
        // 创建通信组件
        val stdioChannel = StdioChannel()
        val messageSerializer = JacksonMessageSerializer()
        val requestTracker = RequestTracker()
        val eventDispatcher = EventDispatcher()
        val communicator = OmniSharpCommunicator(stdioChannel, messageSerializer, requestTracker, eventDispatcher)
        
        // 初始化通信器
        communicator.initialize(mockProcess)
        
        // 准备模拟响应
        val mockResponse = OmniSharpResponse(
            request_seq = 1,
            success = true,
            command = "test",
            body = mapOf("result" to "success")
        )
        
        // 启动线程处理请求并发送响应
        Thread {
            try {
                // 读取请求
                val requestBytes = ByteArray(1024)
                val bytesRead = inputPipe.read(requestBytes)
                val requestJson = String(requestBytes, 0, bytesRead).trim()
                println("Received request: $requestJson")
                
                // 发送响应
                val responseJson = messageSerializer.serialize(mockResponse)
                requestPipe.write(responseJson.toByteArray())
                requestPipe.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        
        // Act
        val responseFuture = communicator.sendRequest("test", mapOf("param" to "value"))
        
        // Assert
        val response = responseFuture.get(2, TimeUnit.SECONDS)
        assertNotNull(response)
        assertEquals(true, response.success)
        assertEquals("success", response.body?.get("result"))
        
        // 清理
        communicator.shutdown()
    }
    
    @Test
    fun testMultipleConcurrentRequests() {
        // Arrange
        // 创建测试用的流
        val inputPipe = PipedInputStream()
        val outputPipe = PipedOutputStream(inputPipe)
        val responsePipe = PipedInputStream()
        val requestPipe = PipedOutputStream(responsePipe)
        
        // 创建模拟进程
        val mockProcess = mock(Process::class.java)
        `when`(mockProcess.inputStream).thenReturn(responsePipe)
        `when`(mockProcess.outputStream).thenReturn(outputPipe)
        `when`(mockProcess.errorStream).thenReturn(PipedInputStream())
        
        // 创建通信组件
        val stdioChannel = StdioChannel()
        val messageSerializer = JacksonMessageSerializer()
        val requestTracker = RequestTracker()
        val eventDispatcher = EventDispatcher()
        val communicator = OmniSharpCommunicator(stdioChannel, messageSerializer, requestTracker, eventDispatcher)
        
        // 初始化通信器
        communicator.initialize(mockProcess)
        
        // 启动线程处理多个请求
        Thread {
            try {
                for (i in 1..3) {
                    // 读取请求
                    val requestBytes = ByteArray(1024)
                    val bytesRead = inputPipe.read(requestBytes)
                    val requestJson = String(requestBytes, 0, bytesRead).trim()
                    println("Received request $i: $requestJson")
                    
                    // 发送相应的响应
                    val mockResponse = OmniSharpResponse(
                        request_seq = i,
                        success = true,
                        command = "test$i",
                        body = mapOf("result" to "success$i")
                    )
                    val responseJson = messageSerializer.serialize(mockResponse)
                    requestPipe.write(responseJson.toByteArray())
                    requestPipe.flush()
                    
                    // 短暂延迟模拟处理时间
                    Thread.sleep(50)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        
        // Act - 发送多个并发请求
        val future1 = communicator.sendRequest("test1", mapOf("param" to "value1"))
        val future2 = communicator.sendRequest("test2", mapOf("param" to "value2"))
        val future3 = communicator.sendRequest("test3", mapOf("param" to "value3"))
        
        // Assert
        val response1 = future1.get(2, TimeUnit.SECONDS)
        val response2 = future2.get(2, TimeUnit.SECONDS)
        val response3 = future3.get(2, TimeUnit.SECONDS)
        
        assertNotNull(response1)
        assertNotNull(response2)
        assertNotNull(response3)
        
        assertEquals("success1", response1.body?.get("result"))
        assertEquals("success2", response2.body?.get("result"))
        assertEquals("success3", response3.body?.get("result"))
        
        // 清理
        communicator.shutdown()
    }
}
```

#### ErrorRecoveryTest.kt

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlinx.coroutines.runBlocking
import kotlin.test.assertEquals

class ErrorRecoveryTest : OmniSharpTestBase() {
    @Test
    fun testRetryOnTimeout() {
        // Arrange
        // 创建模拟组件
        val mockStdioChannel = mock(IStdioChannel::class.java)
        // 第一次调用抛出超时异常，第二次调用成功
        `when`(mockStdioChannel.write(any())).thenThrow(SocketTimeoutException())
            .thenThrow(SocketTimeoutException())
            .thenAnswer { it.getArgument(0) as ByteArray }
        `when`(mockStdioChannel.read()).thenAnswer { 
            val response = OmniSharpResponse(
                request_seq = 1,
                success = true,
                command = "test"
            )
            JacksonMessageSerializer().serialize(response).toByteArray()
        }
        
        val messageSerializer = JacksonMessageSerializer()
        val requestTracker = RequestTracker()
        val eventDispatcher = EventDispatcher()
        
        // 创建带重试的通信器
        val baseCommunicator = OmniSharpCommunicator(mockStdioChannel, messageSerializer, requestTracker, eventDispatcher)
        val errorHandler = DefaultErrorHandler()
        val retryableCommunicator = RetryableCommunicator(baseCommunicator, errorHandler)
        
        // 初始化
        retryableCommunicator.initialize(mock(Process::class.java))
        
        // Act & Assert
        val response = retryableCommunicator.sendRequest("test", emptyMap()).get(5, TimeUnit.SECONDS)
        assertNotNull(response)
        assertTrue(response.success)
        
        // 验证重试被调用
        verify(mockStdioChannel, times(3)).write(any())
    }
    
    @Test
    fun testCircuitBreakerIntegration() {
        // Arrange
        // 创建模拟组件
        val mockStdioChannel = mock(IStdioChannel::class.java)
        // 连续失败多次
        `when`(mockStdioChannel.write(any())).thenThrow(IOException("Connection reset"))
        
        val messageSerializer = JacksonMessageSerializer()
        val requestTracker = RequestTracker()
        val eventDispatcher = EventDispatcher()
        
        // 创建带重试和断路器的通信器
        val baseCommunicator = OmniSharpCommunicator(mockStdioChannel, messageSerializer, requestTracker, eventDispatcher)
        val errorHandler = DefaultErrorHandler()
        val circuitBreaker = CircuitBreaker(failureThreshold = 2)  // 低阈值方便测试
        val retryableCommunicator = RetryableCommunicator(baseCommunicator, errorHandler, null, circuitBreaker)
        
        // 初始化
        retryableCommunicator.initialize(mock(Process::class.java))
        
        // Act & Assert
        // 前两次请求应该触发断路器
        assertThrows(Exception::class.java) {
            retryableCommunicator.sendRequest("test1", emptyMap()).get(1, TimeUnit.SECONDS)
        }
        
        assertThrows(Exception::class.java) {
            retryableCommunicator.sendRequest("test2", emptyMap()).get(1, TimeUnit.SECONDS)
        }
        
        // 第三次请求应该被断路器拒绝
        assertThrows(Exception::class.java) {
            retryableCommunicator.sendRequest("test3", emptyMap()).get(1, TimeUnit.SECONDS)
        }
        
        // 验证断路器状态
        assertEquals(CircuitState.OPEN, circuitBreaker.getState())
    }
}
```

### 端到端测试实现示例

#### EndToEndTest.kt

```kotlin
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Assumptions.*
import kotlin.test.assertEquals
import java.io.File

class EndToEndTest : OmniSharpTestBase() {
    private val omniSharpPath = "path/to/omnisharp/OmniSharp.exe"
    private val testProjectPath = "path/to/test/project"
    
    @BeforeEach
    fun checkEnvironment() {
        // 确保OmniSharp可执行文件存在
        assumeTrue(File(omniSharpPath).exists(), "OmniSharp executable not found")
        
        // 确保测试项目存在
        assumeTrue(File(testProjectPath).exists(), "Test project not found")
    }
    
    @Test
    fun testInitializeAndSimpleCommand() {
        // Arrange
        val serverManager = OmniSharpServerManagerImpl()
        
        // 启动服务器
        serverManager.start(testProjectPath)
        
        // 等待服务器启动
        assertTrue(waitForCondition({ serverManager.isServerRunning }, 10000), "Server failed to start")
        
        try {
            // 创建通信组件
            val stdioChannel = StdioChannel()
            val messageSerializer = JacksonMessageSerializer()
            val requestTracker = RequestTracker()
            val eventDispatcher = EventDispatcher()
            val communicator = OmniSharpCommunicator(stdioChannel, messageSerializer, requestTracker, eventDispatcher)
            
            // 初始化通信器
            val process = serverManager.getServerProcess()
            assertNotNull(process)
            communicator.initialize(process)
            
            // Act - 发送初始化请求
            val initResponse = communicator.sendRequest(
                "initialize",
                mapOf(
                    "rootPath" to testProjectPath,
                    "capabilities" to mapOf<String, Any>()
                )
            ).get(5, TimeUnit.SECONDS)
            
            // Assert - 验证初始化成功
            assertNotNull(initResponse)
            assertTrue(initResponse.success)
            
            // Act - 发送简单命令
            val projectsResponse = communicator.sendRequest(
                "projects",
                emptyMap()
            ).get(5, TimeUnit.SECONDS)
            
            // Assert - 验证命令成功
            assertNotNull(projectsResponse)
            assertTrue(projectsResponse.success)
            assertNotNull(projectsResponse.body)
        } finally {
            // 清理
            serverManager.stop()
        }
    }
    
    @Test
    fun testCodeCompletion() {
        // Arrange
        val serverManager = OmniSharpServerManagerImpl()
        
        // 启动服务器
        serverManager.start(testProjectPath)
        
        // 等待服务器启动
        assertTrue(waitForCondition({ serverManager.isServerRunning }, 10000), "Server failed to start")
        
        try {
            // 创建通信组件
            val stdioChannel = StdioChannel()
            val messageSerializer = JacksonMessageSerializer()
            val requestTracker = RequestTracker()
            val eventDispatcher = EventDispatcher()
            val communicator = OmniSharpCommunicator(stdioChannel, messageSerializer, requestTracker, eventDispatcher)
            
            // 初始化通信器
            val process = serverManager.getServerProcess()
            assertNotNull(process)
            communicator.initialize(process)
            
            // 发送初始化请求
            communicator.sendRequest(
                "initialize",
                mapOf(
                    "rootPath" to testProjectPath,
                    "capabilities" to mapOf<String, Any>()
                )
            ).get(5, TimeUnit.SECONDS)
            
            // 获取测试文件路径
            val testFilePath = File(testProjectPath, "Program.cs").absolutePath
            assumeTrue(File(testFilePath).exists(), "Test file not found")
            
            // Act - 发送代码补全请求
            val completionResponse = communicator.sendRequest(
                "completion",
                mapOf(
                    "FileName" to testFilePath,
                    "Line" to 10,  // 假设在第10行有可补全的代码
                    "Column" to 5  // 假设在第5列有可补全的代码
                )
            ).get(5, TimeUnit.SECONDS)
            
            // Assert - 验证补全结果
            assertNotNull(completionResponse)
            assertTrue(completionResponse.success)
            val completionItems = completionResponse.body?.get("CompletionItems") as? List<*>
            assertNotNull(completionItems)
        } finally {
            // 清理
            serverManager.stop()
        }
    }
}
```

## 后续步骤

1. 编写通信协议实现文档，包含代码示例和流程图

通过全面的单元测试和集成测试，可以确保OmniSharp通信协议的各个组件都能正确工作，并且在各种场景下都能表现出预期的行为。测试覆盖了功能测试、异常测试、边界测试和性能测试等多个方面，提供了充分的验证和保障。