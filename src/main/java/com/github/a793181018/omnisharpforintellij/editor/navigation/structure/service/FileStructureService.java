package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.service;

import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 文件结构服务接口
 * 提供获取C#文件结构信息的功能，包括类、方法、属性、字段等成员
 */
public interface FileStructureService {
    
    /**
     * 获取文件的结构信息
     * 
     * @param project 当前项目
     * @param file 要分析的文件
     * @return 文件中的导航目标列表（类、方法、属性等）
     */
    @NotNull
    CompletableFuture<List<NavigationTarget>> getFileStructure(@NotNull Project project, @NotNull VirtualFile file);
    
    /**
     * 获取文件的结构信息（同步版本）
     * 
     * @param project 当前项目
     * @param file 要分析的文件
     * @return 文件中的导航目标列表，如果获取失败则返回空列表
     */
    @NotNull
    List<NavigationTarget> getFileStructureSync(@NotNull Project project, @NotNull VirtualFile file);
    
    /**
     * 检查文件是否支持结构分析
     * 
     * @param file 要检查的文件
     * @return 如果文件是C#文件则返回true，否则返回false
     */
    boolean isSupportedFile(@Nullable VirtualFile file);
    
    /**
     * 清除指定文件的缓存
     * 
     * @param file 要清除缓存的文件
     */
    void clearCache(@NotNull VirtualFile file);
    
    /**
     * 清除所有缓存
     */
    void clearAllCache();
}