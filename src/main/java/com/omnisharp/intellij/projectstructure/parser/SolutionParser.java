package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.model.SolutionConfiguration;
import java.io.IOException;
import java.nio.file.Path;

/**
 * 解决方案文件解析器接口
 */
public interface SolutionParser {
    /**
     * 解析解决方案文件
     * @param solutionFilePath 解决方案文件路径
     * @return 解析后的解决方案模型
     * @throws IOException 如果文件读取失败
     * @throws ParseException 如果解析失败
     */
    SolutionModel parse(Path solutionFilePath) throws IOException, ParseException;

    /**
     * 解析解决方案文件头部信息
     * @param solutionFilePath 解决方案文件路径
     * @return 解决方案版本信息
     * @throws IOException 如果文件读取失败
     * @throws ParseException 如果解析失败
     */
    String parseSolutionVersion(Path solutionFilePath) throws IOException, ParseException;

    /**
     * 解析解决方案配置信息
     * @param solutionFilePath 解决方案文件路径
     * @return 解决方案配置映射
     * @throws IOException 如果文件读取失败
     * @throws ParseException 如果解析失败
     */
    java.util.Map<String, SolutionConfiguration> parseConfigurations(Path solutionFilePath) throws IOException, ParseException;

    /**
     * 验证解决方案文件格式是否正确
     * @param solutionFilePath 解决方案文件路径
     * @return 是否有效
     */
    boolean isValidSolutionFile(Path solutionFilePath);

    /**
     * 提取解决方案中的项目引用信息
     * @param solutionFilePath 解决方案文件路径
     * @return 项目引用信息列表
     * @throws IOException 如果文件读取失败
     * @throws ParseException 如果解析失败
     */
    java.util.List<ProjectReferenceInfo> extractProjectReferences(Path solutionFilePath) throws IOException, ParseException;

    /**
     * 项目引用信息
     */
    class ProjectReferenceInfo {
        private final String projectId;
        private final String projectName;
        private final Path projectPath;
        private final String projectTypeGuid;

        public ProjectReferenceInfo(String projectId, String projectName, Path projectPath, String projectTypeGuid) {
            this.projectId = projectId;
            this.projectName = projectName;
            this.projectPath = projectPath;
            this.projectTypeGuid = projectTypeGuid;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getProjectName() {
            return projectName;
        }

        public Path getProjectPath() {
            return projectPath;
        }

        public String getProjectTypeGuid() {
            return projectTypeGuid;
        }

        @Override
        public String toString() {
            return "ProjectReferenceInfo{" +
                    "projectId='" + projectId + '\'' +
                    ", projectName='" + projectName + '\'' +
                    ", projectPath=" + projectPath +
                    ", projectTypeGuid='" + projectTypeGuid + '\'' +
                    '}';
        }
    }

    /**
     * 解析异常
     */
    class ParseException extends Exception {
        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}