# OmniSharp代码补全功能集成实现方案

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [核心组件实现](#核心组件实现)
4. [缓存策略](#缓存策略)
5. [错误处理](#错误处理)
6. [性能优化](#性能优化)
7. [配置选项](#配置选项)
8. [测试策略](#测试策略)
9. [使用示例](#使用示例)
10. [总结](#总结)

## 概述

本文档详细描述OmniSharp代码补全功能在IntelliJ平台中的集成实现方案。代码补全是编辑器中最常用的智能功能之一，为开发者提供上下文相关的代码建议，大幅提高编码效率。本实现方案将OmniSharp服务器提供的代码补全能力与IntelliJ的代码补全系统无缝集成，确保高性能、低延迟的用户体验。

### 功能特性

- **实时代码补全**：在用户输入时提供即时的代码建议
- **智能上下文感知**：基于当前编辑位置的语法和语义上下文提供相关补全
- **丰富的补全类型**：支持方法、属性、字段、类、命名空间等多种补全项
- **详细的补全信息**：显示参数信息、文档注释、类型信息等
- **自定义排序和过滤**：根据相关性和用户偏好对补全项进行排序
- **补全后处理**：支持补全后的光标定位、参数提示等高级功能

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
|            CompletionService             |
+---------------------+--------------------+
                      |
     +----------------+------------------+
     |                 |                  |
     v                 v                  v
+-----------------+-----------------+------------------+
|  OmniSharpCompletionProvider  | CompletionCache | CompletionHandler |
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

### 补全流程

1. **触发阶段**：用户在编辑器中输入特定字符（如点号、空格、字母等）触发补全
2. **请求阶段**：收集当前编辑上下文，向OmniSharp服务器发送补全请求
3. **处理阶段**：接收补全响应，转换为IntelliJ补全项格式
4. **显示阶段**：在编辑器中显示补全列表
5. **应用阶段**：用户选择补全项后，应用补全并进行后续处理

## 核心组件实现

### 1. OmniSharpCompletionService

OmniSharpCompletionService是代码补全功能的核心服务类，负责协调各组件的工作并提供统一的API。

```kotlin
class OmniSharpCompletionService : Disposable {
    private val communicatorFactory: OmniSharpCommunicatorFactory
    private val completionCache: CompletionCache
    private val sessionManager: SessionManager
    private val configurationManager: ConfigurationManager
    private val debounceService: DebounceService
    private val logger: Logger
    
    constructor(
        communicatorFactory: OmniSharpCommunicatorFactory,
        sessionManager: SessionManager,
        configurationManager: ConfigurationManager,
        debounceService: DebounceService
    ) {
        this.communicatorFactory = communicatorFactory
        this.completionCache = CompletionCache(configurationManager.getConfiguration("completion.cache.size", 100))
        this.sessionManager = sessionManager
        this.configurationManager = configurationManager
        this.debounceService = debounceService
        this.logger = Logger.getInstance("OmniSharpCompletionService")
    }
    
    /**
     * 获取代码补全项
     */
    fun getCompletions(
        project: Project,
        file: VirtualFile,
        caretOffset: Int,
        document: Document
    ): CompletableFuture<List<OmniSharpCompletionItem>> {
        // 检查是否启用了补全功能
        if (!configurationManager.getConfiguration("completion.enabled", true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 验证文件是否为C#文件
        if (!file.fileType.name.equals("C#", ignoreCase = true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 构建缓存键
        val cacheKey = CompletionCacheKey(file.path, caretOffset, document.text.hashCode())
        
        // 检查缓存
        val cachedCompletions = completionCache.get(cacheKey)
        if (cachedCompletions != null) {
            logger.debug("Completion cache hit for ${file.name}:$caretOffset")
            return CompletableFuture.completedFuture(cachedCompletions)
        }
        
        // 获取当前会话
        val session = sessionManager.getSession(project)
        if (session == null || !session.isAlive) {
            return CompletableFuture.failedFuture(IllegalStateException("OmniSharp session not available"))
        }
        
        // 收集上下文信息
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, caretOffset))
        
        // 构建请求参数
        val requestParams = mapOf(
            "FileName" to file.path,
            "Line" to (lineNumber + 1),  // OmniSharp使用1-based行号
            "Column" to (caretOffset - lineStartOffset + 1),  // OmniSharp使用1-based列号
            "WordToComplete" to extractWordToComplete(lineText),
            "WantDocumentationForEveryCompletionResult" to configurationManager.getConfiguration("completion.show.documentation", true),
            "WantKind" to true,
            "WantReturnType" to true,
            "WantMethodHeader" to true,
            "WantParameters" to true,
            "IncludeInvalid" to configurationManager.getConfiguration("completion.include.invalid", false)
        )
        
        logger.debug("Sending completion request for ${file.name}:$lineNumber:$column")
        
        // 发送请求到OmniSharp服务器
        return session.communicator.sendRequest("completion", requestParams)
            .thenApply { response ->
                // 处理响应
                val completions = processCompletionResponse(response)
                
                // 更新缓存
                completionCache.put(cacheKey, completions)
                
                completions
            }
            .exceptionally { ex ->
                logger.warn("Error getting completions", ex)
                emptyList()
            }
    }
    
    /**
     * 处理补全响应
     */
    private fun processCompletionResponse(response: OmniSharpResponse): List<OmniSharpCompletionItem> {
        try {
            val completions = mutableListOf<OmniSharpCompletionItem>()
            
            // 检查响应是否包含补全数据
            if (response.success && response.body is Map<*, *>) {
                val body = response.body as Map<String, Any>
                val items = body["Completions"] as? List<Map<String, Any>> ?: emptyList()
                
                // 转换每个补全项
                for (item in items) {
                    val completionItem = createCompletionItem(item)
                    completions.add(completionItem)
                }
                
                // 应用排序规则
                return sortCompletions(completions)
            }
            
            return completions
        } catch (e: Exception) {
            logger.warn("Error processing completion response", e)
            return emptyList()
        }
    }
    
    /**
     * 创建补全项
     */
    private fun createCompletionItem(itemData: Map<String, Any>): OmniSharpCompletionItem {
        return DefaultOmniSharpCompletionItem(
            label = itemData["DisplayText"] as? String ?: "",
            kind = itemData["Kind"] as? String,
            detail = buildDetailText(itemData),
            documentation = itemData["Description"] as? String,
            insertText = itemData["InsertText"] as? String ?: (itemData["DisplayText"] as? String ?: ""),
            filterText = itemData["FilterText"] as? String,
            sortText = itemData["SortText"] as? String,
            data = itemData
        )
    }
    
    /**
     * 构建详细文本
     */
    private fun buildDetailText(itemData: Map<String, Any>): String {
        val sb = StringBuilder()
        
        // 添加返回类型
        if (itemData.containsKey("ReturnType")) {
            sb.append(itemData["ReturnType"] as String).append(" ")
        }
        
        // 添加显示文本
        sb.append(itemData["DisplayText"] as String)
        
        return sb.toString()
    }
    
    /**
     * 提取待补全的单词
     */
    private fun extractWordToComplete(lineText: String): String {
        val matcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*$").matcher(lineText)
        return if (matcher.find()) {
            matcher.group()
        } else {
            ""
        }
    }
    
    /**
     * 排序补全项
     */
    private fun sortCompletions(completions: List<OmniSharpCompletionItem>): List<OmniSharpCompletionItem> {
        val showKindLabels = configurationManager.getConfiguration("completion.show.kind.labels", true)
        
        return completions.sortedWith(compareBy {
            // 自定义排序逻辑
            val item = it
            val priority = when (item.kind) {
                "Method" -> 1
                "Property" -> 2
                "Field" -> 3
                "Class" -> 4
                "Interface" -> 5
                "Enum" -> 6
                "Struct" -> 7
                "Namespace" -> 8
                else -> 9
            }
            priority
        })
    }
    
    /**
     * 处理补全项的选择
     */
    fun handleCompletionItemSelected(item: OmniSharpCompletionItem, editor: Editor) {
        // 可以在这里添加补全后的额外处理，如光标定位、参数提示等
        logger.debug("Completion item selected: ${item.label}")
        
        // 触发参数提示
        if (item.kind == "Method") {
            editor.project?.let { project ->
                EditorFactory.getInstance().postponedRun(editor) {
                    // 可以触发参数提示功能
                }
            }
        }
    }
    
    /**
     * 清理资源
     */
    override fun dispose() {
        completionCache.clear()
    }
}
```

### 2. OmniSharpCompletionProvider

OmniSharpCompletionProvider实现了IntelliJ的CompletionProvider接口，是编辑器与补全服务之间的桥梁。

```kotlin
class OmniSharpCompletionProvider : CompletionProvider<CompletionParameters>() {
    private val completionService: OmniSharpCompletionService
    private val logger: Logger
    
    constructor(completionService: OmniSharpCompletionService) {
        this.completionService = completionService
        this.logger = Logger.getInstance("OmniSharpCompletionProvider")
    }
    
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.project
        val editor = parameters.editor
        val file = parameters.originalFile
        val caretOffset = parameters.offset
        val document = editor.document
        
        // 异步获取补全项
        val future = completionService.getCompletions(project, file, caretOffset, document)
        
        future.whenComplete {
            completions, exception ->
            if (exception != null) {
                logger.warn("Error adding completions", exception)
                return@whenComplete
            }
            
            // 转换并添加补全项到结果集
            ApplicationManager.getApplication().runReadAction {
                if (editor.isDisposed) return@runReadAction
                
                val lookupElementBuilder = CompletionResultBuilder(editor)
                
                for (completion in completions) {
                    val lookupElement = completion.toIntelliJItem()
                    result.addElement(lookupElement)
                }
                
                // 添加动态排序和过滤
                result.whenWillShow {
                    // 可以在这里添加额外的排序和过滤逻辑
                }
                
                // 完成处理
                result.stopHere()
            }
        }
    }
    
    /**
     * 补全结果构建器
     */
    private inner class CompletionResultBuilder(private val editor: Editor) {
        fun createLookupElement(item: OmniSharpCompletionItem): LookupElement {
            val presentation = LookupElementPresentation()
            
            // 设置显示文本
            presentation.itemText = item.label
            
            // 设置类型图标
            presentation.setIcon(getCompletionItemIcon(item.kind))
            
            // 设置详细信息
            if (item.detail != null) {
                presentation.tailText = " ${item.detail}"
            }
            
            // 设置类型标签
            if (item.kind != null) {
                presentation.typeText = getKindLabel(item.kind)
            }
            
            // 创建补全项
            val lookupElement = SimpleLookupElement(item.label, presentation)
            
            // 设置插入文本
            if (item.insertText != null && item.insertText != item.label) {
                lookupElement.insertHandler = LookupElementInsertHandler<LookupElement> { context, _ ->
                    val document = context.document
                    val editor = context.editor
                    val startOffset = context.startOffset
                    val currentOffset = context.tailOffset
                    
                    ApplicationManager.getApplication().runWriteAction {
                        document.deleteString(startOffset, currentOffset)
                        document.insertString(startOffset, item.insertText)
                        
                        // 调用补全选择处理器
                        completionService.handleCompletionItemSelected(item, editor)
                    }
                }
            } else {
                // 注册补全选择监听器
                lookupElement.putUserData(CompletionItemSelectedListener.ELEMENT_KEY, 
                    CompletionItemSelectedListener { _, _ ->
                        completionService.handleCompletionItemSelected(item, editor)
                    }
                )
            }
            
            // 添加文档信息
            if (item.documentation != null) {
                lookupElement.psiElement = object : PsiElementBase() {
                    override fun getParent() = null
                    override fun getProject() = editor.project!!
                    override fun getContainingFile() = null
                    override fun getTextRange() = TextRange.EMPTY_RANGE
                    override fun getText() = null
                    override fun navigate(requestFocus: Boolean) = Unit
                    override fun canNavigate() = false
                    override fun canNavigateToSource() = false
                    override fun getTextOffset() = -1
                    override fun toString() = "DocumentationHolder"
                    
                    override fun getDocumentationToolTip() = item.documentation
                }
            }
            
            return lookupElement
        }
        
        /**
         * 获取补全项图标
         */
        private fun getCompletionItemIcon(kind: String?): Icon {
            return when (kind) {
                "Method" -> AllIcons.Nodes.Method
                "Property" -> AllIcons.Nodes.Property
                "Field" -> AllIcons.Nodes.Field
                "Class" -> AllIcons.Nodes.Class
                "Interface" -> AllIcons.Nodes.Interface
                "Enum" -> AllIcons.Nodes.Enum
                "Struct" -> AllIcons.Nodes.Structure
                "Namespace" -> AllIcons.Nodes.Package
                else -> AllIcons.Nodes.Method
            }
        }
        
        /**
         * 获取类型标签
         */
        private fun getKindLabel(kind: String?): String {
            return when (kind) {
                "Method" -> "method"
                "Property" -> "property"
                "Field" -> "field"
                "Class" -> "class"
                "Interface" -> "interface"
                "Enum" -> "enum"
                "Struct" -> "struct"
                "Namespace" -> "namespace"
                else -> ""
            }
        }
    }
}
```

### 3. CompletionCache

CompletionCache实现了高效的补全结果缓存，减少对OmniSharp服务器的请求次数，提高补全响应速度。

```kotlin
class CompletionCache(private val maxSize: Int) {
    private val cache: LinkedHashMap<CompletionCacheKey, List<OmniSharpCompletionItem>>
    private val lock = ReentrantReadWriteLock()
    
    init {
        // 创建一个访问顺序的LinkedHashMap作为LRU缓存
        cache = object : LinkedHashMap<CompletionCacheKey, List<OmniSharpCompletionItem>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<CompletionCacheKey, List<OmniSharpCompletionItem>>): Boolean {
                return size > maxSize
            }
        }
    }
    
    /**
     * 获取缓存项
     */
    fun get(key: CompletionCacheKey): List<OmniSharpCompletionItem>? {
        lock.readLock().lock()
        try {
            return cache[key]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 存储缓存项
     */
    fun put(key: CompletionCacheKey, value: List<OmniSharpCompletionItem>) {
        lock.writeLock().lock()
        try {
            cache[key] = value
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 移除缓存项
     */
    fun remove(key: CompletionCacheKey) {
        lock.writeLock().lock()
        try {
            cache.remove(key)
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
            val keysToRemove = cache.keys.filter { it.filePath == filePath }
            keysToRemove.forEach { cache.remove(it) }
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
            cache.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 获取缓存大小
     */
    fun size(): Int {
        lock.readLock().lock()
        try {
            return cache.size
        } finally {
            lock.readLock().unlock()
        }
    }
}

/**
 * 补全缓存键
 */
data class CompletionCacheKey(
    val filePath: String,
    val caretOffset: Int,
    val contentHash: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as CompletionCacheKey
        
        if (filePath != other.filePath) return false
        if (caretOffset != other.caretOffset) return false
        if (contentHash != other.contentHash) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + caretOffset
        result = 31 * result + contentHash
        return result
    }
}
```

### 4. OmniSharpCompletionItem

OmniSharpCompletionItem接口定义了代码补全项的数据结构，DefaultOmniSharpCompletionItem提供了具体实现。

```kotlin
interface OmniSharpCompletionItem {
    val label: String
    val kind: String?
    val detail: String?
    val documentation: String?
    val insertText: String
    val filterText: String?
    val sortText: String?
    val data: Map<String, Any>?
    
    /**
     * 转换为IntelliJ的LookupElement
     */
    fun toIntelliJItem(): LookupElement
}

class DefaultOmniSharpCompletionItem(
    override val label: String,
    override val kind: String?,
    override val detail: String?,
    override val documentation: String?,
    override val insertText: String,
    override val filterText: String?,
    override val sortText: String?,
    override val data: Map<String, Any>?
) : OmniSharpCompletionItem {
    
    override fun toIntelliJItem(): LookupElement {
        val presentation = LookupElementPresentation()
        presentation.itemText = label
        
        // 设置图标
        val icon = getIconForKind(kind)
        presentation.setIcon(icon)
        
        // 设置详细信息
        if (detail != null) {
            presentation.tailText = " $detail"
        }
        
        // 设置类型标签
        if (kind != null) {
            presentation.typeText = getKindLabel(kind)
        }
        
        // 创建查找元素
        val lookupElement = SimpleLookupElement(label, presentation)
        
        // 如果提供了插入文本，设置插入处理器
        if (insertText.isNotEmpty() && insertText != label) {
            lookupElement.insertHandler = LookupElementInsertHandler<LookupElement> { context, _ ->
                val document = context.document
                val startOffset = context.startOffset
                val currentOffset = context.tailOffset
                
                ApplicationManager.getApplication().runWriteAction {
                    document.deleteString(startOffset, currentOffset)
                    document.insertString(startOffset, insertText)
                }
            }
        }
        
        // 设置文档
        if (documentation != null) {
            lookupElement.psiElement = object : PsiElementBase() {
                override fun getParent() = null
                override fun getProject() = context.project
                override fun getContainingFile() = null
                override fun getTextRange() = TextRange.EMPTY_RANGE
                override fun getText() = null
                override fun navigate(requestFocus: Boolean) = Unit
                override fun canNavigate() = false
                override fun canNavigateToSource() = false
                override fun getTextOffset() = -1
                override fun toString() = "OmniSharpCompletionDocumentation"
                
                override fun getDocumentationToolTip() = documentation
            }
        }
        
        return lookupElement
    }
    
    /**
     * 根据类型获取图标
     */
    private fun getIconForKind(kind: String?): Icon {
        return when (kind) {
            "Method" -> AllIcons.Nodes.Method
            "Property" -> AllIcons.Nodes.Property
            "Field" -> AllIcons.Nodes.Field
            "Class" -> AllIcons.Nodes.Class
            "Interface" -> AllIcons.Nodes.Interface
            "Enum" -> AllIcons.Nodes.Enum
            "Struct" -> AllIcons.Nodes.Structure
            "Namespace" -> AllIcons.Nodes.Package
            else -> AllIcons.Nodes.Method
        }
    }
    
    /**
     * 获取类型标签
     */
    private fun getKindLabel(kind: String?): String {
        return when (kind) {
            "Method" -> "method"
            "Property" -> "property"
            "Field" -> "field"
            "Class" -> "class"
            "Interface" -> "interface"
            "Enum" -> "enum"
            "Struct" -> "struct"
            "Namespace" -> "namespace"
            else -> ""
        }
    }
}
```

### 5. CompletionHandler

CompletionHandler处理用户选择补全项后的逻辑，包括参数提示、光标定位等高级功能。

```kotlin
class CompletionHandler(private val completionService: OmniSharpCompletionService) {
    /**
     * 处理补全项选择
     */
    fun handleItemSelected(item: OmniSharpCompletionItem, editor: Editor, project: Project) {
        when (item.kind) {
            "Method" -> handleMethodCompletion(item, editor, project)
            "Property", "Field" -> handlePropertyCompletion(item, editor)
            // 可以添加其他类型的处理
            else -> handleDefaultCompletion(item, editor)
        }
    }
    
    /**
     * 处理方法补全
     */
    private fun handleMethodCompletion(item: OmniSharpCompletionItem, editor: Editor, project: Project) {
        // 检查是否需要显示参数提示
        val caretModel = editor.caretModel
        val offset = caretModel.offset
        
        // 触发参数提示
        ApplicationManager.getApplication().invokeLater {
            ParameterInfoController.showParameterInfo(editor) { paramList ->
                // 可以在这里提供参数信息
                null
            }
        }
        
        // 如果方法有参数，可以将光标定位到第一个参数位置
        val insertText = item.insertText
        val firstParamIndex = insertText.indexOf('(')
        if (firstParamIndex >= 0) {
            val hasParams = insertText.length > firstParamIndex + 1 && insertText[firstParamIndex + 1] != ')'
            if (hasParams) {
                ApplicationManager.getApplication().runWriteAction {
                    caretModel.moveToOffset(offset + firstParamIndex + 1)
                }
            }
        }
    }
    
    /**
     * 处理属性补全
     */
    private fun handlePropertyCompletion(item: OmniSharpCompletionItem, editor: Editor) {
        // 可以添加属性特定的处理逻辑
    }
    
    /**
     * 处理默认补全
     */
    private fun handleDefaultCompletion(item: OmniSharpCompletionItem, editor: Editor) {
        // 默认处理逻辑
    }
}
```

### 6. 扩展点注册

代码补全功能需要通过IntelliJ的扩展点进行注册，才能在编辑器中生效。

```xml
<!-- plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <!-- 注册代码补全提供者 -->
    <completion.contributor language="C#" implementationClass="com.intellij.omnisharp.completion.OmniSharpCompletionContributor"/>
    
    <!-- 注册代码补全配置 -->
    <applicationConfigurable id="OmniSharpCompletion" instance="com.intellij.omnisharp.completion.OmniSharpCompletionConfigurable"/>
</extensions>
```

```kotlin
class OmniSharpCompletionContributor : CompletionContributor() {
    init {
        // 注册代码补全提供者
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement(),
            OmniSharpCompletionProvider(ServiceManager.getService(OmniSharpCompletionService::class.java))
        )
        
        // 可以添加特定的触发字符
        extend(
            CompletionType.BASIC,
            PlatformPatterns.psiElement().afterLeaf(PsiElementPattern.Capture(StandardPatterns.string().equalTo("."))),
            OmniSharpCompletionProvider(ServiceManager.getService(OmniSharpCompletionService::class.java))
        )
    }
    
    override fun fillCompletionVariants(parameters: CompletionParameters, result: CompletionResultSet) {
        super.fillCompletionVariants(parameters, result)
        
        // 可以添加额外的补全处理
    }
}
```

## 缓存策略

### 1. LRU缓存实现

使用LRU（最近最少使用）缓存策略，保留最近使用的补全结果，当缓存达到最大容量时，自动移除最久未使用的项。

### 2. 缓存失效机制

- **文件变更失效**：当文件内容变更时，清除相关缓存
- **时间过期失效**：设置缓存项的过期时间，过期后自动失效
- **手动清除**：提供API允许其他组件在必要时清除缓存

### 3. 预缓存策略

- **编辑会话缓存**：在用户编辑会话中保持缓存
- **项目级缓存**：跨编辑会话缓存通用补全项

## 错误处理

### 1. 异常捕获

在补全请求和响应处理过程中捕获所有可能的异常，确保错误不会影响编辑器的正常使用。

### 2. 降级策略

- **本地补全**：当OmniSharp服务器不可用时，提供基于本地语法分析的基础补全
- **缓存回退**：使用最近的缓存结果作为备选

### 3. 错误日志

记录详细的错误日志，包括请求参数、响应数据和异常信息，便于问题诊断。

## 性能优化

### 1. 异步处理

所有与OmniSharp服务器的通信都在后台线程进行，避免阻塞UI线程。

### 2. 请求去抖动

实现请求去抖动机制，合并短时间内的多个补全请求，减少网络通信。

### 3. 增量更新

只传输和处理变化的部分数据，减少数据传输量和处理时间。

### 4. 批量处理

将多个小请求合并为一个批量请求，提高效率。

## 配置选项

代码补全功能提供丰富的配置选项，允许用户根据自己的偏好进行定制。

```kotlin
class OmniSharpCompletionConfigurable : SearchableConfigurable {
    private var form: OmniSharpCompletionConfigForm? = null
    
    override fun getId() = "OmniSharpCompletion"
    
    override fun getDisplayName() = "Code Completion"
    
    override fun createComponent(): JComponent {
        if (form == null) {
            form = OmniSharpCompletionConfigForm()
        }
        return form!!.panel
    }
    
    override fun isModified(): Boolean {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        return form!!.isEnabled != configManager.getConfiguration("completion.enabled", true) ||
               form!!.showDocumentation != configManager.getConfiguration("completion.show.documentation", true) ||
               form!!.showKindLabels != configManager.getConfiguration("completion.show.kind.labels", true) ||
               form!!.includeInvalid != configManager.getConfiguration("completion.include.invalid", false) ||
               form!!.cacheSize != configManager.getConfiguration("completion.cache.size", 100)
    }
    
    override fun apply() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        configManager.setConfiguration("completion.enabled", form!!.isEnabled)
        configManager.setConfiguration("completion.show.documentation", form!!.showDocumentation)
        configManager.setConfiguration("completion.show.kind.labels", form!!.showKindLabels)
        configManager.setConfiguration("completion.include.invalid", form!!.includeInvalid)
        configManager.setConfiguration("completion.cache.size", form!!.cacheSize)
        configManager.saveConfiguration()
        
        // 重置缓存
        val completionService = ServiceManager.getService(OmniSharpCompletionService::class.java)
        completionService.clearCache()
    }
    
    override fun reset() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        form!!.isEnabled = configManager.getConfiguration("completion.enabled", true)
        form!!.showDocumentation = configManager.getConfiguration("completion.show.documentation", true)
        form!!.showKindLabels = configManager.getConfiguration("completion.show.kind.labels", true)
        form!!.includeInvalid = configManager.getConfiguration("completion.include.invalid", false)
        form!!.cacheSize = configManager.getConfiguration("completion.cache.size", 100)
    }
}

class OmniSharpCompletionConfigForm {
    val panel: JPanel
    var isEnabled: Boolean by SwingProperty(false)
    var showDocumentation: Boolean by SwingProperty(false)
    var showKindLabels: Boolean by SwingProperty(false)
    var includeInvalid: Boolean by SwingProperty(false)
    var cacheSize: Int by SwingProperty(100)
    
    init {
        panel = JPanel(BorderLayout())
        
        val contentPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.WEST
        constraints.weightx = 1.0
        constraints.insets = Insets(5, 5, 5, 5)
        
        // 启用补全选项
        val enableCheckBox = JCheckBox("Enable code completion")
        enableCheckBox.bindSelectedTo(SwingProperty(this::isEnabled))
        constraints.gridy = 0
        contentPanel.add(enableCheckBox, constraints)
        
        // 显示文档选项
        val showDocsCheckBox = JCheckBox("Show documentation in completion popup")
        showDocsCheckBox.bindSelectedTo(SwingProperty(this::showDocumentation))
        constraints.gridy = 1
        contentPanel.add(showDocsCheckBox, constraints)
        
        // 显示类型标签选项
        val showKindLabelsCheckBox = JCheckBox("Show type labels in completion popup")
        showKindLabelsCheckBox.bindSelectedTo(SwingProperty(this::showKindLabels))
        constraints.gridy = 2
        contentPanel.add(showKindLabelsCheckBox, constraints)
        
        // 包含无效选项
        val includeInvalidCheckBox = JCheckBox("Include invalid completions")
        includeInvalidCheckBox.bindSelectedTo(SwingProperty(this::includeInvalid))
        constraints.gridy = 3
        contentPanel.add(includeInvalidCheckBox, constraints)
        
        // 缓存大小
        val cacheSizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val cacheSizeLabel = JLabel("Cache size:")
        val cacheSizeSpinner = JSpinner(SpinnerNumberModel(100, 10, 1000, 10))
        cacheSizeSpinner.bindValueTo(SwingProperty(this::cacheSize))
        cacheSizePanel.add(cacheSizeLabel)
        cacheSizePanel.add(cacheSizeSpinner)
        constraints.gridy = 4
        contentPanel.add(cacheSizePanel, constraints)
        
        panel.add(contentPanel, BorderLayout.NORTH)
        panel.add(JPanel(), BorderLayout.CENTER) // 填充空间
    }
}
```

## 测试策略

### 1. 单元测试

为每个核心组件编写单元测试，确保功能正确性。

### 2. 集成测试

测试与OmniSharp服务器的交互，验证补全请求和响应处理。

### 3. UI测试

测试在实际编辑器环境中的补全显示和用户交互。

## 使用示例

### 基本用法

在C#文件中，用户可以通过以下方式触发代码补全：

1. 输入代码时自动触发
2. 按Ctrl+Space手动触发
3. 在成员访问操作符（点号）后自动触发

### 高级用法

- **参数提示**：选择方法补全项后，会显示参数提示
- **文档查看**：在补全列表中可以查看项的详细文档
- **类型过滤**：可以根据补全项的类型进行过滤

## 总结

本文档详细描述了OmniSharp代码补全功能在IntelliJ平台中的集成实现方案。该方案通过模块化设计、高效缓存、异步处理等技术，确保了代码补全功能的高性能和良好用户体验。

主要特点：

- **完整集成**：与IntelliJ代码补全系统无缝集成
- **高性能**：通过缓存和异步处理确保低延迟
- **丰富功能**：支持多种补全类型和高级特性
- **可配置**：提供丰富的配置选项满足不同用户需求
- **错误处理**：完善的错误处理机制确保稳定性

通过这一实现方案，我们可以为IntelliJ平台上的C#开发者提供与Visual Studio相当的代码补全体验，大幅提高编码效率。