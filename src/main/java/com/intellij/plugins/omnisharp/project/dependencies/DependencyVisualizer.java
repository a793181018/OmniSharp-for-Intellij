package com.intellij.plugins.omnisharp.project.dependencies;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 依赖图可视化接口
 * 用于将依赖图转换为可视化格式
 */
public interface DependencyVisualizer {
    
    /**
     * 将依赖图导出为DOT格式
     * @param graph 依赖图
     * @return DOT格式的字符串
     */
    String exportToDot(DependencyGraph graph);
    
    /**
     * 将依赖图导出为DOT文件
     * @param graph 依赖图
     * @param filePath 导出文件路径
     * @throws IOException IO异常
     */
    void exportToDotFile(DependencyGraph graph, Path filePath) throws IOException;
    
    /**
     * 将依赖图导出为SVG格式
     * @param graph 依赖图
     * @return SVG格式的字符串
     */
    String exportToSvg(DependencyGraph graph) throws IOException;
    
    /**
     * 将依赖图导出为SVG文件
     * @param graph 依赖图
     * @param filePath 导出文件路径
     * @throws IOException IO异常
     */
    void exportToSvgFile(DependencyGraph graph, Path filePath) throws IOException;
    
    /**
     * 将依赖图导出为PNG格式
     * @param graph 依赖图
     * @return PNG格式的字节数组
     */
    byte[] exportToPng(DependencyGraph graph) throws IOException;
    
    /**
     * 将依赖图导出为PNG文件
     * @param graph 依赖图
     * @param filePath 导出文件路径
     * @throws IOException IO异常
     */
    void exportToPngFile(DependencyGraph graph, Path filePath) throws IOException;
    
    /**
     * 生成依赖图的交互式HTML可视化
     * @param graph 依赖图
     * @param cycles 循环依赖列表（可选）
     * @param conflicts 版本冲突列表（可选）
     * @return HTML字符串
     */
    String generateInteractiveHtml(DependencyGraph graph, 
                                  List<Cycle> cycles, 
                                  List<PackageVersionConflict> conflicts);
    
    /**
     * 生成依赖图的交互式HTML可视化并保存到文件
     * @param graph 依赖图
     * @param cycles 循环依赖列表（可选）
     * @param conflicts 版本冲突列表（可选）
     * @param filePath 导出文件路径
     * @throws IOException IO异常
     */
    void generateInteractiveHtmlFile(DependencyGraph graph, 
                                   List<Cycle> cycles, 
                                   List<PackageVersionConflict> conflicts,
                                   Path filePath) throws IOException;
    
    /**
     * 获取依赖图的统计信息
     * @param graph 依赖图
     * @return 可视化统计信息
     */
    VisualizationStats getVisualizationStats(DependencyGraph graph);
    
    /**
     * 可视化统计信息类
     */
    class VisualizationStats {
        private final int nodeCount;
        private final int edgeCount;
        private final int projectNodeCount;
        private final int packageNodeCount;
        private final int cycleCount;
        private final int conflictCount;
        
        public VisualizationStats(int nodeCount, int edgeCount, int projectNodeCount, 
                                int packageNodeCount, int cycleCount, int conflictCount) {
            this.nodeCount = nodeCount;
            this.edgeCount = edgeCount;
            this.projectNodeCount = projectNodeCount;
            this.packageNodeCount = packageNodeCount;
            this.cycleCount = cycleCount;
            this.conflictCount = conflictCount;
        }
        
        public int getNodeCount() {
            return nodeCount;
        }
        
        public int getEdgeCount() {
            return edgeCount;
        }
        
        public int getProjectNodeCount() {
            return projectNodeCount;
        }
        
        public int getPackageNodeCount() {
            return packageNodeCount;
        }
        
        public int getCycleCount() {
            return cycleCount;
        }
        
        public int getConflictCount() {
            return conflictCount;
        }
        
        @Override
        public String toString() {
            return "VisualizationStats{" +
                   "nodes=" + nodeCount +
                   ", edges=" + edgeCount +
                   ", projects=" + projectNodeCount +
                   ", packages=" + packageNodeCount +
                   ", cycles=" + cycleCount +
                   ", conflicts=" + conflictCount +
                   '}';
        }
    }
    
    /**
     * 可视化选项
     */
    class VisualizationOptions {
        private boolean showPackageNodes = true;
        private boolean showProjectNodes = true;
        private boolean showTransitiveDependencies = true;
        private boolean highlightCycles = true;
        private boolean highlightConflicts = true;
        private int maxDepth = -1; // -1表示不限制
        private String layoutAlgorithm = "dot";
        private int nodeSize = 16;
        private int fontSize = 14;
        private String backgroundColor = "#ffffff";
        private String projectNodeColor = "#4CAF50";
        private String packageNodeColor = "#2196F3";
        private String cycleNodeColor = "#F44336";
        private String conflictNodeColor = "#FF9800";
        
        public boolean isShowPackageNodes() {
            return showPackageNodes;
        }
        
        public void setShowPackageNodes(boolean showPackageNodes) {
            this.showPackageNodes = showPackageNodes;
        }
        
        public boolean isShowProjectNodes() {
            return showProjectNodes;
        }
        
        public void setShowProjectNodes(boolean showProjectNodes) {
            this.showProjectNodes = showProjectNodes;
        }
        
        public boolean isShowTransitiveDependencies() {
            return showTransitiveDependencies;
        }
        
        public void setShowTransitiveDependencies(boolean showTransitiveDependencies) {
            this.showTransitiveDependencies = showTransitiveDependencies;
        }
        
        public boolean isHighlightCycles() {
            return highlightCycles;
        }
        
        public void setHighlightCycles(boolean highlightCycles) {
            this.highlightCycles = highlightCycles;
        }
        
        public boolean isHighlightConflicts() {
            return highlightConflicts;
        }
        
        public void setHighlightConflicts(boolean highlightConflicts) {
            this.highlightConflicts = highlightConflicts;
        }
        
        public int getMaxDepth() {
            return maxDepth;
        }
        
        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
        
        public String getLayoutAlgorithm() {
            return layoutAlgorithm;
        }
        
        public void setLayoutAlgorithm(String layoutAlgorithm) {
            this.layoutAlgorithm = layoutAlgorithm;
        }
        
        public int getNodeSize() {
            return nodeSize;
        }
        
        public void setNodeSize(int nodeSize) {
            this.nodeSize = nodeSize;
        }
        
        public int getFontSize() {
            return fontSize;
        }
        
        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }
        
        public String getBackgroundColor() {
            return backgroundColor;
        }
        
        public void setBackgroundColor(String backgroundColor) {
            this.backgroundColor = backgroundColor;
        }
        
        public String getProjectNodeColor() {
            return projectNodeColor;
        }
        
        public void setProjectNodeColor(String projectNodeColor) {
            this.projectNodeColor = projectNodeColor;
        }
        
        public String getPackageNodeColor() {
            return packageNodeColor;
        }
        
        public void setPackageNodeColor(String packageNodeColor) {
            this.packageNodeColor = packageNodeColor;
        }
        
        public String getCycleNodeColor() {
            return cycleNodeColor;
        }
        
        public void setCycleNodeColor(String cycleNodeColor) {
            this.cycleNodeColor = cycleNodeColor;
        }
        
        public String getConflictNodeColor() {
            return conflictNodeColor;
        }
        
        public void setConflictNodeColor(String conflictNodeColor) {
            this.conflictNodeColor = conflictNodeColor;
        }
    }
}