package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

/**
 * 演示如何使用解决方案解析器
 */
public class SolutionParserExample {

    /**
     * 示例：使用默认解析器同步解析解决方案
     */
    public void exampleDefaultParserSync(String solutionPath) {
        try {
            SolutionParserFacade parser = new DefaultSolutionParserFacade();
            Path path = Paths.get(solutionPath);
            SolutionModel solution = parser.parse(path);
            
            // 处理解析结果
            System.out.println("解析成功：" + solution.getName());
            System.out.println("项目数量：" + solution.getProjectCount());
            
            // 获取配置信息
            solution.getConfiguration().forEach((key, value) -> {
                System.out.println("配置: " + key + " = " + value);
            });
            
        } catch (ParseException e) {
            System.err.println("解析失败: " + e.getMessage());
        }
    }

    /**
     * 示例：使用默认解析器异步解析解决方案
     */
    public void exampleDefaultParserAsync(String solutionPath) {
        SolutionParserFacade parser = new DefaultSolutionParserFacade();
        Path path = Paths.get(solutionPath);
        
        parser.parseAsync(path)
                .thenAccept(solution -> {
                    // 处理解析结果
                    System.out.println("异步解析成功：" + solution.getName());
                    System.out.println("项目数量：" + solution.getProjectCount());
                })
                .exceptionally(e -> {
                    System.err.println("异步解析失败: " + e.getMessage());
                    return null;
                });
    }

    /**
     * 示例：使用带缓存的解析器
     */
    public void exampleCachingParser(String solutionPath) {
        try {
            // 创建带缓存的解析器
            SolutionParserFacade defaultParser = new DefaultSolutionParserFacade();
            CachingSolutionParserFacade parser = new CachingSolutionParserFacade(defaultParser);
            
            Path path = Paths.get(solutionPath);
            
            // 第一次解析（缓存未命中）
            SolutionModel solution1 = parser.parse(path);
            System.out.println("第一次解析成功，缓存大小: " + parser.getCacheSize());
            
            // 第二次解析（缓存命中）
            SolutionModel solution2 = parser.parse(path);
            System.out.println("第二次解析成功，缓存大小: " + parser.getCacheSize());
            
            // 验证是否为同一对象
            System.out.println("两次解析结果是否相同对象: " + (solution1 == solution2));
            
            // 清除缓存
            parser.clearCache();
            System.out.println("清除缓存后，缓存大小: " + parser.getCacheSize());
            
        } catch (ParseException e) {
            System.err.println("解析失败: " + e.getMessage());
        }
    }
}