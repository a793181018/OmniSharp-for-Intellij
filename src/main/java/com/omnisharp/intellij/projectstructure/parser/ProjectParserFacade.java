package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 项目文件解析器的外观接口
 * 提供统一的项目文件解析入口，自动识别并使用适当的解析器
 */
public interface ProjectParserFacade {
    /**
     * 解析项目文件
     * @param projectPath 项目文件路径
     * @return 解析后的项目模型
     * @throws ParseException 解析异常
     */
    ProjectModel parse(Path projectPath) throws ParseException;

    /**
     * 异步解析项目文件
     * @param projectPath 项目文件路径
     * @return 包含解析结果的CompletableFuture
     */
    CompletableFuture<ProjectModel> parseAsync(Path projectPath);
    
    /**
     * 重新解析项目文件（用于文件变更时）
     * @param projectPath 项目文件路径
     * @return 重新解析后的项目模型
     * @throws ParseException 解析异常
     */
    ProjectModel reload(Path projectPath) throws ParseException;
    
    /**
     * 异步重新解析项目文件
     * @param projectPath 项目文件路径
     * @return 包含重新解析结果的CompletableFuture
     */
    CompletableFuture<ProjectModel> reloadAsync(Path projectPath);
    
    /**
     * 检查文件是否为SDK风格的项目文件
     * @param projectPath 项目文件路径
     * @return 是否为SDK风格项目文件
     */
    boolean isSdkStyleProject(Path projectPath);
    
    /**
     * 获取项目文件的最后修改时间戳
     * @param projectPath 项目文件路径
     * @return 时间戳
     */
    long getProjectFileTimestamp(Path projectPath);
    
    /**
     * 关闭解析器资源
     */
    void shutdown();
}