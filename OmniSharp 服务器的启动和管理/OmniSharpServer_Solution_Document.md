# OmniSharp服务器启动和管理解决方案文档

## 目录

1. [概述](#概述)
2. [系统架构](#系统架构)
3. [核心组件设计](#核心组件设计)
4. [进程管理和通信](#进程管理和通信)
5. [配置管理](#配置管理)
6. [错误处理和日志记录](#错误处理和日志记录)
7. [系统流程](#系统流程)
8. [部署和扩展](#部署和扩展)
9. [总结](#总结)

## 概述

本文档提供了OmniSharp-for-Intellij插件中OmniSharp服务器启动和管理功能的综合解决方案。OmniSharp是一个用于C#开发的跨平台Roslyn语言服务器实现，本解决方案旨在实现对OmniSharp服务器的完整生命周期管理，包括启动、通信、监控和关闭等功能。

### 解决方案目标

- 提供OmniSharp服务器的自动启动和管理功能
- 实现与OmniSharp服务器的高效通信机制
- 支持灵活的配置选项，满足不同用户需求
- 提供健壮的错误处理和日志记录机制
- 确保系统的稳定性和可靠性

### 技术栈

- Kotlin - 主要开发语言
- IntelliJ Platform SDK - 插件开发框架
- Gradle - 构建系统
- OmniSharp Server - C#语言服务器
- JSON - 通信数据格式

## 系统架构

### 高层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         IntelliJ IDEA 平台                             │
├─────────┬─────────────────────────────────────┬─────────────────────────┤
│  用户界面 │                                   │  事件和生命周期管理      │
└─────────┴───────────┬─────────────────────────┴─────────┬───────────────┘
                      │                                   │
                      ▼                                   ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         OmniSharp 服务层                                 │
├────────────────────┬─────────────────────────────────────┬──────────────┐
│ IOmniSharpServerManager │ IOmniSharpProcessManager   │ IOmniSharpConfiguration │
└────────────────────┴─────────────────┬─────────────────┴──────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         通信和错误处理层                                 │
├────────────────────┬─────────────────────────────────────┬──────────────┐
│ IOmniSharpCommunicator │ IOmniSharpExceptionHandler  │ IOmniSharpLogManager │
└────────────────────┴─────────────────┬─────────────────┴──────────────┘
                                       │
                                       ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         外部OmniSharp进程                                │
├─────────────────────────────────────────────────────────────────────────┤
│                   OmniSharp.Server (Roslyn Language Server)             │
└─────────────────────────────────────────────────────────────────────────┘
```

### 组件层次关系

系统采用分层架构设计，主要包括以下几层：

1. **用户界面层**：与用户交互的UI组件，包括工具窗口、配置界面等
2. **服务层**：核心服务组件，负责服务器的管理、进程控制和配置管理
3. **通信和错误处理层**：负责与OmniSharp服务器的通信，以及错误处理和日志记录
4. **外部进程层**：实际运行的OmniSharp服务器进程

### 主要数据流

- **启动流程**：配置 → 进程启动 → 初始化通信 → 状态更新
- **通信流程**：请求构建 → 序列化 → 发送 → 接收 → 反序列化 → 响应处理
- **错误处理流程**：异常捕获 → 错误分类 → 日志记录 → 用户通知

## 核心组件设计

### 核心接口设计

#### 1. IOmniSharpServerManager

```kotlin
interface IOmniSharpServerManager {
    val serverStatus: ServerStatus
    fun startServer(): CompletableFuture<Boolean>
    fun stopServer(): CompletableFuture<Boolean>
    fun restartServer(): CompletableFuture<Boolean>
    fun ping(): CompletableFuture<Boolean>
    fun sendRequest(request: OmniSharpRequest): CompletableFuture<OmniSharpResponse>
    fun addServerStatusListener(listener: ServerStatusListener)
    fun removeServerStatusListener(listener: ServerStatusListener)
    fun shutdown()
}
```

#### 2. IOmniSharpProcessManager

```kotlin
interface IOmniSharpProcessManager {
    val isProcessRunning: Boolean
    val process: Process?
    val processId: Long?
    fun startProcess(config: OmniSharpConfiguration): Process
    fun stopProcess(graceful: Boolean = true)
    fun destroyProcess()
    fun waitForProcess(timeoutMs: Long? = null): Int
    fun getProcessOutput(): String
    fun getProcessError(): String
    fun addProcessListener(listener: ProcessListener)
    fun removeProcessListener(listener: ProcessListener)
}
```

#### 3. IOmniSharpCommunicator

```kotlin
interface IOmniSharpCommunicator {
    val isConnected: Boolean
    fun connect(process: Process): CompletableFuture<Boolean>
    fun disconnect()
    fun sendMessage(message: String): CompletableFuture<String>
    fun sendRequest(request: OmniSharpRequest): CompletableFuture<OmniSharpResponse>
    fun addMessageListener(listener: MessageListener)
    fun removeMessageListener(listener: MessageListener)
}
```

#### 4. IOmniSharpConfiguration

```kotlin
interface IOmniSharpConfiguration {
    val serverPath: String
    val workingDirectory: String
    val arguments: List<String>
    val environmentVariables: Map<String, String>
    val startupTimeoutMs: Long
    val requestTimeoutMs: Long
    val maxRestartAttempts: Int
    fun validate(): ValidationResult
    fun save()
    fun load()
}
```

### 类图

```
┌───────────────────┐         ┌─────────────────────┐         ┌─────────────────────┐
│IOmniSharpServerMgr│◄───────┐│IOmniSharpProcessMgr │◄───────┐│IOmniSharpCommunicator│
└─────────┬─────────┘         └───────────┬─────────┘         └───────────┬─────────┘
          │                               │                               │
          ▼                               ▼                               ▼
┌───────────────────┐         ┌─────────────────────┐         ┌─────────────────────┐
│OmniSharpServerMgr │────────>│OmniSharpProcessMgr  │────────>│OmniSharpCommunicator│
│      Impl         │         │        Impl         │         │         Impl        │
└─────────┬─────────┘         └───────────┬─────────┘         └───────────┬─────────┘
          │                               │                               │
          │                               │                               ▼
          │                               ▼                       ┌─────────────────────┐
          │                    ┌─────────────────────┐           │OmniSharpMessageParser│
          │                    │ProcessBuilder       │           └─────────────────────┘
          │                    └─────────────────────┘
          ▼
┌───────────────────┐         ┌─────────────────────┐
│IOmniSharpConfig   │◄───────┐│OmniSharpException   │
└─────────┬─────────┘         └───────────┬─────────┘
          │                               │
          ▼                               ▼
┌───────────────────┐         ┌─────────────────────┐
│OmniSharpConfig    │         │OmniSharpException   │
│      Impl         │         │        Handler      │
└───────────────────┘         └─────────────────────┘
```

### 核心模型类

1. **OmniSharpRequest**：请求模型
2. **OmniSharpResponse**：响应模型
3. **ServerStatus**：服务器状态枚举
4. **OmniSharpConfiguration**：配置模型
5. **OmniSharpException**：异常基类

## 进程管理和通信

### 进程管理流程

#### 启动流程

1. **前置检查**
   - 验证OmniSharp服务器可执行文件路径
   - 检查工作目录
   - 验证配置参数

2. **进程启动**
   - 使用ProcessBuilder创建进程
   - 设置环境变量和工作目录
   - 重定向标准输入/输出/错误流

3. **进程监控**
   - 启动线程监控进程状态
   - 监听输出流和错误流
   - 捕获进程终止事件

4. **错误处理**
   - 处理进程启动失败
   - 监控进程意外终止
   - 实现自动重启机制

#### 终止流程

1. **优雅终止**
   - 发送关闭信号
   - 等待进程正常退出

2. **强制终止**
   - 超时后强制终止进程
   - 清理资源

### 通信协议

OmniSharp服务器使用Stdio通信协议，基于JSON格式进行消息交换。

#### 消息格式

```json
// 请求格式
{
  "Type": "request",
  "Seq": 1,
  "Command": "<command_name>",
  "Arguments": { /* 命令特定参数 */ }
}

// 响应格式
{
  "Type": "response",
  "Seq": 1,
  "Command": "<command_name>",
  "Request_seq": 1,
  "Body": { /* 响应数据 */ },
  "Running": true,
  "Success": true
}

// 事件格式
{
  "Type": "event",
  "Seq": 1,
  "Event": "<event_name>",
  "Body": { /* 事件数据 */ }
}
```

#### 通信流程图

```
┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
│  请求构建与序列化   │───────►│     消息发送        │───────►│   OmniSharp服务器   │
└─────────────────────┘        └─────────────────────┘        └────────┬────────────┘
                                                                        │
                                                                        │
                                                                        ▼
┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
│  响应处理与回调     │◄───────┤     消息接收        │◄───────┤     消息解析        │
└─────────────────────┘        └─────────────────────┘        └─────────────────────┘
```

## 配置管理

### 配置存储

系统支持两级配置：

1. **应用级配置**：
   - 存储在IDE配置目录
   - 适用于所有项目

2. **项目级配置**：
   - 存储在项目目录下的.omnisharp/config.json
   - 针对特定项目的配置

### 配置项

- **serverPath**：OmniSharp服务器路径
- **workingDirectory**：工作目录
- **arguments**：命令行参数
- **environmentVariables**：环境变量
- **startupTimeoutMs**：启动超时时间
- **requestTimeoutMs**：请求超时时间
- **maxRestartAttempts**：最大重启尝试次数
- **logLevel**：日志级别

### 配置验证

配置系统实现了完整的验证逻辑，确保配置的有效性和一致性。

```kotlin
data class ValidationResult(val isValid: Boolean, val errorMessage: String? = null)

fun validate(): ValidationResult {
    // 验证服务器路径
    if (!File(serverPath).exists()) {
        return ValidationResult(false, "OmniSharp server executable not found")
    }
    
    // 验证工作目录
    if (!File(workingDirectory).isDirectory) {
        return ValidationResult(false, "Working directory is not valid")
    }
    
    // 验证超时设置
    if (startupTimeoutMs <= 0) {
        return ValidationResult(false, "Startup timeout must be positive")
    }
    
    return ValidationResult(true)
}
```

## 错误处理和日志记录

### 错误处理机制

系统实现了完整的错误处理机制，包括：

1. **自定义异常体系**：
   - OmniSharpException（基类）
   - OmniSharpConfigurationException（配置错误）
   - OmniSharpServerStartException（启动错误）
   - OmniSharpCommunicationException（通信错误）
   - OmniSharpTimeoutException（超时错误）

2. **错误代码系统**：
   - 分类错误代码（配置、服务器、通信等）
   - 严重程度分级（关键、高、中、低）

3. **全局异常处理器**：
   - 捕获和处理异常
   - 根据错误类型采取适当行动
   - 向用户提供友好的错误反馈

### 日志记录

1. **日志级别**：
   - ERROR
   - WARN
   - INFO
   - DEBUG
   - TRACE

2. **日志功能**：
   - 标准日志记录
   - 服务器输出重定向
   - 日志查看器UI
   - 日志导出功能

3. **日志存储**：
   - 内存缓存
   - 文件存储
   - 限制日志大小，防止内存溢出

## 系统流程

### 服务器启动流程

```
startServer()
    │
    ▼
validateConfiguration()
    │
    ├─┐ invalid
    │ ▼
    │ return false
    │
    ▼ valid
stopExistingServer()
    │
    ▼
createProcessBuilder()
    │
    ▼
startProcess()
    │
    ├─┐ error
    │ ▼
    │ handleStartError()
    │ return false
    │
    ▼ success
setupCommunication()
    │
    ▼
waitForInitialization()
    │
    ├─┐ timeout
    │ ▼
    │ handleTimeout()
    │ return false
    │
    ▼ initialized
updateServerStatus(RUNNING)
    │
    ▼
return true
```

### 消息发送流程

```
sendRequest(request)
    │
    ▼
checkServerStatus()
    │
    ├─┐ not running
    │ ▼
    │ startServer()
    │
    ▼
createRequestMessage()
    │
    ▼
serializeMessage()
    │
    ▼
createResponseFuture()
    │
    ▼
sendMessageToServer()
    │
    ▼
waitForResponse()
    │
    ├─┐ timeout
    │ ▼
    │ completeExceptionally()
    │
    ▼ response received
parseResponse()
    │
    ▼
completeFutureWithResult()
    │
    ▼
return future
```

### 项目打开流程

```
projectOpened()
    │
    ▼
loadProjectConfiguration()
    │
    ▼
initializeServices()
    │
    ▼
registerProjectListeners()
    │
    ├─┐ autoStart enabled
    │ ▼
    │ startServerAsync()
    │
    ▼
showWelcomeNotification()
```

## 部署和扩展

### 依赖管理

- IntelliJ Platform SDK
- Kotlin标准库
- JSON序列化库

### 扩展点

1. **服务器管理器扩展**：自定义服务器管理逻辑
2. **进程管理器扩展**：自定义进程控制行为
3. **通信器扩展**：支持其他通信协议
4. **配置提供程序扩展**：支持其他配置源
5. **异常处理器扩展**：自定义异常处理逻辑

### 测试策略

1. **单元测试**：测试核心组件的功能
2. **集成测试**：测试组件间的交互
3. **端到端测试**：测试完整的服务器启动和通信流程

## 总结

本文档详细介绍了OmniSharp服务器启动和管理的完整解决方案。该方案通过分层架构设计，实现了服务器的自动启动、通信管理、配置控制和错误处理等核心功能。系统采用面向接口的设计原则，提供了良好的扩展性和可维护性。

主要特点包括：

- 完整的生命周期管理
- 高效的通信机制
- 灵活的配置选项
- 健壮的错误处理
- 详细的日志记录
- 友好的用户反馈

该解决方案将大大提升OmniSharp-for-Intellij插件的功能和用户体验，为C#开发者提供更好的开发环境和工具支持。