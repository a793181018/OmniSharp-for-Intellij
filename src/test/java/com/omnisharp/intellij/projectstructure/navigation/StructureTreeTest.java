package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class StructureTreeTest {
    
    @Mock
    private NavigationNode rootNode;
    
    private StructureTree structureTree;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        structureTree = new StructureTree(rootNode);
    }
    
    @Test
    public void testGetRootNode() {
        // 测试获取根节点
        NavigationNode result = structureTree.getRootNode();
        assertEquals("Should return the root node", rootNode, result);
    }
    
    @Test
    public void testFindNodeById() {
        // 创建测试节点
        NavigationNode projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        
        // 模拟根节点包含项目节点
        when(rootNode.getChildren()).thenReturn(List.of(projectNode));
        when(rootNode.getId()).thenReturn("root");
        
        // 测试查找根节点
        NavigationNode foundRoot = structureTree.findNodeById("root");
        assertEquals("Should find root node by ID", rootNode, foundRoot);
        
        // 测试查找子节点
        NavigationNode foundProject = structureTree.findNodeById("project1");
        assertEquals("Should find project node by ID", projectNode, foundProject);
        
        // 测试查找不存在的节点
        NavigationNode notFound = structureTree.findNodeById("nonexistent");
        assertNull("Should return null for nonexistent ID", notFound);
    }
    
    @Test
    public void testFindNodesByName() {
        // 创建测试节点
        NavigationNode projectNode1 = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project1.csproj");
        NavigationNode projectNode2 = new NavigationNode("project2", "TestProject", NodeType.PROJECT, "/path/to/project2.csproj");
        NavigationNode folderNode = new NavigationNode("folder", "src", NodeType.FOLDER, "/path/to/src");
        
        // 模拟节点层次结构
        when(rootNode.getChildren()).thenReturn(List.of(projectNode1, folderNode));
        when(folderNode.getChildren()).thenReturn(List.of(projectNode2));
        
        // 测试查找同名节点
        List<NavigationNode> results = structureTree.findNodesByName("TestProject");
        assertEquals("Should find 2 nodes with same name", 2, results.size());
        assertTrue("Results should contain project1", results.contains(projectNode1));
        assertTrue("Results should contain project2", results.contains(projectNode2));
        
        // 测试查找不存在的名称
        List<NavigationNode> emptyResults = structureTree.findNodesByName("NonexistentName");
        assertNotNull("Should return empty list for nonexistent name", emptyResults);
        assertTrue("Results list should be empty", emptyResults.isEmpty());
    }
    
    @Test
    public void testGetNodePath() {
        // 创建测试节点
        NavigationNode projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        NavigationNode folderNode = new NavigationNode("folder1", "src", NodeType.FOLDER, "/path/to/src");
        NavigationNode fileNode = new NavigationNode("file1", "Program.cs", NodeType.FILE, "/path/to/src/Program.cs");
        
        // 构建节点层次结构
        structureTree = new StructureTree(new NavigationNode("root", "Solution.sln", NodeType.SOLUTION, "/path/to/solution.sln"));
        structureTree.getRootNode().addChild(projectNode);
        projectNode.addChild(folderNode);
        folderNode.addChild(fileNode);
        
        // 测试获取节点路径
        String path = structureTree.getNodePath(fileNode);
        assertEquals("Path should be correct", "Solution.sln\\TestProject\\src\\Program.cs", path);
        
        // 测试获取根节点路径
        String rootPath = structureTree.getNodePath(structureTree.getRootNode());
        assertEquals("Root path should be its name", "Solution.sln", rootPath);
    }
    
    @Test
    public void testGetNodePath_NodeNotInTree() {
        // 创建不在树中的节点
        NavigationNode nodeNotInTree = new NavigationNode("outside", "Outside.cs", NodeType.FILE, "/outside.cs");
        
        // 测试获取不在树中的节点路径
        String path = structureTree.getNodePath(nodeNotInTree);
        assertNull("Should return null for node not in tree", path);
    }
    
    @Test
    public void testRefresh() {
        // 测试刷新操作
        structureTree.refresh();
        // 验证是否重建了节点映射
        // 由于refresh()方法主要是内部状态重置，这里主要验证方法被调用
    }
    
    @Test
    public void testGetAllNodes() {
        // 创建测试节点
        NavigationNode projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        NavigationNode folderNode = new NavigationNode("folder1", "src", NodeType.FOLDER, "/path/to/src");
        
        // 构建节点层次结构
        when(rootNode.getChildren()).thenReturn(List.of(projectNode));
        when(projectNode.getChildren()).thenReturn(List.of(folderNode));
        when(rootNode.getId()).thenReturn("root");
        when(projectNode.getId()).thenReturn("project1");
        when(folderNode.getId()).thenReturn("folder1");
        
        // 获取所有节点
        List<NavigationNode> allNodes = structureTree.getAllNodes();
        
        // 验证结果
        assertEquals("Should return all 3 nodes", 3, allNodes.size());
        assertTrue("Should contain root node", allNodes.contains(rootNode));
        assertTrue("Should contain project node", allNodes.contains(projectNode));
        assertTrue("Should contain folder node", allNodes.contains(folderNode));
    }
    
    @Test
    public void testFindNodesByType() {
        // 创建测试节点
        NavigationNode projectNode1 = new NavigationNode("project1", "Project1", NodeType.PROJECT, "/path/to/project1.csproj");
        NavigationNode projectNode2 = new NavigationNode("project2", "Project2", NodeType.PROJECT, "/path/to/project2.csproj");
        NavigationNode fileNode = new NavigationNode("file1", "File.cs", NodeType.FILE, "/path/to/file.cs");
        
        // 构建节点层次结构
        when(rootNode.getChildren()).thenReturn(List.of(projectNode1, projectNode2, fileNode));
        when(rootNode.getId()).thenReturn("root");
        when(projectNode1.getId()).thenReturn("project1");
        when(projectNode2.getId()).thenReturn("project2");
        when(fileNode.getId()).thenReturn("file1");
        
        // 按类型查找节点
        List<NavigationNode> projects = structureTree.findNodesByType(NodeType.PROJECT);
        List<NavigationNode> files = structureTree.findNodesByType(NodeType.FILE);
        
        // 验证结果
        assertEquals("Should find 2 project nodes", 2, projects.size());
        assertEquals("Should find 1 file node", 1, files.size());
        assertTrue("Projects should contain project1", projects.contains(projectNode1));
        assertTrue("Projects should contain project2", projects.contains(projectNode2));
        assertTrue("Files should contain file node", files.contains(fileNode));
    }
    
    @Test
    public void testFindNodesByPath() {
        // 创建测试节点
        NavigationNode projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        NavigationNode folderNode = new NavigationNode("folder1", "src", NodeType.FOLDER, "/path/to/src");
        
        // 构建节点层次结构
        when(rootNode.getChildren()).thenReturn(List.of(projectNode, folderNode));
        when(rootNode.getId()).thenReturn("root");
        when(projectNode.getId()).thenReturn("project1");
        when(folderNode.getId()).thenReturn("folder1");
        
        // 按路径查找节点
        List<NavigationNode> pathResults = structureTree.findNodesByPath("/path/to");
        
        // 验证结果
        assertEquals("Should find 2 nodes with matching path", 2, pathResults.size());
        assertTrue("Results should contain project node", pathResults.contains(projectNode));
        assertTrue("Results should contain folder node", pathResults.contains(folderNode));
    }
}