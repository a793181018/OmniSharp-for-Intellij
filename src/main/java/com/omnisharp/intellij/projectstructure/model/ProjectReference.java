package com.omnisharp.intellij.projectstructure.model;

/**
 * 项目引用类
 */
public class ProjectReference {
    private final String projectId;
    private final String name;
    private final String path;

    /**
     * 构造函数，只接受项目ID
     * @param projectId 项目ID
     */
    public ProjectReference(String projectId) {
        this.projectId = projectId;
        this.name = null;
        this.path = null;
    }

    /**
     * 构造函数，接受所有参数
     * @param projectId 项目ID
     * @param name 项目名称
     * @param path 项目路径
     */
    public ProjectReference(String projectId, String name, String path) {
        this.projectId = projectId;
        this.name = name;
        this.path = path;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getName() {
        // 如果name为null，返回projectId作为名称
        return name != null ? name : projectId;
    }

    public String getPath() {
        return path;
    }
}