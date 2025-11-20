package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 包管理器接口
 * 负责解析和管理NuGet包依赖
 */
public interface PackageManager {
    /**
     * 初始化包管理器
     */
    void initialize();
    
    /**
     * 解析项目的包依赖
     */
    List<PackageDependency> resolveProjectPackageDependencies(Path projectPath);
    
    /**
     * 异步解析项目的包依赖
     */
    CompletableFuture<List<PackageDependency>> resolveProjectPackageDependenciesAsync(Path projectPath);
    
    /**
     * 解析单个包引用及其传递依赖
     */
    List<PackageDependency> resolvePackageDependencies(String packageId, String version, String projectPath, String projectName);
    
    /**
     * 获取NuGet缓存路径
     */
    Path getNuGetCachePath();
    
    /**
     * 获取包的详细信息
     */
    PackageInfo getPackageInfo(String packageId, String version);
    
    /**
     * 清理缓存
     */
    void clearCache();
    
    /**
     * 包信息类
     */
    class PackageInfo {
        private final String packageId;
        private final String version;
        private final String title;
        private final String authors;
        private final String description;
        private final List<PackageDependencyInfo> dependencies;
        
        public PackageInfo(String packageId, String version, String title, String authors, 
                          String description, List<PackageDependencyInfo> dependencies) {
            this.packageId = packageId;
            this.version = version;
            this.title = title;
            this.authors = authors;
            this.description = description;
            this.dependencies = dependencies;
        }
        
        public String getPackageId() {
            return packageId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getAuthors() {
            return authors;
        }
        
        public String getDescription() {
            return description;
        }
        
        public List<PackageDependencyInfo> getDependencies() {
            return dependencies;
        }
    }
    
    /**
     * 包依赖信息类
     */
    class PackageDependencyInfo {
        private final String packageId;
        private final String versionRange;
        
        public PackageDependencyInfo(String packageId, String versionRange) {
            this.packageId = packageId;
            this.versionRange = versionRange;
        }
        
        public String getPackageId() {
            return packageId;
        }
        
        public String getVersionRange() {
            return versionRange;
        }
    }
}