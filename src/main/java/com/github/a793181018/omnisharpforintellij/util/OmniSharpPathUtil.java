package com.github.a793181018.omnisharpforintellij.util;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/**
 * OmniSharp路径工具类，提供路径处理相关的功能
 */
public final class OmniSharpPathUtil {
    
    private OmniSharpPathUtil() {
        // 私有构造函数，防止实例化
    }
    
    /**
     * 获取项目的基础路径
     */
    @Nullable
    public static String getProjectBasePath(@NotNull Project project) {
        return project.getBasePath();
    }
    
    /**
     * 获取项目的VirtualFile
     */
    @Nullable
    public static VirtualFile getProjectVirtualFile(@NotNull Project project) {
        String basePath = getProjectBasePath(project);
        if (basePath != null) {
            return LocalFileSystem.getInstance().findFileByPath(basePath);
        }
        return null;
    }
    
    /**
     * 规范化路径
     */
    @NotNull
    public static String normalizePath(@NotNull String path) {
        return Paths.get(path).normalize().toString();
    }
    
    /**
     * 检查文件是否存在
     */
    public static boolean fileExists(@NotNull String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 检查文件是否可执行
     */
    public static boolean isExecutable(@NotNull String filePath) {
        Path path = Paths.get(filePath);
        return Files.exists(path) && Files.isExecutable(path);
    }
    
    /**
     * 确保目录存在
     */
    public static boolean ensureDirectoryExists(@NotNull String directoryPath) {
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return directory.isDirectory();
    }
    
    /**
     * 组合路径
     */
    @NotNull
    public static String combinePaths(@NotNull String... paths) {
        Path combinedPath = Paths.get(paths[0]);
        for (int i = 1; i < paths.length; i++) {
            combinedPath = combinedPath.resolve(paths[i]);
        }
        return combinedPath.toString();
    }
    
    /**
     * 获取相对路径
     */
    @Nullable
    public static String getRelativePath(@NotNull String basePath, @NotNull String targetPath) {
        try {
            Path base = Paths.get(basePath);
            Path target = Paths.get(targetPath);
            Path relative = base.relativize(target);
            return relative.toString();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    @NotNull
    public static String getFileExtension(@NotNull String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase(Locale.US);
        }
        return "";
    }
    
    /**
     * 获取文件名（不包含扩展名）
     */
    @NotNull
    public static String getFileNameWithoutExtension(@NotNull String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    /**
     * 检查路径是否是绝对路径
     */
    public static boolean isAbsolutePath(@NotNull String path) {
        return Paths.get(path).isAbsolute();
    }
    
    /**
     * 获取系统临时目录
     */
    @NotNull
    public static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }
    
    /**
     * 获取OmniSharp缓存目录
     */
    @NotNull
    public static String getOmniSharpCacheDirectory(@NotNull Project project) {
        String tempDir = getTempDirectory();
        String projectHash = String.valueOf(project.getLocationHash());
        return combinePaths(tempDir, "omnisharp", "cache", projectHash);
    }
    
    /**
     * 检查是否是C#文件
     */
    public static boolean isCSharpFile(@NotNull String fileName) {
        String extension = getFileExtension(fileName).toLowerCase(Locale.US);
        return extension.equals("cs") || 
               extension.equals("csx") || 
               extension.equals("cake");
    }
    
    /**
     * 检查是否是项目文件
     */
    public static boolean isProjectFile(@NotNull String fileName) {
        String extension = getFileExtension(fileName).toLowerCase(Locale.US);
        return extension.equals("csproj") || 
               extension.equals("fsproj") || 
               extension.equals("vbproj") || 
               extension.equals("shproj");
    }
    
    /**
     * 检查是否是解决方案文件
     */
    public static boolean isSolutionFile(@NotNull String fileName) {
        String extension = getFileExtension(fileName).toLowerCase(Locale.US);
        return extension.equals("sln");
    }
}