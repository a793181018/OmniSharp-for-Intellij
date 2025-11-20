package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Optional;

/**
 * 项目管理的核心服务接口
 */
public interface ProjectManagerService {
    /**
     * 打开解决方案文件
     * @param solutionFilePath 解决方案文件路径
     * @return 打开的解决方案模型
     */
    SolutionModel openSolution(String solutionFilePath);

    /**
     * 关闭当前解决方案
     */
    void closeSolution();

    /**
     * 获取当前打开的解决方案
     * @return 当前解决方案模型，如果没有打开的解决方案则返回空
     */
    Optional<SolutionModel> getCurrentSolution();

    /**
     * 打开项目文件
     * @param projectFilePath 项目文件路径
     * @return 打开的项目模型
     */
    ProjectModel openProject(String projectFilePath);

    /**
     * 获取解决方案中的所有项目
     * @param solutionId 解决方案ID
     * @return 项目列表
     */
    List<ProjectModel> getProjects(String solutionId);

    /**
     * 根据ID获取项目
     * @param projectId 项目ID
     * @return 项目模型，如果找不到则返回空
     */
    Optional<ProjectModel> getProjectById(String projectId);

    /**
     * 刷新解决方案
     * @param solutionId 解决方案ID
     */
    void refreshSolution(String solutionId);

    /**
     * 刷新项目
     * @param projectId 项目ID
     */
    void refreshProject(String projectId);

    /**
     * 保存解决方案
     * @param solutionId 解决方案ID
     */
    void saveSolution(String solutionId);

    /**
     * 保存项目
     * @param projectId 项目ID
     */
    void saveProject(String projectId);

    /**
     * 添加项目监听器
     * @param listener 项目监听器
     */
    void addProjectListener(ProjectListener listener);

    /**
     * 移除项目监听器
     * @param listener 项目监听器
     */
    void removeProjectListener(ProjectListener listener);

    /**
     * 获取IntelliJ项目对象
     * @return IntelliJ项目对象
     */
    Project getIntelliJProject();
}