package com.github.a793181018.omnisharpforintellij.editor.diagnostics.impl;

import com.github.a793181018.omnisharpforintellij.editor.diagnostics.model.Diagnostic;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * 诊断请求和响应数据类
 * 用于与OmniSharp服务器通信
 */
public class OmniSharpDiagnosticsRequest {
    
    /**
     * 诊断请求
     */
    public static class Request {
        private final String type;
        private final String fileName;
        private final String buffer;
        private final String projectFileName;
        private final String language;
        
        public Request(@NotNull String type, @NotNull String fileName, @Nullable String buffer, 
                      @Nullable String projectFileName, @NotNull String language) {
            this.type = type;
            this.fileName = fileName;
            this.buffer = buffer;
            this.projectFileName = projectFileName;
            this.language = language;
        }
        
        @NotNull
        public String getType() {
            return type;
        }
        
        @NotNull
        public String getFileName() {
            return fileName;
        }
        
        @Nullable
        public String getBuffer() {
            return buffer;
        }
        
        @Nullable
        public String getProjectFileName() {
            return projectFileName;
        }
        
        @NotNull
        public String getLanguage() {
            return language;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Objects.equals(type, request.type) &&
                   Objects.equals(fileName, request.fileName) &&
                   Objects.equals(buffer, request.buffer) &&
                   Objects.equals(projectFileName, request.projectFileName) &&
                   Objects.equals(language, request.language);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(type, fileName, buffer, projectFileName, language);
        }
        
        @Override
        public String toString() {
            return "Request{" +
                   "type='" + type + '\'' +
                   ", fileName='" + fileName + '\'' +
                   ", buffer='" + (buffer != null ? "<" + buffer.length() + " chars>" : "null") + '\'' +
                   ", projectFileName='" + projectFileName + '\'' +
                   ", language='" + language + '\'' +
                   '}';
        }
    }
    
    /**
     * 诊断响应
     */
    public static class Response {
        private final List<Diagnostic> diagnostics;
        private final String requestId;
        private final boolean success;
        private final String errorMessage;
        
        public Response(@NotNull List<Diagnostic> diagnostics, @Nullable String requestId, 
                       boolean success, @Nullable String errorMessage) {
            this.diagnostics = diagnostics;
            this.requestId = requestId;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @NotNull
        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }
        
        @Nullable
        public String getRequestId() {
            return requestId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean hasError() {
            return errorMessage != null && !errorMessage.isEmpty();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Response response = (Response) o;
            return success == response.success &&
                   Objects.equals(diagnostics, response.diagnostics) &&
                   Objects.equals(requestId, response.requestId) &&
                   Objects.equals(errorMessage, response.errorMessage);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(diagnostics, requestId, success, errorMessage);
        }
        
        @Override
        public String toString() {
            return "Response{" +
                   "diagnostics.size=" + diagnostics.size() +
                   ", requestId='" + requestId + '\'' +
                   ", success=" + success +
                   ", errorMessage='" + errorMessage + '\'' +
                   '}';
        }
    }
    
    /**
     * 代码修复请求
     */
    public static class CodeFixRequest {
        private final String fileName;
        private final int line;
        private final int column;
        private final String diagnosticId;
        
        public CodeFixRequest(@NotNull String fileName, int line, int column, @NotNull String diagnosticId) {
            this.fileName = fileName;
            this.line = line;
            this.column = column;
            this.diagnosticId = diagnosticId;
        }
        
        @NotNull
        public String getFileName() {
            return fileName;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
        
        @NotNull
        public String getDiagnosticId() {
            return diagnosticId;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeFixRequest that = (CodeFixRequest) o;
            return line == that.line &&
                   column == that.column &&
                   Objects.equals(fileName, that.fileName) &&
                   Objects.equals(diagnosticId, that.diagnosticId);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(fileName, line, column, diagnosticId);
        }
        
        @Override
        public String toString() {
            return "CodeFixRequest{" +
                   "fileName='" + fileName + '\'' +
                   ", line=" + line +
                   ", column=" + column +
                   ", diagnosticId='" + diagnosticId + '\'' +
                   '}';
        }
    }
    
    /**
     * 代码修复响应
     */
    public static class CodeFixResponse {
        private final List<Diagnostic.CodeFix> codeFixes;
        private final String requestId;
        private final boolean success;
        private final String errorMessage;
        
        public CodeFixResponse(@NotNull List<Diagnostic.CodeFix> codeFixes, @Nullable String requestId,
                              boolean success, @Nullable String errorMessage) {
            this.codeFixes = codeFixes;
            this.requestId = requestId;
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        @NotNull
        public List<Diagnostic.CodeFix> getCodeFixes() {
            return codeFixes;
        }
        
        @Nullable
        public String getRequestId() {
            return requestId;
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        @Nullable
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public boolean hasError() {
            return errorMessage != null && !errorMessage.isEmpty();
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CodeFixResponse that = (CodeFixResponse) o;
            return success == that.success &&
                   Objects.equals(codeFixes, that.codeFixes) &&
                   Objects.equals(requestId, that.requestId) &&
                   Objects.equals(errorMessage, that.errorMessage);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(codeFixes, requestId, success, errorMessage);
        }
        
        @Override
        public String toString() {
            return "CodeFixResponse{" +
                   "codeFixes.size=" + codeFixes.size() +
                   ", requestId='" + requestId + '\'' +
                   ", success=" + success +
                   ", errorMessage='" + errorMessage + '\'' +
                   '}';
        }
    }
    
    /**
     * 诊断更新事件
     */
    public static class DiagnosticUpdateEvent {
        private final VirtualFile file;
        private final List<Diagnostic> diagnostics;
        private final long timestamp;
        
        public DiagnosticUpdateEvent(@NotNull VirtualFile file, @NotNull List<Diagnostic> diagnostics) {
            this.file = file;
            this.diagnostics = diagnostics;
            this.timestamp = System.currentTimeMillis();
        }
        
        @NotNull
        public VirtualFile getFile() {
            return file;
        }
        
        @NotNull
        public List<Diagnostic> getDiagnostics() {
            return diagnostics;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiagnosticUpdateEvent that = (DiagnosticUpdateEvent) o;
            return timestamp == that.timestamp &&
                   Objects.equals(file, that.file) &&
                   Objects.equals(diagnostics, that.diagnostics);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(file, diagnostics, timestamp);
        }
        
        @Override
        public String toString() {
            return "DiagnosticUpdateEvent{" +
                   "file=" + file.getName() +
                   ", diagnostics.size=" + diagnostics.size() +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
    
    /**
     * 诊断统计信息
     */
    public static class DiagnosticStatistics {
        private final int totalCount;
        private final int errorCount;
        private final int warningCount;
        private final int infoCount;
        private final int hintCount;
        private final long lastUpdateTime;
        
        public DiagnosticStatistics(int totalCount, int errorCount, int warningCount, int infoCount, int hintCount) {
            this.totalCount = totalCount;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
            this.infoCount = infoCount;
            this.hintCount = hintCount;
            this.lastUpdateTime = System.currentTimeMillis();
        }
        
        public int getTotalCount() {
            return totalCount;
        }
        
        public int getErrorCount() {
            return errorCount;
        }
        
        public int getWarningCount() {
            return warningCount;
        }
        
        public int getInfoCount() {
            return infoCount;
        }
        
        public int getHintCount() {
            return hintCount;
        }
        
        public long getLastUpdateTime() {
            return lastUpdateTime;
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DiagnosticStatistics that = (DiagnosticStatistics) o;
            return totalCount == that.totalCount &&
                   errorCount == that.errorCount &&
                   warningCount == that.warningCount &&
                   infoCount == that.infoCount &&
                   hintCount == that.hintCount &&
                   lastUpdateTime == that.lastUpdateTime;
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(totalCount, errorCount, warningCount, infoCount, hintCount, lastUpdateTime);
        }
        
        @Override
        public String toString() {
            return "DiagnosticStatistics{" +
                   "totalCount=" + totalCount +
                   ", errorCount=" + errorCount +
                   ", warningCount=" + warningCount +
                   ", infoCount=" + infoCount +
                   ", hintCount=" + hintCount +
                   ", lastUpdateTime=" + lastUpdateTime +
                   '}';
        }
    }
}