# OmniSharp.Roslyn 后端接口协议文档

## 概述

OmniSharp.Roslyn 是一个基于 .NET Roslyn 编译器平台的语言服务器，可为各种编辑器提供 C# 语言支持。本文档详细描述了 OmniSharp.Roslyn 提供给 IntelliJ IDEA 插件的后端接口协议，包括接口定义、详细说明、协议实例以及如何使用 Stdio 协议进行集成。

## 通信协议

OmniSharp 支持两种通信协议：
1. **Stdio 协议** - 通过标准输入输出进行通信
2. **HTTP 协议** - 通过 HTTP 请求进行通信

本文档重点介绍 Stdio 协议，因为它更适合 IntelliJ IDEA 插件集成。

### Stdio 协议格式

Stdio 协议使用 JSON 格式的请求和响应。每个请求/响应都是一个 JSON 对象，包含以下基本结构：

```json
{
  "Type": "request",
  "Seq": 1,
  "Command": "endpoint-name",
  "Arguments": {
    // 请求参数
  }
}
```

响应格式：

```json
{
  "Type": "response",
  "Seq": 1,
  "Command": "endpoint-name",
  "Request_seq": 1,
  "Running": true,
  "Success": true,
  "Body": {
    // 响应数据
  }
}
```

## 核心接口协议

### 1. 代码补全 (Code Completion)

**端点**: `/autocomplete` 或 `/v2/completion`

**请求模型**: `CompletionRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "CompletionTrigger": 1,
  "TriggerCharacter": "."
}
```

**响应模型**: `CompletionResponse`

```json
{
  "IsIncomplete": false,
  "Items": [
    {
      "Label": "ToString",
      "Kind": 2,
      "Detail": "string object.ToString()",
      "Documentation": "Returns a string that represents the current object.",
      "Preselect": false,
      "SortText": "ToString",
      "FilterText": "ToString",
      "InsertTextFormat": 1,
      "TextEdit": {
        "StartLine": 10,
        "StartColumn": 15,
        "EndLine": 10,
        "EndColumn": 15,
        "NewText": "ToString()"
      },
      "AdditionalTextEdits": [],
      "Data": {
        "CacheId": 12345,
        "Index": 0
      },
      "HasAfterInsertStep": false
    }
  ]
}
```

**触发类型**:
- `1` (Invoked): 手动触发或自动触发
- `2` (TriggerCharacter): 由特定字符触发

**补全项类型**:
- `1`: Text
- `2`: Method
- `3`: Function
- `4`: Constructor
- `5`: Field
- `6`: Variable
- `7`: Class
- `8`: Interface
- `9`: Module
- `10`: Property
- `11`: Unit
- `12`: Value
- `13`: Enum
- `14`: Keyword
- `15`: Snippet
- `16`: Color
- `17`: File
- `18`: Reference
- `19`: Folder
- `20`: EnumMember
- `21`: Constant
- `22`: Struct
- `23`: Event
- `24`: Operator
- `25`: TypeParameter

### 2. 转到定义 (Go to Definition)

**端点**: `/gotodefinition`

**请求模型**: `GotoDefinitionRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "Timeout": 10000,
  "WantMetadata": false
}
```

**响应模型**: `GotoDefinitionResponse`

```json
{
  "FileName": "path/to/definition.cs",
  "Line": 25,
  "Column": 10,
  "MetadataSource": null,
  "SourceGeneratedInfo": null
}
```

### 3. 转到类型定义 (Go to Type Definition)

**端点**: `/gototypedefinition`

**请求模型**: `GotoTypeDefinitionRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "Timeout": 10000,
  "WantMetadata": false
}
```

**响应模型**: `GotoTypeDefinitionResponse`

```json
{
  "Definitions": [
    {
      "Location": {
        "FileName": "path/to/type.cs",
        "Range": {
          "Start": {
            "Line": 5,
            "Column": 10
          },
          "End": {
            "Line": 20,
            "Column": 15
          }
        }
      },
      "MetadataSource": null,
      "SourceGeneratedFileInfo": null
    }
  ]
}
```

### 4. 查找符号 (Find Symbols)

**端点**: `/findsymbols`

**请求模型**: `Request`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容"
}
```

**响应模型**: `QuickFixResponse`

```json
{
  "QuickFixes": [
    {
      "Text": "MyClass",
      "FileName": "path/to/file.cs",
      "Line": 5,
      "Column": 10,
      "EndLine": 5,
      "EndColumn": 18,
      "Projects": ["Project1"]
    }
  ]
}
```

### 5. 诊断信息 (Diagnostics)

**端点**: `/diagnostics`

**请求模型**: `DiagnosticsRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 0,
  "Column": 0,
  "Buffer": "文件内容"
}
```

**响应模型**: `DiagnosticsResponse`

```json
{
  "QuickFixes": [
    {
      "Text": "Cannot resolve symbol 'MySymbol'",
      "FileName": "path/to/file.cs",
      "Line": 10,
      "Column": 15,
      "EndLine": 10,
      "EndColumn": 23,
      "Projects": ["Project1"],
      "LogLevel": "Error",
      "Id": "CS0103",
      "Tags": []
    }
  ]
}
```

### 6. 更新缓冲区 (Update Buffer)

**端点**: `/updatebuffer`

**请求模型**: `UpdateBufferRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Buffer": "更新后的文件内容",
  "Changes": [
    {
      "StartLine": 10,
      "StartColumn": 5,
      "EndLine": 10,
      "EndColumn": 10,
      "NewText": "newText"
    }
  ],
  "ApplyChangesTogether": false
}
```

**响应模型**: 无特定响应，返回成功状态

### 7. 代码格式化 (Code Format)

**端点**: `/codeformat`

**请求模型**: `CodeFormatRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 0,
  "Column": 0,
  "Buffer": "未格式化的代码"
}
```

**响应模型**: `CodeFormatResponse`

```json
{
  "Buffer": "格式化后的代码"
}
```

### 8. 代码操作 (Code Actions)

**端点**: `/v2/getcodeactions`

**请求模型**: `CodeActionRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "SelectionStartLine": 10,
  "SelectionStartColumn": 15,
  "SelectionEndLine": 10,
  "SelectionEndColumn": 20
}
```

**响应模型**: 包含可用代码操作的列表

```json
{
  "CodeActions": [
    {
      "Identifier": "using System;",
      "Name": "Insert using directive",
      "CodeActionKind": "QuickFix"
    },
    {
      "Identifier": "GenerateMethod",
      "Name": "Generate method 'MyMethod'",
      "CodeActionKind": "Refactor"
    }
  ]
}
```

**代码操作类型**:
- `QuickFix`: 快速修复
- `Refactor`: 重构
- `RefactorInline`: 内联重构
- `RefactorExtract`: 提取重构

### 9. 运行代码操作 (Run Code Action)

**端点**: `/v2/runcodeaction`

**请求模型**: `RunCodeActionRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "CodeAction": "using System;",
  "WantsTextChanges": true,
  "SelectionStartLine": 10,
  "SelectionStartColumn": 15,
  "SelectionEndLine": 10,
  "SelectionEndColumn": 20
}
```

**响应模型**: 包含代码更改

```json
{
  "Changes": [
    {
      "StartLine": 1,
      "StartColumn": 1,
      "EndLine": 1,
      "EndColumn": 1,
      "NewText": "using System;\n\n"
    }
  ]
}
```

### 10. 重命名 (Rename)

**端点**: `/rename`

**请求模型**: `RenameRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "RenameTo": "NewName",
  "ApplyTextChanges": true
}
```

**响应模型**: `RenameResponse`

```json
{
  "Changes": [
    {
      "FileName": "path/to/file.cs",
      "StartLine": 10,
      "StartColumn": 15,
      "EndLine": 10,
      "EndColumn": 20,
      "NewText": "NewName"
    }
  ]
}
```

### 11. 查找引用 (Find References)

**端点**: `/findusages`

**请求模型**: `FindUsagesRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "OnlyThisFile": false,
  "ExcludeDefinition": false
}
```

**响应模型**: `QuickFixResponse`

```json
{
  "QuickFixes": [
    {
      "Text": "MySymbol",
      "FileName": "path/to/file1.cs",
      "Line": 10,
      "Column": 15,
      "EndLine": 10,
      "EndColumn": 23,
      "Projects": ["Project1"]
    },
    {
      "Text": "MySymbol",
      "FileName": "path/to/file2.cs",
      "Line": 20,
      "Column": 5,
      "EndLine": 20,
      "EndColumn": 13,
      "Projects": ["Project1"]
    }
  ]
}
```

### 12. 项目信息 (Project Information)

**端点**: `/project`

**请求模型**: `ProjectInformationRequest`

```json
{
  "FileName": "path/to/file.cs"
}
```

**响应模型**: `ProjectInformationResponse`

```json
{
  "Project": {
    "Path": "path/to/project.csproj",
    "Configurations": ["Debug", "Release"],
    "Frameworks": ["net6.0", "net472"]
  }
}
```

### 13. 工作区信息 (Workspace Information)

**端点**: `/workspace`

**请求模型**: `WorkspaceInformationRequest`

```json
{}
```

**响应模型**: `WorkspaceInformationResponse`

```json
{
  "Projects": [
    {
      "Path": "path/to/project1.csproj",
      "Configurations": ["Debug", "Release"],
      "Frameworks": ["net6.0"]
    },
    {
      "Path": "path/to/project2.csproj",
      "Configurations": ["Debug", "Release"],
      "Frameworks": ["net6.0", "net472"]
    }
  ]
}
```

### 14. 签名帮助 (Signature Help)

**端点**: `/signatureHelp`

**请求模型**: `SignatureHelpRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容"
}
```

**响应模型**: `SignatureHelpResponse`

```json
{
  "Signatures": [
    {
      "Label": "void Write(string value)",
      "Documentation": "Writes the specified string value to the standard output stream.",
      "Parameters": [
        {
          "Label": "string value",
          "Documentation": "The value to write."
        }
      ],
      "ActiveParameter": 0
    }
  ],
  "ActiveSignature": 0
}
```

### 15. 类型查找 (Type Lookup)

**端点**: `/typelookup`

**请求模型**: `TypeLookupRequest`

```json
{
  "FileName": "path/to/file.cs",
  "Line": 10,
  "Column": 15,
  "Buffer": "文件内容",
  "IncludeDocumentation": true
}
```

**响应模型**: `TypeLookupResponse`

```json
{
  "Type": "string",
  "Documentation": "Represents text as a sequence of UTF-16 code units.",
  "Symbol": {
    "Name": "string",
    "Kind": "Class",
    "Documentation": "Represents text as a sequence of UTF-16 code units."
  }
}
```

## 事件协议

OmniSharp 通过事件协议向客户端推送实时更新，如项目加载状态、诊断信息更新等。

### 事件格式

```json
{
  "Event": "event-name",
  "Body": {
    // 事件数据
  }
}
```

### 常见事件类型

1. **项目加载事件**

```json
{
  "Event": "projectAdded",
  "Body": {
    "FileName": "path/to/project.csproj"
  }
}
```

2. **错误事件**

```json
{
  "Event": "error",
  "Body": "Error message"
}
```

3. **日志事件**

```json
{
  "Event": "log",
  "Body": {
    "LogLevel": "Information",
    "Name": "OmniSharp.Stdio.Host",
    "Message": "Starting OmniSharp"
  }
}
```
