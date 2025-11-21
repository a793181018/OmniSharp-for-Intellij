package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.List;

/**
 * 文件解析器接口
 * 负责解析源代码文件并提取符号信息
 */
public interface OmniSharpFileParser {
    /**
     * 解析单个文件并提取符号
     * @param filePath 文件路径
     * @param projectName 项目名称
     * @return 从文件中提取的符号列表
     */
    List<OmniSharpSymbol> parseFile(Path filePath, String projectName);
    
    /**
     * 批量解析多个文件并提取符号
     * @param filePaths 文件路径列表
     * @param projectName 项目名称
     * @return 从所有文件中提取的符号列表
     */
    List<OmniSharpSymbol> parseFiles(List<Path> filePaths, String projectName);
    
    /**
     * 检查文件是否支持解析
     * @param filePath 文件路径
     * @return 如果支持解析则返回true
     */
    boolean supports(Path filePath);
    
    /**
     * 获取支持的文件扩展名
     * @return 文件扩展名列表
     */
    List<String> getSupportedExtensions();
}