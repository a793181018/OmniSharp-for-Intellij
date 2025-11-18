package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.request;

import org.jetbrains.annotations.NotNull;

/**
 * 文件结构请求模型
 * 用于向OmniSharp服务器请求C#文件的结构信息
 */
public class FileStructureRequest {
    private final String fileName;
    
    public FileStructureRequest(@NotNull String fileName) {
        this.fileName = fileName;
    }
    
    @NotNull
    public String getFileName() {
        return fileName;
    }
    
    @Override
    public String toString() {
        return "FileStructureRequest{" +
               "fileName='" + fileName + '\'' +
               '}';
    }
}