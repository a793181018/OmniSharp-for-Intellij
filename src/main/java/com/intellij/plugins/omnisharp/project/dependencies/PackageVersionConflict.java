package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.List;
import java.util.Set;

/**
 * 包版本冲突
 * 表示同一包的不同版本之间的冲突
 */
public class PackageVersionConflict {
    private final String packageId;
    private final Set<String> conflictingVersions;
    private final List<PackageDependency> dependencies;
    
    public PackageVersionConflict(String packageId, Set<String> conflictingVersions, 
                                List<PackageDependency> dependencies) {
        this.packageId = packageId;
        this.conflictingVersions = conflictingVersions;
        this.dependencies = dependencies;
    }
    
    /**
     * 获取包ID
     */
    public String getPackageId() {
        return packageId;
    }
    
    /**
     * 获取冲突的版本集合
     */
    public Set<String> getConflictingVersions() {
        return conflictingVersions;
    }
    
    /**
     * 获取相关的依赖列表
     */
    public List<PackageDependency> getDependencies() {
        return dependencies;
    }
    
    /**
     * 获取冲突的版本数量
     */
    public int getConflictCount() {
        return conflictingVersions.size();
    }
    
    @Override
    public String toString() {
        return "PackageVersionConflict{" + packageId + ": " + 
               String.join(", ", conflictingVersions) + "}";
    }
}