package com.omnisharp.intellij.projectstructure.service;

import com.omnisharp.intellij.projectstructure.api.DependencyService;
import com.omnisharp.intellij.projectstructure.model.DependencyGraph;
import com.omnisharp.intellij.projectstructure.model.FileReference;
import com.omnisharp.intellij.projectstructure.model.PackageReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 依赖管理服务的实现类
 */
public class DependencyServiceImpl implements DependencyService {
    private final Map<String, List<String>> projectReferences = new ConcurrentHashMap<>();
    private final Map<String, List<PackageReference>> packageReferences = new ConcurrentHashMap<>();
    private final Map<String, List<FileReference>> fileReferences = new ConcurrentHashMap<>();
    private final Map<String, DependencyGraph> dependencyGraphCache = new ConcurrentHashMap<>();

    @Override
    public DependencyGraph getProjectDependencies(String projectId) {
        // 从缓存获取或计算依赖图
        return dependencyGraphCache.computeIfAbsent(projectId, this::resolveDependencyGraphInternal);
    }

    @Override
    public List<String> getProjectReferences(String projectId) {
        return projectReferences.getOrDefault(projectId, Collections.emptyList());
    }

    @Override
    public List<PackageReference> getPackageReferences(String projectId) {
        return packageReferences.getOrDefault(projectId, Collections.emptyList());
    }

    @Override
    public List<FileReference> getFileReferences(String projectId) {
        return fileReferences.getOrDefault(projectId, Collections.emptyList());
    }

    @Override
    public boolean addProjectReference(String sourceProjectId, String targetProjectId) {
        projectReferences.computeIfAbsent(sourceProjectId, k -> new ArrayList<>()).add(targetProjectId);
        // 清除缓存，以便下次获取时重新计算
        dependencyGraphCache.remove(sourceProjectId);
        return true;
    }

    @Override
    public boolean removeProjectReference(String sourceProjectId, String targetProjectId) {
        List<String> refs = projectReferences.get(sourceProjectId);
        if (refs != null) {
            boolean removed = refs.remove(targetProjectId);
            if (removed) {
                // 清除缓存
                dependencyGraphCache.remove(sourceProjectId);
            }
            return removed;
        }
        return false;
    }

    @Override
    public boolean addPackageReference(String projectId, PackageReference packageReference) {
        packageReferences.computeIfAbsent(projectId, k -> new ArrayList<>()).add(packageReference);
        return true;
    }

    @Override
    public boolean updatePackageReference(String projectId, String packageId, String newVersion) {
        List<PackageReference> packages = packageReferences.get(projectId);
        if (packages != null) {
            for (PackageReference pkg : packages) {
                if (pkg.getId().equals(packageId)) {
                    // 注意：这里应该使用新的PackageReference对象替换，因为PackageReference是不可变的
                    packages.remove(pkg);
                    PackageReference updated = new PackageReference(
                            pkg.getId(),
                            newVersion,
                            pkg.getIncludeAssets(),
                            pkg.getExcludeAssets()
                    );
                    packages.add(updated);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean removePackageReference(String projectId, String packageId) {
        List<PackageReference> packages = packageReferences.get(projectId);
        if (packages != null) {
            return packages.removeIf(pkg -> pkg.getId().equals(packageId));
        }
        return false;
    }

    @Override
    public boolean addFileReference(String projectId, FileReference fileReference) {
        fileReferences.computeIfAbsent(projectId, k -> new ArrayList<>()).add(fileReference);
        return true;
    }

    @Override
    public boolean removeFileReference(String projectId, String filePath) {
        List<FileReference> files = fileReferences.get(projectId);
        if (files != null) {
            return files.removeIf(file -> file.getPath().equals(filePath));
        }
        return false;
    }

    @Override
    public DependencyGraph resolveDependencyGraph(String projectId) {
        return resolveDependencyGraphInternal(projectId);
    }

    @Override
    public boolean hasCircularDependencies(String projectId) {
        DependencyGraph graph = getProjectDependencies(projectId);
        return graph.hasCircularDependencies();
    }

    @Override
    public Set<String> getAffectedProjects(String changedProjectId) {
        Set<String> affected = new HashSet<>();
        // 找出所有直接或间接依赖于changedProjectId的项目
        for (Map.Entry<String, List<String>> entry : projectReferences.entrySet()) {
            if (entry.getValue().contains(changedProjectId) || 
                hasTransitiveDependency(entry.getKey(), changedProjectId)) {
                affected.add(entry.getKey());
            }
        }
        return affected;
    }

    @Override
    public boolean restorePackages(String projectId) {
        // 模拟NuGet包还原过程
        // 实际实现中需要调用NuGet API或命令行工具
        return true;
    }

    @Override
    public List<String> validateDependencies(String projectId) {
        List<String> invalidDependencies = new ArrayList<>();
        // 验证项目引用
        List<String> projectRefs = getProjectReferences(projectId);
        for (String ref : projectRefs) {
            // 简单验证：检查被引用的项目是否存在
            if (!projectReferences.containsKey(ref)) {
                invalidDependencies.add("Project reference not found: " + ref);
            }
        }
        // 这里可以添加更多的验证逻辑
        return invalidDependencies;
    }

    // 内部方法：计算依赖图
    private DependencyGraph resolveDependencyGraphInternal(String projectId) {
        Map<String, Set<String>> deps = new HashMap<>();
        buildDependencyGraph(projectId, deps, new HashSet<>());
        return new DependencyGraph(deps);
    }

    // 递归构建依赖图
    private void buildDependencyGraph(String projectId, Map<String, Set<String>> deps, Set<String> visited) {
        if (visited.contains(projectId)) {
            return;
        }
        visited.add(projectId);

        List<String> refs = projectReferences.getOrDefault(projectId, Collections.emptyList());
        deps.put(projectId, new HashSet<>(refs));

        for (String ref : refs) {
            buildDependencyGraph(ref, deps, visited);
        }
    }

    // 检查是否存在传递依赖
    private boolean hasTransitiveDependency(String sourceProjectId, String targetProjectId) {
        Set<String> visited = new HashSet<>();
        return hasTransitiveDependencyUtil(sourceProjectId, targetProjectId, visited);
    }

    private boolean hasTransitiveDependencyUtil(String current, String target, Set<String> visited) {
        if (visited.contains(current)) {
            return false;
        }
        visited.add(current);

        List<String> refs = projectReferences.getOrDefault(current, Collections.emptyList());
        for (String ref : refs) {
            if (ref.equals(target) || hasTransitiveDependencyUtil(ref, target, visited)) {
                return true;
            }
        }
        return false;
    }
}