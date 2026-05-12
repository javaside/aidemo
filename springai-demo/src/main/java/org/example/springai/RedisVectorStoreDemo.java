package org.example.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Map;

/**
 * Redis Vector Store 演示 - 使用 Redis Stack 存储和检索向量。
 *
 * 运行要求：
 * 1. 启动 Redis Stack（普通 Redis 不包含 RediSearch/RedisJSON）：
 *    docker run --rm -p 6379:6379 redis/redis-stack:latest
 *
 * 2. 启动 Ollama Embedding 服务：
 *    ollama run Qwen3-Embedding
 *
 * 注意：
 * - initializeSchema(true) 只会在索引不存在时创建索引，不会修改已存在索引。
 * - 如果修改 metadataFields、embedding 模型或维度，需要删除旧索引，或换一个 indexName。
 */
public class RedisVectorStoreDemo {

    private static final String EMBEDDING_MODEL = "Qwen3-Embedding";

    public static void main(String[] args) {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model(EMBEDDING_MODEL).build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(options).build();

        try (JedisPooled jedisPooled = new JedisPooled("localhost", 6379)) {
            RedisVectorStore vectorStore = RedisVectorStore
                    .builder(jedisPooled, embeddingModel)
                    .indexName("spring-ai-demo-index")
                    .prefix("spring-ai-demo:")
                    // Redis 的 metadata 只有在这里显式注册后，才能进入 RediSearch 索引并参与过滤。
                    // 仅把字段放进 Document.metadata 里还不够。
                    .metadataFields(
                            RedisVectorStore.MetadataField.tag("category"),
                            RedisVectorStore.MetadataField.tag("source"))
                    .initializeSchema(true)
                    .build();
            vectorStore.afterPropertiesSet(); // 必须执行，才会创建索引

            List<Document> documents = List.of(
                    new Document("Spring AI 支持向量数据库和 RAG 检索",
                            Map.of("category", "spring", "source", "demo")),
                    new Document("Redis Stack 提供 RediSearch 和 RedisJSON 能力",
                            Map.of("category", "redis", "source", "demo")),
                    new Document("PgVector 是 PostgreSQL 的向量扩展",
                            Map.of("category", "postgres", "source", "demo")));

            vectorStore.add(documents);

            System.out.println("--- 相似度搜索（查询：Spring 向量检索） ---");
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("Spring 向量检索")
                            .topK(3)
                            .build());
            printResults(results);

            System.out.println("--- 过滤搜索（category = 'redis'） ---");
            // 这里能过滤成功，是因为上面把 category 注册成了 metadataField。
            List<Document> filteredResults = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("向量数据库")
                            .topK(3)
                            .filterExpression(new Filter.Expression(
                                    Filter.ExpressionType.EQ,
                                    new Filter.Key("category"),
                                    new Filter.Value("redis")))
                            .build());
            printResults(filteredResults);
        }
    }

    private static void printResults(List<Document> results) {
        for (Document document : results) {
            System.out.println("- ID: " + document.getId());
            System.out.println("  内容: " + document.getText());
            System.out.println("  元数据: " + document.getMetadata());
        }
        System.out.println();
    }
}
