package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 项目解析器接口
 * 负责解析项目文件和项目间的引用关系
 */
public interface ProjectResolver {
    /**
     * 解析单个项目文件
     */
    ProjectInfo resolveProject(Path projectPath) throws ProjectResolutionException;
    
    /**
     * 解析项目的项目引用
     */
    List<ProjectDependency> resolveProjectReferences(Path projectPath);
    
    /**
     * 解析解决方案中的所有项目引用关系
     */
    Map<Path, List<ProjectDependency>> resolveSolutionProjectReferences(Path solutionPath);
    
    /**
     * 获取项目信息
     */
    ProjectInfo getProjectInfo(Path projectPath);
    
    /**
     * 项目信息类
     */
    class ProjectInfo {
        private final String name;
        private final Path projectPath;
        private final Path projectFilePath;
        private final List<Path> referencedProjects;
        private final List<PackageDependencyInfo> packageReferences;
        
        public ProjectInfo(String name, Path projectPath, Path projectFilePath, 
                         List<Path> referencedProjects, 
                         List<PackageDependencyInfo> packageReferences) {
            this.name = name;
            this.projectPath = projectPath;
            this.projectFilePath = projectFilePath;
            this.referencedProjects = referencedProjects;
            this.packageReferences = packageReferences;
        }
        
        public String getName() {
            return name;
        }
        
        public Path getProjectPath() {
            return projectPath;
        }
        
        public Path getProjectFilePath() {
            return projectFilePath;
        }
        
        public List<Path> getReferencedProjects() {
            return referencedProjects;
        }
        
        public List<PackageDependencyInfo> getPackageReferences() {
            return packageReferences;
        }
    }
    
    /**
     * 包依赖信息类
     */
    class PackageDependencyInfo {
        private final String packageId;
        private final String version;
        private final boolean isTransitive;
        
        public PackageDependencyInfo(String packageId, String version) {
            this(packageId, version, false);
        }
        
        public PackageDependencyInfo(String packageId, String version, boolean isTransitive) {
            this.packageId = packageId;
            this.version = version;
            this.isTransitive = isTransitive;
        }
        
        public String getPackageId() {
            return packageId;
        }
        
        public String getVersion() {
            return version;
        }
        
        public boolean isTransitive() {
            return isTransitive;
        }
    }
}