package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;

/**
 * 监听项目结构变化的事件监听器接口
 */
public interface ProjectListener {
    /**
     * 解决方案打开后触发
     * @param solution 打开的解决方案模型
     */
    void onSolutionOpened(SolutionModel solution);

    /**
     * 解决方案关闭前触发
     * @param solution 将要关闭的解决方案模型
     */
    void onSolutionClosed(SolutionModel solution);

    /**
     * 项目添加到解决方案后触发
     * @param solution 解决方案模型
     * @param project 添加的项目模型
     */
    void onProjectAdded(SolutionModel solution, ProjectModel project);

    /**
     * 项目从解决方案移除前触发
     * @param solution 解决方案模型
     * @param project 将要移除的项目模型
     */
    void onProjectRemoved(SolutionModel solution, ProjectModel project);

    /**
     * 项目刷新后触发
     * @param project 刷新后的项目模型
     */
    void onProjectRefreshed(ProjectModel project);

    /**
     * 解决方案刷新后触发
     * @param solution 刷新后的解决方案模型
     */
    void onSolutionRefreshed(SolutionModel solution);

    /**
     * 项目配置变更时触发
     * @param project 项目模型
     */
    void onProjectConfigurationChanged(ProjectModel project);

    /**
     * 项目依赖变更时触发
     * @param project 项目模型
     */
    void onProjectDependenciesChanged(ProjectModel project);

    /**
     * 解决方案加载开始时触发
     * @param solutionPath 解决方案文件路径
     */
    void onSolutionLoadingStarted(String solutionPath);

    /**
     * 解决方案加载完成后触发
     * @param solution 加载完成的解决方案模型
     */
    void onSolutionLoadingFinished(SolutionModel solution);

    /**
     * 解决方案加载失败时触发
     * @param solutionPath 解决方案文件路径
     * @param error 错误信息
     */
    void onSolutionLoadingFailed(String solutionPath, Exception error);

    /**
     * 项目加载开始时触发
     * @param projectPath 项目文件路径
     */
    void onProjectLoadingStarted(String projectPath);

    /**
     * 项目加载完成后触发
     * @param project 加载完成的项目模型
     */
    void onProjectLoadingFinished(ProjectModel project);

    /**
     * 项目加载失败时触发
     * @param projectPath 项目文件路径
     * @param error 错误信息
     */
    void onProjectLoadingFailed(String projectPath, Exception error);
}