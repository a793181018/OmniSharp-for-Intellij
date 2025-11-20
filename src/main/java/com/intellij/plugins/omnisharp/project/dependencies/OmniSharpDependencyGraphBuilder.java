package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * OmniSharp依赖图构建器实现
 * 负责构建和合并依赖图
 */
public class OmniSharpDependencyGraphBuilder implements DependencyGraphBuilder {
    private static final Logger logger = Logger.getLogger(OmniSharpDependencyGraphBuilder.class.getName());
    
    @Override
    public DependencyGraph buildProjectDependencyGraph(ProjectResolver.ProjectInfo projectInfo,
                                                      List<ProjectDependency> projectDependencies,
                                                      List<PackageDependency> packageDependencies) {
        logger.info("开始构建项目依赖图: " + projectInfo.getName());
        
        DependencyGraph graph = new DependencyGraphImpl();
        Map<String, DependencyNode> nodeMap = new HashMap<>();
        
        // 1. 添加项目节点
        ProjectNode projectNode = new ProjectNode(
            projectInfo.getProjectPath().toString(),
            projectInfo.getName(),
            projectInfo.getProjectFilePath().toString()
        );
        graph.addNode(projectNode);
        nodeMap.put(projectNode.getId(), projectNode);
        
        // 2. 添加项目引用依赖
        for (ProjectDependency dependency : projectDependencies) {
            // 添加目标项目节点
            ProjectNode targetNode = new ProjectNode(
                dependency.getTargetProjectPath(),
                dependency.getTargetProjectName(),
                dependency.getTargetProjectPath() // 简化处理，实际应从项目信息中获取
            );
            graph.addNode(targetNode);
            nodeMap.put(targetNode.getId(), targetNode);
            
            // 添加依赖边
            DependencyEdge edge = new DependencyEdge(
                nodeMap.get(dependency.getSourceProjectPath()),
                targetNode,
                EdgeType.PROJECT_REFERENCE
            );
            graph.addEdge(edge);
        }
        
        // 3. 添加包依赖
        for (PackageDependency dependency : packageDependencies) {
            // 添加包节点
            PackageNode packageNode = new PackageNode(
                dependency.getPackageId(),
                dependency.getResolvedVersion()
            );
            graph.addNode(packageNode);
            nodeMap.put(packageNode.getId(), packageNode);
            
            // 添加依赖边
            DependencyEdge edge = new DependencyEdge(
                nodeMap.get(dependency.getProjectPath()),
                packageNode,
                dependency.isTransitive() ? EdgeType.TRANSITIVE_DEPENDENCY : EdgeType.PACKAGE_REFERENCE
            );
            graph.addEdge(edge);
        }
        
        logger.info("项目依赖图构建完成: " + projectInfo.getName() + 
                   ", 节点数: " + graph.getAllNodes().size() + 
                   ", 边数: " + graph.getAllEdges().size());
        return graph;
    }
    
    @Override
    public DependencyGraph buildPackageDependencyGraph(List<PackageDependency> packageDependencies) {
        logger.info("开始构建包依赖图，依赖数量: " + packageDependencies.size());
        
        DependencyGraph graph = new DependencyGraphImpl();
        Map<String, PackageNode> packageNodeMap = new HashMap<>();
        Map<String, ProjectNode> projectNodeMap = new HashMap<>();
        
        for (PackageDependency dependency : packageDependencies) {
            // 添加项目节点
            ProjectNode projectNode = projectNodeMap.computeIfAbsent(
                dependency.getProjectPath(),
                k -> new ProjectNode(
                    dependency.getProjectPath(),
                    dependency.getProjectName(),
                    dependency.getProjectPath()
                )
            );
            graph.addNode(projectNode);
            
            // 添加包节点
            PackageNode packageNode = packageNodeMap.computeIfAbsent(
                dependency.getPackageId() + ":" + dependency.getResolvedVersion(),
                k -> new PackageNode(
                    dependency.getPackageId(),
                    dependency.getResolvedVersion()
                )
            );
            graph.addNode(packageNode);
            
            // 添加依赖边
            DependencyEdge edge = new DependencyEdge(
                projectNode,
                packageNode,
                dependency.isTransitive() ? EdgeType.TRANSITIVE_DEPENDENCY : EdgeType.PACKAGE_REFERENCE
            );
            graph.addEdge(edge);
        }
        
        logger.info("包依赖图构建完成，节点数: " + graph.getAllNodes().size() + 
                   ", 边数: " + graph.getAllEdges().size());
        return graph;
    }
    
    @Override
    public DependencyGraph mergeDependencyGraphs(List<DependencyGraph> graphs) {
        logger.info("开始合并依赖图，图数量: " + graphs.size());
        
        DependencyGraph mergedGraph = new DependencyGraphImpl();
        
        // 合并所有图
        for (DependencyGraph graph : graphs) {
            mergedGraph.merge(graph);
        }
        
        logger.info("依赖图合并完成，节点数: " + mergedGraph.getAllNodes().size() + 
                   ", 边数: " + mergedGraph.getAllEdges().size());
        return mergedGraph;
    }
}