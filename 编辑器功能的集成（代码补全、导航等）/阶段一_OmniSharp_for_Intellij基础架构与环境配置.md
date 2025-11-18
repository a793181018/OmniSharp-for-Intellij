# 阶段一：OmniSharp-for-Intellij 基础架构与环境配置

## 1. 项目概述

OmniSharp-for-Intellij 是一个为 IntelliJ 平台（如 IntelliJ IDEA、Rider、WebStorm 等）提供 C# 语言支持的插件。它通过集成 OmniSharp 服务器，为 C# 开发者提供丰富的编辑器功能，包括代码补全、导航、诊断和格式化等。

### 1.1 核心功能

- **代码补全**：智能的代码建议和自动完成
- **代码导航**：快速查找定义、引用、类型层次结构等
- **代码诊断**：实时错误检查和警告提示
- **代码格式化**：统一代码风格和格式
- **重构支持**：重命名、提取方法等自动化重构操作

### 1.2 设计理念

插件采用模块化设计，将各个功能组件解耦，便于维护和扩展。同时，通过高效的通信协议和缓存机制，确保在提供强大功能的同时保持良好的性能。

## 2. 系统架构

### 2.1 分层设计

OmniSharp-for-Intellij 采用清晰的分层架构，主要包含以下几层：

1. **IntelliJ 平台层**：与 IntelliJ 平台交互的接口和集成点
2. **功能模块层**：实现具体编辑器功能的模块（补全、导航、诊断、格式化等）
3. **服务层**：提供核心服务和业务逻辑的中间层
4. **通信协议层**：处理与 OmniSharp 服务器的通信
5. **OmniSharp 服务器层**：外部 OmniSharp 服务器，提供 C# 语言分析能力

```
┌─────────────────────────────────────────────────────┐
│                  IntelliJ 平台层                      │
├─────────────────────────────────────────────────────┤
│                  功能模块层                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐     │
│  │ 代码补全   │  │ 代码导航   │  │ 代码诊断   │     │
│  └────────────┘  └────────────┘  └────────────┘     │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐     │
│  │ 代码格式化 │  │ 重构支持   │  │ 其他功能   │     │
│  └────────────┘  └────────────┘  └────────────┘     │
├─────────────────────────────────────────────────────┤
│                  服务层                               │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐     │
│  │ 会话管理   │  │ 配置管理   │  │ 缓存管理   │     │
│  └────────────┘  └────────────┘  └────────────┘     │
├─────────────────────────────────────────────────────┤
│                  通信协议层                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐     │
│  │ 请求处理   │  │ 响应解析   │  │ 错误处理   │     │
│  └────────────┘  └────────────┘  └────────────┘     │
├─────────────────────────────────────────────────────┤
│               OmniSharp 服务器层                      │
└─────────────────────────────────────────────────────┘
```

### 2.2 组件关系

各组件之间通过明确的接口进行交互，保持低耦合高内聚的设计原则：

- **功能模块** 调用 **服务层** 提供的接口
- **服务层** 通过 **通信协议层** 与 OmniSharp 服务器交互
- **通信协议层** 负责序列化请求和反序列化响应
- **配置管理** 贯穿各个层次，提供统一的配置访问

## 3. 项目结构

OmniSharp-for-Intellij 项目采用标准的 Java/Kotlin 项目结构，源代码主要位于 `src/main/java` 目录下。以下是项目的主要包结构：

```
com.github.a793181018.omnisharpforintellij/
├── common/          # 通用工具类和常量
├── communicator/    # 与 OmniSharp 服务器通信的组件
├── config/          # 配置管理相关类
├── editor/          # 编辑器功能实现
│   ├── completion/  # 代码补全功能
│   ├── diagnostics/ # 代码诊断功能
│   ├── format/      # 代码格式化功能
│   ├── navigation/  # 代码导航功能
│   └── refactoring/ # 代码重构功能
├── integration/     # 与其他插件或系统的集成
├── notification/    # 通知和消息提示
├── server/          # OmniSharp 服务器管理
├── service/         # 核心服务实现
├── session/         # 会话管理
└── utils/           # 工具类
```

### 3.1 核心包说明

- **common**: 包含所有模块共用的工具类、常量定义和基础接口
- **communicator**: 实现与 OmniSharp 服务器通信的协议处理类
- **config**: 管理插件配置和设置
- **editor**: 包含所有编辑器功能的具体实现
- **server**: 管理 OmniSharp 服务器的生命周期、启动、停止和状态监控
- **service**: 提供核心业务逻辑和服务
- **session**: 管理项目会话和上下文信息

## 4. 环境配置

### 4.1 开发环境要求

- Java Development Kit (JDK) 21 或更高版本
- IntelliJ IDEA Ultimate 或 Community 版本（推荐最新版本）
- IntelliJ Platform Plugin SDK
- Gradle 7.0+（用于构建项目）
- Git（用于版本控制）

### 4.2 项目构建配置

项目使用 Gradle 进行构建和依赖管理。主要配置文件为 `build.gradle.kts`，包含以下关键配置：

```kotlin
// Java 版本配置
sourceCompatibility = JavaVersion.VERSION_21
targetCompatibility = JavaVersion.VERSION_21

// 依赖配置
dependencies {
    // Reactor 用于响应式编程
    implementation("io.projectreactor:reactor-core:3.6.5")
    implementation("io.projectreactor.netty:reactor-netty:1.1.18")
    
    // IntelliJ Platform 依赖
    implementation(platform("com.jetbrains.intellij.platform:platform-bom:2024.1.4"))
    implementation("com.jetbrains.intellij.platform:core")
    implementation("com.jetbrains.intellij.platform:ide-core")
    
    // 测试依赖
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}
```

### 4.3 插件配置

插件的基本配置位于 `plugin.xml` 文件中，定义了插件的 ID、名称、描述、版本、依赖等信息：

```xml
<idea-plugin>
  <id>com.github.a793181018.omnisharpforintellij</id>
  <name>OmniSharp-for-Intellij</name>
  <version>1.0-SNAPSHOT</version>
  <vendor email="your-email@example.com" url="https://github.com/your-username/OmniSharp-for-Intellij">Your Name</vendor>
  
  <description><![CDATA[
    OmniSharp integration for IntelliJ platform IDEs, providing C# language support.
  ]]></description>
  
  <depends>com.intellij.modules.platform</depends>
  <depends>com.intellij.modules.lang</depends>
  
  <!-- 插件扩展点和组件配置 -->
  <extensions defaultExtensionNs="com.intellij">
    <!-- 注册扩展点实现 -->
  </extensions>
  
  <!-- 应用组件 -->
  <application-components>
    <!-- 应用级组件 -->
  </application-components>
  
  <!-- 项目组件 -->
  <project-components>
    <!-- 项目级组件 -->
  </project-components>
</idea-plugin>
```

## 5. OmniSharp 服务器配置

### 5.1 服务器下载与安装

OmniSharp 插件会自动下载和管理 OmniSharp 服务器。服务器的下载和安装逻辑位于 `server` 包中。

### 5.2 服务器配置选项

OmniSharp 服务器可以通过 `omnisharp.json` 配置文件进行自定义配置。插件会在以下位置查找配置文件：

1. 项目根目录下的 `omnisharp.json`
2. 用户主目录下的 `.omnisharp/omnisharp.json`

基本的配置示例：

```json
{
  "FormattingOptions": {
    "OrganizeImports": true,
    "EnableEditorConfigSupport": true,
    "IndentationSize": 4,
    "TabSize": 4,
    "UseTabs": false
  },
  "RoslynExtensionsOptions": {
    "EnableAnalyzersSupport": true
  },
  "MsBuild": {
    "LoadProjectsOnDemand": true
  }
}
```

### 5.3 服务器通信协议

插件使用 Stdio 协议与 OmniSharp 服务器通信，通过标准输入输出流进行数据交换。通信的核心逻辑位于 `communicator` 包中。

## 6. 开发与调试

### 6.1 启动开发环境

1. 克隆项目代码：`git clone https://github.com/your-username/OmniSharp-for-Intellij.git`
2. 在 IntelliJ IDEA 中打开项目
3. 配置 IntelliJ Platform SDK
4. 运行 "Run Plugin" 配置，启动带有插件的 IDEA 实例

### 6.2 调试技巧

- 使用 IDEA 的内置调试器设置断点
- 查看 IDE 日志：Help | Show Log in Explorer
- 启用详细日志：在 Settings/Preferences | Languages & Frameworks | C# | OmniSharp 中启用 "Detailed logging"

## 7. 下一步

完成基础架构与环境配置后，可以继续学习以下阶段的内容：

1. **阶段二：代码补全功能集成** - 学习如何实现和配置代码补全功能
2. **阶段三：代码导航功能集成** - 学习如何实现代码导航相关功能
3. **阶段四：代码诊断与错误检查** - 学习如何实现实时错误检查和警告
4. **阶段五：代码格式化与风格管理** - 学习如何实现代码格式化功能

---

本文档是 OmniSharp-for-Intellij 编辑器功能集成指南的第一阶段，提供了项目的基础架构和环境配置信息。通过本阶段的学习，开发者应该能够了解项目的整体结构和配置方法，为后续的功能集成做好准备。