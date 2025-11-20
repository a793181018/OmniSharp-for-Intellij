package com.omnisharp.intellij.projectstructure.service;

import com.omnisharp.intellij.projectstructure.api.ProjectListener;
import com.omnisharp.intellij.projectstructure.api.ProjectManagerService;
import com.omnisharp.intellij.projectstructure.model.ProjectLanguage;
import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 项目管理服务的实现类
 */
public class ProjectManagerServiceImpl implements ProjectManagerService {
    private final Project intellijProject;
    private final List<ProjectListener> listeners = new CopyOnWriteArrayList<>();
    private SolutionModel currentSolution;
    private final Map<String, ProjectModel> projectCache = new HashMap<>();

    public ProjectManagerServiceImpl(Project intellijProject) {
        this.intellijProject = intellijProject;
    }

    @Override
    public synchronized SolutionModel openSolution(String solutionFilePath) {
        notifySolutionLoadingStarted(solutionFilePath);
        try {
            // 这里应该调用解析器来解析解决方案文件
            // 为了演示，我们创建一个简单的SolutionModel
            SolutionModel solution = new SolutionModel(
                    "Solution-" + UUID.randomUUID().toString(),
                    solutionFilePath,
                    new HashMap<>(), // projects should be Map<String, ProjectModel>
                    new HashMap<>(),
                    "1.0"
            );
            
            this.currentSolution = solution;
            notifySolutionOpened(solution);
            notifySolutionLoadingFinished(solution);
            return solution;
        } catch (Exception e) {
            notifySolutionLoadingFailed(solutionFilePath, e);
            throw new RuntimeException("Failed to open solution: " + solutionFilePath, e);
        }
    }

    @Override
    public synchronized void closeSolution() {
        if (currentSolution != null) {
            notifySolutionClosed(currentSolution);
            currentSolution = null;
            projectCache.clear();
        }
    }

    @Override
    public Optional<SolutionModel> getCurrentSolution() {
        return Optional.ofNullable(currentSolution);
    }

    @Override
    public synchronized ProjectModel openProject(String projectFilePath) {
        notifyProjectLoadingStarted(projectFilePath);
        try {
            // 这里应该调用解析器来解析项目文件
            // 为了演示，我们创建一个简单的ProjectModel
            String projectId = "Project-" + UUID.randomUUID().toString();
            ProjectModel project = new ProjectModel(
                    projectId,
                    projectFilePath.substring(projectFilePath.lastIndexOf('/') + 1).replace(".csproj", ""),
                    projectFilePath,
                    projectFilePath.substring(0, projectFilePath.lastIndexOf('/')), // directory
                    "bin/Debug/netstandard2.0", // outputPath
                    projectFilePath.substring(projectFilePath.lastIndexOf('/') + 1).replace(".csproj", ""), // assemblyName
                    "netstandard2.0", // targetFramework
                    new HashMap<>(), // configurations
                    Collections.emptyList(), // projectReferences
                    Collections.emptyList(), // packageReferences
                    Collections.emptyList(), // fileReferences
                    Collections.emptyList(), // projectFiles
                    ProjectLanguage.CSHARP // language
            );
            
            projectCache.put(projectId, project);
            
            // 如果有当前解决方案，将项目添加到解决方案中
            if (currentSolution != null) {
                currentSolution.addProject(project);
                notifyProjectAdded(currentSolution, project);
            }
            
            notifyProjectLoadingFinished(project);
            return project;
        } catch (Exception e) {
            notifyProjectLoadingFailed(projectFilePath, e);
            throw new RuntimeException("Failed to open project: " + projectFilePath, e);
        }
    }

    @Override
    public List<ProjectModel> getProjects(String solutionId) {
        if (currentSolution != null && currentSolution.getId().equals(solutionId)) {
            return new ArrayList<>(currentSolution.getProjects());
        }
        return Collections.emptyList();
    }

    @Override
    public Optional<ProjectModel> getProjectById(String projectId) {
        return Optional.ofNullable(projectCache.get(projectId));
    }

    @Override
    public synchronized void refreshSolution(String solutionId) {
        if (currentSolution != null && currentSolution.getId().equals(solutionId)) {
            // 刷新解决方案的逻辑
            notifySolutionRefreshed(currentSolution);
        }
    }

    @Override
    public synchronized void refreshProject(String projectId) {
        ProjectModel project = projectCache.get(projectId);
        if (project != null) {
            // 刷新项目的逻辑
            notifyProjectRefreshed(project);
        }
    }

    @Override
    public synchronized void saveSolution(String solutionId) {
        // 保存解决方案的逻辑
        if (currentSolution != null && currentSolution.getId().equals(solutionId)) {
            // 实际实现中需要将解决方案的状态写入文件
        }
    }

    @Override
    public synchronized void saveProject(String projectId) {
        // 保存项目的逻辑
        ProjectModel project = projectCache.get(projectId);
        if (project != null) {
            // 实际实现中需要将项目的状态写入文件
        }
    }

    @Override
    public void addProjectListener(ProjectListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeProjectListener(ProjectListener listener) {
        listeners.remove(listener);
    }

    @Override
    public Project getIntelliJProject() {
        return intellijProject;
    }

    // 通知监听器的辅助方法
    private void notifySolutionOpened(SolutionModel solution) {
        listeners.forEach(listener -> listener.onSolutionOpened(solution));
    }

    private void notifySolutionClosed(SolutionModel solution) {
        listeners.forEach(listener -> listener.onSolutionClosed(solution));
    }

    private void notifyProjectAdded(SolutionModel solution, ProjectModel project) {
        listeners.forEach(listener -> listener.onProjectAdded(solution, project));
    }

    private void notifyProjectRemoved(SolutionModel solution, ProjectModel project) {
        listeners.forEach(listener -> listener.onProjectRemoved(solution, project));
    }

    private void notifyProjectRefreshed(ProjectModel project) {
        listeners.forEach(listener -> listener.onProjectRefreshed(project));
    }

    private void notifySolutionRefreshed(SolutionModel solution) {
        listeners.forEach(listener -> listener.onSolutionRefreshed(solution));
    }

    private void notifySolutionLoadingStarted(String solutionPath) {
        listeners.forEach(listener -> listener.onSolutionLoadingStarted(solutionPath));
    }

    private void notifySolutionLoadingFinished(SolutionModel solution) {
        listeners.forEach(listener -> listener.onSolutionLoadingFinished(solution));
    }

    private void notifySolutionLoadingFailed(String solutionPath, Exception error) {
        listeners.forEach(listener -> listener.onSolutionLoadingFailed(solutionPath, error));
    }

    private void notifyProjectLoadingStarted(String projectPath) {
        listeners.forEach(listener -> listener.onProjectLoadingStarted(projectPath));
    }

    private void notifyProjectLoadingFinished(ProjectModel project) {
        listeners.forEach(listener -> listener.onProjectLoadingFinished(project));
    }

    private void notifyProjectLoadingFailed(String projectPath, Exception error) {
        listeners.forEach(listener -> listener.onProjectLoadingFailed(projectPath, error));
    }
}