package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * SDK风格项目文件解析器
 * 处理.NET Core、.NET 5+的新格式项目文件
 */
public class SdkStyleProjectParser implements ProjectParser {
    private static final String DEFAULT_TARGET_FRAMEWORK = "net5.0";
    private static final String DEFAULT_OUTPUT_PATH = "bin/Debug/";
    private static final String DEFAULT_OUTPUT_TYPE = "Library";
    
    @Override
    public ProjectModel parse(Path projectPath) throws IOException, SolutionParser.ParseException {
        File projectFile = projectPath.toFile();
        String projectId = UUID.randomUUID().toString();
        String projectName = getProjectName(projectFile);
        String projectDirectory = projectFile.getParent();
        
        ProjectModel projectModel = new ProjectModel(projectId, projectName, projectPath.toString(), null);
        projectModel.setDirectory(projectDirectory);
        projectModel.setAssemblyName(projectName);
        projectModel.setSdkProject(true);
        projectModel.setProjectFileTimestamp(projectFile.lastModified());
        
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile);
            document.getDocumentElement().normalize();
            
            // 解析基本属性
            parseProjectProperties(document, projectModel);
            
            // 解析TargetFramework或TargetFrameworks
            parseTargetFrameworks(document, projectModel);
            
            // 解析项目引用
            parseProjectReferences(document, projectModel, projectDirectory);
            
            // 解析包引用
            parsePackageReferences(document, projectModel);
            
            // 解析文件引用
            parseFileReferences(document, projectModel);
            
            // 解析编译文件和资源文件
            parseCompileFiles(document, projectModel, projectDirectory);
            
            // 解析默认包含的文件（SDK风格项目会自动包含）
            parseDefaultIncludes(projectModel, projectDirectory);
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SolutionParser.ParseException("Failed to parse project file: " + e.getMessage(), e);
        }
        
        return projectModel;
    }
    
    private void parseProjectProperties(Document document, ProjectModel projectModel) {
        NodeList propertyGroups = document.getElementsByTagName("PropertyGroup");
        
        for (int i = 0; i < propertyGroups.getLength(); i++) {
            Element propertyGroup = (Element) propertyGroups.item(i);
            
            // 检查是否有条件
            String condition = propertyGroup.getAttribute("Condition");
            if (!condition.isEmpty() && !evaluateCondition(condition, projectModel)) {
                continue;
            }
            
            // 解析AssemblyName
            Node assemblyNameNode = propertyGroup.getElementsByTagName("AssemblyName").item(0);
            if (assemblyNameNode != null && assemblyNameNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setAssemblyName(assemblyNameNode.getTextContent().trim());
            }
            
            // 解析RootNamespace
            Node rootNamespaceNode = propertyGroup.getElementsByTagName("RootNamespace").item(0);
            if (rootNamespaceNode != null && rootNamespaceNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setRootNamespace(rootNamespaceNode.getTextContent().trim());
            }
            
            // 解析OutputPath
            Node outputPathNode = propertyGroup.getElementsByTagName("OutputPath").item(0);
            if (outputPathNode != null && outputPathNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setOutputPath(outputPathNode.getTextContent().trim());
            }
            
            // 解析OutputType
            Node outputTypeNode = propertyGroup.getElementsByTagName("OutputType").item(0);
            if (outputTypeNode != null && outputTypeNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setOutputType(outputTypeNode.getTextContent().trim());
            }
            
            // 解析Nullable
            Node nullableNode = propertyGroup.getElementsByTagName("Nullable").item(0);
            if (nullableNode != null && nullableNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setNullableContextOptions(nullableNode.getTextContent().trim());
            }
            
            // 解析ImplicitUsings
            Node implicitUsingsNode = propertyGroup.getElementsByTagName("ImplicitUsings").item(0);
            if (implicitUsingsNode != null && implicitUsingsNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setImplicitUsings(implicitUsingsNode.getTextContent().trim());
            }
            
            // 解析DefineConstants
            Node defineConstantsNode = propertyGroup.getElementsByTagName("DefineConstants").item(0);
            if (defineConstantsNode != null && defineConstantsNode.getNodeType() == Node.ELEMENT_NODE) {
                String constants = defineConstantsNode.getTextContent().trim();
                if (!constants.isEmpty()) {
                    for (String constant : constants.split(";")) {
                        projectModel.addDefine(constant.trim());
                    }
                }
            }
            
            // 解析其他属性到properties map
            NamedNodeMap attributes = propertyGroup.getAttributes();
            NodeList children = propertyGroup.getChildNodes();
            
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String name = child.getNodeName();
                    String value = child.getTextContent().trim();
                    if (!name.equals("AssemblyName") && !name.equals("RootNamespace") &&
                        !name.equals("OutputPath") && !name.equals("OutputType") &&
                        !name.equals("Nullable") && !name.equals("ImplicitUsings") &&
                        !name.equals("DefineConstants")) {
                        projectModel.setProperty(name, value);
                    }
                }
            }
        }
    }
    
    private void parseTargetFrameworks(Document document, ProjectModel projectModel) {
        // 首先尝试解析TargetFrameworks（多目标）
        Node targetFrameworksNode = document.getElementsByTagName("TargetFrameworks").item(0);
        if (targetFrameworksNode != null && targetFrameworksNode.getNodeType() == Node.ELEMENT_NODE) {
            String frameworks = targetFrameworksNode.getTextContent().trim();
            if (!frameworks.isEmpty()) {
                List<String> targetFrameworkList = Arrays.stream(frameworks.split(";"))
                        .map(String::trim)
                        .collect(Collectors.toList());
                projectModel.setTargetFrameworks(targetFrameworkList);
                // 设置主框架
                if (!targetFrameworkList.isEmpty()) {
                    projectModel.setTargetFramework(targetFrameworkList.get(0));
                }
                return;
            }
        }
        
        // 如果没有多目标，解析单个TargetFramework
        Node targetFrameworkNode = document.getElementsByTagName("TargetFramework").item(0);
        if (targetFrameworkNode != null && targetFrameworkNode.getNodeType() == Node.ELEMENT_NODE) {
            projectModel.setTargetFramework(targetFrameworkNode.getTextContent().trim());
        } else {
            projectModel.setTargetFramework(DEFAULT_TARGET_FRAMEWORK);
        }
    }
    
    private void parseProjectReferences(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList projectReferenceNodes = document.getElementsByTagName("ProjectReference");
        
        for (int i = 0; i < projectReferenceNodes.getLength(); i++) {
            Element referenceElement = (Element) projectReferenceNodes.item(i);
            String includePath = referenceElement.getAttribute("Include");
            
            if (!includePath.isEmpty()) {
                // 解析项目路径
                String projectPath = resolvePath(includePath, projectDirectory);
                File referencedProjectFile = new File(projectPath);
                
                if (referencedProjectFile.exists()) {
                    String projectName = getProjectName(referencedProjectFile);
                    ProjectReference reference = new ProjectReference(UUID.randomUUID().toString(), projectName, projectPath);
                    projectModel.addProjectReference(reference);
                }
            }
        }
    }
    
    private void parsePackageReferences(Document document, ProjectModel projectModel) {
        NodeList packageReferenceNodes = document.getElementsByTagName("PackageReference");
        
        for (int i = 0; i < packageReferenceNodes.getLength(); i++) {
            Element referenceElement = (Element) packageReferenceNodes.item(i);
            String include = referenceElement.getAttribute("Include");
            String version = referenceElement.getAttribute("Version");
            
            if (!include.isEmpty() && !version.isEmpty()) {
                // 解析IncludeAssets和ExcludeAssets
                List<String> includeAssets = new ArrayList<>();
                List<String> excludeAssets = new ArrayList<>();
                
                Node includeAssetsNode = referenceElement.getElementsByTagName("IncludeAssets").item(0);
                if (includeAssetsNode != null && includeAssetsNode.getNodeType() == Node.ELEMENT_NODE) {
                    String assets = includeAssetsNode.getTextContent().trim();
                    if (!assets.isEmpty()) {
                        includeAssets = Arrays.stream(assets.split(";"))
                                .map(String::trim)
                                .collect(Collectors.toList());
                    }
                }
                
                Node excludeAssetsNode = referenceElement.getElementsByTagName("ExcludeAssets").item(0);
                if (excludeAssetsNode != null && excludeAssetsNode.getNodeType() == Node.ELEMENT_NODE) {
                    String assets = excludeAssetsNode.getTextContent().trim();
                    if (!assets.isEmpty()) {
                        excludeAssets = Arrays.stream(assets.split(";"))
                                .map(String::trim)
                                .collect(Collectors.toList());
                    }
                }
                
                PackageReference reference = new PackageReference(include, version, includeAssets, excludeAssets);
                projectModel.addPackageReference(reference);
            }
        }
    }
    
    private void parseFileReferences(Document document, ProjectModel projectModel) {
        NodeList referenceNodes = document.getElementsByTagName("Reference");
        
        for (int i = 0; i < referenceNodes.getLength(); i++) {
            Element referenceElement = (Element) referenceNodes.item(i);
            String include = referenceElement.getAttribute("Include");
            
            if (!include.isEmpty()) {
                String hintPath = null;
                boolean isPrivate = true;
                boolean specificVersion = false;
                
                // 解析HintPath
                Node hintPathNode = referenceElement.getElementsByTagName("HintPath").item(0);
                if (hintPathNode != null && hintPathNode.getNodeType() == Node.ELEMENT_NODE) {
                    hintPath = hintPathNode.getTextContent().trim();
                }
                
                // 解析Private
                Node privateNode = referenceElement.getElementsByTagName("Private").item(0);
                if (privateNode != null && privateNode.getNodeType() == Node.ELEMENT_NODE) {
                    isPrivate = Boolean.parseBoolean(privateNode.getTextContent().trim());
                }
                
                // 解析SpecificVersion
                Node specificVersionNode = referenceElement.getElementsByTagName("SpecificVersion").item(0);
                if (specificVersionNode != null && specificVersionNode.getNodeType() == Node.ELEMENT_NODE) {
                    specificVersion = Boolean.parseBoolean(specificVersionNode.getTextContent().trim());
                }
                
                FileReference reference = new FileReference(include, hintPath, isPrivate, specificVersion);
                projectModel.addFileReference(reference);
            }
        }
    }
    
    private void parseCompileFiles(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList compileNodes = document.getElementsByTagName("Compile");
        NodeList contentNodes = document.getElementsByTagName("Content");
        NodeList embeddedResourceNodes = document.getElementsByTagName("EmbeddedResource");
        
        List<String> compileFiles = new ArrayList<>();
        
        // 解析Compile节点
        for (int i = 0; i < compileNodes.getLength(); i++) {
            Element element = (Element) compileNodes.item(i);
            String includePath = element.getAttribute("Include");
            if (!includePath.isEmpty()) {
                String filePath = resolvePath(includePath, projectDirectory);
                compileFiles.add(filePath);
            }
        }
        
        // 解析Content节点
        for (int i = 0; i < contentNodes.getLength(); i++) {
            Element element = (Element) contentNodes.item(i);
            String includePath = element.getAttribute("Include");
            if (!includePath.isEmpty()) {
                String filePath = resolvePath(includePath, projectDirectory);
                compileFiles.add(filePath);
            }
        }
        
        // 解析EmbeddedResource节点
        for (int i = 0; i < embeddedResourceNodes.getLength(); i++) {
            Element element = (Element) embeddedResourceNodes.item(i);
            String includePath = element.getAttribute("Include");
            if (!includePath.isEmpty()) {
                String filePath = resolvePath(includePath, projectDirectory);
                compileFiles.add(filePath);
            }
        }
        
        projectModel.setCompileFiles(compileFiles);
    }
    
    private void parseDefaultIncludes(ProjectModel projectModel, String projectDirectory) {
        // SDK风格项目默认包含所有源代码文件
        File projectDir = new File(projectDirectory);
        List<String> defaultFiles = new ArrayList<>();
        
        // 根据项目类型添加默认文件
        if (projectModel.isCSharpProject()) {
            addSourceFilesRecursively(projectDir, ".cs", defaultFiles);
        } else if (projectModel.isFSharpProject()) {
            addSourceFilesRecursively(projectDir, ".fs", defaultFiles);
        } else if (projectModel.isVisualBasicProject()) {
            addSourceFilesRecursively(projectDir, ".vb", defaultFiles);
        }
        
        // 也添加资源文件
        addSourceFilesRecursively(projectDir, ".resx", defaultFiles);
        addSourceFilesRecursively(projectDir, ".ico", defaultFiles);
        addSourceFilesRecursively(projectDir, ".png", defaultFiles);
        addSourceFilesRecursively(projectDir, ".jpg", defaultFiles);
        addSourceFilesRecursively(projectDir, ".jpeg", defaultFiles);
        addSourceFilesRecursively(projectDir, ".gif", defaultFiles);
        
        // 合并现有文件列表和默认文件列表，避免重复
        List<String> existingFiles = projectModel.getCompileFiles();
        Set<String> combinedFiles = new HashSet<>(existingFiles);
        combinedFiles.addAll(defaultFiles);
        projectModel.setCompileFiles(new ArrayList<>(combinedFiles));
    }
    
    private void addSourceFilesRecursively(File directory, String extension, List<String> files) {
        if (!directory.exists() || !directory.isDirectory()) {
            return;
        }
        
        File[] children = directory.listFiles();
        if (children == null) {
            return;
        }
        
        for (File child : children) {
            if (child.isDirectory()) {
                // 跳过bin和obj目录
                if (!child.getName().equals("bin") && !child.getName().equals("obj")) {
                    addSourceFilesRecursively(child, extension, files);
                }
            } else if (child.isFile() && child.getName().endsWith(extension)) {
                files.add(child.getAbsolutePath());
            }
        }
    }
    
    private String getProjectName(File projectFile) {
        String fileName = projectFile.getName();
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            return fileName.substring(0, extensionIndex);
        }
        return fileName;
    }
    
    private String resolvePath(String relativePath, String baseDirectory) {
        File file = new File(baseDirectory, relativePath);
        return file.getAbsolutePath();
    }
    
    private boolean evaluateCondition(String condition, ProjectModel projectModel) {
        // 简单的条件评估，实际项目可能需要更复杂的逻辑
        // 这里只处理基本的框架条件
        if (condition.contains("'$(TargetFramework)' == '")) {
            String framework = condition.substring(condition.indexOf("'$(TargetFramework)' == '") + 22, condition.lastIndexOf("'"));
            return framework.equals(projectModel.getTargetFramework());
        }
        
        if (condition.contains("'$(Configuration)' == '")) {
            String config = condition.substring(condition.indexOf("'$(Configuration)' == '") + 24, condition.lastIndexOf("'"));
            // 这里假设默认配置为Debug
            return config.equals("Debug");
        }
        
        return true; // 默认情况下，如果无法评估条件，则应用该PropertyGroup
    }
    
    @Override
    public List<String> parseProjectReferences(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getProjectDependencies();
    }
    
    @Override
    public List<PackageReference> parsePackageReferences(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getPackageReferences();
    }
    
    @Override
    public List<FileReference> parseFileReferences(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getFileReferences();
    }
    
    @Override
    public List<String> parseCompileFiles(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getCompileFiles();
    }
    
    @Override
    public Map<String, String> parseProjectProperties(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getProperties();
    }
    
    @Override
    public List<String> parseTargetFrameworks(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        List<String> frameworks = new ArrayList<>();
        String framework = model.getTargetFramework();
        if (framework != null && !framework.isEmpty()) {
            frameworks.add(framework);
        }
        return frameworks;
    }
    
    @Override
    public Map<String, Map<String, String>> parseConfigurations(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        Map<String, ProjectConfiguration> configs = model.getConfigurations();
        Map<String, Map<String, String>> result = new java.util.HashMap<>();
        
        for (Map.Entry<String, ProjectConfiguration> entry : configs.entrySet()) {
            ProjectConfiguration config = entry.getValue();
            Map<String, String> configProps = new java.util.HashMap<>();
            // 添加配置的基本属性
            // 使用getProperty获取各种配置值
            configProps.put("OutputPath", getPropertyWithDefault(config, "OutputPath", ""));
            configProps.put("OutputType", getPropertyWithDefault(config, "OutputType", ""));
            configProps.put("DefineConstants", getPropertyWithDefault(config, "DefineConstants", ""));
            configProps.put("Optimize", getPropertyWithDefault(config, "Optimize", "false"));
            configProps.put("DebugSymbols", Boolean.toString(config.isDebugInfo()));
            result.put(entry.getKey(), configProps);
        }
        return result;
    }
    
    /**
     * 辅助方法：获取属性值，如果不存在则返回默认值
     */
    private String getPropertyWithDefault(ProjectConfiguration config, String key, String defaultValue) {
        String value = config.getProperty(key);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public boolean isValidProjectFile(Path projectFilePath) {
        // 检查文件是否存在且是.csproj文件
        if (projectFilePath == null || !projectFilePath.toString().toLowerCase().endsWith(".csproj")) {
            return false;
        }
        
        File file = projectFilePath.toFile();
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        try {
            // 尝试简单解析XML结构来验证
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
            document.getDocumentElement().normalize();
            
            // SDK风格项目通常在Project元素上有Sdk属性
            Element projectElement = document.getDocumentElement();
            return projectElement.hasAttribute("Sdk") || 
                   document.getElementsByTagName("Sdk").getLength() > 0;
        } catch (Exception e) {
            // 如果解析失败，不是有效的项目文件
            return false;
        }
    }
}