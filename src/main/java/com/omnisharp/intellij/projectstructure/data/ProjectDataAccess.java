package com.omnisharp.intellij.projectstructure.data;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/**
 * 项目数据访问接口
 */
public interface ProjectDataAccess {
    /**
     * 保存解决方案模型到持久化存储
     * @param solutionModel 解决方案模型
     * @throws IOException 如果保存失败
     */
    void saveSolution(SolutionModel solutionModel) throws IOException;

    /**
     * 读取解决方案模型
     * @param solutionPath 解决方案路径
     * @return 解决方案模型，如果不存在则返回空
     */
    Optional<SolutionModel> loadSolution(Path solutionPath);

    /**
     * 保存项目模型到持久化存储
     * @param projectModel 项目模型
     * @throws IOException 如果保存失败
     */
    void saveProject(ProjectModel projectModel) throws IOException;

    /**
     * 读取项目模型
     * @param projectPath 项目路径
     * @return 项目模型，如果不存在则返回空
     */
    Optional<ProjectModel> loadProject(Path projectPath);

    /**
     * 删除解决方案数据
     * @param solutionPath 解决方案路径
     * @return 是否删除成功
     */
    boolean deleteSolution(Path solutionPath);

    /**
     * 删除项目数据
     * @param projectPath 项目路径
     * @return 是否删除成功
     */
    boolean deleteProject(Path projectPath);

    /**
     * 检查解决方案数据是否存在
     * @param solutionPath 解决方案路径
     * @return 是否存在
     */
    boolean existsSolution(Path solutionPath);

    /**
     * 检查项目数据是否存在
     * @param projectPath 项目路径
     * @return 是否存在
     */
    boolean existsProject(Path projectPath);

    /**
     * 关闭数据访问资源
     */
    void close();
}