package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.model.SolutionConfiguration;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 解决方案文件(.sln)解析器实现
 */
public class SlnParser implements SolutionParser {
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

    @Override
    public SolutionModel parse(Path solutionFilePath) throws IOException, ParseException {
        if (!isValidSolutionFile(solutionFilePath)) {
            throw new ParseException("Invalid solution file: " + solutionFilePath);
        }

        String solutionName = solutionFilePath.getFileName().toString().replace(".sln", "");
        String version = parseSolutionVersion(solutionFilePath);
        Map<String, SolutionConfiguration> configurations = parseConfigurations(solutionFilePath);
        List<ProjectReferenceInfo> projectReferences = extractProjectReferences(solutionFilePath);
        
        // 创建解决方案模型
        SolutionModel solutionModel = new SolutionModel(solutionName, solutionFilePath.toString(), new HashMap<>(), configurations, version);
        
        // 添加配置
        configurations.forEach(solutionModel::addConfiguration);
        
        // 提取项目路径信息
        for (ProjectReferenceInfo refInfo : projectReferences) {
            // 解析项目文件路径（相对于解决方案目录）
            Path projectPath = solutionFilePath.getParent().resolve(refInfo.getProjectPath()).normalize();
            
            // 将项目信息添加到解决方案模型（实际项目解析由ProjectManagerService负责）
            solutionModel.addProjectId(refInfo.getProjectId());
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
}