package com.omnisharp.intellij.projectstructure.navigation.visualization;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.omnisharp.intellij.projectstructure.api.DependencyService;
import com.intellij.openapi.components.ServiceManager;
import java.util.Collection;
import java.util.Optional;
import com.omnisharp.intellij.projectstructure.model.DependencyGraph;
import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import com.omnisharp.intellij.projectstructure.navigation.NavigationNode;
import com.omnisharp.intellij.projectstructure.navigation.ProjectNavigator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 可视化服务，提供项目结构和依赖关系的可视化功能
 */
public class VisualizationService implements Disposable {
    private final Project project;
    private final ProjectNavigator projectNavigator;
    private final DependencyService dependencyService;
    private DependencyGraphVisualizer dependencyGraphVisualizer;
    private ProjectStructureVisualizer projectStructureVisualizer;
    
    private static final VisualizationService INSTANCE = new VisualizationService();
    
    private VisualizationService() {
        this.project = null;
        this.projectNavigator = null;
        this.dependencyService = null;
    }
    
    public VisualizationService(@NotNull Project project) {
        this.project = project;
        this.projectNavigator = new ProjectNavigator(project);
        this.dependencyService = ServiceManager.getService(project, DependencyService.class);
        initializeVisualizers();
    }
    
    private void initializeVisualizers() {
        this.dependencyGraphVisualizer = new DependencyGraphVisualizer(project);
        this.projectStructureVisualizer = new ProjectStructureVisualizer(projectNavigator);
    }
    
    /**
     * 获取可视化服务实例
     * @param project IntelliJ项目实例
     * @return 可视化服务实例
     */
    public static VisualizationService getInstance(@NotNull Project project) {
        return new VisualizationService(project);
    }
    
    /**
     * 获取依赖关系图可视化器
     * @return 依赖关系图可视化器
     */
    @NotNull
    public DependencyGraphVisualizer getDependencyGraphVisualizer() {
        return dependencyGraphVisualizer;
    }
    
    /**
     * 获取项目结构可视化器
     * @return 项目结构可视化器
     */
    @NotNull
    public ProjectStructureVisualizer getProjectStructureVisualizer() {
        return projectStructureVisualizer;
    }
    
    /**
     * 可视化整个解决方案的依赖关系
     */
    public void visualizeSolutionDependencies() {
        // 此功能当前不可用，因为DependencyService中没有提供构建整个解决方案依赖图的方法
        projectNavigator.getProjectManagerService().getCurrentSolution().ifPresent(solutionModel -> {
            // 可以考虑从solutionModel获取所有项目，然后使用getProjectDependencies方法分别获取
            // 但当前依赖服务接口不支持直接构建解决方案级别的依赖图
            System.out.println("Solution dependency visualization not supported by current DependencyService API");
        });
    }
    
    /**
     * 可视化指定项目的依赖关系
     * @param projectName 项目名称
     */
    public void visualizeProjectDependencies(@NotNull String projectName) {
        ProjectModel projectModel = findProjectByName(projectName);
        if (projectModel != null) {
            // 使用getProjectDependencies方法，传入项目ID
            DependencyGraph dependencyGraph = dependencyService.getProjectDependencies(projectModel.getId());
            dependencyGraphVisualizer.visualize(dependencyGraph);
        }
    }
    
    /**
     * 可视化项目结构树
     */
    public void visualizeProjectStructure() {
        projectStructureVisualizer.visualize();
    }
    
    /**
     * 可视化指定节点的子树
     * @param node 导航节点
     */
    public void visualizeNodeSubtree(@NotNull NavigationNode node) {
        projectStructureVisualizer.visualizeSubtree(node);
    }
    
    /**
     * 导出依赖关系图为PNG
     * @param filePath 文件路径
     */
    public void exportDependencyGraphAsPNG(@NotNull String filePath) {
        dependencyGraphVisualizer.exportAsPNG(filePath);
    }
    
    /**
     * 导出项目结构图为PNG
     * @param filePath 文件路径
     */
    public void exportProjectStructureAsPNG(@NotNull String filePath) {
        projectStructureVisualizer.exportAsPNG(filePath);
    }
    
    /**
     * 导出依赖关系图为SVG
     * @param filePath 文件路径
     */
    public void exportDependencyGraphAsSVG(@NotNull String filePath) {
        dependencyGraphVisualizer.exportAsSVG(filePath);
    }
    
    /**
     * 导出项目结构图为SVG
     * @param filePath 文件路径
     */
    public void exportProjectStructureAsSVG(@NotNull String filePath) {
        projectStructureVisualizer.exportAsSVG(filePath);
    }
    
    /**
     * 设置可视化样式
     * @param style 可视化样式
     */
    public void setVisualizationStyle(@NotNull VisualizationStyle style) {
        dependencyGraphVisualizer.setStyle(style);
        projectStructureVisualizer.setStyle(style);
    }
    
    @Nullable
    private ProjectModel findProjectByName(@NotNull String projectName) {
        Optional<SolutionModel> solutionModelOpt = projectNavigator.getProjectManagerService().getCurrentSolution();
        if (solutionModelOpt.isPresent()) {
            SolutionModel solutionModel = solutionModelOpt.get();
            Collection<ProjectModel> projectList = solutionModel.getProjectList();
            if (projectList != null) {
                for (ProjectModel project : projectList) {
                    if (project.getName().equals(projectName)) {
                        return project;
                    }
                }
            }
        }
        return null;
    }
    
    @Override
    public void dispose() {
        if (dependencyGraphVisualizer != null) {
            dependencyGraphVisualizer.dispose();
        }
        if (projectStructureVisualizer != null) {
            projectStructureVisualizer.dispose();
        }
    }
    
    /**
     * 可视化样式枚举
     */
    public enum VisualizationStyle {
        DEFAULT,
        MINIMAL,
        DETAILED,
        COLORFUL,
        HIGH_CONTRAST
    }
}