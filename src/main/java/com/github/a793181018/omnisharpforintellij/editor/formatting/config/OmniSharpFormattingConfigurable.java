/**
 * OmniSharp格式化配置界面
 * 提供格式化选项的配置界面
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting.config;

import com.github.a793181018.omnisharpforintellij.editor.formatting.OmniSharpFormattingSettings;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class OmniSharpFormattingConfigurable implements Configurable {
    
    private final Project project;
    private final OmniSharpFormattingSettings settings;
    
    private JPanel mainPanel;
    private JBCheckBox formatOnSaveCheckBox;
    private JBCheckBox formatOnSaveEnabledCheckBox;
    private JBTextField formattingTimeoutField;
    private JBTextField maxLineLengthField;
    private JBCheckBox useTabsCheckBox;
    private JBTextField tabSizeField;
    private JBTextField indentSizeField;
    private JBCheckBox insertFinalNewlineCheckBox;
    private JBCheckBox trimTrailingWhitespaceCheckBox;
    private JBCheckBox organizeImportsCheckBox;
    private JBCheckBox formatDocumentationCommentsCheckBox;
    private JBCheckBox formatRegionsCheckBox;
    private JBCheckBox formatPreprocessorDirectivesCheckBox;
    private JBCheckBox spacingAroundOperatorsCheckBox;
    private JBCheckBox spacingAfterKeywordsCheckBox;
    private JBCheckBox spacingBeforeParenthesesCheckBox;
    private ComboBox<String> braceStyleComboBox;
    private JBCheckBox indentCaseContentsCheckBox;
    private JBCheckBox indentSwitchCaseContentsCheckBox;
    private JBCheckBox indentNamespaceContentsCheckBox;
    private JBCheckBox newLineBeforeOpenBraceCheckBox;
    private JBCheckBox newLineBeforeElseCheckBox;
    private JBCheckBox newLineBeforeCatchCheckBox;
    private JBCheckBox newLineBeforeFinallyCheckBox;
    private JBCheckBox newLineBeforeMembersInAnonymousTypesCheckBox;
    private JBCheckBox newLineBeforeMembersInObjectInitializersCheckBox;
    private JBCheckBox newLineBeforeQueryKeywordCheckBox;
    private JBCheckBox skipFormattingForGeneratedCodeCheckBox;
    private JBCheckBox skipFormattingForDesignerFilesCheckBox;
    private JBCheckBox formatOnPasteCheckBox;
    private JBCheckBox formatOnTypeCheckBox;
    private JBCheckBox enableEditorConfigSupportCheckBox;
    private JBCheckBox respectEditorConfigIndentationCheckBox;
    private JBCheckBox respectEditorConfigNewLineCheckBox;
    private JBCheckBox respectEditorConfigCharsetCheckBox;
    private JBCheckBox respectEditorConfigTrimTrailingWhitespaceCheckBox;
    private JBCheckBox respectEditorConfigInsertFinalNewlineCheckBox;
    
    public OmniSharpFormattingConfigurable(Project project) {
        this.project = project;
        this.settings = OmniSharpFormattingSettings.getInstance(project);
    }
    
    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "OmniSharp Formatting";
    }
    
    @Nullable
    @Override
    public JComponent createComponent() {
        if (mainPanel == null) {
            mainPanel = createMainPanel();
        }
        return mainPanel;
    }
    
    private JPanel createMainPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // 常规选项
        JPanel generalPanel = createGeneralPanel();
        tabbedPane.addTab("General", generalPanel);
        
        // 缩进选项
        JPanel indentationPanel = createIndentationPanel();
        tabbedPane.addTab("Indentation", indentationPanel);
        
        // 间距选项
        JPanel spacingPanel = createSpacingPanel();
        tabbedPane.addTab("Spacing", spacingPanel);
        
        // 换行选项
        JPanel newLinesPanel = createNewLinesPanel();
        tabbedPane.addTab("New Lines", newLinesPanel);
        
        // 高级选项
        JPanel advancedPanel = createAdvancedPanel();
        tabbedPane.addTab("Advanced", advancedPanel);
        
        // EditorConfig选项
        JPanel editorConfigPanel = createEditorConfigPanel();
        tabbedPane.addTab("EditorConfig", editorConfigPanel);
        
        panel.add(tabbedPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createGeneralPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        formatOnSaveCheckBox = new JBCheckBox("Format on save");
        builder.addLabeledComponent(formatOnSaveCheckBox, new JLabel());
        
        formatOnSaveEnabledCheckBox = new JBCheckBox("Enable format on save");
        builder.addLabeledComponent(formatOnSaveEnabledCheckBox, new JLabel());
        
        formattingTimeoutField = new JBTextField();
        builder.addLabeledComponent(new JBLabel("Formatting timeout (ms):"), formattingTimeoutField);
        
        maxLineLengthField = new JBTextField();
        builder.addLabeledComponent(new JBLabel("Maximum line length:"), maxLineLengthField);
        
        organizeImportsCheckBox = new JBCheckBox("Organize imports on format");
        builder.addLabeledComponent(organizeImportsCheckBox, new JLabel());
        
        formatDocumentationCommentsCheckBox = new JBCheckBox("Format documentation comments");
        builder.addLabeledComponent(formatDocumentationCommentsCheckBox, new JLabel());
        
        formatRegionsCheckBox = new JBCheckBox("Format #region directives");
        builder.addLabeledComponent(formatRegionsCheckBox, new JLabel());
        
        formatPreprocessorDirectivesCheckBox = new JBCheckBox("Format preprocessor directives");
        builder.addLabeledComponent(formatPreprocessorDirectivesCheckBox, new JLabel());
        
        formatOnPasteCheckBox = new JBCheckBox("Format on paste");
        builder.addLabeledComponent(formatOnPasteCheckBox, new JLabel());
        
        formatOnTypeCheckBox = new JBCheckBox("Format on type");
        builder.addLabeledComponent(formatOnTypeCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    private JPanel createIndentationPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        useTabsCheckBox = new JBCheckBox("Use tabs instead of spaces");
        builder.addLabeledComponent(useTabsCheckBox, new JLabel());
        
        tabSizeField = new JBTextField();
        builder.addLabeledComponent(new JBLabel("Tab size:"), tabSizeField);
        
        indentSizeField = new JBTextField();
        builder.addLabeledComponent(new JBLabel("Indent size:"), indentSizeField);
        
        indentCaseContentsCheckBox = new JBCheckBox("Indent case contents");
        builder.addLabeledComponent(indentCaseContentsCheckBox, new JLabel());
        
        indentSwitchCaseContentsCheckBox = new JBCheckBox("Indent switch case contents");
        builder.addLabeledComponent(indentSwitchCaseContentsCheckBox, new JLabel());
        
        indentNamespaceContentsCheckBox = new JBCheckBox("Indent namespace contents");
        builder.addLabeledComponent(indentNamespaceContentsCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    private JPanel createSpacingPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        spacingAroundOperatorsCheckBox = new JBCheckBox("Spacing around operators");
        builder.addLabeledComponent(spacingAroundOperatorsCheckBox, new JLabel());
        
        spacingAfterKeywordsCheckBox = new JBCheckBox("Spacing after keywords");
        builder.addLabeledComponent(spacingAfterKeywordsCheckBox, new JLabel());
        
        spacingBeforeParenthesesCheckBox = new JBCheckBox("Spacing before parentheses");
        builder.addLabeledComponent(spacingBeforeParenthesesCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    private JPanel createNewLinesPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        braceStyleComboBox = new ComboBox<>(new String[]{
            "Allman", "K&R", "Stroustrup", "Whitesmiths", "Banner", "GNU", "Linux", "Horstmann", "1TBS", "LISP", "Pico"
        });
        builder.addLabeledComponent(new JBLabel("Brace style:"), braceStyleComboBox);
        
        newLineBeforeOpenBraceCheckBox = new JBCheckBox("New line before open brace");
        builder.addLabeledComponent(newLineBeforeOpenBraceCheckBox, new JLabel());
        
        newLineBeforeElseCheckBox = new JBCheckBox("New line before else");
        builder.addLabeledComponent(newLineBeforeElseCheckBox, new JLabel());
        
        newLineBeforeCatchCheckBox = new JBCheckBox("New line before catch");
        builder.addLabeledComponent(newLineBeforeCatchCheckBox, new JLabel());
        
        newLineBeforeFinallyCheckBox = new JBCheckBox("New line before finally");
        builder.addLabeledComponent(newLineBeforeFinallyCheckBox, new JLabel());
        
        newLineBeforeMembersInAnonymousTypesCheckBox = new JBCheckBox("New line before members in anonymous types");
        builder.addLabeledComponent(newLineBeforeMembersInAnonymousTypesCheckBox, new JLabel());
        
        newLineBeforeMembersInObjectInitializersCheckBox = new JBCheckBox("New line before members in object initializers");
        builder.addLabeledComponent(newLineBeforeMembersInObjectInitializersCheckBox, new JLabel());
        
        newLineBeforeQueryKeywordCheckBox = new JBCheckBox("New line before query keyword");
        builder.addLabeledComponent(newLineBeforeQueryKeywordCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    private JPanel createAdvancedPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        skipFormattingForGeneratedCodeCheckBox = new JBCheckBox("Skip formatting for generated code");
        builder.addLabeledComponent(skipFormattingForGeneratedCodeCheckBox, new JLabel());
        
        skipFormattingForDesignerFilesCheckBox = new JBCheckBox("Skip formatting for designer files");
        builder.addLabeledComponent(skipFormattingForDesignerFilesCheckBox, new JLabel());
        
        insertFinalNewlineCheckBox = new JBCheckBox("Insert final newline");
        builder.addLabeledComponent(insertFinalNewlineCheckBox, new JLabel());
        
        trimTrailingWhitespaceCheckBox = new JBCheckBox("Trim trailing whitespace");
        builder.addLabeledComponent(trimTrailingWhitespaceCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    private JPanel createEditorConfigPanel() {
        FormBuilder builder = FormBuilder.createFormBuilder();
        
        enableEditorConfigSupportCheckBox = new JBCheckBox("Enable EditorConfig support");
        builder.addLabeledComponent(enableEditorConfigSupportCheckBox, new JLabel());
        
        respectEditorConfigIndentationCheckBox = new JBCheckBox("Respect EditorConfig indentation settings");
        builder.addLabeledComponent(respectEditorConfigIndentationCheckBox, new JLabel());
        
        respectEditorConfigNewLineCheckBox = new JBCheckBox("Respect EditorConfig new line settings");
        builder.addLabeledComponent(respectEditorConfigNewLineCheckBox, new JLabel());
        
        respectEditorConfigCharsetCheckBox = new JBCheckBox("Respect EditorConfig charset settings");
        builder.addLabeledComponent(respectEditorConfigCharsetCheckBox, new JLabel());
        
        respectEditorConfigTrimTrailingWhitespaceCheckBox = new JBCheckBox("Respect EditorConfig trim trailing whitespace settings");
        builder.addLabeledComponent(respectEditorConfigTrimTrailingWhitespaceCheckBox, new JLabel());
        
        respectEditorConfigInsertFinalNewlineCheckBox = new JBCheckBox("Respect EditorConfig insert final newline settings");
        builder.addLabeledComponent(respectEditorConfigInsertFinalNewlineCheckBox, new JLabel());
        
        return builder.getPanel();
    }
    
    @Override
    public boolean isModified() {
        return formatOnSaveCheckBox != null && 
               (formatOnSaveCheckBox.isSelected() != settings.isFormatOnSaveEnabled() ||
                !formattingTimeoutField.getText().equals(String.valueOf(settings.getFormattingTimeout())) ||
                !maxLineLengthField.getText().equals(String.valueOf(settings.getMaxLineLength())) ||
                useTabsCheckBox.isSelected() != settings.isUseTabs() ||
                !tabSizeField.getText().equals(String.valueOf(settings.getTabSize())) ||
                !indentSizeField.getText().equals(String.valueOf(settings.getIndentSize())) ||
                insertFinalNewlineCheckBox.isSelected() != settings.isInsertFinalNewline() ||
                trimTrailingWhitespaceCheckBox.isSelected() != settings.isTrimTrailingWhitespace() ||
                organizeImportsCheckBox.isSelected() != settings.isOrganizeImports() ||
                formatDocumentationCommentsCheckBox.isSelected() != settings.isFormatDocumentationComments() ||
                formatRegionsCheckBox.isSelected() != settings.isFormatRegions() ||
                formatPreprocessorDirectivesCheckBox.isSelected() != settings.isFormatPreprocessorDirectives() ||
                spacingAroundOperatorsCheckBox.isSelected() != settings.isSpacingAroundOperators() ||
                spacingAfterKeywordsCheckBox.isSelected() != settings.isSpacingAfterKeywords() ||
                spacingBeforeParenthesesCheckBox.isSelected() != settings.isSpacingBeforeParentheses() ||
                !braceStyleComboBox.getSelectedItem().equals(settings.getBraceStyle()) ||
                indentCaseContentsCheckBox.isSelected() != settings.isIndentCaseContents() ||
                indentSwitchCaseContentsCheckBox.isSelected() != settings.isIndentSwitchCaseContents() ||
                indentNamespaceContentsCheckBox.isSelected() != settings.isIndentNamespaceContents() ||
                newLineBeforeOpenBraceCheckBox.isSelected() != settings.isNewLineBeforeOpenBrace() ||
                newLineBeforeElseCheckBox.isSelected() != settings.isNewLineBeforeElse() ||
                newLineBeforeCatchCheckBox.isSelected() != settings.isNewLineBeforeCatch() ||
                newLineBeforeFinallyCheckBox.isSelected() != settings.isNewLineBeforeFinally() ||
                newLineBeforeMembersInAnonymousTypesCheckBox.isSelected() != settings.isNewLineBeforeMembersInAnonymousTypes() ||
                newLineBeforeMembersInObjectInitializersCheckBox.isSelected() != settings.isNewLineBeforeMembersInObjectInitializers() ||
                newLineBeforeQueryKeywordCheckBox.isSelected() != settings.isNewLineBeforeQueryKeyword() ||
                skipFormattingForGeneratedCodeCheckBox.isSelected() != settings.isSkipFormattingForGeneratedCode() ||
                skipFormattingForDesignerFilesCheckBox.isSelected() != settings.isSkipFormattingForDesignerFiles() ||
                formatOnPasteCheckBox.isSelected() != settings.isFormatOnPaste() ||
                formatOnTypeCheckBox.isSelected() != settings.isFormatOnType() ||
                enableEditorConfigSupportCheckBox.isSelected() != settings.isEnableEditorConfigSupport() ||
                respectEditorConfigIndentationCheckBox.isSelected() != settings.isRespectEditorConfigIndentation() ||
                respectEditorConfigNewLineCheckBox.isSelected() != settings.isRespectEditorConfigNewLine() ||
                respectEditorConfigCharsetCheckBox.isSelected() != settings.isRespectEditorConfigCharset() ||
                respectEditorConfigTrimTrailingWhitespaceCheckBox.isSelected() != settings.isRespectEditorConfigTrimTrailingWhitespace() ||
                respectEditorConfigInsertFinalNewlineCheckBox.isSelected() != settings.isRespectEditorConfigInsertFinalNewline());
    }
    
    @Override
    public void apply() throws ConfigurationException {
        settings.setFormatOnSaveEnabled(formatOnSaveCheckBox.isSelected());
        settings.setFormattingTimeout(Integer.parseInt(formattingTimeoutField.getText()));
        settings.setMaxLineLength(Integer.parseInt(maxLineLengthField.getText()));
        settings.setUseTabs(useTabsCheckBox.isSelected());
        settings.setTabSize(Integer.parseInt(tabSizeField.getText()));
        settings.setIndentSize(Integer.parseInt(indentSizeField.getText()));
        settings.setInsertFinalNewline(insertFinalNewlineCheckBox.isSelected());
        settings.setTrimTrailingWhitespace(trimTrailingWhitespaceCheckBox.isSelected());
        settings.setOrganizeImports(organizeImportsCheckBox.isSelected());
        settings.setFormatDocumentationComments(formatDocumentationCommentsCheckBox.isSelected());
        settings.setFormatRegions(formatRegionsCheckBox.isSelected());
        settings.setFormatPreprocessorDirectives(formatPreprocessorDirectivesCheckBox.isSelected());
        settings.setSpacingAroundOperators(spacingAroundOperatorsCheckBox.isSelected());
        settings.setSpacingAfterKeywords(spacingAfterKeywordsCheckBox.isSelected());
        settings.setSpacingBeforeParentheses(spacingBeforeParenthesesCheckBox.isSelected());
        settings.setBraceStyle((String) braceStyleComboBox.getSelectedItem());
        settings.setIndentCaseContents(indentCaseContentsCheckBox.isSelected());
        settings.setIndentSwitchCaseContents(indentSwitchCaseContentsCheckBox.isSelected());
        settings.setIndentNamespaceContents(indentNamespaceContentsCheckBox.isSelected());
        settings.setNewLineBeforeOpenBrace(newLineBeforeOpenBraceCheckBox.isSelected());
        settings.setNewLineBeforeElse(newLineBeforeElseCheckBox.isSelected());
        settings.setNewLineBeforeCatch(newLineBeforeCatchCheckBox.isSelected());
        settings.setNewLineBeforeFinally(newLineBeforeFinallyCheckBox.isSelected());
        settings.setNewLineBeforeMembersInAnonymousTypes(newLineBeforeMembersInAnonymousTypesCheckBox.isSelected());
        settings.setNewLineBeforeMembersInObjectInitializers(newLineBeforeMembersInObjectInitializersCheckBox.isSelected());
        settings.setNewLineBeforeQueryKeyword(newLineBeforeQueryKeywordCheckBox.isSelected());
        settings.setSkipFormattingForGeneratedCode(skipFormattingForGeneratedCodeCheckBox.isSelected());
        settings.setSkipFormattingForDesignerFiles(skipFormattingForDesignerFilesCheckBox.isSelected());
        settings.setFormatOnPaste(formatOnPasteCheckBox.isSelected());
        settings.setFormatOnType(formatOnTypeCheckBox.isSelected());
        settings.setEnableEditorConfigSupport(enableEditorConfigSupportCheckBox.isSelected());
        settings.setRespectEditorConfigIndentation(respectEditorConfigIndentationCheckBox.isSelected());
        settings.setRespectEditorConfigNewLine(respectEditorConfigNewLineCheckBox.isSelected());
        settings.setRespectEditorConfigCharset(respectEditorConfigCharsetCheckBox.isSelected());
        settings.setRespectEditorConfigTrimTrailingWhitespace(respectEditorConfigTrimTrailingWhitespaceCheckBox.isSelected());
        settings.setRespectEditorConfigInsertFinalNewline(respectEditorConfigInsertFinalNewlineCheckBox.isSelected());
    }
    
    @Override
    public void reset() {
        formatOnSaveCheckBox.setSelected(settings.isFormatOnSaveEnabled());
        formattingTimeoutField.setText(String.valueOf(settings.getFormattingTimeout()));
        maxLineLengthField.setText(String.valueOf(settings.getMaxLineLength()));
        useTabsCheckBox.setSelected(settings.isUseTabs());
        tabSizeField.setText(String.valueOf(settings.getTabSize()));
        indentSizeField.setText(String.valueOf(settings.getIndentSize()));
        insertFinalNewlineCheckBox.setSelected(settings.isInsertFinalNewline());
        trimTrailingWhitespaceCheckBox.setSelected(settings.isTrimTrailingWhitespace());
        organizeImportsCheckBox.setSelected(settings.isOrganizeImports());
        formatDocumentationCommentsCheckBox.setSelected(settings.isFormatDocumentationComments());
        formatRegionsCheckBox.setSelected(settings.isFormatRegions());
        formatPreprocessorDirectivesCheckBox.setSelected(settings.isFormatPreprocessorDirectives());
        spacingAroundOperatorsCheckBox.setSelected(settings.isSpacingAroundOperators());
        spacingAfterKeywordsCheckBox.setSelected(settings.isSpacingAfterKeywords());
        spacingBeforeParenthesesCheckBox.setSelected(settings.isSpacingBeforeParentheses());
        braceStyleComboBox.setSelectedItem(settings.getBraceStyle());
        indentCaseContentsCheckBox.setSelected(settings.isIndentCaseContents());
        indentSwitchCaseContentsCheckBox.setSelected(settings.isIndentSwitchCaseContents());
        indentNamespaceContentsCheckBox.setSelected(settings.isIndentNamespaceContents());
        newLineBeforeOpenBraceCheckBox.setSelected(settings.isNewLineBeforeOpenBrace());
        newLineBeforeElseCheckBox.setSelected(settings.isNewLineBeforeElse());
        newLineBeforeCatchCheckBox.setSelected(settings.isNewLineBeforeCatch());
        newLineBeforeFinallyCheckBox.setSelected(settings.isNewLineBeforeFinally());
        newLineBeforeMembersInAnonymousTypesCheckBox.setSelected(settings.isNewLineBeforeMembersInAnonymousTypes());
        newLineBeforeMembersInObjectInitializersCheckBox.setSelected(settings.isNewLineBeforeMembersInObjectInitializers());
        newLineBeforeQueryKeywordCheckBox.setSelected(settings.isNewLineBeforeQueryKeyword());
        skipFormattingForGeneratedCodeCheckBox.setSelected(settings.isSkipFormattingForGeneratedCode());
        skipFormattingForDesignerFilesCheckBox.setSelected(settings.isSkipFormattingForDesignerFiles());
        formatOnPasteCheckBox.setSelected(settings.isFormatOnPaste());
        formatOnTypeCheckBox.setSelected(settings.isFormatOnType());
        enableEditorConfigSupportCheckBox.setSelected(settings.isEnableEditorConfigSupport());
        respectEditorConfigIndentationCheckBox.setSelected(settings.isRespectEditorConfigIndentation());
        respectEditorConfigNewLineCheckBox.setSelected(settings.isRespectEditorConfigNewLine());
        respectEditorConfigCharsetCheckBox.setSelected(settings.isRespectEditorConfigCharset());
        respectEditorConfigTrimTrailingWhitespaceCheckBox.setSelected(settings.isRespectEditorConfigTrimTrailingWhitespace());
        respectEditorConfigInsertFinalNewlineCheckBox.setSelected(settings.isRespectEditorConfigInsertFinalNewline());
    }
    
    @Override
    public void disposeUIResources() {
        mainPanel = null;
    }
}