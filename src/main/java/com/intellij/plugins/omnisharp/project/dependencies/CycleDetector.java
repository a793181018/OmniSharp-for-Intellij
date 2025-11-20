package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * 循环依赖检测器
 * 用于检测依赖图中的循环依赖
 */
public class CycleDetector {
    private static final Logger logger = Logger.getLogger(CycleDetector.class.getName());
    
    /**
     * 检测依赖图中的所有循环依赖
     * @param graph 依赖图
     * @return 循环依赖列表
     */
    public List<Cycle> detectCycles(DependencyGraph graph) {
        if (graph == null || graph.getAllNodes().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Cycle> cycles = new ArrayList<>();
        Set<DependencyNode> visited = new HashSet<>();
        
        // 对每个未访问的节点进行DFS
        for (DependencyNode node : graph.getAllNodes()) {
            if (!visited.contains(node)) {
                detectCyclesForNode(node, graph, new ArrayList<>(), new HashSet<>(), visited, cycles);
            }
        }
        
        // 去重并排序
        return removeDuplicateCycles(cycles);
    }
    
    /**
     * 对单个节点进行DFS检测循环依赖
     */
    private void detectCyclesForNode(DependencyNode node, 
                                   DependencyGraph graph, 
                                   List<DependencyNode> path, 
                                   Set<DependencyNode> pathSet, 
                                   Set<DependencyNode> visited, 
                                   List<Cycle> cycles) {
        // 添加当前节点到路径中
        path.add(node);
        pathSet.add(node);
        
        // 获取当前节点的所有出边（依赖）
        List<DependencyEdge> outgoingEdges = graph.getOutgoingEdges(node);
        
        for (DependencyEdge edge : outgoingEdges) {
            DependencyNode neighbor = edge.getTarget();
            
            if (!pathSet.contains(neighbor)) {
                // 邻居节点不在当前路径中，继续DFS
                if (!visited.contains(neighbor)) {
                    detectCyclesForNode(neighbor, graph, path, pathSet, visited, cycles);
                }
            } else {
                // 发现循环依赖
                int index = path.indexOf(neighbor);
                if (index != -1) {
                    // 提取循环路径
                    List<DependencyNode> cyclePath = new ArrayList<>(path.subList(index, path.size()));
                    cyclePath.add(neighbor); // 闭合循环
                    
                    // 构建循环对象
                    Cycle cycle = buildCycle(cyclePath, graph);
                    cycles.add(cycle);
                    
                    logger.warning("检测到循环依赖: " + cycle);
                }
            }
        }
        
        // 回溯：从路径中移除当前节点
        path.remove(path.size() - 1);
        pathSet.remove(node);
        
        // 标记为已访问
        visited.add(node);
    }
    
    /**
     * 根据节点路径构建循环对象
     */
    private Cycle buildCycle(List<DependencyNode> nodePath, DependencyGraph graph) {
        Cycle cycle = new Cycle();
        
        // 添加节点
        for (DependencyNode node : nodePath) {
            cycle.addNode(node);
        }
        
        // 添加边
        for (int i = 0; i < nodePath.size() - 1; i++) {
            DependencyNode source = nodePath.get(i);
            DependencyNode target = nodePath.get(i + 1);
            
            // 查找对应的边
            List<DependencyEdge> edges = graph.getOutgoingEdges(source).stream()
                    .filter(edge -> edge.getTarget().equals(target))
                    .collect(java.util.stream.Collectors.toList());
            if (!edges.isEmpty()) {
                // 取第一条边（可能有多条边，但我们只关心循环）
                cycle.addEdge(edges.get(0));
            }
        }
        
        return cycle;
    }
    
    /**
     * 移除重复的循环
     */
    private List<Cycle> removeDuplicateCycles(List<Cycle> cycles) {
        Set<String> seen = new HashSet<>();
        List<Cycle> uniqueCycles = new ArrayList<>();
        
        for (Cycle cycle : cycles) {
            // 生成循环的唯一表示（排序后的节点ID）
            String cycleKey = getCycleKey(cycle);
            
            if (!seen.contains(cycleKey)) {
                seen.add(cycleKey);
                uniqueCycles.add(cycle);
            }
        }
        
        // 按长度排序，长的循环排在前面
        uniqueCycles.sort((c1, c2) -> Integer.compare(c2.getLength(), c1.getLength()));
        
        return uniqueCycles;
    }
    
    /**
     * 获取循环的唯一键
     */
    private String getCycleKey(Cycle cycle) {
        List<String> nodeIds = cycle.getNodes().stream()
                .map(DependencyNode::getId)
                .collect(Collectors.toList());
        
        // 找到最小的节点ID作为起点，确保循环的表示是唯一的
        int minIndex = 0;
        for (int i = 1; i < nodeIds.size(); i++) {
            if (nodeIds.get(i).compareTo(nodeIds.get(minIndex)) < 0) {
                minIndex = i;
            }
        }
        
        // 旋转列表，以最小节点ID为起点
        List<String> rotated = new ArrayList<>();
        for (int i = 0; i < nodeIds.size(); i++) {
            int index = (minIndex + i) % nodeIds.size();
            rotated.add(nodeIds.get(index));
        }
        
        return String.join("->", rotated);
    }
    
    /**
     * 检测特定节点是否参与循环依赖
     */
    public boolean isInCycle(DependencyNode node, DependencyGraph graph) {
        List<Cycle> cycles = detectCycles(graph);
        return cycles.stream().anyMatch(cycle -> cycle.getNodes().contains(node));
    }
    
    /**
     * 获取包含特定节点的所有循环
     */
    public List<Cycle> getCyclesContainingNode(DependencyNode node, DependencyGraph graph) {
        List<Cycle> cycles = detectCycles(graph);
        return cycles.stream()
                .filter(cycle -> cycle.getNodes().contains(node))
                .collect(Collectors.toList());
    }
    
    /**
     * 检测包版本冲突
     */
    public List<PackageVersionConflict> detectPackageVersionConflicts(List<PackageDependency> packageDependencies) {
        Map<String, Map<String, Set<PackageDependency>>> packageVersions = new ConcurrentHashMap<>();
        
        // 按包ID和版本分组
        for (PackageDependency dep : packageDependencies) {
            packageVersions.computeIfAbsent(dep.getPackageId(), k -> new ConcurrentHashMap<>())
                          .computeIfAbsent(dep.getVersion(), k -> new HashSet<>())
                          .add(dep);
        }
        
        List<PackageVersionConflict> conflicts = new ArrayList<>();
        
        // 查找有多个版本的包
        for (Map.Entry<String, Map<String, Set<PackageDependency>>> packageEntry : packageVersions.entrySet()) {
            String packageId = packageEntry.getKey();
            Map<String, Set<PackageDependency>> versions = packageEntry.getValue();
            
            if (versions.size() > 1) {
                // 有版本冲突
                Set<String> conflictingVersions = versions.keySet();
                Set<PackageDependency> allDependencies = new HashSet<>();
                
                versions.values().forEach(allDependencies::addAll);
                
                conflicts.add(new PackageVersionConflict(packageId, conflictingVersions, new ArrayList<>(allDependencies)));
                logger.warning("检测到包版本冲突: " + packageId + " 有多个版本: " + conflictingVersions);
            }
        }
        
        return conflicts;
    }
    
    /**
     * 计算依赖图的传递闭包
     * 用于确定节点之间的间接依赖关系
     */
    public Map<DependencyNode, Set<DependencyNode>> computeTransitiveClosure(DependencyGraph graph) {
        Map<DependencyNode, Set<DependencyNode>> closure = new HashMap<>();
        
        for (DependencyNode node : graph.getAllNodes()) {
            Set<DependencyNode> reachable = new HashSet<>();
            computeReachableNodes(node, graph, reachable, new HashSet<>());
            closure.put(node, reachable);
        }
        
        return closure;
    }
    
    /**
     * 计算从给定节点可达的所有节点（传递依赖）
     */
    private void computeReachableNodes(DependencyNode node, 
                                     DependencyGraph graph, 
                                     Set<DependencyNode> reachable, 
                                     Set<DependencyNode> visited) {
        if (visited.contains(node)) {
            return;
        }
        
        visited.add(node);
        
        List<DependencyEdge> outgoingEdges = graph.getOutgoingEdges(node);
        for (DependencyEdge edge : outgoingEdges) {
            DependencyNode target = edge.getTarget();
            reachable.add(target);
            computeReachableNodes(target, graph, reachable, visited);
        }
    }
    
    /**
     * 拓扑排序
     * 如果图有循环，则返回null
     */
    public List<DependencyNode> topologicalSort(DependencyGraph graph) {
        List<DependencyNode> result = new ArrayList<>();
        Set<DependencyNode> visited = new HashSet<>();
        Set<DependencyNode> recursionStack = new HashSet<>();
        boolean hasCycle = false;
        
        for (DependencyNode node : graph.getAllNodes()) {
            if (!visited.contains(node)) {
                if (!topologicalSortUtil(node, graph, visited, recursionStack, result)) {
                    hasCycle = true;
                    break;
                }
            }
        }
        
        if (hasCycle) {
            return null; // 图有循环，无法拓扑排序
        }
        
        // 反转结果，因为我们是后序遍历
        Collections.reverse(result);
        return result;
    }
    
    /**
     * 拓扑排序辅助方法
     */
    private boolean topologicalSortUtil(DependencyNode node, 
                                      DependencyGraph graph, 
                                      Set<DependencyNode> visited, 
                                      Set<DependencyNode> recursionStack, 
                                      List<DependencyNode> result) {
        visited.add(node);
        recursionStack.add(node);
        
        List<DependencyEdge> outgoingEdges = graph.getOutgoingEdges(node);
        for (DependencyEdge edge : outgoingEdges) {
            DependencyNode neighbor = edge.getTarget();
            
            if (!visited.contains(neighbor)) {
                if (!topologicalSortUtil(neighbor, graph, visited, recursionStack, result)) {
                    return false;
                }
            } else if (recursionStack.contains(neighbor)) {
                // 发现循环
                return false;
            }
        }
        
        recursionStack.remove(node);
        result.add(node);
        return true;
    }
}