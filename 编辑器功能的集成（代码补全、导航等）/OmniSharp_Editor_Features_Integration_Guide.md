# OmniSharp 编辑器功能集成指南

## 1. 概述

本文档详细介绍了如何在 IntelliJ 平台中集成和使用 OmniSharp 编辑器功能，包括代码补全、导航、诊断和格式化等核心功能。通过本指南，开发者可以全面了解 OmniSharp 编辑器功能的架构设计、使用方法、配置选项和最佳实践。

### 1.1 文档目的

- 提供 OmniSharp 编辑器功能集成的整体架构说明
- 详细介绍各功能模块的使用方法
- 说明配置选项和自定义方式
- 提供常见问题的解决方案
- 总结最佳实践和性能优化建议

### 1.2 适用读者

- IntelliJ 插件开发者
- C# 语言支持维护者
- 希望扩展现有 C# 支持的开发者
- 对语言服务协议感兴趣的开发者

## 2. 系统架构

### 2.1 整体架构

OmniSharp 编辑器功能集成架构采用分层设计，主要包括以下几个核心层次：

1. **IntelliJ 平台集成层**：负责与 IntelliJ 平台组件交互，包括编辑器、PSI、文件系统等
2. **功能模块层**：实现各种编辑器功能，如代码补全、导航、诊断、格式化等
3. **服务层**：提供与 OmniSharp 服务器通信的功能，处理请求和响应
4. **通信协议层**：负责序列化和反序列化 JSON 消息
5. **缓存层**：优化性能，减少重复请求

![整体架构图](architecture_overview.png)

### 2.2 组件关系

以下是主要组件之间的关系：

```
+-------------------+     +-------------------+     +-------------------+
|                   |     |                   |     |                   |
|  IntelliJ 平台    |     |  功能模块         |     |  服务层           |
|                   |     |                   |     |                   |
|  - Editor         |---->|  - Completion     |---->|  - Completion     |
|  - PSI            |     |  - Navigation     |     |    Service        |
|  - Document       |     |  - Diagnostics    |     |  - Navigation     |
|  - FileSystem     |     |  - Formatting     |     |    Service        |
|                   |     |                   |     |  - Diagnostic     |
+-------------------+     +-------------------+     |    Service        |
         |                        |                 |  - Formatting     |
         |                        |                 |    Service        |
         v                        v                 +-------------------+
+-------------------+     +-------------------+               |
|                   |     |                   |               v
|  UI 组件          |     |  配置管理         |     +-------------------+
|                   |     |                   |     |                   |
|  - Completion     |     |  - Settings      |     |  通信协议层       |
|    List           |     |  - Preferences   |     |                   |
|  - Gutter Icons   |     |  - Options       |     |  - JSON           |
|  - Highlighting   |     |                   |     |    Serialization  |
|                   |     +-------------------+     |  - Message        |
+-------------------+                               |    Handling       |
                                                    +-------------------+
                                                          |
                                                          v
                                                    +-------------------+
                                                    |                   |
                                                    |  OmniSharp 服务器 |
                                                    |                   |
                                                    +-------------------+
```

## 3. 功能模块

### 3.1 代码补全功能

#### 3.1.1 功能概述

代码补全功能提供智能的代码建议，包括类名、方法名、变量名、关键字等，帮助开发者快速编写代码。补全项包含详细的描述信息，如类型、参数列表、文档注释等。

#### 3.1.2 使用方法

1. 在 C# 文件中编辑代码时，输入代码会自动触发补全建议
2. 也可以手动触发补全（默认快捷键：Ctrl+空格）
3. 使用上下箭头键选择补全项
4. 按 Enter 键或 Tab 键接受补全项

#### 3.1.3 配置选项

在 Settings/Preferences | Editor | General | Code Completion 中可以配置补全行为：

- **Autopopup in (ms)**：设置自动弹出补全列表的延迟时间
- **Show the documentation popup (ms)**：设置显示文档弹窗的延迟时间
- **Sort by name**：按名称排序补全项
- **Show the parameter info popup (ms)**：设置显示参数信息弹窗的延迟时间

OmniSharp 特有的配置（在 Settings/Preferences | Languages & Frameworks | C# | OmniSharp）：

- **Completion mode**：选择补全模式（智能/基本）
- **Include snippets**：是否在补全中包含代码片段
- **Filter by scope**：根据当前作用域过滤补全项
- **Max suggestions**：设置最大显示的补全项数量

#### 3.1.4 高级功能

- **参数信息提示**：在输入方法参数时显示参数信息
- **文档预览**：显示补全项的文档注释
- **类型推断**：根据上下文智能推断类型
- **实时代码分析**：基于当前代码状态提供补全建议

### 3.2 代码导航功能

#### 3.2.1 功能概述

代码导航功能允许开发者在代码之间快速导航，包括查找定义、查找引用、查看层次结构等，提高代码理解和开发效率。

#### 3.2.2 使用方法

##### 查找定义

1. 将光标放在标识符上
2. 使用以下方式之一查找定义：
   - 快捷键：Ctrl+B 或 Ctrl+鼠标左键点击
   - 右键菜单：Go To | Declaration or Usages
   - 编辑器顶部：Navigate | Declaration

##### 查找引用

1. 将光标放在标识符上
2. 使用以下方式之一查找引用：
   - 快捷键：Alt+F7
   - 右键菜单：Find Usages
   - 编辑器顶部：Edit | Find | Find Usages

##### 查看类型层次结构

1. 将光标放在类名上
2. 使用以下方式之一查看层次结构：
   - 快捷键：Ctrl+H
   - 右键菜单：Type Hierarchy
   - 编辑器顶部：Navigate | Type Hierarchy

##### 查看文件结构

- 快捷键：Ctrl+F12
- 右键菜单：File Structure
- 编辑器顶部：Navigate | File Structure

#### 3.2.3 配置选项

在 Settings/Preferences | Editor | General | Go to Definition & Usages 中可以配置导航行为：

- **Go to Usages on Ctrl+Click**：启用 Ctrl+点击跳转到引用
- **Show popup with usages count**：显示引用数量弹窗
- **Exclude tests**：在查找引用时排除测试文件

OmniSharp 特有的配置：

- **Navigate to metadata**：当定义不在项目中时，导航到元数据视图
- **Max references**：设置显示的最大引用数量
- **Include base classes/interfaces**：在层次结构中包含基类和接口

#### 3.2.4 高级功能

- **快速导航**：使用 Ctrl+Shift+N 快速导航到文件
- **符号导航**：使用 Ctrl+Alt+Shift+N 快速导航到符号
- **最近编辑**：使用 Ctrl+Shift+Backspace 导航到最近编辑位置
- **导航历史**：使用 Ctrl+Alt+Left/Right 导航历史记录

### 3.3 代码诊断功能

#### 3.3.1 功能概述

代码诊断功能实时分析代码，检测潜在的错误、警告和代码质量问题，并提供修复建议，帮助开发者编写高质量的代码。

#### 3.3.2 使用方法

诊断结果以以下方式显示：

1. **编辑器标记**：在编辑器右侧的滚动条区域显示问题标记
2. **代码高亮**：在代码中高亮显示有问题的部分
3. **错误提示**：鼠标悬停时显示详细的错误信息
4. **快速修复**：对于某些问题，提供灯泡图标，点击后显示修复建议

##### 应用快速修复

1. 点击灯泡图标或使用 Alt+Enter 快捷键
2. 从弹出的菜单中选择合适的修复选项
3. 修复将自动应用到代码中

##### 查看项目诊断

- 在 Project 视图中，右键点击项目或模块
- 选择 "Inspect Code..."
- 配置检查范围和选项
- 点击 "OK" 开始分析
- 分析完成后，在 Inspection Results 工具窗口中查看结果

#### 3.3.3 配置选项

在 Settings/Preferences | Editor | Inspections 中可以配置检查规则：

- 启用/禁用特定的检查规则
- 设置规则的严重性级别（Error/Warning/Info）
- 配置规则的详细参数

OmniSharp 特有的配置：

- **Diagnostic level**：设置诊断级别（Errors only/Warnings and above/All issues）
- **Background analysis**：是否启用后台分析
- **Analysis delay**：设置分析延迟时间
- **Exclude generated files**：是否排除生成的文件
- **Custom ruleset**：指定自定义的规则集文件

#### 3.3.4 支持的诊断类型

- **语法错误**：检测语法错误和拼写错误
- **语义错误**：检测类型不匹配、未定义变量等语义错误
- **代码质量问题**：检测潜在的代码质量问题，如未使用的变量、多余的代码等
- **性能问题**：检测可能导致性能问题的代码模式
- **安全问题**：检测潜在的安全漏洞
- **样式问题**：检测不符合代码样式约定的代码

### 3.4 代码格式化功能

#### 3.4.1 功能概述

代码格式化功能自动调整代码的缩进、换行、空格等格式，使代码符合指定的编码规范，提高代码可读性和一致性。

#### 3.4.2 使用方法

##### 格式化整个文件

1. 打开要格式化的 C# 文件
2. 使用以下方式之一格式化：
   - 快捷键：Ctrl+Alt+L
   - 右键菜单：Reformat Code
   - 编辑器顶部：Code | Reformat Code

##### 格式化选择区域

1. 选择要格式化的代码区域
2. 使用以下方式之一格式化：
   - 快捷键：Ctrl+Alt+L
   - 右键菜单：Reformat Code
   - 编辑器顶部：Code | Reformat Code

##### 自动格式化

可以配置在特定操作后自动格式化：

- 保存文件时
- 粘贴代码时
- 输入特定字符（如分号）时

#### 3.4.3 配置选项

在 Settings/Preferences | Editor | Code Style | C# 中可以配置格式化选项：

##### 缩进设置

- **Use tab character**：使用制表符还是空格
- **Tab size**：制表符大小
- **Indent**：缩进大小
- **Continuation indent**：续行缩进大小

##### 空格设置

- 在括号内、操作符周围、方法参数等位置添加或移除空格
- 配置特定语法结构的空格规则

##### 换行设置

- 最大行长度
- 控制语句换行
- 方法调用和定义换行
- 数组和初始化器换行

##### 其他设置

- 大括号风格
- 命名约定
- 空行和空白行
- 注释格式

OmniSharp 特有的配置：

- **Format on save**：保存文件时自动格式化
- **Format on paste**：粘贴代码时自动格式化
- **Format on semicolon**：输入分号时自动格式化当前行
- **Use .editorconfig**：使用项目中的 .editorconfig 文件配置

#### 3.4.4 高级功能

- **格式刷**：复制一段代码的格式并应用到其他代码
- **自动缩进**：自动调整代码缩进
- **行合并/拆分**：根据配置合并或拆分长行
- **代码清理**：执行更全面的代码格式化和清理操作

## 4. 配置管理

### 4.1 全局配置

全局配置适用于所有项目，可以在 Settings/Preferences 中设置：

1. 打开 Settings/Preferences 对话框（Ctrl+Alt+S）
2. 导航到 Languages & Frameworks | C# | OmniSharp
3. 配置各功能模块的选项

### 4.2 项目级配置

项目级配置仅适用于当前项目，可以通过以下方式设置：

#### 4.2.1 .editorconfig 文件

OmniSharp 支持使用 .editorconfig 文件配置代码样式和格式化选项：

```ini
# EditorConfig is awesome: https://EditorConfig.org

# top-most EditorConfig file
root = true

[*.cs]
# 缩进样式
indent_style = space
indent_size = 4

# 换行符
end_of_line = crlf
charset = utf-8
trim_trailing_whitespace = true
insert_final_newline = true

# 代码样式
csharp_new_line_before_open_brace = all
csharp_new_line_before_else = true
csharp_new_line_before_catch = true
csharp_new_line_before_finally = true
csharp_new_line_before_members_in_object_initializers = true
csharp_new_line_before_members_in_anonymous_types = true
csharp_new_line_between_query_expression_clauses = true
```

#### 4.2.2 OmniSharp.json 文件

OmniSharp 还支持使用 omnisharp.json 文件配置服务器行为：

```json
{
  "FormattingOptions": {
    "OrganizeImports": true,
    "EnableEditorConfigSupport": true,
    "NewLine": "\r\n",
    "IndentationSize": 4,
    "TabSize": 4,
    "UseTabs": false,
    "WordWrapColumn": 100
  },
  "RoslynExtensionsOptions": {
    "EnableAnalyzersSupport": true,
    "LocationPaths": []
  },
  "RenameOptions": {
    "RenameInComments": true,
    "RenameInStrings": true,
    "RenameOverloads": true
  }
}
```

### 4.3 自定义键盘快捷键

可以自定义各功能的键盘快捷键：

1. 打开 Settings/Preferences 对话框（Ctrl+Alt+S）
2. 导航到 Keymap
3. 在搜索框中输入功能名称（如 "Reformat Code"）
4. 右键点击功能，选择 "Add Keyboard Shortcut"
5. 按下要使用的快捷键组合
6. 点击 "OK" 保存设置

## 5. 高级用法

### 5.1 扩展功能

OmniSharp 编辑器功能设计为可扩展的架构，可以通过实现相应的接口来扩展功能：

#### 5.1.1 扩展代码补全

```kotlin
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext

class MyCustomCompletionContributor : CompletionContributor() {
    init {
        extend(
            com.intellij.codeInsight.completion.CompletionType.BASIC,
            PlatformPatterns.psiElement(PsiElement::class.java).withLanguage(CSharpLanguage.INSTANCE),
            MyCustomCompletionProvider()
        )
    }
}

class MyCustomCompletionProvider : OmniSharpCompletionProvider {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext?,
        resultSet: CompletionResultSet
    ) {
        // 添加自定义补全逻辑
        val myCompletion = LookupElementBuilder.create("MyCustomCompletion")
            .withPresentableText("MyCustomCompletion")
            .withTypeText("Custom Type")
            .withInsertHandler {\ context, item ->
                // 自定义插入逻辑
            }
        resultSet.addElement(myCompletion)
        
        // 调用父类方法以保留默认补全
        super.addCompletions(parameters, context, resultSet)
    }
}
```

#### 5.1.2 扩展代码诊断

```kotlin
import com.intellij.codeInspection.InspectionToolProvider
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.csharp.psi.CSharpFile
import com.intellij.csharp.psi.CSharpElementVisitor

class MyCustomInspectionToolProvider : InspectionToolProvider {
    override fun getInspectionClasses(): Array<Class<out LocalInspectionTool>> {
        return arrayOf(MyCustomInspection::class.java)
    }
}

class MyCustomInspection : LocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return object : CSharpElementVisitor() {
            override fun visitFile(file: CSharpFile) {
                // 自定义诊断逻辑
                // 例如，检查特定的代码模式
                // 如果发现问题，使用 holder.registerProblem() 注册
            }
        }
    }
}
```

#### 5.1.3 扩展代码格式化

```kotlin
import com.intellij.formatting.FormattingModel
import com.intellij.formatting.FormattingModelBuilder
import com.intellij.formatting.FormattingModelProvider
import com.intellij.formatting.Indent
import com.intellij.formatting.Wrap
import com.intellij.formatting.SpacingBuilder
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.csharp.psi.CSharpFile

class MyCustomFormattingModelBuilder : FormattingModelBuilder {
    override fun createModel(element: PsiElement, settings: CodeStyleSettings): FormattingModel {
        val file = element.containingFile
        if (file is CSharpFile) {
            // 创建自定义格式化模型
            val block = MyCustomFormattingBlock(element, Indent.getNoneIndent(), Wrap.createWrap(WrapType.NONE, false), settings)
            return FormattingModelProvider.createFormattingModelForPsiFile(
                file,
                block,
                settings
            )
        }
        // 如果不是 C# 文件，使用默认格式化
        return FormattingModelProvider.createFormattingModelForPsiFile(
            element.containingFile,
            FormattingModelProvider.createFormattingModelForPsiFile(
                element.containingFile,
                element.node,
                settings
            ).rootBlock,
            settings
        )
    }
    
    override fun getRangeAffectingIndent(element: PsiElement, offset: Int, elementAtOffset: TextRange): TextRange {
        // 自定义缩进范围计算
        return elementAtOffset
    }
}
```

### 5.2 与其他插件集成

OmniSharp 编辑器功能可以与其他插件集成，提供更丰富的功能：

#### 5.2.1 与版本控制系统集成

```kotlin
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.csharp.omnisharp.services.OmniSharpDiagnosticService
import com.intellij.openapi.project.Project

class VcsIntegration {
    fun analyzeChangedFiles(project: Project) {
        val changeListManager = ChangeListManager.getInstance(project)
        val changes = changeListManager.allChanges
        
        val diagnosticService = project.getService(OmniSharpDiagnosticService::class.java)
        
        for (change in changes) {
            val virtualFile = change.virtualFile
            if (virtualFile != null && virtualFile.extension == "cs") {
                // 分析变更的文件
                diagnosticService.getDiagnostics(project, virtualFile).thenAccept {\ diagnostics ->
                    // 处理诊断结果
                    // 例如，标记严重问题
                }
            }
        }
    }
}
```

#### 5.2.2 与构建系统集成

```kotlin
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListenerAdapter
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemLauncher
import com.intellij.openapi.project.Project
import com.intellij.csharp.omnisharp.services.OmniSharpDiagnosticService

class BuildSystemIntegration {
    fun buildAndAnalyze(project: Project) {
        val taskId = ExternalSystemTaskId.create("MsBuild", ExternalSystemTaskType.EXECUTE_TASK, project)
        
        // 启动构建
        ExternalSystemLauncher.getInstance(project).execute(..., object : ExternalSystemTaskNotificationListenerAdapter() {
            override fun onSuccess(id: ExternalSystemTaskId) {
                // 构建成功后执行分析
                val diagnosticService = project.getService(OmniSharpDiagnosticService::class.java)
                
                // 获取项目中的所有 C# 文件并分析
                // ...
            }
        })
    }
}
```

## 6. 性能优化

### 6.1 缓存管理

OmniSharp 编辑器功能使用多级缓存来提高性能：

- **内存缓存**：缓存最近的请求结果
- **文件系统缓存**：缓存分析结果到文件系统
- **索引缓存**：缓存符号索引信息

可以通过以下方式优化缓存：

1. **调整缓存大小**：在 OmniSharp 设置中调整缓存大小
2. **启用持久化缓存**：保存缓存到磁盘，加快下次启动
3. **定期清理缓存**：清除过期的缓存数据

### 6.2 减少请求频率

- **请求合并**：合并短时间内的多个类似请求
- **延迟请求**：使用延迟执行，避免频繁发送请求
- **增量更新**：只分析变更的部分，而不是整个文件

### 6.3 优化配置

- **禁用不必要的功能**：关闭不需要的诊断规则或功能
- **调整分析级别**：根据需要调整分析的深度和广度
- **排除大型文件**：排除大型生成文件或第三方库文件

### 6.4 监控性能

可以通过以下方式监控性能：

1. **启用日志**：在 Help | Diagnostic Tools | Debug Log Settings 中启用 OmniSharp 日志
2. **使用性能分析器**：使用 IDE 内置的性能分析器
3. **检查事件日志**：在 Help | Show Log in Explorer 中查看事件日志

## 7. 故障排除

### 7.1 常见问题

#### 7.1.1 代码补全不工作

**可能的原因**：
- OmniSharp 服务器未启动或已崩溃
- 项目配置不正确
- 缓存问题

**解决方案**：
1. 检查 OmniSharp 服务器状态（View | Tool Windows | OmniSharp）
2. 重新启动 OmniSharp 服务器（右键点击 OmniSharp 工具窗口中的服务器，选择 "Restart"）
3. 清除缓存（File | Invalidate Caches / Restart...）
4. 检查项目文件是否正确

#### 7.1.2 导航功能无法找到定义

**可能的原因**：
- 项目符号未正确索引
- 引用的程序集未加载
- 代码有语法错误

**解决方案**：
1. 重建项目
2. 检查引用路径
3. 修复代码中的语法错误
4. 重新索引项目（File | Invalidate Caches / Restart...，选择 "Invalidate and Restart"）

#### 7.1.3 诊断信息不准确或缺失

**可能的原因**：
- 诊断级别设置过低
- 特定规则被禁用
- 服务器配置问题

**解决方案**：
1. 调整诊断级别设置
2. 检查并启用必要的检查规则
3. 验证 omnisharp.json 配置
4. 重新启动服务器

#### 7.1.4 格式化不符合预期

**可能的原因**：
- 格式化配置不正确
- .editorconfig 文件冲突
- 服务器版本问题

**解决方案**：
1. 检查格式化设置
2. 检查项目中的 .editorconfig 文件
3. 确保使用最新版本的 OmniSharp 服务器
4. 重置格式化设置

### 7.2 日志和调试

#### 7.2.1 查看 OmniSharp 日志

1. 打开 OmniSharp 工具窗口（View | Tool Windows | OmniSharp）
2. 查看日志标签页中的输出
3. 如果需要详细日志，可以启用详细模式：
   - 在 Settings/Preferences | Languages & Frameworks | C# | OmniSharp 中启用 "Detailed logging"

#### 7.2.2 调试插件问题

1. 在开发模式下运行插件（使用 Plugin DevKit）
2. 设置断点并调试
3. 查看 IDE 日志：Help | Show Log in Explorer

#### 7.2.3 报告问题

如果遇到无法解决的问题，可以报告给插件维护者：

1. 收集相关信息：
   - IDE 版本
   - 插件版本
   - OmniSharp 服务器版本
   - 日志文件
   - 复现步骤
   - 错误截图
2. 在插件的 GitHub 仓库上创建 issue

## 8. 最佳实践

### 8.1 开发工作流

1. **设置合理的配置**：根据项目需求配置 OmniSharp 功能
2. **利用自动格式化**：启用 "Format on Save" 确保代码风格一致
3. **关注诊断信息**：及时修复警告和错误
4. **使用代码导航**：利用导航功能快速理解和修改代码
5. **定期更新**：保持插件和 OmniSharp 服务器的最新版本

### 8.2 项目组织结构

1. **使用 .editorconfig**：在项目根目录添加 .editorconfig 文件，统一代码风格
2. **配置 omnisharp.json**：根据项目需求自定义 OmniSharp 行为
3. **设置适当的忽略规则**：在 .gitignore 中排除生成的文件和缓存

### 8.3 团队协作

1. **共享配置**：团队成员使用相同的格式化和诊断配置
2. **代码审查**：使用诊断功能辅助代码审查
3. **自动化检查**：在 CI/CD 流程中集成代码分析

## 9. 结论

OmniSharp 编辑器功能为 IntelliJ 平台提供了强大的 C# 语言支持，包括代码补全、导航、诊断和格式化等核心功能。通过本指南的介绍，开发者可以全面了解这些功能的使用方法和配置选项，并利用高级特性和最佳实践来提高开发效率。

OmniSharp 编辑器功能的设计注重性能、可扩展性和用户体验，为 C# 开发者在 IntelliJ 平台上提供了优秀的开发体验。随着技术的不断发展，OmniSharp 编辑器功能也将持续改进和完善，为开发者提供更多功能和更好的体验。

## 10. 附录

### 10.1 快捷键参考

| 功能 | Windows/Linux | macOS |
|------|--------------|-------|
| 代码补全 | Ctrl+空格 | Control+空格 |
| 查找定义 | Ctrl+B | Command+B |
| 查找引用 | Alt+F7 | Option+F7 |
| 格式化代码 | Ctrl+Alt+L | Command+Option+L |
| 快速修复 | Alt+Enter | Option+Enter |
| 文件结构 | Ctrl+F12 | Command+F12 |
| 类型层次结构 | Ctrl+H | Command+H |
| 快速导航到文件 | Ctrl+Shift+N | Command+Shift+N |
| 快速导航到符号 | Ctrl+Alt+Shift+N | Command+Option+Shift+N |
| 导航历史 | Ctrl+Alt+Left/Right | Command+Option+Left/Right |

### 10.2 配置文件示例

#### 10.2.1 omnisharp.json 完整示例

```json
{
  "FormattingOptions": {
    "OrganizeImports": true,
    "EnableEditorConfigSupport": true,
    "NewLine": "\r\n",
    "IndentationSize": 4,
    "TabSize": 4,
    "UseTabs": false,
    "WordWrapColumn": 100,
    "WrappingPreserveSingleLine": true,
    "WrappingKeepStatementsOnSingleLine": true,
    "NewLinesForBracesInTypes": true,
    "NewLinesForBracesInMethods": true,
    "NewLinesForBracesInProperties": true,
    "NewLinesForBracesInAccessors": true,
    "NewLinesForBracesInAnonymousMethods": true,
    "NewLinesForBracesInControlBlocks": true,
    "NewLinesForBracesInAnonymousTypes": true,
    "NewLinesForBracesInObjectCollectionArrayInitializers": true,
    "NewLinesForBracesInLambdaExpressionBody": true,
    "NewLineForElse": true,
    "NewLineForCatch": true,
    "NewLineForFinally": true,
    "NewLineForMembersInObjectInit": true,
    "NewLineForMembersInAnonymousTypes": true,
    "NewLineForClausesInQuery": true
  },
  "RoslynExtensionsOptions": {
    "EnableAnalyzersSupport": true,
    "LocationPaths": [],
    "ProjectLoadTimeout": 30
  },
  "RenameOptions": {
    "RenameInComments": true,
    "RenameInStrings": true,
    "RenameOverloads": true
  },
  "QuickFixOptions": {
    "ShowQuickInfoTooltip": true,
    "ShowFixAllInDocument": true,
    "ShowFixAllInProject": true,
    "ShowFixAllInSolution": true
  },
  "MsBuild": {
    "LoadProjectsOnDemand": true,
    "UseLegacySdkResolver": false,
    "MsBuildOverridePath": null
  },
  "FileOptions": {
    "SystemExcludeSearchPatterns": ["node_modules/**/*", "bin/**/*", "obj/**/*", ".git/**/*"],
    "ExcludeSearchPatterns": []
  }
}
```

#### 10.2.2 .editorconfig 完整示例

```ini
# EditorConfig is awesome: https://EditorConfig.org

# top-most EditorConfig file
root = true

# Unix-style newlines with a newline ending every file
[*]
end_of_line = crlf
insert_final_newline = true

# Matches multiple files with brace expansion notation
# Set default charset
[*.{cs,cshtml}]
charset = utf-8
trim_trailing_whitespace = true

# Tab indentation (no size specified)
[*.cs]
indent_style = space
indent_size = 4

# CSharp code style settings:
# Prefer var keywords everywhere
csharp_style_var_for_built_in_types = true:suggestion
csharp_style_var_when_type_is_apparent = true:suggestion
csharp_style_var_elsewhere = true:suggestion

# Prefer method-like constructs to have a block body
csharp_style_expression_bodied_methods = false:warning
csharp_style_expression_bodied_constructors = false:warning
csharp_style_expression_bodied_operators = false:warning

# Prefer property-like constructs to have an expression-body
csharp_style_expression_bodied_properties = true:suggestion
csharp_style_expression_bodied_indexers = true:suggestion
csharp_style_expression_bodied_accessors = true:suggestion

# Suggest more modern language features when available
csharp_style_pattern_matching_over_is_with_cast_check = true:suggestion
csharp_style_pattern_matching_over_as_with_null_check = true:suggestion
csharp_style_inferred_tuple_names = true:suggestion
csharp_style_inferred_anonymous_type_member_names = true:suggestion
csharp_style_inferred_variable_type = true:suggestion
csharp_style_implicit_object_creation_when_type_is_apparent = true:suggestion
csharp_prefer_simple_default_expression = true:suggestion
csharp_style_prefer_index_operator = true:suggestion
csharp_style_prefer_range_operator = true:suggestion
csharp_style_prefer_is_null_check_over_reference_equality_method = true:suggestion

# Newline settings
csharp_new_line_before_open_brace = all
csharp_new_line_before_else = true
csharp_new_line_before_catch = true
csharp_new_line_before_finally = true
csharp_new_line_before_members_in_object_initializers = true
csharp_new_line_before_members_in_anonymous_types = true
csharp_new_line_between_query_expression_clauses = true

# Spacing settings
csharp_space_after_cast = false
csharp_space_after_colon_in_inheritance_clause = true
csharp_space_after_comma = true
csharp_space_after_dot = false
csharp_space_after_keywords_in_control_flow_statements = true
csharp_space_after_semicolon_in_for_statement = true
csharp_space_around_binary_operators = before_and_after
csharp_space_around_declaration_statements = do_not_ignore
csharp_space_before_colon_in_inheritance_clause = true
csharp_space_before_comma = false
csharp_space_before_dot = false
csharp_space_before_open_brace = false
csharp_space_before_semicolon_in_for_statement = false
csharp_space_between_empty_braces = false
csharp_space_between_method_declaration_parameter_list_parentheses = false
csharp_space_between_method_declaration_parameters = true
csharp_space_between_method_reference_parameter_list_parentheses = false
csharp_space_between_method_reference_parameters = true
csharp_space_between_parentheses = false
csharp_space_between_square_brackets = false

# Wrapping settings
csharp_preserve_single_line_statements = false
csharp_preserve_single_line_blocks = false
```

### 10.3 资源链接

- [OmniSharp 官方文档](https://omnisharp.github.io/)
- [IntelliJ 平台 SDK 文档](https://jetbrains.org/intellij/sdk/docs/)
- [Roslyn 工作区 API 文档](https://docs.microsoft.com/en-us/dotnet/api/microsoft.codeanalysis.workspace)
- [C# 语言规范](https://docs.microsoft.com/en-us/dotnet/csharp/language-reference/language-specification/introduction)
- [EditorConfig 官方网站](https://editorconfig.org/)

---

本文档由 OmniSharp 团队编写，最后更新于 2023 年 10 月。如有任何问题或建议，请联系我们的团队。