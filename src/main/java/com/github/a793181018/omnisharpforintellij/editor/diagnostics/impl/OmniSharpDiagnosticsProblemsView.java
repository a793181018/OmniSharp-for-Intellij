package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.DoubleClickListener;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OmniSharp诊断问题视图
 * 在问题视图中显示诊断结果
 */
public class OmniSharpDiagnosticsProblemsView implements Disposable {
    private static final Logger LOG = Logger.getInstance(OmniSharpDiagnosticsProblemsView.class);
    
    private final Project project;
    private final OmniSharpDiagnosticsService diagnosticsService;
    private final Map<VirtualFile, List<Diagnostic>> fileDiagnostics = new HashMap<>();
    
    // UI组件
    private JPanel mainPanel;
    private JTree diagnosticsTree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode rootNode;
    private JLabel statusLabel;
    private JComboBox<String> severityFilter;
    private JCheckBox showErrorsCheckBox;
    private JCheckBox showWarningsCheckBox;
    private JCheckBox showInfoCheckBox;
    
    // 过滤选项
    private boolean showErrors = true;
    private boolean showWarnings = true;
    private boolean showInfo = false;
    
    public OmniSharpDiagnosticsProblemsView(@NotNull Project project) {
        this.project = project;
        this.diagnosticsService = project.getService(OmniSharpDiagnosticsService.class);
        
        initializeUI();
        setupListeners();
        
        LOG.info("OmniSharpDiagnosticsProblemsView initialized for project: " + project.getName());
    }
    
    /**
     * 初始化UI
     */
    private void initializeUI() {
        mainPanel = new JPanel(new BorderLayout());
        
        // 创建工具栏
        JPanel toolbar = createToolbar();
        mainPanel.add(toolbar, BorderLayout.NORTH);
        
        // 创建诊断树
        createDiagnosticsTree();
        JScrollPane treeScrollPane = new JBScrollPane(diagnosticsTree);
        mainPanel.add(treeScrollPane, BorderLayout.CENTER);
        
        // 创建状态栏
        JPanel statusBar = createStatusBar();
        mainPanel.add(statusBar, BorderLayout.SOUTH);
        
        // 设置首选大小
        mainPanel.setPreferredSize(new Dimension(400, 300));
    }
    
    /**
     * 创建工具栏
     */
    @NotNull
    private JPanel createToolbar() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        toolbar.setBorder(JBUI.Borders.empty(5));
        
        // 严重级别过滤器
        toolbar.add(new JLabel("Filter:"));
        severityFilter = new JComboBox<>(new String[]{"All", "Errors", "Warnings", "Info"});
        severityFilter.addActionListener(e -> updateView());
        toolbar.add(severityFilter);
        
        toolbar.add(Box.createHorizontalStrut(10));
        
        // 复选框
        showErrorsCheckBox = new JCheckBox("Errors", showErrors);
        showErrorsCheckBox.addActionListener(e -> {
            showErrors = showErrorsCheckBox.isSelected();
            updateView();
        });
        toolbar.add(showErrorsCheckBox);
        
        showWarningsCheckBox = new JCheckBox("Warnings", showWarnings);
        showWarningsCheckBox.addActionListener(e -> {
            showWarnings = showWarningsCheckBox.isSelected();
            updateView();
        });
        toolbar.add(showWarningsCheckBox);
        
        showInfoCheckBox = new JCheckBox("Info", showInfo);
        showInfoCheckBox.addActionListener(e -> {
            showInfo = showInfoCheckBox.isSelected();
            updateView();
        });
        toolbar.add(showInfoCheckBox);
        
        // 刷新按钮
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshDiagnostics());
        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(refreshButton);
        
        return toolbar;
    }
    
    /**
     * 创建诊断树
     */
    private void createDiagnosticsTree() {
        rootNode = new DefaultMutableTreeNode("Diagnostics");
        treeModel = new DefaultTreeModel(rootNode);
        diagnosticsTree = new Tree(treeModel);
        
        // 设置树的外观
        diagnosticsTree.setRootVisible(false);
        diagnosticsTree.setShowsRootHandles(true);
        diagnosticsTree.setCellRenderer(new DiagnosticTreeCellRenderer());
        
        // 添加双击监听器
        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(@NotNull MouseEvent event) {
                navigateToSelectedDiagnostic();
                return true;
            }
        }.installOn(diagnosticsTree);
    }
    
    /**
     * 创建状态栏
     */
    @NotNull
    private JPanel createStatusBar() {
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBorder(JBUI.Borders.empty(2, 5));
        
        statusLabel = new JLabel("Ready");
        statusBar.add(statusLabel);
        
        return statusBar;
    }
    
    /**
     * 设置监听器
     */
    private void setupListeners() {
        // 监听诊断更新 - 为所有文件设置监听器
        diagnosticsService.addGlobalDiagnosticListener(new OmniSharpDiagnosticsService.DiagnosticListener() {
            @Override
            public void onDiagnosticsUpdated(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
                updateFileDiagnostics(file, diagnostics);
            }
        });
        
        // 监听文件打开/关闭事件
        project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
            @Override
            public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (isCSharpFile(file)) {
                    refreshFileDiagnostics(file);
                }
            }
            
            @Override
            public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                if (isCSharpFile(file)) {
                    removeFileDiagnostics(file);
                }
            }
        });
    }
    
    /**
     * 更新文件诊断
     */
    private void updateFileDiagnostics(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (diagnostics.isEmpty()) {
                fileDiagnostics.remove(file);
            } else {
                fileDiagnostics.put(file, new ArrayList<>(diagnostics));
            }
            updateView();
        });
    }
    
    /**
     * 刷新文件诊断
     */
    private void refreshFileDiagnostics(@NotNull VirtualFile file) {
        diagnosticsService.updateDiagnostics(file)
            .whenComplete((diagnostics, throwable) -> {
                if (throwable != null) {
                    LOG.error("Failed to refresh diagnostics for file: " + file.getPath(), throwable);
                }
            });
    }
    
    /**
     * 移除文件诊断
     */
    private void removeFileDiagnostics(@NotNull VirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            fileDiagnostics.remove(file);
            updateView();
        });
    }
    
    /**
     * 刷新所有诊断
     */
    private void refreshDiagnostics() {
        statusLabel.setText("Refreshing...");
        
        // 获取所有打开的C#文件
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
        
        List<VirtualFile> csharpFiles = Arrays.stream(openFiles)
            .filter(this::isCSharpFile)
            .collect(Collectors.toList());
        
        if (csharpFiles.isEmpty()) {
            statusLabel.setText("No C# files open");
            return;
        }
        
        // 批量刷新诊断
        diagnosticsService.updateDiagnosticsBatch(csharpFiles)
            .whenComplete((results, throwable) -> {
                ApplicationManager.getApplication().invokeLater(() -> {
                    if (throwable != null) {
                        statusLabel.setText("Failed to refresh diagnostics");
                        LOG.error("Failed to refresh diagnostics", throwable);
                    } else {
                        statusLabel.setText("Diagnostics refreshed");
                        updateView();
                    }
                });
            });
    }
    
    /**
     * 更新视图
     */
    private void updateView() {
        ApplicationManager.getApplication().invokeLater(() -> {
            rootNode.removeAllChildren();
            
            int totalDiagnostics = 0;
            int errorCount = 0;
            int warningCount = 0;
            int infoCount = 0;
            
            // 按文件分组诊断
            for (Map.Entry<VirtualFile, List<Diagnostic>> entry : fileDiagnostics.entrySet()) {
                VirtualFile file = entry.getKey();
                List<Diagnostic> diagnostics = entry.getValue();
                
                // 过滤诊断
                List<Diagnostic> filteredDiagnostics = filterDiagnostics(diagnostics);
                
                if (!filteredDiagnostics.isEmpty()) {
                    // 创建文件节点
                    DefaultMutableTreeNode fileNode = new DefaultMutableTreeNode(new FileDiagnosticNode(file, filteredDiagnostics));
                    rootNode.add(fileNode);
                    
                    // 添加诊断节点
                    for (Diagnostic diagnostic : filteredDiagnostics) {
                        DefaultMutableTreeNode diagnosticNode = new DefaultMutableTreeNode(new DiagnosticNode(diagnostic));
                        fileNode.add(diagnosticNode);
                        
                        totalDiagnostics++;
                        switch (diagnostic.getSeverity()) {
                            case ERROR:
                                errorCount++;
                                break;
                            case WARNING:
                                warningCount++;
                                break;
                            case INFO:
                                infoCount++;
                                break;
                        }
                    }
                }
            }
            
            // 更新树模型
            treeModel.reload();
            
            // 更新状态标签
            updateStatusLabel(totalDiagnostics, errorCount, warningCount, infoCount);
        });
    }
    
    /**
     * 过滤诊断
     */
    @NotNull
    private List<Diagnostic> filterDiagnostics(@NotNull List<Diagnostic> diagnostics) {
        return diagnostics.stream()
            .filter(diagnostic -> {
                switch (diagnostic.getSeverity()) {
                    case ERROR:
                        return showErrors;
                    case WARNING:
                        return showWarnings;
                    case INFO:
                        return showInfo;
                    default:
                        return false;
                }
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 更新状态标签
     */
    private void updateStatusLabel(int total, int errors, int warnings, int info) {
        StringBuilder status = new StringBuilder();
        status.append("Total: ").append(total);
        
        if (errors > 0) {
            status.append(", Errors: ").append(errors);
        }
        if (warnings > 0) {
            status.append(", Warnings: ").append(warnings);
        }
        if (info > 0) {
            status.append(", Info: ").append(info);
        }
        
        statusLabel.setText(status.toString());
    }
    
    /**
     * 导航到选中的诊断
     */
    private void navigateToSelectedDiagnostic() {
        Object selectedNode = diagnosticsTree.getLastSelectedPathComponent();
        if (selectedNode instanceof DefaultMutableTreeNode) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectedNode;
            Object userObject = treeNode.getUserObject();
            
            if (userObject instanceof DiagnosticNode) {
                DiagnosticNode diagnosticNode = (DiagnosticNode) userObject;
                navigateToDiagnostic(diagnosticNode.diagnostic);
            }
        }
    }
    
    /**
     * 导航到诊断位置
     */
    private void navigateToDiagnostic(@NotNull Diagnostic diagnostic) {
        // 这里可以实现导航到诊断位置的逻辑
        // 例如：打开文件并定位到诊断位置
        LOG.info("Navigating to diagnostic: " + diagnostic.getId());
    }
    
    /**
     * 检查是否为C#文件
     */
    private boolean isCSharpFile(@NotNull VirtualFile file) {
        return "cs".equalsIgnoreCase(file.getExtension());
    }
    
    /**
     * 获取主面板
     */
    @NotNull
    public JPanel getMainPanel() {
        return mainPanel;
    }
    
    /**
     * 获取文件诊断
     */
    @NotNull
    public Map<VirtualFile, List<Diagnostic>> getFileDiagnostics() {
        return new HashMap<>(fileDiagnostics);
    }
    
    /**
     * 清除所有诊断
     */
    public void clearAllDiagnostics() {
        ApplicationManager.getApplication().invokeLater(() -> {
            fileDiagnostics.clear();
            updateView();
        });
    }
    
    @Override
    public void dispose() {
        LOG.info("Disposing OmniSharpDiagnosticsProblemsView");
        
        // 清除数据
        fileDiagnostics.clear();
        
        // 清除UI引用
        mainPanel = null;
        diagnosticsTree = null;
        treeModel = null;
        rootNode = null;
        statusLabel = null;
        severityFilter = null;
        showErrorsCheckBox = null;
        showWarningsCheckBox = null;
        showInfoCheckBox = null;
    }
    
    /**
     * 文件诊断节点
     */
    private static class FileDiagnosticNode {
        final VirtualFile file;
        final List<Diagnostic> diagnostics;
        
        FileDiagnosticNode(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
            this.file = file;
            this.diagnostics = diagnostics;
        }
        
        @Override
        public String toString() {
            return file.getName() + " (" + diagnostics.size() + " diagnostics)";
        }
    }
    
    /**
     * 诊断节点
     */
    private static class DiagnosticNode {
        final Diagnostic diagnostic;
        
        DiagnosticNode(@NotNull Diagnostic diagnostic) {
            this.diagnostic = diagnostic;
        }
        
        @Override
        public String toString() {
            return diagnostic.getSeverity() + ": " + diagnostic.getMessage();
        }
    }
    
    /**
     * 诊断树单元格渲染器
     */
    private static class DiagnosticTreeCellRenderer extends javax.swing.tree.DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            
            if (value instanceof DefaultMutableTreeNode) {
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
                Object userObject = node.getUserObject();
                
                if (userObject instanceof FileDiagnosticNode) {
                    setIcon(AllIcons.FileTypes.Any_type);
                } else if (userObject instanceof DiagnosticNode) {
                    DiagnosticNode diagnosticNode = (DiagnosticNode) userObject;
                    Diagnostic.DiagnosticSeverity severity = diagnosticNode.diagnostic.getSeverity();
                    
                    switch (severity) {
                        case ERROR:
                            setIcon(AllIcons.General.Error);
                            break;
                        case WARNING:
                            setIcon(AllIcons.General.Warning);
                            break;
                        case INFO:
                            setIcon(AllIcons.General.Information);
                            break;
                        case HINT:
                            setIcon(AllIcons.General.ContextHelp);
                            break;
                    }
                }
            }
            
            return this;
        }
    }
    
    /**
     * 问题视图工具窗口工厂
     */
    public static class Factory implements ToolWindowFactory {
        @Override
        public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
            OmniSharpDiagnosticsProblemsView problemsView = new OmniSharpDiagnosticsProblemsView(project);
            
            ContentFactory contentFactory = ContentFactory.getInstance();
            Content content = contentFactory.createContent(problemsView.getMainPanel(), "OmniSharp Diagnostics", false);
            
            toolWindow.getContentManager().addContent(content);
        }
        
        @Override
        public boolean shouldBeAvailable(@NotNull Project project) {
            return true;
        }
    }
}