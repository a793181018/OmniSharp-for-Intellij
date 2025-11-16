# OmniSharp服务器启动和管理 - 错误处理和日志记录设计

## 1. 概述

本文档详细设计了OmniSharp服务器的错误处理和日志记录机制。错误处理负责捕获、分类和适当响应各种错误情况；日志记录则负责记录系统运行状态、错误信息和调试信息，为问题诊断和系统监控提供重要支持。良好的错误处理和日志记录是确保插件稳定性和可维护性的关键。

## 2. 错误类型设计

### 2.1 自定义异常体系

```kotlin
/**
 * OmniSharp服务器相关的基础异常类
 */
open class OmniSharpException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

/**
 * 配置相关异常
 */
class OmniSharpConfigurationException : OmniSharpException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}

/**
 * 服务器启动相关异常
 */
class OmniSharpServerStartException : OmniSharpException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(message: String, exitCode: Int) : 
        super("$message (exit code: $exitCode)")
}

/**
 * 服务器通信相关异常
 */
class OmniSharpCommunicationException : OmniSharpException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(requestId: String?, message: String) : 
        super("Request $requestId failed: $message")
}

/**
 * 服务器超时相关异常
 */
class OmniSharpTimeoutException : OmniSharpException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(requestId: String?, timeoutMs: Long) : 
        super("Request $requestId timed out after $timeoutMs ms")
}
```

### 2.2 错误代码系统

```kotlin
/**
 * OmniSharp错误代码枚举
 */
enum class OmniSharpErrorCode(
    val code: String,
    val severity: Severity,
    val defaultMessage: String
) {
    // 配置错误 (C-xxx)
    CONFIGURATION_NOT_FOUND("C-001", Severity.HIGH, "OmniSharp configuration not found"),
    CONFIGURATION_INVALID("C-002", Severity.HIGH, "OmniSharp configuration is invalid"),
    SERVER_PATH_NOT_FOUND("C-003", Severity.HIGH, "OmniSharp server executable not found"),
    WORKING_DIRECTORY_INVALID("C-004", Severity.HIGH, "Working directory is invalid"),
    
    // 服务器启动错误 (S-xxx)
    SERVER_START_FAILED("S-001", Severity.CRITICAL, "Failed to start OmniSharp server"),
    SERVER_START_TIMEOUT("S-002", Severity.HIGH, "OmniSharp server start timed out"),
    PROCESS_CREATE_FAILED("S-003", Severity.CRITICAL, "Failed to create server process"),
    SERVER_INITIALIZATION_FAILED("S-004", Severity.HIGH, "Server initialization failed"),
    
    // 通信错误 (M-xxx)
    COMMUNICATION_CHANNEL_CLOSED("M-001", Severity.HIGH, "Communication channel is closed"),
    MESSAGE_PARSE_ERROR("M-002", Severity.MEDIUM, "Failed to parse message"),
    MESSAGE_SEND_ERROR("M-003", Severity.MEDIUM, "Failed to send message"),
    RESPONSE_TIMEOUT("M-004", Severity.HIGH, "Response timed out"),
    UNEXPECTED_RESPONSE("M-005", Severity.MEDIUM, "Received unexpected response"),
    
    // 服务器运行时错误 (R-xxx)
    SERVER_CRASHED("R-001", Severity.CRITICAL, "OmniSharp server crashed"),
    MAX_RESTART_ATTEMPTS_REACHED("R-002", Severity.HIGH, "Maximum restart attempts reached"),
    CONNECTION_LOST("R-003", Severity.HIGH, "Connection to server lost"),
    
    // 功能错误 (F-xxx)
    FEATURE_NOT_SUPPORTED("F-001", Severity.LOW, "Feature not supported by the current server version"),
    INVALID_REQUEST_PARAMETERS("F-002", Severity.MEDIUM, "Invalid request parameters");
    
    /**
     * 错误严重程度枚举
     */
    enum class Severity {
        /** 轻微错误，不影响主要功能 */
        LOW,
        /** 中等错误，可能影响某些功能 */
        MEDIUM,
        /** 严重错误，影响重要功能 */
        HIGH,
        /** 致命错误，导致功能完全不可用 */
        CRITICAL
    }
}

/**
 * 带错误代码的OmniSharp异常
 */
open class OmniSharpErrorCodeException(
    val errorCode: OmniSharpErrorCode,
    message: String? = null,
    cause: Throwable? = null
) : OmniSharpException(
    message ?: errorCode.defaultMessage,
    cause
)

// 特定错误代码异常类示例
class OmniSharpConfigurationNotFoundException(
    message: String? = null,
    cause: Throwable? = null
) : OmniSharpErrorCodeException(
    OmniSharpErrorCode.CONFIGURATION_NOT_FOUND,
    message,
    cause
)
```

## 3. 全局异常处理

### 3.1 异常处理器接口

```kotlin
/**
 * OmniSharp异常处理器接口
 */
interface IOmniSharpExceptionHandler {
    /**
     * 处理OmniSharp异常
     */
    fun handleException(exception: OmniSharpException): ExceptionHandlingResult
    
    /**
     * 处理未捕获的异常
     */
    fun handleUncaughtException(thread: Thread, exception: Throwable): ExceptionHandlingResult
}

/**
 * 异常处理结果
 */
data class ExceptionHandlingResult(
    /** 是否成功处理了异常 */
    val handled: Boolean,
    /** 是否需要重新抛出异常 */
    val rethrow: Boolean = false,
    /** 异常处理后采取的行动 */
    val action: ExceptionAction = ExceptionAction.NONE
)

/**
 * 异常处理后采取的行动
 */
enum class ExceptionAction {
    /** 无操作 */
    NONE,
    /** 重启服务器 */
    RESTART_SERVER,
    /** 显示用户界面通知 */
    SHOW_NOTIFICATION,
    /** 显示错误对话框 */
    SHOW_ERROR_DIALOG,
    /** 记录到错误日志 */
    LOG_ERROR
}
```

### 3.2 全局异常处理器实现

```kotlin
/**
 * OmniSharp全局异常处理器实现
 */
class OmniSharpExceptionHandlerImpl(private val project: Project) : IOmniSharpExceptionHandler {
    
    private val logger = thisLogger()
    private val notificationManager = Notifications.Bus
    
    override fun handleException(exception: OmniSharpException): ExceptionHandlingResult {
        logger.error("Handling OmniSharp exception", exception)
        
        return when (exception) {
            is OmniSharpConfigurationException -> handleConfigurationException(exception)
            is OmniSharpServerStartException -> handleServerStartException(exception)
            is OmniSharpCommunicationException -> handleCommunicationException(exception)
            is OmniSharpTimeoutException -> handleTimeoutException(exception)
            is OmniSharpErrorCodeException -> handleErrorCodeException(exception)
            else -> handleGenericException(exception)
        }
    }
    
    private fun handleConfigurationException(exception: OmniSharpConfigurationException): ExceptionHandlingResult {
        logger.warn("Configuration error", exception)
        
        // 显示配置错误通知
        showNotification(
            "OmniSharp Configuration Error",
            exception.message ?: "Invalid OmniSharp configuration",
            NotificationType.ERROR,
            action = { navigateToConfiguration() }
        )
        
        return ExceptionHandlingResult(
            handled = true,
            action = ExceptionAction.SHOW_NOTIFICATION
        )
    }
    
    private fun handleServerStartException(exception: OmniSharpServerStartException): ExceptionHandlingResult {
        logger.error("Server start failed", exception)
        
        // 显示服务器启动错误通知
        showNotification(
            "OmniSharp Server Start Failed",
            exception.message ?: "Failed to start OmniSharp server",
            NotificationType.ERROR,
            action = { showServerLog() }
        )
        
        return ExceptionHandlingResult(
            handled = true,
            action = ExceptionAction.SHOW_NOTIFICATION
        )
    }
    
    private fun handleCommunicationException(exception: OmniSharpCommunicationException): ExceptionHandlingResult {
        logger.error("Communication error", exception)
        
        // 检查服务器状态
        val serverManager = IOmniSharpServerManager.getInstance(project)
        if (serverManager.serverStatus == ServerStatus.RUNNING) {
            // 尝试重新连接
            CompletableFuture.runAsync {
                serverManager.restartServer()
            }
            
            return ExceptionHandlingResult(
                handled = true,
                action = ExceptionAction.RESTART_SERVER
            )
        } else {
            // 显示通信错误通知
            showNotification(
                "OmniSharp Communication Error",
                exception.message ?: "Failed to communicate with OmniSharp server",
                NotificationType.ERROR
            )
            
            return ExceptionHandlingResult(
                handled = true,
                action = ExceptionAction.SHOW_NOTIFICATION
            )
        }
    }
    
    private fun handleTimeoutException(exception: OmniSharpTimeoutException): ExceptionHandlingResult {
        logger.warn("Timeout error", exception)
        
        // 显示超时通知
        showNotification(
            "OmniSharp Request Timeout",
            exception.message ?: "OmniSharp server request timed out",
            NotificationType.WARNING
        )
        
        return ExceptionHandlingResult(
            handled = true,
            action = ExceptionAction.SHOW_NOTIFICATION
        )
    }
    
    private fun handleErrorCodeException(exception: OmniSharpErrorCodeException): ExceptionHandlingResult {
        val errorCode = exception.errorCode
        logger.error("Error code: ${errorCode.code} - ${errorCode.defaultMessage}", exception)
        
        // 根据错误严重程度采取不同行动
        return when (errorCode.severity) {
            OmniSharpErrorCode.Severity.CRITICAL -> {
                // 显示错误对话框
                showErrorDialog(
                    "OmniSharp Critical Error",
                    "${errorCode.code}: ${exception.message}\n\nPlease check the logs for more details and restart the IDE if necessary."
                )
                
                ExceptionHandlingResult(
                    handled = true,
                    action = ExceptionAction.SHOW_ERROR_DIALOG
                )
            }
            OmniSharpErrorCode.Severity.HIGH -> {
                // 显示通知
                showNotification(
                    "OmniSharp Error",
                    "${errorCode.code}: ${exception.message}",
                    NotificationType.ERROR
                )
                
                ExceptionHandlingResult(
                    handled = true,
                    action = ExceptionAction.SHOW_NOTIFICATION
                )
            }
            OmniSharpErrorCode.Severity.MEDIUM -> {
                // 记录警告
                ExceptionHandlingResult(
                    handled = true,
                    action = ExceptionAction.LOG_ERROR
                )
            }
            OmniSharpErrorCode.Severity.LOW -> {
                // 仅记录信息
                logger.info("Low severity error: ${errorCode.code} - ${exception.message}")
                
                ExceptionHandlingResult(
                    handled = true
                )
            }
        }
    }
    
    private fun handleGenericException(exception: OmniSharpException): ExceptionHandlingResult {
        logger.error("Unhandled OmniSharp exception", exception)
        
        // 显示通用错误通知
        showNotification(
            "OmniSharp Error",
            exception.message ?: "An unexpected error occurred in the OmniSharp plugin",
            NotificationType.ERROR
        )
        
        return ExceptionHandlingResult(
            handled = true,
            action = ExceptionAction.SHOW_NOTIFICATION
        )
    }
    
    override fun handleUncaughtException(thread: Thread, exception: Throwable): ExceptionHandlingResult {
        logger.error("Uncaught exception in thread: ${thread.name}", exception)
        
        // 包装为通用异常并处理
        val wrapperException = OmniSharpException(
            "Uncaught exception in thread ${thread.name}: ${exception.message}",
            exception
        )
        
        return handleGenericException(wrapperException)
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(
        title: String,
        message: String,
        type: NotificationType,
        action: (() -> Unit)? = null
    ) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val notification = object : Notification("OmniSharp", title, message, type) {
                    override fun onClick(e: MouseEvent?) {
                        super.onClick(e)
                        action?.invoke()
                    }
                }
                
                notificationManager.notify(notification, project)
            } catch (e: Exception) {
                logger.error("Failed to show notification", e)
            }
        }
    }
    
    /**
     * 显示错误对话框
     */
    private fun showErrorDialog(title: String, message: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                Messages.showErrorDialog(project, message, title)
            } catch (e: Exception) {
                logger.error("Failed to show error dialog", e)
            }
        }
    }
    
    /**
     * 导航到配置页面
     */
    private fun navigateToConfiguration() {
        try {
            ShowSettingsUtil.getInstance().showSettingsDialog(
                project,
                OmniSharpProjectConfigurable::class.java
            )
        } catch (e: Exception) {
            logger.error("Failed to navigate to configuration", e)
        }
    }
    
    /**
     * 显示服务器日志
     */
    private fun showServerLog() {
        try {
            val logManager = IOmniSharpLogManager.getInstance(project)
            logManager.showLogViewer()
        } catch (e: Exception) {
            logger.error("Failed to show server log", e)
        }
    }
}
```

### 3.3 异常监听器设置

```kotlin
/**
 * 在插件启动时设置全局异常处理器
 */
class OmniSharpExceptionHandlerRegistrar : ApplicationComponent {
    
    override fun initComponent() {
        // 注册线程未捕获异常处理器
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val application = ApplicationManager.getApplication()
            val handler = application.getService(IOmniSharpExceptionHandler::class.java)
            handler.handleUncaughtException(thread, throwable)
        }
    }
    
    override fun disposeComponent() {
        // 清理资源
    }
}
```

## 4. 日志记录机制

### 4.1 日志管理器接口

```kotlin
/**
 * OmniSharp日志管理器接口
 */
interface IOmniSharpLogManager {
    /**
     * 获取日志记录器
     */
    fun getLogger(): Logger
    
    /**
     * 记录信息级别的日志
     */
    fun info(message: String)
    
    /**
     * 记录信息级别的日志，带异常
     */
    fun info(message: String, throwable: Throwable)
    
    /**
     * 记录警告级别的日志
     */
    fun warn(message: String)
    
    /**
     * 记录警告级别的日志，带异常
     */
    fun warn(message: String, throwable: Throwable)
    
    /**
     * 记录错误级别的日志
     */
    fun error(message: String)
    
    /**
     * 记录错误级别的日志，带异常
     */
    fun error(message: String, throwable: Throwable)
    
    /**
     * 记录调试级别的日志
     */
    fun debug(message: String)
    
    /**
     * 记录调试级别的日志，带异常
     */
    fun debug(message: String, throwable: Throwable)
    
    /**
     * 记录服务器输出日志
     */
    fun logServerOutput(message: String)
    
    /**
     * 记录服务器错误输出日志
     */
    fun logServerError(message: String)
    
    /**
     * 显示日志查看器
     */
    fun showLogViewer()
    
    /**
     * 获取日志内容
     */
    fun getLogContent(): String
    
    /**
     * 清除日志
     */
    fun clearLogs()
    
    /**
     * 导出日志到文件
     */
    fun exportLogsToFile(file: File): Boolean
    
    /**
     * 设置日志级别
     */
    fun setLogLevel(level: LogLevel)
}

/**
 * 日志级别枚举
 */
enum class LogLevel {
    OFF,
    ERROR,
    WARN,
    INFO,
    DEBUG,
    TRACE
}
```

### 4.2 日志管理器实现

```kotlin
/**
 * OmniSharp日志管理器实现
 */
class OmniSharpLogManagerImpl(private val project: Project) : IOmniSharpLogManager {
    
    private val logger = thisLogger()
    private val logFile: File
    private val logContent = StringBuilder()
    private val serverOutput = StringBuilder()
    private val serverError = StringBuilder()
    private val lock = ReentrantReadWriteLock()
    private var logLevel: LogLevel = LogLevel.INFO
    
    init {
        // 初始化日志文件
        logFile = createLogFile()
        
        // 初始化日志级别
        val settings = OmniSharpApplicationSettings.getInstance()
        logLevel = settings.logLevel
        
        // 记录插件启动信息
        info("OmniSharp plugin initialized for project: ${project.name}")
    }
    
    /**
     * 创建日志文件
     */
    private fun createLogFile(): File {
        val logDir = File(project.basePath, ".omnisharp/logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(logDir, "omnisharp_${timestamp}.log")
    }
    
    override fun getLogger(): Logger {
        return logger
    }
    
    override fun info(message: String) {
        if (logLevel <= LogLevel.INFO) {
            log("INFO", message)
        }
    }
    
    override fun info(message: String, throwable: Throwable) {
        if (logLevel <= LogLevel.INFO) {
            log("INFO", message, throwable)
        }
    }
    
    override fun warn(message: String) {
        if (logLevel <= LogLevel.WARN) {
            log("WARN", message)
        }
    }
    
    override fun warn(message: String, throwable: Throwable) {
        if (logLevel <= LogLevel.WARN) {
            log("WARN", message, throwable)
        }
    }
    
    override fun error(message: String) {
        if (logLevel <= LogLevel.ERROR) {
            log("ERROR", message)
        }
    }
    
    override fun error(message: String, throwable: Throwable) {
        if (logLevel <= LogLevel.ERROR) {
            log("ERROR", message, throwable)
        }
    }
    
    override fun debug(message: String) {
        if (logLevel <= LogLevel.DEBUG) {
            log("DEBUG", message)
        }
    }
    
    override fun debug(message: String, throwable: Throwable) {
        if (logLevel <= LogLevel.DEBUG) {
            log("DEBUG", message, throwable)
        }
    }
    
    override fun logServerOutput(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val formattedMessage = "[$timestamp] SERVER: $message\n"
        
        lock.writeLock().lock()
        try {
            serverOutput.append(formattedMessage)
            appendToLog(formattedMessage)
            writeToLogFile(formattedMessage)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    override fun logServerError(message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val formattedMessage = "[$timestamp] SERVER-ERROR: $message\n"
        
        lock.writeLock().lock()
        try {
            serverError.append(formattedMessage)
            appendToLog(formattedMessage)
            writeToLogFile(formattedMessage)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 记录日志
     */
    private fun log(level: String, message: String) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val formattedMessage = "[$timestamp] [$level] $message\n"
        
        lock.writeLock().lock()
        try {
            appendToLog(formattedMessage)
            writeToLogFile(formattedMessage)
        } finally {
            lock.writeLock().unlock()
        }
        
        // 调用IntelliJ的日志记录器
        when (level) {
            "INFO" -> logger.info(message)
            "WARN" -> logger.warn(message)
            "ERROR" -> logger.error(message)
            "DEBUG" -> logger.debug(message)
        }
    }
    
    /**
     * 记录带异常的日志
     */
    private fun log(level: String, message: String, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
        val stackTrace = ExceptionUtil.getThrowableText(throwable)
        val formattedMessage = "[$timestamp] [$level] $message\n$stackTrace\n"
        
        lock.writeLock().lock()
        try {
            appendToLog(formattedMessage)
            writeToLogFile(formattedMessage)
        } finally {
            lock.writeLock().unlock()
        }
        
        // 调用IntelliJ的日志记录器
        when (level) {
            "INFO" -> logger.info(message, throwable)
            "WARN" -> logger.warn(message, throwable)
            "ERROR" -> logger.error(message, throwable)
            "DEBUG" -> logger.debug(message, throwable)
        }
    }
    
    /**
     * 将日志追加到内存
     */
    private fun appendToLog(message: String) {
        logContent.append(message)
        
        // 限制内存中的日志大小，防止内存溢出
        if (logContent.length > MAX_LOG_SIZE_IN_MEMORY) {
            // 保留最新的日志内容
            val excess = logContent.length - MAX_LOG_SIZE_IN_MEMORY
            logContent.delete(0, excess)
        }
    }
    
    /**
     * 将日志写入文件
     */
    private fun writeToLogFile(message: String) {
        try {
            Files.write(logFile.toPath(), message.toByteArray(), StandardOpenOption.APPEND, StandardOpenOption.CREATE)
        } catch (e: IOException) {
            // 如果写入文件失败，至少保留在内存中
            logger.error("Failed to write to log file: ${logFile.absolutePath}", e)
        }
    }
    
    override fun showLogViewer() {
        ApplicationManager.getApplication().invokeLater {
            try {
                val logWindow = ToolWindowManager.getInstance(project).getToolWindow("OmniSharp Log")
                logWindow?.show(null)
            } catch (e: Exception) {
                logger.error("Failed to show log viewer", e)
            }
        }
    }
    
    override fun getLogContent(): String {
        lock.readLock().lock()
        try {
            return logContent.toString()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun clearLogs() {
        lock.writeLock().lock()
        try {
            logContent.setLength(0)
            serverOutput.setLength(0)
            serverError.setLength(0)
            
            // 清除日志文件
            try {
                Files.write(logFile.toPath(), ByteArray(0), StandardOpenOption.TRUNCATE_EXISTING)
            } catch (e: IOException) {
                logger.error("Failed to clear log file", e)
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    override fun exportLogsToFile(file: File): Boolean {
        lock.readLock().lock()
        try {
            return try {
                Files.write(file.toPath(), getLogContent().toByteArray())
                true
            } catch (e: IOException) {
                logger.error("Failed to export logs to file: ${file.absolutePath}", e)
                false
            }
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun setLogLevel(level: LogLevel) {
        this.logLevel = level
        info("Log level changed to: $level")
    }
    
    companion object {
        private const val MAX_LOG_SIZE_IN_MEMORY = 1024 * 1024 // 1MB
    }
}
```

### 4.3 日志查看器UI

```kotlin
/**
 * OmniSharp日志查看器
 */
class OmniSharpLogViewerToolWindow(private val project: Project) {
    
    fun createContent(): JComponent {
        val panel = JPanel(BorderLayout())
        
        // 日志文本区域
        val logTextArea = JTextArea()
        logTextArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        logTextArea.isEditable = false
        logTextArea.lineWrap = false
        logTextArea.text = getLogContent()
        
        // 自动滚动到末尾
        logTextArea.caretPosition = logTextArea.document.length
        
        // 包裹在滚动面板中
        val scrollPane = JScrollPane(logTextArea)
        scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        
        // 按钮面板
        val buttonPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        // 刷新按钮
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener {
            logTextArea.text = getLogContent()
            logTextArea.caretPosition = logTextArea.document.length
        }
        buttonPanel.add(refreshButton)
        
        // 清除按钮
        val clearButton = JButton("Clear")
        clearButton.addActionListener {
            val result = Messages.showYesNoDialog(
                project,
                "Are you sure you want to clear the logs?",
                "Clear Logs",
                Messages.getQuestionIcon()
            )
            
            if (result == Messages.YES) {
                val logManager = IOmniSharpLogManager.getInstance(project)
                logManager.clearLogs()
                logTextArea.text = ""
            }
        }
        buttonPanel.add(clearButton)
        
        // 导出按钮
        val exportButton = JButton("Export")
        exportButton.addActionListener {
            val fileChooser = JFileChooser()
            fileChooser.dialogTitle = "Export Logs"
            fileChooser.selectedFile = File("omnisharp_log_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}.txt")
            
            if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                val logManager = IOmniSharpLogManager.getInstance(project)
                
                if (logManager.exportLogsToFile(file)) {
                    Messages.showInfoMessage(
                        project,
                        "Logs exported successfully to: ${file.absolutePath}",
                        "Export Successful"
                    )
                } else {
                    Messages.showErrorDialog(
                        project,
                        "Failed to export logs to: ${file.absolutePath}",
                        "Export Failed"
                    )
                }
            }
        }
        buttonPanel.add(exportButton)
        
        // 添加到主面板
        panel.add(buttonPanel, BorderLayout.NORTH)
        panel.add(scrollPane, BorderLayout.CENTER)
        
        // 设置自动刷新定时器
        val timer = Timer(5000) { // 每5秒刷新一次
            if (ToolWindowManager.getInstance(project).getToolWindow("OmniSharp Log")?.isVisible == true) {
                val currentContent = getLogContent()
                if (logTextArea.text != currentContent) {
                    logTextArea.text = currentContent
                    logTextArea.caretPosition = logTextArea.document.length
                }
            }
        }
        timer.isRepeats = true
        timer.start()
        
        // 当组件销毁时停止定时器
        panel.addHierarchyListener {
            if (it.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L && !panel.isDisplayable) {
                timer.stop()
            }
        }
        
        return panel
    }
    
    /**
     * 获取日志内容
     */
    private fun getLogContent(): String {
        return IOmniSharpLogManager.getInstance(project).getLogContent()
    }
}

/**
 * OmniSharp日志查看器工具窗口工厂
 */
class OmniSharpLogToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val contentFactory = ContentFactory.getInstance()
        
        val logViewer = OmniSharpLogViewerToolWindow(project)
        val content = contentFactory.createContent(logViewer.createContent(), "OmniSharp Log", false)
        
        contentManager.addContent(content)
    }
}
```

## 5. 错误反馈机制

### 5.1 用户通知系统

```kotlin
/**
 * OmniSharp通知管理器
 */
class OmniSharpNotificationManager(private val project: Project) {
    
    private val notificationBus = Notifications.Bus
    private val logger = thisLogger()
    
    /**
     * 显示信息通知
     */
    fun showInfo(title: String, message: String) {
        showNotification(title, message, NotificationType.INFORMATION)
    }
    
    /**
     * 显示警告通知
     */
    fun showWarning(title: String, message: String) {
        showNotification(title, message, NotificationType.WARNING)
    }
    
    /**
     * 显示错误通知
     */
    fun showError(title: String, message: String) {
        showNotification(title, message, NotificationType.ERROR)
    }
    
    /**
     * 显示带动作的通知
     */
    fun showNotificationWithAction(
        title: String,
        message: String,
        type: NotificationType,
        actionText: String,
        action: (Notification) -> Unit
    ) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val notification = object : Notification("OmniSharp", title, message, type) {
                    override fun addAction(notificationAction: NotificationAction) {
                        super.addAction(notificationAction)
                    }
                }
                
                notification.addAction(object : NotificationAction(actionText) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        action(notification)
                    }
                })
                
                notificationBus.notify(notification, project)
            } catch (e: Exception) {
                logger.error("Failed to show notification with action", e)
            }
        }
    }
    
    /**
     * 显示通知
     */
    private fun showNotification(title: String, message: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val notification = Notification("OmniSharp", title, message, type)
                notificationBus.notify(notification, project)
            } catch (e: Exception) {
                logger.error("Failed to show notification", e)
            }
        }
    }
    
    /**
     * 显示错误对话框
     */
    fun showErrorDialog(title: String, message: String): Int {
        return Messages.showErrorDialog(project, message, title)
    }
    
    /**
     * 显示确认对话框
     */
    fun showConfirmDialog(title: String, message: String): Boolean {
        return Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon()) == Messages.YES
    }
    
    /**
     * 显示输入对话框
     */
    fun showInputDialog(title: String, message: String, defaultValue: String? = null): String? {
        return Messages.showInputDialog(project, message, title, Messages.getQuestionIcon(), defaultValue, null)
    }
}
```

### 5.2 错误报告功能

```kotlin
/**
 * OmniSharp错误报告管理器
 */
class OmniSharpErrorReportManager(private val project: Project) {
    
    private val logger = thisLogger()
    
    /**
     * 报告错误
     */
    fun reportError(exception: Throwable, context: String? = null): Boolean {
        try {
            // 收集错误信息
            val errorReport = collectErrorReport(exception, context)
            
            // 询问用户是否发送错误报告
            val shouldReport = askUserForErrorReport(errorReport)
            
            if (shouldReport) {
                // 发送错误报告
                return sendErrorReport(errorReport)
            }
            
            return false
        } catch (e: Exception) {
            logger.error("Failed to report error", e)
            return false
        }
    }
    
    /**
     * 收集错误报告信息
     */
    private fun collectErrorReport(exception: Throwable, context: String?): ErrorReport {
        val report = ErrorReport(
            timestamp = System.currentTimeMillis(),
            pluginVersion = getPluginVersion(),
            ideVersion = ApplicationInfo.getInstance().fullVersion,
            os = SystemInfo.OS_NAME + " " + SystemInfo.OS_VERSION,
            javaVersion = System.getProperty("java.version"),
            errorClass = exception.javaClass.name,
            errorMessage = exception.message,
            stackTrace = ExceptionUtil.getThrowableText(exception),
            context = context,
            projectName = project.name,
            recentLogs = getRecentLogs()
        )
        
        return report
    }
    
    /**
     * 询问用户是否发送错误报告
     */
    private fun askUserForErrorReport(report: ErrorReport): Boolean {
        val result = Messages.showYesNoCancelDialog(
            project,
            "An error occurred in the OmniSharp plugin. Would you like to send an error report to help improve the plugin?\n\n" +
            "Error: ${report.errorMessage}\n\n" +
            "The report will include basic diagnostic information but no personal data.",
            "Send Error Report?",
            "Send Report",
            "Don't Send",
            "View Details",
            Messages.getQuestionIcon()
        )
        
        when (result) {
            Messages.YES -> return true
            Messages.NO -> return false
            Messages.CANCEL -> {
                // 显示详细信息
                showErrorReportDetails(report)
                // 再次询问
                return askUserForErrorReport(report)
            }
            else -> return false
        }
    }
    
    /**
     * 显示错误报告详细信息
     */
    private fun showErrorReportDetails(report: ErrorReport) {
        val details = StringBuilder()
        details.append("Error Report Details:\n\n")
        details.append("Plugin Version: ${report.pluginVersion}\n")
        details.append("IDE Version: ${report.ideVersion}\n")
        details.append("OS: ${report.os}\n")
        details.append("Java Version: ${report.javaVersion}\n")
        details.append("Project: ${report.projectName}\n")
        details.append("Error Class: ${report.errorClass}\n")
        details.append("Error Message: ${report.errorMessage}\n\n")
        details.append("Stack Trace:\n${report.stackTrace}\n")
        
        if (!report.context.isNullOrEmpty()) {
            details.append("\nContext:\n${report.context}\n")
        }
        
        val textArea = JTextArea(details.toString())
        textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
        textArea.isEditable = false
        textArea.lineWrap = false
        textArea.caretPosition = 0
        
        val scrollPane = JScrollPane(textArea)
        scrollPane.preferredSize = Dimension(800, 600)
        
        JOptionPane.showMessageDialog(
            project.currentMainFrame,
            scrollPane,
            "Error Report Details",
            JOptionPane.INFORMATION_MESSAGE
        )
    }
    
    /**
     * 发送错误报告
     */
    private fun sendErrorReport(report: ErrorReport): Boolean {
        // 在实际实现中，这里应该发送HTTP请求到错误报告服务器
        // 由于这是示例实现，我们只记录报告
        
        logger.info("Error report would be sent: ${report.errorClass}: ${report.errorMessage}")
        
        // 模拟成功发送
        return true
    }
    
    /**
     * 获取插件版本
     */
    private fun getPluginVersion(): String {
        return try {
            val pluginId = "com.github.a793181018.omnisharpforintellij"
            val pluginDescriptor = PluginManager.getPlugin(PluginId.getId(pluginId))
            pluginDescriptor?.version ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }
    
    /**
     * 获取最近的日志
     */
    private fun getRecentLogs(): String {
        return try {
            val logManager = IOmniSharpLogManager.getInstance(project)
            val logs = logManager.getLogContent()
            
            // 只返回最后1000行日志
            val lines = logs.split('\n')
            val recentLines = lines.takeLast(1000)
            recentLines.joinToString("\n")
        } catch (e: Exception) {
            "Failed to retrieve logs"
        }
    }
    
    /**
     * 错误报告数据类
     */
    data class ErrorReport(
        val timestamp: Long,
        val pluginVersion: String,
        val ideVersion: String,
        val os: String,
        val javaVersion: String,
        val errorClass: String,
        val errorMessage: String?,
        val stackTrace: String,
        val context: String?,
        val projectName: String,
        val recentLogs: String
    )
}
```

## 6. 故障诊断和调试支持

### 6.1 故障诊断工具

```kotlin
/**
 * OmniSharp故障诊断工具
 */
class OmniSharpDiagnosticsManager(private val project: Project) {
    
    private val logger = thisLogger()
    
    /**
     * 运行全面诊断
     */
    fun runFullDiagnostics(): DiagnosticReport {
        logger.info("Running full OmniSharp diagnostics")
        
        val checks = mutableListOf<DiagnosticCheck>()
        
        // 检查配置
        checks.add(checkConfiguration())
        
        // 检查服务器状态
        checks.add(checkServerStatus())
        
        // 检查通信状态
        checks.add(checkCommunication())
        
        // 检查系统环境
        checks.add(checkEnvironment())
        
        // 检查项目结构
        checks.add(checkProjectStructure())
        
        // 汇总结果
        val allSuccessful = checks.all { it.result == DiagnosticResult.SUCCESS }
        val report = DiagnosticReport(allSuccessful, checks)
        
        logger.info("Diagnostics completed: $allSuccessful")
        
        return report
    }
    
    /**
     * 检查配置
     */
    private fun checkConfiguration(): DiagnosticCheck {
        try {
            val config = IOmniSharpConfiguration.getInstance(project)
            val validation = config.validate()
            
            return if (validation.isValid) {
                DiagnosticCheck(
                    "Configuration",
                    DiagnosticResult.SUCCESS,
                    "Configuration is valid"
                )
            } else {
                DiagnosticCheck(
                    "Configuration",
                    DiagnosticResult.FAILURE,
                    validation.errorMessage
                )
            }
        } catch (e: Exception) {
            return DiagnosticCheck(
                "Configuration",
                DiagnosticResult.ERROR,
                "Failed to check configuration: ${e.message}"
            )
        }
    }
    
    /**
     * 检查服务器状态
     */
    private fun checkServerStatus(): DiagnosticCheck {
        try {
            val serverManager = IOmniSharpServerManager.getInstance(project)
            val status = serverManager.serverStatus
            
            return when (status) {
                ServerStatus.RUNNING -> {
                    DiagnosticCheck(
                        "Server Status",
                        DiagnosticResult.SUCCESS,
                        "Server is running"
                    )
                }
                ServerStatus.NOT_STARTED, ServerStatus.STOPPED -> {
                    DiagnosticCheck(
                        "Server Status",
                        DiagnosticResult.WARNING,
                        "Server is not running"
                    )
                }
                ServerStatus.ERROR -> {
                    DiagnosticCheck(
                        "Server Status",
                        DiagnosticResult.FAILURE,
                        "Server is in error state"
                    )
                }
                else -> {
                    DiagnosticCheck(
                        "Server Status",
                        DiagnosticResult.WARNING,
                        "Server is in state: $status"
                    )
                }
            }
        } catch (e: Exception) {
            return DiagnosticCheck(
                "Server Status",
                DiagnosticResult.ERROR,
                "Failed to check server status: ${e.message}"
            )
        }
    }
    
    /**
     * 检查通信状态
     */
    private fun checkCommunication(): DiagnosticCheck {
        try {
            val serverManager = IOmniSharpServerManager.getInstance(project)
            
            if (serverManager.serverStatus != ServerStatus.RUNNING) {
                return DiagnosticCheck(
                    "Communication",
                    DiagnosticResult.SKIPPED,
                    "Server is not running, skipping communication check"
                )
            }
            
            // 发送ping请求
            val pingResult = serverManager.ping().get(3, TimeUnit.SECONDS)
            
            return if (pingResult) {
                DiagnosticCheck(
                    "Communication",
                    DiagnosticResult.SUCCESS,
                    "Communication is working properly"
                )
            } else {
                DiagnosticCheck(
                    "Communication",
                    DiagnosticResult.FAILURE,
                    "Ping request failed"
                )
            }
        } catch (e: TimeoutException) {
            return DiagnosticCheck(
                "Communication",
                DiagnosticResult.FAILURE,
                "Communication timeout"
            )
        } catch (e: Exception) {
            return DiagnosticCheck(
                "Communication",
                DiagnosticResult.ERROR,
                "Failed to check communication: ${e.message}"
            )
        }
    }
    
    /**
     * 检查系统环境
     */
    private fun checkEnvironment(): DiagnosticCheck {
        val issues = mutableListOf<String>()
        
        // 检查操作系统
        if (SystemInfo.isMac) {
            // macOS特定检查
        } else if (SystemInfo.isWindows) {
            // Windows特定检查
        } else if (SystemInfo.isLinux) {
            // Linux特定检查
        }
        
        // 检查Java版本
        val javaVersion = System.getProperty("java.version")
        if (javaVersion.startsWith("1.")) {
            issues.add("Old Java version detected: $javaVersion")
        }
        
        // 检查内存
        val maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024) // MB
        if (maxMemory < 1024) {
            issues.add("Low max memory: ${maxMemory}MB")
        }
        
        return if (issues.isEmpty()) {
            DiagnosticCheck(
                "Environment",
                DiagnosticResult.SUCCESS,
                "Environment looks good"
            )
        } else {
            DiagnosticCheck(
                "Environment",
                DiagnosticResult.WARNING,
                issues.joinToString("; ")
            )
        }
    }
    
    /**
     * 检查项目结构
     */
    private fun checkProjectStructure(): DiagnosticCheck {
        try {
            val projectFiles = mutableListOf<String>()
            
            // 检查项目根目录
            if (project.basePath == null) {
                return DiagnosticCheck(
                    "Project Structure",
                    DiagnosticResult.FAILURE,
                    "Project base path is null"
                )
            }
            
            // 检查C#项目文件
            FileTypeIndex.getFiles(
                StdFileTypes.PLAIN_TEXT,
                GlobalSearchScope.projectScope(project)
            ).forEach { file ->
                val extension = file.name.substringAfterLast('.', "").lowercase()
                if (extension in listOf("csproj", "sln", "cs", "vbproj", "fsproj")) {
                    projectFiles.add(file.name)
                }
            }
            
            return if (projectFiles.isNotEmpty()) {
                DiagnosticCheck(
                    "Project Structure",
                    DiagnosticResult.SUCCESS,
                    "Found ${projectFiles.size} C# files"
                )
            } else {
                DiagnosticCheck(
                    "Project Structure",
                    DiagnosticResult.WARNING,
                    "No C# files found in project"
                )
            }
        } catch (e: Exception) {
            return DiagnosticCheck(
                "Project Structure",
                DiagnosticResult.ERROR,
                "Failed to check project structure: ${e.message}"
            )
        }
    }
    
    /**
     * 显示诊断报告
     */
    fun showDiagnosticReport(report: DiagnosticReport) {
        ApplicationManager.getApplication().invokeLater {
            val message = StringBuilder()
            message.append("OmniSharp Diagnostics Report\n\n")
            message.append("Overall Status: ${if (report.allSuccessful) "SUCCESS" else "FAILED"}\n\n")
            
            for (check in report.checks) {
                message.append("${check.name}: ${check.result}\n")
                message.append("  ${check.details}\n\n")
            }
            
            val textArea = JTextArea(message.toString())
            textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
            textArea.isEditable = false
            textArea.lineWrap = false
            textArea.caretPosition = 0
            
            val scrollPane = JScrollPane(textArea)
            scrollPane.preferredSize = Dimension(800, 600)
            
            JOptionPane.showMessageDialog(
                project.currentMainFrame,
                scrollPane,
                "OmniSharp Diagnostics",
                if (report.allSuccessful) JOptionPane.INFORMATION_MESSAGE else JOptionPane.WARNING_MESSAGE
            )
        }
    }
    
    /**
     * 诊断报告
     */
    data class DiagnosticReport(
        val allSuccessful: Boolean,
        val checks: List<DiagnosticCheck>
    )
    
    /**
     * 诊断检查
     */
    data class DiagnosticCheck(
        val name: String,
        val result: DiagnosticResult,
        val details: String
    )
    
    /**
     * 诊断结果
     */
    enum class DiagnosticResult {
        SUCCESS,
        WARNING,
        FAILURE,
        ERROR,
        SKIPPED
    }
}
```

### 6.2 调试模式支持

```kotlin
/**
 * OmniSharp调试管理器
 */
class OmniSharpDebugManager(private val project: Project) {
    
    private val logger = thisLogger()
    private var debugMode = false
    private val debugInfo = mutableListOf<String>()
    
    /**
     * 启用调试模式
     */
    fun enableDebugMode() {
        debugMode = true
        val logManager = IOmniSharpLogManager.getInstance(project)
        logManager.setLogLevel(LogLevel.DEBUG)
        logger.debug("Debug mode enabled")
    }
    
    /**
     * 禁用调试模式
     */
    fun disableDebugMode() {
        debugMode = false
        val logManager = IOmniSharpLogManager.getInstance(project)
        logManager.setLogLevel(LogLevel.INFO)
        logger.debug("Debug mode disabled")
    }
    
    /**
     * 检查是否启用了调试模式
     */
    fun isDebugModeEnabled(): Boolean {
        return debugMode
    }
    
    /**
     * 记录调试信息
     */
    fun debug(message: String) {
        if (debugMode) {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(Date())
            val threadName = Thread.currentThread().name
            val debugMessage = "[$timestamp] [$threadName] $message"
            
            synchronized(debugInfo) {
                debugInfo.add(debugMessage)
                // 限制调试信息数量
                if (debugInfo.size > 10000) {
                    debugInfo.removeFirst()
                }
            }
            
            logger.debug(message)
        }
    }
    
    /**
     * 获取调试信息
     */
    fun getDebugInfo(): List<String> {
        synchronized(debugInfo) {
            return debugInfo.toList()
        }
    }
    
    /**
     * 清除调试信息
     */
    fun clearDebugInfo() {
        synchronized(debugInfo) {
            debugInfo.clear()
        }
    }
    
    /**
     * 记录通信消息
     */
    fun logCommunicationMessage(direction: CommunicationDirection, message: String) {
        if (debugMode) {
            val prefix = when (direction) {
                CommunicationDirection.SEND -> "[SEND]"
                CommunicationDirection.RECEIVE -> "[RECV]"
            }
            debug("$prefix $message")
        }
    }
    
    /**
     * 通信方向枚举
     */
    enum class CommunicationDirection {
        SEND,
        RECEIVE
    }
}
```

## 7. 性能监控

### 7.1 性能指标收集

```kotlin
/**
 * OmniSharp性能监控器
 */
class OmniSharpPerformanceMonitor(private val project: Project) {
    
    private val logger = thisLogger()
    private val metrics = mutableMapOf<String, Metric>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 开始计时
     */
    fun startTimer(name: String) {
        lock.writeLock().lock()
        try {
            val metric = metrics.getOrPut(name) { Metric(name) }
            metric.startTimer()
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 结束计时
     */
    fun stopTimer(name: String): Long {
        lock.writeLock().lock()
        try {
            val metric = metrics[name]
            return if (metric != null) {
                metric.stopTimer()
            } else {
                logger.warn("Trying to stop non-existent timer: $name")
                -1L
            }
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 记录请求计数
     */
    fun incrementCounter(name: String, delta: Int = 1) {
        lock.writeLock().lock()
        try {
            val metric = metrics.getOrPut(name) { Metric(name) }
            metric.incrementCounter(delta)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 记录数值
     */
    fun recordValue(name: String, value: Double) {
        lock.writeLock().lock()
        try {
            val metric = metrics.getOrPut(name) { Metric(name) }
            metric.recordValue(value)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 获取所有指标
     */
    fun getAllMetrics(): Map<String, Metric> {
        lock.readLock().lock()
        try {
            return metrics.toMap()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 获取指标
     */
    fun getMetric(name: String): Metric? {
        lock.readLock().lock()
        try {
            return metrics[name]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 清除所有指标
     */
    fun clearMetrics() {
        lock.writeLock().lock()
        try {
            metrics.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 导出性能报告
     */
    fun exportPerformanceReport(): String {
        val report = StringBuilder()
        report.append("OmniSharp Performance Report\n")
        report.append("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}\n\n")
        
        lock.readLock().lock()
        try {
            for (metric in metrics.values) {
                report.append("Metric: ${metric.name}\n")
                report.append("  Count: ${metric.count}\n")
                
                if (metric.timerCount > 0) {
                    report.append("  Timer Count: ${metric.timerCount}\n")
                    report.append("  Avg Time: ${metric.avgTime}ms\n")
                    report.append("  Min Time: ${metric.minTime}ms\n")
                    report.append("  Max Time: ${metric.maxTime}ms\n")
                }
                
                if (metric.valueCount > 0) {
                    report.append("  Value Count: ${metric.valueCount}\n")
                    report.append("  Avg Value: ${metric.avgValue}\n")
                    report.append("  Min Value: ${metric.minValue}\n")
                    report.append("  Max Value: ${metric.maxValue}\n")
                }
                
                report.append("\n")
            }
        } finally {
            lock.readLock().unlock()
        }
        
        return report.toString()
    }
    
    /**
     * 性能指标类
     */
    data class Metric(val name: String) {
        var count: Int = 0
        var timerCount: Int = 0
        var minTime: Long = Long.MAX_VALUE
        var maxTime: Long = Long.MIN_VALUE
        var totalTime: Long = 0
        var startTime: Long = 0
        var valueCount: Int = 0
        var minValue: Double = Double.MAX_VALUE
        var maxValue: Double = Double.MIN_VALUE
        var totalValue: Double = 0.0
        
        val avgTime: Double
            get() = if (timerCount > 0) totalTime.toDouble() / timerCount else 0.0
        
        val avgValue: Double
            get() = if (valueCount > 0) totalValue / valueCount else 0.0
        
        fun startTimer() {
            startTime = System.currentTimeMillis()
        }
        
        fun stopTimer(): Long {
            val duration = System.currentTimeMillis() - startTime
            
            timerCount++
            count++
            minTime = minOf(minTime, duration)
            maxTime = maxOf(maxTime, duration)
            totalTime += duration
            
            return duration
        }
        
        fun incrementCounter(delta: Int) {
            count += delta
        }
        
        fun recordValue(value: Double) {
            valueCount++
            minValue = minOf(minValue, value)
            maxValue = maxOf(maxValue, value)
            totalValue += value
        }
    }
}
```

## 8. 小结

本文档详细设计了OmniSharp服务器的错误处理和日志记录机制。错误处理部分定义了完整的异常体系和错误代码系统，实现了全局异常处理器，能够根据不同类型的错误采取适当的处理策略。日志记录部分设计了多级日志机制，支持不同级别的日志记录，实现了日志查看器UI，方便用户查看和导出日志。错误反馈机制提供了用户通知和错误报告功能，能够及时向用户反馈错误情况并收集错误报告。故障诊断和调试支持则提供了全面的诊断工具和调试模式，方便开发者和用户进行问题诊断和调试。性能监控功能能够收集和记录各种性能指标，为性能优化提供依据。

这些机制的实现将大大提高OmniSharp插件的稳定性、可靠性和可维护性，使用户能够更好地使用插件进行C#开发，同时也为开发者提供了良好的工具进行问题诊断和修复。