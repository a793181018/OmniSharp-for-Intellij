package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.model.SolutionConfiguration;
import com.omnisharp.intellij.projectstructure.model.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解决方案文件(.sln)解析器实现
 */
public class SlnParser implements SolutionParser {
    // 使用SolutionFileTokenizer进行标记化解析
    private final SolutionFileTokenizer tokenizer;
    
    // 解决方案版本正则表达式
    private static final Pattern SOLUTION_VERSION_PATTERN = 
            Pattern.compile("Microsoft Visual Studio Solution File, Format Version (\\d+(\\.\\d+)?)");
    
    // 项目引用正则表达式
    private static final Pattern PROJECT_PATTERN = Pattern.compile("Project\\(\\\"([0-9A-Fa-f\\-]+)\\\"\\) = \\\"([^\\\"]+)\\\", \\\"([^\\\"]+)\\\", \\\"([0-9A-Fa-f\\-]+)\\\"");
    
    // 解决方案配置正则表达式
    private static final Pattern CONFIGURATION_PATTERN = 
            Pattern.compile("GlobalSection\\(SolutionConfigurationPlatforms\\) = preSolution");
    
    // 项目配置正则表达式
    private static final Pattern PROJECT_CONFIGURATION_PATTERN = 
            Pattern.compile("({[0-9A-Fa-f\\-]+})\\.(\\w+)\\|(\\w+) = (\\w+)\\|(\\w+)");
    
    // 项目配置映射正则表达式
    private static final Pattern CONFIGURATION_MAP_PATTERN = 
            Pattern.compile("(\\w+)\\|(\\w+) = (\\w+)\\|(\\w+)");
    
    // 全局节结束正则表达式
    private static final Pattern END_GLOBAL_SECTION_PATTERN = 
            Pattern.compile("EndGlobalSection");
            
    // 项目类型GUID映射到语言
    private static final Map<String, ProjectLanguage> PROJECT_TYPE_GUIDS = new HashMap<>();
    
    static {
        // 初始化项目类型GUID映射
        PROJECT_TYPE_GUIDS.put("FAE04EC0-301F-11D3-BF4B-00C04F79EFBC", ProjectLanguage.CSHARP);
        PROJECT_TYPE_GUIDS.put("F184B08F-C81C-45F6-A57F-5ABD9991F28F", ProjectLanguage.CSHARP);
        PROJECT_TYPE_GUIDS.put("2150E333-8FDC-42A3-9474-1A3956D46DE8", ProjectLanguage.CSHARP);
        PROJECT_TYPE_GUIDS.put("8BB2217D-0F2D-49D1-97BC-3654ED321F3B", ProjectLanguage.VISUAL_BASIC);
        PROJECT_TYPE_GUIDS.put("349C5851-65DF-11DA-9384-00065B846F21", ProjectLanguage.WEB);
        PROJECT_TYPE_GUIDS.put("E53F8FEA-EAE0-44A6-8774-FFD645390401", ProjectLanguage.FSHARP);
    }

    public SlnParser() {
        this.tokenizer = new SolutionFileTokenizer();
    }

    @Override
    public SolutionModel parse(Path solutionFilePath) throws IOException, ParseException {
        if (!isValidSolutionFile(solutionFilePath)) {
            throw new ParseException("Invalid solution file: " + solutionFilePath);
        }

        String solutionName = solutionFilePath.getFileName().toString().replace(".sln", "");
        String version = parseSolutionVersion(solutionFilePath);
        Map<String, SolutionConfiguration> configurations = parseConfigurations(solutionFilePath);
        List<ProjectReferenceInfo> projectReferences = extractProjectReferences(solutionFilePath);
        
        // 提取全局部分信息
        List<GlobalSection> globalSections = parseGlobalSections(solutionFilePath);
        
        // 创建解决方案模型
        SolutionModel solutionModel = new SolutionModel(solutionName, solutionFilePath.toString(), new HashMap<>(), configurations, version);
        
        // 添加配置
        configurations.forEach(solutionModel::addConfiguration);
        
        // 处理项目引用
        Map<String, ProjectModel> projects = new HashMap<>();
        for (ProjectReferenceInfo ref : projectReferences) {
            // 解析项目文件路径（相对于解决方案目录）
            Path projectPath = solutionFilePath.getParent().resolve(ref.getProjectPath()).normalize();
            
            // 确定项目语言
            ProjectLanguage language = determineProjectLanguage(ref.getProjectTypeGuid());
            
            // 构建项目模型
            ProjectModel project = buildProjectModel(ref, language, projectPath, solutionFilePath.getParent());
            projects.put(project.getId(), project);
            solutionModel.addProject(project);
            solutionModel.addProjectId(ref.getProjectId());
        }
        
        return solutionModel;
    }

    /**
     * 使用标记化解析解决方案文件
     */
    public SolutionModel parseWithTokenizer(Path solutionFilePath) throws IOException, ParseException {
        if (!isValidSolutionFile(solutionFilePath)) {
            throw new ParseException("Invalid solution file: " + solutionFilePath);
        }
        
        // 进行标记化解析
        List<SolutionFileTokenizer.Token> tokens = tokenizer.tokenize(solutionFilePath);
        
        // 跳过空白和注释
        List<SolutionFileTokenizer.Token> filteredTokens = tokenizer.skipWhitespaceAndComments(tokens, 0);
        
        // 解析解决方案头部
        String version = parseSolutionHeader(filteredTokens);
        
        // 解析项目部分
        List<ProjectReferenceInfo> projectReferences = parseProjects(filteredTokens, solutionFilePath.getParent());
        
        // 解析全局部分
        Map<String, Object> globalData = parseGlobal(filteredTokens);
        Map<String, SolutionConfiguration> configurations = extractConfigurationsFromGlobal(globalData);
        
        // 构建解决方案模型
        String solutionName = solutionFilePath.getFileName().toString().replace(".sln", "");
        SolutionModel solutionModel = new SolutionModel(
                solutionName,
                solutionFilePath.toString(),
                new HashMap<>(),
                configurations,
                version
        );
        
        // 添加项目
        for (ProjectReferenceInfo ref : projectReferences) {
            ProjectLanguage language = determineProjectLanguage(ref.getProjectTypeGuid());
            ProjectModel project = buildProjectModel(ref, language, ref.getProjectPath(), solutionFilePath.getParent());
            solutionModel.addProject(project);
            solutionModel.addProjectId(ref.getProjectId());
        }
        
        return solutionModel;
    }

    @Override
    public String parseSolutionVersion(Path solutionFilePath) throws IOException, ParseException {
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = SOLUTION_VERSION_PATTERN.matcher(line);
                if (matcher.matches()) {
                    return matcher.group(1);
                }
            }
        }
        throw new ParseException("Could not find solution version in " + solutionFilePath);
    }

    @Override
    public Map<String, SolutionConfiguration> parseConfigurations(Path solutionFilePath) throws IOException, ParseException {
        Map<String, SolutionConfiguration> configurations = new HashMap<>();
        boolean inConfigurationSection = false;
        
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 查找配置节开始
                if (CONFIGURATION_PATTERN.matcher(line).matches()) {
                    inConfigurationSection = true;
                    continue;
                }
                
                // 查找配置节结束
                if (inConfigurationSection && END_GLOBAL_SECTION_PATTERN.matcher(line).matches()) {
                    break;
                }
                
                // 解析配置映射
                if (inConfigurationSection) {
                    Matcher matcher = CONFIGURATION_MAP_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        String configName = matcher.group(1);
                        String platform = matcher.group(2);
                        String key = configName + "|" + platform;
                        
                        if (!configurations.containsKey(key)) {
                            configurations.put(key, new SolutionConfiguration(configName, new HashMap<>()));
                        }
                    }
                }
            }
        }
        
        // 解析项目配置映射
        parseProjectConfigurations(solutionFilePath, configurations);
        
        return configurations;
    }

    private void parseProjectConfigurations(Path solutionFilePath, Map<String, SolutionConfiguration> configurations)
            throws IOException, ParseException {
        boolean inProjectConfigurationSection = false;
        
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 查找项目配置节
                if (line.trim().startsWith("GlobalSection(ProjectConfigurationPlatforms) = postSolution")) {
                    inProjectConfigurationSection = true;
                    continue;
                }
                
                if (inProjectConfigurationSection && END_GLOBAL_SECTION_PATTERN.matcher(line).matches()) {
                    break;
                }
                
                // 解析项目配置
                if (inProjectConfigurationSection) {
                    Matcher matcher = PROJECT_CONFIGURATION_PATTERN.matcher(line.trim());
                    if (matcher.matches()) {
                        String projectId = matcher.group(1);
                        String configKey = matcher.group(2) + "|" + matcher.group(3);
                        String configName = matcher.group(4);
                        String platform = matcher.group(5);
                        
                        SolutionConfiguration config = configurations.get(configKey);
                        if (config != null) {
                            config.addProjectConfiguration(projectId, configName, platform);
                        }
                    }
                }
            }
        }
    }

    @Override
    public boolean isValidSolutionFile(Path solutionFilePath) {
        if (!Files.exists(solutionFilePath) || !Files.isRegularFile(solutionFilePath)) {
            return false;
        }
        
        // 检查文件扩展名
        String fileName = solutionFilePath.getFileName().toString();
        if (!fileName.toLowerCase().endsWith(".sln")) {
            return false;
        }
        
        // 简单验证文件内容
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String firstLine = reader.readLine();
            return firstLine != null && firstLine.startsWith("Microsoft Visual Studio Solution File");
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public List<ProjectReferenceInfo> extractProjectReferences(Path solutionFilePath) throws IOException, ParseException {
        List<ProjectReferenceInfo> references = new ArrayList<>();
        
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher matcher = PROJECT_PATTERN.matcher(line);
                if (matcher.matches()) {
                    String projectTypeGuid = matcher.group(1);
                    String projectName = matcher.group(2);
                    String projectPath = matcher.group(3);
                    String projectId = matcher.group(4);
                    
                    // 解析项目路径
                    Path resolvedPath = Paths.get(projectPath);
                    
                    references.add(new ProjectReferenceInfo(
                            projectId,
                            projectName,
                            resolvedPath,
                            projectTypeGuid
                    ));
                }
            }
        }
        
        return references;
    }

    /**
     * 解析全局部分的GlobalSection
     */
    private List<GlobalSection> parseGlobalSections(Path solutionFilePath) throws IOException {
        List<GlobalSection> globalSections = new ArrayList<>();
        boolean inGlobal = false;
        GlobalSection currentSection = null;
        
        try (BufferedReader reader = Files.newBufferedReader(solutionFilePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals("Global")) {
                    inGlobal = true;
                } else if (line.trim().equals("EndGlobal")) {
                    inGlobal = false;
                    break;
                } else if (inGlobal) {
                    // 使用字符串操作替代正则表达式，避免转义问题
                    if (line.trim().startsWith("GlobalSection(") && line.contains(") = ")) {
                        int startIndex = "GlobalSection(".length();
                        int endIndex = line.indexOf(")", startIndex);
                        String name = line.substring(startIndex, endIndex);
                        String type = line.substring(line.indexOf(" = ") + 3).trim();
                        currentSection = new GlobalSection(name, type);
                        globalSections.add(currentSection);
                    } else if (line.trim().equals("EndGlobalSection")) {
                        currentSection = null;
                    } else if (currentSection != null && !line.trim().isEmpty()) {
                        // 添加行到当前section
                        currentSection.addLine(line.trim());
                    }
                }
            }
        }
        
        return globalSections;
    }

    /**
     * 从标记中解析解决方案头部
     */
    private String parseSolutionHeader(List<SolutionFileTokenizer.Token> tokens) throws ParseException {
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                tokens.get(i).getValue().startsWith("Microsoft Visual Studio Solution File")) {
                // 提取版本号
                if (i + 2 < tokens.size() && tokens.get(i + 1).getType() == SolutionFileTokenizer.TokenType.IDENTIFIER &&
                    tokens.get(i + 1).getValue().equals("Version") &&
                    tokens.get(i + 2).getType() == SolutionFileTokenizer.TokenType.IDENTIFIER) {
                    return tokens.get(i + 2).getValue();
                }
            }
        }
        throw new ParseException("Could not parse solution header");
    }

    /**
     * 从标记中解析项目信息
     */
    private List<ProjectReferenceInfo> parseProjects(List<SolutionFileTokenizer.Token> tokens, Path solutionDir) {
        List<ProjectReferenceInfo> projects = new ArrayList<>();
        
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                tokens.get(i).getValue().equals("Project")) {
                // 解析Project(...) = "name", "path", "guid"
                if (i + 10 < tokens.size() && tokens.get(i + 1).getType() == SolutionFileTokenizer.TokenType.LPAREN) {
                    String typeGuid = tokens.get(i + 3).getValue(); // GUID在引号内
                    String projectName = tokens.get(i + 7).getValue();
                    String projectPath = tokens.get(i + 9).getValue();
                    String projectGuid = tokens.get(i + 11).getValue();
                    
                    // 移除GUID中的大括号和引号
                    typeGuid = typeGuid.replace("{", "").replace("}", "").replace("\"", "");
                    projectGuid = projectGuid.replace("{", "").replace("}", "").replace("\"", "");
                    projectName = projectName.replace("\"", "");
                    projectPath = projectPath.replace("\"", "");
                    
                    // 解析项目路径
                    Path resolvedPath = solutionDir.resolve(projectPath).normalize();
                    
                    projects.add(new ProjectReferenceInfo(
                            projectGuid,
                            projectName,
                            resolvedPath,
                            typeGuid
                    ));
                }
            }
        }
        
        return projects;
    }

    /**
     * 从标记中解析全局部分
     */
    private Map<String, Object> parseGlobal(List<SolutionFileTokenizer.Token> tokens) {
        Map<String, Object> globalData = new HashMap<>();
        boolean inGlobal = false;
        Map<String, List<String>> sections = new HashMap<>();
        String currentSection = null;
        
        for (int i = 0; i < tokens.size(); i++) {
            if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                tokens.get(i).getValue().equals("Global")) {
                inGlobal = true;
            } else if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                       tokens.get(i).getValue().equals("EndGlobal")) {
                inGlobal = false;
                globalData.put("sections", sections);
                break;
            } else if (inGlobal) {
                // 解析GlobalSection
                if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                    tokens.get(i).getValue().startsWith("GlobalSection")) {
                    // 简化解析，提取section名称
                    String sectionValue = tokens.get(i).getValue();
                    if (sectionValue.contains("(")) {
                        int startIdx = sectionValue.indexOf("(") + 1;
                        int endIdx = sectionValue.indexOf(")");
                        if (startIdx > 0 && endIdx > startIdx) {
                            currentSection = sectionValue.substring(startIdx, endIdx);
                            sections.put(currentSection, new ArrayList<>());
                        }
                    }
                } else if (tokens.get(i).getType() == SolutionFileTokenizer.TokenType.KEYWORD &&
                           tokens.get(i).getValue().equals("EndGlobalSection")) {
                    currentSection = null;
                } else if (currentSection != null && tokens.get(i).getType() != SolutionFileTokenizer.TokenType.WHITESPACE) {
                    // 添加内容到当前section
                    sections.get(currentSection).add(tokens.get(i).getValue());
                }
            }
        }
        
        return globalData;
    }

    /**
     * 从全局数据中提取配置信息
     */
    private Map<String, SolutionConfiguration> extractConfigurationsFromGlobal(Map<String, Object> globalData) {
        Map<String, SolutionConfiguration> configurations = new HashMap<>();
        
        // 默认配置
        configurations.put("Debug|Any CPU", new SolutionConfiguration("Debug", new HashMap<>()));
        configurations.put("Release|Any CPU", new SolutionConfiguration("Release", new HashMap<>()));
        
        // 从全局数据中提取配置（如果有）
        if (globalData.containsKey("sections")) {
            Map<String, List<String>> sections = (Map<String, List<String>>) globalData.get("sections");
            if (sections.containsKey("SolutionConfigurationPlatforms")) {
                // 处理SolutionConfigurationPlatforms部分
                // 这里简化实现，实际应根据标记内容解析
            }
        }
        
        return configurations;
    }

    /**
     * 根据项目类型GUID确定项目语言
     */
    private ProjectLanguage determineProjectLanguage(String projectTypeGuid) {
        // 移除可能的大括号
        String cleanGuid = projectTypeGuid.replace("{", "").replace("}", "");
        return PROJECT_TYPE_GUIDS.getOrDefault(cleanGuid, ProjectLanguage.CSHARP);
    }

    /**
     * 构建项目模型
     */
    private ProjectModel buildProjectModel(ProjectReferenceInfo reference, ProjectLanguage language, Path projectPath, Path solutionDir) {
        String projectDir = projectPath.getParent() != null ? projectPath.getParent().toString() : solutionDir.toString();
        
        // 创建基本项目模型，详细信息将在后续解析.csproj文件时补充
        ProjectModel projectModel = new ProjectModel(
                reference.getProjectId(),
                reference.getProjectName(),
                projectPath.toString(),
                reference.getProjectTypeGuid()
        );
        
        // 设置其他属性
        projectModel.setDirectory(projectDir);
        projectModel.setAssemblyName(reference.getProjectName());
        projectModel.setLanguage(language);
        
        return projectModel;
    }

    /**
     * 全局部分的Section类
     */
    public static class GlobalSection {
        private final String name;
        private final String type;
        private final List<String> lines;
        
        public GlobalSection(String name, String type) {
            this.name = name;
            this.type = type;
            this.lines = new ArrayList<>();
        }
        
        public void addLine(String line) {
            lines.add(line);
        }
        
        public String getName() {
            return name;
        }
        
        public String getType() {
            return type;
        }
        
        public List<String> getLines() {
            return Collections.unmodifiableList(lines);
        }
    }
}