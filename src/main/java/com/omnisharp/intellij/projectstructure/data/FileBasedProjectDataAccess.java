package com.omnisharp.intellij.projectstructure.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.omnisharp.intellij.projectstructure.model.ProjectModel;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于文件系统的项目数据访问实现
 */
public class FileBasedProjectDataAccess implements ProjectDataAccess {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CACHE_DIR_NAME = ".omnisharp_cache";
    
    // 缓存目录路径
    private final Path cacheDir;
    
    // 内存缓存，提高性能
    private final ConcurrentHashMap<Path, SolutionModel> solutionCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Path, ProjectModel> projectCache = new ConcurrentHashMap<>();
    
    /**
     * 默认构造函数，使用用户目录作为基础
     */
    public FileBasedProjectDataAccess() {
        this.cacheDir = Paths.get(System.getProperty("user.home"), CACHE_DIR_NAME);
        initializeCacheDirectory();
    }
    
    /**
     * 构造函数，使用指定的缓存目录
     * @param baseCacheDir 基础缓存目录
     */
    public FileBasedProjectDataAccess(Path baseCacheDir) {
        this.cacheDir = baseCacheDir.resolve(CACHE_DIR_NAME);
        initializeCacheDirectory();
    }
    
    /**
     * 初始化缓存目录
     */
    private void initializeCacheDirectory() {
        try {
            if (!Files.exists(cacheDir)) {
                Files.createDirectories(cacheDir);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create cache directory: " + cacheDir, e);
        }
    }
    
    /**
     * 获取解决方案缓存文件路径
     * @param solutionPath 解决方案路径
     * @return 缓存文件路径
     */
    private Path getSolutionCachePath(Path solutionPath) {
        String fileName = generateCacheFileName(solutionPath);
        return cacheDir.resolve("solutions").resolve(fileName + ".json");
    }
    
    /**
     * 获取项目缓存文件路径
     * @param projectPath 项目路径
     * @return 缓存文件路径
     */
    private Path getProjectCachePath(Path projectPath) {
        String fileName = generateCacheFileName(projectPath);
        return cacheDir.resolve("projects").resolve(fileName + ".json");
    }
    
    /**
     * 生成缓存文件名
     * @param path 文件路径
     * @return 缓存文件名
     */
    private String generateCacheFileName(Path path) {
        // 使用路径的哈希值作为文件名，确保唯一性
        String normalizedPath = path.normalize().toString();
        return Integer.toHexString(normalizedPath.hashCode());
    }
    
    @Override
    public void saveSolution(SolutionModel solutionModel) throws IOException {
        Path solutionPath = Paths.get(solutionModel.getPath());
        Path cacheFilePath = getSolutionCachePath(solutionPath);
        
        // 确保父目录存在
        Files.createDirectories(cacheFilePath.getParent());
        
        // 序列化并保存到文件
        String json = GSON.toJson(solutionModel);
        Files.writeString(cacheFilePath, json);
        
        // 更新内存缓存
        solutionCache.put(solutionPath, solutionModel);
    }
    
    @Override
    public Optional<SolutionModel> loadSolution(Path solutionPath) {
        // 先尝试从内存缓存获取
        SolutionModel cachedSolution = solutionCache.get(solutionPath);
        if (cachedSolution != null) {
            return Optional.of(cachedSolution);
        }
        
        Path cacheFilePath = getSolutionCachePath(solutionPath);
        if (!Files.exists(cacheFilePath)) {
            return Optional.empty();
        }
        
        try {
            // 从文件加载并反序列化
            String json = Files.readString(cacheFilePath);
            SolutionModel solutionModel = GSON.fromJson(json, SolutionModel.class);
            
            // 更新内存缓存
            solutionCache.put(solutionPath, solutionModel);
            
            return Optional.of(solutionModel);
        } catch (IOException e) {
            // 文件读取失败，不抛出异常而是返回空
            return Optional.empty();
        }
    }
    
    @Override
    public void saveProject(ProjectModel projectModel) throws IOException {
        Path projectPath = Paths.get(projectModel.getPath());
        Path cacheFilePath = getProjectCachePath(projectPath);
        
        // 确保父目录存在
        Files.createDirectories(cacheFilePath.getParent());
        
        // 序列化并保存到文件
        String json = GSON.toJson(projectModel);
        Files.writeString(cacheFilePath, json);
        
        // 更新内存缓存
        projectCache.put(projectPath, projectModel);
    }
    
    @Override
    public Optional<ProjectModel> loadProject(Path projectPath) {
        // 先尝试从内存缓存获取
        ProjectModel cachedProject = projectCache.get(projectPath);
        if (cachedProject != null) {
            return Optional.of(cachedProject);
        }
        
        Path cacheFilePath = getProjectCachePath(projectPath);
        if (!Files.exists(cacheFilePath)) {
            return Optional.empty();
        }
        
        try {
            // 从文件加载并反序列化
            String json = Files.readString(cacheFilePath);
            ProjectModel projectModel = GSON.fromJson(json, ProjectModel.class);
            
            // 更新内存缓存
            projectCache.put(projectPath, projectModel);
            
            return Optional.of(projectModel);
        } catch (IOException e) {
            // 文件读取失败，不抛出异常而是返回空
            return Optional.empty();
        }
    }
    
    @Override
    public boolean deleteSolution(Path solutionPath) {
        try {
            Path cacheFilePath = getSolutionCachePath(solutionPath);
            boolean deleted = Files.deleteIfExists(cacheFilePath);
            
            // 清理内存缓存
            solutionCache.remove(solutionPath);
            
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public boolean deleteProject(Path projectPath) {
        try {
            Path cacheFilePath = getProjectCachePath(projectPath);
            boolean deleted = Files.deleteIfExists(cacheFilePath);
            
            // 清理内存缓存
            projectCache.remove(projectPath);
            
            return deleted;
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public boolean existsSolution(Path solutionPath) {
        // 先检查内存缓存
        if (solutionCache.containsKey(solutionPath)) {
            return true;
        }
        
        // 检查文件缓存
        Path cacheFilePath = getSolutionCachePath(solutionPath);
        return Files.exists(cacheFilePath);
    }
    
    @Override
    public boolean existsProject(Path projectPath) {
        // 先检查内存缓存
        if (projectCache.containsKey(projectPath)) {
            return true;
        }
        
        // 检查文件缓存
        Path cacheFilePath = getProjectCachePath(projectPath);
        return Files.exists(cacheFilePath);
    }
    
    @Override
    public void close() {
        // 清理内存缓存
        solutionCache.clear();
        projectCache.clear();
        
        // 不需要关闭文件句柄，因为我们使用的是Files工具类，它会自动关闭资源
    }
    
    /**
     * 清理指定解决方案的所有相关缓存
     * @param solutionPath 解决方案路径
     */
    public void clearSolutionCache(Path solutionPath) {
        deleteSolution(solutionPath);
        
        // 可以在这里添加逻辑来清理与该解决方案相关的所有项目缓存
    }
    
    /**
     * 清理所有缓存
     */
    public void clearAllCache() {
        try {
            // 清理内存缓存
            solutionCache.clear();
            projectCache.clear();
            
            // 清理文件缓存
            if (Files.exists(cacheDir.resolve("solutions"))) {
                deleteDirectory(cacheDir.resolve("solutions"));
            }
            if (Files.exists(cacheDir.resolve("projects"))) {
                deleteDirectory(cacheDir.resolve("projects"));
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to clear cache", e);
        }
    }
    
    /**
     * 删除目录及其所有内容
     * @param directory 目录路径
     * @throws IOException 如果删除失败
     */
    private void deleteDirectory(Path directory) throws IOException {
        if (Files.isDirectory(directory)) {
            try (var paths = Files.walk(directory)) {
                paths.sorted(java.util.Comparator.reverseOrder())
                     .map(java.nio.file.Path::toFile)
                     .forEach(java.io.File::delete);
            }
        }
    }
}