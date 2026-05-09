package org.example.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * SimpleVectorStore 演示 - 轻量级内存向量数据库
 *
 * 特点：
 * - 基于内存存储，无需外部数据库
 * - 使用 ConcurrentHashMap 管理向量
 * - 支持序列化/反序列化到文件
 * - 仅用于测试和演示，不适合生产环境
 *
 * 运行要求：
 * 启动 Ollama Embedding 服务：ollama run Qwen3-Embedding
 */
public class SimpleVectorStoreDemo {

    public static void main(String[] args) throws Exception {
        // 1. 创建 EmbeddingModel（使用 Ollama）
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model("Qwen3-Embedding")
                .build();
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();

        // 2. 创建 SimpleVectorStore
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();

        System.out.println("=== SimpleVectorStore 演示开始 ===\n");

        // 3. 添加文档
        System.out.println("--- 添加文档 ---");
        List<Document> documents = List.of(
                new Document("Spring AI 是一个 AI 框架，支持多种模型和向量数据库",
                        Map.of("category", "tech", "version", "1.0")),
                new Document("Java 是最流行的企业级编程语言之一",
                        Map.of("category", "tech", "version", "3.0")),
                new Document("人工智能正在改变世界，从医疗到金融各行各业",
                        Map.of("category", "ai", "version", "2.0")),
                new Document("深度学习模型需要大量的计算资源和数据",
                        Map.of("category", "ai", "version", "2.0")),
                new Document("PgVector 是 PostgreSQL 的向量扩展，支持向量相似度搜索",
                        Map.of("category", "database", "version", "1.0"))
        );
        vectorStore.add(documents);
        System.out.println("已添加 " + documents.size() + " 条文档\n");

        // 4. 相似度搜索
        System.out.println("--- 相似度搜索（查询：AI 框架） ---");
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("AI 框架")
                        .topK(3)
                        .build());
        printResults(results);

        // 5. 带过滤条件的搜索
        System.out.println("--- 过滤搜索（category = 'ai'） ---");
        List<Document> filteredResults = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("深度学习")
                        .topK(5)
                        .filterExpression(new Filter.Expression(
                                Filter.ExpressionType.EQ,
                                new Filter.Key("category"),
                                new Filter.Value("ai")))
                        .build());
        printResults(filteredResults);

        // 6. 按 ID 删除
        System.out.println("--- 按 ID 删除文档 ---");
        String docIdToDelete = documents.get(0).getId();
        vectorStore.delete(List.of(docIdToDelete));
        System.out.println("已删除文档: " + docIdToDelete + "\n");

        // 7. 序列化到文件
        System.out.println("--- 序列化到文件 ---");
        Path tempFile = Files.createTempFile("vector_store", ".json");
        vectorStore.save(tempFile.toFile());
        System.out.println("已保存到: " + tempFile);
        System.out.println("文件大小: " + Files.size(tempFile) + " bytes\n");

        // 8. 从文件加载（验证反序列化）
        System.out.println("--- 从文件加载验证 ---");
        SimpleVectorStore loadedStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        loadedStore.load(tempFile.toFile());
        List<Document> loadedDocs = loadedStore.similaritySearch(
                SearchRequest.builder()
                        .query("Java 编程")
                        .topK(5)
                        .build());
        System.out.println("从文件加载后查询 'Java 编程'，找到 " + loadedDocs.size() + " 条文档");

        // 9. 清理临时文件
        Files.deleteIfExists(tempFile);

        System.out.println("\n=== 演示完成 ===");
    }

    private static void printResults(List<Document> results) {
        for (Document doc : results) {
            System.out.println("- ID: " + doc.getId());
            System.out.println("  内容: " + doc.getText());
            System.out.println("  元数据: " + doc.getMetadata());
        }
        System.out.println();
    }
}
