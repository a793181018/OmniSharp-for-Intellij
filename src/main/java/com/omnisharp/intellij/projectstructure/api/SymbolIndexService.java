package com.omnisharp.intellij.projectstructure.api;

import com.omnisharp.intellij.projectstructure.model.SymbolInfo;
import com.omnisharp.intellij.projectstructure.model.SymbolKind;
import java.util.List;
import java.util.Set;

/**
 * 符号索引和搜索服务接口
 */
public interface SymbolIndexService {
    /**
     * 索引整个解决方案中的符号
     * @param solutionId 解决方案ID
     * @return 是否索引成功
     */
    boolean indexSolution(String solutionId);

    /**
     * 索引单个项目中的符号
     * @param projectId 项目ID
     * @return 是否索引成功
     */
    boolean indexProject(String projectId);

    /**
     * 索引单个文件中的符号
     * @param projectId 项目ID
     * @param filePath 文件路径
     * @return 是否索引成功
     */
    boolean indexFile(String projectId, String filePath);

    /**
     * 搜索符号
     * @param query 搜索关键词
     * @return 匹配的符号信息列表
     */
    List<SymbolInfo> searchSymbols(String query);

    /**
     * 高级符号搜索
     * @param query 搜索关键词
     * @param symbolKinds 符号类型过滤
     * @param projectIds 项目ID过滤
     * @param caseSensitive 是否区分大小写
     * @param exactMatch 是否精确匹配
     * @return 匹配的符号信息列表
     */
    List<SymbolInfo> searchSymbols(String query, Set<SymbolKind> symbolKinds, Set<String> projectIds,
                                  boolean caseSensitive, boolean exactMatch);

    /**
     * 根据全限定名查找符号
     * @param fullyQualifiedName 全限定名
     * @return 匹配的符号信息
     */
    List<SymbolInfo> findSymbolByFullyQualifiedName(String fullyQualifiedName);

    /**
     * 获取文件中的所有符号
     * @param filePath 文件路径
     * @return 符号信息列表
     */
    List<SymbolInfo> getSymbolsInFile(String filePath);

    /**
     * 获取项目中的所有符号
     * @param projectId 项目ID
     * @return 符号信息列表
     */
    List<SymbolInfo> getSymbolsInProject(String projectId);

    /**
     * 获取符号的引用位置
     * @param symbolId 符号ID
     * @return 符号引用位置列表
     */
    List<SymbolReference> findReferences(String symbolId);

    /**
     * 更新符号索引（增量更新）
     * @param changedFiles 变更的文件路径列表
     * @return 是否更新成功
     */
    boolean updateIndex(List<String> changedFiles);

    /**
     * 清除符号索引
     * @param solutionId 解决方案ID
     * @return 是否清除成功
     */
    boolean clearIndex(String solutionId);

    /**
     * 获取索引统计信息
     * @param solutionId 解决方案ID
     * @return 索引统计信息
     */
    IndexStats getIndexStatistics(String solutionId);

    /**
     * 检查索引是否存在
     * @param solutionId 解决方案ID
     * @return 索引是否存在
     */
    boolean isIndexAvailable(String solutionId);

    /**
     * 索引进度回调接口
     */
    interface IndexProgressListener {
        void onProgress(int progress, String status);
        void onComplete(boolean success, String message);
    }

    /**
     * 添加索引进度监听器
     * @param listener 进度监听器
     */
    void addIndexProgressListener(IndexProgressListener listener);

    /**
     * 移除索引进度监听器
     * @param listener 进度监听器
     */
    void removeIndexProgressListener(IndexProgressListener listener);

    /**
     * 符号引用位置信息
     */
    class SymbolReference {
        private final String projectId;
        private final String filePath;
        private final int line;
        private final int column;
        private final String referenceType;

        public SymbolReference(String projectId, String filePath, int line, int column, String referenceType) {
            this.projectId = projectId;
            this.filePath = filePath;
            this.line = line;
            this.column = column;
            this.referenceType = referenceType;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getFilePath() {
            return filePath;
        }

        public int getLine() {
            return line;
        }

        public int getColumn() {
            return column;
        }

        public String getReferenceType() {
            return referenceType;
        }
    }

    /**
     * 索引统计信息
     */
    class IndexStats {
        private final int totalSymbols;
        private final int indexedFiles;
        private final int indexingTimeMs;
        private final int symbolsByKind[];

        public IndexStats(int totalSymbols, int indexedFiles, int indexingTimeMs, int[] symbolsByKind) {
            this.totalSymbols = totalSymbols;
            this.indexedFiles = indexedFiles;
            this.indexingTimeMs = indexingTimeMs;
            this.symbolsByKind = symbolsByKind;
        }

        public int getTotalSymbols() {
            return totalSymbols;
        }

        public int getIndexedFiles() {
            return indexedFiles;
        }

        public int getIndexingTimeMs() {
            return indexingTimeMs;
        }

        public int[] getSymbolsByKind() {
            return symbolsByKind;
        }
    }
}