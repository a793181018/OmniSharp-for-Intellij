/**
 * 格式化响应模型
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting.model;

public class FormattingResponse {
    private String formattedContent;
    private boolean success;
    private String errorMessage;
    
    public FormattingResponse() {
        this.success = true;
    }
    
    public FormattingResponse(String formattedContent) {
        this.formattedContent = formattedContent;
        this.success = true;
    }
    
    public FormattingResponse(String errorMessage, boolean success) {
        this.errorMessage = errorMessage;
        this.success = success;
    }
    
    public String getFormattedContent() {
        return formattedContent;
    }
    
    public void setFormattedContent(String formattedContent) {
        this.formattedContent = formattedContent;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public void setSuccess(boolean success) {
        this.success = success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}