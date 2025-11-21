package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class NavigationNodeTest {
    
    private NavigationNode rootNode;
    private NavigationNode projectNode;
    private NavigationNode folderNode;
    private NavigationNode fileNode;
    
    @Before
    public void setUp() {
        // 创建测试节点
        rootNode = new NavigationNode("root", "Solution.sln", NodeType.SOLUTION, "/path/to/solution.sln");
        projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        folderNode = new NavigationNode("folder1", "src", NodeType.FOLDER, "/path/to/src");
        fileNode = new NavigationNode("file1", "Program.cs", NodeType.FILE, "/path/to/src/Program.cs");
        
        // 构建节点层次结构
        rootNode.addChild(projectNode);
        projectNode.addChild(folderNode);
        folderNode.addChild(fileNode);
    }
    
    @Test
    public void testNodeCreation() {
        // 测试节点创建和属性设置
        assertEquals("Node ID should be 'root'", "root", rootNode.getId());
        assertEquals("Node name should be 'Solution.sln'", "Solution.sln", rootNode.getName());
        assertEquals("Node type should be SOLUTION", NodeType.SOLUTION, rootNode.getNodeType());
        assertEquals("Node path should be correct", "/path/to/solution.sln", rootNode.getPath());
    }
    
    @Test
    public void testParentChildRelationship() {
        // 测试父子节点关系
        assertEquals("Project node's parent should be root node", rootNode, projectNode.getParent());
        assertEquals("Folder node's parent should be project node", projectNode, folderNode.getParent());
        assertEquals("File node's parent should be folder node", folderNode, fileNode.getParent());
        
        // 测试子节点列表
        List<NavigationNode> rootChildren = rootNode.getChildren();
        assertEquals("Root should have 1 child", 1, rootChildren.size());
        assertEquals("Root's child should be project node", projectNode, rootChildren.get(0));
        
        List<NavigationNode> projectChildren = projectNode.getChildren();
        assertEquals("Project should have 1 child", 1, projectChildren.size());
        assertEquals("Project's child should be folder node", folderNode, projectChildren.get(0));
    }
    
    @Test
    public void testAddChild() {
        // 创建新节点并添加
        NavigationNode newNode = new NavigationNode("new", "NewFile.cs", NodeType.FILE, "/path/to/src/NewFile.cs");
        folderNode.addChild(newNode);
        
        // 验证添加结果
        List<NavigationNode> folderChildren = folderNode.getChildren();
        assertEquals("Folder should now have 2 children", 2, folderChildren.size());
        assertTrue("Folder should contain new node", folderChildren.contains(newNode));
        assertEquals("New node's parent should be folder", folderNode, newNode.getParent());
    }
    
    @Test
    public void testRemoveChild() {
        // 移除子节点
        boolean removed = folderNode.removeChild(fileNode);
        
        // 验证移除结果
        assertTrue("Should return true when child is removed", removed);
        List<NavigationNode> folderChildren = folderNode.getChildren();
        assertEquals("Folder should have 0 children after removal", 0, folderChildren.size());
        assertFalse("Folder should not contain removed node", folderChildren.contains(fileNode));
        assertNull("Removed node's parent should be null", fileNode.getParent());
    }
    
    @Test
    public void testRemoveChild_NotFound() {
        // 尝试移除非子节点
        NavigationNode unrelatedNode = new NavigationNode("unrelated", "Unrelated.cs", NodeType.FILE, "/unrelated.cs");
        boolean removed = folderNode.removeChild(unrelatedNode);
        
        // 验证结果
        assertFalse("Should return false when trying to remove non-child", removed);
        List<NavigationNode> folderChildren = folderNode.getChildren();
        assertEquals("Folder should still have 1 child", 1, folderChildren.size());
        assertEquals("Folder should still contain original child", fileNode, folderChildren.get(0));
    }
    
    @Test
    public void testIsLeaf() {
        // 测试叶节点判断
        assertFalse("Root node should not be a leaf", rootNode.isLeaf());
        assertFalse("Project node should not be a leaf", projectNode.isLeaf());
        assertFalse("Folder node should not be a leaf", folderNode.isLeaf());
        assertTrue("File node should be a leaf", fileNode.isLeaf());
    }
    
    @Test
    public void testFindChildByName() {
        // 测试按名称查找子节点
        NavigationNode foundProject = rootNode.findChildByName("TestProject");
        assertEquals("Should find project by name", projectNode, foundProject);
        
        NavigationNode foundFolder = projectNode.findChildByName("src");
        assertEquals("Should find folder by name", folderNode, foundFolder);
        
        NavigationNode foundFile = folderNode.findChildByName("Program.cs");
        assertEquals("Should find file by name", fileNode, foundFile);
    }
    
    @Test
    public void testFindChildByName_NotFound() {
        // 测试查找不存在的子节点
        NavigationNode notFound = rootNode.findChildByName("NonexistentProject");
        assertNull("Should return null when child not found", notFound);
    }
    
    @Test
    public void testGetPathComponents() {
        // 测试获取路径组件
        List<String> pathComponents = fileNode.getPathComponents();
        assertEquals("Path components count should be 4", 4, pathComponents.size());
        assertEquals("Path components should match", List.of("Solution.sln", "TestProject", "src", "Program.cs"), pathComponents);
    }
    
    @Test
    public void testIsRootNode() {
        // 测试是否为根节点
        assertTrue("Root node should be considered a root", rootNode.isRoot());
        assertFalse("Project node should not be a root", projectNode.isRoot());
        assertFalse("Folder node should not be a root", folderNode.isRoot());
        assertFalse("File node should not be a root", fileNode.isRoot());
    }
    
    @Test
    public void testDepth() {
        // 测试节点深度
        assertEquals("Root depth should be 0", 0, rootNode.getDepth());
        assertEquals("Project depth should be 1", 1, projectNode.getDepth());
        assertEquals("Folder depth should be 2", 2, folderNode.getDepth());
        assertEquals("File depth should be 3", 3, fileNode.getDepth());
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // 测试相等性和哈希码
        NavigationNode sameRootNode = new NavigationNode("root", "Solution.sln", NodeType.SOLUTION, "/path/to/solution.sln");
        
        assertTrue("Nodes with same ID should be equal", rootNode.equals(sameRootNode));
        assertTrue("Equals should be reflexive", rootNode.equals(rootNode));
        assertFalse("Node should not equal null", rootNode.equals(null));
        assertFalse("Node should not equal different type", rootNode.equals("root"));
        
        assertEquals("Equal nodes should have same hash code", rootNode.hashCode(), sameRootNode.hashCode());
        
        // 测试不同ID的情况
        NavigationNode differentNode = new NavigationNode("different", "Solution.sln", NodeType.SOLUTION, "/path/to/solution.sln");
        assertFalse("Nodes with different IDs should not be equal", rootNode.equals(differentNode));
    }
    
    @Test
    public void testToString() {
        // 测试toString方法
        String toString = fileNode.toString();
        assertNotNull("toString should not be null", toString);
        assertTrue("toString should contain node name", toString.contains("Program.cs"));
        assertTrue("toString should contain node type", toString.contains("FILE"));
    }
}