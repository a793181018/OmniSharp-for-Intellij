package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ProjectNavigatorTest {
    
    @Mock
    private StructureTree structureTree;
    
    @Mock
    private FilterManager filterManager;
    
    private ProjectNavigator projectNavigator;
    
    @Mock
    private NavigationNode rootNode;
    
    @Mock
    private NavigationNode projectNode;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        projectNavigator = new ProjectNavigator(structureTree, filterManager);
        
        // 模拟根节点行为
        when(rootNode.getId()).thenReturn("root");
        when(rootNode.getName()).thenReturn("Solution.sln");
        when(rootNode.getNodeType()).thenReturn(NodeType.SOLUTION);
        
        // 模拟项目节点行为
        when(projectNode.getId()).thenReturn("project1");
        when(projectNode.getName()).thenReturn("TestProject");
        when(projectNode.getNodeType()).thenReturn(NodeType.PROJECT);
        
        // 设置structureTree的模拟行为
        when(structureTree.getRootNode()).thenReturn(rootNode);
    }
    
    @Test
    public void testGetRootNode() {
        NavigationNode result = projectNavigator.getRootNode();
        assertNotNull("Root node should not be null", result);
        assertEquals("Root node ID should be 'root'", "root", result.getId());
        assertEquals("Root node name should be 'Solution.sln'", "Solution.sln", result.getName());
        assertEquals("Root node type should be SOLUTION", NodeType.SOLUTION, result.getNodeType());
        verify(structureTree).getRootNode();
    }
    
    @Test
    public void testFindNodeById() {
        // 模拟查找行为
        when(structureTree.findNodeById("project1")).thenReturn(projectNode);
        
        NavigationNode result = projectNavigator.findNodeById("project1");
        assertNotNull("Found node should not be null", result);
        assertEquals("Found node should be the project node", projectNode, result);
        verify(structureTree).findNodeById("project1");
    }
    
    @Test
    public void testFindNodeById_NotFound() {
        // 模拟查找不到的情况
        when(structureTree.findNodeById("nonexistent")).thenReturn(null);
        
        NavigationNode result = projectNavigator.findNodeById("nonexistent");
        assertNull("Should return null for nonexistent node", result);
        verify(structureTree).findNodeById("nonexistent");
    }
    
    @Test
    public void testFindNodesByName() {
        List<NavigationNode> mockResults = List.of(projectNode);
        when(structureTree.findNodesByName("TestProject")).thenReturn(mockResults);
        
        List<NavigationNode> results = projectNavigator.findNodesByName("TestProject");
        assertNotNull("Results list should not be null", results);
        assertFalse("Results list should not be empty", results.isEmpty());
        assertEquals("Results list should contain project node", 1, results.size());
        assertEquals("Found node should be the project node", projectNode, results.get(0));
        verify(structureTree).findNodesByName("TestProject");
    }
    
    @Test
    public void testFilterNodes_WithActiveFilters() {
        // 设置过滤器管理器的模拟行为
        when(filterManager.hasActiveFilters()).thenReturn(true);
        when(filterManager.filter(List.of(projectNode))).thenReturn(List.of(projectNode));
        
        List<NavigationNode> nodes = List.of(projectNode);
        List<NavigationNode> filteredNodes = projectNavigator.filterNodes(nodes);
        
        assertNotNull("Filtered nodes should not be null", filteredNodes);
        verify(filterManager).hasActiveFilters();
        verify(filterManager).filter(nodes);
    }
    
    @Test
    public void testFilterNodes_WithoutActiveFilters() {
        // 设置过滤器管理器的模拟行为
        when(filterManager.hasActiveFilters()).thenReturn(false);
        
        List<NavigationNode> nodes = List.of(projectNode);
        List<NavigationNode> filteredNodes = projectNavigator.filterNodes(nodes);
        
        assertNotNull("Filtered nodes should not be null", filteredNodes);
        assertEquals("With no active filters, original list should be returned", nodes, filteredNodes);
        verify(filterManager).hasActiveFilters();
        // 确保没有调用filter方法
        verify(filterManager, never()).filter(anyList());
    }
    
    @Test
    public void testIsFilteringActive() {
        when(filterManager.hasActiveFilters()).thenReturn(true);
        assertTrue("Filtering should be active", projectNavigator.isFilteringActive());
        
        when(filterManager.hasActiveFilters()).thenReturn(false);
        assertFalse("Filtering should not be active", projectNavigator.isFilteringActive());
        
        verify(filterManager, times(2)).hasActiveFilters();
    }
    
    @Test
    public void testApplyFilters() {
        // 测试应用过滤器
        projectNavigator.applyFilters();
        verify(filterManager).apply();
    }
    
    @Test
    public void testResetFilters() {
        // 测试重置过滤器
        projectNavigator.resetFilters();
        verify(filterManager).reset();
    }
    
    @Test
    public void testGetFilterManager() {
        // 测试获取过滤器管理器
        FilterManager result = projectNavigator.getFilterManager();
        assertNotNull("Filter manager should not be null", result);
        assertEquals("Returned filter manager should be the injected one", filterManager, result);
    }
    
    @Test
    public void testUpdateStructure() {
        // 测试更新结构树
        projectNavigator.updateStructure();
        verify(structureTree).refresh();
    }
}