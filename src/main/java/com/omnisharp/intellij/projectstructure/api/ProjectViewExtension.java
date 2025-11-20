package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.ProjectStructureNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.openapi.project.Project;
import javax.swing.*;
import java.util.Collection;

/**
 * 项目视图扩展接口，用于自定义C#项目在IntelliJ IDEA中的显示方式
 */
public interface ProjectViewExtension {
    /**
     * 获取项目视图节点的图标
     * @param node 项目结构节点
     * @param isOpen 是否打开状态
     * @return 图标对象
     */
    Icon getNodeIcon(ProjectStructureNode node, boolean isOpen);

    /**
     * 获取节点显示文本
     * @param node 项目结构节点
     * @return 显示文本
     */
    String getNodeText(ProjectStructureNode node);

    /**
     * 自定义节点的上下文菜单
     * @param node 项目结构节点
     * @param menu 上下文菜单
     * @param project IntelliJ项目对象
     */
    void customizeNodeContextMenu(ProjectStructureNode node, JPopupMenu menu, Project project);

    /**
     * 自定义项目视图的右键操作
     * @param project IntelliJ项目对象
     * @return 操作集合
     */
    Collection<ProjectViewAction> getCustomActions(Project project);

    /**
     * 判断节点是否可展开
     * @param node 项目结构节点
     * @return 是否可展开
     */
    boolean isExpandable(ProjectStructureNode node);

    /**
     * 获取树结构提供器
     * @param project IntelliJ项目对象
     * @return 树结构提供器
     */
    TreeStructureProvider getTreeStructureProvider(Project project);

    /**
     * 自定义目录节点
     * @param directoryNode 目录节点
     * @param settings 视图设置
     * @return 处理后的子节点集合
     */
    Collection modifyDirectoryChildren(
            PsiDirectoryNode directoryNode, Collection children,
            ViewSettings settings);

    /**
     * 获取节点的工具提示文本
     * @param node 项目结构节点
     * @return 工具提示文本
     */
    String getNodeTooltip(ProjectStructureNode node);

    /**
     * 判断是否显示节点
     * @param node 项目结构节点
     * @param settings 视图设置
     * @return 是否显示
     */
    boolean shouldShowNode(ProjectStructureNode node, ViewSettings settings);

    /**
     * 项目视图操作接口
     */
    interface ProjectViewAction {
        /**
         * 获取操作名称
         * @return 操作名称
         */
        String getName();

        /**
         * 获取操作描述
         * @return 操作描述
         */
        String getDescription();

        /**
         * 获取操作图标
         * @return 操作图标
         */
        Icon getIcon();

        /**
         * 判断操作是否可用
         * @param selectedNodes 选中的节点集合
         * @return 是否可用
         */
        boolean isEnabled(Collection<ProjectStructureNode> selectedNodes);

        /**
         * 执行操作
         * @param selectedNodes 选中的节点集合
         * @param project IntelliJ项目对象
         */
        void actionPerformed(Collection<ProjectStructureNode> selectedNodes, Project project);
    }
}