package com.github.a793181018.omnisharpforintellij.editor.navigation.structure.impl;

import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget;
import com.github.a793181018.omnisharpforintellij.editor.navigation.model.NavigationTarget.TargetType;
import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.response.FileStructureResponse;
import com.github.a793181018.omnisharpforintellij.editor.navigation.structure.service.FileStructureService;
import com.github.a793181018.omnisharpforintellij.server.OmniSharpServerManager;
import com.github.a793181018.omnisharpforintellij.server.communication.IOmniSharpCommunication;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import com.github.a793181018.omnisharpforintellij.session.OmniSharpSession;
import com.github.a793181018.omnisharpforintellij.session.OmniSharpSessionManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * OmniSharp文件结构服务实现
 * 通过与OmniSharp服务器通信获取C#文件的结构信息
 */
public class OmniSharpFileStructureService implements FileStructureService {
    
    private static final Logger LOG = Logger.getInstance(OmniSharpFileStructureService.class);
    
    private final Project project;
    private final OmniSharpSessionManager sessionManager;
    private final ConcurrentHashMap<String, List<NavigationTarget>> cache = new ConcurrentHashMap<>();
    
    public OmniSharpFileStructureService(@NotNull Project project) {
        this.project = project;
        this.sessionManager = project.getService(OmniSharpSessionManager.class);
    }
    
    @Override
    @NotNull
    public CompletableFuture<List<NavigationTarget>> getFileStructure(@NotNull Project project, @NotNull VirtualFile file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getFileStructureSync(project, file);
            } catch (Exception e) {
                LOG.error("Failed to get file structure for: " + file.getPath(), e);
                return new ArrayList<>();
            }
        });
    }
    
    @Override
    @NotNull
    public List<NavigationTarget> getFileStructureSync(@NotNull Project project, @NotNull VirtualFile file) {
        if (!isSupportedFile(file)) {
            return new ArrayList<>();
        }
        
        // 检查缓存
        String cacheKey = file.getPath();
        List<NavigationTarget> cachedResult = cache.get(cacheKey);
        if (cachedResult != null) {
            return cachedResult;
        }
        
    try {
        // 获取当前会话
        OmniSharpSession session = sessionManager.getActiveSession();
        if (session == null) {
            LOG.warn("No active OmniSharp session for file: " + file.getPath());
            return new ArrayList<>();
        }
        
        // 获取通信服务
        IOmniSharpCommunication communication = session.getProject().getService(IOmniSharpCommunication.class);
        if (communication == null || !communication.isInitialized()) {
            LOG.warn("OmniSharp communication service not initialized for file: " + file.getPath());
            return new ArrayList<>();
        }
        
        // 创建请求参数
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("fileName", file.getPath());
        
        // 创建OmniSharp请求
        OmniSharpRequest<FileStructureResponse> request = new OmniSharpRequest<>(
            "/v2/codestructure",
            arguments,
            FileStructureResponse.class
        );
        
        // 发送请求到OmniSharp服务器
        CompletableFuture<OmniSharpResponse<FileStructureResponse>> responseFuture = communication.sendRequest(request);
        
        // 提取响应体
        CompletableFuture<FileStructureResponse> resultFuture = responseFuture.thenApply(response -> {
            if (response != null && response.isSuccess()) {
                return response.getBody();
            } else {
                LOG.warn("OmniSharp request failed: " + (response != null ? response.getMessage() : "null response"));
                return new FileStructureResponse(null);
            }
        });
        
        FileStructureResponse response = resultFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
        
        if (response == null || response.getMembers() == null) {
            LOG.warn("Empty response from OmniSharp server for file: " + file.getPath());
            return new ArrayList<>();
        }
        
        // 转换响应为NavigationTarget列表
        List<NavigationTarget> targets = convertResponseToTargets(response, file.getPath());
        
        // 缓存结果
        cache.put(cacheKey, targets);
        
        return targets;
            
        } catch (Exception e) {
            LOG.error("Error getting file structure from OmniSharp server for file: " + file.getPath(), e);
            return new ArrayList<>();
        }
    }
    
    @Override
    public boolean isSupportedFile(@Nullable VirtualFile file) {
        if (file == null) {
            return false;
        }
        String extension = file.getExtension();
        return "cs".equalsIgnoreCase(extension);
    }
    
    @Override
    public void clearCache(@NotNull VirtualFile file) {
        cache.remove(file.getPath());
    }
    
    @Override
    public void clearAllCache() {
        cache.clear();
    }
    
    /**
     * 将OmniSharp响应转换为NavigationTarget列表
     */
    @NotNull
    private List<NavigationTarget> convertResponseToTargets(@NotNull FileStructureResponse response, @NotNull String filePath) {
        List<NavigationTarget> targets = new ArrayList<>();
        
        if (response.getMembers() != null) {
            for (FileStructureResponse.Member member : response.getMembers()) {
                NavigationTarget target = convertMemberToTarget(member, filePath);
                if (target != null) {
                    targets.add(target);
                }
            }
        }
        
        return targets;
    }
    
    /**
     * 将单个成员转换为NavigationTarget
     */
    @Nullable
    private NavigationTarget convertMemberToTarget(@NotNull FileStructureResponse.Member member, @NotNull String filePath) {
        try {
            TargetType targetType = mapMemberKindToTargetType(member.getKind());
            if (targetType == null) {
                return null;
            }
            
            NavigationTarget.Builder builder = new NavigationTarget.Builder(filePath, member.getName(), targetType)
                .withLine(member.getLine() + 1)  // OmniSharp使用0基索引，转换为1基
                .withColumn(member.getColumn());
            
            // 设置包含类型（如果有）
            if (member.getContainingType() != null && !member.getContainingType().isEmpty()) {
                builder.withContainingType(member.getContainingType());
            }
            
            // 设置包含命名空间（如果有）
            if (member.getContainingNamespace() != null && !member.getContainingNamespace().isEmpty()) {
                builder.withContainingNamespace(member.getContainingNamespace());
            }
            
            // 设置签名（如果有）
            if (member.getSignature() != null && !member.getSignature().isEmpty()) {
                builder.withSignature(member.getSignature());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            LOG.error("Error converting member to target: " + member.getName(), e);
            return null;
        }
    }
    
    /**
     * 将OmniSharp成员类型映射到NavigationTarget类型
     */
    @Nullable
    private TargetType mapMemberKindToTargetType(@Nullable String kind) {
        if (kind == null) {
            return TargetType.OTHER;
        }
        
        switch (kind.toLowerCase()) {
            case "class":
                return TargetType.CLASS;
            case "interface":
                return TargetType.INTERFACE;
            case "method":
                return TargetType.METHOD;
            case "property":
                return TargetType.PROPERTY;
            case "field":
                return TargetType.FIELD;
            case "enum":
                return TargetType.ENUM;
            case "enumvalue":
                return TargetType.ENUM_VALUE;
            case "namespace":
                return TargetType.NAMESPACE;
            case "constructor":
                return TargetType.CONSTRUCTOR;
            case "destructor":
                return TargetType.DESTRUCTOR;
            case "event":
                return TargetType.EVENT;
            case "operator":
                return TargetType.OPERATOR;
            case "indexer":
                return TargetType.INDEXER;
            case "typeparameter":
                return TargetType.TYPE_PARAMETER;
            default:
                return TargetType.OTHER;
        }
    }
}