package com.omnisharp.intellij.projectstructure.parser;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 解析器工厂类，用于创建和管理各种项目文件解析器
 */
public class ParserFactory {
    private static final ParserFactory INSTANCE = new ParserFactory();
    
    // 缓存已创建的解析器实例
    private final Map<Class<?>, Object> parserInstances = new ConcurrentHashMap<>();
    
    // 文件扩展名到解析器类型的映射
    private final Map<String, Class<?>> extensionToParserMap = new HashMap<>();
    
    /**
     * 私有构造函数，初始化映射关系
     */
    private ParserFactory() {
        // 注册文件扩展名与解析器的映射关系
        extensionToParserMap.put(".sln", SolutionParser.class);
        extensionToParserMap.put(".csproj", ProjectParser.class);
        extensionToParserMap.put(".vbproj", ProjectParser.class);
        extensionToParserMap.put(".fsproj", ProjectParser.class);
        
        // 预初始化常用解析器
        registerParserImplementation(SolutionParser.class, SlnParser.class);
        registerParserImplementation(ProjectParser.class, CsprojParser.class);
    }

    /**
     * 获取ParserFactory单例实例
     * @return ParserFactory实例
     */
    public static ParserFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 根据文件路径获取合适的解析器
     * @param filePath 文件路径
     * @return 对应的解析器实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getParserForFile(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }
        
        String fileName = filePath.getFileName().toString().toLowerCase();
        
        // 查找对应的解析器类型
        for (Map.Entry<String, Class<?>> entry : extensionToParserMap.entrySet()) {
            if (fileName.endsWith(entry.getKey())) {
                return (T) getParser(entry.getValue());
            }
        }
        
        throw new IllegalArgumentException("No parser found for file type: " + fileName);
    }

    /**
     * 获取指定类型的解析器
     * @param parserType 解析器接口类型
     * @return 解析器实例
     */
    @SuppressWarnings("unchecked")
    public <T> T getParser(Class<T> parserType) {
        return (T) parserInstances.computeIfAbsent(parserType, this::createParser);
    }

    /**
     * 注册解析器实现
     * @param interfaceType 接口类型
     * @param implementationType 实现类类型
     */
    @SuppressWarnings("unchecked")
    public <T> void registerParserImplementation(Class<T> interfaceType, Class<? extends T> implementationType) {
        try {
            T instance = implementationType.getDeclaredConstructor().newInstance();
            parserInstances.put(interfaceType, instance);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create parser instance: " + implementationType.getName(), e);
        }
    }

    /**
     * 创建解析器实例
     * @param parserType 解析器类型
     * @return 解析器实例
     */
    private Object createParser(Class<?> parserType) {
        // 尝试通过SPI机制加载实现
        for (Object parser : ServiceLoader.load(parserType)) {
            return parser;
        }
        
        // 如果SPI没有找到实现，则尝试创建默认实现
        if (parserType == SolutionParser.class) {
            return new SlnParser();
        } else if (parserType == ProjectParser.class) {
            return new CsprojParser();
        }
        
        throw new IllegalArgumentException("No implementation found for parser type: " + parserType.getName());
    }

    /**
     * 检查文件是否为解决方案文件
     * @param filePath 文件路径
     * @return 是否为解决方案文件
     */
    public boolean isSolutionFile(Path filePath) {
        return filePath != null && 
               filePath.toString().toLowerCase().endsWith(".sln");
    }

    /**
     * 检查文件是否为项目文件
     * @param filePath 文件路径
     * @return 是否为项目文件
     */
    public boolean isProjectFile(Path filePath) {
        if (filePath == null) {
            return false;
        }
        
        String fileName = filePath.toString().toLowerCase();
        return fileName.endsWith(".csproj") || 
               fileName.endsWith(".vbproj") || 
               fileName.endsWith(".fsproj");
    }

    /**
     * 重置解析器缓存
     */
    public void reset() {
        parserInstances.clear();
    }

    /**
     * 获取解决方案解析器
     * @return SolutionParser实例
     */
    public SolutionParser getSolutionParser() {
        return getParser(SolutionParser.class);
    }

    /**
     * 获取项目解析器
     * @return ProjectParser实例
     */
    public ProjectParser getProjectParser() {
        return getParser(ProjectParser.class);
    }
}