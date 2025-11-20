package com.omnisharp.intellij.projectstructure.utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 文件系统操作工具类，提供常用的文件和目录操作功能
 */
public class FileUtils {
    
    /**
     * 读取文件内容
     * @param filePath 文件路径
     * @return 文件内容字符串
     * @throws IOException 如果读取失败
     */
    public static String readFileContent(String filePath) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filePath)), StandardCharsets.UTF_8);
    }
    
    /**
     * 写入文件内容
     * @param filePath 文件路径
     * @param content 要写入的内容
     * @throws IOException 如果写入失败
     */
    public static void writeFileContent(String filePath, String content) throws IOException {
        Files.write(Paths.get(filePath), content.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * 复制文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException 如果复制失败
     */
    public static void copyFile(String sourcePath, String targetPath) throws IOException {
        Files.copy(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * 移动文件
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     * @throws IOException 如果移动失败
     */
    public static void moveFile(String sourcePath, String targetPath) throws IOException {
        Files.move(Paths.get(sourcePath), Paths.get(targetPath), StandardCopyOption.REPLACE_EXISTING);
    }
    
    /**
     * 删除文件
     * @param filePath 文件路径
     * @return 是否删除成功
     */
    public static boolean deleteFile(String filePath) {
        try {
            return Files.deleteIfExists(Paths.get(filePath));
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * 创建目录（递归创建）
     * @param dirPath 目录路径
     * @return 创建的目录对象
     * @throws IOException 如果创建失败
     */
    public static Path createDirectory(String dirPath) throws IOException {
        return Files.createDirectories(Paths.get(dirPath));
    }
    
    /**
     * 检查文件是否存在
     * @param filePath 文件路径
     * @return 文件是否存在
     */
    public static boolean fileExists(String filePath) {
        return Files.exists(Paths.get(filePath));
    }
    
    /**
     * 检查目录是否存在
     * @param dirPath 目录路径
     * @return 目录是否存在
     */
    public static boolean directoryExists(String dirPath) {
        Path path = Paths.get(dirPath);
        return Files.exists(path) && Files.isDirectory(path);
    }
    
    /**
     * 获取目录中的文件列表
     * @param dirPath 目录路径
     * @return 文件路径列表
     * @throws IOException 如果读取失败
     */
    public static List<String> listFiles(String dirPath) throws IOException {
        try (Stream<Path> stream = Files.list(Paths.get(dirPath))) {
            return stream.map(Path::toString).collect(Collectors.toList());
        }
    }
    
    /**
     * 递归查找指定扩展名的文件
     * @param rootDir 根目录
     * @param extension 文件扩展名（如 ".cs", ".csproj"）
     * @return 文件路径列表
     * @throws IOException 如果查找失败
     */
    public static List<String> findFilesByExtension(String rootDir, String extension) throws IOException {
        List<String> result = new ArrayList<>();
        // 确保扩展名以点号开头，并创建一个final变量供lambda表达式使用
        final String finalExtension = extension.startsWith(".") ? extension.toLowerCase() : "." + extension.toLowerCase();
        
        try (Stream<Path> walk = Files.walk(Paths.get(rootDir))) {
            walk.filter(Files::isRegularFile)
                .filter(p -> p.toString().toLowerCase().endsWith(finalExtension))
                .forEach(p -> result.add(p.toString()));
        }
        
        return result;
    }
    
    /**
     * 获取相对路径
     * @param basePath 基础路径
     * @param targetPath 目标路径
     * @return 相对路径
     * @throws IOException 如果路径无法解析
     */
    public static String getRelativePath(String basePath, String targetPath) throws IOException {
        Path base = Paths.get(basePath);
        Path target = Paths.get(targetPath);
        return base.relativize(target).toString();
    }
    
    /**
     * 获取文件扩展名
     * @param filePath 文件路径
     * @return 文件扩展名（不包含点号），如果没有扩展名则返回空字符串
     */
    public static String getFileExtension(String filePath) {
        int lastDotIndex = filePath.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < filePath.length() - 1) {
            return filePath.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }
    
    /**
     * 获取文件名（不包含扩展名）
     * @param filePath 文件路径
     * @return 文件名（不包含扩展名）
     */
    public static String getFileNameWithoutExtension(String filePath) {
        String fileName = Paths.get(filePath).getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return fileName.substring(0, lastDotIndex);
        }
        return fileName;
    }
    
    /**
     * 确保目录存在，如果不存在则创建
     * @param dirPath 目录路径
     * @return 目录是否已存在或创建成功
     */
    public static boolean ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        return dir.exists() && dir.isDirectory() || dir.mkdirs();
    }
    
    /**
     * 清理目录（删除所有内容但保留目录本身）
     * @param dirPath 目录路径
     * @throws IOException 如果清理失败
     */
    public static void cleanDirectory(String dirPath) throws IOException {
        Path dir = Paths.get(dirPath);
        if (Files.exists(dir) && Files.isDirectory(dir)) {
            try (Stream<Path> paths = Files.walk(dir)) {
                paths.sorted((a, b) -> b.compareTo(a)) // 先删除子目录和文件
                     .forEach(p -> {
                         try {
                             if (!p.equals(dir)) {
                                 Files.delete(p);
                             }
                         } catch (IOException e) {
                             throw new RuntimeException("Failed to delete " + p, e);
                         }
                     });
            }
        }
    }
    
    /**
     * 获取文件大小
     * @param filePath 文件路径
     * @return 文件大小（字节）
     * @throws IOException 如果获取失败
     */
    public static long getFileSize(String filePath) throws IOException {
        return Files.size(Paths.get(filePath));
    }
}