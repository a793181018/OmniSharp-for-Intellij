# OmniSharp代码诊断功能集成实现方案

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [核心组件实现](#核心组件实现)
4. [诊断请求和处理](#诊断请求和处理)
5. [诊断结果展示](#诊断结果展示)
6. [快速修复实现](#快速修复实现)
7. [诊断规则配置](#诊断规则配置)
8. [缓存策略](#缓存策略)
9. [错误处理](#错误处理)
10. [性能优化](#性能优化)
11. [测试策略](#测试策略)
12. [使用示例](#使用示例)
13. [总结](#总结)

## 概述

本文档详细描述OmniSharp代码诊断功能在IntelliJ平台中的集成实现方案。代码诊断是IDE提供的重要功能，可以帮助开发者在开发过程中及时发现和修复代码中的潜在问题、错误和不符合最佳实践的地方。本实现方案将OmniSharp服务器提供的代码诊断能力与IntelliJ的检查和标注系统无缝集成，为开发者提供实时、准确的代码质量反馈。

### 功能特性

- **实时诊断**：编辑过程中实时分析代码并显示诊断结果
- **错误和警告**：区分不同级别的问题（错误、警告、信息）
- **诊断范围**：支持当前文件和整个项目的诊断
- **快速修复**：为诊断问题提供自动修复建议
- **诊断过滤**：支持按类型、严重程度过滤诊断结果
- **代码高亮**：在编辑器中高亮显示问题代码
- **行内提示**：在行号旁或代码下方显示诊断信息
- **诊断面板**：集中展示所有诊断结果，支持跳转

## 架构设计

### 组件关系图

```
+------------------------------------------+
|                                          |
|            IntelliJ编辑器                |
|                                          |
+---------------------+--------------------+
                      |
                      v
+------------------------------------------+
|          DiagnosticService               |
+---------------------+--------------------+
                      |
     +----------------+------------------+
     |                 |                  |
     v                 v                  v
+-----------------+-----------------+------------------+
|DiagnosticAnalyzer|QuickFixProvider | DiagnosticCache |
+-----------------+-----------------+------------------+
                  |                 |
                  v                 v
+------------------------------------------+
|            OmniSharpCommunicator         |
+------------------------------------------+
                      |
                      v
+------------------------------------------+
|             OmniSharp服务器              |
+------------------------------------------+
```

### 诊断流程

1. **触发阶段**：通过文件保存、编辑器失焦、手动触发等方式启动诊断
2. **请求阶段**：向OmniSharp服务器发送诊断请求
3. **处理阶段**：接收诊断结果，转换为IntelliJ的ProblemDescriptor格式
4. **展示阶段**：在编辑器中显示诊断结果，并更新诊断工具窗口
5. **修复阶段**：提供快速修复建议，用户可以选择应用修复

## 核心组件实现

### 1. DiagnosticService

DiagnosticService是代码诊断功能的核心服务类，负责协调诊断分析、结果处理和展示。

```kotlin
class DiagnosticService : Disposable {
    private val communicatorFactory: OmniSharpCommunicatorFactory
    private val sessionManager: SessionManager
    private val configurationManager: ConfigurationManager
    private val diagnosticCache: DiagnosticCache
    private val quickFixRegistry: QuickFixRegistry
    private val diagnosticProcessors: Map<DiagnosticSeverity, DiagnosticProcessor>
    private val logger: Logger
    
    constructor(
        communicatorFactory: OmniSharpCommunicatorFactory,
        sessionManager: SessionManager,
        configurationManager: ConfigurationManager,
        quickFixRegistry: QuickFixRegistry
    ) {
        this.communicatorFactory = communicatorFactory
        this.sessionManager = sessionManager
        this.configurationManager = configurationManager
        this.diagnosticCache = DiagnosticCache(configurationManager.getConfiguration("diagnostics.cache.size", 100))
        this.quickFixRegistry = quickFixRegistry
        this.logger = Logger.getInstance("DiagnosticService")
        
        // 初始化诊断处理器
        this.diagnosticProcessors = mapOf(
            DiagnosticSeverity.ERROR to ErrorDiagnosticProcessor(),
            DiagnosticSeverity.WARNING to WarningDiagnosticProcessor(),
            DiagnosticSeverity.INFO to InfoDiagnosticProcessor()
        )
        
        // 注册文件变更监听器
        registerFileChangeListener()
    }
    
    /**
     * 分析单个文件的诊断信息
     */
    fun analyzeFile(
        project: Project,
        file: VirtualFile,
        document: Document
    ): CompletableFuture<List<ProblemDescriptor>> {
        // 检查是否启用了诊断功能
        if (!configurationManager.getConfiguration("diagnostics.enabled", true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 验证文件是否为C#文件
        if (!file.fileType.name.equals("C#", ignoreCase = true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 构建缓存键
        val cacheKey = DiagnosticCacheKey(file.path, document.text.hashCode())
        
        // 检查缓存
        val cachedDiagnostics = diagnosticCache.getDiagnostics(cacheKey)
        if (cachedDiagnostics != null) {
            logger.debug("Diagnostic cache hit for ${file.name}")
            return CompletableFuture.completedFuture(cachedDiagnostics)
        }
        
        // 获取当前会话
        val session = sessionManager.getSession(project)
        if (session == null || !session.isAlive) {
            return CompletableFuture.failedFuture(IllegalStateException("OmniSharp session not available"))
        }
        
        // 构建请求参数
        val requestParams = mapOf(
            "FileName" to file.path,
            "AnalyzeOpenDocumentsOnly" to configurationManager.getConfiguration("diagnostics.analyze.open.documents.only", false),
            "ExcludeNonexistentFiles" to true
        )
        
        logger.debug("Sending diagnostics request for ${file.name}")
        
        // 发送请求到OmniSharp服务器
        return session.communicator.sendRequest("v2/diagnostics", requestParams)
            .thenApply { response ->
                // 处理响应
                val diagnostics = processDiagnosticsResponse(response, file, document)
                
                // 更新缓存
                diagnosticCache.putDiagnostics(cacheKey, diagnostics)
                
                diagnostics
            }
            .exceptionally { ex ->
                logger.warn("Error analyzing file diagnostics", ex)
                emptyList()
            }
    }
    
    /**
     * 分析整个项目的诊断信息
     */
    fun analyzeProject(project: Project): CompletableFuture<List<ProblemDescriptor>> {
        // 检查是否启用了项目级诊断
        if (!configurationManager.getConfiguration("diagnostics.project.enabled", false)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 获取当前会话
        val session = sessionManager.getSession(project)
        if (session == null || !session.isAlive) {
            return CompletableFuture.failedFuture(IllegalStateException("OmniSharp session not available"))
        }
        
        // 构建请求参数
        val requestParams = mapOf(
            "AnalyzeOpenDocumentsOnly" to false,
            "ExcludeNonexistentFiles" to true
        )
        
        logger.debug("Sending project diagnostics request")
        
        // 发送请求到OmniSharp服务器
        return session.communicator.sendRequest("v2/diagnostics", requestParams)
            .thenApply { response ->
                // 处理响应
                processProjectDiagnosticsResponse(response, project)
            }
            .exceptionally { ex ->
                logger.warn("Error analyzing project diagnostics", ex)
                emptyList()
            }
    }
    
    /**
     * 处理单个文件的诊断响应
     */
    private fun processDiagnosticsResponse(
        response: OmniSharpResponse,
        file: VirtualFile,
        document: Document
    ): List<ProblemDescriptor> {
        try {
            val problems = mutableListOf<ProblemDescriptor>()
            
            // 检查响应是否包含诊断数据
            if (response.success && response.body is Map<*, *>) {
                val body = response.body as Map<String, Any>
                val results = body["Results"] as? List<Map<String, Any>> ?: emptyList()
                
                for (result in results) {
                    val fileName = result["FileName"] as? String ?: continue
                    
                    // 只处理当前文件的诊断结果
                    if (fileName != file.path) continue
                    
                    val diagnostics = result["Diagnostics"] as? List<Map<String, Any>> ?: emptyList()
                    
                    for (diagnostic in diagnostics) {
                        val problem = createProblemDescriptor(diagnostic, document)
                        if (problem != null) {
                            problems.add(problem)
                        }
                    }
                }
            }
            
            return problems
        } catch (e: Exception) {
            logger.warn("Error processing diagnostics response", e)
            return emptyList()
        }
    }
    
    /**
     * 处理项目诊断响应
     */
    private fun processProjectDiagnosticsResponse(
        response: OmniSharpResponse,
        project: Project
    ): List<ProblemDescriptor> {
        try {
            val problems = mutableListOf<ProblemDescriptor>()
            
            // 检查响应是否包含诊断数据
            if (response.success && response.body is Map<*, *>) {
                val body = response.body as Map<String, Any>
                val results = body["Results"] as? List<Map<String, Any>> ?: emptyList()
                
                val psiManager = PsiManager.getInstance(project)
                val fileSystem = LocalFileSystem.getInstance()
                
                for (result in results) {
                    val fileName = result["FileName"] as? String ?: continue
                    val file = fileSystem.findFileByPath(fileName)
                    
                    if (file != null) {
                        val psiFile = psiManager.findFile(file)
                        if (psiFile != null) {
                            val document = psiFile.viewProvider.document
                            if (document != null) {
                                val diagnostics = result["Diagnostics"] as? List<Map<String, Any>> ?: emptyList()
                                
                                for (diagnostic in diagnostics) {
                                    val problem = createProblemDescriptor(diagnostic, document)
                                    if (problem != null) {
                                        problems.add(problem)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            return problems
        } catch (e: Exception) {
            logger.warn("Error processing project diagnostics response", e)
            return emptyList()
        }
    }
    
    /**
     * 创建问题描述符
     */
    private fun createProblemDescriptor(
        diagnostic: Map<String, Any>,
        document: Document
    ): ProblemDescriptor? {
        try {
            // 解析诊断信息
            val message = diagnostic["Message"] as? String ?: ""
            val severity = getSeverity(diagnostic)
            val code = diagnostic["Code"] as? String ?: ""
            val source = diagnostic["Source"] as? String ?: "OmniSharp"
            
            // 解析位置信息
            val startLine = (diagnostic["StartLine"] as? Int ?: 0) - 1  // 转换为0-based
            val startColumn = (diagnostic["StartColumn"] as? Int ?: 0) - 1  // 转换为0-based
            val endLine = (diagnostic["EndLine"] as? Int ?: startLine) - 1
            val endColumn = (diagnostic["EndColumn"] as? Int ?: startColumn) - 1
            
            // 计算偏移量
            val startOffset = document.getLineStartOffset(startLine) + startColumn
            val endOffset = document.getLineStartOffset(endLine) + endColumn
            
            // 创建问题描述符
            return ProblemDescriptorImpl(
                startOffset,
                endOffset,
                message,
                getHighlightInfoType(severity),
                null,  // 快速修复稍后添加
                false,  // 不显示在编辑器中，我们将使用自定义标注
                null,  // 导航目标
                ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
            )
        } catch (e: Exception) {
            logger.warn("Error creating problem descriptor", e)
            return null
        }
    }
    
    /**
     * 获取严重程度
     */
    private fun getSeverity(diagnostic: Map<String, Any>): DiagnosticSeverity {
        val severityText = (diagnostic["Severity"] as? String ?: "").lowercase()
        return when {
            severityText.contains("error") -> DiagnosticSeverity.ERROR
            severityText.contains("warning") -> DiagnosticSeverity.WARNING
            else -> DiagnosticSeverity.INFO
        }
    }
    
    /**
     * 获取高亮信息类型
     */
    private fun getHighlightInfoType(severity: DiagnosticSeverity): HighlightInfoType {
        return when (severity) {
            DiagnosticSeverity.ERROR -> HighlightInfoType.ERROR
            DiagnosticSeverity.WARNING -> HighlightInfoType.WARNING
            DiagnosticSeverity.INFO -> HighlightInfoType.INFORMATION
        }
    }
    
    /**
     * 注册文件变更监听器
     */
    private fun registerFileChangeListener() {
        FileDocumentManager.getInstance().addDocumentListener(object : FileDocumentManagerListener {
            override fun beforeDocumentSaving(document: Document) {
                val file = FileDocumentManager.getInstance().getFile(document)
                if (file != null && file.fileType.name.equals("C#", ignoreCase = true)) {
                    // 当文件保存时，清除缓存以确保下次分析获取最新结果
                    diagnosticCache.clearFileCache(file.path)
                }
            }
        })
    }
    
    /**
     * 获取可用的快速修复
     */
    fun getQuickFixes(problem: ProblemDescriptor): List<QuickFix> {
        return quickFixRegistry.getQuickFixes(problem)
    }
    
    /**
     * 清理资源
     */
    override fun dispose() {
        diagnosticCache.clear()
    }
}

/**
 * 诊断严重程度
 */
enum class DiagnosticSeverity {
    ERROR,
    WARNING,
    INFO
}
```

### 2. DiagnosticAnalyzer

DiagnosticAnalyzer实现了IntelliJ的LocalInspectionTool接口，在编辑器中提供实时诊断。

```kotlin
class OmniSharpDiagnosticAnalyzer : LocalInspectionTool() {
    private val diagnosticService: DiagnosticService
    private val configurationManager: ConfigurationManager
    private val logger: Logger
    
    init {
        this.diagnosticService = ServiceManager.getService(DiagnosticService::class.java)
        this.configurationManager = ServiceManager.getService(ConfigurationManager::class.java)
        this.logger = Logger.getInstance("OmniSharpDiagnosticAnalyzer")
    }
    
    override fun getDisplayName(): String {
        return "OmniSharp Code Diagnostics"
    }
    
    override fun getShortName(): String {
        return "OmniSharpDiagnostics"
    }
    
    override fun getGroupDisplayName(): String {
        return "OmniSharp"
    }
    
    override fun isEnabledByDefault(): Boolean {
        return configurationManager.getConfiguration("diagnostics.enabled", true)
    }
    
    override fun runForWholeFile(): Boolean {
        return true
    }
    
    override fun checkFile(
        file: PsiFile,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): List<ProblemDescriptor>? {
        // 检查是否启用了诊断功能
        if (!isEnabledByDefault()) {
            return null
        }
        
        // 检查是否为C#文件
        if (!file.virtualFile.fileType.name.equals("C#", ignoreCase = true)) {
            return null
        }
        
        // 获取文档
        val document = FileDocumentManager.getInstance().getDocument(file.virtualFile)
        if (document == null) {
            logger.warn("Document not found for file: ${file.virtualFile.path}")
            return null
        }
        
        try {
            // 分析文件
            val future = diagnosticService.analyzeFile(file.project, file.virtualFile, document)
            val problems = future.get(10000, TimeUnit.MILLISECONDS)  // 10秒超时
            
            // 添加快速修复
            val problemsWithFixes = problems.map { problem ->
                val quickFixes = diagnosticService.getQuickFixes(problem)
                if (quickFixes.isNotEmpty()) {
                    ProblemDescriptorImpl(
                        problem.startOffset,
                        problem.endOffset,
                        problem.descriptionTemplate,
                        problem.highlightType,
                        quickFixes.toTypedArray(),
                        problem.isAfterEndOfLine,
                        problem.navigationElement,
                        problem.problemHighlightType
                    )
                } else {
                    problem
                }
            }
            
            return problemsWithFixes
        } catch (e: Exception) {
            logger.warn("Error running diagnostic analysis", e)
            return null
        }
    }
    
    override fun isSuppressedFor(element: PsiElement): Boolean {
        // 支持通过注释抑制诊断
        return checkSuppression(element)
    }
    
    /**
     * 检查是否抑制诊断
     */
    private fun checkSuppression(element: PsiElement): Boolean {
        // 查找相关注释
        val commentLines = findCommentLines(element)
        
        // 检查是否包含抑制注释（如 // ReSharper disable once ... 或 // Diagnostic disable ...）
        return commentLines.any { 
            it.contains("ReSharper disable once") || 
            it.contains("Diagnostic disable") ||
            it.contains("omnisharp disable next line")
        }
    }
    
    /**
     * 查找相关注释行
     */
    private fun findCommentLines(element: PsiElement): List<String> {
        val comments = mutableListOf<String>()
        var currentElement: PsiElement? = element
        
        // 查找当前行和前一行的注释
        while (currentElement != null && currentElement is PsiComment) {
            comments.add(currentElement.text)
            currentElement = currentElement.prevSibling
        }
        
        // 查找父元素中的相关注释
        var parent = element.parent
        while (parent != null && comments.size < 5) {  // 最多检查5个父元素
            for (child in parent.children) {
                if (child is PsiComment && child.textRange.startOffset < element.textRange.startOffset) {
                    comments.add(child.text)
                }
            }
            parent = parent.parent
        }
        
        return comments
    }
}
```

### 3. DiagnosticHighlightRenderer

DiagnosticHighlightRenderer负责在编辑器中渲染诊断标注。

```kotlin
class DiagnosticHighlightRenderer(private val diagnosticService: DiagnosticService) {
    private val highlightManager = HighlightManager.getInstance(null) // 将在使用时注入项目
    private val logger: Logger = Logger.getInstance("DiagnosticHighlightRenderer")
    
    /**
     * 渲染诊断标注
     */
    fun renderDiagnostics(
        project: Project,
        editor: Editor,
        file: VirtualFile,
        diagnostics: List<ProblemDescriptor>
    ) {
        ApplicationManager.getApplication().invokeLater {
            val highlighter = editor.markupModel
            
            // 移除旧的诊断标注
            removeOldDiagnostics(highlighter)
            
            // 添加新的诊断标注
            for (problem in diagnostics) {
                try {
                    // 创建高亮信息
                    val highlightInfo = createHighlightInfo(problem)
                    if (highlightInfo != null) {
                        highlighter.addLineMarkerInfo(
                            problem.lineMarkerRange,
                            getGutterIcon(problem),
                            TooltipRenderer { problem.descriptionTemplate },
                            createGroup(),
                            navigateToProblem(editor, problem),
                            GutterIconRenderer.Alignment.RIGHT
                        )
                        
                        // 添加文本高亮
                        highlighter.addRangeHighlighter(
                            problem.startOffset,
                            problem.endOffset,
                            getHighlightLayer(problem),
                            getTextAttributes(problem),
                            HighlighterTargetArea.EXACT_RANGE
                        )
                    }
                } catch (e: Exception) {
                    logger.warn("Error rendering diagnostic highlight", e)
                }
            }
        }
    }
    
    /**
     * 创建高亮信息
     */
    private fun createHighlightInfo(problem: ProblemDescriptor): HighlightInfo? {
        return HighlightInfo.newHighlightInfo(getHighlightInfoType(problem))
            .range(problem.textRange)
            .descriptionAndTooltip(problem.descriptionTemplate)
            .create()
    }
    
    /**
     * 获取高亮信息类型
     */
    private fun getHighlightInfoType(problem: ProblemDescriptor): HighlightInfoType {
        return when (problem.highlightType) {
            ProblemHighlightType.ERROR -> HighlightInfoType.ERROR
            ProblemHighlightType.WARNING -> HighlightInfoType.WARNING
            else -> HighlightInfoType.INFORMATION
        }
    }
    
    /**
     * 获取装订线图标
     */
    private fun getGutterIcon(problem: ProblemDescriptor): Icon {
        return when (problem.highlightType) {
            ProblemHighlightType.ERROR -> AllIcons.General.Error
            ProblemHighlightType.WARNING -> AllIcons.General.Warning
            else -> AllIcons.General.Information
        }
    }
    
    /**
     * 获取高亮层
     */
    private fun getHighlightLayer(problem: ProblemDescriptor): Int {
        return when (problem.highlightType) {
            ProblemHighlightType.ERROR -> HighlighterLayer.ERROR
            ProblemHighlightType.WARNING -> HighlighterLayer.WARNING
            else -> HighlighterLayer.INFO
        }
    }
    
    /**
     * 获取文本属性
     */
    private fun getTextAttributes(problem: ProblemDescriptor): TextAttributes? {
        return when (problem.highlightType) {
            ProblemHighlightType.ERROR -> TextAttributes.ERRORS_ATTRIBUTES
            ProblemHighlightType.WARNING -> TextAttributes.WARNINGS_ATTRIBUTES
            else -> TextAttributes.INFO_ATTRIBUTES
        }
    }
    
    /**
     * 创建装订线图标组
     */
    private fun createGroup(): HighlightInfoType.HighlightInfoTypeImpl {
        return HighlightInfoType.HighlightInfoTypeImpl("OMNISHARP_DIAGNOSTICS", HighlighterLayer.ERROR, null)
    }
    
    /**
     * 创建导航动作
     */
    private fun navigateToProblem(editor: Editor, problem: ProblemDescriptor): GutterIconNavigationHandler<Any> {
        return GutterIconNavigationHandler<Any> { e, elt ->
            editor.caretModel.moveToOffset(problem.startOffset)
            editor.selectionModel.setSelection(problem.startOffset, problem.endOffset)
        }
    }
    
    /**
     * 移除旧的诊断标注
     */
    private fun removeOldDiagnostics(markupModel: MarkupModel) {
        markupModel.processRangeHighlighters { highlighter ->
            if (highlighter.layer == HighlighterLayer.ERROR || 
                highlighter.layer == HighlighterLayer.WARNING ||
                highlighter.layer == HighlighterLayer.INFO) {
                markupModel.removeHighlighter(highlighter)
            }
            true
        }
        
        // 移除装订线图标
        markupModel.removeAllLineMarkers()
    }
}
```

### 4. DiagnosticToolWindowFactory

DiagnosticToolWindowFactory创建并管理诊断工具窗口，用于集中显示诊断结果。

```kotlin
class DiagnosticToolWindowFactory : ToolWindowFactory {
    private val diagnosticService: DiagnosticService
    private val configurationManager: ConfigurationManager
    
    init {
        this.diagnosticService = ServiceManager.getService(DiagnosticService::class.java)
        this.configurationManager = ServiceManager.getService(ConfigurationManager::class.java)
    }
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        val content = createDiagnosticContent(project, toolWindow)
        contentManager.addContent(content)
    }
    
    /**
     * 创建诊断内容
     */
    private fun createDiagnosticContent(project: Project, toolWindow: ToolWindow): Content {
        val panel = JPanel(BorderLayout())
        
        // 创建工具栏
        val toolbar = createToolbar(project, toolWindow)
        panel.add(toolbar, BorderLayout.NORTH)
        
        // 创建过滤器面板
        val filterPanel = createFilterPanel()
        panel.add(filterPanel, BorderLayout.WEST)
        
        // 创建诊断结果列表
        val resultList = createResultList(project, toolWindow)
        panel.add(resultList, BorderLayout.CENTER)
        
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, "Diagnostics", false)
        content.setDisposer { /* 清理资源 */ }
        
        return content
    }
    
    /**
     * 创建工具栏
     */
    private fun createToolbar(project: Project, toolWindow: ToolWindow): JComponent {
        val toolbar = JPanel(FlowLayout(FlowLayout.LEFT))
        
        // 刷新按钮
        val refreshButton = JButton("Refresh", AllIcons.Actions.Refresh)
        refreshButton.addActionListener {
            refreshDiagnostics(project, toolWindow)
        }
        toolbar.add(refreshButton)
        
        // 分析范围选择
        val scopeLabel = JLabel("Scope:")
        val scopeComboBox = JComboBox(arrayOf("Current File", "Project"))
        scopeComboBox.selectedIndex = if (configurationManager.getConfiguration("diagnostics.analyze.current.file", true)) 0 else 1
        scopeComboBox.addActionListener {
            configurationManager.setConfiguration("diagnostics.analyze.current.file", scopeComboBox.selectedIndex == 0)
        }
        toolbar.add(scopeLabel)
        toolbar.add(scopeComboBox)
        
        return toolbar
    }
    
    /**
     * 创建过滤器面板
     */
    private fun createFilterPanel(): JComponent {
        val panel = JPanel(GridLayout(0, 1))
        panel.border = BorderFactory.createTitledBorder("Filters")
        
        // 严重程度过滤器
        val errorCheckBox = JCheckBox("Errors", true)
        val warningCheckBox = JCheckBox("Warnings", true)
        val infoCheckBox = JCheckBox("Info", false)
        
        errorCheckBox.addItemListener {
            configurationManager.setConfiguration("diagnostics.filter.error", errorCheckBox.isSelected)
        }
        
        warningCheckBox.addItemListener {
            configurationManager.setConfiguration("diagnostics.filter.warning", warningCheckBox.isSelected)
        }
        
        infoCheckBox.addItemListener {
            configurationManager.setConfiguration("diagnostics.filter.info", infoCheckBox.isSelected)
        }
        
        panel.add(errorCheckBox)
        panel.add(warningCheckBox)
        panel.add(infoCheckBox)
        
        return panel
    }
    
    /**
     * 创建诊断结果列表
     */
    private fun createResultList(project: Project, toolWindow: ToolWindow): JComponent {
        val listModel = DefaultListModel<DiagnosticItem>()
        val resultList = JList(listModel)
        resultList.cellRenderer = DiagnosticItemRenderer()
        
        // 添加选择监听器，导航到选中的诊断
        resultList.addListSelectionListener {\ e ->
            if (!e.valueIsAdjusting) {
                val selectedItem = resultList.selectedValue
                if (selectedItem != null) {
                    navigateToDiagnostic(project, selectedItem)
                }
            }
        }
        
        // 初始加载诊断
        loadDiagnostics(project, listModel)
        
        return JScrollPane(resultList)
    }
    
    /**
     * 加载诊断结果
     */
    private fun loadDiagnostics(project: Project, listModel: DefaultListModel<DiagnosticItem>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val editor = FileEditorManager.getInstance(project).selectedTextEditor
                val file = editor?.virtualFile
                val document = editor?.document
                
                val diagnostics = if (file != null && document != null && configurationManager.getConfiguration("diagnostics.analyze.current.file", true)) {
                    diagnosticService.analyzeFile(project, file, document).get()
                } else {
                    diagnosticService.analyzeProject(project).get()
                }
                
                // 应用过滤器
                val filteredDiagnostics = applyFilters(diagnostics)
                
                // 更新列表
                ApplicationManager.getApplication().invokeLater {
                    listModel.clear()
                    filteredDiagnostics.forEach { problem ->
                        listModel.addElement(DiagnosticItem(problem))
                    }
                }
            } catch (e: Exception) {
                Logger.getInstance(javaClass).warn("Error loading diagnostics", e)
            }
        }
    }
    
    /**
     * 刷新诊断
     */
    private fun refreshDiagnostics(project: Project, toolWindow: ToolWindow) {
        val content = toolWindow.contentManager.getContent(0)
        if (content != null) {
            val component = content.component
            // 查找并更新诊断结果列表
            updateDiagnosticsList(component, project)
        }
    }
    
    /**
     * 更新诊断结果列表
     */
    private fun updateDiagnosticsList(component: JComponent, project: Project) {
        // 递归查找诊断结果列表
        if (component is JScrollPane && component.viewport.view is JList<*>) {
            val list = component.viewport.view as JList<DiagnosticItem>
            val model = list.model as DefaultListModel<DiagnosticItem>
            loadDiagnostics(project, model)
        } else {
            for (child in component.components) {
                if (child is JComponent) {
                    updateDiagnosticsList(child, project)
                }
            }
        }
    }
    
    /**
     * 应用过滤器
     */
    private fun applyFilters(diagnostics: List<ProblemDescriptor>): List<ProblemDescriptor> {
        val showErrors = configurationManager.getConfiguration("diagnostics.filter.error", true)
        val showWarnings = configurationManager.getConfiguration("diagnostics.filter.warning", true)
        val showInfo = configurationManager.getConfiguration("diagnostics.filter.info", false)
        
        return diagnostics.filter { problem ->
            when (problem.highlightType) {
                ProblemHighlightType.ERROR -> showErrors
                ProblemHighlightType.WARNING -> showWarnings
                else -> showInfo
            }
        }
    }
    
    /**
     * 导航到诊断位置
     */
    private fun navigateToDiagnostic(project: Project, item: DiagnosticItem) {
        val problem = item.problem
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        
        if (editor != null) {
            editor.caretModel.moveToOffset(problem.startOffset)
            editor.selectionModel.setSelection(problem.startOffset, problem.endOffset)
        }
    }
    
    /**
     * 诊断项
     */
    private class DiagnosticItem(val problem: ProblemDescriptor) {
        override fun toString(): String {
            return problem.descriptionTemplate
        }
    }
    
    /**
     * 诊断项渲染器
     */
    private class DiagnosticItemRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
            
            if (value is DiagnosticItem) {
                val problem = value.problem
                
                // 设置文本
                text = problem.descriptionTemplate
                
                // 设置图标
                icon = when (problem.highlightType) {
                    ProblemHighlightType.ERROR -> AllIcons.General.Error
                    ProblemHighlightType.WARNING -> AllIcons.General.Warning
                    else -> AllIcons.General.Information
                }
                
                // 设置前景色
                foreground = when (problem.highlightType) {
                    ProblemHighlightType.ERROR -> JBColor.RED
                    ProblemHighlightType.WARNING -> JBColor.YELLOW.darker()
                    else -> JBColor.GRAY
                }
            }
            
            return component
        }
    }
}
```

### 5. QuickFixProvider

QuickFixProvider为诊断问题提供自动修复功能。

```kotlin
interface QuickFixProvider {
    /**
     * 获取适用于指定问题的快速修复
     */
    fun getQuickFixes(problem: ProblemDescriptor): List<QuickFix>
    
    /**
     * 检查是否支持指定的问题
     */
    fun isApplicable(problem: ProblemDescriptor): Boolean
}

class OmniSharpQuickFixProvider(
    private val sessionManager: SessionManager,
    private val diagnosticService: DiagnosticService
) : QuickFixProvider {
    private val logger: Logger = Logger.getInstance("OmniSharpQuickFixProvider")
    
    override fun getQuickFixes(problem: ProblemDescriptor): List<QuickFix> {
        val fixes = mutableListOf<QuickFix>()
        
        // 添加通用的快速修复
        fixes.add(SuppressDiagnosticQuickFix(problem))
        
        // 基于问题描述添加特定的快速修复
        if (isImportMissingProblem(problem)) {
            fixes.add(AddImportQuickFix(problem))
        }
        
        if (isVariableNotInitializedProblem(problem)) {
            fixes.add(InitializeVariableQuickFix(problem))
        }
        
        if (isFieldCanBeReadOnlyProblem(problem)) {
            fixes.add(MakeFieldReadOnlyQuickFix(problem))
        }
        
        // 可以添加更多特定的快速修复...
        
        return fixes
    }
    
    override fun isApplicable(problem: ProblemDescriptor): Boolean {
        // 对所有问题都适用
        return true
    }
    
    /**
     * 检查是否为导入缺失问题
     */
    private fun isImportMissingProblem(problem: ProblemDescriptor): Boolean {
        val message = problem.descriptionTemplate.lowercase()
        return message.contains("missing using directive") || 
               message.contains("the type or namespace name") ||
               message.contains("cannot be found")
    }
    
    /**
     * 检查是否为变量未初始化问题
     */
    private fun isVariableNotInitializedProblem(problem: ProblemDescriptor): Boolean {
        val message = problem.descriptionTemplate.lowercase()
        return message.contains("use of unassigned local variable") ||
               message.contains("might not be initialized")
    }
    
    /**
     * 检查是否为字段可设为只读问题
     */
    private fun isFieldCanBeReadOnlyProblem(problem: ProblemDescriptor): Boolean {
        val message = problem.descriptionTemplate.lowercase()
        return message.contains("field can be made readonly") ||
               message.contains("can be marked as readonly")
    }
}

/**
 * 快速修复注册表
 */
class QuickFixRegistry {
    private val providers: MutableList<QuickFixProvider> = mutableListOf()
    
    /**
     * 注册快速修复提供者
     */
    fun registerProvider(provider: QuickFixProvider) {
        providers.add(provider)
    }
    
    /**
     * 获取适用于指定问题的所有快速修复
     */
    fun getQuickFixes(problem: ProblemDescriptor): List<QuickFix> {
        val allFixes = mutableListOf<QuickFix>()
        
        for (provider in providers) {
            if (provider.isApplicable(problem)) {
                allFixes.addAll(provider.getQuickFixes(problem))
            }
        }
        
        return allFixes
    }
}

/**
 * 抑制诊断快速修复
 */
class SuppressDiagnosticQuickFix(private val problem: ProblemDescriptor) : QuickFix {    
    override fun getName(): String {
        return "Suppress this diagnostic"
    }
    
    override fun getFamilyName(): String {
        return "OmniSharp"
    }
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = editor.document
        val lineStartOffset = document.getLineStartOffset(document.getLineNumber(problem.startOffset))
        
        // 在问题行之前添加抑制注释
        ApplicationManager.getApplication().runWriteAction {
            document.insertString(lineStartOffset, "// omnisharp disable next line\n")
        }
    }
}

/**
 * 添加导入快速修复
 */
class AddImportQuickFix(private val problem: ProblemDescriptor) : QuickFix {
    private val logger: Logger = Logger.getInstance("AddImportQuickFix")
    
    override fun getName(): String {
        return "Add missing using directive"
    }
    
    override fun getFamilyName(): String {
        return "OmniSharp"
    }
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        // 解析问题描述以获取缺失的类型
        val missingType = extractMissingType(problem.descriptionTemplate)
        if (missingType.isNotEmpty()) {
            // 获取可能的命名空间
            val possibleNamespaces = getPossibleNamespaces(missingType)
            
            // 如果只有一个可能的命名空间，直接添加
            if (possibleNamespaces.size == 1) {
                addUsingDirective(project, possibleNamespaces.first())
            } else if (possibleNamespaces.size > 1) {
                // 否则显示选择对话框
                showNamespaceSelectionDialog(project, possibleNamespaces)
            }
        }
    }
    
    /**
     * 提取缺失的类型名称
     */
    private fun extractMissingType(message: String): String {
        // 简单的正则表达式匹配，实际实现可能需要更复杂的解析
        val pattern = "'([\\w\\.]+)'" .toRegex()
        val match = pattern.find(message)
        return match?.groups?.get(1)?.value ?: ""
    }
    
    /**
     * 获取可能的命名空间
     */
    private fun getPossibleNamespaces(typeName: String): List<String> {
        // 这里应该查询OmniSharp服务器获取可能的命名空间
        // 为简化示例，这里返回一些常见的命名空间
        return when (typeName) {
            "List" -> listOf("System.Collections.Generic")
            "Dictionary" -> listOf("System.Collections.Generic")
            "Console" -> listOf("System")
            "Exception" -> listOf("System")
            else -> listOf("System", "System.Collections.Generic", "System.Linq")
        }
    }
    
    /**
     * 添加using指令
     */
    private fun addUsingDirective(project: Project, namespace: String) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = editor.document
        
        // 查找所有using指令的结束位置
        var lastUsingOffset = 0
        val text = document.text
        val usingPattern = "using\\s+([\\w\\.]+);" .toRegex()
        
        for (match in usingPattern.findAll(text)) {
            lastUsingOffset = match.range.endInclusive + 1
        }
        
        // 如果没有using指令，在文件开头添加
        if (lastUsingOffset == 0) {
            lastUsingOffset = 0
        }
        
        ApplicationManager.getApplication().runWriteAction {
            document.insertString(lastUsingOffset, "using $namespace;\n")
        }
    }
    
    /**
     * 显示命名空间选择对话框
     */
    private fun showNamespaceSelectionDialog(project: Project, namespaces: List<String>) {
        ApplicationManager.getApplication().invokeLater {
            val result = Messages.showChooseDialog(
                project,
                "Choose namespace to import:",
                "Add Using Directive",
                namespaces.toTypedArray(),
                namespaces.first(),
                Messages.getQuestionIcon()
            )
            
            if (result >= 0 && result < namespaces.size) {
                addUsingDirective(project, namespaces[result])
            }
        }
    }
}

/**
 * 初始化变量快速修复
 */
class InitializeVariableQuickFix(private val problem: ProblemDescriptor) : QuickFix {
    override fun getName(): String {
        return "Initialize variable"
    }
    
    override fun getFamilyName(): String {
        return "OmniSharp"
    }
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = editor.document
        
        // 查找变量声明位置（简化示例，实际实现需要更复杂的解析）
        val variableName = extractVariableName(problem.descriptionTemplate)
        if (variableName.isNotEmpty()) {
            // 查找变量声明行
            val lineNumber = document.getLineNumber(problem.startOffset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
            
            // 替换变量声明，添加初始化
            val newLineText = initializeVariableInLine(lineText, variableName)
            
            ApplicationManager.getApplication().runWriteAction {
                document.replaceString(lineStartOffset, lineEndOffset, newLineText)
            }
        }
    }
    
    /**
     * 提取变量名
     */
    private fun extractVariableName(message: String): String {
        // 简单的正则表达式匹配，实际实现需要更复杂的解析
        val pattern = "variable\\s+'([\\w]+)'" .toRegex()
        val match = pattern.find(message.lowercase())
        return match?.groups?.get(1)?.value ?: ""
    }
    
    /**
     * 在行中初始化变量
     */
    private fun initializeVariableInLine(lineText: String, variableName: String): String {
        // 简单的替换逻辑，实际实现需要更复杂的解析
        if (lineText.contains("int $variableName")) {
            return lineText.replace("int $variableName", "int $variableName = 0")
        } else if (lineText.contains("string $variableName")) {
            return lineText.replace("string $variableName", "string $variableName = \"\"")
        } else if (lineText.contains("bool $variableName")) {
            return lineText.replace("bool $variableName", "bool $variableName = false")
        } else if (lineText.contains("var $variableName")) {
            return lineText.replace("var $variableName", "var $variableName = default")
        }
        // 对于其他类型，尝试在变量名后添加 = default
        return lineText.replace("$variableName", "$variableName = default")
    }
}

/**
 * 将字段设为只读快速修复
 */
class MakeFieldReadOnlyQuickFix(private val problem: ProblemDescriptor) : QuickFix {
    override fun getName(): String {
        return "Make field readonly"
    }
    
    override fun getFamilyName(): String {
        return "OmniSharp"
    }
    
    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
        val document = editor.document
        
        // 查找字段声明位置
        val lineNumber = document.getLineNumber(problem.startOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, lineEndOffset))
        
        // 在访问修饰符和类型之间添加readonly
        val newLineText = makeFieldReadOnlyInLine(lineText)
        
        ApplicationManager.getApplication().runWriteAction {
            document.replaceString(lineStartOffset, lineEndOffset, newLineText)
        }
    }
    
    /**
     * 在行中将字段设为只读
     */
    private fun makeFieldReadOnlyInLine(lineText: String): String {
        // 检查是否已有readonly
        if (lineText.contains("readonly")) {
            return lineText
        }
        
        // 查找修饰符后的位置插入readonly
        val pattern = "(public|private|protected|internal)?\s*(static)?\s*" .toRegex()
        val match = pattern.find(lineText)
        
        return if (match != null) {
            val before = lineText.substring(0, match.range.endInclusive + 1)
            val after = lineText.substring(match.range.endInclusive + 1)
            "$before readonly $after"
        } else {
            "readonly $lineText"
        }
    }
}
```

### 6. DiagnosticCache

DiagnosticCache实现了诊断结果的缓存，减少对OmniSharp服务器的请求次数。

```kotlin
class DiagnosticCache(private val maxSize: Int) {
    private val diagnosticCache: LinkedHashMap<DiagnosticCacheKey, List<ProblemDescriptor>>
    private val lock = ReentrantReadWriteLock()
    
    init {
        // 创建访问顺序的LinkedHashMap作为LRU缓存
        diagnosticCache = object : LinkedHashMap<DiagnosticCacheKey, List<ProblemDescriptor>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<DiagnosticCacheKey, List<ProblemDescriptor>>): Boolean {
                return size > maxSize
            }
        }
    }
    
    /**
     * 获取诊断缓存
     */
    fun getDiagnostics(key: DiagnosticCacheKey): List<ProblemDescriptor>? {
        lock.readLock().lock()
        try {
            return diagnosticCache[key]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 存储诊断缓存
     */
    fun putDiagnostics(key: DiagnosticCacheKey, value: List<ProblemDescriptor>) {
        lock.writeLock().lock()
        try {
            diagnosticCache[key] = value
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 清除特定文件的缓存
     */
    fun clearFileCache(filePath: String) {
        lock.writeLock().lock()
        try {
            val keysToRemove = diagnosticCache.keys.filter { it.filePath == filePath }
            keysToRemove.forEach { diagnosticCache.remove(it) }
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 清除所有缓存
     */
    fun clear() {
        lock.writeLock().lock()
        try {
            diagnosticCache.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun cacheSize(): Int {
        lock.readLock().lock()
        try {
            return diagnosticCache.size
        } finally {
            lock.readLock().unlock()
        }
    }
}

/**
 * 诊断缓存键
 */
data class DiagnosticCacheKey(
    val filePath: String,
    val contentHash: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as DiagnosticCacheKey
        
        if (filePath != other.filePath) return false
        if (contentHash != other.contentHash) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + contentHash
        return result
    }
}
```

### 7. 扩展点注册

代码诊断功能需要通过IntelliJ的扩展点进行注册。

```xml
<!-- plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <!-- 注册检查工具 -->
    <localInspection language="C#" groupName="OmniSharp" shortName="OmniSharpDiagnostics" implementationClass="com.intellij.omnisharp.diagnostics.OmniSharpDiagnosticAnalyzer" enabledByDefault="true" level="WARNING"/>
    
    <!-- 注册工具窗口工厂 -->
    <toolWindow id="OmniSharpDiagnostics" anchor="bottom" factoryClass="com.intellij.omnisharp.diagnostics.DiagnosticToolWindowFactory" showStripeButton="true" stripeButtonOrder="after.Errors"/>
    
    <!-- 注册诊断配置 -->
    <applicationConfigurable id="OmniSharpDiagnostics" instance="com.intellij.omnisharp.diagnostics.OmniSharpDiagnosticsConfigurable"/>
</extensions>
```

```kotlin
class OmniSharpDiagnosticsConfigurable : SearchableConfigurable {
    private var form: OmniSharpDiagnosticsConfigForm? = null
    
    override fun getId() = "OmniSharpDiagnostics"
    
    override fun getDisplayName() = "Code Diagnostics"
    
    override fun createComponent(): JComponent {
        if (form == null) {
            form = OmniSharpDiagnosticsConfigForm()
        }
        return form!!.panel
    }
    
    override fun isModified(): Boolean {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        return form!!.enabled != configManager.getConfiguration("diagnostics.enabled", true) ||
               form!!.projectDiagnosticsEnabled != configManager.getConfiguration("diagnostics.project.enabled", false) ||
               form!!.analyzeCurrentFileOnly != configManager.getConfiguration("diagnostics.analyze.open.documents.only", false) ||
               form!!.showErrors != configManager.getConfiguration("diagnostics.filter.error", true) ||
               form!!.showWarnings != configManager.getConfiguration("diagnostics.filter.warning", true) ||
               form!!.showInfo != configManager.getConfiguration("diagnostics.filter.info", false) ||
               form!!.cacheSize != configManager.getConfiguration("diagnostics.cache.size", 100) ||
               form!!.autoAnalyze != configManager.getConfiguration("diagnostics.auto.analyze", true)
    }
    
    override fun apply() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        configManager.setConfiguration("diagnostics.enabled", form!!.enabled)
        configManager.setConfiguration("diagnostics.project.enabled", form!!.projectDiagnosticsEnabled)
        configManager.setConfiguration("diagnostics.analyze.open.documents.only", form!!.analyzeCurrentFileOnly)
        configManager.setConfiguration("diagnostics.filter.error", form!!.showErrors)
        configManager.setConfiguration("diagnostics.filter.warning", form!!.showWarnings)
        configManager.setConfiguration("diagnostics.filter.info", form!!.showInfo)
        configManager.setConfiguration("diagnostics.cache.size", form!!.cacheSize)
        configManager.setConfiguration("diagnostics.auto.analyze", form!!.autoAnalyze)
        configManager.saveConfiguration()
        
        // 重置缓存
        val diagnosticService = ServiceManager.getService(DiagnosticService::class.java)
        (diagnosticService as? DiagnosticService)?.clearCache()
    }
    
    override fun reset() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        form!!.enabled = configManager.getConfiguration("diagnostics.enabled", true)
        form!!.projectDiagnosticsEnabled = configManager.getConfiguration("diagnostics.project.enabled", false)
        form!!.analyzeCurrentFileOnly = configManager.getConfiguration("diagnostics.analyze.open.documents.only", false)
        form!!.showErrors = configManager.getConfiguration("diagnostics.filter.error", true)
        form!!.showWarnings = configManager.getConfiguration("diagnostics.filter.warning", true)
        form!!.showInfo = configManager.getConfiguration("diagnostics.filter.info", false)
        form!!.cacheSize = configManager.getConfiguration("diagnostics.cache.size", 100)
        form!!.autoAnalyze = configManager.getConfiguration("diagnostics.auto.analyze", true)
    }
}

class OmniSharpDiagnosticsConfigForm {
    val panel: JPanel
    var enabled: Boolean by SwingProperty(false)
    var projectDiagnosticsEnabled: Boolean by SwingProperty(false)
    var analyzeCurrentFileOnly: Boolean by SwingProperty(false)
    var showErrors: Boolean by SwingProperty(false)
    var showWarnings: Boolean by SwingProperty(false)
    var showInfo: Boolean by SwingProperty(false)
    var cacheSize: Int by SwingProperty(100)
    var autoAnalyze: Boolean by SwingProperty(false)
    
    init {
        panel = JPanel(BorderLayout())
        
        val contentPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.WEST
        constraints.weightx = 1.0
        constraints.insets = Insets(5, 5, 5, 5)
        
        // 启用诊断选项
        val enableDiagnosticsCheckBox = JCheckBox("Enable code diagnostics")
        enableDiagnosticsCheckBox.bindSelectedTo(SwingProperty(this::enabled))
        constraints.gridy = 0
        contentPanel.add(enableDiagnosticsCheckBox, constraints)
        
        // 启用项目级诊断
        val enableProjectDiagnosticsCheckBox = JCheckBox("Enable project-wide diagnostics")
        enableProjectDiagnosticsCheckBox.bindSelectedTo(SwingProperty(this::projectDiagnosticsEnabled))
        constraints.gridy = 1
        constraints.insets = Insets(5, 20, 5, 5)  // 缩进
        contentPanel.add(enableProjectDiagnosticsCheckBox, constraints)
        
        // 仅分析当前文件
        val analyzeCurrentFileCheckBox = JCheckBox("Analyze only current file")
        analyzeCurrentFileCheckBox.bindSelectedTo(SwingProperty(this::analyzeCurrentFileOnly))
        constraints.gridy = 2
        contentPanel.add(analyzeCurrentFileCheckBox, constraints)
        
        // 自动分析
        constraints.insets = Insets(5, 5, 5, 5)  // 恢复缩进
        val autoAnalyzeCheckBox = JCheckBox("Automatically analyze on file save")
        autoAnalyzeCheckBox.bindSelectedTo(SwingProperty(this::autoAnalyze))
        constraints.gridy = 3
        contentPanel.add(autoAnalyzeCheckBox, constraints)
        
        // 显示错误
        val showErrorsCheckBox = JCheckBox("Show errors", true)
        showErrorsCheckBox.bindSelectedTo(SwingProperty(this::showErrors))
        constraints.gridy = 4
        contentPanel.add(showErrorsCheckBox, constraints)
        
        // 显示警告
        val showWarningsCheckBox = JCheckBox("Show warnings", true)
        showWarningsCheckBox.bindSelectedTo(SwingProperty(this::showWarnings))
        constraints.gridy = 5
        contentPanel.add(showWarningsCheckBox, constraints)
        
        // 显示信息
        val showInfoCheckBox = JCheckBox("Show information")
        showInfoCheckBox.bindSelectedTo(SwingProperty(this::showInfo))
        constraints.gridy = 6
        contentPanel.add(showInfoCheckBox, constraints)
        
        // 缓存大小
        val cacheSizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val cacheSizeLabel = JLabel("Cache size:")
        val cacheSizeSpinner = JSpinner(SpinnerNumberModel(100, 10, 1000, 10))
        cacheSizeSpinner.bindValueTo(SwingProperty(this::cacheSize))
        cacheSizePanel.add(cacheSizeLabel)
        cacheSizePanel.add(cacheSizeSpinner)
        constraints.gridy = 7
        contentPanel.add(cacheSizePanel, constraints)
        
        panel.add(contentPanel, BorderLayout.NORTH)
        panel.add(JPanel(), BorderLayout.CENTER) // 填充空间
    }
}
```

## 诊断请求和处理

### 诊断请求时机

代码诊断可以在以下时机触发：

1. **文件保存时**：当用户保存文件时自动触发诊断
2. **编辑器失焦时**：当编辑器窗口失去焦点时触发诊断
3. **手动触发**：通过工具栏或菜单手动触发诊断
4. **定时触发**：定期对当前编辑的文件进行诊断

### 诊断响应处理

OmniSharp服务器返回的诊断结果需要转换为IntelliJ的ProblemDescriptor格式，以便在编辑器中显示。

```kotlin
/**
 * 处理OmniSharp诊断响应
 */
fun processDiagnosticsResponse(response: OmniSharpResponse): List<DiagnosticInfo> {
    try {
        val diagnostics = mutableListOf<DiagnosticInfo>()
        
        if (response.success && response.body is Map<*, *>) {
            val body = response.body as Map<String, Any>
            val results = body["Results"] as? List<Map<String, Any>> ?: emptyList()
            
            for (result in results) {
                val fileName = result["FileName"] as? String ?: continue
                val fileDiagnostics = result["Diagnostics"] as? List<Map<String, Any>> ?: emptyList()
                
                for (diagnostic in fileDiagnostics) {
                    val info = DiagnosticInfo(
                        fileName = fileName,
                        message = diagnostic["Message"] as? String ?: "",
                        severity = getSeverity(diagnostic["Severity"] as? String ?: ""),
                        code = diagnostic["Code"] as? String ?: "",
                        source = diagnostic["Source"] as? String ?: "OmniSharp",
                        startLine = (diagnostic["StartLine"] as? Int ?: 0) - 1,
                        startColumn = (diagnostic["StartColumn"] as? Int ?: 0) - 1,
                        endLine = (diagnostic["EndLine"] as? Int ?: 0) - 1,
                        endColumn = (diagnostic["EndColumn"] as? Int ?: 0) - 1,
                        projectName = result["ProjectName"] as? String ?: ""
                    )
                    
                    diagnostics.add(info)
                }
            }
        }
        
        return diagnostics
    } catch (e: Exception) {
        Logger.getInstance(javaClass).warn("Error processing diagnostics response", e)
        return emptyList()
    }
}

/**
 * 诊断信息
 */
data class DiagnosticInfo(
    val fileName: String,
    val message: String,
    val severity: DiagnosticSeverity,
    val code: String,
    val source: String,
    val startLine: Int,
    val startColumn: Int,
    val endLine: Int,
    val endColumn: Int,
    val projectName: String
)
```

## 诊断结果展示

### 编辑器内展示

诊断结果在编辑器中以多种方式展示：

1. **行号旁图标**：在行号旁边显示错误、警告或信息图标
2. **代码高亮**：高亮显示有问题的代码
3. **工具提示**：鼠标悬停时显示诊断详细信息
4. **快速修复提示**：显示可用的快速修复建议

### 诊断工具窗口

诊断工具窗口集中展示所有诊断结果，支持以下功能：

1. **结果列表**：以列表形式展示诊断结果
2. **过滤功能**：按严重程度、类型等过滤结果
3. **分组显示**：按文件、项目等分组显示
4. **快速导航**：点击结果跳转到相应位置
5. **刷新功能**：手动刷新诊断结果

## 快速修复实现

### 快速修复类型

支持多种类型的快速修复：

1. **代码修正**：修复语法错误、变量初始化等问题
2. **导入添加**：自动添加缺失的using指令
3. **代码重构**：如将字段设为只读、移除未使用的变量等
4. **诊断抑制**：添加注释抑制特定诊断

### 快速修复应用

用户可以通过以下方式应用快速修复：

1. **灯泡图标**：点击编辑器中的灯泡图标选择快速修复
2. **快捷键**：使用Alt+Enter快捷键打开快速修复菜单
3. **右键菜单**：通过右键菜单中的快速修复选项

## 诊断规则配置

### 规则过滤

用户可以配置哪些诊断规则需要启用或禁用：

```kotlin
class DiagnosticRuleManager {
    private val configurationManager: ConfigurationManager
    
    constructor(configurationManager: ConfigurationManager) {
        this.configurationManager = configurationManager
    }
    
    /**
     * 检查诊断规则是否启用
     */
    fun isRuleEnabled(ruleId: String): Boolean {
        return configurationManager.getConfiguration("diagnostics.rules.$ruleId", true)
    }
    
    /**
     * 启用诊断规则
     */
    fun enableRule(ruleId: String) {
        configurationManager.setConfiguration("diagnostics.rules.$ruleId", true)
    }
    
    /**
     * 禁用诊断规则
     */
    fun disableRule(ruleId: String) {
        configurationManager.setConfiguration("diagnostics.rules.$ruleId", false)
    }
    
    /**
     * 过滤诊断结果
     */
    fun filterDiagnostics(diagnostics: List<DiagnosticInfo>): List<DiagnosticInfo> {
        return diagnostics.filter { isRuleEnabled(it.code) && isSeverityEnabled(it.severity) }
    }
    
    /**
     * 检查严重程度是否启用
     */
    private fun isSeverityEnabled(severity: DiagnosticSeverity): Boolean {
        return when (severity) {
            DiagnosticSeverity.ERROR -> configurationManager.getConfiguration("diagnostics.filter.error", true)
            DiagnosticSeverity.WARNING -> configurationManager.getConfiguration("diagnostics.filter.warning", true)
            DiagnosticSeverity.INFO -> configurationManager.getConfiguration("diagnostics.filter.info", false)
        }
    }
}
```

## 缓存策略

### 诊断缓存

为了提高性能，我们实现了诊断结果的缓存：

1. **文件级别缓存**：缓存每个文件的诊断结果
2. **内容哈希键**：使用文件内容哈希作为缓存键的一部分
3. **LRU淘汰策略**：当缓存达到最大大小时，淘汰最久未使用的缓存项

### 缓存失效

缓存会在以下情况失效：

1. **文件修改**：当文件内容修改并保存时，清除该文件的缓存
2. **配置变更**：当诊断配置变更时，清除所有缓存
3. **项目刷新**：当项目结构刷新时，清除相关缓存

## 错误处理

### 请求错误处理

在向OmniSharp服务器发送诊断请求时，需要处理各种可能的错误：

```kotlin
/**
 * 处理诊断请求错误
 */
fun handleDiagnosticsError(project: Project, error: Throwable) {
    when (error) {
        is TimeoutException -> {
            showErrorNotification(project, "Diagnostics request timed out. Please try again.")
        }
        is IOException -> {
            showErrorNotification(project, "Connection error while getting diagnostics. Please check OmniSharp server connection.")
        }
        else -> {
            showErrorNotification(project, "Error getting diagnostics: ${error.message}")
        }
    }
}

/**
 * 显示错误通知
 */
fun showErrorNotification(project: Project, message: String) {
    ApplicationManager.getApplication().invokeLater {
        Notifications.Bus.notify(
            Notification(
                "OmniSharp Diagnostics",
                "Diagnostics Error",
                message,
                NotificationType.ERROR
            ),
            project
        )
    }
}
```

### 降级策略

当OmniSharp服务器不可用或诊断功能出现问题时，采用降级策略：

1. **使用缓存结果**：显示最近一次的缓存诊断结果
2. **本地诊断**：启用基本的本地语法检查
3. **错误提示**：向用户提供友好的错误提示

## 性能优化

### 增量诊断

为了提高性能，我们实现了增量诊断：

1. **局部分析**：只分析文件的修改部分
2. **延迟分析**：使用防抖机制，避免频繁触发诊断
3. **后台执行**：所有诊断请求在后台线程执行

### 诊断过滤

对大量诊断结果进行过滤和分组，提高展示效率：

```kotlin
class DiagnosticFilter {
    /**
     * 按严重程度过滤
     */
    fun filterBySeverity(
        diagnostics: List<DiagnosticInfo>,
        showErrors: Boolean,
        showWarnings: Boolean,
        showInfo: Boolean
    ): List<DiagnosticInfo> {
        return diagnostics.filter { diagnostic ->
            when (diagnostic.severity) {
                DiagnosticSeverity.ERROR -> showErrors
                DiagnosticSeverity.WARNING -> showWarnings
                DiagnosticSeverity.INFO -> showInfo
            }
        }
    }
    
    /**
     * 按文件过滤
     */
    fun filterByFile(diagnostics: List<DiagnosticInfo>, fileName: String): List<DiagnosticInfo> {
        return diagnostics.filter { it.fileName.contains(fileName, ignoreCase = true) }
    }
    
    /**
     * 按项目过滤
     */
    fun filterByProject(diagnostics: List<DiagnosticInfo>, projectName: String): List<DiagnosticInfo> {
        return diagnostics.filter { it.projectName == projectName }
    }
    
    /**
     * 按规则ID过滤
     */
    fun filterByRuleId(diagnostics: List<DiagnosticInfo>, ruleId: String): List<DiagnosticInfo> {
        return diagnostics.filter { it.code == ruleId }
    }
    
    /**
     * 按消息文本过滤
     */
    fun filterByMessageText(diagnostics: List<DiagnosticInfo>, text: String): List<DiagnosticInfo> {
        return diagnostics.filter { it.message.contains(text, ignoreCase = true) }
    }
    
    /**
     * 按行范围过滤
     */
    fun filterByLineRange(diagnostics: List<DiagnosticInfo>, startLine: Int, endLine: Int): List<DiagnosticInfo> {
        return diagnostics.filter { it.startLine >= startLine && it.endLine <= endLine }
    }
    
    /**
     * 对诊断结果进行分组
     */
    fun groupBySeverity(diagnostics: List<DiagnosticInfo>): Map<DiagnosticSeverity, List<DiagnosticInfo>> {
        return diagnostics.groupBy { it.severity }
    }
    
    /**
     * 按文件分组诊断结果
     */
    fun groupByFile(diagnostics: List<DiagnosticInfo>): Map<String, List<DiagnosticInfo>> {
        return diagnostics.groupBy { it.fileName }
    }
    
    /**
     * 按项目分组诊断结果
     */
    fun groupByProject(diagnostics: List<DiagnosticInfo>): Map<String, List<DiagnosticInfo>> {
        return diagnostics.groupBy { it.projectName }
    }
}

### 异步处理

为了避免阻塞UI，所有诊断相关操作都在后台线程执行：

```kotlin
/**
 * 异步执行诊断操作
 */
fun analyzeAsync(
    project: Project,
    file: VirtualFile,
    onSuccess: (List<ProblemDescriptor>) -> Unit,
    onError: (Throwable) -> Unit
) {
    ApplicationManager.getApplication().executeOnPooledThread {
        try {
            val future = diagnosticService.analyzeFile(project, file, document)
            val problems = future.get(10000, TimeUnit.MILLISECONDS)  // 10秒超时
            
            ApplicationManager.getApplication().invokeLater {
                onSuccess(problems)
            }
        } catch (e: Exception) {
            ApplicationManager.getApplication().invokeLater {
                onError(e)
            }
        }
    }
}
```

## 测试策略

### 单元测试

为核心组件编写单元测试：

```kotlin
class DiagnosticServiceTest {
    @Mock
    private lateinit var communicatorFactory: OmniSharpCommunicatorFactory
    
    @Mock
    private lateinit var sessionManager: SessionManager
    
    @Mock
    private lateinit var configurationManager: ConfigurationManager
    
    @Mock
    private lateinit var quickFixRegistry: QuickFixRegistry
    
    @Mock
    private lateinit var session: OmniSharpSession
    
    @Mock
    private lateinit var communicator: OmniSharpCommunicator
    
    private lateinit var diagnosticService: DiagnosticService
    
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        `when`(configurationManager.getConfiguration("diagnostics.enabled", true)).thenReturn(true)
        `when`(configurationManager.getConfiguration("diagnostics.cache.size", 100)).thenReturn(100)
        `when`(configurationManager.getConfiguration("diagnostics.analyze.open.documents.only", false)).thenReturn(false)
        `when`(sessionManager.getSession(any())).thenReturn(session)
        `when`(session.communicator).thenReturn(communicator)
        `when`(session.isAlive).thenReturn(true)
        
        diagnosticService = DiagnosticService(
            communicatorFactory,
            sessionManager,
            configurationManager,
            quickFixRegistry
        )
    }
    
    @Test
    fun testAnalyzeFileSuccess() {
        // 准备模拟响应
        val mockResponse = OmniSharpResponse(
            success = true,
            body = mapOf(
                "Results" to listOf(
                    mapOf(
                        "FileName" to "test.cs",
                        "Diagnostics" to listOf(
                            mapOf(
                                "Message" to "Test error",
                                "Severity" to "Error",
                                "Code" to "CS0001",
                                "Source" to "OmniSharp",
                                "StartLine" to 10,
                                "StartColumn" to 5,
                                "EndLine" to 10,
                                "EndColumn" to 10
                            )
                        )
                    )
                )
            ),
            elapsedMilliseconds = 100
        )
        
        `when`(communicator.sendRequest("v2/diagnostics", anyMap())).thenReturn(
            CompletableFuture.completedFuture(mockResponse)
        )
        
        // 创建测试文件和文档
        val file = mock<VirtualFile>()
        val document = mock<Document>()
        
        `when`(file.fileType.name).thenReturn("C#")
        `when`(file.path).thenReturn("test.cs")
        `when`(document.text).thenReturn("test content")
        `when`(document.getLineStartOffset(9)).thenReturn(0)  // 第10行（0-based是9）的开始偏移量
        
        // 执行测试
        val project = mock<Project>()
        val result = diagnosticService.analyzeFile(project, file, document).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("Test error", result[0].descriptionTemplate)
    }
    
    @Test
    fun testAnalyzeFileNotEnabled() {
        // 设置诊断功能未启用
        `when`(configurationManager.getConfiguration("diagnostics.enabled", true)).thenReturn(false)
        
        // 创建测试文件和文档
        val file = mock<VirtualFile>()
        val document = mock<Document>()
        
        // 执行测试
        val project = mock<Project>()
        val result = diagnosticService.analyzeFile(project, file, document).get()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testAnalyzeFileNotCSharp() {
        // 创建非C#文件
        val file = mock<VirtualFile>()
        val document = mock<Document>()
        
        `when`(file.fileType.name).thenReturn("Java")
        
        // 执行测试
        val project = mock<Project>()
        val result = diagnosticService.analyzeFile(project, file, document).get()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testAnalyzeFileSessionNotAvailable() {
        // 设置会话不可用
        `when`(sessionManager.getSession(any())).thenReturn(null)
        
        // 创建测试文件和文档
        val file = mock<VirtualFile>()
        val document = mock<Document>()
        
        `when`(file.fileType.name).thenReturn("C#")
        `when`(file.path).thenReturn("test.cs")
        `when`(document.text).thenReturn("test content")
        
        // 执行测试并验证异常
        val project = mock<Project>()
        val future = diagnosticService.analyzeFile(project, file, document)
        
        try {
            future.get()
            fail("Should throw exception")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is IllegalStateException)
        }
    }
}
```

### 集成测试

为诊断功能与其他组件的集成编写测试：

```kotlin
class DiagnosticIntegrationTest {
    private lateinit var project: Project
    private lateinit var diagnosticService: DiagnosticService
    
    @Before
    fun setUp() {
        // 创建测试项目
        project = ProjectManager.getInstance().defaultProject
        
        // 获取服务实例
        diagnosticService = ServiceManager.getService(DiagnosticService::class.java)
    }
    
    @Test
    fun testDiagnosticAnalysisWithRealFile() {
        // 创建临时C#文件
        val file = createTempFile("test", ".cs")
        file.writeText("class Test { int x; void Method() { Console.WriteLine(x); } }")
        
        // 将文件添加到项目
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        assertNotNull(virtualFile)
        
        // 获取文档
        val document = FileDocumentManager.getInstance().getDocument(virtualFile!!)
        assertNotNull(document)
        
        // 分析文件
        val future = diagnosticService.analyzeFile(project, virtualFile, document!!)
        val problems = future.get(20000, TimeUnit.MILLISECONDS)  // 20秒超时
        
        // 验证至少有一个诊断结果（缺少using指令）
        assertTrue(problems.isNotEmpty())
    }
    
    @After
    fun tearDown() {
        // 清理资源
    }
}
```

## 使用示例

### 基本使用

以下是代码诊断功能的基本使用示例：

```kotlin
// 获取诊断服务
val diagnosticService = ServiceManager.getService(DiagnosticService::class.java)

// 获取当前文件和文档
val editor = FileEditorManager.getInstance(project).selectedTextEditor
val file = editor?.virtualFile
val document = editor?.document

if (file != null && document != null) {
    // 分析文件
    diagnosticService.analyzeFile(project, file, document)
        .thenApply { problems ->
            // 处理诊断结果
            for (problem in problems) {
                println("${problem.descriptionTemplate} at line ${document.getLineNumber(problem.startOffset) + 1}")
            }
        }
        .exceptionally { ex ->
            // 处理错误
            println("Error analyzing file: ${ex.message}")
            null
        }
}
```

### 项目级诊断

以下是项目级诊断的使用示例：

```kotlin
// 获取诊断服务
val diagnosticService = ServiceManager.getService(DiagnosticService::class.java)

// 分析整个项目
val progressIndicator = object : ProgressIndicatorAdapter() {
    override fun setText(text: String) {
        println(text)
    }
    
    override fun setFraction(fraction: Double) {
        println("Progress: ${(fraction * 100).toInt()}%")
    }
}

ProgressManager.getInstance().runProcessWithProgressSynchronously({
    diagnosticService.analyzeProject(project)
        .thenApply { problems ->
            // 处理项目诊断结果
            val groupedBySeverity = problems.groupBy { it.highlightType }
            println("Errors: ${groupedBySeverity[ProblemHighlightType.ERROR]?.size ?: 0}")
            println("Warnings: ${groupedBySeverity[ProblemHighlightType.WARNING]?.size ?: 0}")
            println("Info: ${groupedBySeverity[ProblemHighlightType.INFORMATION]?.size ?: 0}")
        }
        .exceptionally { ex ->
            println("Error analyzing project: ${ex.message}")
            null
        }
}, "Analyzing Project", false, project)
```

### 快速修复应用

以下是应用快速修复的示例：

```kotlin
// 获取诊断服务
val diagnosticService = ServiceManager.getService(DiagnosticService::class.java)

// 获取适用于特定问题的快速修复
val quickFixes = diagnosticService.getQuickFixes(problem)

// 应用第一个快速修复
if (quickFixes.isNotEmpty()) {
    ApplicationManager.getApplication().invokeLater {
        quickFixes[0].applyFix(project, problem)
    }
}
```

## 总结

本实现方案详细描述了OmniSharp代码诊断功能在IntelliJ平台中的集成实现。通过该方案，开发者可以在编辑C#代码时获得实时、准确的代码质量反馈，包括错误检测、警告提示和代码改进建议。

主要特点包括：

1. **实时分析**：在编辑过程中实时分析代码并显示诊断结果
2. **多种诊断级别**：支持错误、警告和信息三种诊断级别
3. **灵活配置**：支持多种配置选项，用户可以根据需要自定义诊断行为
4. **快速修复**：为常见问题提供自动修复功能
5. **性能优化**：通过缓存、增量分析等技术提高性能
6. **集成体验**：与IntelliJ平台无缝集成，提供一致的用户体验

本方案不仅提供了功能实现，还包含了测试策略和性能优化考虑，可以确保代码诊断功能的稳定运行和良好性能。通过这种方式，开发者可以在IntelliJ平台上获得与Visual Studio类似的C#代码诊断体验，提高开发效率和代码质量。

## 后续优化方向

1. **增加更多快速修复类型**：支持更多类型的代码问题自动修复
2. **增强缓存策略**：优化缓存机制，提高诊断性能
3. **扩展诊断规则管理**：提供更细粒度的诊断规则配置
4. **支持自定义诊断规则**：允许用户添加自定义诊断规则
5. **诊断历史记录**：记录诊断历史，帮助开发者跟踪代码质量变化
6. **与版本控制系统集成**：在提交代码前执行诊断检查，防止问题代码提交到仓库