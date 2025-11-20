package com.omnisharp.intellij.projectstructure.api;

import java.nio.file.Path;
import java.util.EventListener;

/**
 * 文件系统变更监听器接口
 */
public interface FileSystemListener extends EventListener {
    /**
     * 文件创建事件
     * @param file 创建的文件路径
     */
    void onFileCreated(Path file);

    /**
     * 文件删除事件
     * @param file 删除的文件路径
     */
    void onFileDeleted(Path file);

    /**
     * 文件修改事件
     * @param file 修改的文件路径
     */
    void onFileModified(Path file);

    /**
     * 文件移动事件
     * @param oldPath 原文件路径
     * @param newPath 新文件路径
     */
    void onFileMoved(Path oldPath, Path newPath);

    /**
     * 文件夹创建事件
     * @param folder 创建的文件夹路径
     */
    void onFolderCreated(Path folder);

    /**
     * 文件夹删除事件
     * @param folder 删除的文件夹路径
     */
    void onFolderDeleted(Path folder);

    /**
     * 文件夹修改事件
     * @param folder 修改的文件夹路径
     */
    void onFolderModified(Path folder);

    /**
     * 文件夹移动事件
     * @param oldPath 原文件夹路径
     * @param newPath 新文件夹路径
     */
    void onFolderMoved(Path oldPath, Path newPath);

    /**
     * 批量文件变更事件
     * @param event 批量变更事件对象
     */
    void onBatchChange(FileSystemBatchEvent event);

    /**
     * 文件系统变更事件过滤器，用于决定哪些文件变更需要被处理
     */
    interface FileSystemEventFilter {
        /**
         * 判断是否应该处理指定路径的变更事件
         * @param path 文件或文件夹路径
         * @return 是否应该处理
         */
        boolean shouldProcess(Path path);

        /**
         * 获取文件扩展名过滤器
         * @return 需要监听的文件扩展名列表，如[.cs, .vb, .csproj]
         */
        String[] getFileExtensions();

        /**
         * 是否排除临时文件
         * @return 是否排除临时文件
         */
        boolean excludeTemporaryFiles();
    }

    /**
     * 批量文件变更事件
     */
    class FileSystemBatchEvent {
        private final java.util.List<Path> createdFiles;
        private final java.util.List<Path> deletedFiles;
        private final java.util.List<Path> modifiedFiles;
        private final java.util.List<FileMoveEvent> movedFiles;
        private final java.util.List<Path> createdFolders;
        private final java.util.List<Path> deletedFolders;
        private final java.util.List<Path> modifiedFolders;
        private final java.util.List<FileMoveEvent> movedFolders;

        public FileSystemBatchEvent() {
            this.createdFiles = new java.util.ArrayList<>();
            this.deletedFiles = new java.util.ArrayList<>();
            this.modifiedFiles = new java.util.ArrayList<>();
            this.movedFiles = new java.util.ArrayList<>();
            this.createdFolders = new java.util.ArrayList<>();
            this.deletedFolders = new java.util.ArrayList<>();
            this.modifiedFolders = new java.util.ArrayList<>();
            this.movedFolders = new java.util.ArrayList<>();
        }

        public java.util.List<Path> getCreatedFiles() {
            return createdFiles;
        }

        public java.util.List<Path> getDeletedFiles() {
            return deletedFiles;
        }

        public java.util.List<Path> getModifiedFiles() {
            return modifiedFiles;
        }

        public java.util.List<FileMoveEvent> getMovedFiles() {
            return movedFiles;
        }

        public java.util.List<Path> getCreatedFolders() {
            return createdFolders;
        }

        public java.util.List<Path> getDeletedFolders() {
            return deletedFolders;
        }

        public java.util.List<Path> getModifiedFolders() {
            return modifiedFolders;
        }

        public java.util.List<FileMoveEvent> getMovedFolders() {
            return movedFolders;
        }

        public boolean isEmpty() {
            return createdFiles.isEmpty() && deletedFiles.isEmpty() && modifiedFiles.isEmpty() && 
                   movedFiles.isEmpty() && createdFolders.isEmpty() && deletedFolders.isEmpty() && 
                   modifiedFolders.isEmpty() && movedFolders.isEmpty();
        }

        public static class FileMoveEvent {
            private final Path oldPath;
            private final Path newPath;

            public FileMoveEvent(Path oldPath, Path newPath) {
                this.oldPath = oldPath;
                this.newPath = newPath;
            }

            public Path getOldPath() {
                return oldPath;
            }

            public Path getNewPath() {
                return newPath;
            }
        }
    }
}