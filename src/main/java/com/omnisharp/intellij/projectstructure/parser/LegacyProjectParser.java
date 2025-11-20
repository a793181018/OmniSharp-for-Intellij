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
import java.util.ArrayList;
import java.util.stream.Collectors;

/**
 * 传统风格项目文件解析器
 * 处理.NET Framework的旧格式项目文件
 */
public class LegacyProjectParser implements ProjectParser {
    private static final String DEFAULT_TARGET_FRAMEWORK = "v4.7.2";
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
        projectModel.setSdkProject(false);
        projectModel.setProjectFileTimestamp(projectFile.lastModified());
        
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile);
            document.getDocumentElement().normalize();
            
            // 解析项目类型GUID
            parseProjectTypeGuids(document, projectModel);
            
            // 解析基本属性
            parseProjectProperties(document, projectModel);
            
            // 解析配置
            parseConfigurations(document, projectModel);
            
            // 解析项目引用
            parseProjectReferences(document, projectModel, projectDirectory);
            
            // 解析包引用（传统项目使用packages.config或直接在项目文件中定义）
            parsePackageReferences(document, projectModel, projectDirectory);
            
            // 解析文件引用
            parseFileReferences(document, projectModel);
            
            // 解析编译文件
            parseCompileFiles(document, projectModel, projectDirectory);
            
            // 解析资源文件
            parseResourceFiles(document, projectModel, projectDirectory);
            
            // 解析内容文件
            parseContentFiles(document, projectModel, projectDirectory);
            
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new SolutionParser.ParseException("Failed to parse project file: " + e.getMessage(), e);
        }
        
        return projectModel;
    }
    
    private void parseProjectTypeGuids(Document document, ProjectModel projectModel) {
        NodeList propertyGroups = document.getElementsByTagName("PropertyGroup");
        
        for (int i = 0; i < propertyGroups.getLength(); i++) {
            Element propertyGroup = (Element) propertyGroups.item(i);
            Node projectTypeGuidsNode = propertyGroup.getElementsByTagName("ProjectTypeGuids").item(0);
            
            if (projectTypeGuidsNode != null && projectTypeGuidsNode.getNodeType() == Node.ELEMENT_NODE) {
                String guidsText = projectTypeGuidsNode.getTextContent().trim();
                if (!guidsText.isEmpty()) {
                    String[] guids = guidsText.split(";\s*");
                    for (String guid : guids) {
                        if (!guid.trim().isEmpty()) {
                            projectModel.addProjectTypeGuid(guid.trim());
                        }
                    }
                }
                break;
            }
        }
    }
    
    private void parseProjectProperties(Document document, ProjectModel projectModel) {
        NodeList propertyGroups = document.getElementsByTagName("PropertyGroup");
        
        for (int i = 0; i < propertyGroups.getLength(); i++) {
            Element propertyGroup = (Element) propertyGroups.item(i);
            
            // 检查是否有条件属性组
            String condition = propertyGroup.getAttribute("Condition");
            if (!condition.isEmpty()) {
                // 跳过有特定条件的属性组，除非是默认的Debug/AnyCPU配置
                if (!condition.contains("Debug|AnyCPU") && !condition.contains("Configuration|Platform")) {
                    continue;
                }
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
            } else {
                projectModel.setOutputPath(DEFAULT_OUTPUT_PATH);
            }
            
            // 解析OutputType
            Node outputTypeNode = propertyGroup.getElementsByTagName("OutputType").item(0);
            if (outputTypeNode != null && outputTypeNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setOutputType(outputTypeNode.getTextContent().trim());
            } else {
                projectModel.setOutputType(DEFAULT_OUTPUT_TYPE);
            }
            
            // 解析TargetFrameworkVersion
            Node targetFrameworkVersionNode = propertyGroup.getElementsByTagName("TargetFrameworkVersion").item(0);
            if (targetFrameworkVersionNode != null && targetFrameworkVersionNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setTargetFramework(targetFrameworkVersionNode.getTextContent().trim());
            } else {
                projectModel.setTargetFramework(DEFAULT_TARGET_FRAMEWORK);
            }
            
            // 解析MSBuildToolsVersion
            Node msbuildToolsVersionNode = propertyGroup.getElementsByTagName("ToolsVersion").item(0);
            if (msbuildToolsVersionNode != null && msbuildToolsVersionNode.getNodeType() == Node.ELEMENT_NODE) {
                projectModel.setMsbuildToolsVersion(msbuildToolsVersionNode.getTextContent().trim());
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
            NodeList children = propertyGroup.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String name = child.getNodeName();
                    String value = child.getTextContent().trim();
                    if (!name.equals("AssemblyName") && !name.equals("RootNamespace") &&
                        !name.equals("OutputPath") && !name.equals("OutputType") &&
                        !name.equals("TargetFrameworkVersion") && !name.equals("ToolsVersion") &&
                        !name.equals("DefineConstants") && !name.equals("ProjectTypeGuids")) {
                        projectModel.setProperty(name, value);
                    }
                }
            }
        }
    }
    
    private void parseConfigurations(Document document, ProjectModel projectModel) {
        // 解析Configurations和Platforms节点
        NodeList propertyGroups = document.getElementsByTagName("PropertyGroup");
        
        for (int i = 0; i < propertyGroups.getLength(); i++) {
            Element propertyGroup = (Element) propertyGroups.item(i);
            
            // 查找包含Configurations的PropertyGroup
            Node configurationsNode = propertyGroup.getElementsByTagName("Configurations").item(0);
            Node platformsNode = propertyGroup.getElementsByTagName("Platforms").item(0);
            
            if (configurationsNode != null && configurationsNode.getNodeType() == Node.ELEMENT_NODE) {
                String configurationsText = configurationsNode.getTextContent().trim();
                String[] configurations = configurationsText.split(";\s*");
                
                for (String configName : configurations) {
                    if (!configName.trim().isEmpty()) {
                        ProjectConfiguration configuration = new ProjectConfiguration(
                                configName.trim(), 
                                platformsNode != null ? platformsNode.getTextContent().split(";\s*")[0].trim() : "AnyCPU");
                        // 这里简化处理，实际应该从特定配置的PropertyGroup中读取属性
                        projectModel.addConfiguration(configName.trim(), configuration);
                    }
                }
                break;
            }
        }
    }
    
    private void parseProjectReferences(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList itemGroups = document.getElementsByTagName("ItemGroup");
        
        for (int i = 0; i < itemGroups.getLength(); i++) {
            Element itemGroup = (Element) itemGroups.item(i);
            NodeList projectReferenceNodes = itemGroup.getElementsByTagName("ProjectReference");
            
            for (int j = 0; j < projectReferenceNodes.getLength(); j++) {
                Element referenceElement = (Element) projectReferenceNodes.item(j);
                String includePath = referenceElement.getAttribute("Include");
                String projectId = referenceElement.getAttribute("Project"); // 通常是GUID
                
                if (!includePath.isEmpty()) {
                    String projectPath = resolvePath(includePath, projectDirectory);
                    File referencedProjectFile = new File(projectPath);
                    String projectName = getProjectName(referencedProjectFile);
                    
                    ProjectReference reference = new ProjectReference(
                            projectId != null && !projectId.isEmpty() ? projectId : UUID.randomUUID().toString(),
                            projectName,
                            projectPath);
                    projectModel.addProjectReference(reference);
                }
            }
        }
    }
    
    private void parsePackageReferences(Document document, ProjectModel projectModel, String projectDirectory) {
        // 1. 首先尝试解析项目文件中的PackageReference（传统项目也可能使用）
        NodeList packageReferenceNodes = document.getElementsByTagName("PackageReference");
        for (int i = 0; i < packageReferenceNodes.getLength(); i++) {
            Element referenceElement = (Element) packageReferenceNodes.item(i);
            String include = referenceElement.getAttribute("Include");
            String version = referenceElement.getAttribute("Version");
            
            if (!include.isEmpty() && !version.isEmpty()) {
                PackageReference reference = new PackageReference(include, version);
                projectModel.addPackageReference(reference);
            }
        }
        
        // 2. 检查packages.config文件（传统项目常用）
        File packagesConfig = new File(projectDirectory, "packages.config");
        if (packagesConfig.exists()) {
            try {
                Document packagesDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(packagesConfig);
                packagesDoc.getDocumentElement().normalize();
                
                NodeList packageNodes = packagesDoc.getElementsByTagName("package");
                for (int i = 0; i < packageNodes.getLength(); i++) {
                    Element packageElement = (Element) packageNodes.item(i);
                    String id = packageElement.getAttribute("id");
                    String version = packageElement.getAttribute("version");
                    
                    if (!id.isEmpty() && !version.isEmpty()) {
                        PackageReference reference = new PackageReference(id, version);
                        projectModel.addPackageReference(reference);
                    }
                }
            } catch (Exception e) {
                // 如果packages.config解析失败，忽略异常
            }
        }
        
        // 3. 解析Reference节点中的NuGet包引用（旧格式）
        NodeList referenceNodes = document.getElementsByTagName("Reference");
        for (int i = 0; i < referenceNodes.getLength(); i++) {
            Element referenceElement = (Element) referenceNodes.item(i);
            String include = referenceElement.getAttribute("Include");
            
            // 检查是否有HintPath指向packages目录
            Node hintPathNode = referenceElement.getElementsByTagName("HintPath").item(0);
            if (hintPathNode != null && hintPathNode.getNodeType() == Node.ELEMENT_NODE) {
                String hintPath = hintPathNode.getTextContent().trim();
                if (hintPath.contains("packages\\")) {
                    // 尝试从HintPath提取包信息
                    int packagesIndex = hintPath.indexOf("packages\\") + 9;
                    int nextSlashIndex = hintPath.indexOf('\\', packagesIndex);
                    if (nextSlashIndex > 0) {
                        String packageInfo = hintPath.substring(packagesIndex, nextSlashIndex);
                        // 包名通常是"PackageId.Version"格式
                        int versionDelimiterIndex = findVersionDelimiterIndex(packageInfo);
                        if (versionDelimiterIndex > 0) {
                            String id = packageInfo.substring(0, versionDelimiterIndex);
                            String version = packageInfo.substring(versionDelimiterIndex + 1);
                            
                            // 检查是否已经添加过这个包
                            boolean alreadyExists = projectModel.getPackageReferences().stream()
                                    .anyMatch(ref -> ref.getId().equals(id));
                            
                            if (!alreadyExists) {
                                PackageReference reference = new PackageReference(id, version);
                                projectModel.addPackageReference(reference);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private int findVersionDelimiterIndex(String packageInfo) {
        // 查找版本号的分隔符（通常是第一个数字前的点）
        for (int i = 0; i < packageInfo.length(); i++) {
            char c = packageInfo.charAt(i);
            if (c == '.' && i + 1 < packageInfo.length() && Character.isDigit(packageInfo.charAt(i + 1))) {
                return i;
            }
        }
        return -1;
    }
    
    private void parseFileReferences(Document document, ProjectModel projectModel) {
        NodeList itemGroups = document.getElementsByTagName("ItemGroup");
        
        for (int i = 0; i < itemGroups.getLength(); i++) {
            Element itemGroup = (Element) itemGroups.item(i);
            NodeList referenceNodes = itemGroup.getElementsByTagName("Reference");
            
            for (int j = 0; j < referenceNodes.getLength(); j++) {
                Element referenceElement = (Element) referenceNodes.item(j);
                String include = referenceElement.getAttribute("Include");
                
                // 跳过项目引用和NuGet包引用
                if (!include.contains(", Version=")) {
                    String hintPath = null;
                    boolean isPrivate = true;
                    boolean specificVersion = false;
                    
                    Node hintPathNode = referenceElement.getElementsByTagName("HintPath").item(0);
                    if (hintPathNode != null && hintPathNode.getNodeType() == Node.ELEMENT_NODE) {
                        hintPath = hintPathNode.getTextContent().trim();
                    }
                    
                    Node privateNode = referenceElement.getElementsByTagName("Private").item(0);
                    if (privateNode != null && privateNode.getNodeType() == Node.ELEMENT_NODE) {
                        isPrivate = Boolean.parseBoolean(privateNode.getTextContent().trim());
                    }
                    
                    Node specificVersionNode = referenceElement.getElementsByTagName("SpecificVersion").item(0);
                    if (specificVersionNode != null && specificVersionNode.getNodeType() == Node.ELEMENT_NODE) {
                        specificVersion = Boolean.parseBoolean(specificVersionNode.getTextContent().trim());
                    }
                    
                    FileReference reference = new FileReference(include, hintPath, isPrivate, specificVersion);
                    projectModel.addFileReference(reference);
                }
            }
        }
    }
    
    private void parseCompileFiles(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList itemGroups = document.getElementsByTagName("ItemGroup");
        List<String> compileFiles = new ArrayList<>();
        
        for (int i = 0; i < itemGroups.getLength(); i++) {
            Element itemGroup = (Element) itemGroups.item(i);
            NodeList compileNodes = itemGroup.getElementsByTagName("Compile");
            
            for (int j = 0; j < compileNodes.getLength(); j++) {
                Element compileElement = (Element) compileNodes.item(j);
                String includePath = compileElement.getAttribute("Include");
                if (!includePath.isEmpty()) {
                    String filePath = resolvePath(includePath, projectDirectory);
                    compileFiles.add(filePath);
                }
            }
        }
        
        // 传统项目不会自动包含文件，所以只添加明确声明的文件
        projectModel.setCompileFiles(compileFiles);
    }
    
    private void parseResourceFiles(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList itemGroups = document.getElementsByTagName("ItemGroup");
        
        for (int i = 0; i < itemGroups.getLength(); i++) {
            Element itemGroup = (Element) itemGroups.item(i);
            NodeList resourceNodes = itemGroup.getElementsByTagName("EmbeddedResource");
            
            for (int j = 0; j < resourceNodes.getLength(); j++) {
                Element resourceElement = (Element) resourceNodes.item(j);
                String includePath = resourceElement.getAttribute("Include");
                if (!includePath.isEmpty()) {
                    String filePath = resolvePath(includePath, projectDirectory);
                    projectModel.addCompileFile(filePath); // 资源文件也添加到编译文件列表
                }
            }
        }
    }
    
    private void parseContentFiles(Document document, ProjectModel projectModel, String projectDirectory) {
        NodeList itemGroups = document.getElementsByTagName("ItemGroup");
        
        for (int i = 0; i < itemGroups.getLength(); i++) {
            Element itemGroup = (Element) itemGroups.item(i);
            NodeList contentNodes = itemGroup.getElementsByTagName("Content");
            
            for (int j = 0; j < contentNodes.getLength(); j++) {
                Element contentElement = (Element) contentNodes.item(j);
                String includePath = contentElement.getAttribute("Include");
                if (!includePath.isEmpty()) {
                    String filePath = resolvePath(includePath, projectDirectory);
                    projectModel.addCompileFile(filePath); // 内容文件也添加到编译文件列表
                }
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
    
    @Override
    public List<String> parseProjectReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        // 直接返回空列表，避免调用不存在的方法
        return new ArrayList<>();
    }
    
    @Override
    public List<PackageReference> parsePackageReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        // 从项目模型中获取包引用
        ProjectModel model = parse(projectFilePath);
        return model.getPackageReferences();
    }
    
    @Override
    public List<FileReference> parseFileReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectFilePath);
        // 返回文件引用列表，需要适配类型
        return model.getFileReferences();
    }
    
    @Override
    public List<String> parseCompileFiles(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectFilePath);
        return model.getCompileFiles();
    }

    @Override
    public Map<String, String> parseProjectProperties(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        // 实现项目属性解析
        ProjectModel model = parse(projectFilePath);
        return model.getProperties();
    }

    @Override
    public java.util.Map<String, java.util.Map<String, String>> parseConfigurations(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        // 返回空的配置映射，实际实现可能需要从项目文件中解析
        return new java.util.HashMap<>();
    }
    
    public String parseTargetFramework(Path projectPath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectPath);
        return model.getTargetFramework();
    }
    
    @Override
    public List<String> parseTargetFrameworks(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        ProjectModel model = parse(projectFilePath);
        List<String> frameworks = new ArrayList<>();
        String framework = model.getTargetFramework();
        if (framework != null && !framework.isEmpty()) {
            frameworks.add(framework);
        }
        return frameworks;
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
            
            // 检查是否包含典型的传统项目元素
            boolean hasProjectElement = document.getDocumentElement().getNodeName().equals("Project");
            boolean hasPropertyGroup = document.getElementsByTagName("PropertyGroup").getLength() > 0;
            
            // 传统项目通常不包含Sdk属性
            Element projectElement = document.getDocumentElement();
            boolean hasSdkAttribute = projectElement.hasAttribute("Sdk");
            
            return hasProjectElement && hasPropertyGroup && !hasSdkAttribute;
        } catch (Exception e) {
            // 如果解析失败，不是有效的项目文件
            return false;
        }
    }
}