package com.intellij.plugins.omnisharp.project.dependencies;

/**
 * 包依赖
 * 表示一个项目对NuGet包的依赖关系
 */
public class PackageDependency {
    private final String projectPath;
    private final String projectName;
    private final String packageId;
    private final String version;
    private final boolean isTransitive;
    private final String resolvedVersion;
    
    public PackageDependency(String projectPath, String projectName, String packageId, String version) {
        this(projectPath, projectName, packageId, version, false, version);
    }
    
    public PackageDependency(String projectPath, String projectName, String packageId, 
                           String version, boolean isTransitive, String resolvedVersion) {
        this.projectPath = projectPath;
        this.projectName = projectName;
        this.packageId = packageId;
        this.version = version;
        this.isTransitive = isTransitive;
        this.resolvedVersion = resolvedVersion;
    }
    
    /**
     * 获取项目路径
     */
    public String getProjectPath() {
        return projectPath;
    }
    
    /**
     * 获取项目名称
     */
    public String getProjectName() {
        return projectName;
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
    
    /**
     * 是否是传递依赖
     */
    public boolean isTransitive() {
        return isTransitive;
    }
    
    /**
     * 获取解析后的版本
     */
    public String getResolvedVersion() {
        return resolvedVersion;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        PackageDependency that = (PackageDependency) o;
        return projectPath.equals(that.projectPath) && 
               packageId.equals(that.packageId) && 
               resolvedVersion.equals(that.resolvedVersion);
    }
    
    @Override
    public int hashCode() {
        int result = projectPath.hashCode();
        result = 31 * result + packageId.hashCode();
        result = 31 * result + resolvedVersion.hashCode();
        return result;
    }
    
    @Override
    public String toString() {
        return "PackageDependency{" + projectName + " -> " + packageId + ":" + resolvedVersion + 
               (isTransitive ? " (transitive)" : "") + "}";
    }
}