# OmniSharp 项目符号索引和缓存功能实现方案

## 1. 概述

本文档描述了 OmniSharp for IntelliJ 平台中项目符号索引和缓存功能的详细实现方案。符号索引是代码智能感知的核心功能，它允许快速查找和检索项目中的类型、方法、属性等符号信息，为代码补全、导航、重构等高级功能提供基础支持。

符号索引和缓存功能将包括以下核心内容：

- 项目符号的收集和解析
- 高效的符号索引结构
- 多级缓存策略
- 增量更新机制
- 符号搜索和过滤
- 与 IntelliJ 平台索引系统的集成

## 2. 架构设计

### 2.1 组件关系图

```
+-------------------+     +-------------------+     +-------------------+
| SymbolCollector   |---->| SymbolIndexer     |---->| SymbolCache       |
+-------------------+     +-------------------+     +-------------------+
        ^                           ^                        ^
        |                           |                        |
        v                           v                        v
+-------------------+     +-------------------+     +-------------------+
| ProjectFileParser |     | SymbolSearcher    |     | IncrementalUpdater|
+-------------------+     +-------------------+     +-------------------+
```

### 2.2 核心流程

1. **符号收集**：遍历项目文件，提取所有符号信息
2. **符号索引**：构建高效的索引数据结构
3. **符号缓存**：实现多级缓存策略
4. **符号搜索**：提供快速查询接口
5. **增量更新**：监听文件变化，只更新受影响的符号

## 3. 核心组件实现

### 3.1 SymbolCollector

`SymbolCollector` 负责从项目文件中收集所有符号信息。它将利用项目文件解析器获取源代码，并使用 C# 编译器 API 或 Roslyn 获取符号信息。

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.project.*
import com.omnisharp.intellij.parsing.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread

/**
 * 符号收集器 - 负责从项目文件中收集所有符号信息
 */
class OmniSharpSymbolCollector(
    private val projectManager: OmniSharpProjectManager,
    private val fileParser: OmniSharpFileParser,
    private val logger: OmniSharpLogger
) {
    
    /**
     * 收集解决方案中的所有符号
     */
    fun collectSymbols(solution: OmniSharpSolutionModel): CompletableFuture<SymbolCollectionResult> {
        val future = CompletableFuture<SymbolCollectionResult>()
        
        thread {
            try {
                logger.info("开始收集解决方案 ${solution.name} 中的符号...")
                
                val symbolMap = ConcurrentHashMap<String, List<OmniSharpSymbol>>()
                val errorFiles = ConcurrentLinkedQueue<String>()
                
                // 并行收集每个项目的符号
                solution.projects.parallelStream().forEach { project ->
                    try {
                        logger.info("收集项目 ${project.name} 中的符号...")
                        val projectSymbols = collectProjectSymbols(project)
                        symbolMap[project.name] = projectSymbols
                    } catch (e: Exception) {
                        logger.error("收集项目 ${project.name} 符号失败: ${e.message}")
                        errorFiles.add(project.name)
                    }
                }
                
                logger.info("符号收集完成，共收集 ${symbolMap.values.sumOf { it.size }} 个符号")
                
                future.complete(SymbolCollectionResult(
                    symbols = symbolMap,
                    errors = errorFiles.toList()
                ))
            } catch (e: Exception) {
                logger.error("符号收集过程中发生异常: ${e.message}")
                future.completeExceptionally(e)
            }
        }
        
        return future
    }
    
    /**
     * 收集单个项目中的所有符号
     */
    private fun collectProjectSymbols(project: OmniSharpProjectModel): List<OmniSharpSymbol> {
        val symbols = mutableListOf<OmniSharpSymbol>()
        
        // 获取项目中所有的源代码文件
        val sourceFiles = projectManager.getSourceFiles(project)
        
        logger.info("项目 ${project.name} 包含 ${sourceFiles.size} 个源代码文件")
        
        // 并行解析每个源文件
        sourceFiles.parallelStream().forEach { filePath ->
            try {
                val fileSymbols = parseFileSymbols(filePath)
                symbols.addAll(fileSymbols)
            } catch (e: Exception) {
                logger.warn("解析文件 $filePath 符号失败: ${e.message}")
            }
        }
        
        return symbols
    }
    
    /**
     * 解析单个文件中的符号
     */
    private fun parseFileSymbols(filePath: String): List<OmniSharpSymbol> {
        // 调用文件解析器解析源代码文件
        val parseResult = fileParser.parseFile(filePath)
        
        // 从解析结果中提取符号
        return extractSymbolsFromParseTree(parseResult)
    }
    
    /**
     * 从解析树中提取符号
     */
    private fun extractSymbolsFromParseTree(parseResult: OmniSharpParseResult): List<OmniSharpSymbol> {
        val symbols = mutableListOf<OmniSharpSymbol>()
        
        // 提取命名空间
        extractNamespaces(parseResult, symbols)
        
        // 提取类型
        extractTypes(parseResult, symbols)
        
        // 提取方法
        extractMethods(parseResult, symbols)
        
        // 提取属性和字段
        extractPropertiesAndFields(parseResult, symbols)
        
        return symbols
    }
    
    private fun extractNamespaces(parseResult: OmniSharpParseResult, symbols: MutableList<OmniSharpSymbol>) {
        // 实现命名空间提取逻辑
        // ...
    }
    
    private fun extractTypes(parseResult: OmniSharpParseResult, symbols: MutableList<OmniSharpSymbol>) {
        // 实现类型提取逻辑
        // ...
    }
    
    private fun extractMethods(parseResult: OmniSharpParseResult, symbols: MutableList<OmniSharpSymbol>) {
        // 实现方法提取逻辑
        // ...
    }
    
    private fun extractPropertiesAndFields(parseResult: OmniSharpParseResult, symbols: MutableList<OmniSharpSymbol>) {
        // 实现属性和字段提取逻辑
        // ...
    }
}

/**
 * 符号收集结果
 */
data class SymbolCollectionResult(
    val symbols: Map<String, List<OmniSharpSymbol>>,
    val errors: List<String>
)
```

### 3.2 SymbolIndexer

`SymbolIndexer` 负责将收集到的符号构建为高效的索引数据结构，以便快速查询。

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.util.*
import java.util.concurrent.*
import java.util.function.*
import kotlin.math.max

/**
 * 符号索引器 - 负责构建符号索引数据结构
 */
class OmniSharpSymbolIndexer(
    private val logger: OmniSharpLogger
) {
    
    /**
     * 构建符号索引
     */
    fun buildIndex(symbolResult: SymbolCollectionResult): OmniSharpSymbolIndex {
        logger.info("开始构建符号索引...")
        
        val startTime = System.currentTimeMillis()
        
        // 创建索引结构
        val index = OmniSharpSymbolIndex()
        
        // 按名称索引
        buildNameIndex(symbolResult, index)
        
        // 按类型索引
        buildTypeIndex(symbolResult, index)
        
        // 按命名空间索引
        buildNamespaceIndex(symbolResult, index)
        
        // 构建继承关系图
        buildInheritanceGraph(symbolResult, index)
        
        val endTime = System.currentTimeMillis()
        logger.info("符号索引构建完成，耗时 ${endTime - startTime} 毫秒")
        
        return index
    }
    
    /**
     * 按名称构建索引
     */
    private fun buildNameIndex(symbolResult: SymbolCollectionResult, index: OmniSharpSymbolIndex) {
        val nameIndex = ConcurrentHashMap<String, ConcurrentLinkedQueue<OmniSharpSymbol>>()
        
        symbolResult.symbols.values.parallelStream().forEach { projectSymbols ->
            projectSymbols.forEach { symbol ->
                // 将符号添加到名称索引中
                nameIndex.computeIfAbsent(symbol.name) { ConcurrentLinkedQueue() }.add(symbol)
                
                // 如果符号有全限定名，也添加到全限定名索引
                if (symbol.fullyQualifiedName.isNotEmpty()) {
                    nameIndex.computeIfAbsent(symbol.fullyQualifiedName) { ConcurrentLinkedQueue() }.add(symbol)
                }
            }
        }
        
        index.nameIndex = nameIndex
    }
    
    /**
     * 按类型构建索引
     */
    private fun buildTypeIndex(symbolResult: SymbolCollectionResult, index: OmniSharpSymbolIndex) {
        val typeIndex = ConcurrentHashMap<OmniSharpSymbolKind, ConcurrentLinkedQueue<OmniSharpSymbol>>()
        
        symbolResult.symbols.values.parallelStream().forEach { projectSymbols ->
            projectSymbols.forEach { symbol ->
                typeIndex.computeIfAbsent(symbol.kind) { ConcurrentLinkedQueue() }.add(symbol)
            }
        }
        
        index.typeIndex = typeIndex
    }
    
    /**
     * 按命名空间构建索引
     */
    private fun buildNamespaceIndex(symbolResult: SymbolCollectionResult, index: OmniSharpSymbolIndex) {
        val namespaceIndex = ConcurrentHashMap<String, ConcurrentLinkedQueue<OmniSharpSymbol>>()
        
        symbolResult.symbols.values.parallelStream().forEach { projectSymbols ->
            projectSymbols.forEach { symbol ->
                namespaceIndex.computeIfAbsent(symbol.namespace) { ConcurrentLinkedQueue() }.add(symbol)
            }
        }
        
        index.namespaceIndex = namespaceIndex
    }
    
    /**
     * 构建继承关系图
     */
    private fun buildInheritanceGraph(symbolResult: SymbolCollectionResult, index: OmniSharpSymbolIndex) {
        val inheritanceGraph = OmniSharpInheritanceGraph()
        
        // 首先构建类型映射，便于快速查找
        val typeMap = mutableMapOf<String, OmniSharpSymbol>()
        
        symbolResult.symbols.values.forEach { projectSymbols ->
            projectSymbols.forEach { symbol ->
                if (symbol.kind == OmniSharpSymbolKind.CLASS || 
                    symbol.kind == OmniSharpSymbolKind.INTERFACE || 
                    symbol.kind == OmniSharpSymbolKind.ENUM ||
                    symbol.kind == OmniSharpSymbolKind.STRUCT) {
                    typeMap[symbol.fullyQualifiedName] = symbol
                }
            }
        }
        
        // 构建继承关系
        symbolResult.symbols.values.forEach { projectSymbols ->
            projectSymbols.forEach { symbol ->
                if (symbol is OmniSharpTypeSymbol) {
                    // 处理基类
                    if (symbol.baseClass.isNotEmpty() && typeMap.containsKey(symbol.baseClass)) {
                        val baseClass = typeMap[symbol.baseClass]!!
                        inheritanceGraph.addInheritance(symbol, baseClass)
                    }
                    
                    // 处理接口
                    symbol.interfaces.forEach { interfaceName ->
                        if (typeMap.containsKey(interfaceName)) {
                            val interfaceSymbol = typeMap[interfaceName]!!
                            inheritanceGraph.addImplementation(symbol, interfaceSymbol)
                        }
                    }
                }
            }
        }
        
        index.inheritanceGraph = inheritanceGraph
    }
    
    /**
     * 更新索引
     */
    fun updateIndex(existingIndex: OmniSharpSymbolIndex, 
                   changedSymbols: List<OmniSharpSymbol>, 
                   removedSymbols: List<OmniSharpSymbol>): OmniSharpSymbolIndex {
        logger.info("更新符号索引：添加 ${changedSymbols.size} 个符号，移除 ${removedSymbols.size} 个符号")
        
        // 创建索引副本
        val newIndex = existingIndex.copy()
        
        // 移除已删除的符号
        removedSymbols.forEach {
            removeSymbolFromIndex(it, newIndex)
        }
        
        // 添加新的或修改的符号
        changedSymbols.forEach {
            addSymbolToIndex(it, newIndex)
        }
        
        // 重建继承关系图（如果有类型符号变化）
        val hasTypeChanges = changedSymbols.any { it.kind == OmniSharpSymbolKind.CLASS || 
                                             it.kind == OmniSharpSymbolKind.INTERFACE ||
                                             it.kind == OmniSharpSymbolKind.ENUM ||
                                             it.kind == OmniSharpSymbolKind.STRUCT }
        
        val hasTypeRemovals = removedSymbols.any { it.kind == OmniSharpSymbolKind.CLASS || 
                                               it.kind == OmniSharpSymbolKind.INTERFACE ||
                                               it.kind == OmniSharpSymbolKind.ENUM ||
                                               it.kind == OmniSharpSymbolKind.STRUCT }
        
        if (hasTypeChanges || hasTypeRemovals) {
            // 重建继承关系图
            rebuildInheritanceGraph(newIndex)
        }
        
        return newIndex
    }
    
    private fun removeSymbolFromIndex(symbol: OmniSharpSymbol, index: OmniSharpSymbolIndex) {
        // 从名称索引中移除
        index.nameIndex[symbol.name]?.remove(symbol)
        if (symbol.fullyQualifiedName.isNotEmpty()) {
            index.nameIndex[symbol.fullyQualifiedName]?.remove(symbol)
        }
        
        // 从类型索引中移除
        index.typeIndex[symbol.kind]?.remove(symbol)
        
        // 从命名空间索引中移除
        index.namespaceIndex[symbol.namespace]?.remove(symbol)
    }
    
    private fun addSymbolToIndex(symbol: OmniSharpSymbol, index: OmniSharpSymbolIndex) {
        // 先移除可能存在的旧版本
        removeSymbolFromIndex(symbol, index)
        
        // 添加到名称索引
        index.nameIndex.computeIfAbsent(symbol.name) { ConcurrentLinkedQueue() }.add(symbol)
        if (symbol.fullyQualifiedName.isNotEmpty()) {
            index.nameIndex.computeIfAbsent(symbol.fullyQualifiedName) { ConcurrentLinkedQueue() }.add(symbol)
        }
        
        // 添加到类型索引
        index.typeIndex.computeIfAbsent(symbol.kind) { ConcurrentLinkedQueue() }.add(symbol)
        
        // 添加到命名空间索引
        index.namespaceIndex.computeIfAbsent(symbol.namespace) { ConcurrentLinkedQueue() }.add(symbol)
    }
    
    private fun rebuildInheritanceGraph(index: OmniSharpSymbolIndex) {
        // 重新构建继承关系图
        val inheritanceGraph = OmniSharpInheritanceGraph()
        
        // 收集所有类型符号
        val typeSymbols = mutableListOf<OmniSharpSymbol>()
        index.typeIndex[OmniSharpSymbolKind.CLASS]?.let { typeSymbols.addAll(it) }
        index.typeIndex[OmniSharpSymbolKind.INTERFACE]?.let { typeSymbols.addAll(it) }
        index.typeIndex[OmniSharpSymbolKind.ENUM]?.let { typeSymbols.addAll(it) }
        index.typeIndex[OmniSharpSymbolKind.STRUCT]?.let { typeSymbols.addAll(it) }
        
        // 构建类型映射
        val typeMap = mutableMapOf<String, OmniSharpSymbol>()
        typeSymbols.forEach {
            typeMap[it.fullyQualifiedName] = it
        }
        
        // 构建继承关系
        typeSymbols.forEach { symbol ->
            if (symbol is OmniSharpTypeSymbol) {
                // 处理基类
                if (symbol.baseClass.isNotEmpty() && typeMap.containsKey(symbol.baseClass)) {
                    val baseClass = typeMap[symbol.baseClass]!!
                    inheritanceGraph.addInheritance(symbol, baseClass)
                }
                
                // 处理接口
                symbol.interfaces.forEach { interfaceName ->
                    if (typeMap.containsKey(interfaceName)) {
                        val interfaceSymbol = typeMap[interfaceName]!!
                        inheritanceGraph.addImplementation(symbol, interfaceSymbol)
                    }
                }
            }
        }
        
        index.inheritanceGraph = inheritanceGraph
    }
}

/**
 * 符号索引
 */
data class OmniSharpSymbolIndex(
    var nameIndex: MutableMap<String, ConcurrentLinkedQueue<OmniSharpSymbol>> = ConcurrentHashMap(),
    var typeIndex: MutableMap<OmniSharpSymbolKind, ConcurrentLinkedQueue<OmniSharpSymbol>> = ConcurrentHashMap(),
    var namespaceIndex: MutableMap<String, ConcurrentLinkedQueue<OmniSharpSymbol>> = ConcurrentHashMap(),
    var inheritanceGraph: OmniSharpInheritanceGraph = OmniSharpInheritanceGraph()
) {
    /**
     * 创建索引副本
     */
    fun copy(): OmniSharpSymbolIndex {
        val newIndex = OmniSharpSymbolIndex()
        
        // 复制名称索引
        nameIndex.forEach { (key, symbols) ->
            newIndex.nameIndex[key] = ConcurrentLinkedQueue(symbols)
        }
        
        // 复制类型索引
        typeIndex.forEach { (key, symbols) ->
            newIndex.typeIndex[key] = ConcurrentLinkedQueue(symbols)
        }
        
        // 复制命名空间索引
        namespaceIndex.forEach { (key, symbols) ->
            newIndex.namespaceIndex[key] = ConcurrentLinkedQueue(symbols)
        }
        
        // 复制继承关系图
        newIndex.inheritanceGraph = inheritanceGraph.copy()
        
        return newIndex
    }
}

/**
 * 继承关系图
 */
class OmniSharpInheritanceGraph {
    private val inheritanceMap = ConcurrentHashMap<OmniSharpSymbol, MutableList<OmniSharpSymbol>>()
    private val implementationMap = ConcurrentHashMap<OmniSharpSymbol, MutableList<OmniSharpSymbol>>()
    
    /**
     * 添加继承关系
     */
    fun addInheritance(derivedType: OmniSharpSymbol, baseType: OmniSharpSymbol) {
        inheritanceMap.computeIfAbsent(derivedType) { mutableListOf() }.add(baseType)
    }
    
    /**
     * 添加接口实现关系
     */
    fun addImplementation(type: OmniSharpSymbol, interfaceType: OmniSharpSymbol) {
        implementationMap.computeIfAbsent(type) { mutableListOf() }.add(interfaceType)
    }
    
    /**
     * 获取基类
     */
    fun getBaseClasses(type: OmniSharpSymbol): List<OmniSharpSymbol> {
        return inheritanceMap[type] ?: emptyList()
    }
    
    /**
     * 获取实现的接口
     */
    fun getImplementedInterfaces(type: OmniSharpSymbol): List<OmniSharpSymbol> {
        return implementationMap[type] ?: emptyList()
    }
    
    /**
     * 创建继承关系图副本
     */
    fun copy(): OmniSharpInheritanceGraph {
        val newGraph = OmniSharpInheritanceGraph()
        
        inheritanceMap.forEach { (derived, bases) ->
            bases.forEach { base ->
                newGraph.addInheritance(derived, base)
            }
        }
        
        implementationMap.forEach { (type, interfaces) ->
            interfaces.forEach { interfaceType ->
                newGraph.addImplementation(type, interfaceType)
            }
        }
        
        return newGraph
    }
}
```

### 3.3 SymbolCache

`SymbolCache` 负责符号索引的缓存管理，实现多级缓存策略，提高符号查询性能。

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.util.*
import java.io.*
import java.util.concurrent.*
import java.util.zip.*

/**
 * 符号缓存 - 负责管理符号索引的缓存
 */
class OmniSharpSymbolCache(
    private val cacheDir: File,
    private val logger: OmniSharpLogger
) {
    // 内存缓存
    private val memoryCache = ConcurrentHashMap<String, CachedIndex>()
    
    // 缓存文件扩展名
    private val CACHE_FILE_EXTENSION = ".idx"
    
    // 缓存元数据扩展名
    private val META_FILE_EXTENSION = ".meta"
    
    init {
        // 确保缓存目录存在
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    /**
     * 保存索引到缓存
     */
    fun saveIndex(solutionId: String, index: OmniSharpSymbolIndex, lastModified: Long): Boolean {
        try {
            logger.info("保存解决方案 $solutionId 的符号索引到缓存...")
            
            // 更新内存缓存
            memoryCache[solutionId] = CachedIndex(index, lastModified)
            
            // 异步保存到磁盘
            CompletableFuture.runAsync {
                saveIndexToDisk(solutionId, index, lastModified)
            }
            
            return true
        } catch (e: Exception) {
            logger.error("保存符号索引失败: ${e.message}")
            return false
        }
    }
    
    /**
     * 从缓存加载索引
     */
    fun loadIndex(solutionId: String, lastModified: Long): OmniSharpSymbolIndex? {
        try {
            // 先检查内存缓存
            val cachedIndex = memoryCache[solutionId]
            if (cachedIndex != null && cachedIndex.lastModified == lastModified) {
                logger.info("从内存缓存加载解决方案 $solutionId 的符号索引")
                return cachedIndex.index
            }
            
            // 再检查磁盘缓存
            return loadIndexFromDisk(solutionId, lastModified)
        } catch (e: Exception) {
            logger.error("加载符号索引失败: ${e.message}")
            return null
        }
    }
    
    /**
     * 清除解决方案的缓存
     */
    fun clearSolutionCache(solutionId: String) {
        // 清除内存缓存
        memoryCache.remove(solutionId)
        
        // 清除磁盘缓存
        val cacheFile = getCacheFile(solutionId)
        val metaFile = getMetaFile(solutionId)
        
        if (cacheFile.exists()) {
            cacheFile.delete()
        }
        
        if (metaFile.exists()) {
            metaFile.delete()
        }
        
        logger.info("已清除解决方案 $solutionId 的符号索引缓存")
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAllCache() {
        // 清除内存缓存
        memoryCache.clear()
        
        // 清除磁盘缓存
        val cacheFiles = cacheDir.listFiles { file -> 
            file.extension == CACHE_FILE_EXTENSION.substring(1) || 
            file.extension == META_FILE_EXTENSION.substring(1)
        }
        
        cacheFiles?.forEach { it.delete() }
        
        logger.info("已清除所有符号索引缓存")
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val memoryEntries = memoryCache.size
        val diskEntries = cacheDir.listFiles { file -> 
            file.extension == CACHE_FILE_EXTENSION.substring(1)
        }?.size ?: 0
        
        val memorySize = memoryCache.values.sumOf { estimateMemoryUsage(it.index) }
        val diskSize = cacheDir.listFiles()?.sumOf { it.length() } ?: 0
        
        return CacheStats(
            memoryEntries = memoryEntries,
            diskEntries = diskEntries,
            memorySize = memorySize,
            diskSize = diskSize
        )
    }
    
    /**
     * 将索引保存到磁盘
     */
    private fun saveIndexToDisk(solutionId: String, index: OmniSharpSymbolIndex, lastModified: Long) {
        try {
            val cacheFile = getCacheFile(solutionId)
            val metaFile = getMetaFile(solutionId)
            
            // 保存索引数据
            DataOutputStream(GZIPOutputStream(FileOutputStream(cacheFile))).use { out ->
                // 序列化索引
                serializeIndex(out, index)
            }
            
            // 保存元数据
            DataOutputStream(FileOutputStream(metaFile)).use { out ->
                out.writeLong(lastModified)
            }
            
            logger.info("已将解决方案 $solutionId 的符号索引保存到磁盘")
        } catch (e: Exception) {
            logger.error("将符号索引保存到磁盘失败: ${e.message}")
        }
    }
    
    /**
     * 从磁盘加载索引
     */
    private fun loadIndexFromDisk(solutionId: String, lastModified: Long): OmniSharpSymbolIndex? {
        try {
            val cacheFile = getCacheFile(solutionId)
            val metaFile = getMetaFile(solutionId)
            
            // 检查文件是否存在
            if (!cacheFile.exists() || !metaFile.exists()) {
                return null
            }
            
            // 检查最后修改时间
            DataInputStream(FileInputStream(metaFile)).use { metaIn ->
                val cachedModified = metaIn.readLong()
                if (cachedModified != lastModified) {
                    return null
                }
            }
            
            // 加载索引
            val index = DataInputStream(GZIPInputStream(FileInputStream(cacheFile))).use { inStream ->
                deserializeIndex(inStream)
            }
            
            // 更新内存缓存
            memoryCache[solutionId] = CachedIndex(index, lastModified)
            
            logger.info("从磁盘缓存加载解决方案 $solutionId 的符号索引")
            
            return index
        } catch (e: Exception) {
            logger.error("从磁盘加载符号索引失败: ${e.message}")
            // 删除损坏的缓存文件
            getCacheFile(solutionId).delete()
            getMetaFile(solutionId).delete()
            return null
        }
    }
    
    /**
     * 获取缓存文件
     */
    private fun getCacheFile(solutionId: String): File {
        return File(cacheDir, "${getSafeFileName(solutionId)}$CACHE_FILE_EXTENSION")
    }
    
    /**
     * 获取元数据文件
     */
    private fun getMetaFile(solutionId: String): File {
        return File(cacheDir, "${getSafeFileName(solutionId)}$META_FILE_EXTENSION")
    }
    
    /**
     * 获取安全的文件名
     */
    private fun getSafeFileName(id: String): String {
        return id.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
    
    /**
     * 序列化索引
     */
    private fun serializeIndex(out: DataOutputStream, index: OmniSharpSymbolIndex) {
        // 此处实现索引的序列化逻辑
        // ...
    }
    
    /**
     * 反序列化索引
     */
    private fun deserializeIndex(inStream: DataInputStream): OmniSharpSymbolIndex {
        // 此处实现索引的反序列化逻辑
        // ...
        return OmniSharpSymbolIndex()
    }
    
    /**
     * 估计内存使用量
     */
    private fun estimateMemoryUsage(index: OmniSharpSymbolIndex): Long {
        // 此处实现内存使用量估算逻辑
        // ...
        return 0L
    }
}

/**
 * 缓存索引
 */
data class CachedIndex(
    val index: OmniSharpSymbolIndex,
    val lastModified: Long
)

/**
 * 缓存统计信息
 */
data class CacheStats(
    val memoryEntries: Int,
    val diskEntries: Int,
    val memorySize: Long,
    val diskSize: Long
)
```

### 3.4 SymbolSearcher

`SymbolSearcher` 提供符号搜索和查询接口，支持各种搜索条件和过滤选项。

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.util.*
import java.util.concurrent.*
import java.util.function.*

/**
 * 符号搜索器 - 提供符号搜索和查询接口
 */
class OmniSharpSymbolSearcher(
    private val logger: OmniSharpLogger
) {
    
    /**
     * 按名称搜索符号
     */
    fun searchSymbolsByName(index: OmniSharpSymbolIndex, 
                          name: String, 
                          caseSensitive: Boolean = false, 
                          exactMatch: Boolean = false): List<OmniSharpSymbol> {
        val results = mutableListOf<OmniSharpSymbol>()
        
        // 精确匹配
        if (exactMatch) {
            val matchingSymbols = index.nameIndex[name]
            if (matchingSymbols != null) {
                results.addAll(matchingSymbols)
            }
            
            // 如果不区分大小写，还需要搜索所有可能的大小写变体
            if (!caseSensitive && results.isEmpty()) {
                index.nameIndex.forEach { (key, symbols) ->
                    if (key.equals(name, ignoreCase = true)) {
                        results.addAll(symbols)
                    }
                }
            }
        } else {
            // 模糊匹配
            val searchName = if (caseSensitive) name else name.toLowerCase()
            
            index.nameIndex.forEach { (key, symbols) ->
                val compareKey = if (caseSensitive) key else key.toLowerCase()
                if (compareKey.contains(searchName)) {
                    results.addAll(symbols)
                }
            }
        }
        
        return results
    }
    
    /**
     * 按类型搜索符号
     */
    fun searchSymbolsByType(index: OmniSharpSymbolIndex, 
                          type: OmniSharpSymbolKind): List<OmniSharpSymbol> {
        return index.typeIndex[type]?.toList() ?: emptyList()
    }
    
    /**
     * 按命名空间搜索符号
     */
    fun searchSymbolsByNamespace(index: OmniSharpSymbolIndex, 
                               namespace: String, 
                               includeNested: Boolean = true): List<OmniSharpSymbol> {
        val results = mutableListOf<OmniSharpSymbol>()
        
        if (includeNested) {
            // 包含嵌套命名空间
            index.namespaceIndex.forEach { (ns, symbols) ->
                if (ns == namespace || ns.startsWith("$namespace.")) {
                    results.addAll(symbols)
                }
            }
        } else {
            // 只搜索指定命名空间
            val matchingSymbols = index.namespaceIndex[namespace]
            if (matchingSymbols != null) {
                results.addAll(matchingSymbols)
            }
        }
        
        return results
    }
    
    /**
     * 高级搜索
     */
    fun advancedSearch(index: OmniSharpSymbolIndex, 
                     query: SearchQuery): List<OmniSharpSymbol> {
        // 首先应用基本过滤条件
        var results = filterSymbols(index, query)
        
        // 排序
        results = sortResults(results, query)
        
        // 限制数量
        if (query.limit > 0) {
            results = results.take(query.limit)
        }
        
        return results
    }
    
    /**
     * 过滤符号
     */
    private fun filterSymbols(index: OmniSharpSymbolIndex, query: SearchQuery): List<OmniSharpSymbol> {
        val results = mutableListOf<OmniSharpSymbol>()
        
        // 如果有名称查询，先按名称过滤
        if (query.nameQuery.isNotEmpty()) {
            results.addAll(searchSymbolsByName(
                index, 
                query.nameQuery, 
                query.caseSensitive, 
                query.exactMatch
            ))
        } else {
            // 否则收集所有符号
            index.nameIndex.forEach { (_, symbols) ->
                results.addAll(symbols)
            }
        }
        
        // 按类型过滤
        if (query.symbolKinds.isNotEmpty()) {
            results.retainAll { query.symbolKinds.contains(it.kind) }
        }
        
        // 按命名空间过滤
        if (query.namespaceQuery.isNotEmpty()) {
            results.retainAll {
                if (query.includeNestedNamespaces) {
                    it.namespace == query.namespaceQuery || it.namespace.startsWith("${query.namespaceQuery}.")
                } else {
                    it.namespace == query.namespaceQuery
                }
            }
        }
        
        // 应用自定义过滤器
        query.filters.forEach {\ filter ->
            results.retainAll { filter.test(it) }
        }
        
        return results.distinct()
    }
    
    /**
     * 排序结果
     */
    private fun sortResults(symbols: List<OmniSharpSymbol>, query: SearchQuery): List<OmniSharpSymbol> {
        return if (query.sortBy != null) {
            symbols.sortedWith(query.sortBy)
        } else {
            // 默认排序：按名称字母顺序
            symbols.sortedBy { it.name }
        }
    }
    
    /**
     * 查找类型的所有子类
     */
    fun findSubclasses(index: OmniSharpSymbolIndex, baseType: OmniSharpSymbol): List<OmniSharpSymbol> {
        val subclasses = mutableListOf<OmniSharpSymbol>()
        
        // 查找所有直接子类
        index.inheritanceGraph.inheritanceMap.forEach { (derived, bases) ->
            if (bases.any { it.fullyQualifiedName == baseType.fullyQualifiedName }) {
                subclasses.add(derived)
            }
        }
        
        return subclasses
    }
    
    /**
     * 查找类型的所有实现类
     */
    fun findImplementations(index: OmniSharpSymbolIndex, interfaceType: OmniSharpSymbol): List<OmniSharpSymbol> {
        val implementations = mutableListOf<OmniSharpSymbol>()
        
        // 查找所有实现类
        index.inheritanceGraph.implementationMap.forEach { (type, interfaces) ->
            if (interfaces.any { it.fullyQualifiedName == interfaceType.fullyQualifiedName }) {
                implementations.add(type)
            }
        }
        
        return implementations
    }
    
    /**
     * 查找符号的引用
     */
    fun findReferences(index: OmniSharpSymbolIndex, symbol: OmniSharpSymbol): List<OmniSharpSymbolReference> {
        // 此处实现符号引用查找逻辑
        // ...
        return emptyList()
    }
}

/**
 * 搜索查询
 */
data class SearchQuery(
    val nameQuery: String = "",
    val symbolKinds: Set<OmniSharpSymbolKind> = emptySet(),
    val namespaceQuery: String = "",
    val includeNestedNamespaces: Boolean = true,
    val caseSensitive: Boolean = false,
    val exactMatch: Boolean = false,
    val filters: List<Predicate<OmniSharpSymbol>> = emptyList(),
    val sortBy: Comparator<OmniSharpSymbol>? = null,
    val limit: Int = 0
)

/**
 * 符号引用
 */
data class OmniSharpSymbolReference(
    val referencedSymbol: OmniSharpSymbol,
    val filePath: String,
    val offset: Int,
    val length: Int,
    val referenceType: ReferenceType
)

/**
 * 引用类型
 */
enum class ReferenceType {
    CALL,
    DECLARATION,
    USAGE,
    INHERITANCE,
    IMPLEMENTATION
}
```

### 3.5 IncrementalUpdater

`IncrementalUpdater` 负责监听文件变化，实现增量更新机制，只重新索引发生变化的文件。

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.project.*
import com.omnisharp.intellij.parsing.*
import com.omnisharp.intellij.util.*
import java.io.*
import java.util.concurrent.*
import java.util.concurrent.atomic.*

/**
 * 增量更新器 - 负责监听文件变化，实现增量更新
 */
class OmniSharpIncrementalSymbolUpdater(
    private val projectManager: OmniSharpProjectManager,
    private val symbolCollector: OmniSharpSymbolCollector,
    private val symbolIndexer: OmniSharpSymbolIndexer,
    private val symbolCache: OmniSharpSymbolCache,
    private val logger: OmniSharpLogger
) : OmniSharpFileChangeListener {
    
    // 更新线程池
    private val updateExecutor = Executors.newSingleThreadExecutor {
        Thread(it, "OmniSharp-Symbol-Updater").apply { isDaemon = true }
    }
    
    // 任务队列
    private val updateQueue = ConcurrentLinkedQueue<UpdateTask>()
    
    // 是否正在处理更新
    private val isProcessing = AtomicBoolean(false)
    
    /**
     * 注册文件变更监听
     */
    fun registerListener() {
        projectManager.addFileChangeListener(this)
        logger.info("符号索引增量更新器已注册")
    }
    
    /**
     * 注销文件变更监听
     */
    fun unregisterListener() {
        projectManager.removeFileChangeListener(this)
        updateExecutor.shutdown()
        logger.info("符号索引增量更新器已注销")
    }
    
    /**
     * 文件创建事件处理
     */
    override fun onFileCreated(file: File, project: OmniSharpProjectModel) {
        if (isSourceFile(file)) {
            scheduleUpdate(UpdateTask(UpdateType.CREATE, file, project))
        }
    }
    
    /**
     * 文件修改事件处理
     */
    override fun onFileModified(file: File, project: OmniSharpProjectModel) {
        if (isSourceFile(file)) {
            scheduleUpdate(UpdateTask(UpdateType.MODIFY, file, project))
        }
    }
    
    /**
     * 文件删除事件处理
     */
    override fun onFileDeleted(file: File, project: OmniSharpProjectModel) {
        if (isSourceFile(file)) {
            scheduleUpdate(UpdateTask(UpdateType.DELETE, file, project))
        }
    }
    
    /**
     * 安排更新任务
     */
    private fun scheduleUpdate(task: UpdateTask) {
        updateQueue.add(task)
        processQueueIfNeeded()
    }
    
    /**
     * 处理任务队列
     */
    private fun processQueueIfNeeded() {
        if (isProcessing.compareAndSet(false, true)) {
            updateExecutor.submit { processUpdates() }
        }
    }
    
    /**
     * 处理更新
     */
    private fun processUpdates() {
        try {
            while (true) {
                val task = updateQueue.poll() ?: break
                
                try {
                    processUpdateTask(task)
                } catch (e: Exception) {
                    logger.error("处理更新任务失败: ${e.message}", e)
                }
            }
        } finally {
            isProcessing.set(false)
            
            // 检查是否还有待处理的任务
            if (!updateQueue.isEmpty()) {
                processQueueIfNeeded()
            }
        }
    }
    
    /**
     * 处理单个更新任务
     */
    private fun processUpdateTask(task: UpdateTask) {
        logger.info("处理${task.type}文件 ${task.file.path} 的符号索引更新")
        
        val solution = projectManager.getSolutionForProject(task.project)
        if (solution == null) {
            logger.warn("无法获取项目 ${task.project.name} 所属的解决方案")
            return
        }
        
        val solutionId = solution.path
        val currentIndex = symbolCache.loadIndex(solutionId, solution.lastModified)
        
        if (currentIndex == null) {
            // 如果没有缓存的索引，执行完整的重建
            logger.info("没有找到缓存的索引，执行完整重建")
            rebuildFullIndex(solution)
            return
        }
        
        when (task.type) {
            UpdateType.CREATE -> handleFileCreate(task.file, task.project, currentIndex, solution)
            UpdateType.MODIFY -> handleFileModify(task.file, task.project, currentIndex, solution)
            UpdateType.DELETE -> handleFileDelete(task.file, task.project, currentIndex, solution)
        }
    }
    
    /**
     * 处理文件创建
     */
    private fun handleFileCreate(file: File, 
                              project: OmniSharpProjectModel, 
                              currentIndex: OmniSharpSymbolIndex, 
                              solution: OmniSharpSolutionModel) {
        try {
            // 解析新文件的符号
            val fileParser = projectManager.getFileParser()
            val parseResult = fileParser.parseFile(file.path)
            
            // 提取符号
            val newSymbols = symbolCollector.extractSymbolsFromParseTree(parseResult)
            
            // 更新索引
            val updatedIndex = symbolIndexer.updateIndex(currentIndex, newSymbols, emptyList())
            
            // 保存更新后的索引
            symbolCache.saveIndex(solution.path, updatedIndex, solution.lastModified)
            
            logger.info("成功更新文件 ${file.path} 的符号索引，新增 ${newSymbols.size} 个符号")
        } catch (e: Exception) {
            logger.error("更新新建文件符号索引失败: ${e.message}", e)
        }
    }
    
    /**
     * 处理文件修改
     */
    private fun handleFileModify(file: File, 
                              project: OmniSharpProjectModel, 
                              currentIndex: OmniSharpSymbolIndex, 
                              solution: OmniSharpSolutionModel) {
        try {
            // 查找旧符号
            val oldSymbols = findSymbolsForFile(currentIndex, file.path)
            
            // 解析修改后的文件
            val fileParser = projectManager.getFileParser()
            val parseResult = fileParser.parseFile(file.path)
            
            // 提取新符号
            val newSymbols = symbolCollector.extractSymbolsFromParseTree(parseResult)
            
            // 更新索引
            val updatedIndex = symbolIndexer.updateIndex(currentIndex, newSymbols, oldSymbols)
            
            // 保存更新后的索引
            symbolCache.saveIndex(solution.path, updatedIndex, solution.lastModified)
            
            logger.info("成功更新文件 ${file.path} 的符号索引，移除 ${oldSymbols.size} 个旧符号，新增 ${newSymbols.size} 个符号")
        } catch (e: Exception) {
            logger.error("更新修改文件符号索引失败: ${e.message}", e)
        }
    }
    
    /**
     * 处理文件删除
     */
    private fun handleFileDelete(file: File, 
                              project: OmniSharpProjectModel, 
                              currentIndex: OmniSharpSymbolIndex, 
                              solution: OmniSharpSolutionModel) {
        try {
            // 查找要删除的符号
            val symbolsToRemove = findSymbolsForFile(currentIndex, file.path)
            
            // 更新索引
            val updatedIndex = symbolIndexer.updateIndex(currentIndex, emptyList(), symbolsToRemove)
            
            // 保存更新后的索引
            symbolCache.saveIndex(solution.path, updatedIndex, solution.lastModified)
            
            logger.info("成功从符号索引中移除文件 ${file.path} 的 ${symbolsToRemove.size} 个符号")
        } catch (e: Exception) {
            logger.error("更新删除文件符号索引失败: ${e.message}", e)
        }
    }
    
    /**
     * 重建完整索引
     */
    private fun rebuildFullIndex(solution: OmniSharpSolutionModel) {
        try {
            logger.info("重建解决方案 ${solution.name} 的完整符号索引")
            
            // 收集所有符号
            val symbolResult = symbolCollector.collectSymbols(solution).get()
            
            // 构建索引
            val index = symbolIndexer.buildIndex(symbolResult)
            
            // 保存索引
            symbolCache.saveIndex(solution.path, index, solution.lastModified)
            
            logger.info("完整符号索引重建成功")
        } catch (e: Exception) {
            logger.error("重建完整符号索引失败: ${e.message}", e)
        }
    }
    
    /**
     * 查找文件对应的符号
     */
    private fun findSymbolsForFile(index: OmniSharpSymbolIndex, filePath: String): List<OmniSharpSymbol> {
        val symbols = mutableListOf<OmniSharpSymbol>()
        
        // 遍历所有索引，查找属于指定文件的符号
        index.nameIndex.values.forEach { symbolList ->
            symbols.addAll(symbolList.filter { it.location.filePath == filePath })
        }
        
        return symbols
    }
    
    /**
     * 检查是否为源代码文件
     */
    private fun isSourceFile(file: File): Boolean {
        val extension = file.extension.toLowerCase()
        return extension in setOf("cs", "vb", "fs")
    }
}

/**
 * 更新任务
 */
data class UpdateTask(
    val type: UpdateType,
    val file: File,
    val project: OmniSharpProjectModel
)

/**
 * 更新类型
 */
enum class UpdateType {
    CREATE,
    MODIFY,
    DELETE
}

/**
 * 文件变更监听器
 */
interface OmniSharpFileChangeListener {
    fun onFileCreated(file: File, project: OmniSharpProjectModel)
    fun onFileModified(file: File, project: OmniSharpProjectModel)
    fun onFileDeleted(file: File, project: OmniSharpProjectModel)
}
```

## 4. 索引策略和算法

### 4.1 索引策略

为了提高符号查询效率，我们采用多维度索引策略：

1. **名称索引**：以符号名称为键，存储符号列表
2. **类型索引**：以符号类型为键，存储符号列表
3. **命名空间索引**：以命名空间为键，存储符号列表
4. **继承关系图**：存储类型之间的继承和实现关系

### 4.2 索引算法

#### 4.2.1 倒排索引

我们使用倒排索引来实现高效的符号搜索。倒排索引是一种以单词为键，以文档列表为值的索引结构，可以快速找到包含特定单词的所有文档。在我们的实现中，单词对应符号名称，文档对应符号对象。

#### 4.2.2 Trie 树（前缀树）

对于符号名称的前缀搜索，我们可以使用 Trie 树来优化查询性能。Trie 树是一种树形数据结构，适合用于存储和检索字符串数据集中的键。

```kotlin
/**
 * Trie 树节点
 */
class TrieNode {
    val children = mutableMapOf<Char, TrieNode>()
    val symbols = mutableListOf<OmniSharpSymbol>()
    var isEndOfWord = false
}

/**
 * Trie 树索引
 */
class TrieIndex {
    private val root = TrieNode()
    
    /**
     * 添加符号到 Trie 树
     */
    fun addSymbol(symbol: OmniSharpSymbol) {
        addWord(symbol.name, symbol)
        if (symbol.fullyQualifiedName.isNotEmpty()) {
            addWord(symbol.fullyQualifiedName, symbol)
        }
    }
    
    /**
     * 添加单词到 Trie 树
     */
    private fun addWord(word: String, symbol: OmniSharpSymbol) {
        var current = root
        
        for (char in word) {
            current = current.children.computeIfAbsent(char) { TrieNode() }
            current.symbols.add(symbol)
        }
        
        current.isEndOfWord = true
    }
    
    /**
     * 按前缀搜索符号
     */
    fun searchByPrefix(prefix: String): List<OmniSharpSymbol> {
        var current = root
        
        // 遍历前缀，找到对应的节点
        for (char in prefix) {
            current = current.children[char] ?: return emptyList()
        }
        
        return current.symbols
    }
}
```

## 5. 缓存机制

### 5.1 多级缓存策略

我们实现了多级缓存策略来提高性能：

1. **内存缓存**：最近使用的索引保存在内存中，提供最快的访问速度
2. **磁盘缓存**：索引序列化到磁盘，支持跨会话持久化

### 5.2 缓存失效策略

缓存失效策略包括：

1. **基于时间戳**：通过比较解决方案和项目文件的最后修改时间，判断是否需要重建索引
2. **基于文件变更**：监听文件系统变化，在文件创建、修改或删除时更新索引

## 6. 性能优化

### 6.1 并行处理

我们使用并行流和线程池来加速符号收集和索引构建过程，特别是对于大型解决方案：

```kotlin
// 使用并行流处理项目符号收集
solution.projects.parallelStream().forEach { project ->
    try {
        val projectSymbols = collectProjectSymbols(project)
        symbolMap[project.name] = projectSymbols
    } catch (e: Exception) {
        errorFiles.add(project.name)
    }
}
```

### 6.2 延迟加载

对于大型项目，可以采用延迟加载策略，只在需要时才加载特定区域的符号：

```kotlin
/**
 * 延迟加载器
 */
class OmniSharpSymbolLazyLoader(
    private val symbolCollector: OmniSharpSymbolCollector
) {
    private val loadedNamespaces = ConcurrentHashMap<String, Boolean>()
    private val namespaceSymbols = ConcurrentHashMap<String, List<OmniSharpSymbol>>()
    
    /**
     * 按需加载命名空间符号
     */
    fun loadNamespaceSymbols(solution: OmniSharpSolutionModel, namespace: String): List<OmniSharpSymbol> {
        if (!loadedNamespaces.contains(namespace)) {
            synchronized(this) {
                if (!loadedNamespaces.contains(namespace)) {
                    // 加载指定命名空间的符号
                    val symbols = symbolCollector.collectSymbolsInNamespace(solution, namespace)
                    namespaceSymbols[namespace] = symbols
                    loadedNamespaces[namespace] = true
                }
            }
        }
        
        return namespaceSymbols[namespace] ?: emptyList()
    }
}
```

### 6.3 索引压缩

对于磁盘缓存，我们使用 GZIP 压缩来减少存储空间：

```kotlin
// 保存索引时使用 GZIP 压缩
DataOutputStream(GZIPOutputStream(FileOutputStream(cacheFile))).use { out ->
    serializeIndex(out, index)
}
```

## 7. 与 IntelliJ 平台集成

### 7.1 与平台索引系统集成

OmniSharp 符号索引需要与 IntelliJ 平台的索引系统集成，以提供统一的符号查找体验：

```kotlin
package com.omnisharp.intellij.platform

import com.intellij.ide.util.gotoByName.*
import com.intellij.openapi.project.*
import com.intellij.psi.search.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.symbol.indexing.*

/**
 * OmniSharp 符号提供者 - 与 IntelliJ 平台 goto symbol 功能集成
 */
class OmniSharpSymbolProvider(private val project: Project) : ChooseByNameContributor {
    private val symbolSearcher: OmniSharpSymbolSearcher?
    private val symbolCache: OmniSharpSymbolCache?
    
    init {
        // 获取 OmniSharp 符号搜索器和缓存
        val component = project.getComponent(OmniSharpComponent::class.java)
        symbolSearcher = component?.symbolSearcher
        symbolCache = component?.symbolCache
    }
    
    override fun getNames(prefix: String, checkBoxState: Boolean, includeNonProjectItems: Boolean): Array<String> {
        // 获取当前活动的解决方案
        val component = project.getComponent(OmniSharpComponent::class.java)
        val activeSolution = component?.activeSolution ?: return emptyArray()
        
        // 加载索引
        val index = symbolCache?.loadIndex(activeSolution.path, activeSolution.lastModified) ?: return emptyArray()
        
        // 搜索符号
        val symbols = symbolSearcher?.searchSymbolsByName(index, prefix, false, false) ?: emptyList()
        
        // 返回符号名称
        return symbols.map { it.name }.toTypedArray()
    }
    
    override fun getItemsByName(name: String, pattern: String, project: Project, includeNonProjectItems: Boolean): Array<Any> {
        // 获取当前活动的解决方案
        val component = project.getComponent(OmniSharpComponent::class.java)
        val activeSolution = component?.activeSolution ?: return emptyArray()
        
        // 加载索引
        val index = symbolCache?.loadIndex(activeSolution.path, activeSolution.lastModified) ?: return emptyArray()
        
        // 搜索符号
        val symbols = symbolSearcher?.searchSymbolsByName(index, name, false, true) ?: emptyList()
        
        // 转换为平台可识别的项目
        return symbols.map { OmniSharpPlatformSymbolWrapper(it) }.toTypedArray()
    }
}

/**
 * OmniSharp 平台符号包装器
 */
class OmniSharpPlatformSymbolWrapper(private val symbol: OmniSharpSymbol) {
    fun getSymbol() = symbol
    
    override fun toString(): String {
        return symbol.name
    }
}
```

### 7.2 与项目视图集成

符号索引还需要与 IntelliJ 的项目视图集成，以显示项目的符号层次结构：

```kotlin
package com.omnisharp.intellij.platform

import com.intellij.ide.projectView.*
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.*
import com.intellij.openapi.project.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.symbol.indexing.*

/**
 * OmniSharp 符号节点 - 用于项目视图中显示符号
 */
class OmniSharpSymbolNode(
    project: Project,
    private val symbol: OmniSharpSymbol,
    viewSettings: ViewSettings
) : PsiAwareNode(project, null, viewSettings) {
    override fun getName(): String {
        return symbol.name
    }
    
    override fun getChildren(): Collection<TreeNode<*>> {
        // 获取子符号
        val component = project?.getComponent(OmniSharpComponent::class.java)
        val index = component?.symbolCache?.loadIndex(component.activeSolution?.path ?: "", 
                                                     component.activeSolution?.lastModified ?: 0)
        
        if (index == null) return emptyList()
        
        // 获取当前符号的子符号
        val children = mutableListOf<TreeNode<*>>()
        
        // 根据符号类型添加不同的子节点
        when (symbol) {
            is OmniSharpNamespaceSymbol -> {
                // 添加命名空间下的类型
                index.namespaceIndex[symbol.name]?.filter { 
                    it is OmniSharpTypeSymbol && it.parentName == symbol.name
                }?.forEach { 
                    children.add(OmniSharpSymbolNode(project!!, it, settings))
                }
            }
            is OmniSharpClassSymbol -> {
                // 添加类的成员
                index.typeIndex[OmniSharpSymbolKind.METHOD]?.filter { 
                    it.parentName == symbol.name
                }?.forEach { 
                    children.add(OmniSharpSymbolNode(project!!, it, settings))
                }
                
                index.typeIndex[OmniSharpSymbolKind.PROPERTY]?.filter { 
                    it.parentName == symbol.name
                }?.forEach { 
                    children.add(OmniSharpSymbolNode(project!!, it, settings))
                }
                
                // 添加嵌套类
                index.typeIndex[OmniSharpSymbolKind.CLASS]?.filter { 
                    it is OmniSharpClassSymbol && it.parentName == symbol.name
                }?.forEach { 
                    children.add(OmniSharpSymbolNode(project!!, it, settings))
                }
            }
        }
        
        return children
    }
    
    override fun isAlwaysLeaf(): Boolean {
        return when (symbol.kind) {
            OmniSharpSymbolKind.METHOD, 
            OmniSharpSymbolKind.PROPERTY, 
            OmniSharpSymbolKind.FIELD, 
            OmniSharpSymbolKind.ENUM_MEMBER -> true
            else -> false
        }
    }
    
    override fun getTypeSortWeight(): Int {
        return when (symbol.kind) {
            OmniSharpSymbolKind.NAMESPACE -> 10
            OmniSharpSymbolKind.CLASS -> 20
            OmniSharpSymbolKind.INTERFACE -> 30
            OmniSharpSymbolKind.ENUM -> 40
            OmniSharpSymbolKind.STRUCT -> 50
            OmniSharpSymbolKind.METHOD -> 60
            OmniSharpSymbolKind.PROPERTY -> 70
            OmniSharpSymbolKind.FIELD -> 80
            OmniSharpSymbolKind.ENUM_MEMBER -> 90
            else -> 100
        }
    }
}
```

## 8. 错误处理

符号索引和缓存功能需要健壮的错误处理机制，以确保在出现问题时能够优雅地降级而不是完全失败：

### 8.1 符号收集错误

在符号收集过程中，某些文件可能无法解析。我们会记录这些错误，并继续处理其他文件：

```kotlin
// 收集单个文件中的符号
private fun parseFileSymbols(filePath: String): List<OmniSharpSymbol> {
    try {
        val parseResult = fileParser.parseFile(filePath)
        return extractSymbolsFromParseTree(parseResult)
    } catch (e: Exception) {
        logger.warn("解析文件 $filePath 符号失败: ${e.message}")
        // 返回空列表，继续处理其他文件
        return emptyList()
    }
}
```

### 8.2 缓存错误

缓存读取失败时，我们会删除损坏的缓存文件并重新构建索引：

```kotlin
private fun loadIndexFromDisk(solutionId: String, lastModified: Long): OmniSharpSymbolIndex? {
    try {
        // ... 加载逻辑 ...
    } catch (e: Exception) {
        logger.error("从磁盘加载符号索引失败: ${e.message}")
        // 删除损坏的缓存文件
        getCacheFile(solutionId).delete()
        getMetaFile(solutionId).delete()
        return null
    }
}
```

### 8.3 增量更新错误

增量更新失败时，我们会回退到完整索引重建：

```kotlin
private fun processUpdateTask(task: UpdateTask) {
    try {
        // ... 更新逻辑 ...
    } catch (e: Exception) {
        logger.error("处理更新任务失败: ${e.message}", e)
        // 回退到完整重建
        val solution = projectManager.getSolutionForProject(task.project)
        solution?.let { rebuildFullIndex(it) }
    }
}
```

## 9. 使用示例

### 9.1 基本符号搜索

```kotlin
// 初始化符号管理器
val symbolManager = OmniSharpSymbolManager(projectManager, fileParser, logger)

// 获取解决方案
val solution = projectManager.loadSolution("c:\\Projects\\MySolution.sln")

// 构建索引
symbolManager.buildIndex(solution)

// 搜索符号
val searchResults = symbolManager.searchSymbols("MyClass", false)

// 处理搜索结果
searchResults.forEach { symbol ->
    println("找到符号: ${symbol.name} (${symbol.kind}) 在 ${symbol.location.filePath}:${symbol.location.line}")
}
```

### 9.2 高级搜索

```kotlin
// 创建高级搜索查询
val query = SearchQuery(
    nameQuery = "Get",
    symbolKinds = setOf(OmniSharpSymbolKind.METHOD),
    namespaceQuery = "MyProject.Services",
    includeNestedNamespaces = true,
    caseSensitive = false,
    exactMatch = false,
    filters = listOf {
        // 只包含公共方法
        it.modifiers.contains("public")
    },
    sortBy = Comparator { a, b -> 
        // 按名称排序
        a.name.compareTo(b.name)
    },
    limit = 50
)

// 执行高级搜索
val advancedResults = symbolManager.advancedSearch(query)
```

### 9.3 增量更新

```kotlin
// 初始化增量更新器
val incrementalUpdater = OmniSharpIncrementalSymbolUpdater(
    projectManager,
    symbolCollector,
    symbolIndexer,
    symbolCache,
    logger
)

// 注册文件变更监听
incrementalUpdater.registerListener()

// 当不再需要时注销监听器
// incrementalUpdater.unregisterListener()
```

## 10. 测试策略

符号索引和缓存功能需要全面的测试覆盖，包括单元测试、集成测试和性能测试：

### 10.1 单元测试

我们为每个核心组件编写单元测试，确保它们能够正确工作：

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import org.junit.*
import kotlin.test.*

class OmniSharpSymbolSearcherTest {
    private lateinit var searcher: OmniSharpSymbolSearcher
    private lateinit var index: OmniSharpSymbolIndex
    
    @Before
    fun setUp() {
        searcher = OmniSharpSymbolSearcher(OmniSharpLogger.getInstance())
        index = createTestIndex()
    }
    
    @Test
    fun testSearchSymbolsByName() {
        // 精确匹配
        val exactResults = searcher.searchSymbolsByName(index, "Person", false, true)
        assertEquals(1, exactResults.size)
        assertEquals("Person", exactResults[0].name)
        
        // 模糊匹配
        val fuzzyResults = searcher.searchSymbolsByName(index, "Pers", false, false)
        assertTrue(fuzzyResults.size >= 1)
    }
    
    @Test
    fun testSearchSymbolsByType() {
        val classResults = searcher.searchSymbolsByType(index, OmniSharpSymbolKind.CLASS)
        assertTrue(classResults.isNotEmpty())
        
        val methodResults = searcher.searchSymbolsByType(index, OmniSharpSymbolKind.METHOD)
        assertTrue(methodResults.isNotEmpty())
    }
    
    @Test
    fun testSearchSymbolsByNamespace() {
        val results = searcher.searchSymbolsByNamespace(index, "MyProject", true)
        assertTrue(results.isNotEmpty())
    }
    
    private fun createTestIndex(): OmniSharpSymbolIndex {
        // 创建测试索引和符号
        val index = OmniSharpSymbolIndex()
        
        // 创建类符号
        val personClass = OmniSharpClassSymbol(
            name = "Person",
            fullyQualifiedName = "MyProject.Models.Person",
            namespace = "MyProject.Models",
            kind = OmniSharpSymbolKind.CLASS,
            location = OmniSharpLocation("c:\\test\\Person.cs", 10, 0),
            modifiers = listOf("public"),
            baseClass = "",
            interfaces = listOf("MyProject.Models.IPerson"),
            parentName = ""
        )
        
        // 添加到索引
        index.nameIndex["Person"] = ConcurrentLinkedQueue(listOf(personClass))
        index.nameIndex["MyProject.Models.Person"] = ConcurrentLinkedQueue(listOf(personClass))
        index.typeIndex[OmniSharpSymbolKind.CLASS] = ConcurrentLinkedQueue(listOf(personClass))
        index.namespaceIndex["MyProject.Models"] = ConcurrentLinkedQueue(listOf(personClass))
        
        return index
    }
}
```

### 10.2 集成测试

我们还需要编写集成测试，测试多个组件协同工作的情况：

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.project.*
import org.junit.*
import kotlin.test.*

class OmniSharpSymbolManagerIntegrationTest {
    private lateinit var symbolManager: OmniSharpSymbolManager
    private lateinit var projectManager: OmniSharpProjectManager
    
    @Before
    fun setUp() {
        projectManager = TestProjectManager()
        symbolManager = OmniSharpSymbolManager(projectManager, TestFileParser(), OmniSharpLogger.getInstance())
    }
    
    @Test
    fun testBuildAndSearchIndex() {
        // 创建测试解决方案
        val solution = createTestSolution()
        
        // 构建索引
        symbolManager.buildIndex(solution)
        
        // 搜索符号
        val results = symbolManager.searchSymbols("Person", false)
        
        // 验证结果
        assertTrue(results.isNotEmpty())
        assertEquals("Person", results[0].name)
    }
    
    @Test
    fun testIncrementalUpdate() {
        // 创建测试解决方案
        val solution = createTestSolution()
        
        // 构建索引
        symbolManager.buildIndex(solution)
        
        // 模拟文件修改
        val projectManager = symbolManager.projectManager as TestProjectManager
        projectManager.simulateFileChange(solution.projects[0], "c:\\test\\Person.cs")
        
        // 验证增量更新是否生效
        val results = symbolManager.searchSymbols("Person", false)
        assertTrue(results.isNotEmpty())
    }
    
    private fun createTestSolution(): OmniSharpSolutionModel {
        val project = OmniSharpProjectModel(
            name = "TestProject",
            path = "c:\\test\\TestProject.csproj",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = listOf("c:\\test\\Person.cs"),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "TestSolution",
            path = "c:\\test\\TestSolution.sln",
            projects = listOf(project),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    private class TestProjectManager : OmniSharpProjectManager {
        // 实现测试用的项目管理器
        private val modifiedFiles = mutableListOf<String>()
        
        fun simulateFileChange(project: OmniSharpProjectModel, filePath: String) {
            modifiedFiles.add(filePath)
        }
        
        override fun getSolutionForProject(project: OmniSharpProjectModel): OmniSharpSolutionModel? {
            return null
        }
        
        override fun getSourceFiles(project: OmniSharpProjectModel): List<String> {
            return project.files
        }
        
        override fun getFileParser(): OmniSharpFileParser {
            return TestFileParser()
        }
        
        override fun addFileChangeListener(listener: OmniSharpFileChangeListener) {
            // 不实现
        }
        
        override fun removeFileChangeListener(listener: OmniSharpFileChangeListener) {
            // 不实现
        }
    }
    
    private class TestFileParser : OmniSharpFileParser {
        override fun parseFile(filePath: String): OmniSharpParseResult {
            return OmniSharpParseResult(filePath, emptyList())
        }
    }
}
```

### 10.3 性能测试

性能测试对于确保符号索引功能在大型项目中也能高效运行至关重要：

```kotlin
package com.omnisharp.intellij.symbol.indexing

import com.omnisharp.intellij.model.*
import org.junit.*
import kotlin.test.*

class OmniSharpSymbolIndexingPerformanceTest {
    private lateinit var symbolCollector: OmniSharpSymbolCollector
    private lateinit var symbolIndexer: OmniSharpSymbolIndexer
    
    @Before
    fun setUp() {
        symbolCollector = OmniSharpSymbolCollector(
            TestProjectManager(),
            TestFileParser(),
            OmniSharpLogger.getInstance()
        )
        symbolIndexer = OmniSharpSymbolIndexer(OmniSharpLogger.getInstance())
    }
    
    @Test
    fun testLargeSolutionIndexing() {
        // 创建大型测试解决方案（模拟100个项目，每个项目100个文件）
        val solution = createLargeTestSolution(100, 100)
        
        // 测量索引构建时间
        val startTime = System.currentTimeMillis()
        
        // 收集符号
        val symbolResult = symbolCollector.collectSymbols(solution).get()
        
        // 构建索引
        val index = symbolIndexer.buildIndex(symbolResult)
        
        val endTime = System.currentTimeMillis()
        
        println("大型解决方案索引构建耗时: ${endTime - startTime} 毫秒")
        println("收集到的符号数量: ${symbolResult.symbols.values.sumOf { it.size }}")
        
        // 验证性能要求
        assertTrue(endTime - startTime < 30000) // 30秒内完成
    }
    
    @Test
    fun testSymbolSearchPerformance() {
        // 创建测试索引
        val solution = createLargeTestSolution(50, 50)
        val symbolResult = symbolCollector.collectSymbols(solution).get()
        val index = symbolIndexer.buildIndex(symbolResult)
        
        // 创建搜索器
        val searcher = OmniSharpSymbolSearcher(OmniSharpLogger.getInstance())
        
        // 测量搜索时间
        val iterations = 100
        val startTime = System.currentTimeMillis()
        
        for (i in 0 until iterations) {
            // 随机搜索不同的符号
            searcher.searchSymbolsByName(index, "Class$i", false, false)
        }
        
        val endTime = System.currentTimeMillis()
        val avgTimePerSearch = (endTime - startTime) / iterations.toDouble()
        
        println("平均搜索时间: ${avgTimePerSearch} 毫秒/次")
        
        // 验证性能要求
        assertTrue(avgTimePerSearch < 50) // 平均每次搜索小于50毫秒
    }
    
    private fun createLargeTestSolution(projectCount: Int, filesPerProject: Int): OmniSharpSolutionModel {
        val projects = mutableListOf<OmniSharpProjectModel>()
        
        for (i in 0 until projectCount) {
            val files = mutableListOf<String>()
            for (j in 0 until filesPerProject) {
                files.add("c:\\test\\Project$i\\Class$j.cs")
            }
            
            val project = OmniSharpProjectModel(
                name = "Project$i",
                path = "c:\\test\\Project$i.csproj",
                type = "C#",
                configuration = "Debug",
                platform = "Any CPU",
                references = emptyList(),
                files = files,
                lastModified = System.currentTimeMillis()
            )
            
            projects.add(project)
        }
        
        return OmniSharpSolutionModel(
            name = "LargeSolution",
            path = "c:\\test\\LargeSolution.sln",
            projects = projects,
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
}
```

## 11. 总结与后续优化

### 11.1 已实现功能

本文档详细描述了 OmniSharp for IntelliJ 平台中项目符号索引和缓存功能的实现方案，包括：

1. **符号收集**：从项目文件中提取所有符号信息
2. **符号索引**：构建高效的索引数据结构，支持多维度查询
3. **符号缓存**：实现多级缓存策略，提高查询性能
4. **符号搜索**：提供灵活的搜索接口，支持各种搜索条件
5. **增量更新**：监听文件变化，只更新受影响的符号
6. **平台集成**：与 IntelliJ 平台的索引和项目视图系统集成

### 11.2 后续优化方向

1. **分布式索引**：对于超大型解决方案，可以考虑实现分布式索引，将索引分散到多个节点上
2. **索引压缩算法优化**：进一步优化索引压缩算法，减少内存占用和磁盘空间
3. **索引预加载**：根据用户的使用习惯，预加载常用项目的索引
4. **搜索算法优化**：实现更高级的搜索算法，如模糊匹配、正则表达式搜索等
5. **并行索引更新**：进一步优化并行索引更新策略，提高大型项目的更新性能

### 11.3 输入输出示例

#### 输入输出示例

输入：
```kotlin
// 构建索引
val solution = projectManager.loadSolution("c:\\Projects\\MySolution.sln")
symbolManager.buildIndex(solution)

// 搜索符号
val results = symbolManager.searchSymbols("User", false)
```

输出：
```kotlin
// 搜索结果示例
[
    OmniSharpClassSymbol(
        name="User",
        fullyQualifiedName="MyProject.Models.User",
        namespace="MyProject.Models",
        kind=CLASS,
        location=OmniSharpLocation("c:\\Projects\\MySolution\\MyProject\\Models\\User.cs", 10, 0),
        modifiers=["public"],
        baseClass="EntityBase",
        interfaces=["IUser", "IValidatableObject"],
        parentName=""
    ),
    OmniSharpMethodSymbol(
        name="GetUser",
        fullyQualifiedName="MyProject.Services.UserService.GetUser",
        namespace="MyProject.Services",
        kind=METHOD,
        location=OmniSharpLocation("c:\\Projects\\MySolution\\MyProject\\Services\\UserService.cs", 50, 0),
        modifiers=["public"],
        returnType="User",
        parameters=[OmniSharpParameter("userId", "int")],
        parentName="UserService"
    )
]
```

本实现方案提供了一个高效、可扩展的符号索引和缓存系统，为 OmniSharp for IntelliJ 平台的代码智能感知功能提供了坚实的基础。通过多级缓存、并行处理和增量更新等优化策略，确保了即使在大型解决方案中也能提供快速、响应迅速的符号查询体验。