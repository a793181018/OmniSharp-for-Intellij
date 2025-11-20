package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.DependencyGraph;
import com.omnisharp.intellij.projectstructure.model.FileReference;
import com.omnisharp.intellij.projectstructure.model.PackageReference;
import java.util.List;
import java.util.Set;

/**
 * 项目依赖管理服务接口
 */
public interface DependencyService {
    /**
     * 获取项目的所有依赖（包括项目引用、包引用和文件引用）
     * @param projectId 项目ID
     * @return 依赖图
     */
    DependencyGraph getProjectDependencies(String projectId);

    /**
     * 获取项目引用的其他项目
     * @param projectId 项目ID
     * @return 被引用的项目ID列表
     */
    List<String> getProjectReferences(String projectId);

    /**
     * 获取项目的包引用
     * @param projectId 项目ID
     * @return 包引用列表
     */
    List<PackageReference> getPackageReferences(String projectId);

    /**
     * 获取项目的文件引用
     * @param projectId 项目ID
     * @return 文件引用列表
     */
    List<FileReference> getFileReferences(String projectId);

    /**
     * 添加项目引用
     * @param sourceProjectId 源项目ID
     * @param targetProjectId 目标项目ID
     * @return 是否添加成功
     */
    boolean addProjectReference(String sourceProjectId, String targetProjectId);

    /**
     * 移除项目引用
     * @param sourceProjectId 源项目ID
     * @param targetProjectId 目标项目ID
     * @return 是否移除成功
     */
    boolean removeProjectReference(String sourceProjectId, String targetProjectId);

    /**
     * 添加包引用
     * @param projectId 项目ID
     * @param packageReference 包引用
     * @return 是否添加成功
     */
    boolean addPackageReference(String projectId, PackageReference packageReference);

    /**
     * 更新包引用版本
     * @param projectId 项目ID
     * @param packageId 包ID
     * @param newVersion 新版本
     * @return 是否更新成功
     */
    boolean updatePackageReference(String projectId, String packageId, String newVersion);

    /**
     * 移除包引用
     * @param projectId 项目ID
     * @param packageId 包ID
     * @return 是否移除成功
     */
    boolean removePackageReference(String projectId, String packageId);

    /**
     * 添加文件引用
     * @param projectId 项目ID
     * @param fileReference 文件引用
     * @return 是否添加成功
     */
    boolean addFileReference(String projectId, FileReference fileReference);

    /**
     * 移除文件引用
     * @param projectId 项目ID
     * @param filePath 文件路径
     * @return 是否移除成功
     */
    boolean removeFileReference(String projectId, String filePath);

    /**
     * 解析依赖树
     * @param projectId 项目ID
     * @return 依赖图
     */
    DependencyGraph resolveDependencyGraph(String projectId);

    /**
     * 检查循环依赖
     * @param projectId 项目ID
     * @return 是否存在循环依赖
     */
    boolean hasCircularDependencies(String projectId);

    /**
     * 获取受影响的项目（当某个项目发生变化时）
     * @param changedProjectId 发生变化的项目ID
     * @return 受影响的项目ID集合
     */
    Set<String> getAffectedProjects(String changedProjectId);

    /**
     * 下载并更新NuGet包
     * @param projectId 项目ID
     * @return 更新状态
     */
    boolean restorePackages(String projectId);

    /**
     * 验证依赖是否有效
     * @param projectId 项目ID
     * @return 无效依赖列表
     */
    List<String> validateDependencies(String projectId);
}