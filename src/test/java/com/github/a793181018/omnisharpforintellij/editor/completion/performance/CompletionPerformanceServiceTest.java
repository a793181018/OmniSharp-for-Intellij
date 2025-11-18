package com.github.a793181018.omnisharpforintellij.editor.completion.performance;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionRequest;
import com.github.a793181018.omnisharpforintellij.util.TestUtils;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorImpl;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiFileImpl;
import com.intellij.testFramework.LightPlatformTestCase;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class CompletionPerformanceServiceTest extends LightPlatformTestCase {
    
    private CompletionPerformanceService performanceService;
    private Project testProject;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        testProject = getProject();
        performanceService = CompletionPerformanceService.getInstance(testProject);
    }
    
    @Test
    public void testPrefetchCompletions() {
        // 测试预取补全功能
        // 由于预取是异步的，我们无法直接验证结果，但可以确保方法不会抛出异常
        
        Editor mockEditor = mock(EditorImpl.class);
        PsiFile mockFile = mock(PsiFileImpl.class);
        int caretOffset = 10;
        
        try {
            performanceService.prefetchCompletions(mockEditor, mockFile, caretOffset);
            // 如果方法执行到这里而没有抛出异常，测试就通过了
            assertTrue("预取方法应该正常执行", true);
        } catch (Exception e) {
            fail("预取方法不应该抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetPrefetchedCompletions() {
        // 测试获取预取的补全项
        // 由于预取逻辑依赖于实际环境，这里我们主要测试空结果的处理
        
        CompletionRequest request = new CompletionRequest();
        request.setFileName("test.cs");
        request.setLine(1);
        request.setColumn(1);
        
        List<CompletionItem> prefetchedItems = performanceService.getPrefetchedCompletions(request);
        
        // 由于我们没有实际预取任何内容，应该返回null或空列表
        // 但不应该抛出异常
        assertTrue("获取未预取的结果应该返回null或空列表", 
                prefetchedItems == null || prefetchedItems.isEmpty());
    }
    
    @Test
    public void testClearPrefetchedCache() {
        // 测试清除预取缓存
        // 由于预取缓存是内部管理的，我们主要验证方法不会抛出异常
        
        try {
            performanceService.clearPrefetchedCache();
            // 如果方法执行到这里而没有抛出异常，测试就通过了
            assertTrue("清除预取缓存方法应该正常执行", true);
        } catch (Exception e) {
            fail("清除预取缓存方法不应该抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    public void testShouldPerformCompletion() {
        // 测试是否应该执行补全的判断逻辑
        
        // 创建不同的前缀场景
        String[] testPrefixes = {
            "",       // 空前缀
            "a",      // 单个字符
            "var",    // 关键词
            "myVar",  // 普通变量名
            "_"       // 下划线
        };
        
        // 验证方法不会抛出异常
        for (String prefix : testPrefixes) {
            try {
                boolean shouldPerform = performanceService.shouldPerformCompletion(prefix);
                // 结果可以是true或false，但方法不应抛出异常
                assertTrue("判断方法应该正常执行", true);
            } catch (Exception e) {
                fail("判断方法不应该抛出异常，前缀: '" + prefix + "', 异常: " + e.getMessage());
            }
        }
    }
    
    @Test
    public void testRecordPerformanceMetric() {
        // 测试记录性能指标
        // 验证方法不会抛出异常
        
        try {
            // 记录不同类型的性能指标
            performanceService.recordPerformanceMetric("test_metric", 100);
            performanceService.recordPerformanceMetric("response_time", 250);
            performanceService.recordPerformanceMetric("cache_hit", 1);
            
            // 如果方法执行到这里而没有抛出异常，测试就通过了
            assertTrue("记录性能指标方法应该正常执行", true);
        } catch (Exception e) {
            fail("记录性能指标方法不应该抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetPerformanceStatistics() {
        // 测试获取性能统计
        
        CompletionPerformanceService.PerformanceStatistics stats = performanceService.getPerformanceStatistics();
        
        assertNotNull("性能统计不应为null", stats);
        
        // 验证所有统计指标都可以正常获取，不会抛出异常
        try {
            stats.getAverageResponseTime();
            stats.getTotalRequests();
            stats.getCacheHitCount();
            stats.getCacheMissCount();
            stats.getPrefetchCount();
            stats.getSuccessfulRequests();
            stats.getFailedRequests();
            
            // 如果方法执行到这里而没有抛出异常，测试就通过了
            assertTrue("获取性能统计方法应该正常执行", true);
        } catch (Exception e) {
            fail("获取性能统计方法不应该抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    public void testParallelProcessing() throws InterruptedException {
        // 测试并行处理能力
        final int threadCount = 5;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        // 创建多个线程同时访问性能服务
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    // 执行各种操作
                    performanceService.shouldPerformCompletion("test" + Thread.currentThread().getId());
                    performanceService.recordPerformanceMetric("parallel_test", 10);
                    
                    // 创建模拟对象进行预取测试
                    Editor mockEditor = mock(EditorImpl.class);
                    PsiFile mockFile = mock(PsiFileImpl.class);
                    performanceService.prefetchCompletions(mockEditor, mockFile, 0);
                    
                    latch.countDown();
                } catch (Exception e) {
                    fail("并行操作不应该抛出异常: " + e.getMessage());
                    latch.countDown();
                }
            });
        }
        
        // 等待所有线程完成
        boolean allCompleted = latch.await(2, TimeUnit.SECONDS);
        
        // 关闭线程池
        executor.shutdown();
        
        assertTrue("所有并行操作应该在超时前完成", allCompleted);
    }
    
    @Test
    public void testCalculateRequestPriority() {
        // 测试请求优先级计算
        
        // 创建不同的请求场景
        CompletionRequest request1 = new CompletionRequest();
        request1.setFileName("frequently_used.cs");
        request1.setLine(10);
        request1.setColumn(20);
        
        CompletionRequest request2 = new CompletionRequest();
        request2.setFileName("rarely_used.cs");
        request2.setLine(100);
        request2.setColumn(5);
        
        // 验证方法不会抛出异常并返回合理的值
        try {
            double priority1 = performanceService.calculateRequestPriority(request1);
            double priority2 = performanceService.calculateRequestPriority(request2);
            
            // 优先级应该是有效的数字（在0-1范围内或合理的优先级值）
            assertTrue("优先级应该是有效的数值", 
                    !Double.isNaN(priority1) && !Double.isInfinite(priority1));
            assertTrue("优先级应该是有效的数值", 
                    !Double.isNaN(priority2) && !Double.isInfinite(priority2));
            
        } catch (Exception e) {
            fail("计算请求优先级方法不应该抛出异常: " + e.getMessage());
        }
    }
    
    @Test
    public void testConcurrentPrefetchAndRetrieve() throws InterruptedException {
        // 测试并发预取和检索操作
        final CountDownLatch prefetchLatch = new CountDownLatch(1);
        final CountDownLatch retrieveLatch = new CountDownLatch(1);
        
        // 线程1: 执行预取
        Thread prefetchThread = new Thread(() -> {
            try {
                Editor mockEditor = mock(EditorImpl.class);
                PsiFile mockFile = mock(PsiFileImpl.class);
                performanceService.prefetchCompletions(mockEditor, mockFile, 10);
                prefetchLatch.countDown();
            } catch (Exception e) {
                fail("预取线程不应该抛出异常: " + e.getMessage());
                prefetchLatch.countDown();
            }
        });
        
        // 线程2: 尝试检索
        Thread retrieveThread = new Thread(() -> {
            try {
                CompletionRequest request = new CompletionRequest();
                request.setFileName("concurrent.cs");
                request.setLine(1);
                request.setColumn(1);
                
                List<CompletionItem> items = performanceService.getPrefetchedCompletions(request);
                // 结果可能为null，但方法不应抛出异常
                retrieveLatch.countDown();
            } catch (Exception e) {
                fail("检索线程不应该抛出异常: " + e.getMessage());
                retrieveLatch.countDown();
            }
        });
        
        // 启动线程
        prefetchThread.start();
        retrieveThread.start();
        
        // 等待完成
        boolean prefetchDone = prefetchLatch.await(1, TimeUnit.SECONDS);
        boolean retrieveDone = retrieveLatch.await(1, TimeUnit.SECONDS);
        
        // 等待线程终止
        prefetchThread.join(500);
        retrieveThread.join(500);
        
        assertTrue("预取操作应该在超时前完成", prefetchDone);
        assertTrue("检索操作应该在超时前完成", retrieveDone);
    }
}