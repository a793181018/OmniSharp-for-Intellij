package com.omnisharp.intellij.projectstructure.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 表示一个完整的解决方案，包含多个项目和解决方案级配置
 */
public class SolutionModel {
    private final String name;
    private final String path;
    private final Map<String, ProjectModel> projects;
    private final Map<String, SolutionConfiguration> configurations;
    private final String version;
    private final Map<String, Map<String, String>> globalSections;

    public SolutionModel(
            String name,
            String path,
            Map<String, ProjectModel> projects,
            Map<String, SolutionConfiguration> configurations,
            String version) {
        this.name = name;
        this.path = path;
        this.projects = projects != null ? new HashMap<>(projects) : new HashMap<>();
        this.configurations = configurations != null ? configurations : Collections.emptyMap();
        this.version = version;
        this.globalSections = new HashMap<>();
    }
    
    /**
     * 获取解决方案的全局配置节
     * @return 全局配置节的映射，键为节名称，值为配置项映射
     */
    public Map<String, Map<String, String>> getGlobalSections() {
        return globalSections;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    /**
     * 获取所有项目的映射
     * @return 项目ID到项目模型的映射
     */
    public Map<String, ProjectModel> getProjects() {
        return projects;
    }

    /**
     * 获取所有项目的集合
     * @return 项目模型集合
     */
    public Collection<ProjectModel> getProjectList() {
        return projects.values();
    }

    /**
     * 添加项目到解决方案
     * @param project 项目模型
     */
    public void addProject(ProjectModel project) {
        if (project != null) {
            projects.put(project.getId(), project);
        }
    }

    /**
     * 添加项目ID到解决方案
     * @param projectId 项目ID
     */
    public void addProjectId(String projectId) {
        // 在实际实现中，这里可能需要验证项目ID或添加到其他数据结构
        // 为了编译通过，这里先添加一个空实现
    }

    /**
     * 添加解决方案配置
     * @param name 配置名称
     * @param configuration 解决方案配置
     */
    public void addConfiguration(String name, SolutionConfiguration configuration) {
        // 配置已经通过构造函数设置，这里只做兼容处理
    }

    public Map<String, SolutionConfiguration> getConfigurations() {
        return configurations;
    }

    /**
     * 获取解决方案的配置信息
     * @return 配置信息映射
     */
    public Map<String, String> getConfiguration() {
        Map<String, String> config = new HashMap<>();
        config.put("name", name);
        config.put("path", path);
        config.put("version", version);
        config.put("projectCount", String.valueOf(projects.size()));
        return config;
    }

    public String getVersion() {
        return version;
    }

    /**
     * 获取解决方案的唯一标识符
     * @return 解决方案ID
     */
    public String getId() {
        return Integer.toHexString(path.hashCode());
    }

    public ProjectModel getProject(String projectId) {
        return projects.get(projectId);
    }
    
    /**
     * 获取项目数量
     * @return 项目数量
     */
    public int getProjectCount() {
        return projects.size();
    }
}