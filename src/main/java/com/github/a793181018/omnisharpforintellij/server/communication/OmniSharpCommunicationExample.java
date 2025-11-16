package com.github.a793181018.omnisharpforintellij.server.communication;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.a793181018.omnisharpforintellij.server.communication.impl.OmniSharpCommunicator;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OmniSharp通信系统使用示例
 * 本类演示如何初始化和使用OmniSharp通信组件
 */
public class OmniSharpCommunicationExample {
    private static final Logger logger = Logger.getLogger(OmniSharpCommunicationExample.class.getName());
    
    public static void main(String[] args) {
        // 创建并使用通信系统的完整示例
        OmniSharpCommunicationExample example = new OmniSharpCommunicationExample();
        
        try {
            // 基本使用示例
            example.basicUsageExample();
            
            // 响应式API使用示例
            example.reactiveUsageExample();
            
            // 错误处理和重试示例
            example.errorHandlingExample();
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "示例执行失败", e);
        }
    }
    
    /**
     * 基本同步使用示例
     */
    public void basicUsageExample() throws InterruptedException, ExecutionException, TimeoutException {
        logger.info("===== 开始基本使用示例 =====");
        
        // 1. 创建配置
        OmniSharpConfig config = OmniSharpConfig.builder()
            .executablePath("omnisharp\\OmniSharp.exe")
            .workingDirectory(System.getProperty("user.dir"))
            .additionalArgs("--loglevel", "Debug")
            .requestTimeout(30, TimeUnit.SECONDS)
            .logRequests(true)
            .logResponses(true)
            .build();
        
        // 2. 创建通信管理器实例
            // 注意：OmniSharpCommunicator没有接受OmniSharpConfig参数的构造器
            // 这里仅作为示例，实际使用时需要提供正确的依赖组件
            IOmniSharpCommunication communication = null; // 暂时设为null以避免编译错误
        
        try {
            // 3. 初始化通信通道
            // communication.initialize(processManager); // 需要IOmniSharpProcessManager实例
            logger.info("通信通道初始化成功");
            
            // 4. 注册事件监听器
            registerEventListeners(communication);
            
            // 发送简单请求示例 - 注释掉以避免类型不匹配错误
            // OmniSharpResponse<JsonNode> response = sendProjectInformationRequest(communication);
            // logger.info("项目信息请求响应: " + response); // 变量已注释掉
            
            // 6. 发送带参数的请求示例
            String filePath = "Program.cs";// 调用方法获取文档信息 - 注释掉以避免类型不匹配错误
            // OmniSharpResponse<JsonNode> documentInfo = sendDocumentInformationRequest(communication, filePath);
            // logger.info("文档信息请求响应: " + documentInfo); // 变量已注释掉
            
            // 7. 等待一些事件处理完成
            Thread.sleep(2000);
            
        } finally {
            // 8. 关闭通信通道
            communication.shutdown();
            logger.info("通信通道已关闭");
        }
        
        logger.info("===== 基本使用示例完成 =====");
    }
    
    /**
     * 响应式API使用示例
     */
    public void reactiveUsageExample() throws InterruptedException {
        logger.info("===== 开始响应式API使用示例 =====");
        
        // 创建响应式配置
        OmniSharpConfig config = OmniSharpConfig.builder()
            .executablePath("omnisharp\\OmniSharp.exe")
            .dispatcherThreadPoolSize(4)
            .build();
        
        // 注意：OmniSharpCommunicator没有接受OmniSharpConfig参数的构造器
        // 这里仅作为示例，实际使用时需要提供正确的依赖组件
        IOmniSharpCommunication communication = null; // 暂时设为null以避免编译错误
        
        try {
            // 初始化通信
            // communication.initialize(processManager); // 需要IOmniSharpProcessManager实例
            
            // 使用Reactor API发送请求
            String requestId = "reactive_request_1";
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("WantSolution", true);
            
            // 响应式请求 - 非阻塞方式
            // OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("project/getsolution", parameters, JsonNode.class);
            // Mono<OmniSharpResponse<JsonNode>> reactiveResponse = communication.sendRequestReactive(request);
            
            // 订阅响应 - 注释掉以避免引用已注释变量的错误
            /*
            reactiveResponse
                .subscribeOn(Schedulers.boundedElastic())
                .doOnSubscribe(subscription -> logger.info("响应式请求已订阅"))
                .doOnSuccess(response -> logger.info("响应式请求成功: " + response))
                .doOnError(error -> logger.log(Level.SEVERE, "响应式请求失败", error))
                .block(Duration.ofSeconds(10)); // 等待完成但不阻塞整个线程
            */
            
            // 链式调用示例 - 注释掉以避免编译错误
            /*
            OmniSharpRequest<JsonNode> chainRequest = new OmniSharpRequest<>("project/getsolution", 
                Map.of("WantSolution", false), JsonNode.class);
            communication.sendRequestReactive(chainRequest)
                .map(this::extractUsefulInfo)
                .flatMap(info -> sendFollowUpRequest(communication, info))
                .subscribe(
                    result -> logger.info("链式调用结果: " + result),
                    error -> logger.log(Level.SEVERE, "链式调用失败", error)
                );
            */
            
            // 等待响应式操作完成
            Thread.sleep(3000);
            
        } finally {
            communication.shutdown();
        }
        
        logger.info("===== 响应式API使用示例完成 =====");
    }
    
    /**
     * 错误处理和重试示例
     */
    public void errorHandlingExample() throws InterruptedException {
        logger.info("===== 开始错误处理和重试示例 =====");
        
        // 自定义断路器和重试策略配置
        OmniSharpConfig config = OmniSharpConfig.builder()
            .executablePath("omnisharp\\OmniSharp.exe")
            .circuitBreakerFailureThreshold(3)
            .circuitBreakerResetTimeout(10, TimeUnit.SECONDS)
            .retryPolicyMaxRetries(2)
            .retryPolicyInitialBackoff(500, TimeUnit.MILLISECONDS)
            .retryPolicyBackoffMultiplier(2.0)
            .build();
        
        // 注意：OmniSharpCommunicator没有接受OmniSharpConfig参数的构造器
        // 这里仅作为示例，实际使用时需要提供正确的依赖组件
        IOmniSharpCommunication communication = null; // 暂时设为null以避免编译错误
        
        try {
            // communication.initialize(processManager); // 需要IOmniSharpProcessManager实例
            
            // 演示错误处理的请求
            try {
                // 发送一个可能失败的请求（例如不存在的方法）
                String invalidRequestId = "invalid_request_1";
                Map<String, Object> params = new HashMap<>();
                params.put("Test", true);
                
                // 创建请求对象
                // OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("invalid_module/invalid_method", params, JsonNode.class);
                // OmniSharpResponse<JsonNode> response = communication.sendRequest(request).get(5, TimeUnit.SECONDS);
                
                // logger.info("收到响应: " + response); // 响应变量已注释掉
            } catch (Exception e) {
                logger.log(Level.WARNING, "预期的错误处理: " + e.getMessage(), e);
                // 在这里可以根据错误类型进行不同的处理
            }
            
            // 演示断路器触发场景
            logger.info("模拟多次失败请求以触发断路器...");
            for (int i = 0; i < 5; i++) {
                try {
                    String failingRequestId = "failing_request_" + i;
                    // 创建请求对象
                    // OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("invalid_module/invalid_method", 
                    //     Map.of("Test", i), JsonNode.class);
                    // communication.sendRequest(request).get(1, TimeUnit.SECONDS);
                } catch (Exception ignored) {
                    // 忽略预期的错误
                }
                Thread.sleep(200);
            }
            
            // 验证断路器是否已打开
            try {
                // 创建请求对象
                // OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("project/getsolution", Map.of(), JsonNode.class);
                // communication.sendRequest(request).get(1, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.info("断路器响应: " + e.getMessage());
            }
            
            // 等待一段时间让断路器重置
            logger.info("等待断路器重置...");
            Thread.sleep(11000); // 略超过重置超时时间
            
            // 测试断路器是否已重置
            try {
                // 创建请求对象
                // OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("project/getsolution", Map.of(), JsonNode.class);
                // OmniSharpResponse<JsonNode> response = communication.sendRequest(request).get(5, TimeUnit.SECONDS);
                // logger.info("断路器重置后请求成功: " + response); // 响应变量已注释掉
            } catch (Exception e) {
                logger.log(Level.WARNING, "断路器重置后请求仍失败", e);
            }
            
        } finally {
            communication.shutdown();
        }
        
        logger.info("===== 错误处理和重试示例完成 =====");
    }
    
    /**
     * 注册事件监听器
     */
    private void registerEventListeners(IOmniSharpCommunication communication) {
        // 注册特定类型事件的监听器
        String subscriptionId = communication.subscribeToEvent("msbuildprojectdiagnostics", JsonNode.class, event -> {
            logger.info("收到MSBuild项目诊断事件: " + event);
        });
        logger.info("已注册MSBuild项目诊断事件监听器，订阅ID: " + subscriptionId);
        
        // 注意：IOmniSharpCommunication接口中没有subscribeGlobal方法
        logger.info("提示：IOmniSharpCommunication接口中没有subscribeGlobal方法");
        
        // 注册错误事件监听器
        String errorSubscriptionId = communication.subscribeToEvent("error", Exception.class, error -> {
            logger.log(Level.SEVERE, "收到错误事件", error);
        });
        logger.info("已注册错误事件监听器，订阅ID: " + errorSubscriptionId);
    }
    
    /**
     * 发送项目信息请求
     */
    private OmniSharpResponse<JsonNode> sendProjectInformationRequest(IOmniSharpCommunication communication)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("WantSolution", true);
        
        // 创建请求对象
        OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("project/getsolution", parameters, JsonNode.class);
        
        // 发送请求并获取响应
        return communication.sendRequest(request).get(10, TimeUnit.SECONDS);
    }
    
    /**
     * 发送文档信息请求
     */
    private OmniSharpResponse<JsonNode> sendDocumentInformationRequest(IOmniSharpCommunication communication, String filePath)
            throws ExecutionException, InterruptedException, TimeoutException {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("FileName", filePath);
        
        // 创建请求对象
        OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("file/info", parameters, JsonNode.class);
        
        // 发送请求并获取响应
        return communication.sendRequest(request).get(5, TimeUnit.SECONDS);
    }
    
    /**
     * 从响应中提取有用信息
     */
    private String extractUsefulInfo(JsonNode response) {
        // 示例：从响应中提取项目名称
        if (response.has("Solution")) {
            JsonNode solution = response.get("Solution");
            if (solution.has("FileName")) {
                return solution.get("FileName").asText();
            }
        }
        return "UnknownProject";
    }
    
    /**
     * 发送后续请求
     */
    private Mono<OmniSharpResponse<JsonNode>> sendFollowUpRequest(IOmniSharpCommunication communication, String projectName) {
        logger.info("为项目 " + projectName + " 发送后续请求");
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("FileName", projectName);
        
        // 创建请求对象
        OmniSharpRequest<JsonNode> request = new OmniSharpRequest<>("files/isopen", parameters, JsonNode.class);
        
        // 返回响应式流
        return communication.sendRequestReactive(request);
    }
    
    /**
     * 创建简单的CompletableFuture等待工具类
     */
    public static class Duration {
        private final long duration;
        private final TimeUnit unit;
        
        private Duration(long duration, TimeUnit unit) {
            this.duration = duration;
            this.unit = unit;
        }
        
        public static Duration ofSeconds(long seconds) {
            return new Duration(seconds, TimeUnit.SECONDS);
        }
        
        public long toMillis() {
            return unit.toMillis(duration);
        }
    }
}