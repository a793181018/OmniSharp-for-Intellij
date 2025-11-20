package com.omnisharp.intellij.projectstructure.model;

import java.util.Collections;
import java.util.List;

/**
 * NuGet包引用
 */
public class PackageReference {
    private final String id;
    private final String version;
    private final List<String> includeAssets;
    private final List<String> excludeAssets;

    public PackageReference(String id, String version) {
        this(id, version, Collections.emptyList(), Collections.emptyList());
    }

    public PackageReference(String id, String version, List<String> includeAssets, List<String> excludeAssets) {
        this.id = id;
        this.version = version;
        this.includeAssets = includeAssets != null ? includeAssets : Collections.emptyList();
        this.excludeAssets = excludeAssets != null ? excludeAssets : Collections.emptyList();
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public List<String> getIncludeAssets() {
        return includeAssets;
    }

    public List<String> getExcludeAssets() {
        return excludeAssets;
    }

    @Override
    public String toString() {
        return id + " (" + version + ")";
    }
}