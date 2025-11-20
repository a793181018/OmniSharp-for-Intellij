package com.intellij.plugins.omnisharp.project.dependencies;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 项目解析器的模拟实现
 * 用于测试OmniSharpDependencyAnalyzer
 */
public class MockProjectResolver implements ProjectResolver {
    
    private final Map<Path, ProjectInfo> projectInfoMap = new HashMap<>();
    private final Map<Path, List<ProjectReferenceInfo>> projectReferencesMap = new HashMap<>();
    private final Map<Path, List<PackageDependencyInfo>> packageDependenciesMap = new HashMap<>();
    private final Map<Path, List<Path>> solutionProjectsMap = new HashMap<>();
    
    /**
     * 添加模拟的项目信息
     */
    public void addProjectInfo(Path projectPath, ProjectInfo projectInfo) {
        projectInfoMap.put(projectPath, projectInfo);
    }
    
    /**
     * 添加模拟的项目引用
     */
    public void addProjectReferences(Path projectPath, List<ProjectReferenceInfo> references) {
        projectReferencesMap.put(projectPath, references);
    }
    
    /**
     * 添加模拟的包依赖
     */
    public void addPackageDependencies(Path projectPath, List<PackageDependencyInfo> dependencies) {
        packageDependenciesMap.put(projectPath, dependencies);
    }
    
    /**
     * 添加模拟的解决方案项目
     */
    public void addSolutionProjects(Path solutionPath, List<Path> projectPaths) {
        solutionProjectsMap.put(solutionPath, projectPaths);
    }
    
    @Override
    public ProjectInfo resolveProject(Path projectPath) throws ProjectResolutionException {
        if (!projectInfoMap.containsKey(projectPath)) {
            throw new ProjectResolutionException(projectPath, "模拟错误: 找不到项目信息");
        }
        return projectInfoMap.get(projectPath);
    }
    
    @Override
    public List<ProjectReferenceInfo> resolveProjectReferences(Path projectPath) throws ProjectResolutionException {
        if (!projectReferencesMap.containsKey(projectPath)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(projectReferencesMap.get(projectPath));
    }
    
    @Override
    public List<PackageDependencyInfo> resolvePackageDependencies(Path projectPath) throws ProjectResolutionException {
        if (!packageDependenciesMap.containsKey(projectPath)) {
            return Collections.emptyList();
        }
        return new ArrayList<>(packageDependenciesMap.get(projectPath));
    }
    
    @Override
    public List<Path> resolveSolutionProjects(Path solutionPath) throws ProjectResolutionException {
        if (!solutionProjectsMap.containsKey(solutionPath)) {
            throw new ProjectResolutionException(solutionPath, "模拟错误: 找不到解决方案信息");
        }
        return new ArrayList<>(solutionProjectsMap.get(solutionPath));
    }
    
    /**
     * 创建测试用的模拟数据
     */
    public static MockProjectResolver createWithTestData() {
        MockProjectResolver resolver = new MockProjectResolver();
        
        // 创建项目路径
        Path projectAPath = Paths.get("C:\\Projects\\TestSolution\\ProjectA");
        Path projectBPath = Paths.get("C:\\Projects\\TestSolution\\ProjectB");
        Path projectCPath = Paths.get("C:\\Projects\\TestSolution\\ProjectC");
        Path solutionPath = Paths.get("C:\\Projects\\TestSolution\\TestSolution.sln");
        
        // 添加项目信息
        resolver.addProjectInfo(projectAPath, new ProjectInfo(
                projectAPath, "ProjectA", "net7.0", "C:\\Projects\\TestSolution\\ProjectA\\ProjectA.csproj"
        ));
        resolver.addProjectInfo(projectBPath, new ProjectInfo(
                projectBPath, "ProjectB", "net7.0", "C:\\Projects\\TestSolution\\ProjectB\\ProjectB.csproj"
        ));
        resolver.addProjectInfo(projectCPath, new ProjectInfo(
                projectCPath, "ProjectC", "net7.0", "C:\\Projects\\TestSolution\\ProjectC\\ProjectC.csproj"
        ));
        
        // 添加项目引用
        List<ProjectReferenceInfo> projectARefs = new ArrayList<>();
        projectARefs.add(new ProjectReferenceInfo(projectBPath, "ProjectB"));
        resolver.addProjectReferences(projectAPath, projectARefs);
        
        List<ProjectReferenceInfo> projectBRefs = new ArrayList<>();
        projectBRefs.add(new ProjectReferenceInfo(projectCPath, "ProjectC"));
        resolver.addProjectReferences(projectBPath, projectBRefs);
        
        // 添加包依赖
        List<PackageDependencyInfo> projectADeps = new ArrayList<>();
        projectADeps.add(new PackageDependencyInfo("Newtonsoft.Json", "13.0.3", false));
        projectADeps.add(new PackageDependencyInfo("Microsoft.Extensions.Logging", "7.0.0", false));
        resolver.addPackageDependencies(projectAPath, projectADeps);
        
        List<PackageDependencyInfo> projectBDeps = new ArrayList<>();
        projectBDeps.add(new PackageDependencyInfo("Microsoft.Extensions.DependencyInjection", "7.0.0", false));
        projectBDeps.add(new PackageDependencyInfo("Newtonsoft.Json", "12.0.3", false)); // 版本冲突
        resolver.addPackageDependencies(projectBPath, projectBDeps);
        
        List<PackageDependencyInfo> projectCDeps = new ArrayList<>();
        projectCDeps.add(new PackageDependencyInfo("EntityFrameworkCore", "7.0.5", false));
        resolver.addPackageDependencies(projectCPath, projectCDeps);
        
        // 添加解决方案项目
        List<Path> solutionProjects = new ArrayList<>();
        solutionProjects.add(projectAPath);
        solutionProjects.add(projectBPath);
        solutionProjects.add(projectCPath);
        resolver.addSolutionProjects(solutionPath, solutionProjects);
        
        return resolver;
    }
}