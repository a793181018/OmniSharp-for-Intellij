package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 包节点
 * 表示NuGet包依赖
 */
public class PackageNode implements DependencyNode {
    private final String packageId;
    private final String version;
    private final String displayName;
    
    public PackageNode(String packageId, String version) {
        this.packageId = packageId;
        this.version = version;
        this.displayName = packageId + ":" + version;
    }
    
    @Override
    public String getId() {
        return packageId + ":" + version;
    }
    
    @Override
    public String getName() {
        return displayName;
    }
    
    @Override
    public DependencyType getType() {
        return DependencyType.PACKAGE;
    }
    
    /**
     * 获取包ID
     */
    public String getPackageId() {
        return packageId;
    }
    
    /**
     * 获取包版本
     */
    public String getVersion() {
        return version;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PackageNode that = (PackageNode) o;
        return packageId.equals(that.packageId) && version.equals(that.version);
    }
    
    @Override
    public int hashCode() {
        int result = packageId.hashCode();
        result = 31 * result + version.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "PackageNode{id='" + packageId + "', version='" + version + "'}";
    }
}