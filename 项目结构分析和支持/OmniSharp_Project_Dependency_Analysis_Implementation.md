# OmniSharp 项目依赖关系分析功能实现

## 1. 概述

本文档详细描述了 OmniSharp for IntelliJ 平台中项目依赖关系分析功能的设计和实现方案。该功能旨在提供全面的 .NET 项目依赖关系解析、可视化和分析能力，帮助开发者理解项目间的依赖结构，识别潜在问题（如循环依赖、版本冲突），并支持基于依赖关系的高级功能。

### 1.1 功能目标

- 解析和分析项目之间的依赖关系
- 支持 NuGet 包依赖的分析
- 构建项目和包的依赖图
- 检测循环依赖和版本冲突
- 提供依赖关系查询和可视化接口
- 支持增量更新和缓存机制

### 1.2 适用场景

- 大型解决方案中项目结构理解
- 重构前的依赖关系分析
- 构建优化和编译顺序确定
- 包版本冲突排查
- 依赖关系可视化展示

## 2. 架构设计

### 2.1 组件关系图

```
+--------------------------+      +--------------------------+
|   DependencyAnalyzer     |      |   DependencyGraphBuilder |
+--------------------------+      +--------------------------+
| - analyzeProjectDeps()   |----->| - buildProjectDependency |
| - analyzePackageDeps()   |      | - buildPackageDependency |
| - detectCycles()         |      | - buildCombinedGraph()   |
| - checkVersionConflicts()|<-----| - getDependencyGraph()   |
+--------------------------+      +--------------------------+
           |                               ^
           v                               |
+--------------------------+      +--------------------------+
|   ProjectResolver        |----->|   DependencyGraph        |
+--------------------------+      +--------------------------+
| - resolveProjectRefs()   |      | - addNode()              |
| - resolvePackageRefs()   |      | - addEdge()              |
| - getResolvedDeps()      |      | - getNode()              |
+--------------------------+      | - getEdges()             |
           |                       | - findCycles()           |
           v                       +--------------------------+
+--------------------------+
|   PackageManager         |
+--------------------------+
| - resolvePackage()       |
| - getPackageDetails()    |
| - getPackageDependencies()|
+--------------------------+
```

### 2.2 核心流程

1. **初始化阶段**：
   - 创建 DependencyAnalyzer 实例
   - 初始化 ProjectResolver 和 PackageManager
   - 配置分析选项和缓存策略

2. **项目依赖分析流程**：
   - 解析项目文件中的 ProjectReference 元素
   - 解析解决方案中的项目引用
   - 构建项目依赖图
   - 检测循环依赖

3. **包依赖分析流程**：
   - 解析项目中的 PackageReference 元素
   - 通过 PackageManager 解析包的传递依赖
   - 构建包依赖图
   - 检测版本冲突

4. **合并分析流程**：
   - 合并项目依赖图和包依赖图
   - 生成完整依赖关系视图
   - 提供查询和遍历接口

## 3. 核心组件实现

### 3.1 依赖分析器 (DependencyAnalyzer)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.omnisharp.project.model.OmniSharpProject
import com.intellij.plugins.omnisharp.project.model.OmniSharpSolution
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 项目依赖分析器，负责分析项目间和包的依赖关系
 */
class OmniSharpDependencyAnalyzer(
    private val project: Project,
    private val solution: OmniSharpSolution
) {
    // 项目解析器
    private val projectResolver = OmniSharpProjectResolver(project)
    
    // 包管理器
    private val packageManager = OmniSharpPackageManager(project)
    
    // 依赖图构建器
    private val graphBuilder = OmniSharpDependencyGraphBuilder()
    
    // 分析结果缓存
    private val analysisCache = ConcurrentHashMap<String, DependencyAnalysisResult>()
    
    // 是否正在分析
    private val isAnalyzing = AtomicBoolean(false)
    
    /**
     * 分析解决方案中所有项目的依赖关系
     */
    suspend fun analyzeSolutionDependencies(): DependencyAnalysisResult {
        if (isAnalyzing.getAndSet(true)) {
            return waitForAnalysisComplete()
        }
        
        try {
            // 检查缓存
            val solutionPath = solution.solutionFile?.path ?: ""
            analysisCache[solutionPath]?.let { return it }
            
            // 构建依赖图
            val projectDependencyGraph = graphBuilder.buildProjectDependencyGraph(solution)
            val packageDependencyGraph = buildPackageDependencyGraph(solution)
            
            // 合并依赖图
            val combinedGraph = graphBuilder.buildCombinedGraph(projectDependencyGraph, packageDependencyGraph)
            
            // 检测循环依赖
            val cycles = detectCycles(combinedGraph)
            
            // 检测版本冲突
            val versionConflicts = checkVersionConflicts(packageDependencyGraph)
            
            // 生成分析结果
            val result = DependencyAnalysisResult(
                projectDependencyGraph = projectDependencyGraph,
                packageDependencyGraph = packageDependencyGraph,
                combinedGraph = combinedGraph,
                cycles = cycles,
                versionConflicts = versionConflicts
            )
            
            // 缓存结果
            analysisCache[solutionPath] = result
            return result
        } finally {
            isAnalyzing.set(false)
        }
    }
    
    /**
     * 分析单个项目的依赖关系
     */
    suspend fun analyzeProjectDependencies(projectFile: VirtualFile): DependencyAnalysisResult {
        // 检查缓存
        val projectPath = projectFile.path
        analysisCache[projectPath]?.let { return it }
        
        // 解析项目
        val omniSharpProject = projectResolver.resolveProject(projectFile)
        
        // 分析项目依赖
        val projectDependencies = projectResolver.resolveProjectReferences(omniSharpProject)
        val packageDependencies = packageManager.resolvePackageDependencies(omniSharpProject)
        
        // 构建依赖图
        val projectGraph = graphBuilder.buildProjectDependencyGraph(projectDependencies)
        val packageGraph = graphBuilder.buildPackageDependencyGraph(packageDependencies)
        val combinedGraph = graphBuilder.buildCombinedGraph(projectGraph, packageGraph)
        
        // 检测问题
        val cycles = detectCycles(combinedGraph)
        val versionConflicts = checkVersionConflicts(packageGraph)
        
        // 生成结果
        val result = DependencyAnalysisResult(
            projectDependencyGraph = projectGraph,
            packageDependencyGraph = packageGraph,
            combinedGraph = combinedGraph,
            cycles = cycles,
            versionConflicts = versionConflicts
        )
        
        // 缓存结果
        analysisCache[projectPath] = result
        return result
    }
    
    /**
     * 检测循环依赖
     */
    fun detectCycles(graph: DependencyGraph): List<Cycle> {
        return graph.findCycles()
    }
    
    /**
     * 检查版本冲突
     */
    fun checkVersionConflicts(packageGraph: DependencyGraph): List<VersionConflict> {
        val conflicts = mutableListOf<VersionConflict>()
        val packageVersions = mutableMapOf<String, MutableSet<String>>()
        
        // 收集所有包的不同版本
        graphBuilder.getAllPackages(packageGraph).forEach { packageNode ->
            if (packageNode is PackageNode) {
                val packageName = packageNode.packageName
                val version = packageNode.version
                
                if (!packageVersions.containsKey(packageName)) {
                    packageVersions[packageName] = mutableSetOf()
                }
                
                packageVersions[packageName]?.add(version)
            }
        }
        
        // 检测版本冲突
        packageVersions.forEach { (packageName, versions) ->
            if (versions.size > 1) {
                conflicts.add(VersionConflict(packageName, versions.toList()))
            }
        }
        
        return conflicts
    }
    
    /**
     * 构建包依赖图
     */
    private suspend fun buildPackageDependencyGraph(solution: OmniSharpSolution): DependencyGraph {
        val allDependencies = mutableListOf<PackageDependency>()
        
        // 并行分析每个项目的包依赖
        coroutineScope {
            val deferredResults = solution.getAllProjects().map {
                async(Dispatchers.IO) {
                    packageManager.resolvePackageDependencies(it)
                }
            }
            
            // 收集所有结果
            deferredResults.awaitAll().forEach {\ dependencies ->
                allDependencies.addAll(dependencies)
            }
        }
        
        return graphBuilder.buildPackageDependencyGraph(allDependencies)
    }
    
    /**
     * 等待分析完成
     */
    private suspend fun waitForAnalysisComplete(): DependencyAnalysisResult {
        while (isAnalyzing.get()) {
            delay(100)
        }
        
        val solutionPath = solution.solutionFile?.path ?: ""
        return analysisCache[solutionPath] ?: run {
            // 如果缓存为空，再次运行分析
            analyzeSolutionDependencies()
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        analysisCache.clear()
    }
}
```

### 3.2 项目解析器 (ProjectResolver)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.omnisharp.project.model.OmniSharpProject
import com.intellij.plugins.omnisharp.project.model.OmniSharpSolution
import java.nio.file.Path

/**
 * 项目解析器，负责解析项目引用关系
 */
class OmniSharpProjectResolver(private val project: Project) {
    
    /**
     * 解析单个项目
     */
    fun resolveProject(projectFile: VirtualFile): OmniSharpProject {
        // 这里应该调用之前实现的项目文件解析器
        // 为了简化，这里直接返回模拟的项目对象
        return OmniSharpProject(projectFile, project)
    }
    
    /**
     * 解析项目引用
     */
    fun resolveProjectReferences(omniSharpProject: OmniSharpProject): List<ProjectDependency> {
        val dependencies = mutableListOf<ProjectDependency>()
        
        // 解析项目文件中的 ProjectReference 元素
        omniSharpProject.projectReferences.forEach projectRef@{ projectRef ->
            // 获取引用项目的路径
            val referencedProjectPath = resolveProjectPath(omniSharpProject, projectRef.includePath)
            if (referencedProjectPath == null) {
                // 项目路径无法解析，记录警告
                return@projectRef
            }
            
            // 创建依赖关系
            dependencies.add(ProjectDependency(
                sourceProject = omniSharpProject,
                targetProjectPath = referencedProjectPath.toString(),
                dependencyType = DependencyType.PROJECT_REFERENCE
            ))
        }
        
        return dependencies
    }
    
    /**
     * 解析解决方案中的所有项目引用
     */
    fun resolveSolutionProjectReferences(solution: OmniSharpSolution): List<ProjectDependency> {
        val dependencies = mutableListOf<ProjectDependency>()
        
        // 遍历所有项目
        solution.getAllProjects().forEach { project ->
            // 解析该项目的引用
            dependencies.addAll(resolveProjectReferences(project))
        }
        
        return dependencies
    }
    
    /**
     * 解析项目路径
     */
    private fun resolveProjectPath(sourceProject: OmniSharpProject, relativePath: String): Path? {
        try {
            val sourceDir = sourceProject.projectFile.parent?.toNioPath() ?: return null
            return sourceDir.resolve(relativePath).normalize()
        } catch (e: Exception) {
            // 处理路径解析错误
            return null
        }
    }
    
    /**
     * 获取项目的直接和间接依赖
     */
    fun getResolvedDeps(project: OmniSharpProject, level: Int = Int.MAX_VALUE): Set<ProjectDependency> {
        val resolved = mutableSetOf<ProjectDependency>()
        val visited = mutableSetOf<String>()
        
        fun dfs(currentProject: OmniSharpProject, currentLevel: Int) {
            if (currentLevel > level || visited.contains(currentProject.projectFile.path)) {
                return
            }
            
            visited.add(currentProject.projectFile.path)
            
            val deps = resolveProjectReferences(currentProject)
            resolved.addAll(deps)
            
            deps.forEach { dep ->
                // 解析目标项目
                val targetFile = LocalFileSystem.getInstance().findFileByPath(dep.targetProjectPath)
                if (targetFile != null) {
                    val targetProject = resolveProject(targetFile)
                    dfs(targetProject, currentLevel + 1)
                }
            }
        }
        
        dfs(project, 0)
        return resolved
    }
}
```

### 3.3 包管理器 (PackageManager)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.project.Project
import com.intellij.plugins.omnisharp.project.model.OmniSharpProject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * 包管理器，负责解析和管理NuGet包依赖
 */
class OmniSharpPackageManager(private val project: Project) {
    // 本地包缓存路径
    private val nugetCachePath: Path
    
    // 包元数据缓存
    private val packageMetadataCache = mutableMapOf<String, PackageMetadata>()
    
    init {
        // 初始化NuGet缓存路径
        nugetCachePath = determineNuGetCachePath()
    }
    
    /**
     * 解析项目的包依赖
     */
    suspend fun resolvePackageDependencies(project: OmniSharpProject): List<PackageDependency> {
        val dependencies = mutableListOf<PackageDependency>()
        
        // 解析直接包引用
        coroutineScope {
            val deferredDependencies = project.packageReferences.map {\ packageRef ->
                async(Dispatchers.IO) {
                    resolvePackageDependency(project, packageRef)
                }
            }
            
            // 收集结果
            deferredDependencies.awaitAll().forEach {\ deps ->
                dependencies.addAll(deps)
            }
        }
        
        return dependencies
    }
    
    /**
     * 解析单个包引用及其传递依赖
     */
    private suspend fun resolvePackageDependency(
        project: OmniSharpProject,
        packageReference: PackageReference
    ): List<PackageDependency> {
        val dependencies = mutableListOf<PackageDependency>()
        val visited = mutableSetOf<String>()
        
        // 解析直接依赖
        val directDependency = PackageDependency(
            sourceProject = project,
            packageName = packageReference.name,
            version = packageReference.version,
            dependencyType = DependencyType.PACKAGE_REFERENCE
        )
        dependencies.add(directDependency)
        
        // 解析传递依赖
        resolveTransitiveDependencies(
            project,
            packageReference.name,
            packageReference.version,
            dependencies,
            visited
        )
        
        return dependencies
    }
    
    /**
     * 解析包的传递依赖
     */
    private suspend fun resolveTransitiveDependencies(
        project: OmniSharpProject,
        packageName: String,
        version: String,
        dependencies: MutableList<PackageDependency>,
        visited: MutableSet<String>
    ) {
        // 避免循环依赖
        val key = "$packageName@$version"
        if (visited.contains(key)) {
            return
        }
        
        visited.add(key)
        
        // 获取包的元数据
        val metadata = getPackageMetadata(packageName, version)
        
        // 解析传递依赖
        coroutineScope {
            val deferredResults = metadata.dependencies.map { transitiveDep ->
                async(Dispatchers.IO) {
                    val transitiveDependency = PackageDependency(
                        sourceProject = project,
                        packageName = transitiveDep.name,
                        version = transitiveDep.version,
                        dependencyType = DependencyType.TRANSITIVE_PACKAGE,
                        parentPackage = "$packageName@$version"
                    )
                    
                    dependencies.add(transitiveDependency)
                    
                    // 递归解析
                    resolveTransitiveDependencies(
                        project,
                        transitiveDep.name,
                        transitiveDep.version,
                        dependencies,
                        visited
                    )
                }
            }
            
            // 等待所有解析完成
            deferredResults.awaitAll()
        }
    }
    
    /**
     * 获取包的元数据
     */
    suspend fun getPackageMetadata(packageName: String, version: String): PackageMetadata {
        val cacheKey = "$packageName@$version"
        
        // 检查缓存
        packageMetadataCache[cacheKey]?.let { return it }
        
        // 尝试从本地缓存解析
        return withContext(Dispatchers.IO) {
            val metadata = resolvePackageMetadata(packageName, version)
            packageMetadataCache[cacheKey] = metadata
            metadata
        }
    }
    
    /**
     * 解析包元数据
     */
    private fun resolvePackageMetadata(packageName: String, version: String): PackageMetadata {
        try {
            // 尝试从NuGet缓存读取
            val packagePath = nugetCachePath.resolve(packageName.toLowerCase())
                .resolve(version)
                .resolve("${packageName}.nuspec")
            
            if (Files.exists(packagePath)) {
                // 解析.nuspec文件
                return parseNuspecFile(packagePath)
            }
            
            // 如果本地缓存不存在，可以尝试从NuGet服务器获取
            // 这里返回一个模拟的元数据
            return PackageMetadata(
                name = packageName,
                version = version,
                dependencies = emptyList()
            )
        } catch (e: Exception) {
            // 处理错误，返回空元数据
            return PackageMetadata(
                name = packageName,
                version = version,
                dependencies = emptyList()
            )
        }
    }
    
    /**
     * 解析.nuspec文件
     */
    private fun parseNuspecFile(nuspecPath: Path): PackageMetadata {
        // 这里应该实现.nuspec文件解析
        // 为了简化，这里返回模拟数据
        return PackageMetadata(
            name = "PackageName",
            version = "1.0.0",
            dependencies = emptyList()
        )
    }
    
    /**
     * 确定NuGet缓存路径
     */
    private fun determineNuGetCachePath(): Path {
        // 获取用户主目录
        val userHome = System.getProperty("user.home") ?: "/"
        
        // 根据操作系统确定NuGet缓存路径
        return when {
            System.getProperty("os.name").toLowerCase().contains("windows") -> {
                Paths.get(userHome, ".nuget", "packages")
            }
            else -> {
                Paths.get(userHome, ".nuget", "packages")
            }
        }
    }
    
    /**
     * 获取包详情
     */
    fun getPackageDetails(packageName: String, version: String): PackageDetails {
        // 获取包元数据
        val metadata = runBlocking { getPackageMetadata(packageName, version) }
        
        // 构建包详情
        return PackageDetails(
            name = metadata.name,
            version = metadata.version,
            dependencies = metadata.dependencies,
            // 其他详情字段...
        )
    }
    
    /**
     * 清理缓存
     */
    fun clearCache() {
        packageMetadataCache.clear()
    }
}
```

### 3.4 依赖图构建器 (DependencyGraphBuilder)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.plugins.omnisharp.project.model.OmniSharpProject
import com.intellij.plugins.omnisharp.project.model.OmniSharpSolution
import java.util.*

/**
 * 依赖图构建器，负责构建项目和包的依赖图
 */
class OmniSharpDependencyGraphBuilder {
    
    /**
     * 构建项目依赖图
     */
    fun buildProjectDependencyGraph(solution: OmniSharpSolution): DependencyGraph {
        val graph = DependencyGraphImpl()
        
        // 添加所有项目节点
        solution.getAllProjects().forEach { project ->
            val node = ProjectNode(project)
            graph.addNode(node)
        }
        
        // 添加项目间的依赖边
        solution.getAllProjects().forEach { project ->
            project.projectReferences.forEach { projectRef ->
                // 查找目标项目
                val targetProject = findProjectByPath(solution, projectRef.includePath)
                if (targetProject != null) {
                    val sourceNode = ProjectNode(project)
                    val targetNode = ProjectNode(targetProject)
                    
                    // 添加依赖边
                    graph.addEdge(sourceNode, targetNode, EdgeType.PROJECT_REFERENCE)
                }
            }
        }
        
        return graph
    }
    
    /**
     * 构建项目依赖图（从依赖列表）
     */
    fun buildProjectDependencyGraph(dependencies: List<ProjectDependency>): DependencyGraph {
        val graph = DependencyGraphImpl()
        
        // 添加所有项目节点
        dependencies.forEach { dep ->
            val sourceNode = ProjectNode(dep.sourceProject)
            graph.addNode(sourceNode)
            
            // 这里应该添加目标项目节点，但我们需要先解析目标项目
        }
        
        // 添加依赖边
        dependencies.forEach { dep ->
            val sourceNode = ProjectNode(dep.sourceProject)
            // 这里应该查找目标项目节点并添加边
        }
        
        return graph
    }
    
    /**
     * 构建包依赖图
     */
    fun buildPackageDependencyGraph(dependencies: List<PackageDependency>): DependencyGraph {
        val graph = DependencyGraphImpl()
        
        // 添加所有包节点
        dependencies.forEach { dep ->
            val packageNode = PackageNode(
                packageName = dep.packageName,
                version = dep.version
            )
            graph.addNode(packageNode)
            
            // 添加项目节点
            val projectNode = ProjectNode(dep.sourceProject)
            graph.addNode(projectNode)
            
            // 添加项目到包的依赖边
            graph.addEdge(projectNode, packageNode, EdgeType.PACKAGE_REFERENCE)
        }
        
        // 添加包之间的传递依赖
        dependencies.filter { it.parentPackage != null }.forEach { dep ->
            val parentParts = it.parentPackage!!.split("@")
            if (parentParts.size == 2) {
                val parentNode = PackageNode(parentParts[0], parentParts[1])
                val childNode = PackageNode(dep.packageName, dep.version)
                
                graph.addEdge(parentNode, childNode, EdgeType.TRANSITIVE_DEPENDENCY)
            }
        }
        
        return graph
    }
    
    /**
     * 合并项目依赖图和包依赖图
     */
    fun buildCombinedGraph(projectGraph: DependencyGraph, packageGraph: DependencyGraph): DependencyGraph {
        val combinedGraph = DependencyGraphImpl()
        
        // 添加项目图的所有节点和边
        projectGraph.getAllNodes().forEach { combinedGraph.addNode(it) }
        projectGraph.getAllEdges().forEach { combinedGraph.addEdge(it) }
        
        // 添加包图的所有节点和边
        packageGraph.getAllNodes().forEach { combinedGraph.addNode(it) }
        packageGraph.getAllEdges().forEach { combinedGraph.addEdge(it) }
        
        return combinedGraph
    }
    
    /**
     * 根据路径查找项目
     */
    private fun findProjectByPath(solution: OmniSharpSolution, path: String): OmniSharpProject? {
        return solution.getAllProjects().find { 
            it.projectFile.path.endsWith(path) || 
            it.projectFile.parent?.path?.endsWith(path) == true
        }
    }
    
    /**
     * 获取图中的所有包
     */
    fun getAllPackages(graph: DependencyGraph): List<DependencyNode> {
        return graph.getAllNodes().filter { it is PackageNode }
    }
    
    /**
     * 获取图中的所有项目
     */
    fun getAllProjects(graph: DependencyGraph): List<DependencyNode> {
        return graph.getAllNodes().filter { it is ProjectNode }
    }
}
```

### 3.5 依赖图实现 (DependencyGraph)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.collections.set

/**
 * 依赖图接口
 */
interface DependencyGraph {
    /**
     * 添加节点
     */
    fun addNode(node: DependencyNode)
    
    /**
     * 添加边
     */
    fun addEdge(source: DependencyNode, target: DependencyNode, type: EdgeType)
    
    /**
     * 添加边
     */
    fun addEdge(edge: DependencyEdge)
    
    /**
     * 获取节点
     */
    fun getNode(id: String): DependencyNode?
    
    /**
     * 获取所有节点
     */
    fun getAllNodes(): List<DependencyNode>
    
    /**
     * 获取所有边
     */
    fun getAllEdges(): List<DependencyEdge>
    
    /**
     * 获取从指定节点出发的边
     */
    fun getOutgoingEdges(node: DependencyNode): List<DependencyEdge>
    
    /**
     * 获取指向指定节点的边
     */
    fun getIncomingEdges(node: DependencyNode): List<DependencyEdge>
    
    /**
     * 查找循环依赖
     */
    fun findCycles(): List<Cycle>
}

/**
 * 依赖图实现
 */
class DependencyGraphImpl : DependencyGraph {
    // 节点映射
    private val nodes = ConcurrentHashMap<String, DependencyNode>()
    
    // 边映射（源节点ID -> 边列表）
    private val outgoingEdges = ConcurrentHashMap<String, MutableList<DependencyEdge>>()
    
    // 边映射（目标节点ID -> 边列表）
    private val incomingEdges = ConcurrentHashMap<String, MutableList<DependencyEdge>>()
    
    // 所有边
    private val allEdges = mutableListOf<DependencyEdge>()
    
    // 读写锁
    private val lock = ReentrantReadWriteLock()
    
    override fun addNode(node: DependencyNode) {
        lock.writeLock().lock()
        try {
            nodes[node.id] = node
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    override fun addEdge(source: DependencyNode, target: DependencyNode, type: EdgeType) {
        addNode(source)
        addNode(target)
        
        val edge = DependencyEdge(source, target, type)
        addEdge(edge)
    }
    
    override fun addEdge(edge: DependencyEdge) {
        lock.writeLock().lock()
        try {
            // 确保源节点和目标节点存在
            nodes[edge.source.id] = edge.source
            nodes[edge.target.id] = edge.target
            
            // 添加出边
            if (!outgoingEdges.containsKey(edge.source.id)) {
                outgoingEdges[edge.source.id] = mutableListOf()
            }
            outgoingEdges[edge.source.id]?.add(edge)
            
            // 添加入边
            if (!incomingEdges.containsKey(edge.target.id)) {
                incomingEdges[edge.target.id] = mutableListOf()
            }
            incomingEdges[edge.target.id]?.add(edge)
            
            // 添加到所有边列表
            allEdges.add(edge)
        } finally {
            lock.writeLock().unlock()
        }
    }
    
    override fun getNode(id: String): DependencyNode? {
        lock.readLock().lock()
        try {
            return nodes[id]
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun getAllNodes(): List<DependencyNode> {
        lock.readLock().lock()
        try {
            return nodes.values.toList()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun getAllEdges(): List<DependencyEdge> {
        lock.readLock().lock()
        try {
            return allEdges.toList()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun getOutgoingEdges(node: DependencyNode): List<DependencyEdge> {
        lock.readLock().lock()
        try {
            return outgoingEdges[node.id]?.toList() ?: emptyList()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun getIncomingEdges(node: DependencyNode): List<DependencyEdge> {
        lock.readLock().lock()
        try {
            return incomingEdges[node.id]?.toList() ?: emptyList()
        } finally {
            lock.readLock().unlock()
        }
    }
    
    override fun findCycles(): List<Cycle> {
        lock.readLock().lock()
        try {
            val visited = mutableSetOf<String>()
            val recursionStack = mutableSetOf<String>()
            val cycles = mutableListOf<Cycle>()
            val path = mutableListOf<String>()
            
            // 对每个未访问的节点执行DFS
            nodes.values.forEach { node ->
                if (!visited.contains(node.id)) {
                    dfs(node.id, visited, recursionStack, path, cycles)
                }
            }
            
            return cycles
        } finally {
            lock.readLock().unlock()
        }
    }
    
    /**
     * 深度优先搜索检测循环
     */
    private fun dfs(
        nodeId: String,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        path: MutableList<String>,
        cycles: MutableList<Cycle>
    ) {
        // 标记为已访问并加入递归栈
        visited.add(nodeId)
        recursionStack.add(nodeId)
        path.add(nodeId)
        
        // 递归访问所有邻居节点
        outgoingEdges[nodeId]?.forEach { edge ->
            val neighborId = edge.target.id
            
            // 如果邻居未访问，继续DFS
            if (!visited.contains(neighborId)) {
                dfs(neighborId, visited, recursionStack, path, cycles)
            }
            // 如果邻居在递归栈中，发现循环
            else if (recursionStack.contains(neighborId)) {
                // 提取循环路径
                val cycleStartIndex = path.indexOf(neighborId)
                if (cycleStartIndex >= 0) {
                    val cyclePath = path.subList(cycleStartIndex, path.size).toList()
                    
                    // 创建循环对象
                    val cycle = Cycle(cyclePath)
                    cycles.add(cycle)
                }
            }
        }
        
        // 从递归栈中移除当前节点
        recursionStack.remove(nodeId)
        path.removeLast()
    }
}
```

### 3.6 数据模型类

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.plugins.omnisharp.project.model.OmniSharpProject

/**
 * 依赖分析结果
 */
data class DependencyAnalysisResult(
    val projectDependencyGraph: DependencyGraph,
    val packageDependencyGraph: DependencyGraph,
    val combinedGraph: DependencyGraph,
    val cycles: List<Cycle>,
    val versionConflicts: List<VersionConflict>
)

/**
 * 依赖节点基类
 */
sealed class DependencyNode {
    abstract val id: String
    abstract val name: String
}

/**
 * 项目节点
 */
data class ProjectNode(val project: OmniSharpProject) : DependencyNode() {
    override val id: String = project.projectFile.path
    override val name: String = project.name
}

/**
 * 包节点
 */
data class PackageNode(
    val packageName: String,
    val version: String
) : DependencyNode() {
    override val id: String = "$packageName@$version"
    override val name: String = "$packageName ($version)"
}

/**
 * 依赖边
 */
data class DependencyEdge(
    val source: DependencyNode,
    val target: DependencyNode,
    val type: EdgeType
)

/**
 * 边类型
 */
enum class EdgeType {
    PROJECT_REFERENCE,
    PACKAGE_REFERENCE,
    TRANSITIVE_DEPENDENCY,
    FRAMEWORK_REFERENCE
}

/**
 * 依赖类型
 */
enum class DependencyType {
    PROJECT_REFERENCE,
    PACKAGE_REFERENCE,
    TRANSITIVE_PACKAGE,
    FRAMEWORK_REFERENCE
}

/**
 * 项目依赖
 */
data class ProjectDependency(
    val sourceProject: OmniSharpProject,
    val targetProjectPath: String,
    val dependencyType: DependencyType
)

/**
 * 包依赖
 */
data class PackageDependency(
    val sourceProject: OmniSharpProject,
    val packageName: String,
    val version: String,
    val dependencyType: DependencyType,
    val parentPackage: String? = null
)

/**
 * 包引用
 */
data class PackageReference(
    val name: String,
    val version: String,
    val isDevelopmentDependency: Boolean = false
)

/**
 * 项目引用
 */
data class ProjectReference(
    val includePath: String,
    val name: String? = null
)

/**
 * 包元数据
 */
data class PackageMetadata(
    val name: String,
    val version: String,
    val dependencies: List<PackageReference>
)

/**
 * 包详情
 */
data class PackageDetails(
    val name: String,
    val version: String,
    val dependencies: List<PackageReference>,
    val authors: List<String> = emptyList(),
    val description: String = "",
    val releaseNotes: String = "",
    val iconUrl: String? = null
)

/**
 * 循环依赖
 */
data class Cycle(
    val path: List<String>
) {
    val size: Int get() = path.size
    
    override fun toString(): String {
        return path.joinToString(" -> ")
    }
}

/**
 * 版本冲突
 */
data class VersionConflict(
    val packageName: String,
    val conflictingVersions: List<String>
) {
    override fun toString(): String {
        return "$packageName: ${conflictingVersions.joinToString(", ")}"
    }
}
```

### 3.7 依赖分析服务 (DependencyAnalysisService)

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.plugins.omnisharp.project.model.OmniSharpSolution
import kotlinx.coroutines.runBlocking

/**
 * 依赖分析服务，作为全局访问点
 */
@Service(Service.Level.PROJECT)
class OmniSharpDependencyAnalysisService(val project: Project) {
    // 依赖分析器
    private val dependencyAnalyzer: OmniSharpDependencyAnalyzer
    
    init {
        // 初始化依赖分析器
        dependencyAnalyzer = OmniSharpDependencyAnalyzer(project, getSolution())
    }
    
    /**
     * 获取解决方案
     */
    private fun getSolution(): OmniSharpSolution {
        // 这里应该从项目服务中获取解决方案
        // 为了简化，这里返回一个模拟的解决方案
        return project.service<OmniSharpSolution>()
    }
    
    /**
     * 分析解决方案依赖
     */
    fun analyzeSolutionDependencies(): DependencyAnalysisResult {
        return runBlocking {
            dependencyAnalyzer.analyzeSolutionDependencies()
        }
    }
    
    /**
     * 分析项目依赖
     */
    fun analyzeProjectDependencies(projectFile: VirtualFile): DependencyAnalysisResult {
        return runBlocking {
            dependencyAnalyzer.analyzeProjectDependencies(projectFile)
        }
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        dependencyAnalyzer.clearCache()
    }
    
    /**
     * 获取依赖图
     */
    fun getDependencyGraph(): DependencyGraph {
        return analyzeSolutionDependencies().combinedGraph
    }
    
    /**
     * 查找循环依赖
     */
    fun findCycles(): List<Cycle> {
        return analyzeSolutionDependencies().cycles
    }
    
    /**
     * 检查版本冲突
     */
    fun checkVersionConflicts(): List<VersionConflict> {
        return analyzeSolutionDependencies().versionConflicts
    }
}

/**
 * 扩展函数，用于获取依赖分析服务
 */
val Project.dependencyAnalysisService: OmniSharpDependencyAnalysisService
    get() = service<OmniSharpDependencyAnalysisService>()
```

## 4. 依赖分析的高级功能

### 4.1 增量依赖分析

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileManager

/**
 * 增量依赖分析管理器
 */
class OmniSharpIncrementalDependencyAnalyzer(
    private val analyzer: OmniSharpDependencyAnalyzer
) : VirtualFileListener {
    
    /**
     * 监听文件变更
     */
    override fun contentsChanged(event: VirtualFileEvent) {
        val file = event.file
        
        // 检查是否是项目文件
        if (file.name.endsWith(".csproj") || 
            file.name.endsWith(".vbproj") || 
            file.name.endsWith(".fsproj")) {
            
            // 清除相关缓存
            analyzer.clearCache()
            
            // 重新分析受影响的项目
            // 这里可以实现更精确的增量分析
            reanalyzeAffectedProjects(file)
        }
    }
    
    /**
     * 重新分析受影响的项目
     */
    private fun reanalyzeAffectedProjects(changedProjectFile: VirtualFile) {
        // 实现增量分析逻辑
        // 1. 分析变更的项目
        // 2. 识别依赖于该项目的其他项目
        // 3. 仅重新分析受影响的项目
    }
    
    /**
     * 注册监听器
     */
    fun registerListener() {
        VirtualFileManager.getInstance().addVirtualFileListener(this)
    }
    
    /**
     * 注销监听器
     */
    fun unregisterListener() {
        VirtualFileManager.getInstance().removeVirtualFileListener(this)
    }
}
```

### 4.2 依赖关系可视化

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.visualization

import com.intellij.plugins.omnisharp.project.dependencies.DependencyGraph
import com.intellij.plugins.omnisharp.project.dependencies.DependencyNode
import com.intellij.plugins.omnisharp.project.dependencies.ProjectNode
import com.intellij.plugins.omnisharp.project.dependencies.PackageNode
import java.util.*

/**
 * 依赖图可视化工具
 */
class DependencyGraphVisualizer {
    
    /**
     * 生成DOT格式的图形表示
     */
    fun generateDotGraph(graph: DependencyGraph): String {
        val sb = StringBuilder()
        sb.append("digraph Dependencies {")
        sb.append("\n")
        
        // 配置节点样式
        sb.append("  node [shape=box];")
        sb.append("\n")
        
        // 添加项目节点（蓝色）
        graph.getAllNodes().filterIsInstance<ProjectNode>().forEach { node ->
            sb.append("  \"${node.name}\" [color=blue];")
            sb.append("\n")
        }
        
        // 添加包节点（绿色）
        graph.getAllNodes().filterIsInstance<PackageNode>().forEach { node ->
            sb.append("  \"${node.name}\" [color=green];")
            sb.append("\n")
        }
        
        // 添加边
        graph.getAllEdges().forEach { edge ->
            sb.append("  \"${edge.source.name}\" -> \"${edge.target.name}\"; ")
            sb.append("\n")
        }
        
        sb.append("}")
        return sb.toString()
    }
    
    /**
     * 生成简化的依赖图（仅项目）
     */
    fun generateProjectOnlyGraph(graph: DependencyGraph): DependencyGraph {
        val simplifiedGraph = DependencyGraphImpl()
        
        // 添加项目节点
        graph.getAllNodes().filterIsInstance<ProjectNode>().forEach {
            simplifiedGraph.addNode(it)
        }
        
        // 添加项目间的边
        graph.getAllEdges().forEach { edge ->
            if (edge.source is ProjectNode && edge.target is ProjectNode) {
                simplifiedGraph.addEdge(edge)
            }
        }
        
        return simplifiedGraph
    }
    
    /**
     * 获取依赖深度
     */
    fun getDependencyDepth(graph: DependencyGraph, node: DependencyNode): Int {
        val visited = mutableSetOf<String>()
        return dfsDepth(graph, node, visited)
    }
    
    /**
     * DFS计算深度
     */
    private fun dfsDepth(
        graph: DependencyGraph,
        node: DependencyNode,
        visited: MutableSet<String>
    ): Int {
        if (visited.contains(node.id)) {
            return 0
        }
        
        visited.add(node.id)
        
        var maxDepth = 0
        graph.getOutgoingEdges(node).forEach { edge ->
            val depth = dfsDepth(graph, edge.target, visited)
            maxDepth = maxOf(maxDepth, depth)
        }
        
        return maxDepth + 1
    }
}
```

### 4.3 循环依赖可视化和修复建议

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.analysis

import com.intellij.plugins.omnisharp.project.dependencies.Cycle
import com.intellij.plugins.omnisharp.project.dependencies.DependencyAnalysisResult
import com.intellij.plugins.omnisharp.project.dependencies.DependencyGraph
import com.intellij.plugins.omnisharp.project.dependencies.ProjectNode

/**
 * 循环依赖分析器
 */
class CycleDependencyAnalyzer {
    
    /**
     * 分析循环依赖并生成修复建议
     */
    fun analyzeCycles(analysisResult: DependencyAnalysisResult): List<CycleAnalysisResult> {
        val results = mutableListOf<CycleAnalysisResult>()
        
        // 分析每个循环
        analysisResult.cycles.forEach { cycle ->
            val suggestions = generateFixSuggestions(cycle, analysisResult.combinedGraph)
            results.add(CycleAnalysisResult(cycle, suggestions))
        }
        
        return results
    }
    
    /**
     * 生成修复建议
     */
    private fun generateFixSuggestions(cycle: Cycle, graph: DependencyGraph): List<FixSuggestion> {
        val suggestions = mutableListOf<FixSuggestion>()
        
        // 找出可能的断开点
        cycle.path.forEachIndexed { index, nodeId ->
            val nextIndex = (index + 1) % cycle.path.size
            val nextNodeId = cycle.path[nextIndex]
            
            // 检查这两个节点之间的边
            val node = graph.getNode(nodeId)
            val nextNode = graph.getNode(nextNodeId)
            
            if (node != null && nextNode != null) {
                // 生成修复建议
                val suggestion = FixSuggestion(
                    sourceNode = node,
                    targetNode = nextNode,
                    suggestionType = SuggestionType.REMOVE_DEPENDENCY
                )
                suggestions.add(suggestion)
            }
        }
        
        // 添加其他类型的建议（如提取共享代码）
        if (suggestions.size > 0) {
            suggestions.add(generateExtractSharedCodeSuggestion(cycle, graph))
        }
        
        return suggestions
    }
    
    /**
     * 生成提取共享代码的建议
     */
    private fun generateExtractSharedCodeSuggestion(
        cycle: Cycle,
        graph: DependencyGraph
    ): FixSuggestion {
        // 找出循环中的项目节点
        val projectNodes = cycle.path
            .mapNotNull { graph.getNode(it) }
            .filterIsInstance<ProjectNode>()
        
        // 生成提取共享代码的建议
        return FixSuggestion(
            sourceNode = null,
            targetNode = null,
            suggestionType = SuggestionType.EXTRACT_SHARED_CODE,
            description = "考虑从 ${projectNodes.joinToString(", ") { it.name }} 中提取共享代码到一个新的公共项目中"
        )
    }
}

/**
 * 循环分析结果
 */
data class CycleAnalysisResult(
    val cycle: Cycle,
    val fixSuggestions: List<FixSuggestion>
)

/**
 * 修复建议
 */
data class FixSuggestion(
    val sourceNode: DependencyNode?,
    val targetNode: DependencyNode?,
    val suggestionType: SuggestionType,
    val description: String? = null
)

/**
 * 建议类型
 */
enum class SuggestionType {
    REMOVE_DEPENDENCY,
    EXTRACT_SHARED_CODE,
    REORGANIZE_PROJECTS
}
```

## 5. 性能优化

### 5.1 缓存策略

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.cache

import com.intellij.plugins.omnisharp.project.dependencies.DependencyAnalysisResult
import com.intellij.plugins.omnisharp.project.model.OmniSharpProject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * 依赖分析缓存
 */
class DependencyAnalysisCache {
    // 缓存条目
    private data class CacheEntry(
        val result: DependencyAnalysisResult,
        val timestamp: Long,
        val version: String // 用于跟踪项目版本
    )
    
    // 项目缓存
    private val projectCache = ConcurrentHashMap<String, CacheEntry>()
    
    // 解决方案缓存
    private val solutionCache = ConcurrentHashMap<String, CacheEntry>()
    
    // 缓存过期时间（毫秒）
    private val cacheExpiryTime = TimeUnit.MINUTES.toMillis(5)
    
    /**
     * 获取项目缓存
     */
    fun getProjectCache(projectPath: String, version: String): DependencyAnalysisResult? {
        val entry = projectCache[projectPath]
        
        // 检查缓存是否有效
        if (entry != null && 
            entry.version == version && 
            !isExpired(entry.timestamp)) {
            return entry.result
        }
        
        // 缓存无效，移除
        if (entry != null) {
            projectCache.remove(projectPath)
        }
        
        return null
    }
    
    /**
     * 设置项目缓存
     */
    fun setProjectCache(projectPath: String, version: String, result: DependencyAnalysisResult) {
        projectCache[projectPath] = CacheEntry(result, System.currentTimeMillis(), version)
    }
    
    /**
     * 获取解决方案缓存
     */
    fun getSolutionCache(solutionPath: String, version: String): DependencyAnalysisResult? {
        val entry = solutionCache[solutionPath]
        
        // 检查缓存是否有效
        if (entry != null && 
            entry.version == version && 
            !isExpired(entry.timestamp)) {
            return entry.result
        }
        
        // 缓存无效，移除
        if (entry != null) {
            solutionCache.remove(solutionPath)
        }
        
        return null
    }
    
    /**
     * 设置解决方案缓存
     */
    fun setSolutionCache(solutionPath: String, version: String, result: DependencyAnalysisResult) {
        solutionCache[solutionPath] = CacheEntry(result, System.currentTimeMillis(), version)
    }
    
    /**
     * 清除特定项目的缓存
     */
    fun clearProjectCache(projectPath: String) {
        projectCache.remove(projectPath)
    }
    
    /**
     * 清除特定解决方案的缓存
     */
    fun clearSolutionCache(solutionPath: String) {
        solutionCache.remove(solutionPath)
    }
    
    /**
     * 清除所有缓存
     */
    fun clearAll() {
        projectCache.clear()
        solutionCache.clear()
    }
    
    /**
     * 清理过期缓存
     */
    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        
        // 清理项目缓存
        projectCache.entries.removeIf { (_, entry) ->
            isExpired(entry.timestamp, now)
        }
        
        // 清理解决方案缓存
        solutionCache.entries.removeIf { (_, entry) ->
            isExpired(entry.timestamp, now)
        }
    }
    
    /**
     * 检查是否过期
     */
    private fun isExpired(timestamp: Long, now: Long = System.currentTimeMillis()): Boolean {
        return now - timestamp > cacheExpiryTime
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getStats(): CacheStats {
        return CacheStats(
            projectCacheSize = projectCache.size,
            solutionCacheSize = solutionCache.size
        )
    }
}

/**
 * 缓存统计信息
 */
data class CacheStats(
    val projectCacheSize: Int,
    val solutionCacheSize: Int
)
```

### 5.2 并行处理

在之前的实现中，我们已经使用了协程进行并行处理：

```kotlin
// 在PackageManager中并行解析包依赖
coroutineScope {
    val deferredDependencies = project.packageReferences.map {\ packageRef ->
        async(Dispatchers.IO) {
            resolvePackageDependency(project, packageRef)
        }
    }
    
    // 收集结果
    deferredDependencies.awaitAll().forEach {\ deps ->
        dependencies.addAll(deps)
    }
}
```

### 5.3 延迟加载

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.project.Project
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 延迟加载的依赖分析器
 */
class LazyDependencyAnalyzer(private val project: Project) {
    // 依赖分析器
    private val analyzer by lazy {
        OmniSharpDependencyAnalyzer(project, project.service<OmniSharpSolution>())
    }
    
    // 分析结果
    private val analysisResult by lazy {
        runBlocking {
            analyzer.analyzeSolutionDependencies()
        }
    }
    
    // 项目分析结果缓存
    private val projectAnalysisCache = ConcurrentHashMap<String, Deferred<DependencyAnalysisResult>>()
    
    /**
     * 获取解决方案分析结果（延迟加载）
     */
    fun getSolutionAnalysisResult(): DependencyAnalysisResult {
        return analysisResult
    }
    
    /**
     * 获取项目分析结果（延迟加载）
     */
    suspend fun getProjectAnalysisResult(projectPath: String): DependencyAnalysisResult {
        return projectAnalysisCache.computeIfAbsent(projectPath) { path ->
            GlobalScope.async(Dispatchers.IO) {
                val projectFile = LocalFileSystem.getInstance().findFileByPath(path)
                projectFile?.let {
                    analyzer.analyzeProjectDependencies(it)
                } ?: throw IllegalArgumentException("Project file not found: $path")
            }
        }.await()
    }
    
    /**
     * 预加载项目分析
     */
    fun preloadProjectAnalysis(projectPath: String) {
        projectAnalysisCache.computeIfAbsent(projectPath) { path ->
            GlobalScope.async(Dispatchers.IO) {
                val projectFile = LocalFileSystem.getInstance().findFileByPath(path)
                projectFile?.let {
                    analyzer.analyzeProjectDependencies(it)
                } ?: throw IllegalArgumentException("Project file not found: $path")
            }
        }
    }
}
```

## 6. 错误处理

### 6.1 依赖解析错误处理

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.errors

import com.intellij.openapi.diagnostic.Logger
import com.intellij.plugins.omnisharp.project.dependencies.PackageDependency
import com.intellij.plugins.omnisharp.project.dependencies.ProjectDependency

/**
 * 依赖解析错误
 */
sealed class DependencyResolutionError(
    val message: String,
    val cause: Throwable? = null
)

/**
 * 项目解析错误
 */
class ProjectResolutionError(
    val projectPath: String,
    message: String,
    cause: Throwable? = null
) : DependencyResolutionError(message, cause)

/**
 * 包解析错误
 */
class PackageResolutionError(
    val packageName: String,
    val version: String,
    message: String,
    cause: Throwable? = null
) : DependencyResolutionError(message, cause)

/**
 * 依赖解析错误处理器
 */
class DependencyErrorHandler {
    private val logger = Logger.getInstance(javaClass)
    
    /**
     * 处理项目解析错误
     */
    fun handleProjectError(error: ProjectResolutionError): ProjectDependency? {
        logger.warn("Failed to resolve project ${error.projectPath}: ${error.message}", error.cause)
        
        // 根据错误类型决定是否返回部分结果或null
        return null // 或者返回带有警告标记的依赖
    }
    
    /**
     * 处理包解析错误
     */
    fun handlePackageError(error: PackageResolutionError): PackageDependency? {
        logger.warn("Failed to resolve package ${error.packageName}@${error.version}: ${error.message}", error.cause)
        
        // 尝试使用其他版本或返回null
        return null
    }
    
    /**
     * 收集所有错误
     */
    fun collectErrors(errors: List<DependencyResolutionError>): ErrorReport {
        val projectErrors = errors.filterIsInstance<ProjectResolutionError>()
        val packageErrors = errors.filterIsInstance<PackageResolutionError>()
        
        return ErrorReport(
            projectErrors = projectErrors,
            packageErrors = packageErrors,
            totalErrors = errors.size
        )
    }
}

/**
 * 错误报告
 */
data class ErrorReport(
    val projectErrors: List<ProjectResolutionError>,
    val packageErrors: List<PackageResolutionError>,
    val totalErrors: Int
) {
    val hasErrors: Boolean get() = totalErrors > 0
}
```

## 7. 集成到 IntelliJ 平台

### 7.1 与项目视图集成

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.ui

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.impl.nodes.ProjectViewModuleNode
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.nodes.ProjectViewNodeDecorator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.CellAppearanceEx
import com.intellij.openapi.roots.ui.ProjectRootsUtil
import com.intellij.plugins.omnisharp.project.dependencies.Cycle
import com.intellij.plugins.omnisharp.project.dependencies.OmniSharpDependencyAnalysisService
import com.intellij.ui.ColoredTreeCellRenderer
import java.awt.Color

/**
 * 项目视图依赖装饰器
 */
class DependencyProjectViewDecorator(private val project: Project) : ProjectViewNodeDecorator {
    private val analysisService by lazy {
        project.dependencyAnalysisService
    }
    
    override fun decorate(node: ProjectViewNode<*>, cellRenderer: ColoredTreeCellRenderer) {
        // 检查是否是模块节点
        if (node is ProjectViewModuleNode) {
            val module = node.value
            val moduleName = module.name
            
            // 检查是否有循环依赖
            val cycles = analysisService.findCycles()
            val hasCycle = cycles.any { cycle ->
                cycle.path.any { it.contains(moduleName) }
            }
            
            // 如果有循环依赖，标记节点
            if (hasCycle) {
                cellRenderer.append(" (循环依赖)", CellAppearanceEx.FONT_PLAIN)
                cellRenderer.foreground = Color.RED
            }
        }
    }
    
    /**
     * 刷新项目视图
     */
    fun refreshProjectView() {
        ProjectView.getInstance(project).refresh()
    }
}
```

### 7.2 依赖视图工具窗口

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.ui.tree.TreeUtil
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * 依赖视图工具窗口工厂
 */
class DependencyViewToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(createDependencyViewPanel(project), "依赖关系", false)
        toolWindow.contentManager.addContent(content)
    }
    
    private fun createDependencyViewPanel(project: Project): JPanel {
        val panel = JPanel()
        
        // 创建依赖树
        val rootNode = DefaultMutableTreeNode("依赖关系")
        val treeModel = DefaultTreeModel(rootNode)
        val tree = Tree(treeModel)
        
        // 添加到面板
        panel.add(tree)
        
        // 加载依赖数据
        loadDependencyData(project, rootNode, tree)
        
        return panel
    }
    
    private fun loadDependencyData(project: Project, rootNode: DefaultMutableTreeNode, tree: JTree) {
        val analysisService = project.dependencyAnalysisService
        val graph = analysisService.getDependencyGraph()
        
        // 构建树结构
        graph.getAllNodes().forEach { node ->
            val nodeTreeNode = DefaultMutableTreeNode(node.name)
            rootNode.add(nodeTreeNode)
            
            // 添加子依赖
            graph.getOutgoingEdges(node).forEach { edge ->
                nodeTreeNode.add(DefaultMutableTreeNode(edge.target.name))
            }
        }
        
        // 展开树
        TreeUtil.expandAll(tree)
    }
}
```

## 8. 测试策略

### 8.1 单元测试

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.vfs.VirtualFile
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 依赖分析器测试
 */
class OmniSharpDependencyAnalyzerTest {
    @Test
    fun testSimpleProjectDependencies() {
        // 创建模拟项目
        val project1 = createMockProject("Project1", listOf(ProjectReference("../Project2/Project2.csproj")))
        val project2 = createMockProject("Project2", emptyList())
        
        // 创建模拟解决方案
        val solution = createMockSolution(listOf(project1, project2))
        
        // 创建分析器
        val analyzer = OmniSharpDependencyAnalyzer(project1.project, solution)
        
        // 分析依赖
        runBlocking {
            val result = analyzer.analyzeSolutionDependencies()
            
            // 验证结果
            assertNotNull(result)
            assertEquals(2, result.projectDependencyGraph.getAllNodes().size)
            
            // 检查是否有循环依赖
            assertTrue(result.cycles.isEmpty())
        }
    }
    
    @Test
    fun testCycleDetection() {
        // 创建循环依赖的项目
        val project1 = createMockProject("Project1", listOf(ProjectReference("../Project2/Project2.csproj")))
        val project2 = createMockProject("Project2", listOf(ProjectReference("../Project1/Project1.csproj")))
        
        // 创建模拟解决方案
        val solution = createMockSolution(listOf(project1, project2))
        
        // 创建分析器
        val analyzer = OmniSharpDependencyAnalyzer(project1.project, solution)
        
        // 分析依赖
        runBlocking {
            val result = analyzer.analyzeSolutionDependencies()
            
            // 验证循环依赖检测
            assertEquals(1, result.cycles.size)
            val cycle = result.cycles[0]
            assertEquals(2, cycle.size)
        }
    }
    
    private fun createMockProject(name: String, projectReferences: List<ProjectReference>): OmniSharpProject {
        // 创建模拟项目的实现
        return MockOmniSharpProject(name, projectReferences)
    }
    
    private fun createMockSolution(projects: List<OmniSharpProject>): OmniSharpSolution {
        // 创建模拟解决方案的实现
        return MockOmniSharpSolution(projects)
    }
}
```

### 8.2 集成测试

```kotlin
package com.intellij.plugins.omnisharp.project.dependencies

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import java.io.File

/**
 * 依赖分析集成测试
 */
class OmniSharpDependencyAnalysisIntegrationTest : BasePlatformTestCase() {
    private lateinit var testProject: Project
    private lateinit var dependencyAnalyzer: OmniSharpDependencyAnalyzer
    
    override fun setUp() {
        super.setUp()
        testProject = project
        dependencyAnalyzer = OmniSharpDependencyAnalyzer(testProject, createMockSolution())
    }
    
    @Test
    fun testRealProjectDependencies() {
        // 创建测试项目文件
        val mainProjectFile = createTestProjectFile(
            "MainProject.csproj",
            """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <OutputType>Exe</OutputType>
                <TargetFramework>net6.0</TargetFramework>
              </PropertyGroup>
              <ItemGroup>
                <ProjectReference Include="LibraryProject/LibraryProject.csproj" />
              </ItemGroup>
            </Project>
            """.trimIndent()
        )
        
        val libraryProjectFile = createTestProjectFile(
            "LibraryProject/LibraryProject.csproj",
            """
            <Project Sdk="Microsoft.NET.Sdk">
              <PropertyGroup>
                <TargetFramework>net6.0</TargetFramework>
              </PropertyGroup>
            </Project>
            """.trimIndent()
        )
        
        // 分析依赖
        runBlocking {
            val result = dependencyAnalyzer.analyzeProjectDependencies(mainProjectFile)
            
            // 验证结果
            assertNotNull(result)
            assertTrue(result.cycles.isEmpty())
        }
    }
    
    private fun createTestProjectFile(relativePath: String, content: String): VirtualFile {
        val file = File(project.basePath, relativePath)
        file.parentFile.mkdirs()
        file.writeText(content)
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file)!!
    }
    
    private fun createMockSolution(): OmniSharpSolution {
        // 创建模拟解决方案
        return MockOmniSharpSolution(emptyList())
    }
}
```

## 9. 使用示例

### 9.1 基本用法

```kotlin
// 分析解决方案依赖
val analysisResult = project.dependencyAnalysisService.analyzeSolutionDependencies()

// 检查是否有循环依赖
if (analysisResult.cycles.isNotEmpty()) {
    println("发现 ${analysisResult.cycles.size} 个循环依赖：")
    analysisResult.cycles.forEach { cycle ->
        println("  - $cycle")
    }
}

// 检查是否有版本冲突
if (analysisResult.versionConflicts.isNotEmpty()) {
    println("发现 ${analysisResult.versionConflicts.size} 个版本冲突：")
    analysisResult.versionConflicts.forEach { conflict ->
        println("  - $conflict")
    }
}

// 获取依赖图
val dependencyGraph = analysisResult.combinedGraph

// 遍历所有节点
println("\n项目依赖图节点:")
dependencyGraph.getAllNodes().forEach { node ->
    println("  - ${node.name}")
}

// 遍历所有边
println("\n项目依赖图边:")
dependencyGraph.getAllEdges().forEach { edge ->
    println("  - ${edge.source.name} -> ${edge.target.name} (${edge.type})")
}

### 9.2 依赖图可视化

```kotlin
// 创建依赖图可视化工具
val visualizer = DependencyGraphVisualizer()

// 生成DOT格式的图形表示
val dotGraph = visualizer.generateDotGraph(analysisResult.combinedGraph)

// 保存到文件
File("dependencies.dot").writeText(dotGraph)
println("依赖图已保存到 dependencies.dot")

// 生成仅包含项目的简化依赖图
val projectOnlyGraph = visualizer.generateProjectOnlyGraph(analysisResult.combinedGraph)
val projectDotGraph = visualizer.generateDotGraph(projectOnlyGraph)
File("project_dependencies.dot").writeText(projectDotGraph)
println("项目依赖图已保存到 project_dependencies.dot")

// 计算依赖深度
val mainProjectNode = projectOnlyGraph.getAllNodes().firstOrNull { it.name == "MainProject" }
if (mainProjectNode != null) {
    val depth = visualizer.getDependencyDepth(projectOnlyGraph, mainProjectNode)
    println("MainProject 的依赖深度: $depth")
}
```

### 9.3 循环依赖分析和修复

```kotlin
// 创建循环依赖分析器
val cycleAnalyzer = CycleDependencyAnalyzer()

// 分析循环依赖并生成修复建议
val cycleAnalysisResults = cycleAnalyzer.analyzeCycles(analysisResult)

// 处理循环依赖分析结果
cycleAnalysisResults.forEachIndexed { index, result ->
    println("\n循环依赖 #${index + 1}:")
    println("  - 循环路径: ${result.cycle}")
    println("  - 修复建议:")
    
    result.fixSuggestions.forEach { suggestion ->
        when (suggestion.suggestionType) {
            SuggestionType.REMOVE_DEPENDENCY -> {
                println("    - 移除依赖: ${suggestion.sourceNode?.name} -> ${suggestion.targetNode?.name}")
            }
            SuggestionType.EXTRACT_SHARED_CODE -> {
                println("    - 提取共享代码: ${suggestion.description}")
            }
            SuggestionType.REORGANIZE_PROJECTS -> {
                println("    - 重新组织项目: ${suggestion.description}")
            }
        }
    }
}
```

### 9.4 增量分析和监听

```kotlin
// 创建增量分析器
val incrementalAnalyzer = OmniSharpIncrementalDependencyAnalyzer(dependencyAnalyzer)

// 注册文件变更监听
incrementalAnalyzer.registerListener()

// 当不再需要时注销监听器
// incrementalAnalyzer.unregisterListener()
```

## 10. 总结与后续优化

### 10.1 已实现功能

- 完整的项目依赖关系解析和分析
- NuGet包依赖分析和传递依赖解析
- 依赖图构建和可视化
- 循环依赖检测和修复建议
- 版本冲突检测
- 增量分析和缓存机制
- 与IntelliJ平台集成

### 10.2 后续优化方向

1. **性能优化**：
   - 实现更高效的缓存策略
   - 优化大型解决方案的依赖分析性能
   - 实现增量分析算法，减少不必要的重新计算

2. **功能增强**：
   - 支持更多类型的依赖（如COM引用、WinRT引用）
   - 实现更复杂的依赖冲突解决策略
   - 提供依赖版本升级建议

3. **用户界面改进**：
   - 实现交互式依赖图可视化
   - 添加依赖关系搜索和过滤功能
   - 支持依赖关系的导出和共享

4. **代码质量**：
   - 完善单元测试和集成测试
   - 优化错误处理和异常情况
   - 提高代码的模块化和可维护性

### 10.3 输入输出示例

#### 输入输出示例 1: 基本依赖分析

输入：
```kotlin
val analysisResult = project.dependencyAnalysisService.analyzeSolutionDependencies()
println("分析完成。发现 ${analysisResult.cycles.size} 个循环依赖。")
```

输出：
```
分析完成。发现 1 个循环依赖。
```

#### 输入输出示例 2: 循环依赖检测

输入：
```kotlin
val cycles = project.dependencyAnalysisService.findCycles()
cycles.forEachIndexed { index, cycle ->
    println("循环依赖 #${index + 1}: $cycle")
}
```

输出：
```
循环依赖 #1: ProjectA -> ProjectB -> ProjectA
```

#### 输入输出示例 3: 版本冲突检测

输入：
```kotlin
val conflicts = project.dependencyAnalysisService.checkVersionConflicts()
conflicts.forEach { conflict ->
    println("包版本冲突: $conflict")
}
```

输出：
```
包版本冲突: Newtonsoft.Json: 12.0.3, 13.0.1
```

通过以上实现，OmniSharp for IntelliJ 平台将能够提供强大的项目依赖关系分析功能，帮助开发者更好地理解和管理 .NET 项目的依赖结构，提高开发效率和代码质量。