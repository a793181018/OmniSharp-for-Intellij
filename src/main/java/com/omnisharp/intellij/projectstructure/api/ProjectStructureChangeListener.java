package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import org.jetbrains.annotations.NotNull;

/**
 * 项目结构变更监听器接口，用于监听项目结构的各种变更事件
 */
public interface ProjectStructureChangeListener extends ProjectListener {
    
    /**
     * 当解决方案构建开始时触发
     * @param solution 解决方案对象
     */
    default void onSolutionBuildStarted(@NotNull SolutionModel solution) {
        // 默认空实现
    }
    
    /**
     * 当解决方案构建完成时触发
     * @param solution 解决方案对象
     * @param success 是否构建成功
     */
    default void onSolutionBuildFinished(@NotNull SolutionModel solution, boolean success) {
        // 默认空实现
    }
    
    /**
     * 当项目构建开始时触发
     * @param project 项目对象
     */
    default void onProjectBuildStarted(@NotNull ProjectModel project) {
        // 默认空实现
    }
    
    /**
     * 当项目构建完成时触发
     * @param project 项目对象
     * @param success 是否构建成功
     */
    default void onProjectBuildFinished(@NotNull ProjectModel project, boolean success) {
        // 默认空实现
    }
}