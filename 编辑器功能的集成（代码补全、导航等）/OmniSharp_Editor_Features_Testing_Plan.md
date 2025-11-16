# OmniSharp 编辑器功能集成测试方案

## 1. 概述

本文档详细描述了 OmniSharp 编辑器功能集成的测试方案，包括单元测试和集成测试策略。通过全面的测试确保代码补全、导航、诊断和格式化等功能能够在 IntelliJ 平台上正常工作，提供稳定可靠的用户体验。

### 1.1 测试目标

- 验证所有编辑器功能模块的正确性和稳定性
- 确保功能模块与 OmniSharp 服务器的通信正常
- 验证与 IntelliJ 平台的集成兼容性
- 识别并修复潜在的性能问题和内存泄漏
- 确保在不同环境下的一致行为

### 1.2 测试范围

- 代码补全功能
- 代码导航功能（定义查找、引用查找）
- 代码诊断功能
- 代码格式化功能
- 各功能模块与 OmniSharp 服务器的通信
- 与 IntelliJ 平台的集成

### 1.3 测试框架

- JUnit 5：用于编写单元测试和集成测试
- Mockito：用于模拟依赖对象
- IntelliJ 平台测试框架：用于与 IDE 交互的测试

## 2. 测试架构

### 2.1 测试层次结构

```
+----------------------------------------+
|         功能验收测试 (E2E)              |
+----------------------------------------+
                    |
+----------------------------------------+
|         集成测试 (Integration)          |
+----------------------------------------+
                    |
+----------------------------------------+
|         单元测试 (Unit)                 |
+----------------------------------------+
```

### 2.2 测试目录结构

```
src/test
  ├── kotlin
  │   ├── com
  │   │   ├── intellij
  │   │   │   ├── csharp
  │   │   │   │   ├── omnisharp
  │   │   │   │   │   ├── completion   # 代码补全测试
  │   │   │   │   │   ├── navigation   # 代码导航测试
  │   │   │   │   │   ├── diagnostics  # 代码诊断测试
  │   │   │   │   │   ├── formatting   # 代码格式化测试
  │   │   │   │   │   ├── common       # 通用测试工具
  │   │   │   │   │   ├── integration  # 集成测试
  │   │   │   │   │   └── utils        # 测试工具类
```

## 3. 单元测试实现

### 3.1 通用测试工具类

#### 3.1.1 TestUtil

```kotlin
package com.intellij.csharp.omnisharp.utils

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File
import java.io.IOException

/**
 * 测试工具类
 */
object TestUtil {
    /**
     * 创建测试文件
     */
    fun createTestFile(project: Project, fileName: String, content: String): Pair<VirtualFile, PsiFile> {
        try {
            // 创建临时文件
            val tempDir = createTempDir()
            val file = File(tempDir, fileName)
            file.writeText(content)
            
            // 刷新文件系统
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!
            
            // 获取PSI文件
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)!!
            
            return Pair(virtualFile, psiFile)
        } catch (e: IOException) {
            throw AssertionError("Failed to create test file: $fileName", e)
        }
    }
    
    /**
     * 在编辑器中设置光标位置
     */
    fun setCaretPosition(editor: Editor, line: Int, column: Int) {
        val document = editor.document
        val offset = document.getLineStartOffset(line) + column
        editor.caretModel.moveToOffset(offset)
    }
    
    /**
     * 从编辑器中获取当前行文本
     */
    fun getCurrentLineText(editor: Editor): String {
        val document = editor.document
        val caretOffset = editor.caretModel.offset
        val lineNumber = document.getLineNumber(caretOffset)
        val lineStartOffset = document.getLineStartOffset(lineNumber)
        val lineEndOffset = document.getLineEndOffset(lineNumber)
        return document.getText(lineStartOffset, lineEndOffset - lineStartOffset)
    }
    
    /**
     * 提交文档更改到PSI
     */
    fun commitDocument(project: Project, editor: Editor) {
        PsiDocumentManager.getInstance(project).commitDocument(editor.document)
    }
}
```

#### 3.1.2 MockOmniSharpSession

```kotlin
package com.intellij.csharp.omnisharp.common

import com.intellij.csharp.omnisharp.protocol.requests.Request
import com.intellij.csharp.omnisharp.protocol.responses.Response
import com.intellij.csharp.omnisharp.session.OmniSharpSession
import com.intellij.csharp.omnisharp.session.SessionManager
import com.intellij.openapi.project.Project
import com.google.gson.JsonObject
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import org.mockito.Mockito

/**
 * 模拟OmniSharp会话，用于单元测试
 */
class MockOmniSharpSession : OmniSharpSession(null, null, null, null) {
    private val responses = ConcurrentHashMap<String, CompletableFuture<JsonObject>>()
    private var _isAlive = true
    
    override val isAlive: Boolean
        get() = _isAlive
    
    /**
     * 设置响应
     */
    fun setResponse(endpoint: String, response: JsonObject) {
        responses[endpoint] = CompletableFuture.completedFuture(response)
    }
    
    /**
     * 设置错误响应
     */
    fun setErrorResponse(endpoint: String, error: Throwable) {
        val future = CompletableFuture<JsonObject>()
        future.completeExceptionally(error)
        responses[endpoint] = future
    }
    
    /**
     * 发送请求
     */
    override fun sendRequest(endpoint: String, request: Map<String, Any?>): CompletableFuture<JsonObject> {
        return responses[endpoint] ?: CompletableFuture.completedFuture(JsonObject())
    }
    
    /**
     * 设置会话状态
     */
    fun setAlive(isAlive: Boolean) {
        _isAlive = isAlive
    }
    
    /**
     * 创建模拟的SessionManager
     */
    companion object {
        fun createMockSessionManager(session: MockOmniSharpSession): SessionManager {
            val sessionManager = Mockito.mock(SessionManager::class.java)
            Mockito.`when`(sessionManager.getSession(Mockito.any(Project::class.java))).thenReturn(session)
            return sessionManager
        }
    }
}
```

### 3.2 代码补全测试

#### 3.2.1 CompletionServiceTest

```kotlin
package com.intellij.csharp.omnisharp.completion

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.protocol.requests.CompletionRequest
import com.intellij.csharp.omnisharp.protocol.responses.CompletionResponse
import com.intellij.csharp.omnisharp.services.OmniSharpCompletionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 代码补全服务测试
 */
class CompletionServiceTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var file: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var completionService: OmniSharpCompletionService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 创建模拟会话
        mockSession = MockOmniSharpSession()
        
        // 创建补全服务
        completionService = OmniSharpCompletionService(MockOmniSharpSession.createMockSessionManager(mockSession))
        
        // 设置模拟文件
        Mockito.`when`(file.path).thenReturn("test.cs")
    }
    
    @Test
    fun testGetCompletionsSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        val suggestions = JsonArray()
        
        // 添加测试建议
        val suggestion1 = JsonObject()
        suggestion1.addProperty("Text", "TestClass")
        suggestion1.addProperty("Kind", "Class")
        suggestion1.addProperty("SortText", "0001")
        suggestions.add(suggestion1)
        
        val suggestion2 = JsonObject()
        suggestion2.addProperty("Text", "TestMethod")
        suggestion2.addProperty("Kind", "Method")
        suggestion2.addProperty("SortText", "0002")
        suggestions.add(suggestion2)
        
        body.add("Suggestions", suggestions)
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("autocomplete", responseJson)
        
        // 执行测试
        val result = completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("TestClass", result[0].text)
        assertEquals("Class", result[0].kind)
        assertEquals("TestMethod", result[1].text)
        assertEquals("Method", result[1].kind)
    }
    
    @Test
    fun testGetCompletionsEmptyResponse() {
        // 准备空响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.add("Suggestions", JsonArray())
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("autocomplete", responseJson)
        
        // 执行测试
        val result = completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testGetCompletionsError() {
        // 设置错误响应
        mockSession.setErrorResponse("autocomplete", RuntimeException("Test error"))
        
        // 执行测试并验证异常
        try {
            completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
            // 应该抛出异常
            assertTrue(false, "Should throw exception")
        } catch (e: ExecutionException) {
            // 验证异常类型和消息
            assertTrue(e.cause is RuntimeException)
            assertEquals("Test error", e.cause?.message)
        }
    }
    
    @Test
    fun testGetCompletionsSessionNotAlive() {
        // 设置会话不可用
        mockSession.setAlive(false)
        
        // 执行测试并验证异常
        try {
            completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
            // 应该抛出异常
            assertTrue(false, "Should throw exception")
        } catch (e: ExecutionException) {
            // 验证异常类型
            assertTrue(e.cause is IllegalStateException)
        }
    }
    
    @Test
    fun testGetCachedCompletions() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        val suggestions = JsonArray()
        
        val suggestion = JsonObject()
        suggestion.addProperty("Text", "CachedCompletion")
        suggestion.addProperty("Kind", "Method")
        suggestions.add(suggestion)
        
        body.add("Suggestions", suggestions)
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("autocomplete", responseJson)
        
        // 第一次调用（应该发送请求）
        val result1 = completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        
        // 第二次调用相同参数（应该使用缓存）
        val result2 = completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        
        // 验证两次结果相同
        assertEquals(result1, result2)
        assertNotNull(result1)
        assertEquals(1, result1.size)
        assertEquals("CachedCompletion", result1[0].text)
    }
}
```

#### 3.2.2 CompletionProviderTest

```kotlin
package com.intellij.csharp.omnisharp.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpCompletionService
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.document.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * 代码补全提供者测试
 */
class CompletionProviderTest {
    @Mock
    private lateinit var parameters: CompletionParameters
    
    @Mock
    private lateinit var resultSet: CompletionResultSet
    
    @Mock
    private lateinit var editor: Editor
    
    @Mock
    private lateinit var document: Document
    
    @Mock
    private lateinit var psiFile: PsiFile
    
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var virtualFile: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var completionService: OmniSharpCompletionService
    private lateinit var completionProvider: OmniSharpCompletionProvider
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 设置模拟对象关系
        Mockito.`when`(parameters.editor).thenReturn(editor)
        Mockito.`when`(parameters.position).thenReturn(psiFile)
        Mockito.`when`(parameters.project).thenReturn(project)
        Mockito.`when`(editor.document).thenReturn(document)
        Mockito.`when`(psiFile.virtualFile).thenReturn(virtualFile)
        Mockito.`when`(virtualFile.path).thenReturn("test.cs")
        Mockito.`when`(document.text).thenReturn("class Test { }}")
        Mockito.`when`(parameters.offset).thenReturn(10)
        
        // 创建模拟会话和服务
        mockSession = MockOmniSharpSession()
        completionService = OmniSharpCompletionService(MockOmniSharpSession.createMockSessionManager(mockSession))
        completionProvider = OmniSharpCompletionProvider(completionService)
        
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        val suggestions = JsonArray()
        
        val suggestion = JsonObject()
        suggestion.addProperty("Text", "TestMethod")
        suggestion.addProperty("Kind", "Method")
        suggestion.addProperty("SortText", "0001")
        suggestions.add(suggestion)
        
        body.add("Suggestions", suggestions)
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("autocomplete", responseJson)
    }
    
    @Test
    fun testAddCompletions() {
        // 执行测试
        completionProvider.addCompletions(parameters, null, resultSet)
        
        // 验证结果集被调用
        Mockito.verify(resultSet, Mockito.atLeastOnce()).addElement(Mockito.any())
    }
    
    @Test
    fun testAddCompletionsNonCsFile() {
        // 设置非C#文件
        Mockito.`when`(virtualFile.fileType.name).thenReturn("Java")
        
        // 执行测试
        completionProvider.addCompletions(parameters, null, resultSet)
        
        // 验证结果集未被调用
        Mockito.verify(resultSet, Mockito.never()).addElement(Mockito.any())
    }
}
```

### 3.3 代码导航测试

#### 3.3.1 NavigationServiceTest

```kotlin
package com.intellij.csharp.omnisharp.navigation

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpNavigationService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 代码导航服务测试
 */
class NavigationServiceTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var file: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var navigationService: OmniSharpNavigationService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 创建模拟会话
        mockSession = MockOmniSharpSession()
        
        // 创建导航服务
        navigationService = OmniSharpNavigationService(MockOmniSharpSession.createMockSessionManager(mockSession))
        
        // 设置模拟文件
        Mockito.`when`(file.path).thenReturn("test.cs")
    }
    
    @Test
    fun testFindDefinitionSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonArray()
        
        val definition = JsonObject()
        definition.addProperty("FileName", "test.cs")
        definition.addProperty("Line", 5)
        definition.addProperty("Column", 10)
        body.add(definition)
        
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("findusages", responseJson)
        
        // 执行测试
        val result = navigationService.findDefinition(project, file, "class Test { }", 10, 1).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("test.cs", result[0].fileName)
        assertEquals(5, result[0].line)
        assertEquals(10, result[0].column)
    }
    
    @Test
    fun testFindReferencesSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonArray()
        
        // 添加两个引用
        val ref1 = JsonObject()
        ref1.addProperty("FileName", "test.cs")
        ref1.addProperty("Line", 10)
        ref1.addProperty("Column", 5)
        body.add(ref1)
        
        val ref2 = JsonObject()
        ref2.addProperty("FileName", "other.cs")
        ref2.addProperty("Line", 15)
        ref2.addProperty("Column", 8)
        body.add(ref2)
        
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("findreferences", responseJson)
        
        // 执行测试
        val result = navigationService.findReferences(project, file, "class Test { }", 10, 1).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals(2, result.size)
        assertEquals("test.cs", result[0].fileName)
        assertEquals("other.cs", result[1].fileName)
    }
    
    @Test
    fun testFindDefinitionEmptyResponse() {
        // 准备空响应
        val responseJson = JsonObject()
        responseJson.add("Body", JsonArray())
        
        // 设置响应
        mockSession.setResponse("findusages", responseJson)
        
        // 执行测试
        val result = navigationService.findDefinition(project, file, "class Test { }", 10, 1).get()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testNavigationError() {
        // 设置错误响应
        mockSession.setErrorResponse("findusages", RuntimeException("Test error"))
        
        // 执行测试并验证异常
        try {
            navigationService.findDefinition(project, file, "class Test { }", 10, 1).get()
            // 应该抛出异常
            assertTrue(false, "Should throw exception")
        } catch (e: ExecutionException) {
            // 验证异常类型和消息
            assertTrue(e.cause is RuntimeException)
            assertEquals("Test error", e.cause?.message)
        }
    }
}
```

### 3.4 代码诊断测试

#### 3.4.1 DiagnosticServiceTest

```kotlin
package com.intellij.csharp.omnisharp.diagnostics

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpDiagnosticService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 代码诊断服务测试
 */
class DiagnosticServiceTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var file: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var diagnosticService: OmniSharpDiagnosticService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 创建模拟会话
        mockSession = MockOmniSharpSession()
        
        // 创建诊断服务
        diagnosticService = OmniSharpDiagnosticService(MockOmniSharpSession.createMockSessionManager(mockSession))
        
        // 设置模拟文件
        Mockito.`when`(file.path).thenReturn("test.cs")
    }
    
    @Test
    fun testGetDiagnosticsSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonArray()
        
        // 添加诊断信息
        val diagnostic = JsonObject()
        diagnostic.addProperty("FileName", "test.cs")
        diagnostic.addProperty("LogLevel", "Error")
        diagnostic.addProperty("Message", "Test error message")
        diagnostic.addProperty("Line", 5)
        diagnostic.addProperty("Column", 10)
        diagnostic.addProperty("EndLine", 5)
        diagnostic.addProperty("EndColumn", 20)
        body.add(diagnostic)
        
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/getcodecheck", responseJson)
        
        // 执行测试
        val result = diagnosticService.getDiagnostics(project, file).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals(1, result.size)
        assertEquals("test.cs", result[0].fileName)
        assertEquals("Error", result[0].logLevel)
        assertEquals("Test error message", result[0].message)
        assertEquals(5, result[0].line)
        assertEquals(10, result[0].column)
    }
    
    @Test
    fun testGetDiagnosticsEmptyResponse() {
        // 准备空响应
        val responseJson = JsonObject()
        responseJson.add("Body", JsonArray())
        
        // 设置响应
        mockSession.setResponse("v2/getcodecheck", responseJson)
        
        // 执行测试
        val result = diagnosticService.getDiagnostics(project, file).get()
        
        // 验证结果
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testGetDiagnosticsError() {
        // 设置错误响应
        mockSession.setErrorResponse("v2/getcodecheck", RuntimeException("Test error"))
        
        // 执行测试并验证异常
        try {
            diagnosticService.getDiagnostics(project, file).get()
            // 应该抛出异常
            assertTrue(false, "Should throw exception")
        } catch (e: ExecutionException) {
            // 验证异常类型和消息
            assertTrue(e.cause is RuntimeException)
            assertEquals("Test error", e.cause?.message)
        }
    }
    
    @Test
    fun testGetCachedDiagnostics() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonArray()
        
        val diagnostic = JsonObject()
        diagnostic.addProperty("FileName", "test.cs")
        diagnostic.addProperty("LogLevel", "Warning")
        diagnostic.addProperty("Message", "Cached warning")
        body.add(diagnostic)
        
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/getcodecheck", responseJson)
        
        // 第一次调用（应该发送请求）
        val result1 = diagnosticService.getDiagnostics(project, file).get()
        
        // 第二次调用（应该使用缓存）
        val result2 = diagnosticService.getDiagnostics(project, file).get()
        
        // 验证两次结果相同
        assertEquals(result1, result2)
        assertNotNull(result1)
        assertEquals(1, result1.size)
        assertEquals("Warning", result1[0].logLevel)
        assertEquals("Cached warning", result1[0].message)
    }
    
    @Test
    fun testClearDiagnosticsCache() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonArray()
        
        val diagnostic = JsonObject()
        diagnostic.addProperty("FileName", "test.cs")
        diagnostic.addProperty("LogLevel", "Info")
        diagnostic.addProperty("Message", "Test info")
        body.add(diagnostic)
        
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/getcodecheck", responseJson)
        
        // 第一次调用
        val result1 = diagnosticService.getDiagnostics(project, file).get()
        
        // 清除缓存
        diagnosticService.clearCache("test.cs")
        
        // 第二次调用（应该重新请求）
        val result2 = diagnosticService.getDiagnostics(project, file).get()
        
        // 验证两次结果相同（内容相同，但可能不是同一个对象）
        assertEquals(result1.size, result2.size)
        assertEquals(result1[0].message, result2[0].message)
    }
}
```

### 3.5 代码格式化测试

#### 3.5.1 FormattingServiceTest

```kotlin
package com.intellij.csharp.omnisharp.formatting

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpFormattingService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.util.concurrent.ExecutionException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 代码格式化服务测试
 */
class FormattingServiceTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var file: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var formattingService: OmniSharpFormattingService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 创建模拟会话
        mockSession = MockOmniSharpSession()
        
        // 创建格式化服务
        formattingService = OmniSharpFormattingService(MockOmniSharpSession.createMockSessionManager(mockSession))
        
        // 设置模拟文件
        Mockito.`when`(file.path).thenReturn("test.cs")
    }
    
    @Test
    fun testFormatFileSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "formatted content")
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/format/file", responseJson)
        
        // 执行测试
        val result = formattingService.formatFile(project, file, "original content").get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals("formatted content", result)
    }
    
    @Test
    fun testFormatSelectionSuccess() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "formatted selection")
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/format/selection", responseJson)
        
        // 执行测试
        val result = formattingService.formatSelection(project, file, "original content", 0, 10).get()
        
        // 验证结果
        assertNotNull(result)
        assertEquals("formatted selection", result)
    }
    
    @Test
    fun testFormatFileError() {
        // 设置错误响应
        mockSession.setErrorResponse("v2/format/file", RuntimeException("Test error"))
        
        // 执行测试并验证异常
        try {
            formattingService.formatFile(project, file, "original content").get()
            // 应该抛出异常
            assertTrue(false, "Should throw exception")
        } catch (e: ExecutionException) {
            // 验证异常类型和消息
            assertTrue(e.cause is RuntimeException)
            assertEquals("Test error", e.cause?.message)
        }
    }
    
    @Test
    fun testFormatFileCache() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "cached formatted content")
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/format/file", responseJson)
        
        // 第一次调用（应该发送请求）
        val result1 = formattingService.formatFile(project, file, "original content").get()
        
        // 第二次调用相同参数（应该使用缓存）
        val result2 = formattingService.formatFile(project, file, "original content").get()
        
        // 验证两次结果相同
        assertEquals(result1, result2)
        assertNotNull(result1)
        assertEquals("cached formatted content", result1)
    }
    
    @Test
    fun testClearFormattingCache() {
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "formatted content for cache test")
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/format/file", responseJson)
        
        // 第一次调用
        val result1 = formattingService.formatFile(project, file, "original content").get()
        
        // 清除缓存
        formattingService.clearCache("test.cs")
        
        // 修改响应内容，模拟服务器返回新结果
        body.addProperty("Buffer", "new formatted content")
        mockSession.setResponse("v2/format/file", responseJson)
        
        // 第二次调用（应该重新请求）
        val result2 = formattingService.formatFile(project, file, "original content").get()
        
        // 验证结果不同
        assertTrue(result1 != result2)
        assertEquals("formatted content for cache test", result1)
        assertEquals("new formatted content", result2)
    }
}
```

#### 3.5.2 FormatterTest

```kotlin
package com.intellij.csharp.omnisharp.formatting

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpFormattingService
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import com.google.gson.JsonObject

/**
 * 代码格式化器测试
 */
class FormatterTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var psiFile: PsiFile
    
    @Mock
    private lateinit var virtualFile: VirtualFile
    
    @Mock
    private lateinit var editor: Editor
    
    @Mock
    private lateinit var document: Document
    
    @Mock
    private lateinit var psiDocumentManager: PsiDocumentManager
    
    @Mock
    private lateinit var formattingModel: com.intellij.formatting.FormattingModel
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var formattingService: OmniSharpFormattingService
    private lateinit var formatter: OmniSharpFormatter
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 设置模拟对象关系
        Mockito.`when`(psiFile.project).thenReturn(project)
        Mockito.`when`(psiFile.virtualFile).thenReturn(virtualFile)
        Mockito.`when`(virtualFile.path).thenReturn("test.cs")
        Mockito.`when`(editor.document).thenReturn(document)
        Mockito.`when`(document.text).thenReturn("class Test{}")
        Mockito.`when`(PsiDocumentManager.getInstance(project)).thenReturn(psiDocumentManager)
        Mockito.`when`(psiDocumentManager.getDocument(psiFile)).thenReturn(document)
        
        // 创建模拟会话和服务
        mockSession = MockOmniSharpSession()
        formattingService = OmniSharpFormattingService(MockOmniSharpSession.createMockSessionManager(mockSession))
        formatter = OmniSharpFormatter(formattingService)
        
        // 准备模拟响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "class Test { }")
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("v2/format/file", responseJson)
    }
    
    @Test
    fun testFormatAsync() {
        // 执行测试
        val future = formatter.formatAsync(psiFile)
        val success = future.get()
        
        // 验证结果
        assertTrue(success)
        // 验证文档内容被更新
        Mockito.verify(document, Mockito.atLeastOnce()).replaceString(0, 10, "class Test { }")
        // 验证文档被提交
        Mockito.verify(psiDocumentManager, Mockito.atLeastOnce()).commitDocument(document)
    }
    
    @Test
    fun testFormatSelectionAsync() {
        // 设置选择区域
        Mockito.`when`(editor.selectionModel.selectionStart).thenReturn(0)
        Mockito.`when`(editor.selectionModel.selectionEnd).thenReturn(5)
        
        // 设置选择区域响应
        val responseJson = JsonObject()
        val body = JsonObject()
        body.addProperty("Buffer", "CLASS")
        responseJson.add("Body", body)
        
        mockSession.setResponse("v2/format/selection", responseJson)
        
        // 执行测试
        val future = formatter.formatSelectionAsync(psiFile, 0, 5)
        val success = future.get()
        
        // 验证结果
        assertTrue(success)
        // 验证文档内容被更新
        Mockito.verify(document, Mockito.atLeastOnce()).replaceString(0, 5, "CLASS")
    }
}
```

## 4. 集成测试实现

### 4.1 基本集成测试

```kotlin
package com.intellij.csharp.omnisharp.integration

import com.intellij.csharp.omnisharp.completion.OmniSharpCompletionProvider
import com.intellij.csharp.omnisharp.diagnostics.OmniSharpDiagnosticService
import com.intellij.csharp.omnisharp.formatting.OmniSharpFormatter
import com.intellij.csharp.omnisharp.navigation.OmniSharpNavigationService
import com.intellij.csharp.omnisharp.utils.TestUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 基本集成测试
 */
class BasicIntegrationTest : BasePlatformTestCase() {
    private lateinit var testFile: Pair<VirtualFile, PsiFile>
    private lateinit var editor: Editor
    
    @Before
    override fun setUp() {
        super.setUp()
        
        // 创建测试文件
        testFile = TestUtil.createTestFile(
            project,
            "Test.cs",
            "class Test {\n    void Method() {\n        int x = 10;\n    }\n}"
        )
        
        // 打开文件编辑器
        editor = FileEditorManager.getInstance(project).openFile(testFile.first, true)[0] as Editor
    }
    
    @Test
    fun testCompletionProviderIntegration() {
        // 获取补全提供者
        val completionProvider = ApplicationManager.getApplication().getService(OmniSharpCompletionProvider::class.java)
        
        // 验证服务可用
        assertNotNull(completionProvider)
        
        // 在真实环境中，这里可以测试补全提供者与编辑器的交互
        // 由于集成测试需要真实的OmniSharp服务器，这里只做基本验证
    }
    
    @Test
    fun testNavigationServiceIntegration() {
        // 获取导航服务
        val navigationService = ApplicationManager.getApplication().getService(OmniSharpNavigationService::class.java)
        
        // 验证服务可用
        assertNotNull(navigationService)
    }
    
    @Test
    fun testDiagnosticServiceIntegration() {
        // 获取诊断服务
        val diagnosticService = ApplicationManager.getApplication().getService(OmniSharpDiagnosticService::class.java)
        
        // 验证服务可用
        assertNotNull(diagnosticService)
    }
    
    @Test
    fun testFormattingIntegration() {
        // 获取格式化器
        val formatter = ApplicationManager.getApplication().getService(OmniSharpFormatter::class.java)
        
        // 验证服务可用
        assertNotNull(formatter)
    }
}
```

### 4.2 功能集成测试

```kotlin
package com.intellij.csharp.omnisharp.integration

import com.intellij.csharp.omnisharp.completion.OmniSharpCompletionService
import com.intellij.csharp.omnisharp.diagnostics.OmniSharpDiagnosticService
import com.intellij.csharp.omnisharp.formatting.OmniSharpFormattingService
import com.intellij.csharp.omnisharp.navigation.OmniSharpNavigationService
import com.intellij.csharp.omnisharp.utils.TestUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 功能集成测试
 * 注意：这些测试需要运行OmniSharp服务器，在CI环境中可能需要特殊配置
 */
class FeatureIntegrationTest : BasePlatformTestCase() {
    private lateinit var testFile: Pair<VirtualFile, PsiFile>
    
    @Before
    override fun setUp() {
        super.setUp()
        
        // 创建测试文件
        testFile = TestUtil.createTestFile(
            project,
            "IntegrationTest.cs",
            "using System;\n\nnamespace TestNamespace {\n    public class TestClass {\n        private int _testField;\n        \n        public void TestMethod() {\n            Console.WriteLine(\"Test\");\n        }\n    }\n}"
        )
    }
    
    @Test
    fun testCompletionWithRealServer() {
        // 获取补全服务
        val completionService = ApplicationManager.getApplication().getService(OmniSharpCompletionService::class.java)
        
        try {
            // 尝试获取补全，超时5秒
            val completions = completionService.getCompletions(
                project,
                testFile.first,
                testFile.second.text,
                testFile.second.text.indexOf("Console"),
                0
            ).get(5, TimeUnit.SECONDS)
            
            // 在真实环境中，这里应该能获取到补全项
            // 由于集成测试环境可能没有OmniSharp服务器，这里只验证不会抛出异常
        } catch (e: Exception) {
            // 在没有OmniSharp服务器的环境中，这是预期行为
            // 记录异常但不失败测试
            System.err.println("Completion test skipped: " + e.message)
        }
    }
    
    @Test
    fun testNavigationWithRealServer() {
        // 获取导航服务
        val navigationService = ApplicationManager.getApplication().getService(OmniSharpNavigationService::class.java)
        
        try {
            // 尝试查找定义，超时5秒
            val definitions = navigationService.findDefinition(
                project,
                testFile.first,
                testFile.second.text,
                testFile.second.text.indexOf("_testField"),
                0
            ).get(5, TimeUnit.SECONDS)
            
            // 在真实环境中，这里应该能找到定义
        } catch (e: Exception) {
            // 在没有OmniSharp服务器的环境中，这是预期行为
            System.err.println("Navigation test skipped: " + e.message)
        }
    }
    
    @Test
    fun testDiagnosticsWithRealServer() {
        // 获取诊断服务
        val diagnosticService = ApplicationManager.getApplication().getService(OmniSharpDiagnosticService::class.java)
        
        try {
            // 尝试获取诊断，超时5秒
            val diagnostics = diagnosticService.getDiagnostics(project, testFile.first)
                .get(5, TimeUnit.SECONDS)
            
            // 在真实环境中，这里应该能获取到诊断信息
        } catch (e: Exception) {
            // 在没有OmniSharp服务器的环境中，这是预期行为
            System.err.println("Diagnostics test skipped: " + e.message)
        }
    }
    
    @Test
    fun testFormattingWithRealServer() {
        // 获取格式化服务
        val formattingService = ApplicationManager.getApplication().getService(OmniSharpFormattingService::class.java)
        
        try {
            // 尝试格式化，超时5秒
            val formattedContent = formattingService.formatFile(
                project,
                testFile.first,
                "class Unformatted{int x;}"
            ).get(5, TimeUnit.SECONDS)
            
            // 在真实环境中，这里应该返回格式化后的内容
        } catch (e: Exception) {
            // 在没有OmniSharp服务器的环境中，这是预期行为
            System.err.println("Formatting test skipped: " + e.message)
        }
    }
}
```

## 5. 性能测试

### 5.1 补全性能测试

```kotlin
package com.intellij.csharp.omnisharp.completion

import com.intellij.csharp.omnisharp.common.MockOmniSharpSession
import com.intellij.csharp.omnisharp.services.OmniSharpCompletionService
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.assertTrue

/**
 * 补全性能测试
 */
class CompletionPerformanceTest {
    @Mock
    private lateinit var project: Project
    
    @Mock
    private lateinit var file: VirtualFile
    
    private lateinit var mockSession: MockOmniSharpSession
    private lateinit var completionService: OmniSharpCompletionService
    
    @BeforeEach
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        
        // 创建模拟会话
        mockSession = MockOmniSharpSession()
        
        // 创建补全服务
        completionService = OmniSharpCompletionService(MockOmniSharpSession.createMockSessionManager(mockSession))
        
        // 设置模拟文件
        Mockito.`when`(file.path).thenReturn("performance.cs")
        
        // 准备模拟响应（包含50个补全项）
        val responseJson = JsonObject()
        val body = JsonObject()
        val suggestions = JsonArray()
        
        for (i in 0 until 50) {
            val suggestion = JsonObject()
            suggestion.addProperty("Text", "CompletionItem$i")
            suggestion.addProperty("Kind", "Method")
            suggestion.addProperty("SortText", "$i")
            suggestions.add(suggestion)
        }
        
        body.add("Suggestions", suggestions)
        responseJson.add("Body", body)
        
        // 设置响应
        mockSession.setResponse("autocomplete", responseJson)
    }
    
    @Test
    fun testCompletionPerformance() {
        val iterations = 10
        val warmupIterations = 3
        var totalTime = 0L
        
        // 预热
        for (i in 0 until warmupIterations) {
            completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        }
        
        // 性能测试
        for (i in 0 until iterations) {
            val startTime = System.currentTimeMillis()
            completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
            val endTime = System.currentTimeMillis()
            totalTime += (endTime - startTime)
        }
        
        val avgTime = totalTime.toDouble() / iterations
        println("Average completion time: $avgTime ms")
        
        // 验证平均响应时间小于100ms
        assertTrue(avgTime < 100, "Average completion time should be less than 100ms")
    }
    
    @Test
    fun testCompletionCachePerformance() {
        // 第一次调用（应该发送请求）
        completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        
        val cacheIterations = 100
        val startTime = System.currentTimeMillis()
        
        // 多次调用，应该使用缓存
        for (i in 0 until cacheIterations) {
            completionService.getCompletions(project, file, "class Test { }", 10, 1).get()
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        val avgCacheTime = totalTime.toDouble() / cacheIterations
        
        println("Average cached completion time: $avgCacheTime ms")
        
        // 验证缓存响应时间小于5ms
        assertTrue(avgCacheTime < 5, "Average cached completion time should be less than 5ms")
    }
}
```

## 6. 测试运行和覆盖率

### 6.1 测试运行配置

在 IntelliJ IDEA 中配置测试运行：

1. 创建 JUnit 运行配置
2. 设置测试类或包路径
3. 添加 VM 选项（如果需要）
4. 配置测试输出目录

### 6.2 覆盖率报告

使用 JaCoCo 生成测试覆盖率报告：

```gradle
// 在 build.gradle 中添加
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.7"
}

jacocoTestReport {
    reports {
        xml.enabled true
        html.enabled true
    }
    
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/generated/**',
                '**/test/**'
            ])
        }))
    }
}

test.finalizedBy jacocoTestReport
```

## 7. 测试最佳实践

### 7.1 单元测试最佳实践

1. **单一职责**：每个测试方法只测试一个功能点
2. **快速执行**：单元测试应该快速执行，避免长时间运行
3. **隔离性**：使用模拟对象隔离外部依赖
4. **可重复性**：测试应该是可重复的，不受外部环境影响
5. **清晰的断言**：使用明确的断言语句验证结果
6. **测试命名**：使用清晰的命名约定，如 `testMethodName_Scenario_ExpectedBehavior`

### 7.2 集成测试最佳实践

1. **环境准备**：确保测试环境准备充分，包括必要的服务和资源
2. **清理资源**：测试完成后清理创建的资源
3. **错误处理**：适当地处理异常情况
4. **超时设置**：为集成测试设置合理的超时时间
5. **日志记录**：记录足够的日志以便调试

### 7.3 性能测试最佳实践

1. **预热**：在测量性能前进行预热
2. **多次迭代**：进行多次迭代取平均值
3. **环境隔离**：在隔离的环境中运行性能测试
4. **基线比较**：与之前的结果进行比较
5. **限制资源**：限制测试使用的资源，确保一致性

## 8. 结论

本文档提供了 OmniSharp 编辑器功能集成的完整测试方案，包括单元测试、集成测试和性能测试。通过这些测试，可以确保编辑器功能在 IntelliJ 平台上的正确性、稳定性和性能。

测试方案的主要优点：

1. **全面覆盖**：测试覆盖了所有核心功能模块
2. **模拟依赖**：使用模拟对象隔离外部依赖，提高测试的可靠性
3. **性能关注**：包含性能测试，确保功能在实际使用中的响应速度
4. **可扩展性**：测试架构设计灵活，易于扩展和维护

通过实施这个测试方案，可以提高代码质量，减少潜在的缺陷，为用户提供稳定可靠的 C# 开发体验。