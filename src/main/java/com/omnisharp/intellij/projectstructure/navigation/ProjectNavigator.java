package com.omnisharp.intellij.projectstructure.navigation;

import com.intellij.openapi.project.Project;
import com.omnisharp.intellij.projectstructure.service.ProjectManagerService;
import com.omnisharp.intellij.projectstructure.model.ProjectStructureNode;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 项目结构导航的核心组件
 * 负责提供项目结构的导航功能，包括查找节点、过滤节点等
 */
public class ProjectNavigator {
    private final Project project;
    private final ProjectManagerService projectManagerService;
    private final FilterManager filterManager;
    private StructureTree structureTree;
    
    public ProjectNavigator(@NotNull Project project) {
        this.project = project;
        this.projectManagerService = new ProjectManagerService();
        this.filterManager = new FilterManager();
        initialize();
    }
    
    private void initialize() {
        // 初始化结构树
        Optional<SolutionModel> solutionModelOpt = projectManagerService.getCurrentSolution();
        if (solutionModelOpt.isPresent()) {
            this.structureTree = new StructureTree(solutionModelOpt.get());
        }
    }
    
    /**
     * 重新加载项目结构
     */
    public void reloadStructure() {
        Optional<SolutionModel> solutionModelOpt = projectManagerService.getCurrentSolution();
        if (solutionModelOpt.isPresent()) {
            this.structureTree = new StructureTree(solutionModelOpt.get());
        }
    }
    
    /**
     * 获取项目管理器服务
     * @return 项目管理器服务实例
     */
    public ProjectManagerService getProjectManagerService() {
        return projectManagerService;
    }
    
    /**
     * 获取项目实例
     * @return 项目实例
     */
    @NotNull
    public Project getProject() {
        return project;
    }
    
    /**
     * 获取根节点
     * @return 根导航节点
     */
    @Nullable
    public NavigationNode getRootNode() {
        if (structureTree == null) {
            return null;
        }
        return structureTree.getRootNode();
    }
    
    /**
     * 通过ID查找节点
     * @param nodeId 节点ID
     * @return 找到的导航节点
     */
    @Nullable
    public NavigationNode findNodeById(String nodeId) {
        if (structureTree == null) {
            return null;
        }
        return structureTree.findNodeById(nodeId);
    }
    
    /**
     * 通过名称查找节点
     * @param name 节点名称
     * @return 找到的导航节点列表
     */
    @NotNull
    public List<NavigationNode> findNodesByName(String name) {
        if (structureTree == null) {
            return List.of();
        }
        return structureTree.findNodesByName(name);
    }
    
    /**
     * 过滤节点
     * @param predicate 过滤条件
     * @return 过滤后的导航节点列表
     */
    @NotNull
    public List<NavigationNode> filterNodes(@NotNull Predicate<NavigationNode> predicate) {
        if (structureTree == null) {
            return List.of();
        }
        return structureTree.filterNodes(predicate);
    }
    
    /**
     * 获取项目列表
     * @return 项目导航节点列表
     */
    @NotNull
    public List<NavigationNode> getProjects() {
        if (structureTree == null) {
            return List.of();
        }
        NavigationNode rootNode = structureTree.getRootNode();
        if (rootNode == null) {
            return List.of();
        }
        return rootNode.getChildren().stream()
                .filter(node -> node.getNodeType() == NodeType.PROJECT)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取指定项目的所有节点
     * @param projectName 项目名称
     * @return 项目下的所有节点
     */
    @NotNull
    public List<NavigationNode> getProjectNodes(String projectName) {
        List<NavigationNode> projects = getProjects();
        Optional<NavigationNode> projectNode = projects.stream()
                .filter(node -> node.getName().equals(projectName))
                .findFirst();
        
        if (projectNode.isPresent()) {
            return structureTree.getAllDescendants(projectNode.get());
        }
        return List.of();
    }
    
    /**
     * 获取过滤器管理器
     * @return 过滤器管理器实例
     */
    @NotNull
    public FilterManager getFilterManager() {
        return filterManager;
    }
    
    /**
     * 应用过滤器
     * @return 应用过滤器后的结构树
     */
    @NotNull
    public List<NavigationNode> applyFilters() {
        if (structureTree == null) {
            return List.of();
        }
        List<Predicate<NavigationNode>> activeFilters = filterManager.getActiveFilterPredicates();
        if (activeFilters.isEmpty()) {
            return structureTree.getAllNodes();
        }
        
        return structureTree.filterNodes(node -> 
                activeFilters.stream().allMatch(filter -> filter.test(node))
        );
    }
    
    /**
     * 导航到指定文件
     * @param filePath 文件路径
     * @return 对应的导航节点
     */
    @Nullable
    public NavigationNode navigateToFile(String filePath) {
        if (structureTree == null) {
            return null;
        }
        return structureTree.findNodeByPath(filePath);
    }
    
    /**
     * 获取节点的面包屑路径
     * @param node 导航节点
     * @return 面包屑路径列表
     */
    @NotNull
    public List<String> getBreadcrumbPath(@NotNull NavigationNode node) {
        if (structureTree == null) {
            return List.of(node.getName());
        }
        return structureTree.getBreadcrumbPath(node);
    }
}