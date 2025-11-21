package com.omnisharp.intellij.projectstructure.navigation;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
// import com.intellij.ide.util.treeView.AbstractTreeBuilder; // 移除不存在的导入
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;
import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

/**
 * 视图控制器，负责管理项目结构导航的UI显示和交互
 */
public class ViewController {
    private final Project project;
    private final ProjectNavigator projectNavigator;
    private final Tree projectTree;
    private final DefaultTreeModel treeModel;
    private final DefaultMutableTreeNode rootTreeNode;
    private SimpleToolWindowPanel toolWindowPanel;
    
    private Consumer<NavigationNode> nodeSelectedCallback;
    private Consumer<NavigationNode> nodeDoubleClickedCallback;
    
    public ViewController(@NotNull Project project) {
        this.project = project;
        this.projectNavigator = new ProjectNavigator(project);
        this.rootTreeNode = new DefaultMutableTreeNode("Loading...");
        this.treeModel = new DefaultTreeModel(rootTreeNode);
        this.projectTree = new Tree(treeModel);
        initializeUI();
    }
    
    private void initializeUI() {
        // 设置树的渲染器
        projectTree.setCellRenderer(new NavigationTreeCellRenderer());
        
        // 启用根节点可见
        projectTree.setRootVisible(true);
        projectTree.setShowsRootHandles(true);
        
        // 添加选择监听器
        projectTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                handleNodeSelection(e);
            }
        });
        
        // 添加鼠标监听器处理双击事件
        projectTree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    handleNodeDoubleClick(e);
                }
            }
        });
        
        // 创建工具窗口面板
        toolWindowPanel = new SimpleToolWindowPanel(true);
        toolWindowPanel.setContent(ScrollPaneFactory.createScrollPane(projectTree));
        
        // 初始加载项目结构
        reloadProjectStructure();
    }
    
    /**
     * 重新加载项目结构
     */
    public void reloadProjectStructure() {
        ApplicationManager.getApplication().runReadAction(() -> {
            ApplicationManager.getApplication().invokeLater(() -> {
                projectNavigator.reloadStructure();
                rebuildTree();
            });
        });
    }
    
    private void rebuildTree() {
        // 清空现有树
        rootTreeNode.removeAllChildren();
        
        // 获取根导航节点
        NavigationNode rootNavigationNode = projectNavigator.getRootNode();
        if (rootNavigationNode != null) {
            // 创建树节点
            DefaultMutableTreeNode rootNode = createTreeNode(rootNavigationNode);
            rootTreeNode.setUserObject(rootNavigationNode.getName());
            addChildNodes(rootNavigationNode, rootTreeNode);
            
            // 展开树
            treeModel.reload();
            TreeUtil.expandAll(projectTree);
        } else {
            rootTreeNode.setUserObject("No project structure available");
            treeModel.reload();
        }
    }
    
    private DefaultMutableTreeNode createTreeNode(@NotNull NavigationNode navigationNode) {
        DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(navigationNode.getName());
        treeNode.setUserObject(navigationNode);
        return treeNode;
    }
    
    private void addChildNodes(@NotNull NavigationNode parentNode, @NotNull DefaultMutableTreeNode parentTreeNode) {
        for (NavigationNode childNode : parentNode.getChildren()) {
            DefaultMutableTreeNode childTreeNode = createTreeNode(childNode);
            parentTreeNode.add(childTreeNode);
            addChildNodes(childNode, childTreeNode);
        }
    }
    
    private void handleNodeSelection(TreeSelectionEvent e) {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof NavigationNode) {
            NavigationNode navigationNode = (NavigationNode) selectedNode.getUserObject();
            if (nodeSelectedCallback != null) {
                nodeSelectedCallback.accept(navigationNode);
            }
        }
    }
    
    private void handleNodeDoubleClick(MouseEvent e) {
        int row = projectTree.getRowForLocation(e.getX(), e.getY());
        if (row >= 0) {
            projectTree.setSelectionRow(row);
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();
            if (selectedNode != null && selectedNode.getUserObject() instanceof NavigationNode) {
                NavigationNode navigationNode = (NavigationNode) selectedNode.getUserObject();
                if (nodeDoubleClickedCallback != null) {
                    nodeDoubleClickedCallback.accept(navigationNode);
                }
            }
        }
    }
    
    /**
     * 应用过滤器并刷新树
     */
    public void applyFilters() {
        List<NavigationNode> filteredNodes = projectNavigator.applyFilters();
        // 这里可以实现基于过滤结果的树重建逻辑
        // 为了简化，我们暂时只重建整个树
        rebuildTree();
    }
    
    /**
     * 搜索节点
     * @param keyword 搜索关键词
     */
    public void searchNodes(@NotNull String keyword) {
        // 创建临时过滤器
        String filterId = "search_filter_" + System.currentTimeMillis();
        projectNavigator.getFilterManager().createNameFilter(filterId, "Search: " + keyword, keyword, false);
        projectNavigator.getFilterManager().activateFilter(filterId);
        
        // 应用过滤器
        applyFilters();
        
        // 可以选择保留或移除临时过滤器
        // projectNavigator.getFilterManager().removeFilter(filterId);
    }
    
    /**
     * 展开所有节点
     */
    public void expandAll() {
        TreeUtil.expandAll(projectTree);
    }
    
    /**
     * 折叠所有节点
     */
    public void collapseAll() {
        TreeUtil.collapseAll(projectTree, 1);
    }
    
    /**
     * 获取当前选中的节点
     * @return 选中的导航节点，如果没有选中则返回null
     */
    @Nullable
    public NavigationNode getSelectedNode() {
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) projectTree.getLastSelectedPathComponent();
        if (selectedNode != null && selectedNode.getUserObject() instanceof NavigationNode) {
            return (NavigationNode) selectedNode.getUserObject();
        }
        return null;
    }
    
    /**
     * 设置节点选中回调
     * @param callback 选中回调
     */
    public void setNodeSelectedCallback(@Nullable Consumer<NavigationNode> callback) {
        this.nodeSelectedCallback = callback;
    }
    
    /**
     * 设置节点双击回调
     * @param callback 双击回调
     */
    public void setNodeDoubleClickedCallback(@Nullable Consumer<NavigationNode> callback) {
        this.nodeDoubleClickedCallback = callback;
    }
    
    /**
     * 获取工具窗口面板
     * @return 工具窗口面板
     */
    @NotNull
    public SimpleToolWindowPanel getToolWindowPanel() {
        return toolWindowPanel;
    }
    
    /**
     * 获取项目导航器
     * @return 项目导航器实例
     */
    @NotNull
    public ProjectNavigator getProjectNavigator() {
        return projectNavigator;
    }
    
    /**
     * 获取项目树
     * @return 项目树组件
     */
    @NotNull
    public Tree getProjectTree() {
        return projectTree;
    }
    
    /**
     * 树单元格渲染器，用于自定义节点显示
     */
    private class NavigationTreeCellRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                      boolean expanded, boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof NavigationNode) {
                    NavigationNode navigationNode = (NavigationNode) userObject;
                    setText(navigationNode.getName());
                    
                    // 设置图标
                    setIcon(getIconForNodeType(navigationNode.getNodeType()));
                }
            }
            
            return this;
        }
        
        private Icon getIconForNodeType(NodeType type) {
            switch (type) {
                case SOLUTION:
                    return AllIcons.Nodes.IdeaProject;
                case PROJECT:
                    return AllIcons.Nodes.Module;
                case FOLDER:
                    return AllIcons.Nodes.Folder;
                case FILE:
                    return AllIcons.FileTypes.Any_type;
                case REFERENCES:
                case PROJECT_REFERENCES:
                case PACKAGE_REFERENCES:
                case ASSEMBLY_REFERENCES:
                    return AllIcons.Nodes.PpLibFolder;
                default:
                    return AllIcons.Nodes.Tag;
            }
        }
    }
}