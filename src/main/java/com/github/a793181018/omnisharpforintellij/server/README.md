# OmniSharp 服务器管理模块

本模块提供了在 IntelliJ IDEA 插件中集成和管理 OmniSharp 服务器的功能。OmniSharp 是一个用于 C# 代码分析的服务器，本模块提供了与 OmniSharp 服务器通信的完整解决方案。

## 功能概述

- 服务器生命周期管理（启动、停止、重启）
- 配置管理与验证
- 进程管理与监控
- JSON-RPC 通信协议实现
- 异步请求/响应处理
- 事件订阅机制
- 响应式编程支持（基于 Reactor）
- 状态监控与变更通知

## 核心组件

### 模型类

- `ServerStatus` - 枚举服务器状态（NOT_STARTED、STARTING、RUNNING、STOPPING、STOPPED、FAILED）
- `OmniSharpRequest<T>` - 请求模型，包含命令和参数
- `OmniSharpResponse<T>` - 响应模型，包含响应数据和状态
- `OmniSharpEvent<T>` - 事件模型，用于处理服务器事件通知

### 异常类

- `OmniSharpException` - 基础异常类
- `OmniSharpServerStartupException` - 服务器启动异常
- `OmniSharpCommunicationException` - 通信异常
- `OmniSharpResponseException` - 响应异常
- `OmniSharpConfigurationException` - 配置异常

### 接口

- `IOmniSharpServerManager` - 服务器管理主接口
- `IOmniSharpProcessManager` - 进程管理接口
- `ProcessListener` - 进程事件监听器
- `IOmniSharpCommunication` - 通信接口
- `IOmniSharpConfiguration` - 配置接口

### 实现类

- `OmniSharpServerManagerImpl` - 服务器管理器实现
- `OmniSharpProcessManagerImpl` - 进程管理器实现
- `OmniSharpCommunicationImpl` - 通信实现
- `OmniSharpConfigurationImpl` - 配置实现（支持 IntelliJ 持久化）

## 使用示例

### 初始化和配置

```java
// 获取服务器管理器实例
IOmniSharpServerManager serverManager = OmniSharpServerManagerImpl.getInstance();

// 配置 OmniSharp 服务器（如果需要自定义配置）
IOmniSharpConfiguration configuration = getConfiguration(); // 获取配置实例
configuration.setServerPath("path/to/omnisharp");
configuration.setWorkingDirectory("path/to/project");
configuration.setServerArguments(Arrays.asList("--languageserver", "--hostPID", String.valueOf(ProcessHandle.current().pid())));
```

### 启动服务器

```java
// 启动服务器
boolean started = serverManager.start();
if (started) {
    try {
        // 等待服务器就绪
        serverManager.waitForServerReady(10000); // 10秒超时
        System.out.println("OmniSharp server started successfully");
    } catch (TimeoutException | InterruptedException e) {
        System.err.println("Failed to start OmniSharp server: " + e.getMessage());
        serverManager.stop();
    }
}
```

### 发送请求

```java
// 创建请求
Map<String, Object> arguments = new HashMap<>();
arguments.put("FileName", "path/to/file.cs");
arguments.put("Line", 10);
arguments.put("Column", 5);

OmniSharpRequest<Map<String, Object>> request = new OmniSharpRequest<>("gotodefinition", arguments, null);

// 同步发送请求
try {
    OmniSharpResponse<Map<String, Object>> response = serverManager.sendRequest(request)
            .get(5, TimeUnit.SECONDS);
    
    if (response.isSuccess()) {
        System.out.println("Response: " + response.getBody());
    } else {
        System.err.println("Request failed: " + response.getMessage());
    }
} catch (Exception e) {
    System.err.println("Error sending request: " + e.getMessage());
}

// 异步/响应式发送请求
serverManager.sendRequestReactive(request)
    .subscribe(
        response -> {
            if (response.isSuccess()) {
                System.out.println("Reactive response: " + response.getBody());
            }
        },
        error -> System.err.println("Reactive error: " + error.getMessage())
    );
```

### 事件订阅

```java
// 订阅日志事件
String subscriptionId = serverManager.subscribeToEvent("log", String.class, event -> {
    System.out.println("Log event: " + event.getBody());
});

// 稍后取消订阅
serverManager.unsubscribeFromEvent(subscriptionId);
```

### 状态变更监听

```java
// 添加状态变更监听器
serverManager.addStatusChangeListener(() -> {
    ServerStatus status = serverManager.getStatus();
    System.out.println("Server status changed to: " + status);
});
```

### 停止服务器

```java
// 停止服务器
boolean stopped = serverManager.stop();
if (stopped) {
    System.out.println("OmniSharp server stopped successfully");
} else {
    System.err.println("Failed to stop OmniSharp server");
}
```

## 配置选项

OmniSharp 服务器配置主要包括以下选项：

- `serverPath` - OmniSharp 可执行文件路径
- `workingDirectory` - 工作目录（通常是项目根目录）
- `serverArguments` - 启动参数列表
- `startupTimeoutMs` - 启动超时时间（毫秒）
- `shutdownTimeoutMs` - 关闭超时时间（毫秒）

## 故障排除

### 常见问题

1. **服务器启动失败**
   - 检查 OmniSharp 可执行文件路径是否正确
   - 确保有执行权限
   - 检查工作目录是否存在
   - 查看日志以获取详细错误信息

2. **通信错误**
   - 确保服务器已成功启动
   - 检查网络/进程管道连接
   - 验证请求格式是否正确

3. **超时问题**
   - 对于大型项目，可能需要增加启动超时时间
   - 检查系统资源使用情况

## 注意事项

1. 本模块使用了 IntelliJ 平台的服务机制，确保正确处理组件的生命周期
2. 通信基于 JSON-RPC 协议，需要正确格式化请求和响应
3. 对于性能敏感的场景，推荐使用响应式 API
4. 确保在插件卸载时正确停止服务器和释放资源

## 依赖

- IntelliJ Platform SDK
- Jackson (JSON 处理)
- Reactor (响应式编程)