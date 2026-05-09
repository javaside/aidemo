package org.example.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import redis.clients.jedis.JedisPooled;

import java.util.List;
import java.util.Map;

public class RedisVectorStoreDemo {
    public static void main(String[] args) {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(options).build();

        JedisPooled jedisPooled = new JedisPooled("localhost", 6379);

        RedisVectorStore vectorStore = RedisVectorStore
                .builder(jedisPooled, embeddingModel)
                .initializeSchema(true)
                .build();
        vectorStore.afterPropertiesSet(); //必须执行，才会创建索引

        //SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List <Document> documents = List.of(
                new Document("Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!! Spring AI rocks!!", Map.of("meta1", "meta1")),
                new Document("The World is Big and Salvation Lurks Around the Corner"),
                new Document("You walk forward facing the past and you turn back toward the future.", Map.of("meta2", "meta2")));

// Add the documents to Redis
        vectorStore.add(documents);

// Retrieve documents similar to a query
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder().query("Spring").topK(1).build());

        for (Document document : results) {
            System.out.println("===============");
            System.out.println(document.getId());
            System.out.println(document.getText());
        }
    }
}
