package com.omnisharp.intellij.symbol.indexing;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 方法符号类
 * 表示方法、构造函数等方法相关的符号
 */
public class OmniSharpMethodSymbol extends OmniSharpSymbol {
    /** 方法的返回类型 */
    private final String returnType;
    /** 方法的参数列表 */
    private final List<ParameterInfo> parameters;
    /** 方法的泛型参数列表 */
    private final List<String> typeParameters;
    /** 该方法是否是抽象的 */
    private final boolean isAbstract;
    /** 该方法是否是虚拟的 */
    private final boolean isVirtual;
    /** 该方法是否是重写的 */
    private final boolean isOverride;
    /** 该方法是否是静态的 */
    private final boolean isStatic;
    /** 该方法是否是异步的 */
    private final boolean isAsync;
    /** 方法的可见性 */
    private final OmniSharpTypeSymbol.Visibility visibility;

    /**
     * 参数信息内部类
     */
    public static class ParameterInfo {
        private final String name;
        private final String type;
        private final boolean isRef;
        private final boolean isOut;
        private final boolean isParams;
        private final String defaultValue;

        public ParameterInfo(String name, String type, boolean isRef, boolean isOut, boolean isParams, String defaultValue) {
            this.name = name;
            this.type = type;
            this.isRef = isRef;
            this.isOut = isOut;
            this.isParams = isParams;
            this.defaultValue = defaultValue;
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean isRef() {
            return isRef;
        }

        public boolean isOut() {
            return isOut;
        }

        public boolean isParams() {
            return isParams;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (isRef) sb.append("ref ");
            if (isOut) sb.append("out ");
            sb.append(type).append(" ").append(name);
            if (defaultValue != null) {
                sb.append(" = ").append(defaultValue);
            }
            if (isParams) {
                sb.append(" params");
            }
            return sb.toString();
        }
    }

    /**
     * 构造函数
     * @param name 符号名称
     * @param fullyQualifiedName 符号完全限定名
     * @param filePath 包含符号的文件路径
     * @param startLine 符号在文件中的起始行号
     * @param startColumn 符号在文件中的起始列号
     * @param endLine 符号在文件中的结束行号
     * @param endColumn 符号在文件中的结束列号
     * @param projectName 符号所在的项目名称
     * @param returnType 方法的返回类型
     * @param parameters 方法的参数列表
     * @param typeParameters 方法的泛型参数列表
     * @param isAbstract 该方法是否是抽象的
     * @param isVirtual 该方法是否是虚拟的
     * @param isOverride 该方法是否是重写的
     * @param isStatic 该方法是否是静态的
     * @param isAsync 该方法是否是异步的
     * @param visibility 方法的可见性
     */
    public OmniSharpMethodSymbol(
            String name,
            String fullyQualifiedName,
            Path filePath,
            int startLine,
            int startColumn,
            int endLine,
            int endColumn,
            String projectName,
            String returnType,
            List<ParameterInfo> parameters,
            List<String> typeParameters,
            boolean isAbstract,
            boolean isVirtual,
            boolean isOverride,
            boolean isStatic,
            boolean isAsync,
            OmniSharpTypeSymbol.Visibility visibility) {
        super(name, fullyQualifiedName, OmniSharpSymbolKind.METHOD, filePath, startLine, startColumn, endLine, endColumn, projectName);
        this.returnType = returnType;
        this.parameters = parameters != null ? new ArrayList<>(parameters) : new ArrayList<>();
        this.typeParameters = typeParameters != null ? new ArrayList<>(typeParameters) : new ArrayList<>();
        this.isAbstract = isAbstract;
        this.isVirtual = isVirtual;
        this.isOverride = isOverride;
        this.isStatic = isStatic;
        this.isAsync = isAsync;
        this.visibility = visibility;
    }

    public String getReturnType() {
        return returnType;
    }

    public List<ParameterInfo> getParameters() {
        return Collections.unmodifiableList(parameters);
    }

    public List<String> getTypeParameters() {
        return Collections.unmodifiableList(typeParameters);
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public boolean isVirtual() {
        return isVirtual;
    }

    public boolean isOverride() {
        return isOverride;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public boolean isAsync() {
        return isAsync;
    }

    public OmniSharpTypeSymbol.Visibility getVisibility() {
        return visibility;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("OmniSharpMethodSymbol{");
        sb.append("name='").append(getName()).append("'\'");
        sb.append(", returnType='").append(returnType).append("'\'");
        sb.append(", parameters=[");
        for (int i = 0; i < parameters.size(); i++) {
            sb.append(parameters.get(i));
            if (i < parameters.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        sb.append(", static=").append(isStatic);
        sb.append(", async=").append(isAsync);
        sb.append(", visibility=").append(visibility);
        sb.append("}");
        return sb.toString();
    }
}