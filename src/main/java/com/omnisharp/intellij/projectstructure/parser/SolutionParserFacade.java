package com.omnisharp.intellij.projectstructure.parser;

import com.omnisharp.intellij.projectstructure.model.SolutionModel;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

/**
 * 解决方案文件解析器的外观接口
 */
public interface SolutionParserFacade {
    /**
     * 解析解决方案文件
     * @param solutionPath 解决方案文件路径
     * @return 解析后的解决方案模型
     * @throws ParseException 解析异常
     */
    SolutionModel parse(Path solutionPath) throws ParseException;

    /**
     * 异步解析解决方案文件
     * @param solutionPath 解决方案文件路径
     * @return 包含解析结果的CompletableFuture
     */
    CompletableFuture<SolutionModel> parseAsync(Path solutionPath);
    
    /**
     * 关闭解析器资源
     */
    void shutdown();
}