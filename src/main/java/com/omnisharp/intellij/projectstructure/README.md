# OmniSharp Project File Parser 实现

## 项目概述

本模块实现了对.NET项目文件（.csproj、.fsproj、.vbproj）的解析和管理功能，支持SDK风格（.NET Core/.NET 5+）和传统风格（.NET Framework）项目文件的解析，并提供实时文件监控和项目生命周期管理。

## 实现架构

### 核心组件

1. **数据模型层**
   - 提供项目结构的内存表示

2. **解析器层**
   - 处理不同格式项目文件的解析

3. **监控器层**
   - 实时监控项目文件变更

4. **管理层**
   - 集成解析器和监控器，提供项目完整生命周期管理

## 数据模型类

### 主要模型类

- **ProjectModel** - 项目的核心模型，包含项目的所有信息
- **PackageReference** - NuGet包引用信息
- **ProjectReference** - 项目间引用信息
- **FileReference** - 外部文件（如.dll）引用信息
- **ProjectConfiguration** - 项目配置信息
- **ProjectLanguage** - 项目语言枚举

### ProjectModel 核心属性

```java
// 基本属性
private String name;            // 项目名称
private Path path;              // 项目文件路径
private Path directory;         // 项目目录
private boolean isSdkProject;   // 是否为SDK风格项目
private String projectType;     // 项目类型
private String targetFramework; // 目标框架
private String outputType;      // 输出类型

// 引用信息
private List<PackageReference> packageReferences;    // NuGet包引用
private List<ProjectReference> projectReferences;    // 项目引用
private List<FileReference> fileReferences;          // 文件引用

// 编译信息
private List<String> compileFiles;                   // 编译文件列表
private List<String> contentFiles;                   // 内容文件列表
private List<String> noneFiles;                      // 其他文件列表

// 配置信息
private List<ProjectConfiguration> configurations;   // 项目配置列表
private Map<String, String> properties;              // 项目属性
```

## 解析器层

### ProjectParserFacade 接口

统一的项目文件解析入口，支持同步和异步解析操作：

```java
public interface ProjectParserFacade {
    // 同步解析项目文件
    ProjectModel parse(Path projectFilePath) throws ParseException;
    
    // 异步解析项目文件
    CompletableFuture<ProjectModel> parseAsync(Path projectFilePath);
    
    // 重新解析项目文件
    ProjectModel reload(Path projectFilePath) throws ParseException;
    
    // 异步重新解析项目文件
    CompletableFuture<ProjectModel> reloadAsync(Path projectFilePath);
    
    // 检查是否为SDK风格项目
    boolean isSdkStyleProject(Path projectFilePath);
    
    // 获取项目文件最后修改时间
    long getProjectFileTimestamp(Path projectFilePath);
    
    // 关闭资源
    void shutdown();
}
```

### 解析器实现

1. **OmniSharpProjectParserFacade** - 外观模式实现，根据项目类型自动选择合适的解析器
2. **SdkStyleProjectParser** - SDK风格项目文件解析器，支持.NET Core/.NET 5+项目
3. **LegacyProjectParser** - 传统风格项目文件解析器，支持.NET Framework项目

## 监控器层

### ProjectFileWatcher 接口

定义项目文件监控功能：

```java
public interface ProjectFileWatcher {
    // 开始监控项目文件
    void startWatching(Path projectFilePath);
    
    // 停止监控
    void stopWatching();
    
    // 停止监控特定项目
    void stopWatching(Path projectFilePath);
    
    // 注册文件变更监听器
    void addProjectFileChangeListener(ProjectFileChangeListener listener);
    
    // 移除文件变更监听器
    void removeProjectFileChangeListener(ProjectFileChangeListener listener);
    
    // 检查是否正在监控
    boolean isWatching(Path projectFilePath);
    
    // 文件变更监听器接口
    interface ProjectFileChangeListener {
        void projectFileCreated(Path filePath);
        void projectFileModified(Path filePath);
        void projectFileDeleted(Path filePath);
        void watcherError(Exception error);
    }
}
```

### OmniSharpProjectFileWatcher 实现

使用Java NIO的WatchService实现项目文件监控：

- 监控项目文件本身及其目录变化
- 支持文件创建、修改、删除事件
- 线程安全的监听器管理

## 管理层

### ProjectFileManager 接口

定义项目文件管理功能：

```java
public interface ProjectFileManager {
    // 加载项目
    ProjectModel loadProject(Path projectFilePath) throws ProjectManagerException;
    
    // 异步加载项目
    CompletableFuture<ProjectModel> loadProjectAsync(Path projectFilePath);
    
    // 获取已加载的项目
    ProjectModel getProject(Path projectFilePath);
    
    // 刷新项目
    ProjectModel refreshProject(Path projectFilePath) throws ProjectManagerException;
    
    // 异步刷新项目
    CompletableFuture<ProjectModel> refreshProjectAsync(Path projectFilePath);
    
    // 关闭项目
    void closeProject(Path projectFilePath);
    
    // 关闭所有项目
    void closeAllProjects();
    
    // 检查项目是否已加载
    boolean isProjectLoaded(Path projectFilePath);
    
    // 获取所有已加载的项目路径
    List<Path> getLoadedProjects();
    
    // 启动项目文件监控
    void startWatching(Path projectFilePath);
    
    // 停止项目文件监控
    void stopWatching(Path projectFilePath);
    
    // 注册项目变更监听器
    void addProjectChangeListener(ProjectChangeListener listener);
    
    // 移除项目变更监听器
    void removeProjectChangeListener(ProjectChangeListener listener);
    
    // 项目变更监听器接口
    interface ProjectChangeListener {
        void projectLoaded(ProjectModel projectModel);
        void projectRefreshed(ProjectModel projectModel);
        void projectClosed(Path path);
        void projectChanged(Path path, ProjectModel projectModel);
        void projectError(Exception error, Path path);
    }
    
    // 项目管理器异常
    class ProjectManagerException extends Exception {
        // 异常实现
    }
}
```

### OmniSharpProjectFileManager 实现

项目管理的核心实现，单例模式：

- 集成解析器和监控器功能
- 维护项目缓存，避免重复解析
- 提供项目生命周期管理
- 实现事件通知机制

## 使用示例

### 基本使用

```java
// 获取项目管理器实例
OmniSharpProjectFileManager manager = OmniSharpProjectFileManager.getInstance();

// 加载项目
Path projectPath = Paths.get("path/to/project.csproj");
ProjectModel project = manager.loadProject(projectPath);

// 获取项目信息
System.out.println("项目名称: " + project.getName());
System.out.println("目标框架: " + project.getTargetFramework());

// 获取引用信息
List<PackageReference> packages = project.getPackageReferences();
for (PackageReference pkg : packages) {
    System.out.println("包: " + pkg.getId() + " (v" + pkg.getVersion() + ")
}

// 关闭项目
manager.closeProject(projectPath);
```

### 异步加载

```java
// 异步加载项目
CompletableFuture<ProjectModel> future = manager.loadProjectAsync(projectPath);

// 处理异步结果
future.thenAccept(project -> {
    // 使用加载的项目
}).exceptionally(ex -> {
    // 处理异常
    return null;
});
```

### 文件监控

```java
// 注册项目变更监听器
manager.addProjectChangeListener(new ProjectFileManager.ProjectChangeListener() {
    @Override
    public void projectChanged(Path path, ProjectModel projectModel) {
        // 处理项目变更
    }
    
    // 实现其他方法...
});

// 启动监控
manager.startWatching(projectPath);

// 停止监控
manager.stopWatching(projectPath);
```

## 项目结构

```
com.omnisharp.intellij.projectstructure/
├── model/                # 数据模型类
│   ├── ProjectModel.java
│   ├── PackageReference.java
│   ├── ProjectReference.java
│   ├── FileReference.java
│   ├── ProjectConfiguration.java
│   └── ProjectLanguage.java
├── parser/               # 解析器实现
│   ├── ProjectParserFacade.java
│   ├── OmniSharpProjectParserFacade.java
│   ├── SdkStyleProjectParser.java
│   └── LegacyProjectParser.java
├── watcher/              # 监控器实现
│   ├── ProjectFileWatcher.java
│   └── OmniSharpProjectFileWatcher.java
├── manager/              # 管理器实现
│   ├── ProjectFileManager.java
│   └── OmniSharpProjectFileManager.java
├── example/              # 示例代码
│   └── ProjectFileParserExample.java
├── exception/            # 异常类
│   └── ParseException.java
└── README.md             # 本文档
```

## 注意事项

1. **性能考虑**
   - 大型解决方案中建议使用异步解析方法
   - 适当使用项目关闭功能释放资源

2. **错误处理**
   - 解析过程中可能出现格式错误，请妥善处理异常
   - 监听文件变更时注意处理并发访问问题

3. **线程安全**
   - 项目管理器实现了线程安全，可以在多线程环境中使用
   - 事件通知在单独的线程中执行，避免阻塞主线程

## 扩展建议

1. 支持更多项目属性和配置项的解析
2. 实现项目文件的修改和保存功能
3. 添加项目依赖图分析功能
4. 优化大型解决方案的加载性能
5. 增加项目模板和升级建议功能

## 贡献指南

欢迎提交问题报告和功能请求，以及代码改进和测试用例。请确保所有代码遵循项目的编码规范。