package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.List;

/**
 * 项目模型接口
 * 表示一个C#项目
 */
public interface OmniSharpProjectModel {
    /**
     * 获取项目名称
     * @return 项目名称
     */
    String getName();
    
    /**
     * 获取项目文件路径
     * @return 项目文件路径
     */
    Path getFilePath();
    
    /**
     * 获取项目根目录路径
     * @return 项目根目录路径
     */
    Path getProjectDirectory();
    
    /**
     * 获取项目中的引用列表
     * @return 项目引用列表
     */
    List<OmniSharpProjectReference> getReferences();
    
    /**
     * 获取项目的目标框架
     * @return 目标框架名称
     */
    String getTargetFramework();
    
    /**
     * 获取项目的配置信息
     * @return 配置信息映射
     */
    java.util.Map<String, String> getProperties();
    
    /**
     * 判断项目是否是SDK风格的项目
     * @return 如果是SDK风格则返回true
     */
    boolean isSdkStyle();
}