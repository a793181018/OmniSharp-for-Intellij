package com.github.a793181018.omnisharpforintellij.editor.completion.model;

import com.google.gson.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.Map;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON转换器类，用于代码补全请求和响应的序列化与反序列化
 */
public class CompletionJsonConverter {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(CompletionItem.class, new CompletionItemDeserializer())
            .registerTypeAdapter(CompletionItem.CompletionKind.class, new CompletionKindSerializer())
            .create();

    /**
     * 将CompletionRequest转换为JSON字符串
     * @param request 补全请求对象
     * @return JSON字符串
     */
    @NotNull
    public static String toJson(@NotNull CompletionRequest request) {
        // 创建请求JSON对象
        JsonObject json = new JsonObject();
        json.addProperty("FileName", request.getFileName());
        json.addProperty("Line", request.getLine());
        json.addProperty("Column", request.getColumn());
        json.addProperty("Buffer", request.getBuffer());

        // 添加选项
        JsonObject options = new JsonObject();
        // 使用默认值设置选项
        options.addProperty("IncludeImportStatements", true);
        options.addProperty("IncludeSnippets", true);
        options.addProperty("IncludeKeywords", true);
        options.addProperty("IncludeMemberAccessCompletions", true);
        options.addProperty("IncludeTypeCompletions", true);
        options.addProperty("MaxResults", 100);
        options.addProperty("CaseSensitive", false);
        options.addProperty("UseCachedResults", false);
        json.add("Options", options);

        // 添加上下文信息
        JsonObject context = new JsonObject();
        // 使用默认上下文值
        context.addProperty("ContextType", "Unknown");
        context.addProperty("Prefix", "");
        context.addProperty("Offset", 0);
        json.add("Context", context);

        // 添加额外数据
        JsonObject additionalData = new JsonObject();
        for (Map.Entry<String, Object> entry : request.getAdditionalData().entrySet()) {
            additionalData.addProperty(entry.getKey(), entry.getValue().toString());
        }
        if (!additionalData.entrySet().isEmpty()) {
            json.add("AdditionalData", additionalData);
        }

        return GSON.toJson(json);
    }

    /**
     * 将JSON字符串转换为CompletionResponse
     * @param jsonString JSON响应字符串
     * @return 补全响应对象
     */
    @NotNull
    public static CompletionResponse fromJson(@NotNull String jsonString, long requestTimeMs) {
        try {
            JsonObject responseJson = JsonParser.parseString(jsonString).getAsJsonObject();
            List<CompletionItem> items = new ArrayList<>();

            // 解析补全项
            if (responseJson.has("Items") && responseJson.get("Items").isJsonArray()) {
                JsonArray itemsArray = responseJson.getAsJsonArray("Items");
                for (JsonElement itemElement : itemsArray) {
                    CompletionItem item = parseCompletionItem(itemElement.getAsJsonObject());
                    items.add(item);
                }
            }

            // 构建响应对象
            return new CompletionResponse.Builder()
                    .withItems(items)
                    .isIncomplete(responseJson.has("IsIncomplete") && responseJson.get("IsIncomplete").getAsBoolean())
                    .withRequestTimeMs(requestTimeMs)
                    .withResponseTimeMs(System.currentTimeMillis())
                    .build();
        } catch (Exception e) {
            return CompletionResponse.createErrorResponse("Failed to parse completion response: " + e.getMessage());
        }
    }

    /**
     * 解析单个补全项
     */
    private static CompletionItem parseCompletionItem(@NotNull JsonObject itemJson) {
        CompletionItem.Builder builder = new CompletionItem.Builder(
                itemJson.has("Label") ? itemJson.get("Label").getAsString() : "");

        if (itemJson.has("InsertText")) {
            builder.withInsertText(itemJson.get("InsertText").getAsString());
        }
        if (itemJson.has("SortText")) {
            builder.withSortText(itemJson.get("SortText").getAsString());
        }
        if (itemJson.has("FilterText")) {
            builder.withFilterText(itemJson.get("FilterText").getAsString());
        }
        if (itemJson.has("Kind")) {
            String kindStr = itemJson.get("Kind").getAsString();
            builder.withKind(mapCompletionKind(kindStr));
        }
        if (itemJson.has("Detail")) {
            builder.withDetail(itemJson.get("Detail").getAsString());
        }
        if (itemJson.has("Documentation")) {
            builder.withDocumentation(itemJson.get("Documentation").getAsString());
        }
        if (itemJson.has("InsertTextIsSnippet")) {
            builder.withInsertTextIsSnippet(itemJson.get("InsertTextIsSnippet").getAsBoolean());
        }
        if (itemJson.has("Priority")) {
            builder.withPriority(itemJson.get("Priority").getAsInt());
        }
        if (itemJson.has("Source")) {
            builder.withSource(itemJson.get("Source").getAsString());
        }

        // 解析附加文本编辑
        if (itemJson.has("AdditionalTextEdits") && itemJson.get("AdditionalTextEdits").isJsonArray()) {
            List<CompletionItem.TextEdit> textEdits = new ArrayList<>();
            JsonArray editsArray = itemJson.getAsJsonArray("AdditionalTextEdits");
            for (JsonElement editElement : editsArray) {
                JsonObject editJson = editElement.getAsJsonObject();
                if (editJson.has("StartOffset") && editJson.has("EndOffset") && editJson.has("NewText")) {
                    textEdits.add(new CompletionItem.TextEdit(
                            editJson.get("StartOffset").getAsInt(),
                            editJson.get("EndOffset").getAsInt(),
                            editJson.get("NewText").getAsString()
                    ));
                }
            }
            if (!textEdits.isEmpty()) {
                builder.withAdditionalTextEdits(textEdits);
            }
        }

        return builder.build();
    }

    /**
     * 映射补全项类型
     */
    @NotNull
    private static CompletionItem.CompletionKind mapCompletionKind(@NotNull String kindStr) {
        try {
            return CompletionItem.CompletionKind.valueOf(kindStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // 默认类型
            return CompletionItem.CompletionKind.TEXT;
        }
    }

    /**
     * CompletionItem反序列化器
     */
    private static class CompletionItemDeserializer implements JsonDeserializer<CompletionItem> {
        @Override
        @NotNull
        public CompletionItem deserialize(
                @NotNull JsonElement json, 
                @NotNull Type typeOfT, 
                @NotNull JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonObject()) {
                return parseCompletionItem(json.getAsJsonObject());
            }
            throw new JsonParseException("Expected JSON object for CompletionItem");
        }
    }

    /**
     * CompletionKind序列化器
     */
    private static class CompletionKindSerializer implements JsonSerializer<CompletionItem.CompletionKind> {
        @Override
        @NotNull
        public JsonElement serialize(
                @NotNull CompletionItem.CompletionKind src, 
                @NotNull Type typeOfSrc, 
                @NotNull JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    }
}