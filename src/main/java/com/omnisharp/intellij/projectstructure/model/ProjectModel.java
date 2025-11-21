package com.omnisharp.intellij.projectstructure.model;

import java.io.File;
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
    private String outputPath = "bin/Debug/";
    private String targetFramework = "v4.7.2";
    private String outputType = "Library"; // 输出类型: Exe, Library, WinExe
    private String rootNamespace;
    private String defaultNamespace;
    private boolean isSdkProject = false; // 是否为SDK风格项目
    private long projectFileTimestamp = 0; // 项目文件时间戳
    private int projectFileVersion = 0; // 项目文件版本号，用于跟踪变更
    private List<String> projectTypeGuids = new ArrayList<>(); // 项目类型GUID列表
    private String msbuildToolsVersion = "15.0"; // MSBuild工具版本
    private List<String> defines = new ArrayList<>(); // 条件编译符号
    private String nullableContextOptions = "disable"; // 可空引用类型选项
    private String implicitUsings = "disable"; // 隐式using指令选项
    private List<String> targetFrameworks = new ArrayList<>(); // 多目标框架

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
    
    public String getOutputPath() {
        return outputPath;
    }
    
    public void setOutputPath(String outputPath) {
        this.outputPath = outputPath;
    }
    
    public String getTargetFramework() {
        return targetFramework;
    }
    
    public void setTargetFramework(String targetFramework) {
        this.targetFramework = targetFramework;
    }
    
    public String getOutputType() {
        return outputType;
    }
    
    public void setOutputType(String outputType) {
        this.outputType = outputType;
    }
    
    public String getRootNamespace() {
        return rootNamespace != null ? rootNamespace : name;
    }
    
    public void setRootNamespace(String rootNamespace) {
        this.rootNamespace = rootNamespace;
    }
    
    public String getDefaultNamespace() {
        return defaultNamespace != null ? defaultNamespace : name;
    }
    
    public void setDefaultNamespace(String defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
    }
    
    public boolean isSdkProject() {
        return isSdkProject;
    }
    
    public void setSdkProject(boolean sdkProject) {
        isSdkProject = sdkProject;
    }
    
    public long getProjectFileTimestamp() {
        return projectFileTimestamp;
    }
    
    public void setProjectFileTimestamp(long projectFileTimestamp) {
        this.projectFileTimestamp = projectFileTimestamp;
    }
    
    public int getProjectFileVersion() {
        return projectFileVersion;
    }
    
    public void setProjectFileVersion(int projectFileVersion) {
        this.projectFileVersion = projectFileVersion;
    }
    
    public List<String> getProjectTypeGuids() {
        return projectTypeGuids;
    }
    
    public void setProjectTypeGuids(List<String> projectTypeGuids) {
        this.projectTypeGuids = projectTypeGuids;
    }
    
    public void addProjectTypeGuid(String guid) {
        this.projectTypeGuids.add(guid);
    }
    
    public String getMsbuildToolsVersion() {
        return msbuildToolsVersion;
    }
    
    public void setMsbuildToolsVersion(String msbuildToolsVersion) {
        this.msbuildToolsVersion = msbuildToolsVersion;
    }
    
    public List<String> getDefines() {
        return defines;
    }
    
    public void setDefines(List<String> defines) {
        this.defines = defines;
    }
    
    public void addDefine(String define) {
        this.defines.add(define);
    }
    
    public String getNullableContextOptions() {
        return nullableContextOptions;
    }
    
    public void setNullableContextOptions(String nullableContextOptions) {
        this.nullableContextOptions = nullableContextOptions;
    }
    
    public String getImplicitUsings() {
        return implicitUsings;
    }
    
    public void setImplicitUsings(String implicitUsings) {
        this.implicitUsings = implicitUsings;
    }
    
    public List<String> getTargetFrameworks() {
        return targetFrameworks;
    }
    
    public void setTargetFrameworks(List<String> targetFrameworks) {
        this.targetFrameworks = targetFrameworks;
    }
    
    /**
     * 获取项目的默认输出目录
     */
    public String getDefaultOutputDirectory(String configuration, String platform) {
        if (configuration == null) {
            configuration = "Debug";
        }
        if (platform == null) {
            platform = "AnyCPU";
        }
        
        String configPath = outputPath;
        if (outputPath.contains("$(Configuration)") || outputPath.contains("$(Platform)")) {
            configPath = outputPath
                .replace("$(Configuration)", configuration)
                .replace("$(Platform)", platform);
        }
        
        return directory + File.separator + configPath;
    }
    
    /**
     * 获取项目的默认输出目录（使用默认配置）
     */
    public String getDefaultOutputDirectory() {
        return getDefaultOutputDirectory("Debug", "AnyCPU");
    }
    
    /**
     * 获取项目的输出文件路径
     */
    public String getOutputFilePath(String configuration, String platform) {
        String extension = ".dll";
        if ("Exe".equals(outputType) || "WinExe".equals(outputType)) {
            extension = ".exe";
        }
        
        String outputDir = getDefaultOutputDirectory(configuration, platform);
        return outputDir + File.separator + assemblyName + extension;
    }
    
    /**
     * 获取项目的输出文件路径（使用默认配置）
     */
    public String getOutputFilePath() {
        return getOutputFilePath("Debug", "AnyCPU");
    }
    
    /**
     * 获取项目配置
     */
    public ProjectConfiguration getConfiguration(String configurationName) {
        return configurations.get(configurationName);
    }
    
    /**
     * 检查是否是C#项目
     */
    public boolean isCSharpProject() {
        return language == ProjectLanguage.CSHARP;
    }
    
    /**
     * 检查是否是F#项目
     */
    public boolean isFSharpProject() {
        return language == ProjectLanguage.FSHARP;
    }
    
    /**
     * 检查是否是VB项目
     */
    public boolean isVisualBasicProject() {
        return language == ProjectLanguage.VISUAL_BASIC;
    }
    
    /**
     * 检查是否是可执行项目
     */
    public boolean isExecutableProject() {
        return "Exe".equals(outputType) || "WinExe".equals(outputType);
    }
    
    /**
     * 检查是否是库项目
     */
    public boolean isLibraryProject() {
        return "Library".equals(outputType);
    }
    
    /**
     * 检查是否是多目标框架项目
     */
    public boolean isMultiTargetProject() {
        return !targetFrameworks.isEmpty();
    }
    
    /**
     * 获取所有源文件
     */
    public List<String> getSourceFiles() {
        List<String> sourceFiles = new ArrayList<>();
        for (String filePath : compileFiles) {
            if (isSourceFile(filePath)) {
                sourceFiles.add(filePath);
            }
        }
        return sourceFiles;
    }
    
    /**
     * 获取所有资源文件
     */
    public List<String> getResourceFiles() {
        List<String> resourceFiles = new ArrayList<>();
        for (String filePath : compileFiles) {
            if (isResourceFile(filePath)) {
                resourceFiles.add(filePath);
            }
        }
        return resourceFiles;
    }
    
    /**
     * 检查文件是否为源文件
     */
    private boolean isSourceFile(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        switch (language) {
            case CSHARP:
                return "cs".equals(extension);
            case FSHARP:
                return "fs".equals(extension);
            case VISUAL_BASIC:
                return "vb".equals(extension);
            default:
                return false;
        }
    }
    
    /**
     * 检查文件是否为资源文件
     */
    private boolean isResourceFile(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return extension.equals("resx") || extension.equals("resources") || 
               extension.equals("ico") || extension.equals("png") || 
               extension.equals("jpg") || extension.equals("jpeg") || 
               extension.equals("gif");
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filePath.substring(lastDotIndex + 1);
        }
        return "";
    }
    
    /**
     * 获取项目类型
     */
    public ProjectType getProjectType() {
        // 根据输出类型和项目文件类型判断
        if (isExecutableProject()) {
            return ProjectType.CONSOLE;
        }
        
        for (String guid : projectTypeGuids) {
            if ("{3AC096D0-A1C2-E12C-1390-A8335801FDAB}".equalsIgnoreCase(guid) ||
                "{349c5851-65df-11da-bb86-000d5675b067}".equalsIgnoreCase(guid)) {
                return ProjectType.WEB;
            }
        }
        
        for (PackageReference ref : packageReferences) {
            if (ref.getId().toLowerCase().contains("test")) {
                return ProjectType.TEST;
            }
        }
        
        return ProjectType.LIBRARY;
    }
    
    /**
     * 获取项目文件的相对路径
     */
    public String getRelativePath(String filePath) {
        try {
            return new File(filePath).toPath().relativize(new File(directory).toPath()).toString();
        } catch (IllegalArgumentException e) {
            return filePath; // 如果不在同一目录下，返回完整路径
        }
    }
    
    /**
     * 增加项目文件版本号
     */
    public void incrementVersion() {
        projectFileVersion++;
    }
    
    /**
     * 设置项目引用列表
     */
    public void setProjectReferences(List<ProjectReference> projectReferences) {
        this.projectReferences.clear();
        if (projectReferences != null) {
            this.projectReferences.addAll(projectReferences);
        }
    }
    
    /**
     * 设置包引用列表
     */
    public void setPackageReferences(List<PackageReference> packageReferences) {
        this.packageReferences.clear();
        if (packageReferences != null) {
            this.packageReferences.addAll(packageReferences);
        }
    }
    
    /**
     * 设置文件引用列表
     */
    public void setFileReferences(List<FileReference> fileReferences) {
        this.fileReferences.clear();
        if (fileReferences != null) {
            this.fileReferences.addAll(fileReferences);
        }
    }
    
    /**
     * 设置编译文件列表
     */
    public void setCompileFiles(List<String> compileFiles) {
        this.compileFiles.clear();
        if (compileFiles != null) {
            this.compileFiles.addAll(compileFiles);
        }
    }
    
    /**
     * 获取所有项目文件（与Kotlin版本保持一致）
     */
    public List<String> getProjectFiles() {
        return compileFiles;
    }
    
    /**
     * 设置所有项目文件（与Kotlin版本保持一致）
     */
    public void setProjectFiles(List<String> projectFiles) {
        setCompileFiles(projectFiles);
    }
    
    /**
     * 获取项目目录
     * @return 项目目录路径
     */
    public String getDirectory() {
        return directory;
    }

    @Override
    public String toString() {
        return "ProjectModel(id='" + id + "', name='" + name + "', path='" + path + "', language=" + language + ", outputType='" + outputType + "')";
    }
    
    /**
     * 获取项目的结构信息
     * @return 项目结构标识符
     */
    public String getStructure() {
        // 返回项目的基本结构信息
        return name + "-" + language + "-" + outputType;
    }
    
    /**
     * 设置程序集名称
     */
    public void setAssemblyName(String assemblyName) {
        this.assemblyName = assemblyName;
    }
    
    /**
     * 获取程序集名称
     * @return 程序集名称
     */
    public String getAssemblyName() {
        return assemblyName;
    }
    
    /**
     * 项目文件类型分类
     */
    public enum ProjectType {
        APPLICATION,
        LIBRARY,
        TEST,
        WEB,
        CONSOLE
    }
}