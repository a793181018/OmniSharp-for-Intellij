package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.ProjectModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * 项目文件解析器外观的实现
 * 自动识别并使用适当的解析器（SDK风格或传统风格）
 */
public class OmniSharpProjectParserFacade implements ProjectParserFacade {
    private static final Pattern SDK_PROJECT_PATTERN = Pattern.compile("<Project Sdk=\"[\"]*\">", Pattern.CASE_INSENSITIVE);
    private static final Pattern SDK_PROJECT_PATTERN2 = Pattern.compile("<Sdk\s+=\"[^\"]*\"\s*/>", Pattern.CASE_INSENSITIVE);
    
    private final ProjectParser sdkStyleParser;
    private final ProjectParser legacyParser;
    private final ExecutorService executorService;
    private final Map<Path, ProjectModel> projectCache;
    private final Map<Path, Long> projectTimestamps;
    
    /**
     * 构造函数
     * @param sdkStyleParser SDK风格项目解析器
     * @param legacyParser 传统风格项目解析器
     */
    public OmniSharpProjectParserFacade(ProjectParser sdkStyleParser, ProjectParser legacyParser) {
        this.sdkStyleParser = sdkStyleParser;
        this.legacyParser = legacyParser;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.projectCache = new ConcurrentHashMap<>();
        this.projectTimestamps = new ConcurrentHashMap<>();
    }
    
    /**
     * 默认构造函数，创建默认的解析器实例
     */
    public OmniSharpProjectParserFacade() {
        this(new SdkStyleProjectParser(), new LegacyProjectParser());
    }
    
    @Override
    public ProjectModel parse(Path projectPath) throws ParseException {
        validateProjectPath(projectPath);
        
        // 检查是否有缓存且文件未修改
        if (projectCache.containsKey(projectPath)) {
            long currentTimestamp = getProjectFileTimestamp(projectPath);
            Long cachedTimestamp = projectTimestamps.get(projectPath);
            if (cachedTimestamp != null && cachedTimestamp == currentTimestamp) {
                return projectCache.get(projectPath);
            }
        }
        
        try {
            // 根据项目类型选择适当的解析器
            ProjectParser parser = isSdkStyleProject(projectPath) ? sdkStyleParser : legacyParser;
            ProjectModel projectModel = parser.parse(projectPath);
            
            // 更新缓存
            projectCache.put(projectPath, projectModel);
            projectTimestamps.put(projectPath, getProjectFileTimestamp(projectPath));
            
            return projectModel;
        } catch (IOException e) {
            throw new ParseException("Failed to parse project file: " + e.getMessage(), e);
        } catch (SolutionParser.ParseException e) {
            throw new ParseException("Failed to parse project file: " + e.getMessage(), e);
        }
    }
    
    @Override
    public CompletableFuture<ProjectModel> parseAsync(Path projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return parse(projectPath);
            } catch (ParseException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    @Override
    public ProjectModel reload(Path projectPath) throws ParseException {
        // 移除缓存，强制重新解析
        projectCache.remove(projectPath);
        projectTimestamps.remove(projectPath);
        return parse(projectPath);
    }
    
    @Override
    public CompletableFuture<ProjectModel> reloadAsync(Path projectPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reload(projectPath);
            } catch (ParseException e) {
                throw new CompletionException(e);
            }
        }, executorService);
    }
    
    @Override
    public boolean isSdkStyleProject(Path projectPath) {
        try {
            String content = new String(Files.readAllBytes(projectPath));
            return SDK_PROJECT_PATTERN.matcher(content).find() || SDK_PROJECT_PATTERN2.matcher(content).find();
        } catch (IOException e) {
            return false;
        }
    }
    
    @Override
    public long getProjectFileTimestamp(Path projectPath) {
        try {
            return Files.getLastModifiedTime(projectPath).toMillis();
        } catch (IOException e) {
            return 0;
        }
    }
    
    @Override
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        projectCache.clear();
        projectTimestamps.clear();
    }
    
    /**
     * 验证项目文件路径
     * @param projectPath 项目文件路径
     * @throws ParseException 如果路径无效
     */
    private void validateProjectPath(Path projectPath) throws ParseException {
        if (projectPath == null) {
            throw new ParseException("Project path cannot be null");
        }
        
        if (!Files.exists(projectPath)) {
            throw new ParseException("Project file does not exist: " + projectPath);
        }
        
        if (!Files.isRegularFile(projectPath)) {
            throw new ParseException("Path is not a file: " + projectPath);
        }
        
        String fileName = projectPath.getFileName().toString();
        if (!(fileName.endsWith(".csproj") || fileName.endsWith(".vbproj") || fileName.endsWith(".fsproj"))) {
            throw new ParseException("Not a recognized project file: " + fileName);
        }
    }
    
    /**
     * 获取缓存的项目模型
     * @param projectPath 项目文件路径
     * @return 缓存的项目模型，如果不存在返回null
     */
    public ProjectModel getCachedProject(Path projectPath) {
        return projectCache.get(projectPath);
    }
    
    /**
     * 清除项目缓存
     * @param projectPath 项目文件路径
     */
    public void clearCache(Path projectPath) {
        projectCache.remove(projectPath);
        projectTimestamps.remove(projectPath);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        projectCache.clear();
        projectTimestamps.clear();
    }
}