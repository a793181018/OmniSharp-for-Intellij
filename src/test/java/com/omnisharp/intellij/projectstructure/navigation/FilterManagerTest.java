package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class FilterManagerTest {
    
    private FilterManager filterManager;
    private NavigationNode projectNode;
    private NavigationNode folderNode;
    private NavigationNode fileNode;
    private NavigationNode hiddenFileNode;
    
    @Before
    public void setUp() {
        filterManager = new FilterManager();
        
        // 创建测试节点
        projectNode = new NavigationNode("project1", "TestProject", NodeType.PROJECT, "/path/to/project.csproj");
        folderNode = new NavigationNode("folder1", "src", NodeType.FOLDER, "/path/to/src");
        fileNode = new NavigationNode("file1", "Program.cs", NodeType.FILE, "/path/to/src/Program.cs");
        hiddenFileNode = new NavigationNode("file2", ".hidden.cs", NodeType.FILE, "/path/to/src/.hidden.cs");
    }
    
    @Test
    public void testAddFilter() {
        // 添加过滤器
        String filterId = filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        
        assertNotNull("Filter ID should not be null", filterId);
        assertTrue("Filter manager should have active filters", filterManager.hasActiveFilters());
        assertTrue("Filter should be in the filter map", filterManager.getFilters().containsKey(filterId));
    }
    
    @Test
    public void testRemoveFilter() {
        // 添加过滤器
        String filterId = filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        
        // 移除过滤器
        boolean removed = filterManager.removeFilter(filterId);
        
        assertTrue("Should return true when filter is removed", removed);
        assertFalse("Filter manager should not have active filters after removal", filterManager.hasActiveFilters());
        assertFalse("Filter should not be in the filter map after removal", filterManager.getFilters().containsKey(filterId));
    }
    
    @Test
    public void testRemoveFilter_NotFound() {
        // 尝试移除不存在的过滤器
        boolean removed = filterManager.removeFilter("nonexistent");
        
        assertFalse("Should return false when trying to remove nonexistent filter", removed);
    }
    
    @Test
    public void testToggleFilter() {
        // 添加并激活过滤器
        String filterId = filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        FilterManager.NodeFilter filter = filterManager.getFilters().get(filterId);
        
        assertTrue("Filter should be active initially", filter.isActive());
        
        // 切换过滤器状态
        filterManager.toggleFilter(filterId);
        assertFalse("Filter should be inactive after toggle", filter.isActive());
        assertFalse("Filter manager should not have active filters after toggle", filterManager.hasActiveFilters());
        
        // 再次切换
        filterManager.toggleFilter(filterId);
        assertTrue("Filter should be active after second toggle", filter.isActive());
        assertTrue("Filter manager should have active filters after second toggle", filterManager.hasActiveFilters());
    }
    
    @Test
    public void testReset() {
        // 添加多个过滤器
        filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        filterManager.addFilter(FilterManager.FilterType.TYPE, "PROJECT", true);
        
        assertTrue("Filter manager should have active filters", filterManager.hasActiveFilters());
        assertEquals("Filter manager should have 2 filters", 2, filterManager.getFilters().size());
        
        // 重置过滤器
        filterManager.reset();
        
        assertFalse("Filter manager should not have active filters after reset", filterManager.hasActiveFilters());
        assertTrue("Filter manager should have no filters after reset", filterManager.getFilters().isEmpty());
    }
    
    @Test
    public void testApply() {
        // 添加过滤器
        filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        
        // 应用过滤器（主要是重置内部状态）
        filterManager.apply();
        
        // 验证过滤器仍然存在
        assertTrue("Filter manager should still have active filters", filterManager.hasActiveFilters());
    }
    
    @Test
    public void testFilterByName() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加名称过滤器
        filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", true);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果
        assertEquals("Should filter to 1 node", 1, filteredNodes.size());
        assertEquals("Filtered node should be Program.cs", fileNode, filteredNodes.get(0));
    }
    
    @Test
    public void testFilterByType() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加类型过滤器
        filterManager.addFilter(FilterManager.FilterType.TYPE, "FILE", true);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果
        assertEquals("Should filter to 2 file nodes", 2, filteredNodes.size());
        assertTrue("Filtered nodes should contain Program.cs", filteredNodes.contains(fileNode));
        assertTrue("Filtered nodes should contain .hidden.cs", filteredNodes.contains(hiddenFileNode));
    }
    
    @Test
    public void testFilterByPath() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加路径过滤器
        filterManager.addFilter(FilterManager.FilterType.PATH, "/path/to/src", true);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果
        assertEquals("Should filter to 2 nodes in src folder", 2, filteredNodes.size());
        assertTrue("Filtered nodes should contain Program.cs", filteredNodes.contains(fileNode));
        assertTrue("Filtered nodes should contain .hidden.cs", filteredNodes.contains(hiddenFileNode));
    }
    
    @Test
    public void testFilterByRegex() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加正则表达式过滤器，匹配所有.cs文件
        filterManager.addFilter(FilterManager.FilterType.REGEX, ".*\\.cs$", true);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果
        assertEquals("Should filter to 2 .cs files", 2, filteredNodes.size());
        assertTrue("Filtered nodes should contain Program.cs", filteredNodes.contains(fileNode));
        assertTrue("Filtered nodes should contain .hidden.cs", filteredNodes.contains(hiddenFileNode));
    }
    
    @Test
    public void testMultipleFilters() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加多个过滤器（文件类型 + 不以.开头的名称）
        filterManager.addFilter(FilterManager.FilterType.TYPE, "FILE", true);
        filterManager.addFilter(FilterManager.FilterType.REGEX, "^[^\\.].*$", true);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果（只应该包含Program.cs）
        assertEquals("Should filter to 1 node matching all criteria", 1, filteredNodes.size());
        assertEquals("Filtered node should be Program.cs", fileNode, filteredNodes.get(0));
    }
    
    @Test
    public void testInactiveFilters() {
        // 创建节点列表
        List<NavigationNode> nodes = List.of(projectNode, folderNode, fileNode, hiddenFileNode);
        
        // 添加过滤器但设为非活动状态
        String filterId = filterManager.addFilter(FilterManager.FilterType.NAME, "Program.cs", false);
        
        // 过滤节点
        List<NavigationNode> filteredNodes = filterManager.filter(nodes);
        
        // 验证结果（不应该过滤任何节点）
        assertEquals("All nodes should be returned when filter is inactive", nodes.size(), filteredNodes.size());
    }
    
    @Test
    public void testCreateNameFilter() {
        // 创建名称过滤器
        FilterManager.NodeFilter filter = filterManager.createNameFilter("Program.cs", true);
        
        assertNotNull("Filter should not be null", filter);
        assertTrue("Filter should be active", filter.isActive());
        assertTrue("Filter should match Program.cs", filter.matches(fileNode));
        assertFalse("Filter should not match .hidden.cs", filter.matches(hiddenFileNode));
    }
    
    @Test
    public void testCreateRegexFilter() {
        // 创建正则表达式过滤器
        Pattern pattern = Pattern.compile(".*\\.cs$");
        FilterManager.NodeFilter filter = filterManager.createRegexFilter(pattern, true);
        
        assertNotNull("Filter should not be null", filter);
        assertTrue("Filter should be active", filter.isActive());
        assertTrue("Filter should match Program.cs", filter.matches(fileNode));
        assertTrue("Filter should match .hidden.cs", filter.matches(hiddenFileNode));
        assertFalse("Filter should not match folder", filter.matches(folderNode));
    }
    
    @Test
    public void testCreateTypeFilter() {
        // 创建类型过滤器
        FilterManager.NodeFilter filter = filterManager.createTypeFilter(NodeType.FILE, true);
        
        assertNotNull("Filter should not be null", filter);
        assertTrue("Filter should be active", filter.isActive());
        assertTrue("Filter should match Program.cs", filter.matches(fileNode));
        assertTrue("Filter should match .hidden.cs", filter.matches(hiddenFileNode));
        assertFalse("Filter should not match folder", filter.matches(folderNode));
    }
    
    @Test
    public void testCreatePathFilter() {
        // 创建路径过滤器
        FilterManager.NodeFilter filter = filterManager.createPathFilter("/path/to/src", true);
        
        assertNotNull("Filter should not be null", filter);
        assertTrue("Filter should be active", filter.isActive());
        assertTrue("Filter should match Program.cs", filter.matches(fileNode));
        assertTrue("Filter should match .hidden.cs", filter.matches(hiddenFileNode));
        assertFalse("Filter should not match project", filter.matches(projectNode));
    }
}