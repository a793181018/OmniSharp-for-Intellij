# OmniSharp代码导航功能集成实现方案

## 目录

1. [概述](#概述)
2. [架构设计](#架构设计)
3. [核心组件实现](#核心组件实现)
4. [定义查找实现](#定义查找实现)
5. [引用查找实现](#引用查找实现)
6. [缓存策略](#缓存策略)
7. [错误处理](#错误处理)
8. [性能优化](#性能优化)
9. [配置选项](#配置选项)
10. [测试策略](#测试策略)
11. [使用示例](#使用示例)
12. [总结](#总结)

## 概述

本文档详细描述OmniSharp代码导航功能在IntelliJ平台中的集成实现方案。代码导航是IDE的核心功能之一，包括定义查找和引用查找两大核心功能，帮助开发者在复杂代码库中快速定位和理解代码。本实现方案将OmniSharp服务器提供的代码导航能力与IntelliJ的导航系统无缝集成，提供高效、准确的代码导航体验。

### 功能特性

- **定义查找**：快速跳转到符号（类、方法、属性、字段等）的定义位置
- **引用查找**：查找项目中符号的所有引用，包括读写引用区分
- **智能上下文感知**：基于当前编辑位置的语法和语义上下文提供相关导航
- **层级展示**：以层级树或列表形式展示引用信息，便于浏览和筛选
- **引用分组**：按项目、文件、类型等方式对引用进行分组
- **行内引用高亮**：在编辑器中高亮显示当前符号的所有引用
- **远程导航**：支持跨文件、跨项目的导航功能

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
|           NavigationService              |
+---------------------+--------------------+
                      |
     +----------------+------------------+
     |                 |                  |
     v                 v                  v
+-----------------+-----------------+------------------+
| DefinitionNavigator | ReferenceNavigator | NavigationCache |
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

### 导航流程

#### 定义查找流程

1. **触发阶段**：用户在编辑器中对符号进行操作（如右键菜单、快捷键）触发定义查找
2. **分析阶段**：分析当前光标位置的符号，确定需要查找的目标
3. **请求阶段**：向OmniSharp服务器发送查找定义请求
4. **处理阶段**：接收定义位置响应，转换为IntelliJ导航目标
5. **导航阶段**：在编辑器中跳转到找到的定义位置

#### 引用查找流程

1. **触发阶段**：用户在编辑器中对符号进行操作触发引用查找
2. **分析阶段**：分析当前光标位置的符号，确定需要查找的目标
3. **请求阶段**：向OmniSharp服务器发送查找引用请求
4. **处理阶段**：接收引用列表响应，转换为IntelliJ引用结果
5. **展示阶段**：在结果窗口中展示找到的引用列表

## 核心组件实现

### 1. NavigationService

NavigationService是代码导航功能的核心服务类，负责协调定义查找和引用查找功能。

```kotlin
class NavigationService : Disposable {
    private val communicatorFactory: OmniSharpCommunicatorFactory
    private val sessionManager: SessionManager
    private val configurationManager: ConfigurationManager
    private val navigationCache: NavigationCache
    private val logger: Logger
    
    constructor(
        communicatorFactory: OmniSharpCommunicatorFactory,
        sessionManager: SessionManager,
        configurationManager: ConfigurationManager
    ) {
        this.communicatorFactory = communicatorFactory
        this.sessionManager = sessionManager
        this.configurationManager = configurationManager
        this.navigationCache = NavigationCache(configurationManager.getConfiguration("navigation.cache.size", 100))
        this.logger = Logger.getInstance("NavigationService")
    }
    
    /**
     * 获取定义位置
     */
    fun findDefinition(
        project: Project,
        file: VirtualFile,
        caretOffset: Int,
        document: Document
    ): CompletableFuture<List<DefinitionLocation>> {
        // 检查是否启用了定义查找功能
        if (!configurationManager.getConfiguration("navigation.definition.enabled", true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 验证文件是否为C#文件
        if (!file.fileType.name.equals("C#", ignoreCase = true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 构建缓存键
        val cacheKey = DefinitionCacheKey(file.path, caretOffset, document.text.hashCode())
        
        // 检查缓存
        val cachedDefinitions = navigationCache.getDefinition(cacheKey)
        if (cachedDefinitions != null) {
            logger.debug("Definition cache hit for ${file.name}:$caretOffset")
            return CompletableFuture.completedFuture(cachedDefinitions)
        }
        
        // 获取当前会话
        val session = sessionManager.getSession(project)
        if (session == null || !session.isAlive) {
            return CompletableFuture.failedFuture(IllegalStateException("OmniSharp session not available"))
        }
        
        // 收集上下文信息
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineText = document.getText(TextRange(lineStartOffset, document.getLineEndOffset(lineNumber)))
        val column = caretOffset - lineStartOffset
        
        // 构建请求参数
        val requestParams = mapOf(
            "FileName" to file.path,
            "Line" to (lineNumber + 1),  // OmniSharp使用1-based行号
            "Column" to (column + 1)  // OmniSharp使用1-based列号
        )
        
        logger.debug("Sending find definition request for ${file.name}:$lineNumber:$column")
        
        // 发送请求到OmniSharp服务器
        return session.communicator.sendRequest("findusages", requestParams)
            .thenApply { response ->
                // 处理响应
                val definitions = processDefinitionResponse(response)
                
                // 更新缓存
                navigationCache.putDefinition(cacheKey, definitions)
                
                definitions
            }
            .exceptionally { ex ->
                logger.warn("Error finding definitions", ex)
                emptyList()
            }
    }
    
    /**
     * 查找引用
     */
    fun findReferences(
        project: Project,
        file: VirtualFile,
        caretOffset: Int,
        document: Document,
        includeDefinition: Boolean = true,
        includeImplementations: Boolean = true,
        includeOverloads: Boolean = false
    ): CompletableFuture<List<ReferenceLocation>> {
        // 检查是否启用了引用查找功能
        if (!configurationManager.getConfiguration("navigation.references.enabled", true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 验证文件是否为C#文件
        if (!file.fileType.name.equals("C#", ignoreCase = true)) {
            return CompletableFuture.completedFuture(emptyList())
        }
        
        // 构建缓存键
        val cacheKey = ReferenceCacheKey(
            file.path,
            caretOffset,
            document.text.hashCode(),
            includeDefinition,
            includeImplementations,
            includeOverloads
        )
        
        // 检查缓存
        val cachedReferences = navigationCache.getReferences(cacheKey)
        if (cachedReferences != null) {
            logger.debug("Reference cache hit for ${file.name}:$caretOffset")
            return CompletableFuture.completedFuture(cachedReferences)
        }
        
        // 获取当前会话
        val session = sessionManager.getSession(project)
        if (session == null || !session.isAlive) {
            return CompletableFuture.failedFuture(IllegalStateException("OmniSharp session not available"))
        }
        
        // 收集上下文信息
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val column = caretOffset - lineStartOffset
        
        // 构建请求参数
        val requestParams = mapOf(
            "FileName" to file.path,
            "Line" to (lineNumber + 1),  // OmniSharp使用1-based行号
            "Column" to (column + 1),  // OmniSharp使用1-based列号
            "IncludeDefinition" to includeDefinition,
            "IncludeImplementations" to includeImplementations,
            "IncludeOverloads" to includeOverloads
        )
        
        logger.debug("Sending find references request for ${file.name}:$lineNumber:$column")
        
        // 发送请求到OmniSharp服务器
        return session.communicator.sendRequest("findusages", requestParams)
            .thenApply { response ->
                // 处理响应
                val references = processReferencesResponse(response)
                
                // 更新缓存
                navigationCache.putReferences(cacheKey, references)
                
                references
            }
            .exceptionally { ex ->
                logger.warn("Error finding references", ex)
                emptyList()
            }
    }
    
    /**
     * 处理定义响应
     */
    private fun processDefinitionResponse(response: OmniSharpResponse): List<DefinitionLocation> {
        try {
            val definitions = mutableListOf<DefinitionLocation>()
            
            // 检查响应是否包含定义数据
            if (response.success && response.body is Map<*, *>) {
                val body = response.body as Map<String, Any>
                val quickFixes = body["QuickFixes"] as? List<Map<String, Any>> ?: emptyList()
                
                for (fix in quickFixes) {
                    if (fix["FromSpan"] is Map<*, *> && fix["ToSpan"] is Map<*, *>) {
                        val fromSpan = fix["FromSpan"] as Map<String, Any>
                        val toSpan = fix["ToSpan"] as Map<String, Any>
                        
                        val definition = DefaultDefinitionLocation(
                            fileName = toSpan["FileName"] as? String,
                            line = (toSpan["StartLine"] as? Int ?: 0) - 1,  // 转换为0-based
                            column = (toSpan["StartColumn"] as? Int ?: 0) - 1,  // 转换为0-based
                            endLine = (toSpan["EndLine"] as? Int ?: 0) - 1,
                            endColumn = (toSpan["EndColumn"] as? Int ?: 0) - 1,
                            displayName = fix["Text"] as? String,
                            isWriteAccess = false  // 定义查找不区分读写
                        )
                        
                        definitions.add(definition)
                    }
                }
            }
            
            return definitions
        } catch (e: Exception) {
            logger.warn("Error processing definition response", e)
            return emptyList()
        }
    }
    
    /**
     * 处理引用响应
     */
    private fun processReferencesResponse(response: OmniSharpResponse): List<ReferenceLocation> {
        try {
            val references = mutableListOf<ReferenceLocation>()
            
            // 检查响应是否包含引用数据
            if (response.success && response.body is Map<*, *>) {
                val body = response.body as Map<String, Any>
                val quickFixes = body["QuickFixes"] as? List<Map<String, Any>> ?: emptyList()
                
                for (fix in quickFixes) {
                    if (fix["FromSpan"] is Map<*, *>) {
                        val fromSpan = fix["FromSpan"] as Map<String, Any>
                        
                        val reference = DefaultReferenceLocation(
                            fileName = fromSpan["FileName"] as? String,
                            line = (fromSpan["StartLine"] as? Int ?: 0) - 1,  // 转换为0-based
                            column = (fromSpan["StartColumn"] as? Int ?: 0) - 1,  // 转换为0-based
                            endLine = (fromSpan["EndLine"] as? Int ?: 0) - 1,
                            endColumn = (fromSpan["EndColumn"] as? Int ?: 0) - 1,
                            displayName = fix["Text"] as? String,
                            isWriteAccess = isWriteAccess(fix),
                            isDefinition = isDefinition(fix)
                        )
                        
                        references.add(reference)
                    }
                }
                
                // 应用排序规则
                return sortReferences(references)
            }
            
            return references
        } catch (e: Exception) {
            logger.warn("Error processing references response", e)
            return emptyList()
        }
    }
    
    /**
     * 判断是否为写访问
     */
    private fun isWriteAccess(fix: Map<String, Any>): Boolean {
        // 从响应数据中判断是否为写访问
        // 这里需要根据OmniSharp的响应格式进行实现
        val text = fix["Text"] as? String ?: ""
        return text.contains("write") || text.contains("set")
    }
    
    /**
     * 判断是否为定义
     */
    private fun isDefinition(fix: Map<String, Any>): Boolean {
        // 从响应数据中判断是否为定义
        val text = fix["Text"] as? String ?: ""
        return text.contains("definition") || text.contains("declaration")
    }
    
    /**
     * 排序引用
     */
    private fun sortReferences(references: List<ReferenceLocation>): List<ReferenceLocation> {
        // 定义优先，然后按文件名排序
        return references.sortedWith(compareBy<ReferenceLocation> {
            if (it.isDefinition) 0 else 1
        }.thenBy { it.fileName })
    }
    
    /**
     * 清理资源
     */
    override fun dispose() {
        navigationCache.clear()
    }
}
```

### 2. DefinitionNavigator

DefinitionNavigator实现了IntelliJ的GotoDeclarationHandler接口，处理定义查找功能。

```kotlin
class DefinitionNavigator(private val navigationService: NavigationService) : GotoDeclarationHandler {
    private val logger: Logger = Logger.getInstance("DefinitionNavigator")
    
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?, 
        offset: Int, 
        editor: Editor?
    ): Array<out PsiElement>? {
        if (sourceElement == null || editor == null) return null
        
        val project = editor.project ?: return null
        val file = editor.virtualFile ?: return null
        val document = editor.document
        
        // 获取定义位置
        try {
            val future = navigationService.findDefinition(project, file, offset, document)
            val definitions = future.get(5000, TimeUnit.MILLISECONDS)  // 5秒超时
            
            if (definitions.isEmpty()) return null
            
            // 转换为PsiElement数组
            val psiElements = mutableListOf<PsiElement>()
            val psiManager = PsiManager.getInstance(project)
            
            for (definition in definitions) {
                val targetFile = LocalFileSystem.getInstance().findFileByPath(definition.fileName ?: continue)
                if (targetFile != null) {
                    val psiFile = psiManager.findFile(targetFile)
                    if (psiFile != null) {
                        // 创建一个标记元素作为导航目标
                        val markerElement = createMarkerElement(psiFile, definition)
                        psiElements.add(markerElement)
                    }
                }
            }
            
            return psiElements.toTypedArray()
        } catch (e: Exception) {
            logger.warn("Error finding definition targets", e)
            return null
        }
    }
    
    /**
     * 创建标记元素
     */
    private fun createMarkerElement(psiFile: PsiFile, location: DefinitionLocation): PsiElement {
        return object : PsiElementBase() {
            override fun getParent(): PsiElement? = null
            override fun getProject(): Project = psiFile.project
            override fun getContainingFile(): PsiFile = psiFile
            override fun getTextRange(): TextRange {
                return TextRange(
                    psiFile.viewProvider.document!!.getLineStartOffset(location.line) + location.column,
                    psiFile.viewProvider.document!!.getLineStartOffset(location.endLine) + location.endColumn
                )
            }
            override fun getText(): String? = location.displayName
            override fun navigate(requestFocus: Boolean) {
                // 导航到定义位置
                OpenFileDescriptor(
                    project,
                    psiFile.virtualFile,
                    location.line,
                    location.column
                ).navigate(requestFocus)
            }
            override fun canNavigate(): Boolean = true
            override fun canNavigateToSource(): Boolean = true
            override fun getTextOffset(): Int {
                return psiFile.viewProvider.document!!.getLineStartOffset(location.line) + location.column
            }
            override fun toString(): String = "OmniSharpDefinitionMarker"
        }
    }
    
    override fun getActionText(context: Editor, element: PsiElement?): String? {
        return "Go to OmniSharp Definition"
    }
}
```

### 3. ReferenceNavigator

ReferenceNavigator实现了IntelliJ的FindUsagesHandler接口，处理引用查找功能。

```kotlin
class ReferenceNavigator(private val navigationService: NavigationService) : FindUsagesHandlerFactory {
    private val logger: Logger = Logger.getInstance("ReferenceNavigator")
    
    override fun canFindUsages(element: PsiElement): Boolean {
        // 检查元素是否支持引用查找
        return element is PsiIdentifier || element is PsiReferenceExpression
    }
    
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        return OmniSharpFindUsagesHandler(element, navigationService)
    }
    
    /**
     * OmniSharp查找引用处理器
     */
    private class OmniSharpFindUsagesHandler(
        element: PsiElement,
        private val navigationService: NavigationService
    ) : FindUsagesHandler(element) {
        
        override fun findReferencesToHighlight(usageSearchScope: UsageSearchScope): Collection<UsageInfo> {
            // 用于编辑器中的引用高亮
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return emptyList()
            val file = editor.virtualFile ?: return emptyList()
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            
            try {
                val future = navigationService.findReferences(project, file, caretOffset, document)
                val references = future.get(5000, TimeUnit.MILLISECONDS)  // 5秒超时
                
                return convertToUsageInfo(references, project)
            } catch (e: Exception) {
                logger.warn("Error finding references to highlight", e)
                return emptyList()
            }
        }
        
        override fun getPrimaryElements(): Array<out PsiElement> {
            return arrayOf(element)
        }
        
        override fun getFindUsagesOptions(psiElement: PsiElement): FindUsagesOptions {
            val options = FindUsagesOptions(project)
            options.isSearchForTextOccurrences = false  // 不搜索文本出现
            return options
        }
        
        override fun processElementUsages(
            element: PsiElement,
            processor: Processor<UsageInfo>,
            options: FindUsagesOptions
        ): Boolean {
            val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return true
            val file = editor.virtualFile ?: return true
            val document = editor.document
            val caretOffset = editor.caretModel.offset
            
            try {
                val future = navigationService.findReferences(
                    project,
                    file,
                    caretOffset,
                    document,
                    includeDefinition = options.isIncludeDefinition,
                    includeImplementations = options.isIncludeImplementations
                )
                
                val references = future.get(10000, TimeUnit.MILLISECONDS)  // 10秒超时
                val usageInfos = convertToUsageInfo(references, project)
                
                // 处理每个引用
                for (usageInfo in usageInfos) {
                    if (!processor.process(usageInfo)) {
                        return false  // 处理被取消
                    }
                }
                
                return true
            } catch (e: Exception) {
                logger.warn("Error processing element usages", e)
                return true  // 错误不应该中断处理
            }
        }
        
        /**
         * 转换为UsageInfo
         */
        private fun convertToUsageInfo(references: List<ReferenceLocation>, project: Project): Collection<UsageInfo> {
            val usageInfos = mutableListOf<UsageInfo>()
            val psiManager = PsiManager.getInstance(project)
            
            for (reference in references) {
                val targetFile = LocalFileSystem.getInstance().findFileByPath(reference.fileName ?: continue)
                if (targetFile != null) {
                    val psiFile = psiManager.findFile(targetFile)
                    if (psiFile != null) {
                        val document = psiFile.viewProvider.document ?: continue
                        
                        val startOffset = document.getLineStartOffset(reference.line) + reference.column
                        val endOffset = document.getLineStartOffset(reference.endLine) + reference.endColumn
                        
                        // 创建引用元素
                        val referenceElement = createReferenceElement(psiFile, startOffset, endOffset)
                        
                        // 创建UsageInfo
                        val usageInfo = UsageInfo(referenceElement)
                        
                        // 设置引用类型
                        if (reference.isDefinition) {
                            usageInfo.tooltipText = "Definition"
                        } else if (reference.isWriteAccess) {
                            usageInfo.tooltipText = "Write access"
                        } else {
                            usageInfo.tooltipText = "Read access"
                        }
                        
                        usageInfos.add(usageInfo)
                    }
                }
            }
            
            return usageInfos
        }
        
        /**
         * 创建引用元素
         */
        private fun createReferenceElement(psiFile: PsiFile, startOffset: Int, endOffset: Int): PsiElement {
            return object : PsiElementBase() {
                override fun getParent(): PsiElement? = null
                override fun getProject(): Project = psiFile.project
                override fun getContainingFile(): PsiFile = psiFile
                override fun getTextRange(): TextRange {
                    return TextRange(startOffset, endOffset)
                }
                override fun getText(): String? {
                    return psiFile.viewProvider.document?.getText(TextRange(startOffset, endOffset))
                }
                override fun navigate(requestFocus: Boolean) {
                    // 导航到引用位置
                    OpenFileDescriptor(
                        project,
                        psiFile.virtualFile,
                        startOffset
                    ).navigate(requestFocus)
                }
                override fun canNavigate(): Boolean = true
                override fun canNavigateToSource(): Boolean = true
                override fun getTextOffset(): Int = startOffset
                override fun toString(): String = "OmniSharpReferenceMarker"
            }
        }
    }
}
```

### 4. NavigationCache

NavigationCache实现了导航结果的缓存，减少对OmniSharp服务器的请求次数。

```kotlin
class NavigationCache(private val maxSize: Int) {
    private val definitionCache: LinkedHashMap<DefinitionCacheKey, List<DefinitionLocation>>
    private val referenceCache: LinkedHashMap<ReferenceCacheKey, List<ReferenceLocation>>
    private val lock = ReentrantReadWriteLock()
    
    init {
        // 创建访问顺序的LinkedHashMap作为LRU缓存
        definitionCache = object : LinkedHashMap<DefinitionCacheKey, List<DefinitionLocation>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<DefinitionCacheKey, List<DefinitionLocation>>): Boolean {
                return size > maxSize
            }
        }
        
        referenceCache = object : LinkedHashMap<ReferenceCacheKey, List<ReferenceLocation>>(16, 0.75f, true) {
            override fun removeEldestEntry(eldest: Map.Entry<ReferenceCacheKey, List<ReferenceLocation>>): Boolean {
                return size > maxSize
            }
        }
    }
    
    /**
     * 获取定义缓存
     */
    fun getDefinition(key: DefinitionCacheKey): List<DefinitionLocation>? {
        lock.readLock().lock()
        try {
            return definitionCache[key]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 存储定义缓存
     */
    fun putDefinition(key: DefinitionCacheKey, value: List<DefinitionLocation>) {
        lock.writeLock().lock()
        try {
            definitionCache[key] = value
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 获取引用缓存
     */
    fun getReferences(key: ReferenceCacheKey): List<ReferenceLocation>? {
        lock.readLock().lock()
        try {
            return referenceCache[key]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 存储引用缓存
     */
    fun putReferences(key: ReferenceCacheKey, value: List<ReferenceLocation>) {
        lock.writeLock().lock()
        try {
            referenceCache[key] = value
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
            val definitionKeysToRemove = definitionCache.keys.filter { it.filePath == filePath }
            definitionKeysToRemove.forEach { definitionCache.remove(it) }
            
            val referenceKeysToRemove = referenceCache.keys.filter { it.filePath == filePath }
            referenceKeysToRemove.forEach { referenceCache.remove(it) }
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
            definitionCache.clear()
            referenceCache.clear()
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    /**
     * 获取定义缓存大小
     */
    fun definitionCacheSize(): Int {
        lock.readLock().lock()
        try {
            return definitionCache.size
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 获取引用缓存大小
     */
    fun referenceCacheSize(): Int {
        lock.readLock().lock()
        try {
            return referenceCache.size
        } finally {
            lock.readLock().unlock()
        }
    }
}

/**
 * 定义缓存键
 */
data class DefinitionCacheKey(
    val filePath: String,
    val caretOffset: Int,
    val contentHash: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as DefinitionCacheKey
        
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

/**
 * 引用缓存键
 */
data class ReferenceCacheKey(
    val filePath: String,
    val caretOffset: Int,
    val contentHash: Int,
    val includeDefinition: Boolean,
    val includeImplementations: Boolean,
    val includeOverloads: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as ReferenceCacheKey
        
        if (filePath != other.filePath) return false
        if (caretOffset != other.caretOffset) return false
        if (contentHash != other.contentHash) return false
        if (includeDefinition != other.includeDefinition) return false
        if (includeImplementations != other.includeImplementations) return false
        if (includeOverloads != other.includeOverloads) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + caretOffset
        result = 31 * result + contentHash
        result = 31 * result + includeDefinition.hashCode()
        result = 31 * result + includeImplementations.hashCode()
        result = 31 * result + includeOverloads.hashCode()
        return result
    }
}
```

### 5. 数据模型

#### DefinitionLocation

定义位置的数据模型接口及实现。

```kotlin
interface DefinitionLocation {
    val fileName: String?
    val line: Int
    val column: Int
    val endLine: Int
    val endColumn: Int
    val displayName: String?
    val isWriteAccess: Boolean
}

class DefaultDefinitionLocation(
    override val fileName: String?,
    override val line: Int,
    override val column: Int,
    override val endLine: Int,
    override val endColumn: Int,
    override val displayName: String?,
    override val isWriteAccess: Boolean
) : DefinitionLocation
```

#### ReferenceLocation

引用位置的数据模型接口及实现。

```kotlin
interface ReferenceLocation {
    val fileName: String?
    val line: Int
    val column: Int
    val endLine: Int
    val endColumn: Int
    val displayName: String?
    val isWriteAccess: Boolean
    val isDefinition: Boolean
}

class DefaultReferenceLocation(
    override val fileName: String?,
    override val line: Int,
    override val column: Int,
    override val endLine: Int,
    override val endColumn: Int,
    override val displayName: String?,
    override val isWriteAccess: Boolean,
    override val isDefinition: Boolean
) : ReferenceLocation
```

### 6. 扩展点注册

代码导航功能需要通过IntelliJ的扩展点进行注册。

```xml
<!-- plugin.xml -->
<extensions defaultExtensionNs="com.intellij">
    <!-- 注册定义查找处理器 -->
    <gotoDeclarationHandler implementationClass="com.intellij.omnisharp.navigation.OmniSharpGotoDeclarationHandler"/>
    
    <!-- 注册引用查找处理器工厂 -->
    <findUsagesHandlerFactory implementationClass="com.intellij.omnisharp.navigation.OmniSharpFindUsagesHandlerFactory"/>
    
    <!-- 注册导航配置 -->
    <applicationConfigurable id="OmniSharpNavigation" instance="com.intellij.omnisharp.navigation.OmniSharpNavigationConfigurable"/>
</extensions>
```

```kotlin
class OmniSharpGotoDeclarationHandler(private val navigationService: NavigationService) : GotoDeclarationHandler {
    override fun getGotoDeclarationTargets(
        sourceElement: PsiElement?, 
        offset: Int, 
        editor: Editor?
    ): Array<out PsiElement>? {
        return DefinitionNavigator(navigationService).getGotoDeclarationTargets(sourceElement, offset, editor)
    }
    
    override fun getActionText(context: Editor, element: PsiElement?): String? {
        return "Go to OmniSharp Definition"
    }
}

class OmniSharpFindUsagesHandlerFactory(private val navigationService: NavigationService) : FindUsagesHandlerFactory {
    override fun canFindUsages(element: PsiElement): Boolean {
        return ReferenceNavigator(navigationService).canFindUsages(element)
    }
    
    override fun createFindUsagesHandler(element: PsiElement, forHighlightUsages: Boolean): FindUsagesHandler? {
        return ReferenceNavigator(navigationService).createFindUsagesHandler(element, forHighlightUsages)
    }
}
```

## 定义查找实现

### 1. 定义查找触发方式

- **快捷键**：支持默认的Ctrl+B/Command+B快捷键
- **右键菜单**：在右键菜单中添加"Go to Definition"选项
- **Ctrl+点击**：按住Ctrl键并点击符号跳转到定义

### 2. 多定义处理

当一个符号有多个定义时，提供选择界面让用户选择要跳转到的定义。

```kotlin
class MultipleDefinitionsHandler {
    /**
     * 处理多个定义的情况
     */
    fun handleMultipleDefinitions(
        project: Project,
        definitions: List<DefinitionLocation>
    ): DefinitionLocation? {
        if (definitions.size == 1) {
            return definitions.first()
        }
        
        // 创建选择器
        val choices = definitions.mapIndexed { index, definition ->
            val fileName = File(definition.fileName ?: "Unknown").name
            "$index. $fileName:${definition.line + 1} - ${definition.displayName}"
        }
        
        // 显示选择对话框
        val result = Messages.showChooseDialog(
            project,
            "Multiple definitions found. Choose one:",
            "Multiple Definitions",
            choices.toTypedArray(),
            choices.first(),
            Messages.getQuestionIcon()
        )
        
        return if (result >= 0 && result < definitions.size) {
            definitions[result]
        } else {
            null
        }
    }
}
```

## 引用查找实现

### 1. 引用查找触发方式

- **快捷键**：支持默认的Alt+F7快捷键
- **右键菜单**：在右键菜单中添加"Find Usages"选项
- **查找工具栏**：通过查找工具栏进行引用查找

### 2. 引用分组展示

支持按项目、文件、类型等方式对引用进行分组展示。

```kotlin
class ReferenceGrouper {
    /**
     * 按文件分组引用
     */
    fun groupByFile(references: List<ReferenceLocation>): Map<String, List<ReferenceLocation>> {
        return references.groupBy { it.fileName ?: "Unknown File" }
    }
    
    /**
     * 按类型分组引用（定义、读访问、写访问）
     */
    fun groupByType(references: List<ReferenceLocation>): Map<String, List<ReferenceLocation>> {
        return references.groupBy {
            when {
                it.isDefinition -> "Definitions"
                it.isWriteAccess -> "Write Access"
                else -> "Read Access"
            }
        }
    }
}
```

### 3. 引用高亮

在编辑器中高亮显示当前符号的所有引用。

```kotlin
class ReferenceHighlighter(private val navigationService: NavigationService) : TextEditorHighlightingPass {
    private val project: Project
    private val document: Document
    private val file: VirtualFile
    private val editor: Editor
    private val highlights: MutableList<TextRange>
    
    constructor(
        project: Project,
        editor: Editor,
        navigationService: NavigationService
    ) : super(project, editor.document) {
        this.project = project
        this.document = editor.document
        this.file = editor.virtualFile
        this.editor = editor
        this.highlights = mutableListOf()
    }
    
    override fun doCollectInformation() {
        val caretOffset = editor.caretModel.offset
        
        try {
            val future = navigationService.findReferences(project, file, caretOffset, document)
            val references = future.get(5000, TimeUnit.MILLISECONDS)  // 5秒超时
            
            // 过滤出当前文件的引用
            val currentFileReferences = references.filter { it.fileName == file.path }
            
            // 转换为TextRange
            for (reference in currentFileReferences) {
                val startOffset = document.getLineStartOffset(reference.line) + reference.column
                val endOffset = document.getLineStartOffset(reference.endLine) + reference.endColumn
                highlights.add(TextRange(startOffset, endOffset))
            }
        } catch (e: Exception) {
            Logger.getInstance(javaClass).warn("Error highlighting references", e)
        }
    }
    
    override fun doApplyInformationToEditor() {
        // 获取或创建highlighter
        val highlighter = editor.markupModel
        
        // 移除旧的高亮
        highlighter.removeAllHighlighters()
        
        // 添加新的高亮
        for (range in highlights) {
            highlighter.addRangeHighlighter(
                range.startOffset,
                range.endOffset,
                HighlighterLayer.SELECTION,
                TextAttributes.ERASE_MARKER,
                HighlighterTargetArea.EXACT_RANGE
            )
        }
    }
}
```

## 缓存策略

### 1. LRU缓存实现

使用LRU（最近最少使用）缓存策略，保留最近使用的导航结果。

### 2. 缓存失效机制

- **文件变更失效**：当文件内容变更时，清除相关缓存
- **时间过期失效**：设置缓存项的过期时间
- **项目刷新失效**：项目结构发生变化时，清除缓存

### 3. 缓存级别

- **会话级缓存**：在编辑会话中保持
- **项目级缓存**：跨会话缓存通用导航结果

## 错误处理

### 1. 异常捕获

在导航请求和响应处理过程中捕获所有可能的异常。

### 2. 超时处理

为导航请求设置合理的超时时间，避免长时间阻塞UI。

### 3. 降级策略

- **本地回退**：当OmniSharp服务器不可用时，使用IntelliJ的本地导航功能
- **缓存回退**：使用最近的缓存结果作为备选

### 4. 错误提示

当导航失败时，提供友好的错误提示。

```kotlin
class NavigationErrorHandler {
    /**
     * 显示导航错误
     */
    fun showNavigationError(project: Project, errorMessage: String) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showErrorDialog(
                project,
                errorMessage,
                "Navigation Error"
            )
        }
    }
    
    /**
     * 处理导航超时
     */
    fun handleTimeout(project: Project) {
        showNavigationError(
            project,
            "Navigation request timed out. Please try again or check your OmniSharp server connection."
        )
    }
    
    /**
     * 处理未找到定义
     */
    fun handleDefinitionNotFound(project: Project) {
        ApplicationManager.getApplication().invokeLater {
            Messages.showInfoMessage(
                project,
                "Cannot find definition",
                "Information"
            )
        }
    }
}
```

## 性能优化

### 1. 异步处理

所有导航请求都在后台线程进行，避免阻塞UI线程。

### 2. 增量更新

对于大项目的引用查找，支持增量更新显示结果。

### 3. 缓存优化

- **精确缓存键**：使用文件路径、光标位置和内容哈希作为缓存键
- **批量缓存**：缓存整个查找结果，而不是单个位置

### 4. 网络优化

- **压缩传输**：启用请求和响应的压缩
- **请求合并**：合并短时间内的多个相同请求

## 配置选项

代码导航功能提供丰富的配置选项，允许用户根据自己的偏好进行定制。

```kotlin
class OmniSharpNavigationConfigurable : SearchableConfigurable {
    private var form: OmniSharpNavigationConfigForm? = null
    
    override fun getId() = "OmniSharpNavigation"
    
    override fun getDisplayName() = "Code Navigation"
    
    override fun createComponent(): JComponent {
        if (form == null) {
            form = OmniSharpNavigationConfigForm()
        }
        return form!!.panel
    }
    
    override fun isModified(): Boolean {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        return form!!.definitionEnabled != configManager.getConfiguration("navigation.definition.enabled", true) ||
               form!!.referencesEnabled != configManager.getConfiguration("navigation.references.enabled", true) ||
               form!!.includeDefinitionInReferences != configManager.getConfiguration("navigation.references.include.definition", true) ||
               form!!.includeImplementationsInReferences != configManager.getConfiguration("navigation.references.include.implementations", true) ||
               form!!.highlightReferences != configManager.getConfiguration("navigation.highlight.references", true) ||
               form!!.cacheSize != configManager.getConfiguration("navigation.cache.size", 100)
    }
    
    override fun apply() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        configManager.setConfiguration("navigation.definition.enabled", form!!.definitionEnabled)
        configManager.setConfiguration("navigation.references.enabled", form!!.referencesEnabled)
        configManager.setConfiguration("navigation.references.include.definition", form!!.includeDefinitionInReferences)
        configManager.setConfiguration("navigation.references.include.implementations", form!!.includeImplementationsInReferences)
        configManager.setConfiguration("navigation.highlight.references", form!!.highlightReferences)
        configManager.setConfiguration("navigation.cache.size", form!!.cacheSize)
        configManager.saveConfiguration()
        
        // 重置缓存
        val navigationService = ServiceManager.getService(NavigationService::class.java)
        (navigationService as? NavigationService)?.clearCache()
    }
    
    override fun reset() {
        val configManager = ServiceManager.getService(ConfigurationManager::class.java)
        form!!.definitionEnabled = configManager.getConfiguration("navigation.definition.enabled", true)
        form!!.referencesEnabled = configManager.getConfiguration("navigation.references.enabled", true)
        form!!.includeDefinitionInReferences = configManager.getConfiguration("navigation.references.include.definition", true)
        form!!.includeImplementationsInReferences = configManager.getConfiguration("navigation.references.include.implementations", true)
        form!!.highlightReferences = configManager.getConfiguration("navigation.highlight.references", true)
        form!!.cacheSize = configManager.getConfiguration("navigation.cache.size", 100)
    }
}

class OmniSharpNavigationConfigForm {
    val panel: JPanel
    var definitionEnabled: Boolean by SwingProperty(false)
    var referencesEnabled: Boolean by SwingProperty(false)
    var includeDefinitionInReferences: Boolean by SwingProperty(false)
    var includeImplementationsInReferences: Boolean by SwingProperty(false)
    var highlightReferences: Boolean by SwingProperty(false)
    var cacheSize: Int by SwingProperty(100)
    
    init {
        panel = JPanel(BorderLayout())
        
        val contentPanel = JPanel(GridBagLayout())
        val constraints = GridBagConstraints()
        constraints.fill = GridBagConstraints.HORIZONTAL
        constraints.anchor = GridBagConstraints.WEST
        constraints.weightx = 1.0
        constraints.insets = Insets(5, 5, 5, 5)
        
        // 启用定义查找选项
        val enableDefinitionCheckBox = JCheckBox("Enable definition navigation")
        enableDefinitionCheckBox.bindSelectedTo(SwingProperty(this::definitionEnabled))
        constraints.gridy = 0
        contentPanel.add(enableDefinitionCheckBox, constraints)
        
        // 启用引用查找选项
        val enableReferencesCheckBox = JCheckBox("Enable references navigation")
        enableReferencesCheckBox.bindSelectedTo(SwingProperty(this::referencesEnabled))
        constraints.gridy = 1
        contentPanel.add(enableReferencesCheckBox, constraints)
        
        // 引用查找中包含定义
        val includeDefinitionCheckBox = JCheckBox("Include definition in references results")
        includeDefinitionCheckBox.bindSelectedTo(SwingProperty(this::includeDefinitionInReferences))
        constraints.gridy = 2
        constraints.insets = Insets(5, 20, 5, 5)  // 缩进
        contentPanel.add(includeDefinitionCheckBox, constraints)
        
        // 引用查找中包含实现
        val includeImplementationsCheckBox = JCheckBox("Include implementations in references results")
        includeImplementationsCheckBox.bindSelectedTo(SwingProperty(this::includeImplementationsInReferences))
        constraints.gridy = 3
        contentPanel.add(includeImplementationsCheckBox, constraints)
        
        // 高亮引用选项
        constraints.insets = Insets(5, 5, 5, 5)  // 恢复缩进
        val highlightReferencesCheckBox = JCheckBox("Highlight references in editor")
        highlightReferencesCheckBox.bindSelectedTo(SwingProperty(this::highlightReferences))
        constraints.gridy = 4
        contentPanel.add(highlightReferencesCheckBox, constraints)
        
        // 缓存大小
        val cacheSizePanel = JPanel(FlowLayout(FlowLayout.LEFT))
        val cacheSizeLabel = JLabel("Cache size:")
        val cacheSizeSpinner = JSpinner(SpinnerNumberModel(100, 10, 1000, 10))
        cacheSizeSpinner.bindValueTo(SwingProperty(this::cacheSize))
        cacheSizePanel.add(cacheSizeLabel)
        cacheSizePanel.add(cacheSizeSpinner)
        constraints.gridy = 5
        contentPanel.add(cacheSizePanel, constraints)
        
        panel.add(contentPanel, BorderLayout.NORTH)
        panel.add(JPanel(), BorderLayout.CENTER) // 填充空间
    }
}
```

## 测试策略

### 1. 单元测试

为每个核心组件编写单元测试，验证功能正确性。

### 2. 集成测试

测试与OmniSharp服务器的交互，验证导航请求和响应处理。

### 3. UI测试

测试在实际编辑器环境中的导航体验和用户交互。

## 使用示例

### 定义查找

在C#文件中，用户可以通过以下方式触发定义查找：

1. 将光标放在符号上，按F12或Ctrl+B/Command+B
2. 右键点击符号，选择"Go to Definition"
3. 按住Ctrl键并点击符号

### 引用查找

在C#文件中，用户可以通过以下方式触发引用查找：

1. 将光标放在符号上，按Alt+F7
2. 右键点击符号，选择"Find Usages"
3. 使用查找工具栏搜索符号的引用

## 总结

本文档详细描述了OmniSharp代码导航功能在IntelliJ平台中的集成实现方案。该方案通过模块化设计、高效缓存、异步处理等技术，确保了代码导航功能的高性能和良好用户体验。

主要特点：

- **完整集成**：与IntelliJ导航系统无缝集成
- **高性能**：通过缓存和异步处理确保低延迟
- **丰富功能**：支持定义查找、引用查找、引用高亮等功能
- **智能分组**：支持多种引用分组方式，便于浏览大量引用
- **可配置**：提供丰富的配置选项满足不同用户需求
- **错误处理**：完善的错误处理机制确保稳定性

通过这一实现方案，我们可以为IntelliJ平台上的C#开发者提供高效、准确的代码导航体验，大幅提高代码理解和开发效率。