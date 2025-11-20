package com.omnisharp.intellij.projectstructure.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 表示一个C#项目，包含项目配置、引用和文件信息
 */
public class ProjectModel {
    private final String id;
    private final String name;
    private final String path;
    private final String directory;
    private final String outputPath;
    private final String assemblyName;
    private final String targetFramework;
    private final Map<String, ProjectConfiguration> configurations;
    private final List<String> projectReferences;
    private final List<PackageReference> packageReferences;
    private final List<FileReference> fileReferences;
    private final List<String> projectFiles;
    private final ProjectLanguage language;

    public ProjectModel(
            String id,
            String name,
            String path,
            String directory,
            String outputPath,
            String assemblyName,
            String targetFramework,
            Map<String, ProjectConfiguration> configurations,
            List<String> projectReferences,
            List<PackageReference> packageReferences,
            List<FileReference> fileReferences,
            List<String> projectFiles,
            ProjectLanguage language) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.directory = directory;
        this.outputPath = outputPath;
        this.assemblyName = assemblyName;
        this.targetFramework = targetFramework;
        this.configurations = configurations != null ? configurations : Collections.emptyMap();
        this.projectReferences = projectReferences != null ? new ArrayList<>(projectReferences) : new ArrayList<>();
        this.packageReferences = packageReferences != null ? new ArrayList<>(packageReferences) : new ArrayList<>();
        this.fileReferences = fileReferences != null ? new ArrayList<>(fileReferences) : new ArrayList<>();
        this.projectFiles = projectFiles != null ? new ArrayList<>(projectFiles) : new ArrayList<>();
        this.language = language != null ? language : ProjectLanguage.CSHARP;
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

    public String getDirectory() {
        return directory;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public String getAssemblyName() {
        return assemblyName;
    }

    public String getTargetFramework() {
        return targetFramework;
    }

    public Map<String, ProjectConfiguration> getConfigurations() {
        return configurations;
    }

    public List<String> getProjectReferences() {
        return projectReferences;
    }

    public List<PackageReference> getPackageReferences() {
        return packageReferences;
    }

    public List<FileReference> getFileReferences() {
        return fileReferences;
    }

    public List<String> getProjectFiles() {
        return projectFiles;
    }

    

    /**
     * 添加包引用
     * @param packageReference 包引用对象
     */
    public void addPackageReference(PackageReference packageReference) {
        if (packageReference != null) {
            this.packageReferences.add(packageReference);
        }
    }

    /**
     * 添加文件引用
     * @param fileReference 文件引用对象
     */
    public void addFileReference(FileReference fileReference) {
        if (fileReference != null) {
            this.fileReferences.add(fileReference);
        }
    }

    /**
     * 添加编译文件
     * @param compileFile 编译文件路径
     */
    public void addCompileFile(String compileFile) {
        if (compileFile != null && !compileFile.isEmpty()) {
            this.projectFiles.add(compileFile);
        }
    }

    /**
     * 添加项目引用
     * @param projectReference 项目引用名称
     */
    public void addProjectReference(String projectReference) {
        if (projectReference != null && !projectReference.isEmpty()) {
            this.projectReferences.add(projectReference);
        }
    }

    /**
     * 设置项目属性
     * @param key 属性键
     * @param value 属性值
     */
    public void setProperty(String key, String value) {
        // 由于configurations是Map类型，我们需要添加相应的逻辑
        // 这里简化实现，实际可能需要更复杂的处理
    }

   

    public ProjectConfiguration getConfiguration(String name) {
        return configurations.get(name);
    }

    @Override
    public String toString() {
        return name + " (" + language + ")";
    }
}