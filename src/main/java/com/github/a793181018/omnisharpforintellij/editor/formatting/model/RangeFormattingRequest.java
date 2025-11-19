/**
 * 范围格式化请求模型
 */
package com.github.a793181018.omnisharpforintellij.editor.formatting.model;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.util.TextRange;

public class RangeFormattingRequest {
    private final String filePath;
    private final String fileName;
    private final String content;
    private final int startOffset;
    private final int endOffset;
    
    public RangeFormattingRequest(VirtualFile file, TextRange range) {
        this.filePath = file.getPath();
        this.fileName = file.getName();
        try {
            this.content = new String(file.contentsToByteArray());
        } catch (java.io.IOException e) {
            throw new RuntimeException("Failed to read file content", e);
        }
        this.startOffset = range.getStartOffset();
        this.endOffset = range.getEndOffset();
    }
    
    public String getFilePath() {
        return filePath;
    }
    
    public String getFileName() {
        return fileName;
    }
    
    public String getContent() {
        return content;
    }
    
    public int getStartOffset() {
        return startOffset;
    }
    
    public int getEndOffset() {
        return endOffset;
    }
}