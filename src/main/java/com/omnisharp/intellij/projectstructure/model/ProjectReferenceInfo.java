package com.omnisharp.intellij.projectstructure.model;

/**
 * 表示项目引用的信息
 */
public class ProjectReferenceInfo {
    private final String projectId;
    private final String projectName;
    private final String projectPath;
    private final String projectTypeGuid;

    public ProjectReferenceInfo(String projectId, String projectName, String projectPath, String projectTypeGuid) {
        this.projectId = projectId;
        this.projectName = projectName;
        this.projectPath = projectPath;
        this.projectTypeGuid = projectTypeGuid;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public String getProjectPath() {
        return projectPath;
    }

    public String getProjectTypeGuid() {
        return projectTypeGuid;
    }
}