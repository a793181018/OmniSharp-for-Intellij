package com.omnisharp.intellij.projectstructure.model;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 表示项目依赖图
 */
public class DependencyGraph {
    private final Map<String, Set<String>> dependencies; // projectId -> dependent projectIds

    public DependencyGraph(Map<String, Set<String>> dependencies) {
        this.dependencies = new HashMap<>();
        if (dependencies != null) {
            dependencies.forEach((key, value) -> 
                    this.dependencies.put(key, new HashSet<>(value))
            );
        }
    }

    public Set<String> getDependencies(String projectId) {
        return new HashSet<>(dependencies.getOrDefault(projectId, Collections.emptySet()));
    }

    public void addDependency(String sourceProjectId, String targetProjectId) {
        dependencies.computeIfAbsent(sourceProjectId, k -> new HashSet<>()).add(targetProjectId);
    }

    public boolean hasCircularDependencies() {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();

        for (String projectId : dependencies.keySet()) {
            if (!visited.contains(projectId) && 
                hasCircularDependenciesUtil(projectId, visited, recursionStack)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCircularDependenciesUtil(String projectId, Set<String> visited, Set<String> recursionStack) {
        visited.add(projectId);
        recursionStack.add(projectId);

        Set<String> deps = dependencies.getOrDefault(projectId, Collections.emptySet());
        for (String dep : deps) {
            if (!visited.contains(dep) && hasCircularDependenciesUtil(dep, visited, recursionStack)) {
                return true;
            } else if (recursionStack.contains(dep)) {
                return true;
            }
        }

        recursionStack.remove(projectId);
        return false;
    }

    public List<String> topologicalSort() {
        Set<String> visited = new HashSet<>();
        List<String> result = new ArrayList<>();

        // 首先添加所有可能的项目ID
        Set<String> allProjects = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allProjects::addAll);

        for (String projectId : allProjects) {
            if (!visited.contains(projectId)) {
                topologicalSortUtil(projectId, visited, result);
            }
        }

        Collections.reverse(result);
        return result;
    }

    private void topologicalSortUtil(String projectId, Set<String> visited, List<String> result) {
        visited.add(projectId);

        Set<String> deps = dependencies.getOrDefault(projectId, Collections.emptySet());
        for (String dep : deps) {
            if (!visited.contains(dep)) {
                topologicalSortUtil(dep, visited, result);
            }
        }

        result.add(projectId);
    }

    public List<List<String>> findCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String projectId : dependencies.keySet()) {
            if (!visited.contains(projectId)) {
                List<String> path = new ArrayList<>();
                findCyclesUtil(projectId, visited, new HashSet<>(), path, cycles);
            }
        }

        return cycles;
    }

    private void findCyclesUtil(String projectId, Set<String> visited, Set<String> pathSet,
                              List<String> path, List<List<String>> cycles) {
        if (pathSet.contains(projectId)) {
            // 找到循环
            int index = path.indexOf(projectId);
            if (index >= 0) {
                List<String> cycle = new ArrayList<>(path.subList(index, path.size()));
                cycle.add(projectId);
                cycles.add(cycle);
            }
            return;
        }

        if (visited.contains(projectId)) {
            return;
        }

        visited.add(projectId);
        pathSet.add(projectId);
        path.add(projectId);

        Set<String> deps = dependencies.getOrDefault(projectId, Collections.emptySet());
        for (String dep : deps) {
            findCyclesUtil(dep, visited, pathSet, path, cycles);
        }

        pathSet.remove(projectId);
        path.remove(path.size() - 1);
    }
    
    /**
     * 获取所有项目名称
     * @return 项目名称集合
     */
    public Set<String> getProjects() {
        Set<String> allProjects = new HashSet<>(dependencies.keySet());
        dependencies.values().forEach(allProjects::addAll);
        return allProjects;
    }
    
    /**
     * 获取依赖关系映射（兼容可视化组件）
     * @return 项目到其依赖引用的映射
     */
    public Map<String, Set<ProjectReference>> getDependencies() {
        Map<String, Set<ProjectReference>> result = new HashMap<>();
        dependencies.forEach((projectId, depProjects) -> {
            Set<ProjectReference> refs = new HashSet<>();
            depProjects.forEach(dep -> refs.add(new ProjectReference(dep)));
            result.put(projectId, refs);
        });
        return result;
    }
}