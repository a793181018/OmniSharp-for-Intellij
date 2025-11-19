/**
 * OmniSharp格式化设置管理类
 * 管理格式化相关的配置选项
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.util.xmlb.annotations.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
    name = "OmniSharpFormattingSettings",
    storages = {@Storage("omnisharp-formatting.xml")}
)
public class OmniSharpFormattingSettings implements PersistentStateComponent<OmniSharpFormattingSettings.State> {
    
    private State myState = new State();
    
    public static class State {
        @Attribute("formatOnSave")
        public boolean formatOnSave = true;
        
        @Tag("formattingTimeout")
        public int formattingTimeout = 3000;
        
        @Tag("maxLineLength")
        public int maxLineLength = 120;
        
        @Tag("useTabs")
        public boolean useTabs = false;
        
        @Tag("tabSize")
        public int tabSize = 4;
        
        @Tag("indentSize")
        public int indentSize = 4;
        
        @Tag("insertFinalNewline")
        public boolean insertFinalNewline = true;
        
        @Tag("trimTrailingWhitespace")
        public boolean trimTrailingWhitespace = true;
        
        @Tag("organizeImports")
        public boolean organizeImports = false;
        
        @Tag("formatDocumentationComments")
        public boolean formatDocumentationComments = true;
        
        @Tag("formatRegions")
        public boolean formatRegions = true;
        
        @Tag("formatPreprocessorDirectives")
        public boolean formatPreprocessorDirectives = true;
        
        @Tag("spacingAroundOperators")
        public boolean spacingAroundOperators = true;
        
        @Tag("spacingAfterKeywords")
        public boolean spacingAfterKeywords = true;
        
        @Tag("spacingBeforeParentheses")
        public boolean spacingBeforeParentheses = true;
        
        @Tag("braceStyle")
        public String braceStyle = "Allman"; // Allman, K&R, Stroustrup, Whitesmiths, Banner, GNU, Linux, Horstmann, 1TBS, LISP, Pico
        
        @Tag("indentCaseContents")
        public boolean indentCaseContents = true;
        
        @Tag("indentSwitchCaseContents")
        public boolean indentSwitchCaseContents = true;
        
        @Tag("indentNamespaceContents")
        public boolean indentNamespaceContents = true;
        
        @Tag("newLineBeforeOpenBrace")
        public boolean newLineBeforeOpenBrace = true;
        
        @Tag("newLineBeforeElse")
        public boolean newLineBeforeElse = true;
        
        @Tag("newLineBeforeCatch")
        public boolean newLineBeforeCatch = true;
        
        @Tag("newLineBeforeFinally")
        public boolean newLineBeforeFinally = true;
        
        @Tag("newLineBeforeMembersInAnonymousTypes")
        public boolean newLineBeforeMembersInAnonymousTypes = true;
        
        @Tag("newLineBeforeMembersInObjectInitializers")
        public boolean newLineBeforeMembersInObjectInitializers = true;
        
        @Tag("newLineBeforeQueryKeyword")
        public boolean newLineBeforeQueryKeyword = true;
        
        @Tag("skipFormattingForGeneratedCode")
        public boolean skipFormattingForGeneratedCode = true;
        
        @Tag("skipFormattingForDesignerFiles")
        public boolean skipFormattingForDesignerFiles = true;
        
        @Tag("formatOnPaste")
        public boolean formatOnPaste = false;
        
        @Tag("formatOnType")
        public boolean formatOnType = false;
        
        @Tag("enableEditorConfigSupport")
        public boolean enableEditorConfigSupport = true;
        
        @Tag("respectEditorConfigIndentation")
        public boolean respectEditorConfigIndentation = true;
        
        @Tag("respectEditorConfigNewLine")
        public boolean respectEditorConfigNewLine = true;
        
        @Tag("respectEditorConfigCharset")
        public boolean respectEditorConfigCharset = true;
        
        @Tag("respectEditorConfigTrimTrailingWhitespace")
        public boolean respectEditorConfigTrimTrailingWhitespace = true;
        
        @Tag("respectEditorConfigInsertFinalNewline")
        public boolean respectEditorConfigInsertFinalNewline = true;
        
        @Tag("excludedDirectories")
        public java.util.List<String> excludedDirectories = new java.util.ArrayList<>();
        
        @Tag("maxFileSize")
        public long maxFileSize = 1024 * 1024; // 1MB
    }
    
    public static OmniSharpFormattingSettings getInstance(Project project) {
        return project.getService(OmniSharpFormattingSettings.class);
    }
    
    @Nullable
    @Override
    public State getState() {
        return myState;
    }
    
    @Override
    public void loadState(@NotNull State state) {
        XmlSerializerUtil.copyBean(state, myState);
    }
    
    // Getter和Setter方法
    
    public boolean isFormatOnSaveEnabled() {
        return myState.formatOnSave;
    }
    
    public void setFormatOnSaveEnabled(boolean enabled) {
        myState.formatOnSave = enabled;
    }
    
    public int getFormattingTimeout() {
        return myState.formattingTimeout;
    }
    
    public void setFormattingTimeout(int timeout) {
        myState.formattingTimeout = timeout;
    }
    
    public int getMaxLineLength() {
        return myState.maxLineLength;
    }
    
    public void setMaxLineLength(int length) {
        myState.maxLineLength = length;
    }
    
    public boolean isUseTabs() {
        return myState.useTabs;
    }
    
    public void setUseTabs(boolean useTabs) {
        myState.useTabs = useTabs;
    }
    
    public int getTabSize() {
        return myState.tabSize;
    }
    
    public void setTabSize(int size) {
        myState.tabSize = size;
    }
    
    public int getIndentSize() {
        return myState.indentSize;
    }
    
    public void setIndentSize(int size) {
        myState.indentSize = size;
    }
    
    public boolean isInsertFinalNewline() {
        return myState.insertFinalNewline;
    }
    
    public void setInsertFinalNewline(boolean insert) {
        myState.insertFinalNewline = insert;
    }
    
    public boolean isTrimTrailingWhitespace() {
        return myState.trimTrailingWhitespace;
    }
    
    public void setTrimTrailingWhitespace(boolean trim) {
        myState.trimTrailingWhitespace = trim;
    }
    
    public boolean isOrganizeImports() {
        return myState.organizeImports;
    }
    
    public void setOrganizeImports(boolean organize) {
        myState.organizeImports = organize;
    }
    
    public boolean isFormatDocumentationComments() {
        return myState.formatDocumentationComments;
    }
    
    public void setFormatDocumentationComments(boolean format) {
        myState.formatDocumentationComments = format;
    }
    
    public boolean isFormatRegions() {
        return myState.formatRegions;
    }
    
    public void setFormatRegions(boolean format) {
        myState.formatRegions = format;
    }
    
    public boolean isFormatPreprocessorDirectives() {
        return myState.formatPreprocessorDirectives;
    }
    
    public void setFormatPreprocessorDirectives(boolean format) {
        myState.formatPreprocessorDirectives = format;
    }
    
    public boolean isSpacingAroundOperators() {
        return myState.spacingAroundOperators;
    }
    
    public void setSpacingAroundOperators(boolean spacing) {
        myState.spacingAroundOperators = spacing;
    }
    
    public boolean isSpacingAfterKeywords() {
        return myState.spacingAfterKeywords;
    }
    
    public void setSpacingAfterKeywords(boolean spacing) {
        myState.spacingAfterKeywords = spacing;
    }
    
    public boolean isSpacingBeforeParentheses() {
        return myState.spacingBeforeParentheses;
    }
    
    public void setSpacingBeforeParentheses(boolean spacing) {
        myState.spacingBeforeParentheses = spacing;
    }
    
    public String getBraceStyle() {
        return myState.braceStyle;
    }
    
    public void setBraceStyle(String style) {
        myState.braceStyle = style;
    }
    
    public boolean isIndentCaseContents() {
        return myState.indentCaseContents;
    }
    
    public void setIndentCaseContents(boolean indent) {
        myState.indentCaseContents = indent;
    }
    
    public boolean isIndentSwitchCaseContents() {
        return myState.indentSwitchCaseContents;
    }
    
    public void setIndentSwitchCaseContents(boolean indent) {
        myState.indentSwitchCaseContents = indent;
    }
    
    public boolean isIndentNamespaceContents() {
        return myState.indentNamespaceContents;
    }
    
    public void setIndentNamespaceContents(boolean indent) {
        myState.indentNamespaceContents = indent;
    }
    
    public boolean isNewLineBeforeOpenBrace() {
        return myState.newLineBeforeOpenBrace;
    }
    
    public void setNewLineBeforeOpenBrace(boolean newline) {
        myState.newLineBeforeOpenBrace = newline;
    }
    
    public boolean isNewLineBeforeElse() {
        return myState.newLineBeforeElse;
    }
    
    public void setNewLineBeforeElse(boolean newline) {
        myState.newLineBeforeElse = newline;
    }
    
    public boolean isNewLineBeforeCatch() {
        return myState.newLineBeforeCatch;
    }
    
    public void setNewLineBeforeCatch(boolean newline) {
        myState.newLineBeforeCatch = newline;
    }
    
    public boolean isNewLineBeforeFinally() {
        return myState.newLineBeforeFinally;
    }
    
    public void setNewLineBeforeFinally(boolean newline) {
        myState.newLineBeforeFinally = newline;
    }
    
    public boolean isNewLineBeforeMembersInAnonymousTypes() {
        return myState.newLineBeforeMembersInAnonymousTypes;
    }
    
    public void setNewLineBeforeMembersInAnonymousTypes(boolean newline) {
        myState.newLineBeforeMembersInAnonymousTypes = newline;
    }
    
    public boolean isNewLineBeforeMembersInObjectInitializers() {
        return myState.newLineBeforeMembersInObjectInitializers;
    }
    
    public void setNewLineBeforeMembersInObjectInitializers(boolean newline) {
        myState.newLineBeforeMembersInObjectInitializers = newline;
    }
    
    public boolean isNewLineBeforeQueryKeyword() {
        return myState.newLineBeforeQueryKeyword;
    }
    
    public void setNewLineBeforeQueryKeyword(boolean newline) {
        myState.newLineBeforeQueryKeyword = newline;
    }
    
    public boolean isSkipFormattingForGeneratedCode() {
        return myState.skipFormattingForGeneratedCode;
    }
    
    public void setSkipFormattingForGeneratedCode(boolean skip) {
        myState.skipFormattingForGeneratedCode = skip;
    }
    
    public boolean isSkipFormattingForDesignerFiles() {
        return myState.skipFormattingForDesignerFiles;
    }
    
    public void setSkipFormattingForDesignerFiles(boolean skip) {
        myState.skipFormattingForDesignerFiles = skip;
    }
    
    public boolean isFormatOnPaste() {
        return myState.formatOnPaste;
    }
    
    public void setFormatOnPaste(boolean format) {
        myState.formatOnPaste = format;
    }
    
    public boolean isFormatOnType() {
        return myState.formatOnType;
    }
    
    public void setFormatOnType(boolean format) {
        myState.formatOnType = format;
    }
    
    public boolean isEnableEditorConfigSupport() {
        return myState.enableEditorConfigSupport;
    }
    
    public void setEnableEditorConfigSupport(boolean enable) {
        myState.enableEditorConfigSupport = enable;
    }
    
    public boolean isRespectEditorConfigIndentation() {
        return myState.respectEditorConfigIndentation;
    }
    
    public void setRespectEditorConfigIndentation(boolean respect) {
        myState.respectEditorConfigIndentation = respect;
    }
    
    public boolean isRespectEditorConfigNewLine() {
        return myState.respectEditorConfigNewLine;
    }
    
    public void setRespectEditorConfigNewLine(boolean respect) {
        myState.respectEditorConfigNewLine = respect;
    }
    
    public boolean isRespectEditorConfigCharset() {
        return myState.respectEditorConfigCharset;
    }
    
    public void setRespectEditorConfigCharset(boolean respect) {
        myState.respectEditorConfigCharset = respect;
    }
    
    public boolean isRespectEditorConfigTrimTrailingWhitespace() {
        return myState.respectEditorConfigTrimTrailingWhitespace;
    }
    
    public void setRespectEditorConfigTrimTrailingWhitespace(boolean respect) {
        myState.respectEditorConfigTrimTrailingWhitespace = respect;
    }
    
    public boolean isRespectEditorConfigInsertFinalNewline() {
        return myState.respectEditorConfigInsertFinalNewline;
    }
    
    public void setRespectEditorConfigInsertFinalNewline(boolean respect) {
        myState.respectEditorConfigInsertFinalNewline = respect;
    }
    
    public java.util.List<String> getExcludedDirectories() {
        return myState.excludedDirectories;
    }
    
    public void setExcludedDirectories(java.util.List<String> excludedDirectories) {
        myState.excludedDirectories = excludedDirectories;
    }
    
    public long getMaxFileSize() {
        return myState.maxFileSize;
    }
    
    public void setMaxFileSize(long maxFileSize) {
        myState.maxFileSize = maxFileSize;
    }
    
    /**
     * 重置为默认设置
     */
    public void resetToDefaults() {
        myState = new State();
    }
    
    /**
     * 导出设置为JSON格式
     */
    public String exportToJson() {
        return "{\"formatOnSave\":" + myState.formatOnSave + 
               ",\"formatOnSaveEnabled\":" + myState.formatOnSave +
               ",\"formattingTimeout\":" + myState.formattingTimeout +
               ",\"maxLineLength\":" + myState.maxLineLength +
               ",\"useTabs\":" + myState.useTabs +
               ",\"tabSize\":" + myState.tabSize +
               ",\"indentSize\":" + myState.indentSize +
               ",\"insertFinalNewline\":" + myState.insertFinalNewline +
               ",\"trimTrailingWhitespace\":" + myState.trimTrailingWhitespace +
               ",\"organizeImports\":" + myState.organizeImports +
               ",\"formatDocumentationComments\":" + myState.formatDocumentationComments +
               ",\"formatRegions\":" + myState.formatRegions +
               ",\"formatPreprocessorDirectives\":" + myState.formatPreprocessorDirectives +
               ",\"spacingAroundOperators\":" + myState.spacingAroundOperators +
               ",\"spacingAfterKeywords\":" + myState.spacingAfterKeywords +
               ",\"spacingBeforeParentheses\":" + myState.spacingBeforeParentheses +
               ",\"braceStyle\":\"" + myState.braceStyle + "\"" +
               ",\"indentCaseContents\":" + myState.indentCaseContents +
               ",\"indentSwitchCaseContents\":" + myState.indentSwitchCaseContents +
               ",\"indentNamespaceContents\":" + myState.indentNamespaceContents +
               ",\"newLineBeforeOpenBrace\":" + myState.newLineBeforeOpenBrace +
               ",\"newLineBeforeElse\":" + myState.newLineBeforeElse +
               ",\"newLineBeforeCatch\":" + myState.newLineBeforeCatch +
               ",\"newLineBeforeFinally\":" + myState.newLineBeforeFinally +
               ",\"newLineBeforeMembersInAnonymousTypes\":" + myState.newLineBeforeMembersInAnonymousTypes +
               ",\"newLineBeforeMembersInObjectInitializers\":" + myState.newLineBeforeMembersInObjectInitializers +
               ",\"newLineBeforeQueryKeyword\":" + myState.newLineBeforeQueryKeyword +
               ",\"skipFormattingForGeneratedCode\":" + myState.skipFormattingForGeneratedCode +
               ",\"skipFormattingForDesignerFiles\":" + myState.skipFormattingForDesignerFiles +
               ",\"formatOnPaste\":" + myState.formatOnPaste +
               ",\"formatOnType\":" + myState.formatOnType +
               ",\"enableEditorConfigSupport\":" + myState.enableEditorConfigSupport +
               ",\"respectEditorConfigIndentation\":" + myState.respectEditorConfigIndentation +
               ",\"respectEditorConfigNewLine\":" + myState.respectEditorConfigNewLine +
               ",\"respectEditorConfigCharset\":" + myState.respectEditorConfigCharset +
               ",\"respectEditorConfigTrimTrailingWhitespace\":" + myState.respectEditorConfigTrimTrailingWhitespace +
               ",\"respectEditorConfigInsertFinalNewline\":" + myState.respectEditorConfigInsertFinalNewline + "}";
    }
}