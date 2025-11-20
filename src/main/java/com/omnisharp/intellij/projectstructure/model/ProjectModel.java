package com.omnisharp.intellij.projectstructure.model;

import java.util.*;

/**
 * 表示一个C#项目的模型
 */
public class ProjectModel {
    private final String id;
    private final String name;
    private final String path;
    private final String typeGuid;
    private final Map<String, String> properties;
    private final List<ProjectReference> projectReferences;
    private final List<PackageReference> packageReferences;
    private final List<FileReference> fileReferences;
    private final List<String> compileFiles;
    private final Map<String, ProjectConfiguration> configurations;
    private ProjectLanguage language = ProjectLanguage.CSHARP;
    private String directory;
    private String assemblyName;

    /**
     * 构造函数，接收四个必需参数
     * @param id 项目唯一标识符
     * @param name 项目名称
     * @param path 项目路径
     * @param typeGuid 项目类型GUID
     */
    public ProjectModel(String id, String name, String path, String typeGuid) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.typeGuid = typeGuid;
        this.properties = new HashMap<>();
        this.projectReferences = new ArrayList<>();
        this.packageReferences = new ArrayList<>();
        this.fileReferences = new ArrayList<>();
        this.compileFiles = new ArrayList<>();
        this.configurations = new HashMap<>();
        this.language = ProjectLanguage.CSHARP; // 默认语言为C#
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getTypeGuid() {
        return typeGuid;
    }

    /**
     * 设置项目属性
     * @param key 属性键
     * @param value 属性值
     */
    public void setProperty(String key, String value) {
        properties.put(key, value);
    }

    /**
     * 获取项目属性映射
     * @return 属性映射
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * 添加项目引用
     * @param reference 项目引用
     */
    public void addProjectReference(ProjectReference reference) {
        projectReferences.add(reference);
    }

    /**
     * 获取项目引用列表
     * @return 项目引用列表
     */
    public List<ProjectReference> getProjectReferences() {
        return projectReferences;
    }

    /**
     * 获取项目依赖的ID列表
     * @return 项目依赖ID列表
     */
    public List<String> getProjectDependencies() {
        List<String> dependencies = new ArrayList<>();
        for (ProjectReference ref : projectReferences) {
            dependencies.add(ref.getProjectId());
        }
        return dependencies;
    }

    /**
     * 添加NuGet包引用
     * @param reference 包引用
     */
    public void addPackageReference(PackageReference reference) {
        packageReferences.add(reference);
    }

    /**
     * 获取NuGet包引用列表
     * @return 包引用列表
     */
    public List<PackageReference> getPackageReferences() {
        return packageReferences;
    }

    /**
     * 添加文件引用
     * @param reference 文件引用
     */
    public void addFileReference(FileReference reference) {
        fileReferences.add(reference);
    }

    /**
     * 获取文件引用列表
     * @return 文件引用列表
     */
    public List<FileReference> getFileReferences() {
        return fileReferences;
    }

    /**
     * 添加编译文件
     * @param filePath 文件路径
     */
    public void addCompileFile(String filePath) {
        compileFiles.add(filePath);
    }

    /**
     * 获取编译文件列表
     * @return 编译文件列表
     */
    public List<String> getCompileFiles() {
        return compileFiles;
    }

    /**
     * 添加项目配置
     * @param configurationName 配置名称
     * @param configuration 项目配置
     */
    public void addConfiguration(String configurationName, ProjectConfiguration configuration) {
        configurations.put(configurationName, configuration);
    }

    /**
     * 获取项目配置映射
     * @return 配置映射
     */
    public Map<String, ProjectConfiguration> getConfigurations() {
        return configurations;
    }

    /**
     * 添加项目依赖ID
     * @param projectId 项目ID
     */
    public void addProjectDependency(String projectId) {
        this.projectReferences.add(new ProjectReference(projectId));
    }

    /**
     * 设置项目配置映射
     * @param configurations 配置映射
     */
    public void setConfigurations(Map<String, ProjectConfiguration> configurations) {
        this.configurations.clear();
        if (configurations != null) {
            this.configurations.putAll(configurations);
        }
    }
    
    public void setLanguage(ProjectLanguage language) {
        this.language = language;
    }
    
    public ProjectLanguage getLanguage() {
        return language;
    }
    
    public void setDirectory(String directory) {
        this.directory = directory;
    }
    
    public void setAssemblyName(String assemblyName) {
        this.assemblyName = assemblyName;
    }
}