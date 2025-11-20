package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.*;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 解决方案模型构建器类
 * 负责将解析器提取的数据构建成结构化的解决方案模型
 */
public class SolutionModelBuilder {

    /**
     * 从解析结果构建完整的解决方案模型
     */
    public SolutionModel buildSolutionModel(
            String solutionName,
            String solutionPath,
            String version,
            List<ProjectReferenceInfo> projectReferences,
            Map<String, SolutionConfiguration> configurations,
            List<SlnParser.GlobalSection> globalSections,
            Path solutionDir
    ) {
        // 创建解决方案模型基础对象
        SolutionModel solutionModel = new SolutionModel(
                solutionName,
                solutionPath,
                new HashMap<>(),
                configurations,
                version
        );

        // 提取解决方案配置
        Map<String, String> solutionConfigurations = extractSolutionConfigurations(globalSections);
        
        // 构建项目模型
        Map<String, ProjectModel> projects = buildProjectModels(
                projectReferences,
                solutionDir,
                solutionConfigurations
        );

        // 添加项目到解决方案模型
        projects.forEach((id, project) -> {
            solutionModel.addProject(project);
            solutionModel.addProjectId(id);
        });

        // 处理项目间的依赖关系
        processProjectDependencies(solutionModel, globalSections);

        // 处理解决方案级别的配置映射
        processConfigurationMappings(solutionModel, globalSections);

        return solutionModel;
    }

    /**
     * 从全局节中提取解决方案配置
     */
    private Map<String, String> extractSolutionConfigurations(List<SlnParser.GlobalSection> globalSections) {
        Map<String, String> configurations = new HashMap<>();

        if (globalSections == null) {
            return configurations;
        }

        for (SlnParser.GlobalSection section : globalSections) {
            if ("SolutionConfigurationPlatforms".equals(section.getName())) {
                for (String line : section.getLines()) {
                    // 解析配置行，例如："Debug|Any CPU = Debug|Any CPU"
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String key = line.substring(0, equalsIndex).trim();
                        String value = line.substring(equalsIndex + 1).trim();
                        configurations.put(key, value);
                    }
                }
                break;
            }
        }

        return configurations;
    }

    /**
     * 构建项目模型集合
     */
    private Map<String, ProjectModel> buildProjectModels(
            List<ProjectReferenceInfo> projectReferences,
            Path solutionDir,
            Map<String, String> solutionConfigurations
    ) {
        Map<String, ProjectModel> projects = new HashMap<>();

        for (ProjectReferenceInfo ref : projectReferences) {
            // 解析项目路径
            Path projectPath = solutionDir.resolve(ref.getProjectPath()).normalize();
            String projectDir = projectPath.getParent() != null 
                    ? projectPath.getParent().toString() 
                    : solutionDir.toString();

            // 确定项目语言
            ProjectLanguage language = determineProjectLanguage(ref.getProjectTypeGuid());

            // 创建项目配置
            Map<String, ProjectConfiguration> projectConfigurations = createProjectConfigurations(solutionConfigurations);

            // 构建项目模型 - 使用简化的构造器
            ProjectModel project = new ProjectModel(
                    ref.getProjectId(),
                    ref.getProjectName(),
                    projectPath.toString(),
                    ref.getProjectTypeGuid()
            );
            
            // 设置其他属性
            project.setDirectory(projectDir);
            project.setAssemblyName(ref.getProjectName());
            project.setConfigurations(projectConfigurations);
            project.setLanguage(language);

            projects.put(project.getId(), project);
        }

        return projects;
    }

    /**
     * 创建默认的项目配置
     */
    private Map<String, ProjectConfiguration> createProjectConfigurations(Map<String, String> solutionConfigurations) {
        Map<String, ProjectConfiguration> projectConfigurations = new HashMap<>();

        if (solutionConfigurations.isEmpty()) {
            // 如果没有解决方案配置，添加默认配置
            projectConfigurations.put("Debug|Any CPU", new ProjectConfiguration("Debug", "Any CPU"));
            projectConfigurations.put("Release|Any CPU", new ProjectConfiguration("Release", "Any CPU"));
        } else {
            // 使用解决方案配置
            for (Map.Entry<String, String> entry : solutionConfigurations.entrySet()) {
                String configKey = entry.getKey();
                String[] parts = configKey.split("\\|");
                if (parts.length == 2) {
                    String configName = parts[0];
                    String platform = parts[1];
                    projectConfigurations.put(configKey, new ProjectConfiguration(configName, platform));
                }
            }
        }

        return projectConfigurations;
    }

    /**
     * 处理项目间的依赖关系
     */
    private void processProjectDependencies(SolutionModel solutionModel, List<SlnParser.GlobalSection> globalSections) {
        if (globalSections == null) {
            return;
        }

        // 查找项目依赖信息（通常在NestedProjects或其他相关section中）
        for (SlnParser.GlobalSection section : globalSections) {
            if ("NestedProjects".equals(section.getName())) {
                for (String line : section.getLines()) {
                    // 解析嵌套项目行，例如：{00000000-0000-0000-0000-000000000000} = {11111111-1111-1111-1111-111111111111}
                    int equalsIndex = line.indexOf('=');
                    if (equalsIndex > 0) {
                        String childProjectId = line.substring(0, equalsIndex).trim();
                        String parentProjectId = line.substring(equalsIndex + 1).trim();

                        // 移除可能的大括号
                        childProjectId = childProjectId.replace("{", "").replace("}", "");
                        parentProjectId = parentProjectId.replace("{", "").replace("}", "");

                        // 设置项目依赖关系（简化处理）
                        ProjectModel parentProject = solutionModel.getProjects().get(parentProjectId);
                        if (parentProject != null) {
                            parentProject.addProjectDependency(childProjectId);
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理配置映射关系
     */
    private void processConfigurationMappings(SolutionModel solutionModel, List<SlnParser.GlobalSection> globalSections) {
        if (globalSections == null) {
            return;
        }

        for (SlnParser.GlobalSection section : globalSections) {
            if ("ProjectConfigurationPlatforms".equals(section.getName())) {
                for (String line : section.getLines()) {
                    // 解析配置映射行，例如：{00000000-0000-0000-0000-000000000000}.Debug|Any CPU.ActiveCfg = Debug|Any CPU
                    // 或：{00000000-0000-0000-0000-000000000000}.Debug|Any CPU.Build.0 = Debug|Any CPU
                    
                    // 提取项目ID
                    int firstDotIndex = line.indexOf('.');
                    if (firstDotIndex > 0) {
                        String projectIdPart = line.substring(0, firstDotIndex);
                        String projectId = projectIdPart.replace("{", "").replace("}", "");

                        // 提取配置信息
                        String remaining = line.substring(firstDotIndex + 1);
                        int equalsIndex = remaining.indexOf('=');
                        if (equalsIndex > 0) {
                            String configKeyPart = remaining.substring(0, equalsIndex).trim();
                            String projectConfigValue = remaining.substring(equalsIndex + 1).trim();

                            // 检查是否是ActiveCfg行
                            if (configKeyPart.contains(".ActiveCfg")) {
                                String solutionConfigKey = configKeyPart.replace(".ActiveCfg", "");
                                
                                // 获取项目配置
                                ProjectConfiguration projectConfig = createProjectConfiguration(projectConfigValue);
                                if (projectConfig != null) {
                                    // 更新项目配置映射
                                    ProjectModel project = solutionModel.getProjects().get(projectId);
                                    if (project != null && project.getConfigurations().containsKey(solutionConfigKey)) {
                                        project.getConfigurations().put(solutionConfigKey, projectConfig);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 创建项目配置对象
     */
    private ProjectConfiguration createProjectConfiguration(String configValue) {
        // 配置值格式："Debug|Any CPU"
        String[] parts = configValue.split("\\|");
        if (parts.length == 2) {
            return new ProjectConfiguration(parts[0].trim(), parts[1].trim());
        }
        return null;
    }

    /**
     * 根据项目类型GUID确定项目语言
     */
    private ProjectLanguage determineProjectLanguage(String projectTypeGuid) {
        // 移除可能的大括号
        String cleanGuid = projectTypeGuid.replace("{", "").replace("}", "");
        
        // 项目类型GUID映射到语言
        Map<String, ProjectLanguage> projectTypeGuids = new HashMap<>();
        projectTypeGuids.put("FAE04EC0-301F-11D3-BF4B-00C04F79EFBC", ProjectLanguage.CSHARP);
        projectTypeGuids.put("F184B08F-C81C-45F6-A57F-5ABD9991F28F", ProjectLanguage.CSHARP);
        projectTypeGuids.put("2150E333-8FDC-42A3-9474-1A3956D46DE8", ProjectLanguage.CSHARP);
        projectTypeGuids.put("8BB2217D-0F2D-49D1-97BC-3654ED321F3B", ProjectLanguage.VISUAL_BASIC);
        projectTypeGuids.put("349C5851-65DF-11DA-9384-00065B846F21", ProjectLanguage.WEB);
        projectTypeGuids.put("E53F8FEA-EAE0-44A6-8774-FFD645390401", ProjectLanguage.FSHARP);
        
        return projectTypeGuids.getOrDefault(cleanGuid, ProjectLanguage.CSHARP);
    }

    /**
     * 优化解决方案模型，处理循环依赖，合并配置等
     */
    public SolutionModel optimizeSolutionModel(SolutionModel solutionModel) {
        // 这里可以添加优化逻辑
        // 1. 检测并处理循环依赖
        // 2. 合并重复配置
        // 3. 预计算项目依赖图
        
        return solutionModel;
    }

    /**
     * 验证解决方案模型的完整性
     */
    public boolean validateSolutionModel(SolutionModel solutionModel) {
        // 验证所有必需字段
        if (solutionModel == null || 
            solutionModel.getName() == null || 
            solutionModel.getPath() == null) {
            return false;
        }

        // 验证每个项目
        for (ProjectModel project : solutionModel.getProjects().values()) {
            if (project.getId() == null || 
                project.getName() == null || 
                project.getPath() == null) {
                return false;
            }

            // 验证项目引用的存在性
            for (String refId : project.getProjectDependencies()) {
                if (!solutionModel.getProjects().containsKey(refId)) {
                    return false;
                }
            }
        }

        return true;
    }
}