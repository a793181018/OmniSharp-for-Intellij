package com.intellij.plugins.omnisharp.project.dependencies;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.intellij.plugins.omnisharp.project.dependencies.DependencyType;

/**
 * OmniSharp依赖图可视化实现
 * 提供依赖图的多种格式导出和可视化功能
 */
public class OmniSharpDependencyVisualizer implements DependencyVisualizer {
    private static final Logger logger = Logger.getLogger(OmniSharpDependencyVisualizer.class.getName());
    private final VisualizationOptions options;
    
    public OmniSharpDependencyVisualizer() {
        this.options = new VisualizationOptions();
    }
    
    public OmniSharpDependencyVisualizer(VisualizationOptions options) {
        this.options = options;
    }
    
    @Override
    public String exportToDot(DependencyGraph graph) {
        StringBuilder dotBuilder = new StringBuilder();
        
        // 创建有向图
        dotBuilder.append("digraph DependencyGraph {");
        dotBuilder.append("\n");
        
        // 设置图属性
        dotBuilder.append("  bgcolor=\"").append(options.getBackgroundColor()).append("\"");
        dotBuilder.append("\n");
        dotBuilder.append("  fontsize=").append(options.getFontSize());
        dotBuilder.append("\n");
        dotBuilder.append("  node [fontsize=").append(options.getFontSize()).append(", width=").append(options.getNodeSize() / 10.0).append(", height=").append(options.getNodeSize() / 20.0).append("]");
        dotBuilder.append("\n");
        dotBuilder.append("  rankdir=TB;");
        dotBuilder.append("\n\n");
        
        // 添加节点
        for (DependencyNode node : graph.getAllNodes()) {
            if (shouldIncludeNode(node)) {
                dotBuilder.append("  ").append(getNodeId(node)).append(" [");
                dotBuilder.append("label=\"").append(getNodeLabel(node)).append("\", ");
                dotBuilder.append("fillcolor=\"").append(getNodeColor(node)).append("\", ");
                dotBuilder.append("style=filled, shape=box");
                dotBuilder.append("];");
                dotBuilder.append("\n");
            }
        }
        
        dotBuilder.append("\n");
        
        // 添加边
        for (DependencyEdge edge : graph.getAllEdges()) {
            if (shouldIncludeNode(edge.getSource()) && shouldIncludeNode(edge.getTarget())) {
                dotBuilder.append("  ").append(getNodeId(edge.getSource())).append(" -> ").append(getNodeId(edge.getTarget()));
                dotBuilder.append(" [");
                
                // 设置边的属性
                String edgeLabel = getEdgeLabel(edge);
                if (!edgeLabel.isEmpty()) {
                    dotBuilder.append("label=\"").append(edgeLabel).append("\", ");
                }
                
                // 设置边的颜色
                String edgeColor = getEdgeColor(edge);
                dotBuilder.append("color=\"").append(edgeColor).append("\"");
                
                dotBuilder.append("];");
                dotBuilder.append("\n");
            }
        }
        
        dotBuilder.append("}");
        
        return dotBuilder.toString();
    }
    
    @Override
    public void exportToDotFile(DependencyGraph graph, Path filePath) throws IOException {
        String dotContent = exportToDot(graph);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath.toFile()))) {
            writer.write(dotContent);
        }
        logger.info("依赖图已导出为DOT文件: " + filePath);
    }
    
    @Override
    public String exportToSvg(DependencyGraph graph) throws IOException {
        // 生成临时DOT文件
        Path tempDir = Files.createTempDirectory("dependency-visualizer");
        Path dotFilePath = tempDir.resolve("graph.dot");
        Path svgFilePath = tempDir.resolve("graph.svg");
        
        try {
            // 导出DOT文件
            exportToDotFile(graph, dotFilePath);
            
            // 使用Graphviz生成SVG
            Process process = new ProcessBuilder("dot", "-Tsvg", dotFilePath.toString(), "-o", svgFilePath.toString())
                    .redirectErrorStream(true)
                    .start();
            
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                throw new IOException("Graphviz处理超时");
            }
            
            if (process.exitValue() != 0) {
                throw new IOException("Graphviz处理失败，退出码: " + process.exitValue());
            }
            
            // 读取SVG内容
            return new String(Files.readAllBytes(svgFilePath));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("处理被中断", e);
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(dotFilePath);
                Files.deleteIfExists(svgFilePath);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                logger.warning("清理临时文件失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void exportToSvgFile(DependencyGraph graph, Path filePath) throws IOException {
        String svgContent = exportToSvg(graph);
        Files.write(filePath, svgContent.getBytes());
        logger.info("依赖图已导出为SVG文件: " + filePath);
    }
    
    @Override
    public byte[] exportToPng(DependencyGraph graph) throws IOException {
        // 生成临时DOT文件
        Path tempDir = Files.createTempDirectory("dependency-visualizer");
        Path dotFilePath = tempDir.resolve("graph.dot");
        Path pngFilePath = tempDir.resolve("graph.png");
        
        try {
            // 导出DOT文件
            exportToDotFile(graph, dotFilePath);
            
            // 使用Graphviz生成PNG
            Process process = new ProcessBuilder("dot", "-Tpng", dotFilePath.toString(), "-o", pngFilePath.toString())
                    .redirectErrorStream(true)
                    .start();
            
            boolean completed = process.waitFor(10, TimeUnit.SECONDS);
            if (!completed) {
                process.destroy();
                throw new IOException("Graphviz处理超时");
            }
            
            if (process.exitValue() != 0) {
                throw new IOException("Graphviz处理失败，退出码: " + process.exitValue());
            }
            
            // 读取PNG内容
            return Files.readAllBytes(pngFilePath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("处理被中断", e);
        } finally {
            // 清理临时文件
            try {
                Files.deleteIfExists(dotFilePath);
                Files.deleteIfExists(pngFilePath);
                Files.deleteIfExists(tempDir);
            } catch (IOException e) {
                logger.warning("清理临时文件失败: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void exportToPngFile(DependencyGraph graph, Path filePath) throws IOException {
        byte[] pngData = exportToPng(graph);
        Files.write(filePath, pngData);
        logger.info("依赖图已导出为PNG文件: " + filePath);
    }
    
    @Override
    public String generateInteractiveHtml(DependencyGraph graph, List<Cycle> cycles, List<PackageVersionConflict> conflicts) {
        StringBuilder htmlBuilder = new StringBuilder();
        
        // HTML头部
        htmlBuilder.append("<!DOCTYPE html>");
        htmlBuilder.append("<html lang=\"zh-CN\">");
        htmlBuilder.append("<head>");
        htmlBuilder.append("<meta charset=\"UTF-8\">");
        htmlBuilder.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        htmlBuilder.append("<title>依赖图可视化</title>");
        htmlBuilder.append("<style>");
        htmlBuilder.append("  body { font-family: Arial, sans-serif; margin: 20px; }");
        htmlBuilder.append("  h1 { color: #333; }");
        htmlBuilder.append("  .stats { background: #f5f5f5; padding: 15px; border-radius: 5px; margin-bottom: 20px; }");
        htmlBuilder.append("  .graph-container { border: 1px solid #ddd; border-radius: 5px; overflow: auto; }");
        htmlBuilder.append("  .legend { display: flex; flex-wrap: wrap; gap: 15px; margin-bottom: 15px; }");
        htmlBuilder.append("  .legend-item { display: flex; align-items: center; }");
        htmlBuilder.append("  .legend-color { width: 20px; height: 20px; margin-right: 5px; border-radius: 3px; }");
        htmlBuilder.append("  .cycle-warning { background: #ffebee; border-left: 4px solid #f44336; padding: 10px; margin-top: 20px; }");
        htmlBuilder.append("  .conflict-warning { background: #fff8e1; border-left: 4px solid #ff9800; padding: 10px; margin-top: 20px; }");
        htmlBuilder.append("</style>");
        htmlBuilder.append("</head>");
        htmlBuilder.append("<body>");
        
        // 标题
        htmlBuilder.append("<h1>项目依赖图可视化</h1>");
        
        // 统计信息
        VisualizationStats stats = getVisualizationStats(graph);
        htmlBuilder.append("<div class=\"stats\">");
        htmlBuilder.append("<h3>统计信息</h3>");
        htmlBuilder.append("<p>总节点数: ").append(stats.getNodeCount()).append("</p>");
        htmlBuilder.append("<p>总边数: ").append(stats.getEdgeCount()).append("</p>");
        htmlBuilder.append("<p>项目节点数: ").append(stats.getProjectNodeCount()).append("</p>");
        htmlBuilder.append("<p>包节点数: ").append(stats.getPackageNodeCount()).append("</p>");
        htmlBuilder.append("<p>循环依赖数: ").append(stats.getCycleCount()).append("</p>");
        htmlBuilder.append("<p>版本冲突数: ").append(stats.getConflictCount()).append("</p>");
        htmlBuilder.append("</div>");
        
        // 图例
        htmlBuilder.append("<div class=\"legend\">");
        htmlBuilder.append("<div class=\"legend-item\"><div class=\"legend-color\" style=\"background-color: ")
                .append(options.getProjectNodeColor()).append("\"></div>项目</div>");
        htmlBuilder.append("<div class=\"legend-item\"><div class=\"legend-color\" style=\"background-color: ")
                .append(options.getPackageNodeColor()).append("\"></div>NuGet包</div>");
        htmlBuilder.append("<div class=\"legend-item\"><div class=\"legend-color\" style=\"background-color: ")
                .append(options.getCycleNodeColor()).append("\"></div>循环依赖</div>");
        htmlBuilder.append("<div class=\"legend-item\"><div class=\"legend-color\" style=\"background-color: ")
                .append(options.getConflictNodeColor()).append("\"></div>版本冲突</div>");
        htmlBuilder.append("</div>");
        
        // 依赖图（使用DOT格式嵌入）
        htmlBuilder.append("<div class=\"graph-container\">");
        htmlBuilder.append("<pre style=\"white-space: pre-wrap; word-wrap: break-word;\">");
        htmlBuilder.append(exportToDot(graph));
        htmlBuilder.append("</pre>");
        htmlBuilder.append("</div>");
        
        // 循环依赖警告
        if (cycles != null && !cycles.isEmpty()) {
            htmlBuilder.append("<div class=\"cycle-warning\">");
            htmlBuilder.append("<h3>检测到循环依赖</h3>");
            for (Cycle cycle : cycles) {
                htmlBuilder.append("<p>循环路径: ").append(cycle.toString()).append("</p>");
            }
            htmlBuilder.append("</div>");
        }
        
        // 版本冲突警告
        if (conflicts != null && !conflicts.isEmpty()) {
            htmlBuilder.append("<div class=\"conflict-warning\">");
            htmlBuilder.append("<h3>检测到版本冲突</h3>");
            for (PackageVersionConflict conflict : conflicts) {
                htmlBuilder.append("<p>包: ").append(conflict.getPackageId()).append(", 冲突版本: ");
                htmlBuilder.append(String.join(", ", conflict.getConflictingVersions())).append("</p>");
            }
            htmlBuilder.append("</div>");
        }
        
        htmlBuilder.append("</body>");
        htmlBuilder.append("</html>");
        
        return htmlBuilder.toString();
    }
    
    @Override
    public void generateInteractiveHtmlFile(DependencyGraph graph, List<Cycle> cycles, 
                                           List<PackageVersionConflict> conflicts, Path filePath) throws IOException {
        String htmlContent = generateInteractiveHtml(graph, cycles, conflicts);
        Files.write(filePath, htmlContent.getBytes());
        logger.info("交互式HTML可视化已导出: " + filePath);
    }
    
    @Override
    public VisualizationStats getVisualizationStats(DependencyGraph graph) {
        int projectNodeCount = 0;
        int packageNodeCount = 0;
        
        for (DependencyNode node : graph.getAllNodes()) {
            if (shouldIncludeNode(node)) {
                if (node.getType() == DependencyType.PROJECT) {
                    projectNodeCount++;
                } else if (node.getType() == DependencyType.PACKAGE) {
                    packageNodeCount++;
                }
            }
        }
        
        // 计算实际显示的边数
        int edgeCount = 0;
        for (DependencyEdge edge : graph.getAllEdges()) {
            if (shouldIncludeNode(edge.getSource()) && shouldIncludeNode(edge.getTarget())) {
                edgeCount++;
            }
        }
        
        return new VisualizationStats(
                graph.getAllNodes().size(),
                edgeCount,
                projectNodeCount,
                packageNodeCount,
                0, // 这里可以通过CycleDetector计算，但暂时设为0
                0  // 这里可以通过CycleDetector计算，但暂时设为0
        );
    }
    
    // 辅助方法
    private boolean shouldIncludeNode(DependencyNode node) {
        if (node.getType() == DependencyType.PROJECT && !options.isShowProjectNodes()) {
            return false;
        }
        if (node.getType() == DependencyType.PACKAGE && !options.isShowPackageNodes()) {
            return false;
        }
        return true;
    }
    
    private String getNodeId(DependencyNode node) {
        // 确保ID在DOT语言中有效
        return "node_" + node.getId().replaceAll("[^a-zA-Z0-9_]", "_");
    }
    
    private String getNodeLabel(DependencyNode node) {
        if (node.getType() == DependencyType.PROJECT) {
            return node.getName();
        } else if (node.getType() == DependencyType.PACKAGE) {
            if (node instanceof PackageNode) {
                return ((PackageNode) node).getName() + " (Package)";
            }
        }
        return node.getName();
    }
    
    private String getNodeColor(DependencyNode node) {
        if (node.getType() == DependencyType.PROJECT) {
            return options.getProjectNodeColor();
        } else if (node.getType() == DependencyType.PACKAGE) {
            return options.getPackageNodeColor();
        }
        return "#CCCCCC";
    }
    
    private String getEdgeLabel(DependencyEdge edge) {
        switch (edge.getType()) {
            case PROJECT_REFERENCE:
                return "项目引用";
            case PACKAGE_REFERENCE:
                return "包引用";
            case TRANSITIVE_DEPENDENCY:
                return "传递依赖";
            default:
                return "";
        }
    }
    
    private String getEdgeColor(DependencyEdge edge) {
        switch (edge.getType()) {
            case PROJECT_REFERENCE:
                return "#4CAF50";
            case PACKAGE_REFERENCE:
                return "#2196F3";
            case TRANSITIVE_DEPENDENCY:
                return "#9E9E9E";
            default:
                return "#666666";
        }
    }
    
    public VisualizationOptions getOptions() {
        return options;
    }
}