package com.omnisharp.intellij.symbol.indexing;

/**
 * 项目引用接口
 * 表示项目中的引用（可以是项目引用或包引用）
 */
public interface OmniSharpProjectReference {
    /**
     * 获取引用名称
     * @return 引用名称
     */
    String getName();
    
    /**
     * 判断是否是项目引用
     * @return 如果是项目引用则返回true
     */
    boolean isProjectReference();
    
    /**
     * 判断是否是包引用
     * @return 如果是包引用则返回true
     */
    boolean isPackageReference();
    
    /**
     * 获取引用路径（如果适用）
     * @return 引用路径
     */
    String getPath();
    
    /**
     * 获取引用版本（如果适用）
     * @return 引用版本
     */
    String getVersion();
}