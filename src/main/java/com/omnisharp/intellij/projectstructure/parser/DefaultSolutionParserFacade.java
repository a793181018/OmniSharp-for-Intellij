package com.omnisharp.intellij.projectstructure.parser;

import com.intellij.openapi.util.text.StringUtil;
import com.omnisharp.intellij.projectstructure.model.SolutionModel;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 解决方案文件解析器外观的默认实现
 */
public class DefaultSolutionParserFacade implements SolutionParserFacade {
    private final SlnParser parser;
    private final ExecutorService executorService;

    public DefaultSolutionParserFacade() {
        this.parser = new SlnParser();
        this.executorService = Executors.newCachedThreadPool();
    }

    @Override
    @NotNull
    public SolutionModel parse(@NotNull Path solutionPath) throws ParseException {
        try {
            return parser.parse(solutionPath);
        } catch (IOException e) {
            throw new ParseException("Failed to read solution file: " + solutionPath, e);
        } catch (Exception e) {
            if (e instanceof ParseException) {
                throw (ParseException) e;
            }
            throw new ParseException("Failed to parse solution file: " + solutionPath, e);
        }
    }

    @Override
    public CompletableFuture<SolutionModel> parseAsync(Path solutionPath) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return parse(solutionPath);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    @Override
    public void shutdown() {
        executorService.shutdown();
    }
}