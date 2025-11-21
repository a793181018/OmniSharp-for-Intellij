package com.omnisharp.intellij.projectstructure.navigation;

import com.omnisharp.intellij.projectstructure.model.NodeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * 过滤器管理器，用于管理和应用项目结构的各种过滤器
 */
public class FilterManager {
    private final Map<String, NodeFilter> filters;
    private final Set<String> activeFilterIds;
    
    public FilterManager() {
        this.filters = new HashMap<>();
        this.activeFilterIds = new HashSet<>();
        initializeDefaultFilters();
    }
    
    private void initializeDefaultFilters() {
        // 添加默认过滤器
        addFilter("showOnlyFiles", "仅显示文件", node -> node.getNodeType() == NodeType.FILE);
        addFilter("showOnlyFolders", "仅显示文件夹", node -> node.getNodeType() == NodeType.FOLDER);
        addFilter("showOnlyProjects", "仅显示项目", node -> node.getNodeType() == NodeType.PROJECT);
    }
    
    /**
     * 添加过滤器
     * @param id 过滤器ID
     * @param name 过滤器名称
     * @param predicate 过滤条件
     */
    public void addFilter(@NotNull String id, @NotNull String name, @NotNull Predicate<NavigationNode> predicate) {
        filters.put(id, new NodeFilter(id, name, predicate));
    }
    
    /**
     * 移除过滤器
     * @param id 过滤器ID
     * @return 是否移除成功
     */
    public boolean removeFilter(@NotNull String id) {
        if (filters.containsKey(id)) {
            filters.remove(id);
            activeFilterIds.remove(id);
            return true;
        }
        return false;
    }
    
    /**
     * 激活过滤器
     * @param id 过滤器ID
     * @return 是否激活成功
     */
    public boolean activateFilter(@NotNull String id) {
        if (filters.containsKey(id)) {
            activeFilterIds.add(id);
            return true;
        }
        return false;
    }
    
    /**
     * 停用过滤器
     * @param id 过滤器ID
     * @return 是否停用成功
     */
    public boolean deactivateFilter(@NotNull String id) {
        return activeFilterIds.remove(id);
    }
    
    /**
     * 切换过滤器状态
     * @param id 过滤器ID
     * @return 切换后的状态（true表示激活，false表示停用）
     */
    public boolean toggleFilter(@NotNull String id) {
        if (!filters.containsKey(id)) {
            return false;
        }
        
        if (activeFilterIds.contains(id)) {
            activeFilterIds.remove(id);
            return false;
        } else {
            activeFilterIds.add(id);
            return true;
        }
    }
    
    /**
     * 检查过滤器是否激活
     * @param id 过滤器ID
     * @return 是否激活
     */
    public boolean isFilterActive(@NotNull String id) {
        return activeFilterIds.contains(id);
    }
    
    /**
     * 获取所有过滤器
     * @return 过滤器列表
     */
    @NotNull
    public List<NodeFilter> getAllFilters() {
        return new ArrayList<>(filters.values());
    }
    
    /**
     * 获取激活的过滤器
     * @return 激活的过滤器列表
     */
    @NotNull
    public List<NodeFilter> getActiveFilters() {
        return activeFilterIds.stream()
                .map(filters::get)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取激活的过滤器谓词列表
     * @return 激活的过滤器谓词列表
     */
    @NotNull
    public List<Predicate<NavigationNode>> getActiveFilterPredicates() {
        return getActiveFilters().stream()
                .map(NodeFilter::getPredicate)
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 清除所有过滤器
     */
    public void clearAllFilters() {
        activeFilterIds.clear();
    }
    
    /**
     * 根据ID获取过滤器
     * @param id 过滤器ID
     * @return 过滤器实例，如果不存在则返回null
     */
    @Nullable
    public NodeFilter getFilterById(@NotNull String id) {
        return filters.get(id);
    }
    
    /**
     * 创建名称过滤器
     * @param id 过滤器ID
     * @param name 过滤器名称
     * @param keyword 关键词
     * @param caseSensitive 是否区分大小写
     */
    public void createNameFilter(@NotNull String id, @NotNull String name, 
                                @NotNull String keyword, boolean caseSensitive) {
        if (caseSensitive) {
            addFilter(id, name, node -> node.getName().contains(keyword));
        } else {
            String lowerKeyword = keyword.toLowerCase();
            addFilter(id, name, node -> node.getName().toLowerCase().contains(lowerKeyword));
        }
    }
    
    /**
     * 创建正则表达式过滤器
     * @param id 过滤器ID
     * @param name 过滤器名称
     * @param regex 正则表达式
     * @return 是否创建成功
     */
    public boolean createRegexFilter(@NotNull String id, @NotNull String name, @NotNull String regex) {
        try {
            Pattern pattern = Pattern.compile(regex);
            addFilter(id, name, node -> pattern.matcher(node.getName()).find());
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }
    
    /**
     * 创建类型过滤器
     * @param id 过滤器ID
     * @param name 过滤器名称
     * @param nodeTypes 节点类型列表
     */
    public void createTypeFilter(@NotNull String id, @NotNull String name, 
                                @NotNull List<NodeType> nodeTypes) {
        Set<NodeType> typeSet = new HashSet<>(nodeTypes);
        addFilter(id, name, node -> typeSet.contains(node.getNodeType()));
    }
    
    /**
     * 创建路径过滤器
     * @param id 过滤器ID
     * @param name 过滤器名称
     * @param path 路径关键词
     * @param caseSensitive 是否区分大小写
     */
    public void createPathFilter(@NotNull String id, @NotNull String name, 
                                @NotNull String path, boolean caseSensitive) {
        if (caseSensitive) {
            addFilter(id, name, node -> node.getPath().contains(path));
        } else {
            String lowerPath = path.toLowerCase();
            addFilter(id, name, node -> node.getPath().toLowerCase().contains(lowerPath));
        }
    }
    
    /**
     * 过滤器类，封装过滤条件和元数据
     */
    public static class NodeFilter {
        private final String id;
        private final String name;
        private final Predicate<NavigationNode> predicate;
        
        public NodeFilter(@NotNull String id, @NotNull String name, 
                         @NotNull Predicate<NavigationNode> predicate) {
            this.id = id;
            this.name = name;
            this.predicate = predicate;
        }
        
        @NotNull
        public String getId() {
            return id;
        }
        
        @NotNull
        public String getName() {
            return name;
        }
        
        @NotNull
        public Predicate<NavigationNode> getPredicate() {
            return predicate;
        }
        
        public boolean test(@NotNull NavigationNode node) {
            return predicate.test(node);
        }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            
            NodeFilter that = (NodeFilter) o;
            return id.equals(that.id);
        }
        
        @Override
        public int hashCode() {
            return id.hashCode();
        }
        
        @Override
        public String toString() {
            return "NodeFilter{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    '}';
        }
    }
}