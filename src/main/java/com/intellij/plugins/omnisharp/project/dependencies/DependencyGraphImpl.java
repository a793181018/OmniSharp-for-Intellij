package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 依赖图实现
 * 线程安全的依赖图实现，使用读写锁保证并发安全
 */
public class DependencyGraphImpl implements DependencyGraph {
    private final ConcurrentHashMap<String, DependencyNode> nodes;
    private final ConcurrentHashMap<DependencyNode, List<DependencyEdge>> outgoingEdges;
    private final ConcurrentHashMap<DependencyNode, List<DependencyEdge>> incomingEdges;
    private final ReadWriteLock lock;
    
    public DependencyGraphImpl() {
        this.nodes = new ConcurrentHashMap<>();
        this.outgoingEdges = new ConcurrentHashMap<>();
        this.incomingEdges = new ConcurrentHashMap<>();
        this.lock = new ReentrantReadWriteLock();
    }
    
    @Override
    public void addNode(DependencyNode node) {
        lock.writeLock().lock();
        try {
            nodes.put(node.getId(), node);
            outgoingEdges.computeIfAbsent(node, k -> new ArrayList<>());
            incomingEdges.computeIfAbsent(node, k -> new ArrayList<>());
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void addEdge(DependencyEdge edge) {
        lock.writeLock().lock();
        try {
            // 确保源节点和目标节点已经在图中
            addNode(edge.getSource());
            addNode(edge.getTarget());
            
            // 添加出边
            outgoingEdges.get(edge.getSource()).add(edge);
            // 添加入边
            incomingEdges.get(edge.getTarget()).add(edge);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Set<DependencyNode> getAllNodes() {
        lock.readLock().lock();
        try {
            return new HashSet<>(nodes.values());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public Set<DependencyEdge> getAllEdges() {
        lock.readLock().lock();
        try {
            Set<DependencyEdge> allEdges = new HashSet<>();
            for (List<DependencyEdge> edges : outgoingEdges.values()) {
                allEdges.addAll(edges);
            }
            return allEdges;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public DependencyNode getNodeById(String id) {
        lock.readLock().lock();
        try {
            return nodes.get(id);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<DependencyEdge> getOutgoingEdges(DependencyNode node) {
        lock.readLock().lock();
        try {
            List<DependencyEdge> edges = outgoingEdges.get(node);
            return edges != null ? new ArrayList<>(edges) : Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<DependencyEdge> getIncomingEdges(DependencyNode node) {
        lock.readLock().lock();
        try {
            List<DependencyEdge> edges = incomingEdges.get(node);
            return edges != null ? new ArrayList<>(edges) : Collections.emptyList();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    @Override
    public List<Cycle> findCycles() {
        lock.readLock().lock();
        try {
            List<Cycle> cycles = new ArrayList<>();
            Set<DependencyNode> visited = new HashSet<>();
            
            for (DependencyNode node : nodes.values()) {
                if (!visited.contains(node)) {
                    findCyclesDFS(node, new ArrayList<>(), new HashSet<>(), visited, cycles);
                }
            }
            
            return cycles;
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 使用深度优先搜索查找循环依赖
     */
    private void findCyclesDFS(DependencyNode current, List<DependencyNode> path, 
                              Set<DependencyNode> pathSet, Set<DependencyNode> visited, 
                              List<Cycle> cycles) {
        // 标记为已访问
        visited.add(current);
        // 添加到当前路径
        path.add(current);
        pathSet.add(current);
        
        // 遍历所有出边
        List<DependencyEdge> edges = outgoingEdges.get(current);
        if (edges != null) {
            for (DependencyEdge edge : edges) {
                DependencyNode neighbor = edge.getTarget();
                
                // 如果邻居节点已经在当前路径中，发现了循环
                if (pathSet.contains(neighbor)) {
                    // 创建循环对象
                    Cycle cycle = new Cycle();
                    // 找到邻居节点在路径中的位置
                    int index = path.indexOf(neighbor);
                    // 添加循环中的节点和边
                    for (int i = index; i < path.size(); i++) {
                        cycle.addNode(path.get(i));
                        if (i < path.size() - 1) {
                            // 查找对应的边
                            for (DependencyEdge e : outgoingEdges.get(path.get(i))) {
                                if (e.getTarget() == path.get(i + 1)) {
                                    cycle.addEdge(e);
                                    break;
                                }
                            }
                        } else {
                            // 最后一条边，从路径最后一个节点到循环起始节点
                            cycle.addEdge(edge);
                        }
                    }
                    cycles.add(cycle);
                } else if (!visited.contains(neighbor)) {
                    // 继续深度搜索
                    findCyclesDFS(neighbor, path, pathSet, visited, cycles);
                }
            }
        }
        
        // 回溯，从当前路径中移除
        path.remove(path.size() - 1);
        pathSet.remove(current);
    }
    
    @Override
    public boolean hasCycles() {
        return !findCycles().isEmpty();
    }
    
    @Override
    public void merge(DependencyGraph other) {
        lock.writeLock().lock();
        try {
            // 添加所有节点
            for (DependencyNode node : other.getAllNodes()) {
                addNode(node);
            }
            
            // 添加所有边
            for (DependencyEdge edge : other.getAllEdges()) {
                addEdge(edge);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            nodes.clear();
            outgoingEdges.clear();
            incomingEdges.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
}