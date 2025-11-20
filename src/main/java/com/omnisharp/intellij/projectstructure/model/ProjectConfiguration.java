package com.omnisharp.intellij.projectstructure.model;

import java.util.Collections;
import java.util.Map;

/**
 * 项目配置信息
 */
public class ProjectConfiguration {
    private final String name;
    private final String platform;
    private final boolean debugInfo;
    private final Map<String, String> properties;

    /**
     * 构造函数，接受所有参数
     * @param name 配置名称
     * @param platform 平台
     * @param debugInfo 是否包含调试信息
     * @param properties 配置属性
     */
    public ProjectConfiguration(String name, String platform, boolean debugInfo, Map<String, String> properties) {
        this.name = name;
        this.platform = platform;
        this.debugInfo = debugInfo;
        this.properties = properties;
    }

    /**
     * 构造函数，只接受name和platform参数
     * @param name 配置名称
     * @param platform 平台
     */
    public ProjectConfiguration(String name, String platform) {
        this.name = name;
        this.platform = platform;
        this.debugInfo = false;
        this.properties = Collections.emptyMap();
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