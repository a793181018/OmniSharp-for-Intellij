package com.intellij.plugins.omnisharp.project.dependencies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本解析器
 * 用于解析、比较和管理NuGet包版本
 */
public class VersionResolver {
    private static final Logger logger = Logger.getLogger(VersionResolver.class.getName());
    
    // NuGet版本正则表达式
    private static final Pattern VERSION_PATTERN = Pattern.compile(
            "^(\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(?:[-+](.+))?$",
            Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 解析版本字符串
     * @param versionStr 版本字符串
     * @return Version对象
     */
    public static Version parseVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            throw new IllegalArgumentException("版本字符串不能为空");
        }
        
        Matcher matcher = VERSION_PATTERN.matcher(versionStr.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("无效的版本格式: " + versionStr);
        }
        
        int major = Integer.parseInt(matcher.group(1));
        int minor = Integer.parseInt(matcher.group(2));
        int patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
        int revision = matcher.group(4) != null ? Integer.parseInt(matcher.group(4)) : 0;
        String suffix = matcher.group(5);
        
        return new Version(major, minor, patch, revision, suffix);
    }
    
    /**
     * 比较两个版本字符串
     * @param version1 第一个版本
     * @param version2 第二个版本
     * @return 如果version1 > version2返回正数，如果version1 < version2返回负数，如果相等返回0
     */
    public static int compareVersions(String version1, String version2) {
        Version v1 = parseVersion(version1);
        Version v2 = parseVersion(version2);
        return compareVersions(v1, v2);
    }
    
    /**
     * 比较两个Version对象
     */
    public static int compareVersions(Version v1, Version v2) {
        // 比较数字部分
        if (v1.getMajor() != v2.getMajor()) {
            return Integer.compare(v1.getMajor(), v2.getMajor());
        }
        if (v1.getMinor() != v2.getMinor()) {
            return Integer.compare(v1.getMinor(), v2.getMinor());
        }
        if (v1.getPatch() != v2.getPatch()) {
            return Integer.compare(v1.getPatch(), v2.getPatch());
        }
        if (v1.getRevision() != v2.getRevision()) {
            return Integer.compare(v1.getRevision(), v2.getRevision());
        }
        
        // 比较后缀部分
        if (v1.getSuffix() == null && v2.getSuffix() == null) {
            return 0;
        }
        if (v1.getSuffix() == null) {
            return 1; // 没有后缀的版本更高
        }
        if (v2.getSuffix() == null) {
            return -1;
        }
        
        // 比较后缀
        return compareVersionSuffixes(v1.getSuffix(), v2.getSuffix());
    }
    
    /**
     * 比较版本后缀
     */
    private static int compareVersionSuffixes(String suffix1, String suffix2) {
        // 常见的预发布标识符优先级
        List<String> preReleaseIdentifiers = Arrays.asList(
                "preview", "pre", "rc", "beta", "alpha", "dev"
        );
        
        String s1 = suffix1.toLowerCase();
        String s2 = suffix2.toLowerCase();
        
        // 检查是否是预发布标识符
        for (String identifier : preReleaseIdentifiers) {
            if (s1.contains(identifier) && !s2.contains(identifier)) {
                return -1;
            }
            if (!s1.contains(identifier) && s2.contains(identifier)) {
                return 1;
            }
            if (s1.contains(identifier) && s2.contains(identifier)) {
                // 提取数字部分进行比较
                int num1 = extractNumberFromSuffix(s1);
                int num2 = extractNumberFromSuffix(s2);
                return Integer.compare(num1, num2);
            }
        }
        
        // 默认按字典序比较
        return s1.compareTo(s2);
    }
    
    /**
     * 从后缀中提取数字部分
     */
    private static int extractNumberFromSuffix(String suffix) {
        Pattern numPattern = Pattern.compile("\\d+");
        Matcher matcher = numPattern.matcher(suffix);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
    
    /**
     * 判断版本字符串是否有效
     */
    public static boolean isValidVersion(String versionStr) {
        if (versionStr == null || versionStr.trim().isEmpty()) {
            return false;
        }
        return VERSION_PATTERN.matcher(versionStr.trim()).matches();
    }
    
    /**
     * 获取最新版本
     * @param versions 版本列表
     * @return 最新版本的字符串表示
     */
    public static String getLatestVersion(List<String> versions) {
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("版本列表不能为空");
        }
        
        String latestVersion = versions.get(0);
        for (String version : versions) {
            if (compareVersions(version, latestVersion) > 0) {
                latestVersion = version;
            }
        }
        
        return latestVersion;
    }
    
    /**
     * 解析版本范围
     * 支持常见的版本范围格式：
     * - 1.0.0 - 精确版本
     * - ^1.0.0 - 兼容版本，等效于 >=1.0.0 且 <2.0.0
     * - ~1.0.0 - 补丁版本，等效于 >=1.0.0 且 <1.1.0
     * - >=1.0.0 && <2.0.0 - 显式范围
     */
    public static VersionRange parseVersionRange(String rangeStr) {
        if (rangeStr == null || rangeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("版本范围不能为空");
        }
        
        String range = rangeStr.trim();
        
        // 精确版本
        if (isValidVersion(range)) {
            Version exact = parseVersion(range);
            return new VersionRange(exact, exact, true, true);
        }
        
        // 兼容版本 ^1.0.0
        if (range.startsWith("^")) {
            String versionPart = range.substring(1);
            Version baseVersion = parseVersion(versionPart);
            
            // 计算上限版本
            int nextMajor = baseVersion.getMajor() + 1;
            Version upperBound = new Version(nextMajor, 0, 0, 0, null);
            
            return new VersionRange(baseVersion, upperBound, true, false);
        }
        
        // 补丁版本 ~1.0.0
        if (range.startsWith("~")) {
            String versionPart = range.substring(1);
            Version baseVersion = parseVersion(versionPart);
            
            // 计算上限版本
            int nextMinor = baseVersion.getMinor() + 1;
            Version upperBound = new Version(baseVersion.getMajor(), nextMinor, 0, 0, null);
            
            return new VersionRange(baseVersion, upperBound, true, false);
        }
        
        // 处理显式范围 >=1.0.0 && <2.0.0
        if (range.contains("&&")) {
            String[] parts = range.split("&&");
            if (parts.length == 2) {
                String lowerPart = parts[0].trim();
                String upperPart = parts[1].trim();
                
                Version lower = null;
                boolean lowerInclusive = true;
                Version upper = null;
                boolean upperInclusive = false;
                
                if (lowerPart.startsWith(">=")) {
                    lower = parseVersion(lowerPart.substring(2));
                } else if (lowerPart.startsWith(">")) {
                    lower = parseVersion(lowerPart.substring(1));
                    lowerInclusive = false;
                } else if (lowerPart.startsWith("==")) {
                    lower = parseVersion(lowerPart.substring(2));
                    upper = lower;
                }
                
                if (upperPart.startsWith("<=")) {
                    upper = parseVersion(upperPart.substring(2));
                    upperInclusive = true;
                } else if (upperPart.startsWith("<")) {
                    upper = parseVersion(upperPart.substring(1));
                }
                
                if (lower != null && upper != null) {
                    return new VersionRange(lower, upper, lowerInclusive, upperInclusive);
                }
            }
        }
        
        throw new IllegalArgumentException("不支持的版本范围格式: " + rangeStr);
    }
    
    /**
     * 检查版本是否在指定范围内
     */
    public static boolean isVersionInRange(String version, String range) {
        Version v = parseVersion(version);
        VersionRange vr = parseVersionRange(range);
        return isVersionInRange(v, vr);
    }
    
    /**
     * 检查版本是否在指定范围内
     */
    public static boolean isVersionInRange(Version version, VersionRange range) {
        int lowerCompare = compareVersions(version, range.getLowerBound());
        int upperCompare = compareVersions(version, range.getUpperBound());
        
        boolean isAboveLower = (range.isLowerInclusive() && lowerCompare >= 0) || 
                              (!range.isLowerInclusive() && lowerCompare > 0);
        
        boolean isBelowUpper = (range.isUpperInclusive() && upperCompare <= 0) || 
                              (!range.isUpperInclusive() && upperCompare < 0);
        
        return isAboveLower && isBelowUpper;
    }
    
    /**
     * 版本类
     */
    public static class Version {
        private final int major;
        private final int minor;
        private final int patch;
        private final int revision;
        private final String suffix;
        
        public Version(int major, int minor, int patch, int revision, String suffix) {
            this.major = major;
            this.minor = minor;
            this.patch = patch;
            this.revision = revision;
            this.suffix = suffix;
        }
        
        public int getMajor() {
            return major;
        }
        
        public int getMinor() {
            return minor;
        }
        
        public int getPatch() {
            return patch;
        }
        
        public int getRevision() {
            return revision;
        }
        
        public String getSuffix() {
            return suffix;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(major).append(".").append(minor);
            
            // 只有当patch或revision大于0或有后缀时才添加
            if (patch > 0 || revision > 0 || suffix != null) {
                sb.append(".").append(patch);
                
                if (revision > 0 || suffix != null) {
                    sb.append(".").append(revision);
                    
                    if (suffix != null) {
                        sb.append("-").append(suffix);
                    }
                }
            }
            
            return sb.toString();
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            
            Version version = (Version) obj;
            return major == version.major &&
                   minor == version.minor &&
                   patch == version.patch &&
                   revision == version.revision &&
                   Objects.equals(suffix, version.suffix);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(major, minor, patch, revision, suffix);
        }
    }
    
    /**
     * 版本范围类
     */
    public static class VersionRange {
        private final Version lowerBound;
        private final Version upperBound;
        private final boolean lowerInclusive;
        private final boolean upperInclusive;
        
        public VersionRange(Version lowerBound, Version upperBound, 
                          boolean lowerInclusive, boolean upperInclusive) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.lowerInclusive = lowerInclusive;
            this.upperInclusive = upperInclusive;
        }
        
        public Version getLowerBound() {
            return lowerBound;
        }
        
        public Version getUpperBound() {
            return upperBound;
        }
        
        public boolean isLowerInclusive() {
            return lowerInclusive;
        }
        
        public boolean isUpperInclusive() {
            return upperInclusive;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(lowerInclusive ? ">=" : ">").append(lowerBound);
            sb.append(" && ");
            sb.append(upperInclusive ? "<=" : "<").append(upperBound);
            return sb.toString();
        }
    }
}