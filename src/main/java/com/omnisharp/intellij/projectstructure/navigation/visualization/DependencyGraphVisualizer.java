package com.omnisharp.intellij.projectstructure.navigation.visualization;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.omnisharp.intellij.projectstructure.model.DependencyGraph;
import com.omnisharp.intellij.projectstructure.model.ProjectReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 依赖关系图可视化器，用于可视化项目间的依赖关系
 */
public class DependencyGraphVisualizer {
    private final Project project;
    private DependencyGraph currentGraph;
    private JComponent visualizationComponent;
    private VisualizationService.VisualizationStyle style;
    private final Map<String, Color> projectColors;
    
    public DependencyGraphVisualizer(@NotNull Project project) {
        this.project = project;
        this.style = VisualizationService.VisualizationStyle.DEFAULT;
        this.projectColors = new ConcurrentHashMap<>();
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
                if (currentGraph != null) {
                    paintGraph(g, this.getWidth(), this.getHeight());
                }
            }
        };
        panel.setPreferredSize(new Dimension(800, 600));
        panel.setBackground(getBackgroundColor());
        return new JBScrollPane(panel);
    }
    
    /**
     * 可视化依赖关系图
     * @param graph 依赖关系图
     */
    public void visualize(@NotNull DependencyGraph graph) {
        this.currentGraph = graph;
        generateProjectColors();
        
        // 创建并显示对话框
        DialogWrapper dialog = new DialogWrapper(project) {
            {
                init();
                setTitle("项目依赖关系可视化");
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
    
    private void paintGraph(Graphics g, int width, int height) {
        Graphics2D g2d = (Graphics2D) g;
        
        // 设置渲染质量
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        // 计算节点位置（简化的圆形布局）
        Map<String, Point> nodePositions = calculateNodePositions(width, height);
        
        // 绘制依赖关系边
        paintEdges(g2d, nodePositions);
        
        // 绘制项目节点
        paintNodes(g2d, nodePositions);
        
        // 绘制标签
        paintLabels(g2d, nodePositions);
    }
    
    @NotNull
    private Map<String, Point> calculateNodePositions(int width, int height) {
        Map<String, Point> positions = new ConcurrentHashMap<>();
        if (currentGraph == null) {
            return positions;
        }
        
        Set<String> projects = currentGraph.getProjects();
        int projectCount = projects.size();
        
        if (projectCount == 0) {
            return positions;
        }
        
        // 中心坐标
        int centerX = width / 2;
        int centerY = height / 2;
        
        // 半径（留出边距）
        int radius = Math.min(width, height) / 3;
        
        // 计算每个项目的位置（圆形布局）
        int i = 0;
        for (String project : projects) {
            double angle = 2 * Math.PI * i / projectCount;
            int x = centerX + (int) (radius * Math.cos(angle));
            int y = centerY + (int) (radius * Math.sin(angle));
            positions.put(project, new Point(x, y));
            i++;
        }
        
        return positions;
    }
    
    private void paintEdges(Graphics2D g2d, Map<String, Point> nodePositions) {
        if (currentGraph == null) {
            return;
        }
        
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.setColor(getEdgeColor());
        
        // 绘制每条依赖边
        for (Map.Entry<String, Set<ProjectReference>> entry : currentGraph.getDependencies().entrySet()) {
            String sourceProject = entry.getKey();
            Point sourcePos = nodePositions.get(sourceProject);
            
            if (sourcePos != null) {
                for (ProjectReference ref : entry.getValue()) {
                    Point targetPos = nodePositions.get(ref.getName());
                    if (targetPos != null) {
                        // 绘制箭头
                        drawArrow(g2d, sourcePos.x, sourcePos.y, targetPos.x, targetPos.y);
                    }
                }
            }
        }
    }
    
    private void drawArrow(Graphics2D g2d, int x1, int y1, int x2, int y2) {
        // 绘制线条
        g2d.drawLine(x1, y1, x2, y2);
        
        // 绘制箭头
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowLength = 10;
        
        g2d.drawLine(
                x2, y2,
                x2 - (int) (arrowLength * Math.cos(angle - Math.PI / 6)),
                y2 - (int) (arrowLength * Math.sin(angle - Math.PI / 6))
        );
        
        g2d.drawLine(
                x2, y2,
                x2 - (int) (arrowLength * Math.cos(angle + Math.PI / 6)),
                y2 - (int) (arrowLength * Math.sin(angle + Math.PI / 6))
        );
    }
    
    private void paintNodes(Graphics2D g2d, Map<String, Point> nodePositions) {
        int nodeSize = getNodeSize();
        
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            String projectName = entry.getKey();
            Point pos = entry.getValue();
            Color color = projectColors.getOrDefault(projectName, getDefaultNodeColor());
            
            // 填充节点
            g2d.setColor(color);
            g2d.fillOval(pos.x - nodeSize / 2, pos.y - nodeSize / 2, nodeSize, nodeSize);
            
            // 绘制边框
            g2d.setColor(Color.BLACK);
            g2d.drawOval(pos.x - nodeSize / 2, pos.y - nodeSize / 2, nodeSize, nodeSize);
        }
    }
    
    private void paintLabels(Graphics2D g2d, Map<String, Point> nodePositions) {
        Font font = getLabelFont();
        g2d.setFont(font);
        g2d.setColor(getLabelColor());
        
        FontMetrics metrics = g2d.getFontMetrics();
        
        for (Map.Entry<String, Point> entry : nodePositions.entrySet()) {
            String projectName = entry.getKey();
            Point pos = entry.getValue();
            
            // 计算文本位置（居中显示在节点下方）
            int textWidth = metrics.stringWidth(projectName);
            int textHeight = metrics.getHeight();
            int x = pos.x - textWidth / 2;
            int y = pos.y + getNodeSize() / 2 + textHeight;
            
            // 绘制文本
            g2d.drawString(projectName, x, y);
        }
    }
    
    private void generateProjectColors() {
        projectColors.clear();
        if (currentGraph == null) {
            return;
        }
        
        // 预定义的颜色列表
        Color[] colors = {
                JBColor.BLUE,
                JBColor.RED,
                JBColor.GREEN,
                JBColor.MAGENTA,
                JBColor.CYAN,
                JBColor.ORANGE,
                JBColor.PINK,
                Color.YELLOW
        };
        
        int colorIndex = 0;
        for (String project : currentGraph.getProjects()) {
            projectColors.put(project, colors[colorIndex % colors.length]);
            colorIndex++;
        }
    }
    
    /**
     * 导出为PNG文件
     * @param filePath 文件路径
     */
    public void exportAsPNG(@NotNull String filePath) {
        // 实际实现需要使用Java的图像处理API
        // 这里简化处理
        try {
            // 示例代码，实际实现需要更复杂的逻辑
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
            // 示例代码，实际实现需要更复杂的逻辑
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
    
    // 根据样式获取各种视觉属性
    private Color getBackgroundColor() {
        switch (style) {
            case HIGH_CONTRAST:
                return Color.BLACK;
            default:
                return Color.WHITE;
        }
    }
    
    private Color getEdgeColor() {
        switch (style) {
            case HIGH_CONTRAST:
                return JBColor.GREEN;
            case COLORFUL:
                return JBColor.BLUE;
            default:
                return JBColor.GRAY;
        }
    }
    
    private Color getLabelColor() {
        switch (style) {
            case HIGH_CONTRAST:
                return Color.WHITE;
            case COLORFUL:
                return JBColor.DARK_GRAY;
            default:
                return Color.BLACK;
        }
    }
    
    private Color getDefaultNodeColor() {
        switch (style) {
            case COLORFUL:
                return JBColor.ORANGE;
            case HIGH_CONTRAST:
                return JBColor.CYAN;
            case MINIMAL:
                return JBColor.LIGHT_GRAY;
            default:
                return JBColor.BLUE;
        }
    }
    
    private Font getLabelFont() {
        switch (style) {
            case DETAILED:
                return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
            case MINIMAL:
                return new Font(Font.SANS_SERIF, Font.PLAIN, 10);
            default:
                return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
        }
    }
    
    private int getNodeSize() {
        switch (style) {
            case DETAILED:
                return 40;
            case MINIMAL:
                return 20;
            default:
                return 30;
        }
    }
    
    /**
     * 清理资源
     */
    public void dispose() {
        projectColors.clear();
        currentGraph = null;
    }
}