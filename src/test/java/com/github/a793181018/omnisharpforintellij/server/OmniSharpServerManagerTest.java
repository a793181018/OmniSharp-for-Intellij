//package com.github.a793181018.omnisharpforintellij.server;
//
//import com.github.a793181018.omnisharpforintellij.server.configuration.IOmniSharpConfiguration;
//import com.github.a793181018.omnisharpforintellij.server.configuration.impl.OmniSharpConfigurationImpl;
//import com.github.a793181018.omnisharpforintellij.server.exceptions.OmniSharpConfigurationException;
//import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpEvent;
//import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpRequest;
//import com.github.a793181018.omnisharpforintellij.server.model.OmniSharpResponse;
//import com.github.a793181018.omnisharpforintellij.server.model.ServerStatus;
//import com.github.a793181018.omnisharpforintellij.server.impl.OmniSharpServerManagerImpl;
//import org.junit.jupiter.api.AfterEach;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
//import org.mockito.Mockito;
//
//import java.io.File;
//import java.util.Collections;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.Mockito.*;
//
///**
// * OmniSharp服务器管理器的测试类
// */
//class OmniSharpServerManagerTest {
//
//    private IOmniSharpServerManager serverManager;
//    private IOmniSharpConfiguration configuration;
//
//    @BeforeEach
//    void setUp() {
//        // 创建配置对象并模拟
//        configuration = mock(IOmniSharpConfiguration.class);
//
//        // 设置基本配置
//        when(configuration.validate()).thenReturn(new IOmniSharpConfiguration.ValidationResult(true, ""));
//        when(configuration.getServerPath()).thenReturn("omnisharp"); // 这应该在真实环境中指向实际的可执行文件
//        when(configuration.getWorkingDirectory()).thenReturn(System.getProperty("user.dir"));
//        when(configuration.getServerArguments()).thenReturn(Collections.emptyList());
//        when(configuration.getStartupTimeoutMs()).thenReturn(30000L);
//        when(configuration.getShutdownTimeoutMs()).thenReturn(5000L);
//
//        // 创建服务器管理器实例
//        serverManager = new OmniSharpServerManagerImpl(configuration);
//    }
//
//    @AfterEach
//    void tearDown() {
//        // 确保在测试结束时停止服务器
//        if (serverManager != null && serverManager.isRunning()) {
//            serverManager.stop();
//        }
//        // 由于我们使用了IntelliJ的Service注解，这里应该调用dispose方法
//        ((OmniSharpServerManagerImpl)serverManager).dispose();
//    }
//
//    @Test
//    void testGetInitialStatus() {
//        // 测试初始状态是否为NOT_STARTED
//        assertEquals(ServerStatus.NOT_STARTED, serverManager.getStatus());
//        assertFalse(serverManager.isRunning());
//    }
//
//    @Test
//    void testStatusChangeListeners() throws InterruptedException {
//        // 测试状态变更监听器
//        CountDownLatch latch = new CountDownLatch(1);
//
//        serverManager.addStatusChangeListener(() -> {
//            if (serverManager.getStatus() == ServerStatus.STARTING) {
//                latch.countDown();
//            }
//        });
//
//        // 模拟启动过程
//        when(configuration.validate()).thenReturn(new IOmniSharpConfiguration.ValidationResult(false, "Invalid path"));
//
//        boolean started = serverManager.start();
//        assertFalse(started);
//
//        // 清理监听器
//        serverManager.removeStatusChangeListener(() -> {});
//    }
//
//    @Test
//    void testConfigurationValidation() {
//        // 测试配置验证失败的情况
//        when(configuration.validate()).thenReturn(new IOmniSharpConfiguration.ValidationResult(false, "Invalid path"));
//
//        boolean started = serverManager.start();
//        assertFalse(started);
//        assertEquals(ServerStatus.FAILED, serverManager.getStatus());
//    }
//
//    @Test
//    void testSendRequestWhenNotRunning() {
//        // 测试当服务器未运行时发送请求
//        OmniSharpRequest<String> request = new OmniSharpRequest<>("test", null, null);
//        CompletableFuture<OmniSharpResponse<String>> future = serverManager.sendRequest(request);
//
//        // 验证请求失败
//        assertThrows(Exception.class, () -> future.get(1, TimeUnit.SECONDS));
//    }
//
//    @Test
//    void testEventSubscription() {
//        // 测试事件订阅和取消订阅
//        String subscriptionId = serverManager.subscribeToEvent("testEvent", String.class, event -> {
//            // 这个回调在实际测试中不会被调用，因为服务器未启动
//        });
//
//        assertNotNull(subscriptionId);
//        assertTrue(subscriptionId.startsWith("sub_"));
//
//        // 取消订阅
//        serverManager.unsubscribeFromEvent(subscriptionId);
//    }
//
//    @Test
//    void testStopWhenNotRunning() {
//        // 测试当服务器未运行时停止
//        boolean stopped = serverManager.stop();
//        assertFalse(stopped);
//    }
//
//    @Test
//    void testRestartSequence() {
//        // 测试重启序列
//        // 注意：这只是测试方法调用顺序，不会真正启动服务器
//        when(configuration.validate()).thenReturn(new IOmniSharpConfiguration.ValidationResult(false, "Invalid path"));
//
//        boolean restarted = serverManager.restart();
//        assertFalse(restarted);
//    }
//
//    @Test
//    void testWaitForServerReadyTimeout() {
//        // 测试等待服务器就绪超时
//        assertThrows(Exception.class, () -> {
//            serverManager.waitForServerReady(100); // 100ms应该会超时
//        });
//    }
//
//    // 注意：以下测试需要实际的OmniSharp服务器可执行文件，在CI环境中可能需要禁用
//    @DisabledIfEnvironmentVariable(named = "CI", matches = ".*")
//    void testIntegrationWithRealServer() {
//        // 这个测试应该使用实际的OmniSharp服务器进行集成测试
//        // 由于需要外部依赖，默认禁用
//        System.out.println("Integration test skipped - requires OmniSharp server executable");
//    }
//
//    /**
//     * 测试辅助方法：模拟成功启动服务器
//     */
//    private void mockSuccessfulServerStart() {
//        // 配置模拟的成功启动
//        when(configuration.validate()).thenReturn(new IOmniSharpConfiguration.ValidationResult(true, ""));
//        when(configuration.getServerPath()).thenReturn(getMockOmniSharpPath());
//        // 其他必要的配置...
//    }
//
//    /**
//     * 获取模拟的OmniSharp路径
//     */
//    private String getMockOmniSharpPath() {
//        // 在真实测试中，这应该指向测试用的OmniSharp服务器或模拟脚本
//        String os = System.getProperty("os.name").toLowerCase();
//        if (os.contains("win")) {
//            return "omnisharp.cmd";
//        } else {
//            return "./omnisharp";
//        }
//    }
//}