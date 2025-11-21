package com.omnisharp.intellij.symbol.indexing;

/**
 * 符号类型枚举
 * 表示代码中不同类型的符号，如类、方法、属性等
 */
public enum OmniSharpSymbolKind {
    /** 命名空间 */
    NAMESPACE,
    /** 类 */
    CLASS,
    /** 接口 */
    INTERFACE,
    /** 结构 */
    STRUCT,
    /** 枚举 */
    ENUM,
    /** 枚举值 */
    ENUM_MEMBER,
    /** 字段 */
    FIELD,
    /** 属性 */
    PROPERTY,
    /** 方法 */
    METHOD,
    /** 构造函数 */
    CONSTRUCTOR,
    /** 析构函数 */
    DESTRUCTOR,
    /** 事件 */
    EVENT,
    /** 委托 */
    DELEGATE,
    /** 局部变量 */
    LOCAL_VARIABLE,
    /** 参数 */
    PARAMETER,
    /** 类型参数 */
    TYPE_PARAMETER,
    /** 模块 */
    MODULE,
    /** 其他 */
    OTHER
}