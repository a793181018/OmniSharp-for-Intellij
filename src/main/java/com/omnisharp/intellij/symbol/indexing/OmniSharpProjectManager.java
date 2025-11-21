package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.List;

/**
 * 项目管理器接口
 * 负责管理项目文件和源代码文件
 */
public interface OmniSharpProjectManager {
    /**
     * 获取项目中的所有源代码文件
     * @param project 项目模型
     * @return 源代码文件路径列表
     */
    List<Path> getSourceFiles(OmniSharpProjectModel project);
    
    /**
     * 获取项目中的指定类型的源代码文件
     * @param project 项目模型
     * @param extensions 文件扩展名列表
     * @return 过滤后的源代码文件路径列表
     */
    List<Path> getSourceFilesByExtensions(OmniSharpProjectModel project, List<String> extensions);
    
    /**
     * 扫描项目目录获取源代码文件
     * @param projectDir 项目目录
     * @return 源代码文件路径列表
     */
    List<Path> scanProjectDirectoryForSourceFiles(Path projectDir);
    
    /**
     * 检查文件是否是源代码文件
     * @param filePath 文件路径
     * @return 如果是源代码文件则返回true
     */
    boolean isSourceFile(Path filePath);
}