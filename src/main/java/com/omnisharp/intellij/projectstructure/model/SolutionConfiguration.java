package com.omnisharp.intellij.projectstructure.model;

import java.util.Map;

/**
 * 解决方案配置信息
 */
public class SolutionConfiguration {
    private final String name;
    private final Map<String, String> projectConfigurations; // 项目ID -> 项目配置名称

    public SolutionConfiguration(String name, Map<String, String> projectConfigurations) {
        this.name = name;
        this.projectConfigurations = projectConfigurations;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getProjectConfigurations() {
        return projectConfigurations;
    }

    public String getProjectConfiguration(String projectId) {
        return projectConfigurations.get(projectId);
    }

    /**
     * 添加项目配置到解决方案配置
     * @param projectId 项目ID
     * @param configName 配置名称
     * @param platform 平台名称
     */
    public void addProjectConfiguration(String projectId, String configName, String platform) {
        if (projectConfigurations != null) {
            projectConfigurations.put(projectId, configName + "|" + platform);
        }
    }
}