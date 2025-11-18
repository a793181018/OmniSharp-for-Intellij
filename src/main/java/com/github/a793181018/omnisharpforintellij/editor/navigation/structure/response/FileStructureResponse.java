package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.response;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 文件结构响应模型
 * 接收来自OmniSharp服务器的C#文件结构信息
 */
public class FileStructureResponse {
    private final List<Member> members;
    
    public FileStructureResponse(@Nullable List<Member> members) {
        this.members = members;
    }
    
    @Nullable
    public List<Member> getMembers() {
        return members;
    }
    
    @Override
    public String toString() {
        return "FileStructureResponse{" +
               "members=" + members +
               '}';
    }
    
    /**
     * 文件成员模型
     */
    public static class Member {
        private final String name;
        private final String kind;
        private final int line;
        private final int column;
        private final String containingType;
        private final String containingNamespace;
        private final String signature;
        private final List<Member> children;
        
        public Member(@NotNull String name, 
                     @NotNull String kind,
                     int line, 
                     int column,
                     @Nullable String containingType,
                     @Nullable String containingNamespace,
                     @Nullable String signature,
                     @Nullable List<Member> children) {
            this.name = name;
            this.kind = kind;
            this.line = line;
            this.column = column;
            this.containingType = containingType;
            this.containingNamespace = containingNamespace;
            this.signature = signature;
            this.children = children;
        }
        
        @NotNull
        public String getName() {
            return name;
        }
        
        @NotNull
        public String getKind() {
            return kind;
        }
        
        public int getLine() {
            return line;
        }
        
        public int getColumn() {
            return column;
        }
        
        @Nullable
        public String getContainingType() {
            return containingType;
        }
        
        @Nullable
        public String getContainingNamespace() {
            return containingNamespace;
        }
        
        @Nullable
        public String getSignature() {
            return signature;
        }
        
        @Nullable
        public List<Member> getChildren() {
            return children;
        }
        
        @Override
        public String toString() {
            return "Member{" +
                   "name='" + name + '\'' +
                   ", kind='" + kind + '\'' +
                   ", line=" + line +
                   ", column=" + column +
                   '}';
        }
    }
}