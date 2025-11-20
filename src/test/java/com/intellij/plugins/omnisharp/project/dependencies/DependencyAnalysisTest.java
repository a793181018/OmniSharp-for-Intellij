package com.intellij.plugins.omnisharp.project.dependencies;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 依赖分析功能测试
 */
public class DependencyAnalysisTest {
    
    private DependencyGraph graph;
    private ProjectNode projectA;
    private ProjectNode projectB;
    private ProjectNode projectC;
    private PackageNode package1;
    private PackageNode package2;
    private PackageNode package3;
    
    @Before
    public void setUp() {
        // 创建依赖图
        graph = new DependencyGraphImpl();
        
        // 创建项目节点
        Path projectAPath = Paths.get("C:\\Projects\\ProjectA");
        Path projectBPath = Paths.get("C:\\Projects\\ProjectB");
        Path projectCPath = Paths.get("C:\\Projects\\ProjectC");
        
        projectA = new ProjectNode(projectAPath, "ProjectA", "ProjectA.csproj");
        projectB = new ProjectNode(projectBPath, "ProjectB", "ProjectB.csproj");
        projectC = new ProjectNode(projectCPath, "ProjectC", "ProjectC.csproj");
        
        // 创建包节点
        package1 = new PackageNode("Newtonsoft.Json", "13.0.3");
        package2 = new PackageNode("Microsoft.Extensions.DependencyInjection", "7.0.0");
        package3 = new PackageNode("EntityFrameworkCore", "7.0.5");
        
        // 添加节点到图中
        graph.addNode(projectA);
        graph.addNode(projectB);
        graph.addNode(projectC);
        graph.addNode(package1);
        graph.addNode(package2);
        graph.addNode(package3);
        
        // 添加边
        graph.addEdge(projectA, projectB, EdgeType.PROJECT_REFERENCE);
        graph.addEdge(projectB, projectC, EdgeType.PROJECT_REFERENCE);
        graph.addEdge(projectA, package1, EdgeType.PACKAGE_REFERENCE);
        graph.addEdge(projectB, package2, EdgeType.PACKAGE_REFERENCE);
        graph.addEdge(projectC, package3, EdgeType.PACKAGE_REFERENCE);
        graph.addEdge(package2, package1, EdgeType.TRANSITIVE_DEPENDENCY);
    }
    
    @Test
    public void testDependencyGraphBasics() {
        // 测试节点数量
        assertEquals(6, graph.getNodes().size());
        assertTrue(graph.getNodes().contains(projectA));
        assertTrue(graph.getNodes().contains(projectB));
        assertTrue(graph.getNodes().contains(projectC));
        assertTrue(graph.getNodes().contains(package1));
        assertTrue(graph.getNodes().contains(package2));
        assertTrue(graph.getNodes().contains(package3));
        
        // 测试边数量
        assertEquals(6, graph.getEdges().size());
        
        // 测试获取特定边
        List<DependencyEdge> edgesAB = graph.getEdges(projectA, projectB);
        assertEquals(1, edgesAB.size());
        assertEquals(EdgeType.PROJECT_REFERENCE, edgesAB.get(0).getType());
        
        // 测试出边
        List<DependencyEdge> outgoingEdgesA = graph.getOutgoingEdges(projectA);
        assertEquals(2, outgoingEdgesA.size());
        
        // 测试入边
        List<DependencyEdge> incomingEdgesB = graph.getIncomingEdges(projectB);
        assertEquals(1, incomingEdgesB.size());
    }
    
    @Test
    public void testCycleDetection() {
        CycleDetector detector = new CycleDetector();
        
        // 初始图应该没有循环
        List<Cycle> cycles = detector.detectCycles(graph);
        assertTrue(cycles.isEmpty());
        
        // 添加循环依赖
        graph.addEdge(projectC, projectA, EdgeType.PROJECT_REFERENCE);
        
        // 现在应该有一个循环
        cycles = detector.detectCycles(graph);
        assertEquals(1, cycles.size());
        
        // 检查循环中的节点
        Cycle cycle = cycles.get(0);
        assertEquals(3, cycle.getNodes().size());
        assertTrue(cycle.getNodes().contains(projectA));
        assertTrue(cycle.getNodes().contains(projectB));
        assertTrue(cycle.getNodes().contains(projectC));
        
        // 检查节点是否在循环中
        assertTrue(detector.isInCycle(projectA, graph));
        assertTrue(detector.isInCycle(projectB, graph));
        assertTrue(detector.isInCycle(projectC, graph));
        assertFalse(detector.isInCycle(package1, graph));
    }
    
    @Test
    public void testTopologicalSort() {
        CycleDetector detector = new CycleDetector();
        
        // 初始图没有循环，应该可以拓扑排序
        List<DependencyNode> sorted = detector.topologicalSort(graph);
        assertNotNull(sorted);
        assertFalse(sorted.isEmpty());
        
        // 检查依赖顺序是否正确
        int indexA = sorted.indexOf(projectA);
        int indexB = sorted.indexOf(projectB);
        int indexC = sorted.indexOf(projectC);
        
        // ProjectA 依赖 ProjectB，所以 ProjectB 应该在 ProjectA 前面
        assertTrue(indexB < indexA);
        // ProjectB 依赖 ProjectC，所以 ProjectC 应该在 ProjectB 前面
        assertTrue(indexC < indexB);
        
        // 添加循环依赖后，拓扑排序应该返回null
        graph.addEdge(projectC, projectA, EdgeType.PROJECT_REFERENCE);
        sorted = detector.topologicalSort(graph);
        assertNull(sorted);
    }
    
    @Test
    public void testTransitiveClosure() {
        CycleDetector detector = new CycleDetector();
        Map<DependencyNode, Set<DependencyNode>> closure = detector.computeTransitiveClosure(graph);
        
        // 检查 ProjectA 的传递依赖
        Set<DependencyNode> projectADependencies = closure.get(projectA);
        assertNotNull(projectADependencies);
        assertTrue(projectADependencies.contains(projectB)); // 直接依赖
        assertTrue(projectADependencies.contains(projectC)); // 间接依赖
        assertTrue(projectADependencies.contains(package1)); // 直接依赖
        assertTrue(projectADependencies.contains(package2)); // 间接依赖
        assertTrue(projectADependencies.contains(package3)); // 间接依赖
    }
    
    @Test
    public void testVersionResolver() {
        // 测试版本解析
        VersionResolver.Version v1 = VersionResolver.parseVersion("1.2.3");
        assertEquals(1, v1.getMajor());
        assertEquals(2, v1.getMinor());
        assertEquals(3, v1.getPatch());
        assertEquals(0, v1.getRevision());
        assertNull(v1.getSuffix());
        
        VersionResolver.Version v2 = VersionResolver.parseVersion("1.2.3.4-beta01");
        assertEquals(1, v2.getMajor());
        assertEquals(2, v2.getMinor());
        assertEquals(3, v2.getPatch());
        assertEquals(4, v2.getRevision());
        assertEquals("beta01", v2.getSuffix());
        
        // 测试版本比较
        assertTrue(VersionResolver.compareVersions("1.2.3", "1.2.2") > 0);
        assertTrue(VersionResolver.compareVersions("1.2.3", "1.3.0") < 0);
        assertTrue(VersionResolver.compareVersions("1.2.3", "1.2.3") == 0);
        assertTrue(VersionResolver.compareVersions("1.2.3", "1.2.3-beta") > 0);
        
        // 测试最新版本
        List<String> versions = Arrays.asList("1.0.0", "1.2.0", "1.1.0", "2.0.0");
        assertEquals("2.0.0", VersionResolver.getLatestVersion(versions));
        
        // 测试版本范围
        VersionResolver.VersionRange range = VersionResolver.parseVersionRange("^1.0.0");
        assertTrue(VersionResolver.isVersionInRange("1.1.0", "^1.0.0"));
        assertTrue(VersionResolver.isVersionInRange("1.9.9", "^1.0.0"));
        assertFalse(VersionResolver.isVersionInRange("2.0.0", "^1.0.0"));
        
        assertTrue(VersionResolver.isVersionInRange("1.0.1", "~1.0.0"));
        assertFalse(VersionResolver.isVersionInRange("1.1.0", "~1.0.0"));
    }
    
    @Test
    public void testPackageVersionConflictDetection() {
        // 创建包依赖
        List<PackageDependency> dependencies = new ArrayList<>();
        dependencies.add(new PackageDependency(
                Paths.get("C:\\Projects\\ProjectA"),
                "ProjectA",
                "Newtonsoft.Json",
                "13.0.3",
                false
        ));
        dependencies.add(new PackageDependency(
                Paths.get("C:\\Projects\\ProjectB"),
                "ProjectB",
                "Newtonsoft.Json",
                "12.0.3",
                false
        ));
        dependencies.add(new PackageDependency(
                Paths.get("C:\\Projects\\ProjectC"),
                "ProjectC",
                "Microsoft.Extensions.DependencyInjection",
                "7.0.0",
                false
        ));
        
        // 检测冲突
        CycleDetector detector = new CycleDetector();
        List<PackageVersionConflict> conflicts = detector.detectPackageVersionConflicts(dependencies);
        
        // 应该有一个冲突
        assertEquals(1, conflicts.size());
        
        // 检查冲突详情
        PackageVersionConflict conflict = conflicts.get(0);
        assertEquals("Newtonsoft.Json", conflict.getPackageId());
        assertEquals(2, conflict.getConflictingVersions().size());
        assertTrue(conflict.getConflictingVersions().contains("13.0.3"));
        assertTrue(conflict.getConflictingVersions().contains("12.0.3"));
    }
    
    @Test
    public void testDependencyGraphModification() {
        // 测试移除节点
        graph.removeNode(projectA);
        assertFalse(graph.getNodes().contains(projectA));
        
        // 检查关联的边是否也被移除
        List<DependencyEdge> allEdges = graph.getEdges();
        for (DependencyEdge edge : allEdges) {
            assertFalse(edge.getSource().equals(projectA) || edge.getTarget().equals(projectA));
        }
        
        // 测试移除边
        int initialEdgeCount = graph.getEdges().size();
        graph.removeEdge(projectB, projectC);
        assertEquals(initialEdgeCount - 1, graph.getEdges().size());
        assertTrue(graph.getEdges(projectB, projectC).isEmpty());
        
        // 测试清空图
        graph.clear();
        assertTrue(graph.getNodes().isEmpty());
        assertTrue(graph.getEdges().isEmpty());
    }
    
    @Test
    public void testDependencyNodeEquality() {
        // 测试项目节点相等性
        ProjectNode projectACopy = new ProjectNode(
                projectA.getProjectPath(),
                projectA.getName(),
                projectA.getProjectFile()
        );
        assertEquals(projectA, projectACopy);
        assertEquals(projectA.hashCode(), projectACopy.hashCode());
        
        // 测试包节点相等性
        PackageNode package1Copy = new PackageNode(
                package1.getPackageId(),
                package1.getVersion()
        );
        assertEquals(package1, package1Copy);
        assertEquals(package1.hashCode(), package1Copy.hashCode());
        
        // 不同的包版本应该不相等
        PackageNode package1DifferentVersion = new PackageNode(
                package1.getPackageId(),
                "12.0.0"
        );
        assertNotEquals(package1, package1DifferentVersion);
    }
    
    @Test
    public void testDependencyEdgeEquality() {
        DependencyEdge edge1 = new DependencyEdge(projectA, projectB, EdgeType.PROJECT_REFERENCE);
        DependencyEdge edge2 = new DependencyEdge(projectA, projectB, EdgeType.PROJECT_REFERENCE);
        DependencyEdge edge3 = new DependencyEdge(projectB, projectA, EdgeType.PROJECT_REFERENCE);
        
        // 相同的源、目标和类型应该相等
        assertEquals(edge1, edge2);
        assertEquals(edge1.hashCode(), edge2.hashCode());
        
        // 源和目标不同应该不相等
        assertNotEquals(edge1, edge3);
    }
    
    @Test
    public void testToStringMethods() {
        // 测试节点的toString方法
        assertTrue(projectA.toString().contains("ProjectA"));
        assertTrue(package1.toString().contains("Newtonsoft.Json"));
        assertTrue(package1.toString().contains("13.0.3"));
        
        // 测试边的toString方法
        DependencyEdge edge = new DependencyEdge(projectA, projectB, EdgeType.PROJECT_REFERENCE);
        assertTrue(edge.toString().contains("ProjectA"));
        assertTrue(edge.toString().contains("ProjectB"));
        assertTrue(edge.toString().contains("PROJECT_REFERENCE"));
    }
}