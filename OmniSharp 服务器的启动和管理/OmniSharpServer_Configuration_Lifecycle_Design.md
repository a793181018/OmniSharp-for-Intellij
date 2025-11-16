# OmniSharp服务器启动和管理 - 配置管理和生命周期控制设计

## 1. 概述

本文档详细设计了OmniSharp服务器的配置管理和生命周期控制机制。配置管理负责OmniSharp服务器各项参数的持久化存储、加载、验证和应用；生命周期控制则负责管理服务器从启动到停止的完整过程，确保服务器能够正确地响应各种事件和状态变化。

## 2. 配置管理设计

### 2.1 配置存储机制

#### 2.1.1 项目级配置存储

```kotlin
/**
 * OmniSharp服务器配置实现类
 * 利用IntelliJ的PersistentStateComponent机制进行配置持久化
 */
@State(
    name = "OmniSharpConfiguration",
    storages = [
        // 项目级配置存储在项目的.idea目录下
        Storage("omnisharp.xml")
    ]
)
class OmniSharpConfigurationImpl(private val project: Project) : 
    IOmniSharpConfiguration, 
    PersistentStateComponent<OmniSharpConfigurationImpl.OmniSharpConfigurationState> {
    
    // 配置字段
    private var serverPath: String = ""
    private var workingDirectory: File = project.basePath?.let { File(it) } ?: File(System.getProperty("user.dir"))
    private var arguments: List<String> = listOf("--stdio")
    private var maxStartupWaitTime: Long = 30000L // 30秒
    private var autoRestart: Boolean = true
    private var maxRestartAttempts: Int = 3
    private var communicationTimeout: Long = 5000L // 5秒
    
    // 实现接口方法...
    
    /**
     * 配置状态类，用于持久化
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
    
    /**
     * 获取当前状态，用于持久化
     */
    override fun getState(): OmniSharpConfigurationState {
        return OmniSharpConfigurationState(
            serverPath = this.serverPath,
            workingDirectory = this.workingDirectory.absolutePath,
            arguments = this.arguments,
            maxStartupWaitTime = this.maxStartupWaitTime,
            autoRestart = this.autoRestart,
            maxRestartAttempts = this.maxRestartAttempts,
            communicationTimeout = this.communicationTimeout
        )
    }
    
    /**
     * 从持久化存储加载状态
     */
    override fun loadState(state: OmniSharpConfigurationState) {
        this.serverPath = state.serverPath
        this.workingDirectory = File(state.workingDirectory)
        this.arguments = state.arguments
        this.maxStartupWaitTime = state.maxStartupWaitTime
        this.autoRestart = state.autoRestart
        this.maxRestartAttempts = state.maxRestartAttempts
        this.communicationTimeout = state.communicationTimeout
    }
    
    /**
     * 从持久化存储加载配置
     */
    override fun load() {
        // IntelliJ平台会自动调用loadState方法
        // 这里可以添加额外的加载逻辑
        val service = ApplicationManager.getApplication().getService(OmniSharpApplicationSettings::class.java)
        
        // 如果项目级配置未设置，使用全局默认值
        if (serverPath.isBlank()) {
            serverPath = service.defaultServerPath
        }
        
        // 确保工作目录有效
        if (!workingDirectory.exists() || !workingDirectory.isDirectory) {
            workingDirectory = project.basePath?.let { File(it) } ?: File(System.getProperty("user.dir"))
        }
    }
    
    /**
     * 保存配置
     */
    override fun save() {
        // IntelliJ平台会自动保存状态
        // 这里可以添加额外的保存逻辑
    }
}
```

#### 2.1.2 应用级配置存储

```kotlin
/**
 * OmniSharp应用级配置
 * 用于存储全局默认设置
 */
@State(
    name = "OmniSharpApplicationSettings",
    storages = [
        // 应用级配置存储在IDE的配置目录下
        Storage(StoragePathMacros.APP_CONFIG + "/omnisharp.xml")
    ]
)
class OmniSharpApplicationSettings : PersistentStateComponent<OmniSharpApplicationSettings.ApplicationSettingsState> {
    
    var defaultServerPath: String = ""
    var downloadServerAutomatically: Boolean = true
    var preferredServerVersion: String = "latest"
    var serverDownloadDirectory: String = getDefaultDownloadDirectory()
    
    /**
     * 获取默认下载目录
     */
    private fun getDefaultDownloadDirectory(): String {
        val systemTempDir = System.getProperty("java.io.tmpdir")
        return File(systemTempDir, "omnisharp-servers").absolutePath
    }
    
    /**
     * 应用设置状态类
     */
    data class ApplicationSettingsState(
        var defaultServerPath: String = "",
        var downloadServerAutomatically: Boolean = true,
        var preferredServerVersion: String = "latest",
        var serverDownloadDirectory: String = ""
    )
    
    override fun getState(): ApplicationSettingsState {
        return ApplicationSettingsState(
            defaultServerPath = this.defaultServerPath,
            downloadServerAutomatically = this.downloadServerAutomatically,
            preferredServerVersion = this.preferredServerVersion,
            serverDownloadDirectory = this.serverDownloadDirectory
        )
    }
    
    override fun loadState(state: ApplicationSettingsState) {
        this.defaultServerPath = state.defaultServerPath
        this.downloadServerAutomatically = state.downloadServerAutomatically
        this.preferredServerVersion = state.preferredServerVersion
        this.serverDownloadDirectory = if (state.serverDownloadDirectory.isNotBlank()) {
            state.serverDownloadDirectory
        } else {
            getDefaultDownloadDirectory()
        }
    }
    
    companion object {
        fun getInstance(): OmniSharpApplicationSettings {
            return ApplicationManager.getApplication().getService(OmniSharpApplicationSettings::class.java)
        }
    }
}
```

### 2.2 配置验证机制

```kotlin
/**
 * 验证配置是否有效
 */
override fun validate(): IOmniSharpConfiguration.ValidationResult {
    // 验证服务器路径
    if (serverPath.isBlank()) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "OmniSharp server path is not configured"
        )
    }
    
    val serverFile = File(serverPath)
    if (!serverFile.exists()) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "OmniSharp server executable not found at: $serverPath"
        )
    }
    
    if (!isExecutable(serverFile)) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "OmniSharp server file is not executable: $serverPath"
        )
    }
    
    // 验证工作目录
    if (!workingDirectory.exists()) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "Working directory does not exist: ${workingDirectory.absolutePath}"
        )
    }
    
    if (!workingDirectory.isDirectory) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "Working directory is not a directory: ${workingDirectory.absolutePath}"
        )
    }
    
    // 验证超时设置
    if (maxStartupWaitTime <= 0) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "Max startup wait time must be positive: $maxStartupWaitTime"
        )
    }
    
    if (communicationTimeout <= 0) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "Communication timeout must be positive: $communicationTimeout"
        )
    }
    
    // 验证重启设置
    if (maxRestartAttempts < 0) {
        return IOmniSharpConfiguration.ValidationResult(
            false, 
            "Max restart attempts cannot be negative: $maxRestartAttempts"
        )
    }
    
    return IOmniSharpConfiguration.ValidationResult(true)
}

/**
 * 检查文件是否可执行
 */
private fun isExecutable(file: File): Boolean {
    // Windows平台下所有文件都可执行
    if (SystemInfo.isWindows) {
        // 检查文件扩展名是否为可执行文件
        val extension = file.name.substringAfterLast('.', "").lowercase()
        return extension in listOf("exe", "cmd", "bat", "ps1")
    }
    
    // Unix/Linux平台检查执行权限
    return file.canExecute()
}
```

### 2.3 配置UI界面

#### 2.3.1 项目级配置UI

```kotlin
/**
 * OmniSharp项目配置界面
 */
class OmniSharpProjectConfigurable(private val project: Project) : 
    ProjectConfigurable,
    Configurable.NoScroll,
    Configurable.Composite<Configurable> {
    
    private lateinit var form: OmniSharpProjectConfigForm
    
    override fun getDisplayName(): String {
        return "OmniSharp"
    }
    
    override fun getHelpTopic(): String? {
        return null
    }
    
    override fun createComponent(): JComponent {
        form = OmniSharpProjectConfigForm()
        reset()
        return form.mainPanel
    }
    
    override fun isModified(): Boolean {
        val config = IOmniSharpConfiguration.getInstance(project)
        
        return form.serverPathField.text != config.serverPath ||
               form.workingDirectoryField.text != config.workingDirectory.absolutePath ||
               form.argumentsField.text != config.arguments.joinToString(" ") ||
               form.maxStartupWaitTimeField.value != config.maxStartupWaitTime ||
               form.autoRestartCheckBox.isSelected != config.autoRestart ||
               form.maxRestartAttemptsField.value != config.maxRestartAttempts ||
               form.communicationTimeoutField.value != config.communicationTimeout
    }
    
    override fun apply() {
        val config = IOmniSharpConfiguration.getInstance(project)
        
        config.serverPath = form.serverPathField.text
        config.workingDirectory = File(form.workingDirectoryField.text)
        config.arguments = form.argumentsField.text.split("\\s+")
            .filter { it.isNotBlank() }
        config.maxStartupWaitTime = form.maxStartupWaitTimeField.value
        config.autoRestart = form.autoRestartCheckBox.isSelected
        config.maxRestartAttempts = form.maxRestartAttemptsField.value
        config.communicationTimeout = form.communicationTimeoutField.value
        
        // 保存配置
        config.save()
        
        // 如果服务器正在运行，提示用户配置更改需要重启
        val serverManager = IOmniSharpServerManager.getInstance(project)
        if (serverManager.serverStatus == ServerStatus.RUNNING) {
            Messages.showInfoMessage(
                project,
                "OmniSharp server configuration has changed. Please restart the server for changes to take effect.",
                "Configuration Changed"
            )
        }
    }
    
    override fun reset() {
        val config = IOmniSharpConfiguration.getInstance(project)
        
        form.serverPathField.text = config.serverPath
        form.workingDirectoryField.text = config.workingDirectory.absolutePath
        form.argumentsField.text = config.arguments.joinToString(" ")
        form.maxStartupWaitTimeField.value = config.maxStartupWaitTime
        form.autoRestartCheckBox.isSelected = config.autoRestart
        form.maxRestartAttemptsField.value = config.maxRestartAttempts
        form.communicationTimeoutField.value = config.communicationTimeout
    }
    
    // 实现其他必要方法...
}

/**
 * OmniSharp项目配置表单
 */
class OmniSharpProjectConfigForm {
    val mainPanel: JPanel
    val serverPathField: JTextField
    val browseServerPathButton: JButton
    val workingDirectoryField: JTextField
    val browseWorkingDirectoryButton: JButton
    val argumentsField: JTextField
    val maxStartupWaitTimeField: JSpinner
    val autoRestartCheckBox: JCheckBox
    val maxRestartAttemptsField: JSpinner
    val communicationTimeoutField: JSpinner
    
    init {
        // 初始化UI组件
        mainPanel = JPanel(BorderLayout())
        
        val contentPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.insets = Insets(5, 5, 5, 5)
        
        // 服务器路径
        constraints.gridx = 0
        constraints.gridy = 0
        constraints.weightx = 0.0
        contentPanel.add(JLabel("Server Path:"), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        serverPathField = JTextField()
        contentPanel.add(serverPathField, constraints)
        
        constraints.gridx = 2
        constraints.weightx = 0.0
        browseServerPathButton = JButton("Browse...")
        browseServerPathButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
            
            if (SystemInfo.isWindows) {
                fileChooser.fileFilter = FileNameExtensionFilter("Executable Files", "exe", "cmd", "bat", "ps1")
            }
            
            if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                serverPathField.text = fileChooser.selectedFile.absolutePath
            }
        }
        contentPanel.add(browseServerPathButton, constraints)
        
        // 工作目录
        constraints.gridx = 0
        constraints.gridy = 1
        constraints.weightx = 0.0
        contentPanel.add(JLabel("Working Directory:"), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        workingDirectoryField = JTextField()
        contentPanel.add(workingDirectoryField, constraints)
        
        constraints.gridx = 2
        constraints.weightx = 0.0
        browseWorkingDirectoryButton = JButton("Browse...")
        browseWorkingDirectoryButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            
            if (fileChooser.showOpenDialog(mainPanel) == JFileChooser.APPROVE_OPTION) {
                workingDirectoryField.text = fileChooser.selectedFile.absolutePath
            }
        }
        contentPanel.add(browseWorkingDirectoryButton, constraints)
        
        // 参数
        constraints.gridx = 0
        constraints.gridy = 2
        constraints.weightx = 0.0
        contentPanel.add(JLabel("Arguments:"), constraints)
        
        constraints.gridx = 1
        constraints.gridwidth = 2
        constraints.weightx = 1.0
        argumentsField = JTextField("--stdio")
        contentPanel.add(argumentsField, constraints)
        
        // 启动等待时间
        constraints.gridx = 0
        constraints.gridy = 3
        constraints.gridwidth = 1
        constraints.weightx = 0.0
        contentPanel.add(JLabel("Max Startup Wait Time (ms):"), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        maxStartupWaitTimeField = JSpinner(SpinnerNumberModel(30000, 1000, 300000, 1000))
        contentPanel.add(maxStartupWaitTimeField, constraints)
        
        // 自动重启
        constraints.gridx = 0
        constraints.gridy = 4
        constraints.weightx = 0.0
        autoRestartCheckBox = JCheckBox("Auto Restart Server")
        autoRestartCheckBox.isSelected = true
        contentPanel.add(autoRestartCheckBox, constraints)
        
        // 最大重启次数
        constraints.gridx = 1
        constraints.weightx = 1.0
        maxRestartAttemptsField = JSpinner(SpinnerNumberModel(3, 0, 10, 1))
        contentPanel.add(maxRestartAttemptsField, constraints)
        
        // 通信超时
        constraints.gridx = 0
        constraints.gridy = 5
        constraints.weightx = 0.0
        contentPanel.add(JLabel("Communication Timeout (ms):"), constraints)
        
        constraints.gridx = 1
        constraints.weightx = 1.0
        communicationTimeoutField = JSpinner(SpinnerNumberModel(5000, 1000, 30000, 1000))
        contentPanel.add(communicationTimeoutField, constraints)
        
        mainPanel.add(contentPanel, BorderLayout.CENTER)
    }
}
```

#### 2.3.2 应用级配置UI

```kotlin
/**
 * OmniSharp应用配置界面
 */
class OmniSharpApplicationConfigurable : 
    ApplicationConfigurable,
    Configurable.NoScroll {
    
    private lateinit var form: OmniSharpApplicationConfigForm
    
    override fun getDisplayName(): String {
        return "OmniSharp"
    }
    
    override fun getHelpTopic(): String? {
        return null
    }
    
    override fun createComponent(): JComponent {
        form = OmniSharpApplicationConfigForm()
        reset()
        return form.mainPanel
    }
    
    override fun isModified(): Boolean {
        val settings = OmniSharpApplicationSettings.getInstance()
        
        return form.defaultServerPathField.text != settings.defaultServerPath ||
               form.downloadAutomaticallyCheckBox.isSelected != settings.downloadServerAutomatically ||
               form.preferredVersionField.text != settings.preferredServerVersion ||
               form.downloadDirectoryField.text != settings.serverDownloadDirectory
    }
    
    override fun apply() {
        val settings = OmniSharpApplicationSettings.getInstance()
        
        settings.defaultServerPath = form.defaultServerPathField.text
        settings.downloadServerAutomatically = form.downloadAutomaticallyCheckBox.isSelected
        settings.preferredServerVersion = form.preferredVersionField.text
        settings.serverDownloadDirectory = form.downloadDirectoryField.text
    }
    
    override fun reset() {
        val settings = OmniSharpApplicationSettings.getInstance()
        
        form.defaultServerPathField.text = settings.defaultServerPath
        form.downloadAutomaticallyCheckBox.isSelected = settings.downloadServerAutomatically
        form.preferredVersionField.text = settings.preferredServerVersion
        form.downloadDirectoryField.text = settings.serverDownloadDirectory
    }
    
    // 实现其他必要方法...
}
```

### 2.4 配置监听机制

```kotlin
/**
 * 配置变更监听器接口
 */
interface ConfigurationChangeListener {
    /**
     * 当配置变更时调用
     */
    fun onConfigurationChanged(oldConfig: IOmniSharpConfiguration, newConfig: IOmniSharpConfiguration)
}

/**
 * 在配置实现中添加监听器支持
 */
class OmniSharpConfigurationImpl(private val project: Project) : 
    IOmniSharpConfiguration, 
    PersistentStateComponent<OmniSharpConfigurationState> {
    
    // 其他字段...
    
    private val listeners = CopyOnWriteArrayList<ConfigurationChangeListener>()
    
    /**
     * 添加配置变更监听器
     */
    fun addChangeListener(listener: ConfigurationChangeListener) {
        listeners.add(listener)
    }
    
    /**
     * 移除配置变更监听器
     */
    fun removeChangeListener(listener: ConfigurationChangeListener) {
        listeners.remove(listener)
    }
    
    /**
     * 通知配置变更
     */
    private fun notifyConfigurationChanged(oldConfig: OmniSharpConfigurationImpl) {
        for (listener in listeners) {
            try {
                listener.onConfigurationChanged(oldConfig, this)
            } catch (e: Exception) {
                logger.error("Error notifying configuration change listener", e)
            }
        }
    }
    
    // 重写setter方法以检测变更
    override var serverPath: String
        get() = _serverPath
        set(value) {
            val oldValue = _serverPath
            _serverPath = value
            if (oldValue != value) {
                notifyConfigurationChanged(copy())
            }
        }
    
    // 其他setter方法类似...
    
    /**
     * 创建当前配置的副本
     */
    private fun copy(): OmniSharpConfigurationImpl {
        val copy = OmniSharpConfigurationImpl(project)
        copy._serverPath = this._serverPath
        copy._workingDirectory = this._workingDirectory
        copy._arguments = this._arguments
        copy._maxStartupWaitTime = this._maxStartupWaitTime
        copy._autoRestart = this._autoRestart
        copy._maxRestartAttempts = this._maxRestartAttempts
        copy._communicationTimeout = this._communicationTimeout
        return copy
    }
}
```

## 3. 生命周期控制设计

### 3.1 服务器状态转换

```kotlin
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
    ERROR;
    
    /**
     * 检查是否可以转换到指定状态
     */
    fun canTransitionTo(target: ServerStatus): Boolean {
        return when (this) {
            NOT_STARTED -> target == STARTING
            STARTING -> target == RUNNING || target == STOPPING || target == ERROR
            RUNNING -> target == STOPPING || target == ERROR
            STOPPING -> target == STOPPED || target == ERROR
            STOPPED -> target == STARTING || target == NOT_STARTED
            ERROR -> target == STOPPED
        }
    }
}
```

### 3.2 生命周期事件处理

#### 3.2.1 项目打开事件

```kotlin
/**
 * 项目打开时自动启动OmniSharp服务器
 */
class OmniSharpProjectManagerListener : ProjectManagerListener {
    
    private val logger = thisLogger()
    
    override fun projectOpened(project: Project) {
        logger.info("Project opened: ${project.name}, checking if we should start OmniSharp server")
        
        // 检查项目类型，只对支持的项目类型启动服务器
        if (!isSupportedProject(project)) {
            logger.info("Project ${project.name} is not a supported C# project, skipping OmniSharp server startup")
            return
        }
        
        // 获取配置并验证
        val configuration = IOmniSharpConfiguration.getInstance(project)
        val validationResult = configuration.validate()
        
        if (!validationResult.isValid) {
            logger.warn("OmniSharp configuration is invalid: ${validationResult.errorMessage}")
            // 可以选择显示通知，引导用户配置
            return
        }
        
        // 启动服务器
        val serverManager = IOmniSharpServerManager.getInstance(project)
        
        serverManager.startServer()
            .thenAccept {
                if (it) {
                    logger.info("OmniSharp server started successfully for project: ${project.name}")
                } else {
                    logger.error("Failed to start OmniSharp server for project: ${project.name}")
                }
            }
            .exceptionally {
                logger.error("Error starting OmniSharp server for project: ${project.name}", it)
                null
            }
    }
    
    /**
     * 检查项目是否为支持的C#项目类型
     */
    private fun isSupportedProject(project: Project): Boolean {
        // 检查是否存在.csproj, .sln等C#项目文件
        val projectFiles = FileTypeIndex.getFiles(
            StdFileTypes.PLAIN_TEXT, 
            GlobalSearchScope.projectScope(project)
        )
        
        return projectFiles.any { file ->
            val extension = file.name.substringAfterLast('.', "").lowercase()
            extension in listOf("csproj", "sln", "cs", "vbproj", "fsproj")
        }
    }
    
    // 其他项目生命周期方法...
}
```

#### 3.2.2 项目关闭事件

```kotlin
/**
 * 项目关闭时停止OmniSharp服务器
 */
override fun projectClosing(project: Project): Boolean {
    logger.info("Project closing: ${project.name}, stopping OmniSharp server")
    
    val serverManager = IOmniSharpServerManager.getInstance(project)
    
    try {
        // 同步等待服务器停止，避免项目关闭时服务器仍在运行
        val stopped = serverManager.stopServer().get(10, TimeUnit.SECONDS)
        
        if (stopped) {
            logger.info("OmniSharp server stopped successfully for project: ${project.name}")
        } else {
            logger.warn("Failed to stop OmniSharp server for project: ${project.name}")
        }
    } catch (e: TimeoutException) {
        logger.error("Timed out waiting for OmniSharp server to stop for project: ${project.name}")
    } catch (e: Exception) {
        logger.error("Error stopping OmniSharp server for project: ${project.name}", e)
    }
    
    return true
}
```

### 3.3 手动生命周期控制

```kotlin
/**
 * 重启OmniSharp服务器
 */
override fun restartServer(): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    
    // 更新状态为停止中
    if (!updateStatus(ServerStatus.STOPPING)) {
        logger.warn("Cannot restart server, current status: $serverStatus")
        future.complete(false)
        return future
    }
    
    // 先停止服务器
    stopServer()
        .thenCompose { stopped ->
            if (stopped) {
                logger.info("Server stopped successfully, now starting again")
                // 停止成功后启动服务器
                return@thenCompose startServer()
            } else {
                logger.error("Failed to stop server for restart")
                updateStatus(ServerStatus.ERROR)
                return@thenCompose CompletableFuture.completedFuture(false)
            }
        }
        .thenAccept(future::complete)
        .exceptionally { e ->
            logger.error("Error restarting OmniSharp server", e)
            updateStatus(ServerStatus.ERROR)
            future.completeExceptionally(e)
            null
        }
    
    return future
}

/**
 * 更新服务器状态
 * @return 是否成功更新状态
 */
private fun updateStatus(newStatus: ServerStatus): Boolean {
    val oldStatus = serverStatus
    
    // 检查状态转换是否合法
    if (!oldStatus.canTransitionTo(newStatus)) {
        logger.warn("Invalid status transition: $oldStatus -> $newStatus")
        return false
    }
    
    _serverStatus.value = newStatus
    logger.info("OmniSharp server status changed: $oldStatus -> $newStatus")
    
    // 通知状态变更监听器
    notifyStatusChanged(oldStatus, newStatus)
    
    return true
}
```

### 3.4 资源管理和清理

```kotlin
/**
 * 停止OmniSharp服务器并清理资源
 */
override fun stopServer(): CompletableFuture<Boolean> {
    val future = CompletableFuture<Boolean>()
    
    // 检查当前状态
    if (serverStatus != ServerStatus.RUNNING && serverStatus != ServerStatus.ERROR) {
        logger.warn("Cannot stop server, current status: $serverStatus")
        future.complete(false)
        return future
    }
    
    // 更新状态为停止中
    updateStatus(ServerStatus.STOPPING)
    
    // 清理资源
    CompletableFuture.runAsync {
        try {
            // 取消所有活动的请求
            cancelAllRequests()
            
            // 停止进程
            var processStopped = false
            if (serverProcess != null) {
                processStopped = processManager.stopProcess(serverProcess!!).get(5, TimeUnit.SECONDS)
                serverProcess = null
            }
            
            // 关闭通信通道
            communication.close()
            
            // 更新状态为已停止
            updateStatus(ServerStatus.STOPPED)
            
            // 重置重启计数
            restartCount = 0
            
            // 完成future
            future.complete(processStopped)
        } catch (e: Exception) {
            logger.error("Error stopping OmniSharp server", e)
            updateStatus(ServerStatus.ERROR)
            future.completeExceptionally(e)
        }
    }
    
    return future
}

/**
 * 取消所有活动的请求
 */
private fun cancelAllRequests() {
    try {
        // 取消所有响应future
        for ((seq, future) in responseFutures) {
            future.cancel(true)
        }
        responseFutures.clear()
        
        logger.debug("Cancelled all active requests")
    } catch (e: Exception) {
        logger.error("Error cancelling active requests", e)
    }
}
```

## 4. 自动恢复机制

### 4.1 崩溃自动恢复

```kotlin
/**
 * 处理进程意外终止
 */
private fun handleUnexpectedProcessTermination(exitCode: Int) {
    logger.warn("OmniSharp server process terminated unexpectedly with exit code: $exitCode")
    
    // 如果启用了自动重启且未超过最大重启次数
    if (configuration.autoRestart && restartCount < configuration.maxRestartAttempts) {
        restartCount++
        logger.info("Attempting to restart OmniSharp server (attempt $restartCount of ${configuration.maxRestartAttempts})")
        
        // 延迟重启，避免频繁重启
        CompletableFuture.runAsync {
            try {
                Thread.sleep(2000)
                startServer()
                    .thenAccept { restarted ->
                        if (!restarted) {
                            logger.error("Failed to restart OmniSharp server")
                            updateStatus(ServerStatus.ERROR)
                        }
                    }
                    .exceptionally { e ->
                        logger.error("Error restarting OmniSharp server", e)
                        updateStatus(ServerStatus.ERROR)
                        null
                    }
            } catch (e: Exception) {
                logger.error("Error scheduling server restart", e)
            }
        }
    } else {
        logger.error("OmniSharp server crashed and auto-restart is either disabled or reached maximum attempts")
        updateStatus(ServerStatus.ERROR)
        
        // 显示通知，告知用户服务器崩溃
        ApplicationManager.getApplication().invokeLater {
            Notifications.Bus.notify(
                Notification(
                    "OmniSharp",
                    "OmniSharp Server Crashed",
                    "The OmniSharp server has crashed unexpectedly and could not be restarted. Please check the logs for more information and try restarting manually.",
                    NotificationType.ERROR
                ),
                project
            )
        }
    }
}
```

### 4.2 连接断开自动恢复

```kotlin
/**
 * 检测连接断开并尝试恢复
 */
private fun setupConnectionMonitor() {
    // 定时发送ping请求检查连接
    val scheduler = Executors.newSingleThreadScheduledExecutor {\ runnable ->
        Thread(runnable, "OmniSharp-Connection-Monitor").apply { isDaemon = true }
    }
    
    scheduler.scheduleAtFixedRate({
        checkConnection()
    }, 30, 30, TimeUnit.SECONDS) // 每30秒检查一次连接
    
    // 存储scheduler引用以便在服务器停止时关闭
    connectionMonitor = scheduler
}

/**
 * 检查连接是否正常
 */
private fun checkConnection() {
    if (serverStatus != ServerStatus.RUNNING) {
        return
    }
    
    try {
        // 发送ping请求
        val pingRequest = OmniSharpRequest<Any>(
            command = "/ping",
            arguments = emptyMap(),
            responseType = ObjectMapper().typeFactory.constructType(Any::class.java)
        )
        
        communication.sendRequest(pingRequest)
            .orTimeout(5, TimeUnit.SECONDS)
            .exceptionally { e ->
                logger.warn("Connection check failed, attempting to reconnect", e)
                reconnect()
                null
            }
    } catch (e: Exception) {
        logger.error("Error checking connection", e)
        reconnect()
    }
}

/**
 * 尝试重新连接
 */
private fun reconnect() {
    if (serverStatus != ServerStatus.RUNNING) {
        return
    }
    
    logger.info("Attempting to reconnect to OmniSharp server")
    
    // 关闭现有通信通道
    try {
        communication.close()
    } catch (e: Exception) {
        logger.warn("Error closing communication channel during reconnect", e)
    }
    
    // 重新初始化通信
    if (serverProcess != null && processManager.isProcessRunning(serverProcess!!)) {
        try {
            communication.initialize(serverProcess!!.inputStream, serverProcess!!.outputStream)
            logger.info("Reconnected to OmniSharp server")
        } catch (e: Exception) {
            logger.error("Failed to reconnect to OmniSharp server", e)
            // 如果重新连接失败，重启服务器
            restartServer()
        }
    } else {
        // 进程已终止，重启服务器
        restartServer()
    }
}
```

## 5. 配置和生命周期集成

### 5.1 配置变更时的生命周期处理

```kotlin
/**
 * 在服务器管理器中监听配置变更
 */
private fun setupConfigurationListener() {
    val configuration = IOmniSharpConfiguration.getInstance(project)
    
    // 如果配置实现支持监听
    if (configuration is OmniSharpConfigurationImpl) {
        configuration.addChangeListener(object : ConfigurationChangeListener {
            override fun onConfigurationChanged(oldConfig: IOmniSharpConfiguration, newConfig: IOmniSharpConfiguration) {
                handleConfigurationChanged(oldConfig, newConfig)
            }
        })
    }
}

/**
 * 处理配置变更
 */
private fun handleConfigurationChanged(oldConfig: IOmniSharpConfiguration, newConfig: IOmniSharpConfiguration) {
    logger.info("OmniSharp configuration changed, checking if server restart is needed")
    
    // 验证新配置
    val validationResult = newConfig.validate()
    if (!validationResult.isValid) {
        logger.warn("New configuration is invalid: ${validationResult.errorMessage}")
        return
    }
    
    // 检查是否需要重启服务器
    val needsRestart = 
        oldConfig.serverPath != newConfig.serverPath ||
        oldConfig.arguments != newConfig.arguments
    
    if (needsRestart && serverStatus == ServerStatus.RUNNING) {
        // 询问用户是否要重启服务器
        ApplicationManager.getApplication().invokeLater {
            val result = Messages.showYesNoDialog(
                project,
                "OmniSharp server configuration has changed. Restart the server now?",
                "Configuration Changed",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                restartServer()
            }
        }
    }
}
```

### 5.2 服务器健康检查

```kotlin
/**
 * 执行服务器健康检查
 */
fun performHealthCheck(): HealthCheckResult {
    val checks = mutableListOf<HealthCheck>()
    
    // 检查进程状态
    checks.add(checkProcessStatus())
    
    // 检查通信状态
    checks.add(checkCommunicationStatus())
    
    // 检查最近错误
    checks.add(checkRecentErrors())
    
    // 汇总结果
    val allHealthy = checks.all { it.healthy }
    val message = checks.joinToString(", ") { it.message }
    
    return HealthCheckResult(allHealthy, message, checks)
}

/**
 * 检查进程状态
 */
private fun checkProcessStatus(): HealthCheck {
    if (serverStatus != ServerStatus.RUNNING) {
        return HealthCheck(false, "Server is not running")
    }
    
    if (serverProcess == null || !processManager.isProcessRunning(serverProcess!!)) {
        return HealthCheck(false, "Server process is not running")
    }
    
    return HealthCheck(true, "Process is running")
}

/**
 * 检查通信状态
 */
private fun checkCommunicationStatus(): HealthCheck {
    if (communication.isClosed) {
        return HealthCheck(false, "Communication channel is closed")
    }
    
    return HealthCheck(true, "Communication is active")
}

/**
 * 检查最近错误
 */
private fun checkRecentErrors(): HealthCheck {
    // 检查最近的错误日志
    val recentErrors = errorLog.takeLast(5)
    if (recentErrors.isNotEmpty()) {
        return HealthCheck(false, "Recent errors detected: ${recentErrors.size}")
    }
    
    return HealthCheck(true, "No recent errors")
}

/**
 * 健康检查结果
 */
data class HealthCheckResult(
    val healthy: Boolean,
    val summary: String,
    val checks: List<HealthCheck>
)

/**
 * 单个健康检查
 */
data class HealthCheck(
    val healthy: Boolean,
    val message: String
)
```

## 6. 性能优化和资源管理

### 6.1 延迟初始化

```kotlin
/**
 * 延迟初始化通信组件
 */
private val communication: IOmniSharpCommunication by lazy {
    logger.info("Lazily initializing OmniSharp communication component")
    ApplicationManager.getApplication().getService(IOmniSharpCommunication::class.java)
}

/**
 * 延迟初始化进程管理组件
 */
private val processManager: IOmniSharpProcessManager by lazy {
    logger.info("Lazily initializing OmniSharp process manager component")
    ApplicationManager.getApplication().getService(IOmniSharpProcessManager::class.java)
}
```

### 6.2 资源回收

```kotlin
/**
 * 在服务器管理器销毁时清理资源
 */
fun dispose() {
    logger.info("Disposing OmniSharp server manager")
    
    // 停止服务器
    try {
        stopServer().get(5, TimeUnit.SECONDS)
    } catch (e: Exception) {
        logger.error("Error stopping server during disposal", e)
    }
    
    // 关闭连接监控
    if (connectionMonitor != null) {
        connectionMonitor!!.shutdownNow()
        connectionMonitor = null
    }
    
    // 清理事件订阅
    eventSubscriptions.clear()
}
```

## 7. 小结

本文档详细设计了OmniSharp服务器的配置管理和生命周期控制机制。配置管理模块利用IntelliJ平台的持久化机制实现了配置的存储和加载，并提供了友好的UI界面用于配置管理。生命周期控制模块则管理服务器的完整生命周期，包括启动、运行、重启和停止，确保服务器能够正确响应各种事件和状态变化。同时，本文档还设计了自动恢复机制，使服务器能够在意外崩溃或连接断开时自动恢复，提高了插件的稳定性和可靠性。