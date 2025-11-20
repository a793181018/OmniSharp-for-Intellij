package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.PackageReference;
import com.omnisharp.intellij.projectstructure.model.FileReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * C#项目文件解析器接口
 */
public interface ProjectParser {
    /**
     * 解析项目文件
     * @param projectFilePath 项目文件路径
     * @return 解析后的项目模型
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    ProjectModel parse(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析项目引用
     * @param projectFilePath 项目文件路径
     * @return 项目引用ID列表
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    List<String> parseProjectReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析NuGet包引用
     * @param projectFilePath 项目文件路径
     * @return 包引用列表
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    List<PackageReference> parsePackageReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析文件引用
     * @param projectFilePath 项目文件路径
     * @return 文件引用列表
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    List<FileReference> parseFileReferences(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析项目配置
     * @param projectFilePath 项目文件路径
     * @return 项目配置映射
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    java.util.Map<String, java.util.Map<String, String>> parseConfigurations(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析项目属性
     * @param projectFilePath 项目文件路径
     * @return 项目属性映射
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    java.util.Map<String, String> parseProjectProperties(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 解析编译文件
     * @param projectFilePath 项目文件路径
     * @return 编译文件路径列表
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    List<String> parseCompileFiles(Path projectFilePath) throws IOException, SolutionParser.ParseException;

    /**
     * 验证项目文件格式是否正确
     * @param projectFilePath 项目文件路径
     * @return 是否有效
     */
    boolean isValidProjectFile(Path projectFilePath);

    /**
     * 解析目标框架信息
     * @param projectFilePath 项目文件路径
     * @return 目标框架列表
     * @throws IOException 如果文件读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    List<String> parseTargetFrameworks(Path projectFilePath) throws IOException, SolutionParser.ParseException;
}