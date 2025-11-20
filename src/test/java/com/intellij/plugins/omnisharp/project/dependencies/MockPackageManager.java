package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 包管理器的模拟实现
 * 用于测试OmniSharpDependencyAnalyzer
 */
public class MockPackageManager implements PackageManager {
    
    private final Map<String, PackageInfo> packageInfoMap = new HashMap<>();
    private final Map<String, List<PackageDependencyInfo>> packageDependenciesMap = new HashMap<>();
    private final Path nugetCachePath = Paths.get("C:\\.nuget\\packages");
    
    /**
     * 添加模拟的包信息
     */
    public void addPackageInfo(PackageInfo packageInfo) {
        String key = packageInfo.getPackageId() + ":" + packageInfo.getVersion();
        packageInfoMap.put(key, packageInfo);
    }
    
    /**
     * 添加模拟的包依赖
     */
    public void addPackageDependencies(String packageId, String version, List<PackageDependencyInfo> dependencies) {
        String key = packageId + ":" + version;
        packageDependenciesMap.put(key, dependencies);
    }
    
    @Override
    public void initialize() {
        // 模拟初始化，不需要实际操作
    }
    
    @Override
    public List<PackageDependencyInfo> resolvePackageDependencies(Path projectPath) throws ProjectResolutionException {
        // 这个方法在MockProjectResolver中已经实现，这里简单返回空列表
        return Collections.emptyList();
    }
    
    @Override
    public CompletableFuture<List<PackageDependencyInfo>> resolvePackageDependenciesAsync(Path projectPath) {
        // 异步版本返回空列表
        return CompletableFuture.completedFuture(Collections.emptyList());
    }
    
    @Override
    public PackageInfo getPackageInfo(String packageId, String version) {
        String key = packageId + ":" + version;
        return packageInfoMap.getOrDefault(key, null);
    }
    
    @Override
    public List<PackageDependencyInfo> getPackageDependencies(String packageId, String version) {
        String key = packageId + ":" + version;
        return packageDependenciesMap.getOrDefault(key, Collections.emptyList());
    }
    
    @Override
    public Path getNugetCachePath() {
        return nugetCachePath;
    }
    
    @Override
    public boolean isPackageInstalled(String packageId, String version) {
        String key = packageId + ":" + version;
        return packageInfoMap.containsKey(key);
    }
    
    /**
     * 创建测试用的模拟数据
     */
    public static MockPackageManager createWithTestData() {
        MockPackageManager manager = new MockPackageManager();
        
        // 添加包信息
        manager.addPackageInfo(new PackageInfo("Newtonsoft.Json", "13.0.3", "JSON处理库", "MIT"));
        manager.addPackageInfo(new PackageInfo("Newtonsoft.Json", "12.0.3", "JSON处理库", "MIT"));
        manager.addPackageInfo(new PackageInfo("Microsoft.Extensions.Logging", "7.0.0", "日志框架", "MIT"));
        manager.addPackageInfo(new PackageInfo("Microsoft.Extensions.DependencyInjection", "7.0.0", "依赖注入", "MIT"));
        manager.addPackageInfo(new PackageInfo("EntityFrameworkCore", "7.0.5", "ORM框架", "MIT"));
        
        // 添加包的依赖关系
        List<PackageDependencyInfo> jsonDeps = new ArrayList<>();
        manager.addPackageDependencies("Newtonsoft.Json", "13.0.3", jsonDeps);
        manager.addPackageDependencies("Newtonsoft.Json", "12.0.3", jsonDeps);
        
        List<PackageDependencyInfo> loggingDeps = new ArrayList<>();
        loggingDeps.add(new PackageDependencyInfo("Microsoft.Extensions.DependencyInjection", "7.0.0", false));
        manager.addPackageDependencies("Microsoft.Extensions.Logging", "7.0.0", loggingDeps);
        
        List<PackageDependencyInfo> diDeps = new ArrayList<>();
        manager.addPackageDependencies("Microsoft.Extensions.DependencyInjection", "7.0.0", diDeps);
        
        List<PackageDependencyInfo> efDeps = new ArrayList<>();
        efDeps.add(new PackageDependencyInfo("Microsoft.Extensions.DependencyInjection", "7.0.0", false));
        efDeps.add(new PackageDependencyInfo("Newtonsoft.Json", "13.0.1", false));
        manager.addPackageDependencies("EntityFrameworkCore", "7.0.5", efDeps);
        
        return manager;
    }
}