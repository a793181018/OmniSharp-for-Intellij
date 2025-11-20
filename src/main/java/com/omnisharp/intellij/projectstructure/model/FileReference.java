package com.omnisharp.intellij.projectstructure.model;

/**
 * 文件引用（如.dll、.exe等）
 */
public class FileReference {
    private final String path;
    private final String hintPath;
    private final boolean isPrivate;
    private final boolean specificVersion;

    public FileReference(String path, String hintPath) {
        this(path, hintPath, true, false);
    }

    public FileReference(String path, String hintPath, boolean isPrivate, boolean specificVersion) {
        this.path = path;
        this.hintPath = hintPath;
        this.isPrivate = isPrivate;
        this.specificVersion = specificVersion;
    }

    public String getPath() {
        return path;
    }

    public String getHintPath() {
        return hintPath;
    }

    public boolean isPrivate() {
        return isPrivate;
    }

    public boolean isSpecificVersion() {
        return specificVersion;
    }

    @Override
    public String toString() {
        return path + (hintPath != null ? " (Hint: " + hintPath + ")" : "");
    }
}