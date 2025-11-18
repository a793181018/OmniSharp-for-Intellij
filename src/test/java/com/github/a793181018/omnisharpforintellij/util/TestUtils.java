package com.github.a793181018.omnisharpforintellij.util;

import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItem;
import com.github.a793181018.omnisharpforintellij.editor.completion.model.CompletionItemKind;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TestUtils {
    
    /**
     * 创建测试用的补全项列表
     * 
     * @return 包含测试补全项的列表
     */
    public static List<CompletionItem> createTestCompletionItems() {
        List<CompletionItem> items = new ArrayList<>();
        
        // 创建一些不同类型的补全项用于测试
        items.add(createTestItem("String", CompletionItemKind.CLASS, "System.String", "Represents text as a sequence of Unicode characters.", ":String", ":global::System.String"));
        items.add(createTestItem("StringBuilder", CompletionItemKind.CLASS, "System.Text.StringBuilder", "Represents a mutable string of characters.", ":StringBuilder", ":global::System.Text.StringBuilder"));
        items.add(createTestItem("ToString", CompletionItemKind.METHOD, "object.ToString", "Returns a string that represents the current object.", ".ToString()", ".ToString()"));
        items.add(createTestItem("Length", CompletionItemKind.PROPERTY, "String.Length", "Gets the number of characters in the current string object.", ".Length", ".Length"));
        items.add(createTestItem("Contains", CompletionItemKind.METHOD, "String.Contains", "Returns a value indicating whether a specified substring occurs within this string.", ".Contains($1)", ".Contains($1)"));
        
        return items;
    }
    
    /**
     * 创建单个测试用的补全项
     * 
     * @param label 补全项的标签
     * @param kind 补全项的类型
     * @param insertText 插入的文本
     * @param insertTextFormat 插入文本的格式
     * @return 创建的补全项
     */
    public static CompletionItem createTestItem(String label, CompletionItemKind kind, String description, String documentation, String insertText, String insertTextFormat) {
        CompletionItem item = new CompletionItem();
        item.setLabel(label);
        item.setKind(kind);
        item.setDescription(description);
        item.setDocumentation(documentation);
        item.setInsertText(insertText);
        item.setInsertTextFormat(insertTextFormat);
        
        // 设置一些额外的属性
        item.setFilterText(label.toLowerCase());
        item.setSortText("0" + label); // 确保测试项按字母顺序排序
        
        return item;
    }
    
    /**
     * 创建一个简单的C#测试文件内容
     * 
     * @param className 类名
     * @param methodName 方法名
     * @return 测试文件内容
     */
    public static String createTestCSharpFile(String className, String methodName) {
        return String.format("using System;\nusing System.Collections.Generic;\nusing System.Linq;\nusing System.Text;\nusing System.Threading.Tasks;\n\nnamespace TestNamespace\n{\n    public class %s\n    {\n        public void %s()\n        {\n            // Test method body\n        }\n    }\n}", className, methodName);
    }
    
    /**
     * 创建用于测试的不同前缀的补全请求数据
     * 
     * @return 前缀列表
     */
    public static List<String> createTestPrefixes() {
        return Arrays.asList(
            "S",       // 简单前缀
            "stri",    // 部分匹配
            "toS",     // 驼峰式前缀
            "",        // 空前缀
            "NonExistent" // 不存在的前缀
        );
    }
    
    /**
     * 验证补全项列表是否符合预期
     * 
     * @param items 实际的补全项列表
     * @param expectedCount 预期的数量
     * @param expectedLabels 预期包含的标签
     * @return 是否验证通过
     */
    public static boolean validateCompletionItems(List<CompletionItem> items, int expectedCount, List<String> expectedLabels) {
        if (items == null) {
            return false;
        }
        
        // 验证数量
        if (items.size() != expectedCount) {
            return false;
        }
        
        // 验证包含的标签
        for (String expectedLabel : expectedLabels) {
            boolean found = false;
            for (CompletionItem item : items) {
                if (expectedLabel.equals(item.getLabel())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        
        return true;
    }
}