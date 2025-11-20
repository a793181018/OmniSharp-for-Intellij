package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.PackageReference;
import com.omnisharp.intellij.projectstructure.model.FileReference;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import com.omnisharp.intellij.projectstructure.model.ProjectLanguage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * C#项目文件(.csproj)解析器实现
 */
public class CsprojParser implements ProjectParser {
    private static final Pattern CS_PROJECT_PATTERN = Pattern.compile(".*\\.csproj$", Pattern.CASE_INSENSITIVE);
    private static final Pattern VBNET_PROJECT_PATTERN = Pattern.compile(".*\\.vbproj$", Pattern.CASE_INSENSITIVE);
    private static final Pattern FSHARP_PROJECT_PATTERN = Pattern.compile(".*\\.fsproj$", Pattern.CASE_INSENSITIVE);

    @Override
    public ProjectModel parse(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        if (!isValidProjectFile(projectFilePath)) {
            throw new SolutionParser.ParseException("Invalid project file: " + projectFilePath);
        }

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false); // .csproj文件通常不使用命名空间
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(projectFilePath.toFile());
            doc.getDocumentElement().normalize();

            // 解析项目属性
            Map<String, String> properties = parseProjectProperties(doc);
            
            // 构建项目模型
            String projectId = UUID.randomUUID().toString(); // 可以使用项目路径生成更稳定的ID
            String projectName = projectFilePath.getFileName().toString().replaceFirst("\\.(cs|vb|fs)proj$", "");
            String projectPath = projectFilePath.toString();
            
            ProjectModel projectModel = new ProjectModel(
                    projectId,                          // id
                    projectName,                        // name
                    projectPath,                        // path
                    projectFilePath.getParent().toString(), // directory
                    "bin/Debug/",                       // outputPath (默认值)
                    projectName,                        // assemblyName
                    parseTargetFrameworks(projectFilePath) != null && !parseTargetFrameworks(projectFilePath).isEmpty() ? parseTargetFrameworks(projectFilePath).get(0) : "netstandard2.0", // targetFramework
                    new HashMap<>(),                    // configurations
                    new ArrayList<>(),                  // projectReferences
                    new ArrayList<>(),                  // packageReferences
                    new ArrayList<>(),                  // fileReferences
                    new ArrayList<>(),                  // projectFiles
                    ProjectLanguage.CSHARP              // language (默认C#)
            );

            // 设置项目属性
            properties.forEach(projectModel::setProperty);

            // 添加项目引用
            parseProjectReferences(projectFilePath).forEach(projectModel::addProjectReference);
            
            // 添加包引用
            parsePackageReferences(projectFilePath).forEach(projectModel::addPackageReference);
            
            // 添加文件引用
            parseFileReferences(projectFilePath).forEach(projectModel::addFileReference);
            
            // 添加编译文件
            parseCompileFiles(projectFilePath).forEach(projectModel::addCompileFile);

            return projectModel;
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse project file: " + projectFilePath, e);
        }
    }

    @Override
    public List<String> parseProjectReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        List<String> references = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            NodeList projectReferenceNodes = doc.getElementsByTagName("ProjectReference");
            
            for (int i = 0; i < projectReferenceNodes.getLength(); i++) {
                Element element = (Element) projectReferenceNodes.item(i);
                Node nameNode = element.getElementsByTagName("Name").item(0);
                Node includeAttr = element.getAttributeNode("Include");
                
                if (nameNode != null) {
                    references.add(nameNode.getTextContent().trim());
                } else if (includeAttr != null) {
                    // 提取项目文件名作为引用名称
                    String includePath = includeAttr.getNodeValue();
                    int lastSlashIndex = includePath.lastIndexOf('/');
                    int lastBackslashIndex = includePath.lastIndexOf('\\');
                    int lastIndex = Math.max(lastSlashIndex, lastBackslashIndex);
                    if (lastIndex >= 0 && lastIndex < includePath.length() - 1) {
                        String fileName = includePath.substring(lastIndex + 1);
                        if (fileName.endsWith(".csproj")) {
                            references.add(fileName.substring(0, fileName.length() - 7));
                        } else {
                            references.add(fileName);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse project references", e);
        }
        return references;
    }

    @Override
    public List<PackageReference> parsePackageReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        List<PackageReference> references = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            
            // 解析PackageReference节点
            NodeList packageNodes = doc.getElementsByTagName("PackageReference");
            for (int i = 0; i < packageNodes.getLength(); i++) {
                Element element = (Element) packageNodes.item(i);
                String include = element.getAttribute("Include");
                String version = element.getAttribute("Version");
                
                // 如果Version不在属性中，可能在子节点
                if (version == null || version.isEmpty()) {
                    Node versionNode = element.getElementsByTagName("Version").item(0);
                    if (versionNode != null) {
                        version = versionNode.getTextContent().trim();
                    }
                }
                
                if (include != null && !include.isEmpty() && version != null && !version.isEmpty()) {
                    String includeAssets = getChildElementValue(element, "IncludeAssets");
                    String excludeAssets = getChildElementValue(element, "ExcludeAssets");
                    
                    // 将单个字符串转换为List
                    List<String> includeAssetsList = includeAssets != null ? Collections.singletonList(includeAssets) : Collections.emptyList();
                    List<String> excludeAssetsList = excludeAssets != null ? Collections.singletonList(excludeAssets) : Collections.emptyList();
                    references.add(new PackageReference(include, version, includeAssetsList, excludeAssetsList));
                }
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse package references", e);
        }
        return references;
    }

    @Override
    public List<FileReference> parseFileReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        List<FileReference> references = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            
            // 解析Reference节点（文件引用）
            NodeList referenceNodes = doc.getElementsByTagName("Reference");
            for (int i = 0; i < referenceNodes.getLength(); i++) {
                Element element = (Element) referenceNodes.item(i);
                String include = element.getAttribute("Include");
                
                if (include != null && !include.isEmpty()) {
                    String hintPath = getChildElementValue(element, "HintPath");
                    boolean isPrivate = Boolean.parseBoolean(getChildElementValue(element, "Private", "false"));
                    boolean specificVersion = Boolean.parseBoolean(getChildElementValue(element, "SpecificVersion", "false"));
                    
                    references.add(new FileReference(include, hintPath, isPrivate, specificVersion));
                }
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse file references", e);
        }
        return references;
    }

    @Override
    public Map<String, Map<String, String>> parseConfigurations(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        Map<String, Map<String, String>> configurations = new HashMap<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            
            // 查找所有PropertyGroup节点，它们可能包含配置特定的属性
            NodeList propertyGroups = doc.getElementsByTagName("PropertyGroup");
            for (int i = 0; i < propertyGroups.getLength(); i++) {
                Element group = (Element) propertyGroups.item(i);
                String condition = group.getAttribute("Condition");
                
                // 尝试提取配置名称
                String configName = "Debug"; // 默认值
                if (condition != null && !condition.isEmpty()) {
                    if (condition.contains("'Release'")) {
                        configName = "Release";
                    } else if (condition.contains("'Debug'")) {
                        configName = "Debug";
                    }
                }
                
                Map<String, String> configProps = configurations.computeIfAbsent(configName, k -> new HashMap<>());
                
                // 提取所有子元素作为配置属性
                NodeList children = group.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    Node node = children.item(j);
                    if (node.getNodeType() == Node.ELEMENT_NODE) {
                        Element child = (Element) node;
                        configProps.put(child.getNodeName(), child.getTextContent().trim());
                    }
                }
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse configurations", e);
        }
        return configurations;
    }

    @Override
    public Map<String, String> parseProjectProperties(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        try {
            Document doc = parseXmlDocument(projectFilePath);
            return parseProjectProperties(doc);
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse project properties", e);
        }
    }

    private Map<String, String> parseProjectProperties(Document doc) {
        Map<String, String> properties = new HashMap<>();
        
        // 查找所有PropertyGroup节点中的属性
        NodeList propertyGroups = doc.getElementsByTagName("PropertyGroup");
        for (int i = 0; i < propertyGroups.getLength(); i++) {
            Element group = (Element) propertyGroups.item(i);
            NodeList children = group.getChildNodes();
            
            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element) node;
                    properties.put(child.getNodeName(), child.getTextContent().trim());
                }
            }
        }
        
        return properties;
    }

    @Override
    public List<String> parseCompileFiles(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        List<String> compileFiles = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            
            // 解析Compile节点
            NodeList compileNodes = doc.getElementsByTagName("Compile");
            for (int i = 0; i < compileNodes.getLength(); i++) {
                Element element = (Element) compileNodes.item(i);
                String include = element.getAttribute("Include");
                if (include != null && !include.isEmpty()) {
                    compileFiles.add(include);
                }
            }
            
            // 对于SDK风格的项目，可能需要额外处理
            if (compileFiles.isEmpty()) {
                // SDK风格项目通常使用通配符，这里简单处理
                String projectDir = projectFilePath.getParent().toString();
                String fileExtension = ".cs"; // 默认假设C#项目
                if (projectFilePath.toString().toLowerCase().endsWith(".vbproj")) {
                    fileExtension = ".vb";
                } else if (projectFilePath.toString().toLowerCase().endsWith(".fsproj")) {
                    fileExtension = ".fs";
                }
                
                // 注意：这里只是示例，实际实现需要递归扫描目录
                compileFiles.add("**/*" + fileExtension);
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse compile files", e);
        }
        return compileFiles;
    }

    @Override
    public boolean isValidProjectFile(Path projectFilePath) {
        if (!Files.exists(projectFilePath) || !Files.isRegularFile(projectFilePath)) {
            return false;
        }
        
        String fileName = projectFilePath.getFileName().toString().toLowerCase();
        return fileName.endsWith(".csproj") || 
               fileName.endsWith(".vbproj") || 
               fileName.endsWith(".fsproj");
    }

    @Override
    public List<String> parseTargetFrameworks(Path projectFilePath) throws IOException, SolutionParser.ParseException {
        List<String> targetFrameworks = new ArrayList<>();
        try {
            Document doc = parseXmlDocument(projectFilePath);
            
            // 优先查找TargetFrameworks（多目标）
            NodeList multiTargetNodes = doc.getElementsByTagName("TargetFrameworks");
            if (multiTargetNodes.getLength() > 0) {
                String frameworks = multiTargetNodes.item(0).getTextContent().trim();
                for (String framework : frameworks.split("\\s*;\\s*")) {
                    if (!framework.isEmpty()) {
                        targetFrameworks.add(framework);
                    }
                }
            } else {
                // 查找单个TargetFramework
                NodeList singleTargetNodes = doc.getElementsByTagName("TargetFramework");
                if (singleTargetNodes.getLength() > 0) {
                    targetFrameworks.add(singleTargetNodes.item(0).getTextContent().trim());
                }
            }
            
            // 默认值
            if (targetFrameworks.isEmpty()) {
                targetFrameworks.add(".NET Framework");
            }
        } catch (Exception e) {
            throw new SolutionParser.ParseException("Failed to parse target frameworks", e);
        }
        return targetFrameworks;
    }

    private Document parseXmlDocument(Path filePath) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(filePath.toFile());
    }

    private String getChildElementValue(Element parent, String elementName) {
        return getChildElementValue(parent, elementName, null);
    }

    private String getChildElementValue(Element parent, String elementName, String defaultValue) {
        NodeList nodes = parent.getElementsByTagName(elementName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent().trim();
        }
        return defaultValue;
    }

    private String parseProjectType(Path projectFilePath) {
        String fileName = projectFilePath.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".csproj")) {
            return "C#";
        } else if (fileName.endsWith(".vbproj")) {
            return "Visual Basic";
        } else if (fileName.endsWith(".fsproj")) {
            return "F#";
        }
        return "Unknown";
    }
}