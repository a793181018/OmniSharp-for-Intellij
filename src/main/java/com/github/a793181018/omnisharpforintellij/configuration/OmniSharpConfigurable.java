package com.github.a793181018.omnisharpforintellij.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * OmniSharp配置页面，实现Configurable接口
 */
public final class OmniSharpConfigurable implements Configurable {
    private final Project project;
    private final OmniSharpConfiguration configuration;
    private JTextField serverPathField;
    private JTextField serverArgumentsField;
    private JTextField serverTimeoutField;
    private JTextField codeCompletionMaxResultsField;
    private JCheckBox autoStartServerCheckbox;
    private JCheckBox debugModeCheckbox;
    
    public OmniSharpConfigurable(@Nullable Project project) {
        this.project = project;
        this.configuration = project != null ? 
                project.getService(OmniSharpConfiguration.class) : 
                OmniSharpSettings.getInstance();
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "OmniSharp for IntelliJ";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        serverPathField = new JBTextField(configuration.getServerPath(), 40);
        serverArgumentsField = new JBTextField(configuration.getServerArguments(), 60);
        serverTimeoutField = new JBTextField(String.valueOf(configuration.getServerTimeoutMs()), 10);
        codeCompletionMaxResultsField = new JBTextField(String.valueOf(configuration.getCodeCompletionMaxResults()), 10);
        autoStartServerCheckbox = new JCheckBox("Auto start server when opening a C# project");
        autoStartServerCheckbox.setSelected(configuration.isAutoStartServer());
        debugModeCheckbox = new JCheckBox("Enable debug mode (logs more information)");
        debugModeCheckbox.setSelected(configuration.isDebugMode());
        
        return FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel("OmniSharp Server Path:"), serverPathField, 1, false)
                .addTooltip("Path to the OmniSharp server executable")
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Server Arguments:"), serverArgumentsField, 1, false)
                .addTooltip("Command line arguments for the OmniSharp server")
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Server Timeout (ms):"), serverTimeoutField, 1, false)
                .addTooltip("Timeout for server operations")
                .addVerticalGap(5)
                .addLabeledComponent(new JBLabel("Code Completion Max Results:"), codeCompletionMaxResultsField, 1, false)
                .addTooltip("Maximum number of results to show in code completion")
                .addVerticalGap(5)
                .addComponent(autoStartServerCheckbox)
                .addVerticalGap(5)
                .addComponent(debugModeCheckbox)
                .addVerticalGap(10)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }
    
    @Override
    public boolean isModified() {
        if (configuration.getServerPath() == null) {
            return !serverPathField.getText().isEmpty() ||
                    !serverArgumentsField.getText().equals(configuration.getServerArguments()) ||
                    !serverTimeoutField.getText().equals(String.valueOf(configuration.getServerTimeoutMs())) ||
                    !codeCompletionMaxResultsField.getText().equals(String.valueOf(configuration.getCodeCompletionMaxResults())) ||
                    autoStartServerCheckbox.isSelected() != configuration.isAutoStartServer() ||
                    debugModeCheckbox.isSelected() != configuration.isDebugMode();
        }
        
        return !serverPathField.getText().equals(configuration.getServerPath()) ||
                !serverArgumentsField.getText().equals(configuration.getServerArguments()) ||
                !serverTimeoutField.getText().equals(String.valueOf(configuration.getServerTimeoutMs())) ||
                !codeCompletionMaxResultsField.getText().equals(String.valueOf(configuration.getCodeCompletionMaxResults())) ||
                autoStartServerCheckbox.isSelected() != configuration.isAutoStartServer() ||
                debugModeCheckbox.isSelected() != configuration.isDebugMode();
    }
    
    @Override
    public void apply() {
        String serverPath = serverPathField.getText().trim();
        if (!serverPath.isEmpty()) {
            // 验证服务器路径
            Path path = Path.of(serverPath);
            if (!Files.exists(path)) {
                Messages.showWarningDialog(
                        "The specified OmniSharp server path does not exist: " + serverPath,
                        "Invalid Server Path"
                );
                return;
            }
            
            if (!Files.isExecutable(path)) {
                Messages.showWarningDialog(
                        "The specified OmniSharp server file is not executable: " + serverPath,
                        "Invalid Server Path"
                );
                return;
            }
        }
        
        try {
            // 验证超时时间
            int timeoutMs = Integer.parseInt(serverTimeoutField.getText().trim());
            if (timeoutMs < 1000) {
                Messages.showWarningDialog(
                        "Server timeout must be at least 1000ms (1 second).",
                        "Invalid Timeout Value"
                );
                return;
            }
            
            // 验证最大结果数
            int maxResults = Integer.parseInt(codeCompletionMaxResultsField.getText().trim());
            if (maxResults < 1 || maxResults > 1000) {
                Messages.showWarningDialog(
                        "Code completion max results must be between 1 and 1000.",
                        "Invalid Max Results Value"
                );
                return;
            }
            
            // 应用配置
            configuration.setServerPath(serverPath.isEmpty() ? null : serverPath);
            configuration.setServerArguments(serverArgumentsField.getText().trim());
            configuration.setServerTimeoutMs(timeoutMs);
            configuration.setCodeCompletionMaxResults(maxResults);
            configuration.setAutoStartServer(autoStartServerCheckbox.isSelected());
            configuration.setDebugMode(debugModeCheckbox.isSelected());
            configuration.save();
            
            // 通知文件系统更新
            VirtualFileManager.getInstance().refreshWithoutFileWatcher(false);
            
        } catch (NumberFormatException e) {
            Messages.showErrorDialog(
                    "Invalid number format for timeout or max results.",
                    "Configuration Error"
            );
        }
    }
    
    @Override
    public void reset() {
        serverPathField.setText(configuration.getServerPath() != null ? configuration.getServerPath() : "");
        serverArgumentsField.setText(configuration.getServerArguments());
        serverTimeoutField.setText(String.valueOf(configuration.getServerTimeoutMs()));
        codeCompletionMaxResultsField.setText(String.valueOf(configuration.getCodeCompletionMaxResults()));
        autoStartServerCheckbox.setSelected(configuration.isAutoStartServer());
        debugModeCheckbox.setSelected(configuration.isDebugMode());
    }
    
    @Override
    public void disposeUIResources() {
        serverPathField = null;
        serverArgumentsField = null;
        serverTimeoutField = null;
        codeCompletionMaxResultsField = null;
        autoStartServerCheckbox = null;
        debugModeCheckbox = null;
    }
}