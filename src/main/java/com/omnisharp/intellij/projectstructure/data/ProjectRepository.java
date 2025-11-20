package com.omnisharp.intellij.projectstructure.data;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.parser.SolutionParser;
import com.omnisharp.intellij.projectstructure.parser.ProjectParser;
import com.omnisharp.intellij.projectstructure.parser.ParserFactory;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 项目数据仓库，提供高级数据访问功能
 */
public class ProjectRepository {
    private final ProjectDataAccess dataAccess;
    private final SolutionParser solutionParser;
    private final ProjectParser projectParser;
    private final Map<String, SolutionModel> activeSolutions = new ConcurrentHashMap<>();
    private final Map<String, ProjectModel> activeProjects = new ConcurrentHashMap<>();

    /**
     * 构造函数
     * @param dataAccess 数据访问组件
     */
    public ProjectRepository(ProjectDataAccess dataAccess) {
        this.dataAccess = dataAccess;
        this.solutionParser = ParserFactory.getInstance().getSolutionParser();
        this.projectParser = ParserFactory.getInstance().getProjectParser();
    }

    /**
     * 加载并解析解决方案
     * @param solutionPath 解决方案路径
     * @return 解决方案模型
     * @throws IOException 如果读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    public SolutionModel loadSolution(String solutionPath) throws IOException, SolutionParser.ParseException {
        Path path = Paths.get(solutionPath);
        String pathStr = path.normalize().toString();
        
        // 检查是否已经加载
        if (activeSolutions.containsKey(pathStr)) {
            return activeSolutions.get(pathStr);
        }
        
        // 尝试从缓存加载
        Optional<SolutionModel> cachedSolution = dataAccess.loadSolution(path);
        if (cachedSolution.isPresent()) {
            SolutionModel solution = cachedSolution.get();
            activeSolutions.put(pathStr, solution);
            return solution;
        }
        
        // 解析解决方案文件
        SolutionModel solution = solutionParser.parse(path);
        
        // 加载所有项目
        loadProjectsInSolution(solution);
        
        // 保存到缓存
        dataAccess.saveSolution(solution);
        activeSolutions.put(pathStr, solution);
        
        return solution;
    }

    /**
     * 加载解决方案中的所有项目
     * @param solution 解决方案模型
     * @throws IOException 如果读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    private void loadProjectsInSolution(SolutionModel solution) throws IOException, SolutionParser.ParseException {
        Path solutionDir = Paths.get(solution.getPath()).getParent();
        
        // 这里简化处理，实际应该从解决方案文件中提取项目路径
        // 遍历解决方案目录，查找项目文件
        try {
            java.nio.file.Files.walk(solutionDir, 3) // 最多搜索3层目录
                .filter(file -> file.toString().toLowerCase().endsWith(".csproj") ||
                                file.toString().toLowerCase().endsWith(".vbproj") ||
                                file.toString().toLowerCase().endsWith(".fsproj"))
                .forEach(file -> {
                    try {
                        ProjectModel project = loadProject(file.toString());
                        solution.addProject(project);
                    } catch (IOException | SolutionParser.ParseException e) {
                        // 记录错误但继续处理其他项目
                        System.err.println("Failed to load project: " + file + ", error: " + e.getMessage());
                    }
                });
        } catch (IOException e) {
            throw new IOException("Failed to scan solution directory for projects", e);
        }
    }

    /**
     * 加载并解析项目
     * @param projectPath 项目路径
     * @return 项目模型
     * @throws IOException 如果读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    public ProjectModel loadProject(String projectPath) throws IOException, SolutionParser.ParseException {
        Path path = Paths.get(projectPath);
        String pathStr = path.normalize().toString();
        
        // 检查是否已经加载
        if (activeProjects.containsKey(pathStr)) {
            return activeProjects.get(pathStr);
        }
        
        // 尝试从缓存加载
        Optional<ProjectModel> cachedProject = dataAccess.loadProject(path);
        if (cachedProject.isPresent()) {
            ProjectModel project = cachedProject.get();
            activeProjects.put(pathStr, project);
            return project;
        }
        
        // 解析项目文件
        ProjectModel project = projectParser.parse(path);
        
        // 保存到缓存
        dataAccess.saveProject(project);
        activeProjects.put(pathStr, project);
        
        return project;
    }

    /**
     * 保存解决方案
     * @param solution 解决方案模型
     * @throws IOException 如果保存失败
     */
    public void saveSolution(SolutionModel solution) throws IOException {
        dataAccess.saveSolution(solution);
        activeSolutions.put(Paths.get(solution.getPath()).normalize().toString(), solution);
    }

    /**
     * 保存项目
     * @param project 项目模型
     * @throws IOException 如果保存失败
     */
    public void saveProject(ProjectModel project) throws IOException {
        dataAccess.saveProject(project);
        activeProjects.put(Paths.get(project.getPath()).normalize().toString(), project);
    }

    /**
     * 获取已加载的解决方案
     * @param solutionPath 解决方案路径
     * @return 解决方案模型，如果未加载则返回空
     */
    public Optional<SolutionModel> getSolution(String solutionPath) {
        String pathStr = Paths.get(solutionPath).normalize().toString();
        return Optional.ofNullable(activeSolutions.get(pathStr));
    }

    /**
     * 获取已加载的项目
     * @param projectPath 项目路径
     * @return 项目模型，如果未加载则返回空
     */
    public Optional<ProjectModel> getProject(String projectPath) {
        String pathStr = Paths.get(projectPath).normalize().toString();
        return Optional.ofNullable(activeProjects.get(pathStr));
    }

    /**
     * 获取所有已加载的解决方案
     * @return 解决方案模型集合
     */
    public Collection<SolutionModel> getAllSolutions() {
        return Collections.unmodifiableCollection(activeSolutions.values());
    }

    /**
     * 获取所有已加载的项目
     * @return 项目模型集合
     */
    public Collection<ProjectModel> getAllProjects() {
        return Collections.unmodifiableCollection(activeProjects.values());
    }

    /**
     * 关闭解决方案
     * @param solutionPath 解决方案路径
     * @return 是否成功关闭
     */
    public boolean closeSolution(String solutionPath) {
        String pathStr = Paths.get(solutionPath).normalize().toString();
        SolutionModel removed = activeSolutions.remove(pathStr);
        
        if (removed != null) {
            // 清理相关项目
            removed.getProjects().values().forEach(project -> {
                String projectPath = Paths.get(project.getPath()).normalize().toString();
                activeProjects.remove(projectPath);
            });
            return true;
        }
        return false;
    }

    /**
     * 关闭项目
     * @param projectPath 项目路径
     * @return 是否成功关闭
     */
    public boolean closeProject(String projectPath) {
        String pathStr = Paths.get(projectPath).normalize().toString();
        return activeProjects.remove(pathStr) != null;
    }

    /**
     * 清理所有缓存
     */
    public void clearCache() {
        activeSolutions.clear();
        activeProjects.clear();
        if (dataAccess instanceof FileBasedProjectDataAccess) {
            ((FileBasedProjectDataAccess) dataAccess).clearAllCache();
        }
    }

    /**
     * 刷新解决方案
     * @param solutionPath 解决方案路径
     * @return 更新后的解决方案模型
     * @throws IOException 如果读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    public SolutionModel refreshSolution(String solutionPath) throws IOException, SolutionParser.ParseException {
        closeSolution(solutionPath);
        return loadSolution(solutionPath);
    }

    /**
     * 刷新项目
     * @param projectPath 项目路径
     * @return 更新后的项目模型
     * @throws IOException 如果读取失败
     * @throws SolutionParser.ParseException 如果解析失败
     */
    public ProjectModel refreshProject(String projectPath) throws IOException, SolutionParser.ParseException {
        closeProject(projectPath);
        return loadProject(projectPath);
    }

    /**
     * 遍历所有已加载的解决方案
     * @param action 对每个解决方案执行的操作
     */
    public void forEachSolution(Consumer<SolutionModel> action) {
        activeSolutions.values().forEach(action);
    }

    /**
     * 遍历所有已加载的项目
     * @param action 对每个项目执行的操作
     */
    public void forEachProject(Consumer<ProjectModel> action) {
        activeProjects.values().forEach(action);
    }

    /**
     * 关闭资源
     */
    public void close() {
        activeSolutions.clear();
        activeProjects.clear();
        dataAccess.close();
    }

    /**
     * 检查解决方案是否已加载
     * @param solutionPath 解决方案路径
     * @return 是否已加载
     */
    public boolean isSolutionLoaded(String solutionPath) {
        String pathStr = Paths.get(solutionPath).normalize().toString();
        return activeSolutions.containsKey(pathStr);
    }

    /**
     * 检查项目是否已加载
     * @param projectPath 项目路径
     * @return 是否已加载
     */
    public boolean isProjectLoaded(String projectPath) {
        String pathStr = Paths.get(projectPath).normalize().toString();
        return activeProjects.containsKey(pathStr);
    }
}