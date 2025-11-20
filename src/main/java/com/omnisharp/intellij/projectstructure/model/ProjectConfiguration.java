package com.omnisharp.intellij.projectstructure.model;

import java.util.Map;

/**
 * 项目配置信息
 */
public class ProjectConfiguration {
    private final String name;
    private final String platform;
    private final boolean debugInfo;
    private final Map<String, String> properties;

    public ProjectConfiguration(String name, String platform, boolean debugInfo, Map<String, String> properties) {
        this.name = name;
        this.platform = platform;
        this.debugInfo = debugInfo;
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public String getPlatform() {
        return platform;
    }

    public boolean isDebugInfo() {
        return debugInfo;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String key) {
        return properties.get(key);
    }
}