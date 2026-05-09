package org.example.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * PgVector Store 演示 - 编程方式配置向量数据库
 *
 * 运行要求：
 * 1. 启动 PgVector 数据库：
 *    docker run -it --rm --name postgres -p 5432:5432 \
 *      -e POSTGRES_USER=postgres \
 *      -e POSTGRES_PASSWORD=postgres \
 *      pgvector/pgvector
 *
 * 2. 启动 Ollama Embedding 服务：
 *    ollama run Qwen3-Embedding
 */
public class PgVectorStoreDemo {

    public static void main(String[] args) {
        // 1. 创建 EmbeddingModel（使用 Ollama）
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder()
                .model("Qwen3-Embedding")
                .build();
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();

        // 2. 编程方式创建 DataSource
        DataSource dataSource = DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/postgres")
                .username("postgres")
                .password("postgres")
                .driverClassName("org.postgresql.Driver")
                .build();

        // 3. 编程方式创建 JdbcTemplate
        org.springframework.jdbc.core.JdbcTemplate jdbcTemplate =
                new org.springframework.jdbc.core.JdbcTemplate(dataSource);

        // 4. 编程方式创建 PgVectorStore
        // 注意：Qwen3-Embedding 输出 4096 维向量，HNSW/IVFFlat 索引最多支持 2000 维
        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(4096)                    // Qwen3-Embedding 输出维度
                .indexType(PgIndexType.NONE)          // 高维向量暂不使用索引
                .maxDocumentBatchSize(100)           // 演示分批入库：每批最多 100 条
                .initializeSchema(true)              // 自动创建表结构
                .build();

        // 5. 初始化 schema
        vectorStore.afterPropertiesSet();

        System.out.println("=== 向量数据库演示开始 ===\n");

        // 6. 批量添加文档（内部自动分批）
        // 注意：add() 方法会自动按 maxDocumentBatchSize 分批入库
        System.out.println("--- 批量添加文档 ---");
        List<Document> documents = List.of(
                new Document("Spring AI 是一个 AI 框架，支持多种模型和向量数据库",
                        Map.of("category", "tech", "version", "1.0")),
                new Document("PgVector 是 PostgreSQL 的向量扩展，支持向量相似度搜索",
                        Map.of("category", "database", "version", "1.0")),
                new Document("人工智能正在改变世界，从医疗到金融各行各业",
                        Map.of("category", "ai", "version", "2.0")),
                new Document("深度学习模型需要大量的计算资源和数据",
                        Map.of("category", "ai", "version", "2.0")),
                new Document("Java 是最流行的企业级编程语言之一",
                        Map.of("category", "tech", "version", "3.0"))
        );
        vectorStore.add(documents);
        System.out.println("已添加 " + documents.size() + " 条文档\n");

        // 7. 相似度搜索
        System.out.println("--- 相似度搜索（查询：AI 框架） ---");
        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("AI 框架")
                        .topK(3)
                        .build());
        printResults(results);

        // 8. 带过滤条件的搜索
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

        // 9. 按 ID 删除
        System.out.println("--- 删除文档 ---");
        String docIdToDelete = documents.get(0).getId();
        vectorStore.delete(List.of(docIdToDelete));
        System.out.println("已删除文档: " + docIdToDelete + "\n");

        // 10. 验证删除
        System.out.println("--- 验证删除（查询：AI 框架） ---");
        List<Document> afterDelete = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("AI 框架")
                        .topK(5)
                        .build());
        System.out.println("剩余文档数: " + afterDelete.size());

        // 11. 获取原生客户端
        System.out.println("\n=== 演示完成 ===");
        vectorStore.getNativeClient().ifPresent(jdbc ->
                System.out.println("原生 JDBC 客户端: " + jdbc.getClass().getSimpleName()));
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