package org.example.springai;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

/**
 * 文档分割演示 - 使用 DocumentReader 读取文件并分割
 *
 * 运行要求：
 * 1. 启动 Ollama Embedding 服务：ollama run Qwen3-Embedding
 * 2. resources/book.txt 文件存在
 */
public class DocumentSplitDemo {

    public static void main(String[] args) {
        // 1. 创建 EmbeddingModel
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        EmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build())
                .build();

        // 2. 使用 TextReader 读取 classpath 中的文件
        TextReader textReader = new TextReader(new ClassPathResource("book.txt"));
        List<Document> documents = textReader.read();

        System.out.println("=== 文档分割演示 ===");
        System.out.println("读取文件: book.txt");
        System.out.println("文档数量: " + documents.size() + "\n");

        // 3. 分割文档
        TokenTextSplitter splitter = new TokenTextSplitter();

        List<Document> chunks = splitter.split(documents.get(0));
        System.out.println("分割结果: " + chunks.size() + " 个 chunk\n");

        // 4. 添加 chunk 元数据
        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).getMetadata().put("chunk_index", i);
            chunks.get(i).getMetadata().put("total_chunks", chunks.size());
        }

        // 5. 存入向量数据库
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        vectorStore.add(chunks);
        System.out.println("已存入向量数据库: " + chunks.size() + " 个 chunk\n");

        // 6. 语义搜索演示
        System.out.println("--- 语义搜索 ---");
        String[] queries = {"深度学习应用", "RAG 是什么", "Transformer 架构"};

        for (String query : queries) {
            System.out.println("查询: " + query);
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(1).build());
            for (Document doc : results) {
                String text = doc.getText();
                System.out.println("  [块 " + doc.getMetadata().get("chunk_index") + "] " +
                        text.substring(0, Math.min(60, text.length())) + "...\n");
            }
        }
    }
}