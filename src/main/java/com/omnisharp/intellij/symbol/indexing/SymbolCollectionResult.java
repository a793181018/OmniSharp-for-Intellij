package com.omnisharp.intellij.symbol.indexing;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 符号收集结果类
 * 存储从项目中收集的所有符号信息
 */
public class SymbolCollectionResult {
    /** 项目名称到符号列表的映射 */
    private final Map<String, List<OmniSharpSymbol>> symbols;
    /** 收集过程中出现错误的文件列表 */
    private final List<String> errors;
    /** 收集的符号总数 */
    private final int totalSymbols;

    /**
     * 构造函数
     * @param symbols 项目名称到符号列表的映射
     * @param errors 收集过程中出现错误的文件列表
     */
    public SymbolCollectionResult(Map<String, List<OmniSharpSymbol>> symbols, List<String> errors) {
        this.symbols = symbols != null ? new HashMap<>(symbols) : new HashMap<>();
        this.errors = errors != null ? List.copyOf(errors) : Collections.emptyList();
        
        // 计算符号总数
        this.totalSymbols = symbols != null ? 
                symbols.values().stream().mapToInt(List::size).sum() : 0;
    }

    /**
     * 获取项目名称到符号列表的映射
     * @return 不可修改的映射
     */
    public Map<String, List<OmniSharpSymbol>> getSymbols() {
        // 返回每个项目符号列表的不可修改视图
        Map<String, List<OmniSharpSymbol>> result = new HashMap<>();
        symbols.forEach((project, symbolList) -> {
            result.put(project, Collections.unmodifiableList(symbolList));
        });
        return Collections.unmodifiableMap(result);
    }

    /**
     * 获取指定项目的符号列表
     * @param projectName 项目名称
     * @return 符号列表，如果项目不存在则返回空列表
     */
    public List<OmniSharpSymbol> getSymbolsByProject(String projectName) {
        return symbols.getOrDefault(projectName, Collections.emptyList());
    }

    /**
     * 获取收集过程中出现错误的文件列表
     * @return 不可修改的错误文件列表
     */
    public List<String> getErrors() {
        return errors;
    }

    /**
     * 获取收集的符号总数
     * @return 符号总数
     */
    public int getTotalSymbols() {
        return totalSymbols;
    }

    /**
     * 获取包含符号的项目数量
     * @return 项目数量
     */
    public int getProjectCount() {
        return symbols.size();
    }

    /**
     * 判断是否有错误发生
     * @return 如果有错误则返回true
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public String toString() {
        return "SymbolCollectionResult{" +
                "totalSymbols=" + totalSymbols +
                ", projectCount=" + getProjectCount() +
                ", errorsCount=" + errors.size() +
                "}";
    }
}