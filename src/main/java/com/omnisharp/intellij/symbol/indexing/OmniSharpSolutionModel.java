package com.omnisharp.intellij.symbol.indexing;

import java.util.List;

/**
 * 解决方案模型接口
 * 表示一个C#解决方案
 */
public interface OmniSharpSolutionModel {
    /**
     * 获取解决方案名称
     * @return 解决方案名称
     */
    String getName();
    
    /**
     * 获取解决方案路径
     * @return 解决方案文件路径
     */
    String getPath();
    
    /**
     * 获取解决方案中的所有项目
     * @return 项目模型列表
     */
    List<OmniSharpProjectModel> getProjects();
    
    /**
     * 根据名称获取项目
     * @param projectName 项目名称
     * @return 项目模型，如果不存在则返回null
     */
    OmniSharpProjectModel getProjectByName(String projectName);
    
    /**
     * 获取解决方案的配置信息
     * @return 配置信息映射
     */
    java.util.Map<String, String> getConfiguration();
}