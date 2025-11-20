package com.omnisharp.intellij.projectstructure.data;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 数据层模块配置，用于依赖注入
 */
public class DataModule extends AbstractModule {
    private final Path cacheDir;

    /**
     * 默认构造函数，使用用户目录作为缓存基础目录
     */
    public DataModule() {
        this.cacheDir = Paths.get(System.getProperty("user.home"));
    }

    /**
     * 构造函数，使用指定的缓存目录
     * @param cacheDir 缓存目录路径
     */
    public DataModule(Path cacheDir) {
        this.cacheDir = cacheDir;
    }

    @Override
    protected void configure() {
        // 配置ProjectDataAccess实现
        bind(ProjectDataAccess.class)
                .to(FileBasedProjectDataAccess.class)
                .in(Singleton.class);

        // 配置ProjectRepository
        bind(ProjectRepository.class)
                .in(Singleton.class);

        // 提供缓存目录
        bind(Path.class)
                .annotatedWith(CacheDirectory.class)
                .toInstance(cacheDir);
    }

    /**
     * 缓存目录注解，用于依赖注入
     */
    public static @interface CacheDirectory {
        // 标记接口
    }

    /**
     * 创建默认的数据模块
     * @return 默认数据模块
     */
    public static DataModule createDefault() {
        return new DataModule();
    }

    /**
     * 创建自定义缓存目录的数据模块
     * @param cacheDirPath 缓存目录路径
     * @return 自定义数据模块
     */
    public static DataModule createWithCustomCache(String cacheDirPath) {
        return new DataModule(Paths.get(cacheDirPath));
    }
}