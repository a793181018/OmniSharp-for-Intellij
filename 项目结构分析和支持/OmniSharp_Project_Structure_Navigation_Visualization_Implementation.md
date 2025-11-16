# OmniSharp for IntelliJ 项目结构导航和可视化功能实现方案

## 1. 概述

本文档详细描述 OmniSharp for IntelliJ 平台中项目结构导航和可视化功能的实现方案。该功能旨在提供直观、高效的方式让用户浏览、导航和可视化 C# 项目结构，包括解决方案、项目、文件以及它们之间的关系。

主要目标：
- 提供直观的项目结构导航界面
- 支持多维度的项目视图（按逻辑结构、物理文件结构等）
- 实现项目依赖关系的可视化展示
- 提供快速定位和过滤功能
- 与 IntelliJ 平台的导航系统深度集成

## 2. 架构设计

### 2.1 组件关系图

```
+-------------------------------------------+
|            IntelliJ 平台集成               |
|  +------------------+  +----------------+  |
|  | Project View 插件|  | Tool Window API|  |
|  +------------------+  +----------------+  |
+-------------------------------------------+
                      |
                      v
+-----------------------------------------------------------+
|                 OmniSharp 结构导航核心                       |
|  +----------------+  +----------------+  +---------------+  |
|  | ProjectNavigator|  | StructureTree  |  | FilterManager |  |
|  +----------------+  +----------------+  +---------------+  |
|                                                            |
|  +----------------+  +----------------+  +---------------+  |
|  | ViewController |  | NavigationNode |  | Visualization |  |
|  +----------------+  +----------------+  +---------------+  |
+-----------------------------------------------------------+
                      |
                      v
+-----------------------------------------------------------+
|                OmniSharp 核心服务                             |
|  +----------------+  +----------------+  +---------------+  |
|  | ProjectManager |  | SymbolManager  |  | DependencyMgr |  |
|  +----------------+  +----------------+  +---------------+  |
+-----------------------------------------------------------+
```

### 2.2 结构导航流程

1. 用户打开 C# 解决方案
2. ProjectManager 加载解决方案和项目信息
3. ProjectNavigator 根据加载的信息构建导航树
4. StructureTree 组件将导航树渲染到 UI
5. 用户可以通过各种视图模式切换、过滤和搜索功能来浏览项目结构
6. 用户交互事件通过 ViewController 处理，与底层服务进行交互

## 3. 核心组件实现

### 3.1 ProjectNavigator

ProjectNavigator 是整个导航功能的核心控制器，负责协调各个组件的工作：

```kotlin
package com.omnisharp.intellij.navigation

import com.intellij.openapi.project.Project
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.services.*
import com.omnisharp.intellij.util.*
import java.util.concurrent.atomic.AtomicReference

class OmniSharpProjectNavigator(
    private val project: Project,
    private val projectManager: OmniSharpProjectManager,
    private val symbolManager: OmniSharpSymbolManager,
    private val dependencyManager: OmniSharpDependencyManager
) {
    private val navigationTreeRef = AtomicReference<OmniSharpNavigationTree?>(null)
    
    /**
     * 为指定解决方案构建导航树
     */
    fun buildNavigationTree(solution: OmniSharpSolutionModel): OmniSharpNavigationTree {
        val rootNode = OmniSharpNavigationNode(
            id = solution.path,
            name = solution.name,
            type = NodeType.SOLUTION,
            model = solution
        )
        
        // 添加项目节点
        solution.projects.forEach { project ->
            val projectNode = buildProjectNode(project, solution)
            rootNode.addChild(projectNode)
        }
        
        val tree = OmniSharpNavigationTree(rootNode)
        navigationTreeRef.set(tree)
        return tree
    }
    
    /**
     * 构建项目节点及其子节点
     */
    private fun buildProjectNode(project: OmniSharpProjectModel, solution: OmniSharpSolutionModel): OmniSharpNavigationNode {
        val projectNode = OmniSharpNavigationNode(
            id = project.path,
            name = project.name,
            type = NodeType.PROJECT,
            model = project
        )
        
        // 构建逻辑结构（按命名空间组织）
        buildLogicalStructure(projectNode, project, solution)
        
        // 构建物理结构（按文件系统组织）
        buildPhysicalStructure(projectNode, project)
        
        return projectNode
    }
    
    /**
     * 构建逻辑结构（命名空间层级）
     */
    private fun buildLogicalStructure(
        projectNode: OmniSharpNavigationNode,
        project: OmniSharpProjectModel,
        solution: OmniSharpSolutionModel
    ) {
        val logicalNode = OmniSharpNavigationNode(
            id = "${project.path}_logical",
            name = "Logical Structure",
            type = NodeType.LOGICAL_STRUCTURE,
            model = null
        )
        projectNode.addChild(logicalNode)
        
        // 获取项目符号并按命名空间组织
        val symbols = symbolManager.getSymbolsForProject(project)
        val namespaceMap = mutableMapOf<String, OmniSharpNavigationNode>()
        
        symbols.forEach {\ symbol ->
            if (symbol is OmniSharpClassSymbol || symbol is OmniSharpInterfaceSymbol || 
                symbol is OmniSharpEnumSymbol || symbol is OmniSharpStructSymbol) {
                
                val namespace = symbol.namespace
                if (namespace.isNotEmpty()) {
                    val nsNode = namespaceMap.getOrPut(namespace) { 
                        createNamespaceNode(namespace, logicalNode)
                    }
                    
                    val typeNode = OmniSharpNavigationNode(
                        id = symbol.fullyQualifiedName,
                        name = symbol.name,
                        type = getNodeTypeForSymbol(symbol),
                        model = symbol
                    )
                    nsNode.addChild(typeNode)
                }
            }
        }
    }
    
    /**
     * 构建物理结构（文件系统层级）
     */
    private fun buildPhysicalStructure(
        projectNode: OmniSharpNavigationNode,
        project: OmniSharpProjectModel
    ) {
        val physicalNode = OmniSharpNavigationNode(
            id = "${project.path}_physical",
            name = "Files",
            type = NodeType.PHYSICAL_STRUCTURE,
            model = null
        )
        projectNode.addChild(physicalNode)
        
        // 按目录结构组织文件
        val directoryMap = mutableMapOf<String, OmniSharpNavigationNode>()
        
        project.files.forEach { filePath ->
            val fileInfo = OmniSharpFileUtil.getFileInfo(filePath)
            val directoryPath = fileInfo.directoryPath
            
            val directoryNode = getOrCreateDirectoryNode(directoryPath, physicalNode, directoryMap)
            
            val fileNode = OmniSharpNavigationNode(
                id = filePath,
                name = fileInfo.fileName,
                type = NodeType.FILE,
                model = filePath
            )
            directoryNode.addChild(fileNode)
        }
    }
    
    /**
     * 创建命名空间节点
     */
    private fun createNamespaceNode(namespace: String, parentNode: OmniSharpNavigationNode): OmniSharpNavigationNode {
        val parts = namespace.split('.')
        var currentNode = parentNode
        var currentNamespace = ""
        
        parts.forEachIndexed { index, part ->
            currentNamespace = if (index == 0) part else "$currentNamespace.$part"
            
            val existingChild = currentNode.findChildByTypeAndName(NodeType.NAMESPACE, part)
            if (existingChild != null) {
                currentNode = existingChild
            } else {
                val newNode = OmniSharpNavigationNode(
                    id = currentNamespace,
                    name = part,
                    type = NodeType.NAMESPACE,
                    model = currentNamespace
                )
                currentNode.addChild(newNode)
                currentNode = newNode
            }
        }
        
        return currentNode
    }
    
    /**
     * 获取或创建目录节点
     */
    private fun getOrCreateDirectoryNode(
        directoryPath: String,
        rootNode: OmniSharpNavigationNode,
        directoryMap: MutableMap<String, OmniSharpNavigationNode>
    ): OmniSharpNavigationNode {
        if (directoryMap.containsKey(directoryPath)) {
            return directoryMap[directoryPath]!!
        }
        
        // 分割路径并构建目录层次
        val dirs = directoryPath.split('\\', '/').filter { it.isNotEmpty() }
        var currentNode = rootNode
        var currentPath = ""
        
        dirs.forEachIndexed { index, dir ->
            currentPath = if (index == 0) dir else "$currentPath/${dir}"
            
            val existingChild = currentNode.findChildByTypeAndName(NodeType.DIRECTORY, dir)
            if (existingChild != null) {
                currentNode = existingChild
            } else {
                val newNode = OmniSharpNavigationNode(
                    id = currentPath,
                    name = dir,
                    type = NodeType.DIRECTORY,
                    model = currentPath
                )
                currentNode.addChild(newNode)
                currentNode = newNode
                directoryMap[currentPath] = newNode
            }
        }
        
        directoryMap[directoryPath] = currentNode
        return currentNode
    }
    
    /**
     * 获取符号对应的节点类型
     */
    private fun getNodeTypeForSymbol(symbol: OmniSharpSymbol): NodeType {
        return when (symbol) {
            is OmniSharpClassSymbol -> NodeType.CLASS
            is OmniSharpInterfaceSymbol -> NodeType.INTERFACE
            is OmniSharpEnumSymbol -> NodeType.ENUM
            is OmniSharpStructSymbol -> NodeType.STRUCT
            else -> NodeType.SYMBOL
        }
    }
    
    /**
     * 获取当前导航树
     */
    fun getCurrentNavigationTree(): OmniSharpNavigationTree? {
        return navigationTreeRef.get()
    }
    
    /**
     * 刷新导航树
     */
    fun refreshNavigationTree(solution: OmniSharpSolutionModel): OmniSharpNavigationTree {
        return buildNavigationTree(solution)
    }
}

/**
 * 导航节点类型枚举
 */
enum class NodeType {
    SOLUTION,
    PROJECT,
    LOGICAL_STRUCTURE,
    PHYSICAL_STRUCTURE,
    NAMESPACE,
    DIRECTORY,
    FILE,
    CLASS,
    INTERFACE,
    ENUM,
    STRUCT,
    SYMBOL,
    DEPENDENCY
}

/**
 * 导航节点类，代表导航树中的一个节点
 */
class OmniSharpNavigationNode(
    val id: String,
    val name: String,
    val type: NodeType,
    val model: Any?
) {
    private val children = mutableListOf<OmniSharpNavigationNode>()
    private var parent: OmniSharpNavigationNode? = null
    
    /**
     * 添加子节点
     */
    fun addChild(child: OmniSharpNavigationNode) {
        children.add(child)
        child.parent = this
    }
    
    /**
     * 移除子节点
     */
    fun removeChild(child: OmniSharpNavigationNode) {
        children.remove(child)
        child.parent = null
    }
    
    /**
     * 获取子节点列表
     */
    fun getChildren(): List<OmniSharpNavigationNode> {
        return children.toList()
    }
    
    /**
     * 获取父节点
     */
    fun getParent(): OmniSharpNavigationNode? {
        return parent
    }
    
    /**
     * 查找指定类型和名称的子节点
     */
    fun findChildByTypeAndName(type: NodeType, name: String): OmniSharpNavigationNode? {
        return children.find { it.type == type && it.name == name }
    }
    
    /**
     * 检查节点是否有子节点
     */
    fun hasChildren(): Boolean {
        return children.isNotEmpty()
    }
    
    /**
     * 获取节点路径
     */
    fun getPath(): List<OmniSharpNavigationNode> {
        val path = mutableListOf<OmniSharpNavigationNode>()
        var current: OmniSharpNavigationNode? = this
        
        while (current != null) {
            path.add(0, current)
            current = current.parent
        }
        
        return path
    }
}

/**
 * 导航树类，代表整个项目结构的导航树
 */
class OmniSharpNavigationTree(val root: OmniSharpNavigationNode) {
    /**
     * 查找指定ID的节点
     */
    fun findNodeById(id: String): OmniSharpNavigationNode? {
        return findNodeById(root, id)
    }
    
    /**
     * 递归查找指定ID的节点
     */
    private fun findNodeById(node: OmniSharpNavigationNode, id: String): OmniSharpNavigationNode? {
        if (node.id == id) {
            return node
        }
        
        for (child in node.getChildren()) {
            val result = findNodeById(child, id)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
    
    /**
     * 按类型查找节点
     */
    fun findNodesByType(type: NodeType): List<OmniSharpNavigationNode> {
        val result = mutableListOf<OmniSharpNavigationNode>()
        findNodesByType(root, type, result)
        return result
    }
    
    /**
     * 递归按类型查找节点
     */
    private fun findNodesByType(
        node: OmniSharpNavigationNode,
        type: NodeType,
        result: MutableList<OmniSharpNavigationNode>
    ) {
        if (node.type == type) {
            result.add(node)
        }
        
        node.getChildren().forEach { child ->
            findNodesByType(child, type, result)
        }
    }
}
```

### 3.2 StructureTree

StructureTree 组件负责将导航树渲染到 IntelliJ 平台的 UI 中，提供直观的项目结构展示：

```kotlin
package com.omnisharp.intellij.navigation

import com.intellij.ide.projectView.*
import com.intellij.ide.projectView.impl.*
import com.intellij.ide.projectView.impl.nodes.*
import com.intellij.ide.util.treeView.*
import com.intellij.openapi.project.*
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
import com.omnisharp.intellij.model.*
import javax.swing.tree.*

/**
 * OmniSharp 项目结构树视图
 */
class OmniSharpStructureTree(val project: Project, private val projectNavigator: OmniSharpProjectNavigator) {
    private var treeBuilder: TreeBuilder? = null
    private var treeStructure: OmniSharpTreeStructure? = null
    
    /**
     * 创建项目结构树视图
     */
    fun createStructureTree(): ProjectViewTree {
        val projectViewTree = ProjectViewTree(project)
        treeStructure = OmniSharpTreeStructure(project, projectNavigator)
        
        treeBuilder = TreeBuilder.createBuilder(
            projectViewTree,
            null,
            treeStructure!!,
            DefaultTreeModel(null)
        )
        
        return projectViewTree
    }
    
    /**
     * 刷新项目结构树
     */
    fun refresh() {
        treeBuilder?.queueUpdate()
    }
    
    /**
     * 展开到指定节点
     */
    fun expandToNode(nodeId: String) {
        val node = projectNavigator.getCurrentNavigationTree()?.findNodeById(nodeId)
        if (node != null) {
            val path = node.getPath().map { 
                OmniSharpTreeNode(project, it) 
            }.toTypedArray()
            
            treeBuilder?.expandToLevel(TreePath(path), 0)
        }
    }
    
    /**
     * 折叠所有节点
     */
    fun collapseAll() {
        treeBuilder?.collapseAll()
    }
    
    /**
     * 过滤树节点
     */
    fun filter(filter: (OmniSharpNavigationNode) -> Boolean) {
        treeStructure?.setFilter(filter)
        treeBuilder?.queueUpdate()
    }
}

/**
 * OmniSharp 树结构实现
 */
class OmniSharpTreeStructure(
    private val project: Project,
    private val projectNavigator: OmniSharpProjectNavigator
) : AbstractTreeStructure() {
    private var filter: ((OmniSharpNavigationNode) -> Boolean)? = null
    
    /**
     * 设置过滤函数
     */
    fun setFilter(filter: (OmniSharpNavigationNode) -> Boolean) {
        this.filter = filter
    }
    
    /**
     * 清除过滤
     */
    fun clearFilter() {
        this.filter = null
    }
    
    override fun getRootElement(): Any {
        val navigationTree = projectNavigator.getCurrentNavigationTree()
        return if (navigationTree != null) {
            OmniSharpTreeNode(project, navigationTree.root)
        } else {
            EmptyTraversableNode(project)
        }
    }
    
    override fun createChildren(node: Any): MutableCollection<Any> {
        return when (node) {
            is OmniSharpTreeNode -> {
                val children = mutableListOf<Any>()
                
                node.navigationNode.getChildren().forEach { child ->
                    // 应用过滤
                    if (filter?.invoke(child) != false) {
                        children.add(OmniSharpTreeNode(project, child))
                    }
                }
                
                children
            }
            else -> mutableListOf()
        }
    }
    
    override fun getParentElement(element: Any): Any? {
        return when (element) {
            is OmniSharpTreeNode -> {
                val parent = element.navigationNode.getParent()
                if (parent != null) {
                    OmniSharpTreeNode(project, parent)
                } else {
                    null
                }
            }
            else -> null
        }
    }
    
    override fun hasSomethingToCommit(): Boolean {
        return false
    }
    
    override fun commit() {
        // 不需要提交更改
    }
}

/**
 * OmniSharp 树节点实现
 */
class OmniSharpTreeNode(val project: Project, val navigationNode: OmniSharpNavigationNode) : AbstractTreeNode<Any>(project, navigationNode.model) {
    override fun update(presentation: PresentationData) {
        // 设置节点显示信息
        presentation.presentableText = navigationNode.name
        
        // 设置节点图标
        presentation.setIcon(OmniSharpIcons.getIconForNodeType(navigationNode.type))
        
        // 设置节点工具提示
        presentation.tooltip = getTooltip()
        
        // 根据节点类型设置其他属性
        when (navigationNode.type) {
            NodeType.PROJECT -> {
                if (navigationNode.model is OmniSharpProjectModel) {
                    val projectModel = navigationNode.model as OmniSharpProjectModel
                    presentation.locationString = "${projectModel.type} | ${projectModel.configuration}/${projectModel.platform}"
                }
            }
            NodeType.FILE -> {
                presentation.locationString = navigationNode.model as String
            }
            NodeType.CLASS, NodeType.INTERFACE, NodeType.ENUM, NodeType.STRUCT -> {
                if (navigationNode.model is OmniSharpClassSymbol) {
                    val classSymbol = navigationNode.model as OmniSharpClassSymbol
                    presentation.locationString = classSymbol.namespace
                }
            }
            else -> {}
        }
    }
    
    /**
     * 获取节点工具提示
     */
    private fun getTooltip(): String {
        return when (navigationNode.type) {
            NodeType.SOLUTION -> "Solution: ${navigationNode.name}"
            NodeType.PROJECT -> "Project: ${navigationNode.name}"
            NodeType.FILE -> "File: ${navigationNode.model as String}"
            else -> navigationNode.name
        }
    }
    
    override fun getChildren(): MutableCollection<out AbstractTreeNode<*>> {
        val children = mutableListOf<AbstractTreeNode<*>>()
        
        navigationNode.getChildren().forEach { child ->
            children.add(OmniSharpTreeNode(project, child))
        }
        
        return children
    }
    
    override fun contains(file: VirtualFile): Boolean {
        // 检查节点是否包含指定文件
        if (navigationNode.type == NodeType.FILE && navigationNode.model == file.path) {
            return true
        }
        
        // 递归检查子节点
        for (child in getChildren()) {
            if (child.contains(file)) {
                return true
            }
        }
        
        return false
    }
}
```

### 3.3 ViewController

ViewController 负责处理用户交互，连接 UI 和底层服务：

```kotlin
package com.omnisharp.intellij.navigation

import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.vfs.*
import com.intellij.ui.treeStructure.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.services.*
import java.awt.*
import java.awt.event.*
import javax.swing.*

/**
 * 视图控制器，处理用户交互和视图状态管理
 */
class OmniSharpViewController(
    private val project: Project,
    private val projectNavigator: OmniSharpProjectNavigator,
    private val structureTree: OmniSharpStructureTree
) {
    private var currentViewMode: ViewMode = ViewMode.LOGICAL
    private val filterManager = OmniSharpFilterManager()
    
    /**
     * 初始化视图控制器
     */
    fun initialize() {
        // 初始化视图模式
        setViewMode(currentViewMode)
        
        // 注册事件监听器
        registerEventListeners()
    }
    
    /**
     * 切换视图模式
     */
    fun setViewMode(mode: ViewMode) {
        currentViewMode = mode
        
        // 根据视图模式应用不同的过滤和排序
        when (mode) {
            ViewMode.LOGICAL -> applyLogicalViewFilter()
            ViewMode.PHYSICAL -> applyPhysicalViewFilter()
            ViewMode.TYPES -> applyTypesViewFilter()
            ViewMode.DEPENDENCIES -> applyDependenciesViewFilter()
        }
        
        // 刷新树
        structureTree.refresh()
    }
    
    /**
     * 应用逻辑视图过滤（只显示命名空间和类型）
     */
    private fun applyLogicalViewFilter() {
        structureTree.filter { node ->
            when (node.type) {
                NodeType.SOLUTION,
                NodeType.PROJECT,
                NodeType.LOGICAL_STRUCTURE,
                NodeType.NAMESPACE,
                NodeType.CLASS,
                NodeType.INTERFACE,
                NodeType.ENUM,
                NodeType.STRUCT,
                NodeType.SYMBOL -> true
                else -> false
            }
        }
    }
    
    /**
     * 应用物理视图过滤（只显示文件系统结构）
     */
    private fun applyPhysicalViewFilter() {
        structureTree.filter { node ->
            when (node.type) {
                NodeType.SOLUTION,
                NodeType.PROJECT,
                NodeType.PHYSICAL_STRUCTURE,
                NodeType.DIRECTORY,
                NodeType.FILE -> true
                else -> false
            }
        }
    }
    
    /**
     * 应用类型视图过滤（只显示类型定义）
     */
    private fun applyTypesViewFilter() {
        structureTree.filter { node ->
            when (node.type) {
                NodeType.SOLUTION,
                NodeType.PROJECT,
                NodeType.CLASS,
                NodeType.INTERFACE,
                NodeType.ENUM,
                NodeType.STRUCT -> true
                else -> false
            }
        }
    }
    
    /**
     * 应用依赖关系视图过滤（只显示项目和依赖）
     */
    private fun applyDependenciesViewFilter() {
        structureTree.filter { node ->
            when (node.type) {
                NodeType.SOLUTION,
                NodeType.PROJECT,
                NodeType.DEPENDENCY -> true
                else -> false
            }
        }
    }
    
    /**
     * 搜索并定位指定符号
     */
    fun searchAndNavigate(symbolName: String): Boolean {
        val navigationTree = projectNavigator.getCurrentNavigationTree() ?: return false
        
        // 搜索节点
        val nodes = mutableListOf<OmniSharpNavigationNode>()
        findNodesByName(navigationTree.root, symbolName, nodes)
        
        if (nodes.isNotEmpty()) {
            // 导航到第一个匹配的节点
            structureTree.expandToNode(nodes[0].id)
            return true
        }
        
        return false
    }
    
    /**
     * 递归查找指定名称的节点
     */
    private fun findNodesByName(node: OmniSharpNavigationNode, name: String, result: MutableList<OmniSharpNavigationNode>) {
        if (node.name.contains(name, ignoreCase = true)) {
            result.add(node)
        }
        
        node.getChildren().forEach { child ->
            findNodesByName(child, name, result)
        }
    }
    
    /**
     * 注册事件监听器
     */
    private fun registerEventListeners() {
        // 监听项目文件变化
        OmniSharpFileListener.getInstance(project).addListener {
            // 文件变化时刷新树
            structureTree.refresh()
        }
    }
    
    /**
     * 应用过滤条件
     */
    fun applyFilter(filterText: String, filterTypes: Set<NodeType>) {
        structureTree.filter {
            // 名称过滤
            val nameFilter = filterText.isEmpty() || it.name.contains(filterText, ignoreCase = true)
            
            // 类型过滤
            val typeFilter = filterTypes.isEmpty() || filterTypes.contains(it.type)
            
            nameFilter && typeFilter
        }
    }
    
    /**
     * 清除所有过滤
     */
    fun clearFilters() {
        structureTree.filter { true }
        setViewMode(currentViewMode) // 重新应用视图模式过滤
    }
    
    /**
     * 导航到指定文件
     */
    fun navigateToFile(filePath: String) {
        val navigationTree = projectNavigator.getCurrentNavigationTree() ?: return
        
        // 查找文件节点
        val fileNode = findFileNode(navigationTree.root, filePath)
        if (fileNode != null) {
            structureTree.expandToNode(fileNode.id)
        }
    }
    
    /**
     * 递归查找文件节点
     */
    private fun findFileNode(node: OmniSharpNavigationNode, filePath: String): OmniSharpNavigationNode? {
        if (node.type == NodeType.FILE && node.model == filePath) {
            return node
        }
        
        for (child in node.getChildren()) {
            val result = findFileNode(child, filePath)
            if (result != null) {
                return result
            }
        }
        
        return null
    }
}

/**
 * 视图模式枚举
 */
enum class ViewMode {
    LOGICAL,     // 逻辑结构视图（按命名空间）
    PHYSICAL,    // 物理结构视图（按文件系统）
    TYPES,       // 类型视图（只显示类型定义）
    DEPENDENCIES // 依赖关系视图
}
```

### 3.4 FilterManager

FilterManager 负责管理和应用过滤条件，帮助用户快速定位和查找项目元素：

```kotlin
package com.omnisharp.intellij.navigation

import com.omnisharp.intellij.model.*
import java.util.regex.*

/**
 * 过滤管理器，处理项目结构树的过滤逻辑
 */
class OmniSharpFilterManager {
    private val activeFilters = mutableMapOf<String, (OmniSharpNavigationNode) -> Boolean>()
    
    /**
     * 添加名称过滤
     */
    fun addNameFilter(filterId: String, namePattern: String): OmniSharpFilterManager {
        val pattern = Pattern.compile(namePattern, Pattern.CASE_INSENSITIVE)
        
        activeFilters[filterId] = { node ->
            pattern.matcher(node.name).find()
        }
        
        return this
    }
    
    /**
     * 添加类型过滤
     */
    fun addTypeFilter(filterId: String, vararg types: NodeType): OmniSharpFilterManager {
        val typeSet = types.toSet()
        
        activeFilters[filterId] = { node ->
            typeSet.contains(node.type)
        }
        
        return this
    }
    
    /**
     * 添加自定义过滤
     */
    fun addCustomFilter(filterId: String, filter: (OmniSharpNavigationNode) -> Boolean): OmniSharpFilterManager {
        activeFilters[filterId] = filter
        return this
    }
    
    /**
     * 移除过滤
     */
    fun removeFilter(filterId: String): OmniSharpFilterManager {
        activeFilters.remove(filterId)
        return this
    }
    
    /**
     * 清除所有过滤
     */
    fun clearAllFilters(): OmniSharpFilterManager {
        activeFilters.clear()
        return this
    }
    
    /**
     * 获取组合过滤函数
     */
    fun getCombinedFilter(): (OmniSharpNavigationNode) -> Boolean {
        return { node ->
            activeFilters.values.all { filter -> filter(node) }
        }
    }
    
    /**
     * 检查是否有活动的过滤
     */
    fun hasActiveFilters(): Boolean {
         return activeFilters.isNotEmpty()
     }
 }
```

### 3.5 Visualization 组件

Visualization 组件负责项目结构和依赖关系的图形化展示：

```kotlin
package com.omnisharp.intellij.navigation.visualization

import com.intellij.openapi.project.*
import com.intellij.ui.jcef.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.services.*
import org.cef.*
import org.cef.browser.*
import org.cef.callback.*
import org.cef.handler.*
import java.awt.*
import javax.swing.*

/**
 * OmniSharp 项目结构可视化组件
 */
class OmniSharpVisualizationComponent(val project: Project, val dependencyManager: OmniSharpDependencyManager) {
    private var browser: JBCefBrowser? = null
    
    /**
     * 创建可视化面板
     */
    fun createVisualizationPanel(): JComponent {
        if (JCEFApp.isSupported()) {
            // 使用 JCEF 浏览器渲染可视化
            browser = JBCefBrowser()
            return browser!!.component
        } else {
            // 回退到简单的 Swing 组件
            return createFallbackPanel()
        }
    }
    
    /**
     * 回退面板（当 JCEF 不支持时）
     */
    private fun createFallbackPanel(): JPanel {
        val panel = JPanel(BorderLayout())
        val label = JLabel("项目依赖可视化需要 JCEF 支持")
        label.horizontalAlignment = JLabel.CENTER
        panel.add(label, BorderLayout.CENTER)
        return panel
    }
    
    /**
     * 可视化项目依赖关系
     */
    fun visualizeDependencies(solution: OmniSharpSolutionModel) {
        if (browser == null || !JCEFApp.isSupported()) {
            return
        }
        
        // 构建依赖图数据
        val dependencyGraph = dependencyManager.getDependencyGraph(solution)
        val jsonData = convertToJson(dependencyGraph)
        
        // 使用 HTML/JavaScript 渲染可视化
        val html = generateVisualizationHtml(jsonData)
        browser!!.loadHTML(html)
    }
    
    /**
     * 可视化项目结构
     */
    fun visualizeProjectStructure(solution: OmniSharpSolutionModel) {
        if (browser == null || !JCEFApp.isSupported()) {
            return
        }
        
        // 构建项目结构数据
        val structureData = buildStructureData(solution)
        val jsonData = convertToJson(structureData)
        
        // 使用 HTML/JavaScript 渲染可视化
        val html = generateStructureVisualizationHtml(jsonData)
        browser!!.loadHTML(html)
    }
    
    /**
     * 将依赖图转换为 JSON
     */
    private fun convertToJson(graph: OmniSharpDependencyGraph): String {
        val nodes = graph.nodes.joinToString(",") {
            "{\"id\":\"${it.id}\",\"name\":\"${it.name}\",\"type\":\"${it.type}\"}"
        }
        
        val edges = graph.edges.joinToString(",") {
            "{\"source\":\"${it.source.id}\",\"target\":\"${it.target.id}\",\"type\":\"${it.type}\"}"
        }
        
        return "{\"nodes\":[$nodes],\"edges\":[$edges]}"
    }
    
    /**
     * 生成依赖关系可视化 HTML
     */
    private fun generateVisualizationHtml(jsonData: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>OmniSharp 依赖可视化</title>
            <script src="https://d3js.org/d3.v7.min.js"></script>
            <style>
                body { margin: 0; padding: 20px; font-family: Arial, sans-serif; }
                .node { stroke: #fff; stroke-width: 1.5px; }
                .link { stroke: #999; stroke-opacity: 0.6; stroke-width: 2px; }
                .tooltip { position: absolute; background: rgba(0,0,0,0.8); color: white; padding: 5px; border-radius: 3px; pointer-events: none; }
                .controls { margin-bottom: 20px; }
                .node.project { fill: #3498db; }
                .node.package { fill: #2ecc71; }
                .node.assembly { fill: #e74c3c; }
            </style>
        </head>
        <body>
            <div class="controls">
                <button id="zoomIn">放大</button>
                <button id="zoomOut">缩小</button>
                <button id="resetZoom">重置缩放</button>
            </div>
            <svg width="100%" height="800"></svg>
            <script>
                const data = $jsonData;
                const svg = d3.select("svg");
                const width = +svg.attr("width");
                const height = +svg.attr("height");
                const tooltip = d3.select("body").append("div").attr("class", "tooltip");
                
                // 创建缩放行为
                const zoom = d3.zoom()
                    .scaleExtent([0.1, 4])
                    .on("zoom", (event) => {
                        g.attr("transform", event.transform);
                    });
                
                svg.call(zoom);
                
                // 创建容器
                const g = svg.append("g");
                
                // 创建力导向图
                const simulation = d3.forceSimulation(data.nodes)
                    .force("link", d3.forceLink(data.edges).id(d => d.id).distance(150))
                    .force("charge", d3.forceManyBody().strength(-300))
                    .force("center", d3.forceCenter(width / 2, height / 2));
                
                // 绘制连接线
                const link = g.append("g")
                    .selectAll("line")
                    .data(data.edges)
                    .enter()
                    .append("line")
                    .attr("class", "link")
                    .style("stroke-width", d => Math.sqrt(d.value || 1));
                
                // 绘制节点
                const node = g.append("g")
                    .selectAll("circle")
                    .data(data.nodes)
                    .enter()
                    .append("circle")
                    .attr("class", d => `node ${d.type}`)
                    .attr("r", 15)
                    .call(d3.drag()
                        .on("start", dragstarted)
                        .on("drag", dragged)
                        .on("end", dragended))
                    .on("mouseover", (event, d) => {
                        tooltip.html(d.name + "<br/>" + d.type)
                            .style("left", (event.pageX + 10) + "px")
                            .style("top", (event.pageY - 28) + "px")
                            .style("opacity", 1);
                    })
                    .on("mouseout", () => {
                        tooltip.style("opacity", 0);
                    });
                
                // 添加节点标签
                g.append("g")
                    .selectAll("text")
                    .data(data.nodes)
                    .enter()
                    .append("text")
                    .text(d => d.name)
                    .attr("text-anchor", "middle")
                    .attr("dy", ".35em")
                    .style("font-size", "10px")
                    .style("fill", "white");
                
                // 更新力导向图
                simulation.on("tick", () => {
                    link
                        .attr("x1", d => d.source.x)
                        .attr("y1", d => d.source.y)
                        .attr("x2", d => d.target.x)
                        .attr("y2", d => d.target.y);
                    
                    node
                        .attr("cx", d => d.x)
                        .attr("cy", d => d.y);
                    
                    g.selectAll("text")
                        .attr("x", d => d.x)
                        .attr("y", d => d.y);
                });
                
                // 拖拽函数
                function dragstarted(event, d) {
                    if (!event.active) simulation.alphaTarget(0.3).restart();
                    d.fx = d.x;
                    d.fy = d.y;
                }
                
                function dragged(event, d) {
                    d.fx = event.x;
                    d.fy = event.y;
                }
                
                function dragended(event, d) {
                    if (!event.active) simulation.alphaTarget(0);
                    d.fx = null;
                    d.fy = null;
                }
                
                // 控制按钮
                document.getElementById("zoomIn").onclick = () => {
                    svg.transition().call(zoom.scaleBy, 1.2);
                };
                
                document.getElementById("zoomOut").onclick = () => {
                    svg.transition().call(zoom.scaleBy, 0.8);
                };
                
                document.getElementById("resetZoom").onclick = () => {
                    svg.transition().call(zoom.transform, d3.zoomIdentity);
                };
            </script>
        </body>
        </html>
        """
    }
    
    /**
     * 构建项目结构数据
     */
    private fun buildStructureData(solution: OmniSharpSolutionModel): Map<String, Any> {
        val structure = mutableMapOf<String, Any>()
        val projects = mutableListOf<Map<String, Any>>()
        
        solution.projects.forEach { project ->
            val projectData = mutableMapOf(
                "name" to project.name,
                "type" to project.type,
                "fileCount" to project.files.size,
                "lastModified" to project.lastModified
            )
            projects.add(projectData)
        }
        
        structure["name"] = solution.name
        structure["projects"] = projects
        structure["projectCount"] = solution.projects.size
        
        return structure
    }
    
    /**
     * 生成项目结构可视化 HTML
     */
    private fun generateStructureVisualizationHtml(jsonData: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <title>OmniSharp 项目结构可视化</title>
            <script src="https://d3js.org/d3.v7.min.js"></script>
            <style>
                body { margin: 0; padding: 20px; font-family: Arial, sans-serif; }
                .chart-container { margin-top: 20px; }
                .bar { fill: #3498db; }
                .bar:hover { fill: #2980b9; }
                .axis-label { font-size: 12px; }
                .tooltip { position: absolute; background: rgba(0,0,0,0.8); color: white; padding: 5px; border-radius: 3px; pointer-events: none; }
            </style>
        </head>
        <body>
            <h1 id="solutionName"></h1>
            <div class="stats">
                <p><strong>项目数量:</strong> <span id="projectCount"></span></p>
            </div>
            
            <div class="chart-container">
                <h3>各项目文件数量</h3>
                <svg width="100%" height="400"></svg>
            </div>
            
            <script>
                const data = $jsonData;
                
                // 更新统计信息
                document.getElementById("solutionName").textContent = data.name;
                document.getElementById("projectCount").textContent = data.projectCount;
                
                // 创建文件数量条形图
                const svg = d3.select("svg");
                const width = +svg.attr("width");
                const height = +svg.attr("height");
                const margin = {top: 20, right: 30, bottom: 100, left: 40};
                
                const tooltip = d3.select("body").append("div")
                    .attr("class", "tooltip")
                    .style("opacity", 0);
                
                // 创建比例尺
                const x = d3.scaleBand()
                    .domain(data.projects.map(d => d.name))
                    .range([margin.left, width - margin.right])
                    .padding(0.1);
                
                const y = d3.scaleLinear()
                    .domain([0, d3.max(data.projects, d => d.fileCount)])
                    .range([height - margin.bottom, margin.top]);
                
                // 添加坐标轴
                svg.append("g")
                    .attr("transform", `translate(0,${height - margin.bottom})`)
                    .call(d3.axisBottom(x))
                    .selectAll("text")
                    .attr("transform", "rotate(-45)")
                    .style("text-anchor", "end")
                    .style("font-size", "10px");
                
                svg.append("g")
                    .attr("transform", `translate(${margin.left},0)`)
                    .call(d3.axisLeft(y));
                
                // 添加坐标轴标签
                svg.append("text")
                    .attr("class", "axis-label")
                    .attr("transform", `translate(${width/2},${height - margin.bottom/3})`)
                    .text("项目");
                
                svg.append("text")
                    .attr("class", "axis-label")
                    .attr("transform", "rotate(-90)")
                    .attr("y", margin.left/2)
                    .attr("x", -height/2)
                    .text("文件数量");
                
                // 添加条形
                svg.selectAll(".bar")
                    .data(data.projects)
                    .enter()
                    .append("rect")
                    .attr("class", "bar")
                    .attr("x", d => x(d.name))
                    .attr("y", d => y(d.fileCount))
                    .attr("width", x.bandwidth())
                    .attr("height", d => height - margin.bottom - y(d.fileCount))
                    .on("mouseover", (event, d) => {
                        tooltip.html(`项目: ${d.name}<br/>文件数量: ${d.fileCount}<br/>类型: ${d.type}`)
                            .style("left", (event.pageX + 10) + "px")
                            .style("top", (event.pageY - 28) + "px")
                            .style("opacity", 1);
                    })
                    .on("mouseout", () => {
                        tooltip.style("opacity", 0);
                    });
            </script>
        </body>
        </html>
        """
    }
    
    /**
     * 清理资源
     */
    fun dispose() {
        browser?.dispose()
    }
}
```

## 4. IntelliJ 平台集成

### 4.1 工具栏实现

实现用于切换视图模式、过滤和搜索的工具栏：

```kotlin
package com.omnisharp.intellij.ui

import com.intellij.icons.*
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.omnisharp.intellij.navigation.*
import java.awt.*
import javax.swing.*

/**
 * OmniSharp 项目结构导航工具栏
 */
class OmniSharpNavigationToolbar(
    private val project: Project,
    private val viewController: OmniSharpViewController
) : JPanel(BorderLayout()) {
    private val filterField: JTextField
    private val searchField: JTextField
    
    init {
        // 创建工具栏
        val toolbar = JToolBar()
        toolbar.isFloatable = false
        
        // 添加视图模式按钮
        addViewModeButtons(toolbar)
        
        // 添加分隔符
        toolbar.addSeparator()
        
        // 添加过滤字段
        toolbar.add(JLabel("过滤: "))
        filterField = JTextField(20)
        filterField.toolTipText = "输入过滤文本"
        filterField.addActionListener { applyFilter() }
        toolbar.add(filterField)
        
        // 添加搜索字段
        toolbar.add(JLabel("  搜索: "))
        searchField = JTextField(20)
        searchField.toolTipText = "搜索符号"
        searchField.addActionListener { performSearch() }
        toolbar.add(searchField)
        
        // 添加清除按钮
        val clearButton = JButton(Icons.ACTION_CLEAR)
        clearButton.toolTipText = "清除过滤和搜索"
        clearButton.addActionListener { clearAll() }
        toolbar.add(clearButton)
        
        add(toolbar, BorderLayout.CENTER)
    }
    
    /**
     * 添加视图模式按钮
     */
    private fun addViewModeButtons(toolbar: JToolBar) {
        // 逻辑视图按钮
        val logicalViewButton = JButton("逻辑视图")
        logicalViewButton.toolTipText = "按命名空间查看"
        logicalViewButton.addActionListener { 
            viewController.setViewMode(ViewMode.LOGICAL)
        }
        toolbar.add(logicalViewButton)
        
        // 物理视图按钮
        val physicalViewButton = JButton("文件视图")
        physicalViewButton.toolTipText = "按文件系统查看"
        physicalViewButton.addActionListener { 
            viewController.setViewMode(ViewMode.PHYSICAL)
        }
        toolbar.add(physicalViewButton)
        
        // 类型视图按钮
        val typesViewButton = JButton("类型视图")
        typesViewButton.toolTipText = "仅查看类型定义"
        typesViewButton.addActionListener { 
            viewController.setViewMode(ViewMode.TYPES)
        }
        toolbar.add(typesViewButton)
        
        // 依赖视图按钮
        val dependenciesViewButton = JButton("依赖视图")
        dependenciesViewButton.toolTipText = "查看项目依赖"
        dependenciesViewButton.addActionListener { 
            viewController.setViewMode(ViewMode.DEPENDENCIES)
        }
        toolbar.add(dependenciesViewButton)
    }
    
    /**
     * 应用过滤
     */
    private fun applyFilter() {
        val filterText = filterField.text.trim()
        // 可以根据需要添加类型过滤
        viewController.applyFilter(filterText, emptySet())
    }
    
    /**
     * 执行搜索
     */
    private fun performSearch() {
        val searchText = searchField.text.trim()
        if (searchText.isNotEmpty()) {
            val found = viewController.searchAndNavigate(searchText)
            if (!found) {
                Messages.showInfoMessage(project, "未找到匹配的符号: $searchText", "搜索结果")
            }
        }
    }
    
    /**
     * 清除所有过滤和搜索
     */
    private fun clearAll() {
        filterField.text = ""
        searchField.text = ""
        viewController.clearFilters()
    }
}
```

### 4.2 工具窗口实现

实现项目结构导航的工具窗口：

```kotlin
package com.omnisharp.intellij.ui

import com.intellij.openapi.project.*
import com.intellij.openapi.ui.*
import com.intellij.openapi.wm.*
import com.intellij.ui.content.*
import com.omnisharp.intellij.navigation.*
import com.omnisharp.intellij.navigation.visualization.*
import com.omnisharp.intellij.services.*
import java.awt.*
import javax.swing.*

/**
 * OmniSharp 项目结构导航工具窗口
 */
class OmniSharpNavigationToolWindow(val project: Project) : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentManager = toolWindow.contentManager
        
        // 创建导航视图内容
        val navigationContent = createNavigationContent()
        contentManager.addContent(navigationContent)
        
        // 创建可视化视图内容
        val visualizationContent = createVisualizationContent()
        contentManager.addContent(visualizationContent)
    }
    
    /**
     * 创建导航视图内容
     */
    private fun createNavigationContent(): Content {
        val panel = JPanel(BorderLayout())
        
        // 获取服务
        val projectManager = OmniSharpProjectManager.getInstance(project)
        val symbolManager = OmniSharpSymbolManager.getInstance(project)
        val dependencyManager = OmniSharpDependencyManager.getInstance(project)
        
        // 创建导航器
        val projectNavigator = OmniSharpProjectNavigator(project, projectManager, symbolManager, dependencyManager)
        
        // 创建结构树
        val structureTree = OmniSharpStructureTree(project, projectNavigator)
        val treeComponent = structureTree.createStructureTree()
        
        // 创建工具栏
        val viewController = OmniSharpViewController(project, projectNavigator, structureTree)
        viewController.initialize()
        val toolbar = OmniSharpNavigationToolbar(project, viewController)
        
        // 布局组件
        panel.add(toolbar, BorderLayout.NORTH)
        panel.add(JScrollPane(treeComponent), BorderLayout.CENTER)
        
        // 加载解决方案
        projectManager.getCurrentSolution()?.let {
            projectNavigator.buildNavigationTree(it)
            structureTree.refresh()
        }
        
        // 创建内容
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "项目结构", false)
        
        return content
    }
    
    /**
     * 创建可视化视图内容
     */
    private fun createVisualizationContent(): Content {
        val panel = JPanel(BorderLayout())
        
        // 创建可视化控制面板
        val controlPanel = createVisualizationControlPanel(panel)
        panel.add(controlPanel, BorderLayout.NORTH)
        
        // 创建内容
        val contentFactory = ContentFactory.SERVICE.getInstance()
        val content = contentFactory.createContent(panel, "项目可视化", false)
        
        return content
    }
    
    /**
     * 创建可视化控制面板
     */
    private fun createVisualizationControlPanel(contentPanel: JPanel): JPanel {
        val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
        
        // 依赖可视化按钮
        val dependencyButton = JButton("显示依赖关系")
        dependencyButton.addActionListener {
            val dependencyManager = OmniSharpDependencyManager.getInstance(project)
            val projectManager = OmniSharpProjectManager.getInstance(project)
            
            projectManager.getCurrentSolution()?.let {
                // 移除旧的可视化组件
                contentPanel.removeAll()
                contentPanel.add(controlPanel, BorderLayout.NORTH)
                
                // 创建并添加新的可视化组件
                val visualizationComponent = OmniSharpVisualizationComponent(project, dependencyManager)
                contentPanel.add(visualizationComponent.createVisualizationPanel(), BorderLayout.CENTER)
                
                // 可视化依赖关系
                visualizationComponent.visualizeDependencies(it)
                
                contentPanel.revalidate()
                contentPanel.repaint()
            }
        }
        controlPanel.add(dependencyButton)
        
        // 项目结构可视化按钮
        val structureButton = JButton("显示项目结构")
        structureButton.addActionListener {
            val dependencyManager = OmniSharpDependencyManager.getInstance(project)
            val projectManager = OmniSharpProjectManager.getInstance(project)
            
            projectManager.getCurrentSolution()?.let {
                // 移除旧的可视化组件
                contentPanel.removeAll()
                contentPanel.add(controlPanel, BorderLayout.NORTH)
                
                // 创建并添加新的可视化组件
                val visualizationComponent = OmniSharpVisualizationComponent(project, dependencyManager)
                contentPanel.add(visualizationComponent.createVisualizationPanel(), BorderLayout.CENTER)
                
                // 可视化项目结构
                visualizationComponent.visualizeProjectStructure(it)
                
                contentPanel.revalidate()
                contentPanel.repaint()
            }
        }
        controlPanel.add(structureButton)
        
        return controlPanel
    }
}
```

## 5. 集成与使用示例

### 5.1 项目结构导航使用示例

```kotlin
package com.omnisharp.intellij.navigation

import com.intellij.openapi.project.*
import com.omnisharp.intellij.model.*
import com.omnisharp.intellij.services.*

class OmniSharpNavigationExample {
    fun navigateExample(project: Project) {
        // 获取服务
        val projectManager = OmniSharpProjectManager.getInstance(project)
        val symbolManager = OmniSharpSymbolManager.getInstance(project)
        val dependencyManager = OmniSharpDependencyManager.getInstance(project)
        
        // 创建导航器
        val projectNavigator = OmniSharpProjectNavigator(project, projectManager, symbolManager, dependencyManager)
        
        // 加载解决方案
        val solution = projectManager.loadSolution("c:\\Projects\\MySolution.sln")
        
        // 构建导航树
        val navigationTree = projectNavigator.buildNavigationTree(solution)
        
        // 创建结构树和视图控制器
        val structureTree = OmniSharpStructureTree(project, projectNavigator)
        val viewController = OmniSharpViewController(project, projectNavigator, structureTree)
        
        // 切换视图模式
        viewController.setViewMode(ViewMode.LOGICAL)  // 逻辑视图
        // viewController.setViewMode(ViewMode.PHYSICAL)  // 物理视图
        // viewController.setViewMode(ViewMode.TYPES)  // 类型视图
        // viewController.setViewMode(ViewMode.DEPENDENCIES)  // 依赖视图
        
        // 搜索符号
        viewController.searchAndNavigate("User")
        
        // 应用过滤
        viewController.applyFilter("Model", setOf(NodeType.CLASS, NodeType.INTERFACE))
        
        // 清除过滤
        viewController.clearFilters()
        
        // 导航到特定文件
        viewController.navigateToFile("c:\\Projects\\MySolution\\MyProject\\Models\\User.cs")
    }
}
```

### 5.2 项目结构可视化使用示例

```kotlin
package com.omnisharp.intellij.navigation.visualization

import com.intellij.openapi.project.*
import com.omnisharp.intellij.services.*
import javax.swing.*

class OmniSharpVisualizationExample {
    fun visualizeExample(project: Project, container: JComponent) {
        // 获取服务
        val projectManager = OmniSharpProjectManager.getInstance(project)
        val dependencyManager = OmniSharpDependencyManager.getInstance(project)
        
        // 加载解决方案
        val solution = projectManager.loadSolution("c:\\Projects\\MySolution.sln")
        
        // 创建可视化组件
        val visualizationComponent = OmniSharpVisualizationComponent(project, dependencyManager)
        
        // 添加到容器
        container.removeAll()
        container.add(visualizationComponent.createVisualizationPanel())
        container.revalidate()
        container.repaint()
        
        // 可视化依赖关系
        visualizationComponent.visualizeDependencies(solution)
        
        // 或者可视化项目结构
        // visualizationComponent.visualizeProjectStructure(solution)
    }
}
```

## 6. 测试用例

### 6.1 ProjectNavigator 测试

```kotlin
package com.omnisharp.intellij.navigation

import com.omnisharp.intellij.model.*
import org.junit.*
import kotlin.test.*

class OmniSharpProjectNavigatorTest {
    private lateinit var projectNavigator: OmniSharpProjectNavigator
    
    @Before
    fun setUp() {
        // 创建测试服务
        val projectManager = TestProjectManager()
        val symbolManager = TestSymbolManager()
        val dependencyManager = TestDependencyManager()
        
        // 模拟项目
        projectNavigator = OmniSharpProjectNavigator(
            project = null as Any as Project,
            projectManager = projectManager,
            symbolManager = symbolManager,
            dependencyManager = dependencyManager
        )
    }
    
    @Test
    fun testBuildNavigationTree() {
        // 创建测试解决方案
        val solution = createTestSolution()
        
        // 构建导航树
        val navigationTree = projectNavigator.buildNavigationTree(solution)
        
        // 验证根节点
        assertEquals(NodeType.SOLUTION, navigationTree.root.type)
        assertEquals("TestSolution", navigationTree.root.name)
        
        // 验证项目节点
        val projectNodes = navigationTree.root.getChildren()
        assertEquals(1, projectNodes.size)
        assertEquals(NodeType.PROJECT, projectNodes[0].type)
        assertEquals("TestProject", projectNodes[0].name)
        
        // 验证逻辑结构和物理结构节点
        val projectNodeChildren = projectNodes[0].getChildren()
        assertEquals(2, projectNodeChildren.size) // 逻辑结构和物理结构
        
        // 查找节点
        val foundNode = navigationTree.findNodeById("TestProject")
        assertNotNull(foundNode)
        assertEquals("TestProject", foundNode.name)
    }
    
    private fun createTestSolution(): OmniSharpSolutionModel {
        val project = OmniSharpProjectModel(
            name = "TestProject",
            path = "TestProject",
            type = "C#",
            configuration = "Debug",
            platform = "Any CPU",
            references = emptyList(),
            files = listOf("c:\\test\\TestProject\\Models\\User.cs"),
            lastModified = System.currentTimeMillis()
        )
        
        return OmniSharpSolutionModel(
            name = "TestSolution",
            path = "TestSolution",
            projects = listOf(project),
            configuration = "Debug",
            platform = "Any CPU",
            lastModified = System.currentTimeMillis()
        )
    }
    
    // 测试服务实现
    private class TestProjectManager : OmniSharpProjectManager {
        // 实现测试用的项目管理器
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
        
        override fun getCurrentSolution(): OmniSharpSolutionModel? {
            return null
        }
        
        override fun loadSolution(solutionPath: String): OmniSharpSolutionModel {
            return createTestSolution()
        }
    }
    
    private class TestSymbolManager : OmniSharpSymbolManager {
        // 实现测试用的符号管理器
        override fun buildIndex(solution: OmniSharpSolutionModel) {
            // 不实现
        }
        
        override fun searchSymbols(pattern: String, caseSensitive: Boolean): List<OmniSharpSymbol> {
            return emptyList()
        }
        
        override fun getSymbolsForProject(project: OmniSharpProjectModel): List<OmniSharpSymbol> {
            return emptyList()
        }
    }
    
    private class TestDependencyManager : OmniSharpDependencyManager {
        // 实现测试用的依赖管理器
        override fun analyzeDependencies(solution: OmniSharpSolutionModel): OmniSharpDependencyGraph {
            return OmniSharpDependencyGraph(emptyList(), emptyList())
        }
        
        override fun getDependencyGraph(solution: OmniSharpSolutionModel): OmniSharpDependencyGraph {
            return OmniSharpDependencyGraph(emptyList(), emptyList())
        }
    }
    
    private class TestFileParser : OmniSharpFileParser {
        override fun parseFile(filePath: String): OmniSharpParseResult {
            return OmniSharpParseResult(filePath, emptyList())
        }
    }
}
```

## 7. 总结与后续优化

### 7.1 已实现功能

本文档详细描述了 OmniSharp for IntelliJ 平台中项目结构导航和可视化功能的实现方案，包括：

1. **灵活的项目结构导航**：支持逻辑结构、物理结构、类型视图和依赖视图等多种导航模式
2. **高效的过滤和搜索**：提供强大的过滤和搜索功能，帮助用户快速定位项目元素
3. **直观的可视化展示**：使用 D3.js 实现项目结构和依赖关系的图形化展示
4. **与 IntelliJ 平台深度集成**：实现工具窗口、工具栏等 UI 组件，提供良好的用户体验
5. **可扩展的架构设计**：模块化的组件设计，便于后续功能扩展和性能优化

### 7.2 后续优化方向

1. **性能优化**：对于大型解决方案，可以实现虚拟列表和增量渲染，提高导航树的加载和渲染性能
2. **更多可视化选项**：增加更多的可视化选项，如 UML 类图、调用图等
3. **自定义视图**：允许用户自定义导航视图，保存和恢复视图配置
4. **智能导航建议**：基于用户的使用习惯，提供智能导航建议和快捷键
5. **协作功能**：支持团队协作场景下的项目结构共享和讨论

### 7.3 输入输出示例

#### 输入输出示例

输入：
```kotlin
// 创建并初始化项目导航器
val projectNavigator = OmniSharpProjectNavigator(project, projectManager, symbolManager, dependencyManager)
val solution = projectManager.loadSolution("c:\\Projects\\MySolution.sln")
val navigationTree = projectNavigator.buildNavigationTree(solution)

// 切换到逻辑视图
val structureTree = OmniSharpStructureTree(project, projectNavigator)
val viewController = OmniSharpViewController(project, projectNavigator, structureTree)
viewController.setViewMode(ViewMode.LOGICAL)

// 搜索符号
viewController.searchAndNavigate("User")
```

输出：
```
// 成功导航到 "User" 类，在 UI 中展开并高亮显示该节点
// 导航树的结构类似于：
// MySolution
//   └── MyProject
//       └── Logical Structure
//           └── MyProject.Models
//               └── User (高亮显示)
```

本实现方案提供了一个功能完整、用户友好的项目结构导航和可视化系统，为 OmniSharp for IntelliJ 平台提供了强大的项目结构浏览和分析能力。通过多种视图模式、过滤搜索功能和直观的可视化展示，帮助开发者更高效地理解和操作 C# 项目结构。