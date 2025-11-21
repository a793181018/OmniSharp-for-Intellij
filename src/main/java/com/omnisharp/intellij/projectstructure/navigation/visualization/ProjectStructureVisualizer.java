package com.omnisharp.intellij.projectstructure.navigation.visualization;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.omnisharp.intellij.projectstructure.navigation.NavigationNode;
import com.omnisharp.intellij.projectstructure.navigation.ProjectNavigator;
import com.omnisharp.intellij.projectstructure.model.NodeType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目结构可视化器，用于可视化项目的层次结构
 */
public class ProjectStructureVisualizer {
    private final ProjectNavigator projectNavigator;
    private final Project project;
    private NavigationNode rootNode;
    private JComponent visualizationComponent;
    private VisualizationService.VisualizationStyle style;
    private final int NODE_WIDTH = 150;
    private final int NODE_HEIGHT = 40;
    private final int HORIZONTAL_GAP = 50;
    private final int VERTICAL_GAP = 60;
    
    public ProjectStructureVisualizer(@NotNull ProjectNavigator projectNavigator) {
        this.projectNavigator = projectNavigator;
        this.project = projectNavigator.getProject();
        this.style = VisualizationService.VisualizationStyle.DEFAULT;
        initialize();
    }
    
    private void initialize() {
        visualizationComponent = createVisualizationComponent();
    }
    
    @NotNull
    private JComponent createVisualizationComponent() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (rootNode != null) {
                    paintTree(g, this.getWidth(), this.getHeight());
                }
            }
        };
        panel.setPreferredSize(new Dimension(1000, 800));
        panel.setBackground(getBackgroundColor());
        return new JBScrollPane(panel);
    }
    
    /**
     * 可视化整个项目结构
     */
    public void visualize() {
        this.rootNode = projectNavigator.getRootNode();
        
        if (rootNode == null) {
            showErrorMessage("没有可用的项目结构");
            return;
        }
        
        // 创建并显示对话框
        DialogWrapper dialog = new DialogWrapper(project) {
            {
                init();
                setTitle("项目结构可视化");
            }
            
            @Nullable
            @Override
            protected JComponent createCenterPanel() {
                return visualizationComponent;
            }
            
            @Override
            protected Action[] createActions() {
                return new Action[] {getOKAction()};
            }
        };
        
        dialog.show();
    }
    
    /**
     * 可视化指定节点的子树
     * @param node 起始节点
     */
    public void visualizeSubtree(@NotNull NavigationNode node) {
        this.rootNode = node;
        
        // 创建并显示对话框
        DialogWrapper dialog = new DialogWrapper(project) {
            {
                init();
                setTitle("节点子树可视化 - " + node.getName());
            }
            
            @Nullable
            @Override
            protected JComponent createCenterPanel() {
                return visualizationComponent;
            }
            
            @Override
            protected Action[] createActions() {
                return new Action[] {getOKAction()};
            }
        };
        
        dialog.show();
    }
    
    private void paintTree(Graphics g, int width, int height) {
        if (rootNode == null) {
            return;
        }
        
        Graphics2D g2d = (Graphics2D) g;
        
        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 计算树的尺寸
        TreeMetrics metrics = calculateTreeMetrics(rootNode);
        
        // 计算起始位置（居中）
        int startX = width / 2 - metrics.totalWidth / 2;
        int startY = 50;
        
        // 绘制树
        Map<NavigationNode, Point> nodePositions = new HashMap<>();
        paintTreeRecursive(g2d, rootNode, startX, startY, metrics, nodePositions);
    }
    
    private TreeMetrics calculateTreeMetrics(@NotNull NavigationNode node) {
        TreeMetrics metrics = new TreeMetrics();
        
        if (node.isLeaf()) {
            metrics.width = NODE_WIDTH;
            metrics.height = NODE_HEIGHT;
            metrics.totalWidth = NODE_WIDTH;
            metrics.totalHeight = NODE_HEIGHT;
            metrics.maxLevelWidth = 1;
            metrics.maxDepth = 1;
            return metrics;
        }
        
        int totalWidth = 0;
        int maxSubtreeHeight = 0;
        int childCount = node.getChildren().size();
        
        for (NavigationNode child : node.getChildren()) {
            TreeMetrics childMetrics = calculateTreeMetrics(child);
            totalWidth += childMetrics.totalWidth + HORIZONTAL_GAP;
            maxSubtreeHeight = Math.max(maxSubtreeHeight, childMetrics.totalHeight);
            metrics.maxDepth = Math.max(metrics.maxDepth, childMetrics.maxDepth + 1);
        }
        
        // 减去多余的水平间隙
        if (childCount > 0) {
            totalWidth -= HORIZONTAL_GAP;
        }
        
        metrics.width = Math.max(NODE_WIDTH, totalWidth);
        metrics.height = NODE_HEIGHT;
        metrics.totalWidth = totalWidth;
        metrics.totalHeight = NODE_HEIGHT + VERTICAL_GAP + maxSubtreeHeight;
        metrics.maxLevelWidth = Math.max(childCount, metrics.maxLevelWidth);
        
        return metrics;
    }
    
    private void paintTreeRecursive(Graphics2D g2d, NavigationNode node, int x, int y,
                                  TreeMetrics metrics, Map<NavigationNode, Point> nodePositions) {
        // 保存当前节点位置
        nodePositions.put(node, new Point(x, y));
        
        // 绘制当前节点
        paintNode(g2d, node, x, y);
        
        if (node.isLeaf()) {
            return;
        }
        
        // 绘制子节点
        int childY = y + NODE_HEIGHT + VERTICAL_GAP;
        int childX = x - metrics.totalWidth / 2 + NODE_WIDTH / 2;
        
        for (NavigationNode child : node.getChildren()) {
            // 计算子节点的度量
            TreeMetrics childMetrics = calculateTreeMetrics(child);
            
            // 绘制连接线
            paintConnection(g2d, x, y, childX, childY);
            
            // 递归绘制子树
            paintTreeRecursive(g2d, child, childX, childY, childMetrics, nodePositions);
            
            // 更新下一个子节点的X坐标
            childX += childMetrics.totalWidth + HORIZONTAL_GAP;
        }
    }
    
    private void paintNode(Graphics2D g2d, NavigationNode node, int x, int y) {
        // 确定节点颜色
        Color nodeColor = getColorForNodeType(node.getNodeType());
        
        // 绘制节点背景
        g2d.setColor(nodeColor);
        g2d.fillRoundRect(x - NODE_WIDTH / 2, y - NODE_HEIGHT / 2, NODE_WIDTH, NODE_HEIGHT, 8, 8);
        
        // 绘制节点边框
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x - NODE_WIDTH / 2, y - NODE_HEIGHT / 2, NODE_WIDTH, NODE_HEIGHT, 8, 8);
        
        // 绘制节点文本
        g2d.setColor(Color.BLACK);
        g2d.setFont(getNodeFont());
        FontMetrics metrics = g2d.getFontMetrics();
        
        // 截断过长的文本
        String displayName = truncateText(node.getName(), metrics, NODE_WIDTH - 20);
        int textWidth = metrics.stringWidth(displayName);
        int textHeight = metrics.getAscent();
        
        g2d.drawString(displayName, x - textWidth / 2, y + textHeight / 4);
    }
    
    private void paintConnection(Graphics2D g2d, int parentX, int parentY, int childX, int childY) {
        g2d.setColor(getConnectionColor());
        g2d.setStroke(new BasicStroke(1.5f));
        
        // 绘制从父节点底部到子节点顶部的连接线
        g2d.drawLine(parentX, parentY + NODE_HEIGHT / 2, childX, childY - NODE_HEIGHT / 2);
    }
    
    private Color getColorForNodeType(NodeType type) {
        switch (type) {
            case SOLUTION:
                return Color.YELLOW;
            case PROJECT:
                return Color.LIGHT_GRAY;
            case FOLDER:
                return Color.CYAN;
            case FILE:
                return Color.WHITE;
            case REFERENCES:
            case PROJECT_REFERENCES:
            case PACKAGE_REFERENCES:
            case ASSEMBLY_REFERENCES:
                return Color.GREEN;
            default:
                return Color.WHITE;
        }
    }
    
    private Color getConnectionColor() {
        switch (style) {
            case HIGH_CONTRAST:
                return JBColor.GREEN;
            case COLORFUL:
                return JBColor.BLUE;
            default:
                return JBColor.GRAY;
        }
    }
    
    private Font getNodeFont() {
        switch (style) {
            case DETAILED:
                return new Font(Font.SANS_SERIF, Font.BOLD, 12);
            case MINIMAL:
                return new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            default:
                return new Font(Font.SANS_SERIF, Font.PLAIN, 11);
        }
    }
    
    private Color getBackgroundColor() {
        switch (style) {
            case HIGH_CONTRAST:
                return Color.BLACK;
            default:
                return Color.WHITE;
        }
    }
    
    private String truncateText(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }
        
        // 截断文本并添加省略号
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (metrics.stringWidth(sb + "...") > maxWidth) {
                sb.setLength(sb.length() - 1);
                break;
            }
        }
        return sb + "...";
    }
    
    /**
     * 导出为PNG文件
     * @param filePath 文件路径
     */
    public void exportAsPNG(@NotNull String filePath) {
        // 实际实现需要使用Java的图像处理API
        // 这里简化处理
        try {
            File file = new File(filePath);
            file.createNewFile();
            // 导出逻辑...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 导出为SVG文件
     * @param filePath 文件路径
     */
    public void exportAsSVG(@NotNull String filePath) {
        // 实际实现需要生成SVG格式的XML
        // 这里简化处理
        try {
            File file = new File(filePath);
            file.createNewFile();
            // 导出逻辑...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 设置可视化样式
     * @param style 样式枚举
     */
    public void setStyle(@NotNull VisualizationService.VisualizationStyle style) {
        this.style = style;
    }
    
    private void showErrorMessage(String message) {
        JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE);
    }
    
    /**
     * 树度量类，用于存储树的尺寸信息
     */
    private static class TreeMetrics {
        int width;
        int height;
        int totalWidth;
        int totalHeight;
        int maxLevelWidth;
        int maxDepth;
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        rootNode = null;
    }
}