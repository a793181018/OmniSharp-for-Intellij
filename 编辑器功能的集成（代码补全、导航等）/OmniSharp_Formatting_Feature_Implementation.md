# OmniSharp 代码格式化功能集成实现方案

## 1. 概述

本文档详细描述了在 IntelliJ 平台中集成 OmniSharp 代码格式化功能的实现方案。该功能允许开发者在 IntelliJ IDE 中对 C# 代码进行自动格式化，保持代码风格一致性和可读性。

### 1.1 功能目标

- 支持对整个文件、选定区域或特定代码块进行格式化
- 提供多种格式化选项，包括缩进、空格、换行等
- 集成到 IntelliJ 的格式化系统中，支持快捷键和右键菜单
- 支持异步格式化，不阻塞 UI 线程
- 提供格式化预览功能

### 1.2 技术依赖

- IntelliJ Platform SDK
- OmniSharp 通信协议
- Kotlin 语言

## 2. 架构设计

### 2.1 组件关系图

```
+--------------------------------+     +--------------------------------+
|                                |     |                                |
|  IntelliJ Formatter API        |     |  OmniSharp Formatting API       |
|                                |     |                                |
+--------------------------------+     +--------------------------------+
                 |                               |
                 v                               v
+--------------------------------+     +--------------------------------+
|                                |     |                                |
|  OmniSharpFormatter            |     |  OmniSharpFormattingService     |
|                                |     |                                |
+--------------------------------+     +--------------------------------+
                 |                               ^
                 |                               |
                 v                               |
+--------------------------------+     +--------------------------------+
|                                |     |                                |
|  FormattingCache               |     |  OmniSharpCommunicator          |
|                                |     |                                |
+--------------------------------+     +--------------------------------+
```

### 2.2 格式化流程

1. 用户触发格式化操作（快捷键、菜单）
2. IntelliJ 平台调用 OmniSharpFormatter
3. OmniSharpFormatter 准备格式化请求参数
4. OmniSharpFormattingService 发送请求到 OmniSharp 服务器
5. 处理服务器返回的格式化结果
6. 应用格式化后的代码到编辑器

## 3. 核心组件实现

### 3.1 OmniSharpFormattingService

```kotlin
package com.intellij.csharp.omnisharp.services

import com.intellij.csharp.omnisharp.OmniSharpBundle
import com.intellij.csharp.omnisharp.OmniSharpSettings
import com.intellij.csharp.omnisharp.session.OmniSharpSession
import com.intellij.csharp.omnisharp.session.SessionManager
import com.intellij.csharp.omnisharp.protocol.requests.FormatRequest
import com.intellij.csharp.omnisharp.protocol.responses.FormatResponse
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * OmniSharp 代码格式化服务
 * 负责与 OmniSharp 服务器通信，执行代码格式化操作
 */
class OmniSharpFormattingService(private val sessionManager: SessionManager) {
    private val logger = Logger.getInstance(OmniSharpFormattingService::class.java)
    private val formattingCache = ConcurrentHashMap<String, String>()
    
    /**
     * 格式化整个文件
     */
    fun formatFile(project: Project, file: VirtualFile, fileContent: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        
        // 检查缓存
        val cacheKey = generateCacheKey(file.path, fileContent)
        val cachedResult = formattingCache[cacheKey]
        if (cachedResult != null) {
            future.complete(cachedResult)
            return future
        }
        
        try {
            val session = sessionManager.getSession(project)
            if (session == null || !session.isAlive) {
                future.completeExceptionally(IllegalStateException(OmniSharpBundle.message("omnisharp.session.not.available")))
                return future
            }
            
            val request = FormatRequest(
                fileName = file.path,
                text = fileContent,
                range = null, // null 表示格式化整个文件
                wantSpans = false
            )
            
            session.communicator.sendRequest("v2/format/file", request.toJson())
                .thenAccept {
                    try {
                        val response = FormatResponse.fromJson(it)
                        val formattedText = response.buffer
                        formattingCache[cacheKey] = formattedText
                        future.complete(formattedText)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }
                .exceptionally { ex ->
                    logger.error("Failed to format file: ${file.path}", ex)
                    future.completeExceptionally(ex)
                    null
                }
        } catch (e: Exception) {
            logger.error("Error formatting file: ${file.path}", e)
            future.completeExceptionally(e)
        }
        
        return future
    }
    
    /**
     * 格式化文件的选定区域
     */
    fun formatSelection(project: Project, file: VirtualFile, fileContent: String, startOffset: Int, endOffset: Int): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        
        try {
            val session = sessionManager.getSession(project)
            if (session == null || !session.isAlive) {
                future.completeExceptionally(IllegalStateException(OmniSharpBundle.message("omnisharp.session.not.available")))
                return future
            }
            
            val request = FormatRequest(
                fileName = file.path,
                text = fileContent,
                range = FormatRequest.Range(
                    start = FormatRequest.Position(line = 0, character = startOffset),
                    end = FormatRequest.Position(line = 0, character = endOffset)
                ),
                wantSpans = false
            )
            
            session.communicator.sendRequest("v2/format/selection", request.toJson())
                .thenAccept {
                    try {
                        val response = FormatResponse.fromJson(it)
                        future.complete(response.buffer)
                    } catch (e: Exception) {
                        future.completeExceptionally(e)
                    }
                }
                .exceptionally { ex ->
                    logger.error("Failed to format selection in file: ${file.path}", ex)
                    future.completeExceptionally(ex)
                    null
                }
        } catch (e: Exception) {
            logger.error("Error formatting selection in file: ${file.path}", e)
            future.completeExceptionally(e)
        }
        
        return future
    }
    
    /**
     * 生成缓存键
     */
    private fun generateCacheKey(filePath: String, content: String): String {
        return "$filePath:${content.hashCode()}"
    }
    
    /**
     * 清除缓存
     */
    fun clearCache(filePath: String? = null) {
        if (filePath != null) {
            formattingCache.keys.removeIf { it.startsWith(filePath) }
        } else {
            formattingCache.clear()
        }
    }
}
```

### 3.2 OmniSharpFormatter

```kotlin
package com.intellij.csharp.omnisharp.formatting

import com.intellij.csharp.omnisharp.services.OmniSharpFormattingService
import com.intellij.csharp.omnisharp.session.SessionManager
import com.intellij.formatting.*
import com.intellij.lang.ASTNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import java.util.concurrent.CompletableFuture

/**
 * OmniSharp 格式化器实现
 * 集成到 IntelliJ 的格式化系统中
 */
class OmniSharpFormatter(
    private val formattingService: OmniSharpFormattingService
) : Formatter {
    private val logger = Logger.getInstance(OmniSharpFormatter::class.java)
    
    override fun getTextRangesToFormat(psiFile: PsiFile, range: TextRange, settings: FormattingMode): MutableList<TextRange> {
        // 默认返回整个请求的范围
        return mutableListOf(range)
    }
    
    override fun format(psiFile: PsiFile, settings: FormattingModel): FormattingModel {
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile) ?: return settings
        
        try {
            // 获取文件内容
            val fileContent = document.text
            val virtualFile = psiFile.virtualFile ?: return settings
            
            // 执行格式化
            val formattedContent = formattingService.formatFile(project, virtualFile, fileContent).get()
            
            // 应用格式化结果
            if (formattedContent != fileContent) {
                document.replaceString(0, document.textLength, formattedContent)
                PsiDocumentManager.getInstance(project).commitDocument(document)
            }
        } catch (e: Exception) {
            logger.error("Error formatting file: ${psiFile.name}", e)
        }
        
        return settings
    }
    
    /**
     * 异步格式化文件
     */
    fun formatAsync(psiFile: PsiFile): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        
        if (document == null) {
            future.complete(false)
            return future
        }
        
        try {
            val fileContent = document.text
            val virtualFile = psiFile.virtualFile ?: run {
                future.complete(false)
                return future
            }
            
            formattingService.formatFile(project, virtualFile, fileContent)
                .thenAccept { formattedContent ->
                    if (formattedContent != fileContent) {
                        try {
                            // 在 EDT 线程中应用更改
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                try {
                                    document.replaceString(0, document.textLength, formattedContent)
                                    PsiDocumentManager.getInstance(project).commitDocument(document)
                                    future.complete(true)
                                } catch (e: Exception) {
                                    logger.error("Failed to apply formatted content to document", e)
                                    future.complete(false)
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error applying formatted content", e)
                            future.complete(false)
                        }
                    } else {
                        future.complete(false) // 没有变化
                    }
                }
                .exceptionally { ex ->
                    logger.error("Error formatting file asynchronously: ${psiFile.name}", ex)
                    future.complete(false)
                    null
                }
        } catch (e: Exception) {
            logger.error("Error preparing formatting", e)
            future.complete(false)
        }
        
        return future
    }
    
    /**
     * 异步格式化选择区域
     */
    fun formatSelectionAsync(psiFile: PsiFile, startOffset: Int, endOffset: Int): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        val project = psiFile.project
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        
        if (document == null || startOffset < 0 || endOffset > document.textLength) {
            future.complete(false)
            return future
        }
        
        try {
            val fileContent = document.text
            val virtualFile = psiFile.virtualFile ?: run {
                future.complete(false)
                return future
            }
            
            formattingService.formatSelection(project, virtualFile, fileContent, startOffset, endOffset)
                .thenAccept { formattedContent ->
                    // 只替换选中的部分
                    val originalSelection = fileContent.substring(startOffset, endOffset)
                    if (formattedContent != originalSelection) {
                        try {
                            // 在 EDT 线程中应用更改
                            com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                                try {
                                    document.replaceString(startOffset, endOffset, formattedContent)
                                    PsiDocumentManager.getInstance(project).commitDocument(document)
                                    future.complete(true)
                                } catch (e: Exception) {
                                    logger.error("Failed to apply formatted selection to document", e)
                                    future.complete(false)
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("Error applying formatted selection", e)
                            future.complete(false)
                        }
                    } else {
                        future.complete(false) // 没有变化
                    }
                }
                .exceptionally { ex ->
                    logger.error("Error formatting selection asynchronously: ${psiFile.name}", ex)
                    future.complete(false)
                    null
                }
        } catch (e: Exception) {
            logger.error("Error preparing selection formatting", e)
            future.complete(false)
        }
        
        return future
    }
}
```

### 3.3 FormattingCache

```kotlin
package com.intellij.csharp.omnisharp.formatting.cache

import com.intellij.openapi.diagnostic.Logger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 格式化结果缓存
 * 存储最近格式化的文件结果，提高性能
 */
class FormattingCache(private val maxSize: Int = 100, private val ttlMinutes: Long = 10) {
    private val logger = Logger.getInstance(FormattingCache::class.java)
    private val cache = ConcurrentHashMap<String, CacheEntry>()
    private val accessCounter = AtomicInteger(0)
    
    data class CacheEntry(
        val content: String,
        val timestamp: Long,
        var accessCount: Int
    )
    
    /**
     * 获取缓存的格式化结果
     */
    fun get(key: String): String? {
        val entry = cache[key]
        if (entry != null) {
            // 检查是否过期
            if (isExpired(entry.timestamp)) {
                remove(key)
                return null
            }
            
            // 更新访问计数
            entry.accessCount++
            return entry.content
        }
        return null
    }
    
    /**
     * 存储格式化结果到缓存
     */
    fun put(key: String, content: String) {
        // 检查缓存大小，如果超过限制，进行清理
        if (cache.size >= maxSize) {
            evictEntries()
        }
        
        cache[key] = CacheEntry(content, System.currentTimeMillis(), 0)
        accessCounter.incrementAndGet()
        
        // 每 100 次访问清理一次过期条目
        if (accessCounter.get() % 100 == 0) {
            cleanupExpiredEntries()
        }
    }
    
    /**
     * 移除特定键的缓存
     */
    fun remove(key: String) {
        cache.remove(key)
    }
    
    /**
     * 清除特定文件的所有缓存
     */
    fun clearFileCache(filePath: String) {
        cache.keys.removeIf { it.startsWith(filePath) }
    }
    
    /**
     * 清空缓存
     */
    fun clear() {
        cache.clear()
    }
    
    /**
     * 检查条目是否过期
     */
    private fun isExpired(timestamp: Long): Boolean {
        val now = System.currentTimeMillis()
        val expirationTime = timestamp + TimeUnit.MINUTES.toMillis(ttlMinutes)
        return now > expirationTime
    }
    
    /**
     * 清理过期条目
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        val expiredKeys = cache.entries.filter { 
            isExpired(it.value.timestamp)
        }.map { it.key }
        
        expiredKeys.forEach { cache.remove(it) }
        
        if (expiredKeys.isNotEmpty()) {
            logger.debug("Cleaned up ${expiredKeys.size} expired formatting cache entries")
        }
    }
    
    /**
     * 驱逐策略：移除最少访问的条目
     */
    private fun evictEntries() {
        // 保留最近的 80% 条目
        val keepCount = (maxSize * 0.8).toInt()
        val entriesToKeep = cache.entries
            .sortedByDescending { it.value.accessCount }
            .take(keepCount)
            .map { it.key }
        
        val keysToRemove = cache.keys - entriesToKeep.toSet()
        keysToRemove.forEach { cache.remove(it) }
        
        logger.debug("Evicted ${keysToRemove.size} least accessed formatting cache entries")
    }
}
```

### 3.4 FormattingAction

```kotlin
package com.intellij.csharp.omnisharp.formatting.actions

import com.intellij.csharp.omnisharp.OmniSharpBundle
import com.intellij.csharp.omnisharp.formatting.OmniSharpFormatter
import com.intellij.csharp.omnisharp.services.OmniSharpFormattingService
import com.intellij.csharp.omnisharp.session.SessionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFile
import javax.inject.Inject

/**
 * 格式化文件操作
 */
class FormatDocumentAction @Inject constructor(
    private val formattingService: OmniSharpFormattingService,
    private val sessionManager: SessionManager
) : AnAction(OmniSharpBundle.message("action.format.document.text")), DumbAware {
    private val logger = Logger.getInstance(FormatDocumentAction::class.java)
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        
        // 只有当项目、编辑器和文件都可用，并且是C#文件时才启用该操作
        e.presentation.isEnabled = project != null && editor != null && file != null && 
                file.fileType.name.equals("C#", ignoreCase = true) &&
                sessionManager.getSession(project)?.isAlive == true
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        // 检查是否有选中区域
        if (editor.selectionModel.hasSelection()) {
            // 格式化选中区域
            formatSelection(project, editor, file)
        } else {
            // 格式化整个文档
            formatDocument(project, editor, file)
        }
    }
    
    /**
     * 格式化整个文档
     */
    private fun formatDocument(project: Project, editor: Editor, file: PsiFile) {
        val formatter = OmniSharpFormatter(formattingService)
        
        // 显示进度指示器
        val progressIndicator = com.intellij.openapi.progress.ProgressManager.getInstance().progressIndicator
        progressIndicator?.text = OmniSharpBundle.message("formatting.in.progress")
        
        formatter.formatAsync(file)
            .thenAccept { success ->
                if (!success) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            OmniSharpBundle.message("formatting.failed"),
                            OmniSharpBundle.message("formatting.error.title")
                        )
                    }
                }
            }
            .exceptionally { ex ->
                logger.error("Error formatting document", ex)
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        OmniSharpBundle.message("formatting.failed.with.error", ex.message),
                        OmniSharpBundle.message("formatting.error.title")
                    )
                }
                null
            }
    }
    
    /**
     * 格式化选中区域
     */
    private fun formatSelection(project: Project, editor: Editor, file: PsiFile) {
        val formatter = OmniSharpFormatter(formattingService)
        val startOffset = editor.selectionModel.selectionStart
        val endOffset = editor.selectionModel.selectionEnd
        
        formatter.formatSelectionAsync(file, startOffset, endOffset)
            .thenAccept { success ->
                if (!success) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            OmniSharpBundle.message("formatting.selection.failed"),
                            OmniSharpBundle.message("formatting.error.title")
                        )
                    }
                }
            }
            .exceptionally { ex ->
                logger.error("Error formatting selection", ex)
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        OmniSharpBundle.message("formatting.selection.failed.with.error", ex.message),
                        OmniSharpBundle.message("formatting.error.title")
                    )
                }
                null
            }
    }
}

/**
 * 格式化选中区域操作
 */
class FormatSelectionAction @Inject constructor(
    private val formattingService: OmniSharpFormattingService,
    private val sessionManager: SessionManager
) : AnAction(OmniSharpBundle.message("action.format.selection.text")), DumbAware {
    private val logger = Logger.getInstance(FormatSelectionAction::class.java)
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val file = e.getData(CommonDataKeys.PSI_FILE)
        
        // 只有当项目、编辑器、文件都可用，是C#文件，并且有选中区域时才启用该操作
        e.presentation.isEnabled = project != null && editor != null && file != null && 
                editor.selectionModel.hasSelection() &&
                file.fileType.name.equals("C#", ignoreCase = true) &&
                sessionManager.getSession(project)?.isAlive == true
    }
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        val startOffset = editor.selectionModel.selectionStart
        val endOffset = editor.selectionModel.selectionEnd
        
        val formatter = OmniSharpFormatter(formattingService)
        formatter.formatSelectionAsync(file, startOffset, endOffset)
            .thenAccept { success ->
                if (!success) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        Messages.showErrorDialog(
                            project,
                            OmniSharpBundle.message("formatting.selection.failed"),
                            OmniSharpBundle.message("formatting.error.title")
                        )
                    }
                }
            }
            .exceptionally { ex ->
                logger.error("Error formatting selection", ex)
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        OmniSharpBundle.message("formatting.selection.failed.with.error", ex.message),
                        OmniSharpBundle.message("formatting.error.title")
                    )
                }
                null
            }
    }
}
```

### 3.5 FormattingConfig

```kotlin
package com.intellij.csharp.omnisharp.formatting.config

import com.intellij.csharp.omnisharp.OmniSharpSettings
import com.intellij.csharp.omnisharp.protocol.requests.FormatRequest
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * 格式化配置
 */
@State(
    name = "OmniSharpFormattingConfig",
    storages = [Storage("omnisharp_formatting.xml")]
)
class FormattingConfig : PersistentStateComponent<FormattingConfig.State> {
    
    data class State(
        var indentSize: Int = 4,
        var tabSize: Int = 4,
        var useTabs: Boolean = false,
        var newlineForBraces: Boolean = true,
        var spaceAfterCast: Boolean = true,
        var spaceAfterKeywordInControlFlowStatements: Boolean = true,
        var spaceBeforeOpenSquareBracket: Boolean = false,
        var spaceBetweenEmptySquareBrackets: Boolean = false,
        var spaceAfterColonInBaseTypeDeclaration: Boolean = true,
        var spaceAfterComma: Boolean = true,
        var spaceAfterDot: Boolean = false,
        var spaceAfterSemicolonsInForStatements: Boolean = true,
        var spaceBeforeColonInBaseTypeDeclaration: Boolean = false,
        var spaceBeforeComma: Boolean = false,
        var spaceBeforeDot: Boolean = false,
        var spaceBeforeSemicolonsInForStatements: Boolean = false,
        var spaceBeforeOpenCurlyBrace: Boolean = true,
        var spaceBetweenEmptyMethodBraces: Boolean = true,
        var spaceBetweenMethodDeclarationNameAndOpenParenthesis: Boolean = false,
        var spaceBetweenEmptyMethodCallParentheses: Boolean = false,
        var spaceBetweenParenthesizedExpressionAndOpenParenthesis: Boolean = false,
        var wrapLineLength: Int = 120
    )
    
    private var myState = State()
    
    override fun getState(): State? {
        return myState
    }
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    // 获取当前配置的格式化选项
    fun getFormattingOptions(): FormatRequest.Options {
        return FormatRequest.Options(
            indentSize = myState.indentSize,
            tabSize = myState.tabSize,
            useTabs = myState.useTabs,
            newlineForBraces = myState.newlineForBraces,
            spaceAfterCast = myState.spaceAfterCast,
            spaceAfterKeywordInControlFlowStatements = myState.spaceAfterKeywordInControlFlowStatements,
            spaceBeforeOpenSquareBracket = myState.spaceBeforeOpenSquareBracket,
            spaceBetweenEmptySquareBrackets = myState.spaceBetweenEmptySquareBrackets,
            spaceAfterColonInBaseTypeDeclaration = myState.spaceAfterColonInBaseTypeDeclaration,
            spaceAfterComma = myState.spaceAfterComma,
            spaceAfterDot = myState.spaceAfterDot,
            spaceAfterSemicolonsInForStatements = myState.spaceAfterSemicolonsInForStatements,
            spaceBeforeColonInBaseTypeDeclaration = myState.spaceBeforeColonInBaseTypeDeclaration,
            spaceBeforeComma = myState.spaceBeforeComma,
            spaceBeforeDot = myState.spaceBeforeDot,
            spaceBeforeSemicolonsInForStatements = myState.spaceBeforeSemicolonsInForStatements,
            spaceBeforeOpenCurlyBrace = myState.spaceBeforeOpenCurlyBrace,
            spaceBetweenEmptyMethodBraces = myState.spaceBetweenEmptyMethodBraces,
            spaceBetweenMethodDeclarationNameAndOpenParenthesis = myState.spaceBetweenMethodDeclarationNameAndOpenParenthesis,
            spaceBetweenEmptyMethodCallParentheses = myState.spaceBetweenEmptyMethodCallParentheses,
            spaceBetweenParenthesizedExpressionAndOpenParenthesis = myState.spaceBetweenParenthesizedExpressionAndOpenParenthesis,
            wrapLineLength = myState.wrapLineLength
        )
    }
    
    // 设置配置
    fun setState(state: State) {
        myState = state
    }
    
    companion object {
        fun getInstance(project: Project): FormattingConfig {
            return ServiceManager.getService(project, FormattingConfig::class.java)
        }
    }
}
```

### 3.6 FormattingConfigurable

```kotlin
package com.intellij.csharp.omnisharp.formatting.config

import com.intellij.csharp.omnisharp.OmniSharpBundle
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JCheckBox
import javax.swing.JSpinner
import javax.swing.SpinnerNumberModel
import javax.inject.Inject
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.layout.panel

/**
 * 格式化配置UI
 */
class FormattingConfigurable @Inject constructor(
    private val project: Project
) : Configurable {
    private val config = FormattingConfig.getInstance(project)
    private lateinit var panel: JPanel
    private lateinit var indentSizeSpinner: JSpinner
    private lateinit var tabSizeSpinner: JSpinner
    private lateinit var useTabsCheckBox: JCheckBox
    private lateinit var newlineForBracesCheckBox: JCheckBox
    private lateinit var spaceAfterCastCheckBox: JCheckBox
    private lateinit var wrapLineLengthSpinner: JSpinner
    
    override fun getDisplayName(): String {
        return OmniSharpBundle.message("configurable.formatting.name")
    }
    
    override fun createComponent(): JComponent {
        panel = panel {
            titledRow(OmniSharpBundle.message("configurable.formatting.indentation")) {
                row {
                    label(OmniSharpBundle.message("configurable.formatting.indent.size"))
                    indentSizeSpinner = spinner(0..20, 4, 1)
                    label(OmniSharpBundle.message("configurable.formatting.tab.size"))
                    tabSizeSpinner = spinner(0..20, 4, 1)
                }
                row {
                    useTabsCheckBox = checkBox(OmniSharpBundle.message("configurable.formatting.use.tabs")).component
                }
            }
            
            titledRow(OmniSharpBundle.message("configurable.formatting.spacing")) {
                row {
                    newlineForBracesCheckBox = checkBox(OmniSharpBundle.message("configurable.formatting.newline.for.braces")).component
                }
                row {
                    spaceAfterCastCheckBox = checkBox(OmniSharpBundle.message("configurable.formatting.space.after.cast")).component
                }
            }
            
            titledRow(OmniSharpBundle.message("configurable.formatting.wrapping")) {
                row {
                    label(OmniSharpBundle.message("configurable.formatting.wrap.line.length"))
                    wrapLineLengthSpinner = spinner(40..500, 120, 1)
                }
            }
        }
        
        return panel
    }
    
    override fun isModified(): Boolean {
        val state = config.state ?: return false
        return state.indentSize != indentSizeSpinner.value ||
               state.tabSize != tabSizeSpinner.value ||
               state.useTabs != useTabsCheckBox.isSelected ||
               state.newlineForBraces != newlineForBracesCheckBox.isSelected ||
               state.spaceAfterCast != spaceAfterCastCheckBox.isSelected ||
               state.wrapLineLength != wrapLineLengthSpinner.value
    }
    
    override fun apply() {
        val state = config.state ?: FormattingConfig.State()
        
        state.indentSize = indentSizeSpinner.value as Int
        state.tabSize = tabSizeSpinner.value as Int
        state.useTabs = useTabsCheckBox.isSelected
        state.newlineForBraces = newlineForBracesCheckBox.isSelected
        state.spaceAfterCast = spaceAfterCastCheckBox.isSelected
        state.wrapLineLength = wrapLineLengthSpinner.value as Int
        
        config.loadState(state)
    }
    
    override fun reset() {
        val state = config.state ?: FormattingConfig.State()
        
        indentSizeSpinner.value = state.indentSize
        tabSizeSpinner.value = state.tabSize
        useTabsCheckBox.isSelected = state.useTabs
        newlineForBracesCheckBox.isSelected = state.newlineForBraces
        spaceAfterCastCheckBox.isSelected = state.spaceAfterCast
        wrapLineLengthSpinner.value = state.wrapLineLength
    }
}
```

### 3.7 FormattingPreviewManager

```kotlin
package com.intellij.csharp.omnisharp.formatting.preview

import com.intellij.csharp.omnisharp.formatting.FormattingCache
import com.intellij.csharp.omnisharp.services.OmniSharpFormattingService
import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.contents.DiffContent
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.util.concurrent.CompletableFuture

/**
 * 格式化预览管理器
 * 提供格式化前后代码对比预览功能
 */
class FormattingPreviewManager(private val formattingService: OmniSharpFormattingService) {
    private val logger = Logger.getInstance(FormattingPreviewManager::class.java)
    
    /**
     * 预览整个文件的格式化结果
     */
    fun previewFormatFile(project: Project, file: VirtualFile, originalContent: String): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        
        formattingService.formatFile(project, file, originalContent)
            .thenAccept { formattedContent ->
                if (formattedContent != originalContent) {
                    showDiffPreview(project, file.name, originalContent, formattedContent)
                    future.complete(true)
                } else {
                    future.complete(false)
                }
            }
            .exceptionally { ex ->
                logger.error("Error previewing file formatting", ex)
                future.complete(false)
                null
            }
        
        return future
    }
    
    /**
     * 预览选中区域的格式化结果
     */
    fun previewFormatSelection(
        project: Project, 
        file: VirtualFile, 
        originalContent: String, 
        selectionStart: Int, 
        selectionEnd: Int
    ): CompletableFuture<Boolean> {
        val future = CompletableFuture<Boolean>()
        
        formattingService.formatSelection(project, file, originalContent, selectionStart, selectionEnd)
            .thenAccept { formattedSelection ->
                val originalSelection = originalContent.substring(selectionStart, selectionEnd)
                if (formattedSelection != originalSelection) {
                    showDiffPreview(project, "Selection in ${file.name}", originalSelection, formattedSelection)
                    future.complete(true)
                } else {
                    future.complete(false)
                }
            }
            .exceptionally { ex ->
                logger.error("Error previewing selection formatting", ex)
                future.complete(false)
                null
            }
        
        return future
    }
    
    /**
     * 显示差异预览
     */
    private fun showDiffPreview(project: Project, title: String, beforeContent: String, afterContent: String) {
        try {
            val contentFactory = DiffContentFactory.getInstance()
            val before: DiffContent = contentFactory.create(beforeContent, null, "Original")
            val after: DiffContent = contentFactory.create(afterContent, null, "Formatted")
            
            val request = SimpleDiffRequest(title, before, after, "Original", "Formatted")
            DiffManager.getInstance().showDiff(project, request)
        } catch (e: Exception) {
            logger.error("Failed to show diff preview", e)
        }
    }
}
```

## 4. 请求与响应模型

### 4.1 FormatRequest

```kotlin
package com.intellij.csharp.omnisharp.protocol.requests

import com.google.gson.annotations.SerializedName
import java.util.*

/**
 * 格式化请求模型
 */
data class FormatRequest(
    @SerializedName("FileName")
    val fileName: String,
    
    @SerializedName("Text")
    val text: String,
    
    @SerializedName("Range")
    val range: Range? = null,
    
    @SerializedName("WantSpans")
    val wantSpans: Boolean = false,
    
    @SerializedName("Options")
    val options: Options? = null
) {
    /**
     * 位置范围
     */
    data class Range(
        @SerializedName("Start")
        val start: Position,
        
        @SerializedName("End")
        val end: Position
    )
    
    /**
     * 位置坐标
     */
    data class Position(
        @SerializedName("Line")
        val line: Int,
        
        @SerializedName("Character")
        val character: Int
    )
    
    /**
     * 格式化选项
     */
    data class Options(
        @SerializedName("IndentSize")
        val indentSize: Int = 4,
        
        @SerializedName("TabSize")
        val tabSize: Int = 4,
        
        @SerializedName("UseTabs")
        val useTabs: Boolean = false,
        
        @SerializedName("NewlineForBraces")
        val newlineForBraces: Boolean = true,
        
        @SerializedName("SpaceAfterCast")
        val spaceAfterCast: Boolean = true,
        
        @SerializedName("SpaceAfterKeywordInControlFlowStatements")
        val spaceAfterKeywordInControlFlowStatements: Boolean = true,
        
        @SerializedName("SpaceBeforeOpenSquareBracket")
        val spaceBeforeOpenSquareBracket: Boolean = false,
        
        @SerializedName("SpaceBetweenEmptySquareBrackets")
        val spaceBetweenEmptySquareBrackets: Boolean = false,
        
        @SerializedName("SpaceAfterColonInBaseTypeDeclaration")
        val spaceAfterColonInBaseTypeDeclaration: Boolean = true,
        
        @SerializedName("SpaceAfterComma")
        val spaceAfterComma: Boolean = true,
        
        @SerializedName("SpaceAfterDot")
        val spaceAfterDot: Boolean = false,
        
        @SerializedName("SpaceAfterSemicolonsInForStatements")
        val spaceAfterSemicolonsInForStatements: Boolean = true,
        
        @SerializedName("SpaceBeforeColonInBaseTypeDeclaration")
        val spaceBeforeColonInBaseTypeDeclaration: Boolean = false,
        
        @SerializedName("SpaceBeforeComma")
        val spaceBeforeComma: Boolean = false,
        
        @SerializedName("SpaceBeforeDot")
        val spaceBeforeDot: Boolean = false,
        
        @SerializedName("SpaceBeforeSemicolonsInForStatements")
        val spaceBeforeSemicolonsInForStatements: Boolean = false,
        
        @SerializedName("SpaceBeforeOpenCurlyBrace")
        val spaceBeforeOpenCurlyBrace: Boolean = true,
        
        @SerializedName("SpaceBetweenEmptyMethodBraces")
        val spaceBetweenEmptyMethodBraces: Boolean = true,
        
        @SerializedName("SpaceBetweenMethodDeclarationNameAndOpenParenthesis")
        val spaceBetweenMethodDeclarationNameAndOpenParenthesis: Boolean = false,
        
        @SerializedName("SpaceBetweenEmptyMethodCallParentheses")
        val spaceBetweenEmptyMethodCallParentheses: Boolean = false,
        
        @SerializedName("SpaceBetweenParenthesizedExpressionAndOpenParenthesis")
        val spaceBetweenParenthesizedExpressionAndOpenParenthesis: Boolean = false,
        
        @SerializedName("WrapLineLength")
        val wrapLineLength: Int = 120
    )
    
    /**
     * 转换为JSON Map
     */
    fun toJson(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>
        map["FileName"] = fileName
        map["Text"] = text
        map["WantSpans"] = wantSpans
        
        if (range != null) {
            map["Range"] = mapOf(
                "Start" to mapOf(
                    "Line" to range.start.line,
                    "Character" to range.start.character
                ),
                "End" to mapOf(
                    "Line" to range.end.line,
                    "Character" to range.end.character
                )
            )
        }
        
        if (options != null) {
            map["Options"] = mapOf(
                "IndentSize" to options.indentSize,
                "TabSize" to options.tabSize,
                "UseTabs" to options.useTabs,
                "NewlineForBraces" to options.newlineForBraces,
                "SpaceAfterCast" to options.spaceAfterCast,
                "SpaceAfterKeywordInControlFlowStatements" to options.spaceAfterKeywordInControlFlowStatements,
                "SpaceBeforeOpenSquareBracket" to options.spaceBeforeOpenSquareBracket,
                "SpaceBetweenEmptySquareBrackets" to options.spaceBetweenEmptySquareBrackets,
                "SpaceAfterColonInBaseTypeDeclaration" to options.spaceAfterColonInBaseTypeDeclaration,
                "SpaceAfterComma" to options.spaceAfterComma,
                "SpaceAfterDot" to options.spaceAfterDot,
                "SpaceAfterSemicolonsInForStatements" to options.spaceAfterSemicolonsInForStatements,
                "SpaceBeforeColonInBaseTypeDeclaration" to options.spaceBeforeColonInBaseTypeDeclaration,
                "SpaceBeforeComma" to options.spaceBeforeComma,
                "SpaceBeforeDot" to options.spaceBeforeDot,
                "SpaceBeforeSemicolonsInForStatements" to options.spaceBeforeSemicolonsInForStatements,
                "SpaceBeforeOpenCurlyBrace" to options.spaceBeforeOpenCurlyBrace,
                "SpaceBetweenEmptyMethodBraces" to options.spaceBetweenEmptyMethodBraces,
                "SpaceBetweenMethodDeclarationNameAndOpenParenthesis" to options.spaceBetweenMethodDeclarationNameAndOpenParenthesis,
                "SpaceBetweenEmptyMethodCallParentheses" to options.spaceBetweenEmptyMethodCallParentheses,
                "SpaceBetweenParenthesizedExpressionAndOpenParenthesis" to options.spaceBetweenParenthesizedExpressionAndOpenParenthesis,
                "WrapLineLength" to options.wrapLineLength
            )
        }
        
        return map
    }
}
```

### 4.2 FormatResponse

```kotlin
package com.intellij.csharp.omnisharp.protocol.responses

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * 格式化响应模型
 */
data class FormatResponse(
    val buffer: String,
    val spans: List<Span>? = null
) {
    data class Span(
        val start: Int,
        val end: Int,
        val newText: String
    )
    
    companion object {
        /**
         * 从JSON响应创建FormatResponse
         */
        fun fromJson(responseJson: JsonObject): FormatResponse {
            val buffer = responseJson.getAsJsonObject("Body").get("Buffer").asString
            
            // 检查是否有Spans字段
            val spansJson = responseJson.getAsJsonObject("Body").get("Spans")
            val spans = if (spansJson != null && !spansJson.isJsonNull) {
                spansJson.asJsonArray.map {
                    val span = it.asJsonObject
                    Span(
                        span.get("Start").asInt,
                        span.get("End").asInt,
                        span.get("NewText").asString
                    )
                }
            } else {
                null
            }
            
            return FormatResponse(buffer, spans)
        }
    }
}
```

## 5. 性能优化

### 5.1 缓存策略

实现了多级缓存机制：

1. **内存缓存**：使用 `FormattingCache` 缓存最近格式化的文件结果
2. **缓存清理**：
   - 基于时间的过期策略（默认10分钟）
   - 基于大小的限制（默认100个条目）
   - 基于访问频率的淘汰机制

### 5.2 异步处理

所有格式化操作都在后台线程执行，避免阻塞UI线程：

```kotlin
// 在后台线程执行格式化
ApplicationManager.getApplication().executeOnPooledThread {
    // 执行格式化
    val formattedContent = formattingService.formatFile(project, file, content).get()
    
    // 在UI线程应用结果
    ApplicationManager.getApplication().invokeLater {
        // 更新编辑器内容
    }
}
```

### 5.3 增量格式化

对于大文件，支持选择区域格式化，避免整个文件重新格式化：

```kotlin
// 只格式化选中的区域
if (editor.selectionModel.hasSelection()) {
    val startOffset = editor.selectionModel.selectionStart
    val endOffset = editor.selectionModel.selectionEnd
    formatter.formatSelectionAsync(file, startOffset, endOffset)
}
```

### 5.4 请求优化

实现请求去抖动，避免频繁格式化请求：

```kotlin
/**
 * 格式化请求去抖动
 */
class FormattingDebouncer(private val delayMs: Long = 500) {
    private var scheduledFuture: ScheduledFuture<*>? = null
    private val executor = Executors.newSingleThreadScheduledExecutor()
    
    fun schedule(action: () -> Unit) {
        // 取消之前的任务
        scheduledFuture?.cancel(false)
        
        // 安排新任务
        scheduledFuture = executor.schedule(action, delayMs, TimeUnit.MILLISECONDS)
    }
    
    fun shutdown() {
        executor.shutdown()
    }
}
```

## 6. 错误处理

### 6.1 全局错误处理

```kotlin
/**
 * 格式化错误处理器
 */
class FormattingErrorHandler {
    companion object {
        fun handleError(project: Project, error: Throwable, operation: String) {
            val message = when (error) {
                is TimeoutException -> "格式化超时，请检查OmniSharp服务器连接"
                is IllegalStateException -> "OmniSharp会话不可用"
                is IOException -> "网络或IO错误: ${error.message}"
                else -> "格式化过程中发生错误: ${error.message}"
            }
            
            // 记录错误日志
            Logger.getInstance(FormattingErrorHandler::class.java)
                .error("Error during $operation: ${error.message}", error)
            
            // 显示用户友好的错误消息
            ApplicationManager.getApplication().invokeLater {
                Messages.showErrorDialog(
                    project,
                    message,
                    "格式化错误"
                )
            }
        }
    }
}
```

### 6.2 重试机制

```kotlin
/**
 * 带重试的格式化操作
 */
fun formatWithRetry(
    operation: () -> CompletableFuture<String>,
    maxRetries: Int = 3,
    delayMs: Long = 1000
): CompletableFuture<String> {
    return retryOperation(operation, maxRetries, delayMs)
}

/**
 * 通用重试操作
 */
private fun <T> retryOperation(
    operation: () -> CompletableFuture<T>,
    maxRetries: Int,
    delayMs: Long
): CompletableFuture<T> {
    val result = CompletableFuture<T>()
    
    fun attempt(retryCount: Int) {
        operation()
            .thenAccept(result::complete)
            .exceptionally { ex ->
                if (retryCount < maxRetries) {
                    // 延迟后重试
                    Thread.sleep(delayMs)
                    attempt(retryCount + 1)
                } else {
                    result.completeExceptionally(ex)
                }
                null
            }
    }
    
    attempt(0)
    return result
}
```

## 7. 配置选项

### 7.1 全局配置

```kotlin
// 在插件配置文件中添加格式化相关选项

package com.intellij.csharp.omnisharp

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "OmniSharpSettings",
    storages = [Storage("omnisharp_settings.xml")]
)
class OmniSharpSettings : PersistentStateComponent<OmniSharpSettings.State> {
    
    data class State(
        // 其他配置...
        
        // 格式化配置
        var formattingEnabled: Boolean = true,
        var formatOnSave: Boolean = false,
        var formatOnPaste: Boolean = false,
        var formatAfterTypingSemicolon: Boolean = false,
        var formatAfterTypingCurlyBrace: Boolean = false,
        var maxLineLength: Int = 120,
        var cacheSize: Int = 100,
        var cacheTtlMinutes: Int = 10
    )
    
    private var myState = State()
    
    override fun getState(): State? {
        return myState
    }
    
    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }
    
    companion object {
        fun getInstance(): OmniSharpSettings {
            return ServiceManager.getService(OmniSharpSettings::class.java)
        }
    }
}
```

### 7.2 项目级配置

支持在项目 `.editorconfig` 文件中配置格式化规则，与 OmniSharp 服务器兼容的格式：

```kotlin
/**
 * 读取 .editorconfig 文件中的格式化配置
 */
class EditorConfigReader {
    fun readFormattingOptions(project: Project, file: VirtualFile): FormatRequest.Options {
        val editorConfig = findEditorConfig(project, file)
        if (editorConfig != null) {
            try {
                val properties = Properties()
                properties.load(FileInputStream(editorConfig.path))
                
                return FormatRequest.Options(
                    indentSize = properties.getProperty("indent_size")?.toInt() ?: 4,
                    tabSize = properties.getProperty("tab_width")?.toInt() ?: 4,
                    useTabs = properties.getProperty("indent_style")?.equals("tab", ignoreCase = true) ?: false,
                    newlineForBraces = properties.getProperty("csharp_new_line_before_open_brace")?.equals("all", ignoreCase = true) ?: true,
                    spaceAfterCast = properties.getProperty("csharp_space_after_cast")?.equals("true", ignoreCase = true) ?: true,
                    // 其他选项...
                )
            } catch (e: Exception) {
                Logger.getInstance(EditorConfigReader::class.java).warn("Failed to read .editorconfig", e)
            }
        }
        
        // 返回默认配置
        return FormatRequest.Options()
    }
    
    private fun findEditorConfig(project: Project, file: VirtualFile): VirtualFile? {
        // 从文件目录开始向上查找 .editorconfig 文件
        var currentDir = file.parent
        while (currentDir != null && currentDir != project.baseDir) {
            val editorConfig = currentDir.findChild(".editorconfig")
            if (editorConfig != null && !editorConfig.isDirectory) {
                return editorConfig
            }
            currentDir = currentDir.parent
        }
        
        // 检查项目根目录
        return project.baseDir?.findChild(".editorconfig")
    }
}
```

## 8. 测试策略

### 8.1 单元测试

为核心组件编写单元测试：

```kotlin
class FormattingServiceTest {
    @Mock
    private lateinit var sessionManager: SessionManager
    
    @Mock
    private lateinit var session: OmniSharpSession
    
    @Mock
    private lateinit var communicator: OmniSharpCommunicator
    
    private lateinit var formattingService: OmniSharpFormattingService
    
    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        `when`(sessionManager.getSession(any())).thenReturn(session)
        `when`(session.communicator).thenReturn(communicator)
        `when`(session.isAlive).thenReturn(true)
        
        formattingService = OmniSharpFormattingService(sessionManager)
    }
    
    @Test
    fun testFormatFileSuccess() {
        // 准备模拟响应
        val mockResponse = JsonObject().apply {
            add("Body", JsonObject().apply {
                addProperty("Buffer", "formatted content")
            })
        }
        
        `when`(communicator.sendRequest("v2/format/file", anyMap())).thenReturn(
            CompletableFuture.completedFuture(mockResponse)
        )
        
        // 创建测试文件
        val file = mock<VirtualFile>()
        `when`(file.path).thenReturn("test.cs")
        
        // 执行测试
        val project = mock<Project>()
        val result = formattingService.formatFile(project, file, "original content").get()
        
        // 验证结果
        assertEquals("formatted content", result)
    }
    
    @Test
    fun testFormatFileCache() {
        // 设置模拟响应
        val mockResponse = JsonObject().apply {
            add("Body", JsonObject().apply {
                addProperty("Buffer", "formatted content")
            })
        }
        
        `when`(communicator.sendRequest("v2/format/file", anyMap())).thenReturn(
            CompletableFuture.completedFuture(mockResponse)
        )
        
        val file = mock<VirtualFile>()
        `when`(file.path).thenReturn("test.cs")
        
        val project = mock<Project>()
        
        // 第一次调用（应该发送请求）
        val result1 = formattingService.formatFile(project, file, "original content").get()
        
        // 第二次调用（应该使用缓存）
        val result2 = formattingService.formatFile(project, file, "original content").get()
        
        // 验证两次结果相同
        assertEquals(result1, result2)
        
        // 验证只发送了一次请求
        verify(communicator, times(1)).sendRequest("v2/format/file", anyMap())
    }
    
    @Test
    fun testFormatSelectionSuccess() {
        // 准备模拟响应
        val mockResponse = JsonObject().apply {
            add("Body", JsonObject().apply {
                addProperty("Buffer", "formatted selection")
            })
        }
        
        `when`(communicator.sendRequest("v2/format/selection", anyMap())).thenReturn(
            CompletableFuture.completedFuture(mockResponse)
        )
        
        val file = mock<VirtualFile>()
        `when`(file.path).thenReturn("test.cs")
        
        val project = mock<Project>()
        val result = formattingService.formatSelection(project, file, "original content", 0, 10).get()
        
        assertEquals("formatted selection", result)
    }
    
    @Test
    fun testFormatFileSessionNotAvailable() {
        `when`(sessionManager.getSession(any())).thenReturn(null)
        
        val file = mock<VirtualFile>()
        `when`(file.path).thenReturn("test.cs")
        
        val project = mock<Project>()
        
        val future = formattingService.formatFile(project, file, "original content")
        
        try {
            future.get()
            fail("Should throw exception")
        } catch (e: ExecutionException) {
            assertTrue(e.cause is IllegalStateException)
        }
    }
}
```

### 8.2 集成测试

为格式化功能与其他组件的集成编写测试：

```kotlin
class FormattingIntegrationTest {
    private lateinit var project: Project
    private lateinit var formattingService: OmniSharpFormattingService
    private lateinit var formatter: OmniSharpFormatter
    
    @Before
    fun setUp() {
        // 创建测试项目
        project = ProjectManager.getInstance().defaultProject
        
        // 获取服务实例
        formattingService = ServiceManager.getService(FormattingService::class.java)
        formatter = OmniSharpFormatter(formattingService)
    }
    
    @Test
    fun testFormattingWithRealFile() {
        // 创建临时C#文件
        val file = createTempFile("test", ".cs")
        file.writeText("class Test{int x;void Method(){Console.WriteLine(x);}}")
        
        // 将文件添加到项目
        val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)
        assertNotNull(virtualFile)
        
        // 获取PSI文件
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile!!)
        assertNotNull(psiFile)
        
        // 格式化文件
        val future = formatter.formatAsync(psiFile!!)
        val success = future.get(20000, TimeUnit.MILLISECONDS)  // 20秒超时
        
        // 验证格式化成功
        assertTrue(success)
        
        // 验证文件内容已改变（缩进、空格等）
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        assertNotNull(document)
        val formattedContent = document!!.text
        
        // 检查是否添加了适当的空格
        assertTrue(formattedContent.contains("class Test {"))
        assertTrue(formattedContent.contains("int x;"))
        assertTrue(formattedContent.contains("void Method() {"))
    }
    
    @After
    fun tearDown() {
        // 清理资源
    }
}
```

## 9. 使用示例

### 9.1 基本使用

以下是代码格式化功能的基本使用示例：

```kotlin
// 获取格式化服务
val formattingService = ServiceManager.getService(OmniSharpFormattingService::class.java)

// 获取当前文件和编辑器
val editor = FileEditorManager.getInstance(project).selectedTextEditor
val file = editor?.virtualFile

if (file != null && editor != null) {
    // 检查是否有选中区域
    if (editor.selectionModel.hasSelection()) {
        // 格式化选中区域
        val startOffset = editor.selectionModel.selectionStart
        val endOffset = editor.selectionModel.selectionEnd
        val document = editor.document
        val fileContent = document.text
        
        formattingService.formatSelection(project, file, fileContent, startOffset, endOffset)
            .thenAccept { formattedContent ->
                // 在UI线程中应用更改
                ApplicationManager.getApplication().invokeLater {
                    document.replaceString(startOffset, endOffset, formattedContent)
                }
            }
            .exceptionally { ex ->
                // 处理错误
                println("Error formatting selection: ${ex.message}")
                null
            }
    } else {
        // 格式化整个文件
        val document = editor.document
        val fileContent = document.text
        
        formattingService.formatFile(project, file, fileContent)
            .thenAccept { formattedContent ->
                // 在UI线程中应用更改
                ApplicationManager.getApplication().invokeLater {
                    document.replaceString(0, document.textLength, formattedContent)
                }
            }
            .exceptionally { ex ->
                // 处理错误
                println("Error formatting file: ${ex.message}")
                null
            }
    }
}
```

### 9.2 保存时格式化

以下是实现保存时自动格式化的示例：

```kotlin
class FormatOnSaveHandler {
    companion object {
        fun register(project: Project) {
            // 注册文档保存前监听器
            FileDocumentManager.getInstance().addDocumentListener(object : FileDocumentManagerAdapter() {
                override fun beforeDocumentSaving(document: Document) {
                    val file = FileDocumentManager.getInstance().getFile(document) ?: return
                    
                    // 检查是否是C#文件
                    if (!file.fileType.name.equals("C#", ignoreCase = true)) return
                    
                    // 检查是否启用了保存时格式化
                    val settings = OmniSharpSettings.getInstance()
                    if (!settings.state.formatOnSave) return
                    
                    // 格式化文件
                    val formattingService = ServiceManager.getService(OmniSharpFormattingService::class.java)
                    val psiFile = PsiManager.getInstance(project).findFile(file) ?: return
                    
                    try {
                        // 同步执行格式化
                        val formattedContent = formattingService.formatFile(project, file, document.text).get(5000, TimeUnit.MILLISECONDS)
                        
                        // 应用格式化结果
                        if (formattedContent != document.text) {
                            document.replaceString(0, document.textLength, formattedContent)
                        }
                    } catch (e: Exception) {
                        Logger.getInstance(FormatOnSaveHandler::class.java)
                            .warn("Failed to format file on save: ${file.path}", e)
                    }
                }
            }, project)
        }
    }
}
```

### 9.3 实时格式化

以下是实现输入时自动格式化的示例：

```kotlin
class FormatOnTypeHandler : TypedActionHandlerDelegate {
    private val debouncer = FormattingDebouncer(500) // 500ms 延迟
    
    override fun beforeCharTyped(c: Char, project: Project, editor: Editor, file: PsiFile, fileType: FileType): Result {
        // 只处理C#文件
        if (!fileType.name.equals("C#", ignoreCase = true)) {
            return Result.CONTINUE
        }
        
        // 只在输入分号或大括号后触发
        if (c == ';' || c == '}' || c == '{') {
            // 获取格式化服务
            val formattingService = ServiceManager.getService(OmniSharpFormattingService::class.java)
            val formatter = OmniSharpFormatter(formattingService)
            val settings = OmniSharpSettings.getInstance()
            
            // 检查是否启用了相应的自动格式化选项
            val shouldFormat = when (c) {
                ';' -> settings.state.formatAfterTypingSemicolon
                '}' -> settings.state.formatAfterTypingCurlyBrace
                '{' -> settings.state.formatAfterTypingCurlyBrace
                else -> false
            }
            
            if (shouldFormat) {
                // 使用去抖动，避免频繁格式化
                debouncer.schedule {
                    try {
                        // 获取当前行范围
                        val caretOffset = editor.caretModel.offset
                        val lineNumber = editor.document.getLineNumber(caretOffset)
                        val lineStartOffset = editor.document.getLineStartOffset(lineNumber)
                        val lineEndOffset = editor.document.getLineEndOffset(lineNumber)
                        
                        // 格式化当前行
                        formatter.formatSelectionAsync(file, lineStartOffset, lineEndOffset)
                    } catch (e: Exception) {
                        Logger.getInstance(FormatOnTypeHandler::class.java)
                            .warn("Failed to format on type", e)
                    }
                }
            }
        }
        
        return Result.CONTINUE
    }
    
    override fun charTyped(c: Char, project: Project, editor: Editor, file: PsiFile): Result {
        return Result.CONTINUE
    }
}
```

## 10. 总结

本实现方案详细描述了OmniSharp代码格式化功能在IntelliJ平台中的集成实现。通过该方案，开发者可以在编辑C#代码时获得强大的格式化功能，保持代码风格一致性和可读性。

主要特点包括：

1. **多种格式化模式**：支持格式化整个文件或选中区域
2. **丰富的配置选项**：支持多种格式化风格配置
3. **性能优化**：通过缓存、异步处理等技术提高性能
4. **智能格式化**：支持保存时格式化、输入时格式化等智能功能
5. **用户友好**：提供格式化预览、错误提示等用户友好功能
6. **与IntelliJ平台集成**：无缝集成到IntelliJ的格式化系统中

本方案不仅提供了功能实现，还包含了性能优化考虑和详细的测试策略，可以确保代码格式化功能的稳定运行和良好性能。通过这种方式，开发者可以在IntelliJ平台上获得与Visual Studio类似的C#代码格式化体验，提高开发效率和代码质量。

## 11. 后续优化方向

1. **支持更多格式化规则**：扩展配置选项，支持更多格式化规则
2. **智能格式化增强**：提供更智能的格式化建议和自动应用
3. **格式化模板**：支持保存和加载自定义格式化模板
4. **批量格式化**：支持批量格式化多个文件或整个项目
5. **格式化对比**：提供不同格式化风格的对比预览
6. **与版本控制系统集成**：在提交前自动格式化修改的文件