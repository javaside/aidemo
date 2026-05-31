package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.expansion.QueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;

/**
 * 演示 RetrievalAugmentationAdvisor 的查询变换与扩展能力。
 *
 * 场景设计思路：
 * - 12 篇文档分布在 4 个语义维度（认证、限流、优化、监控），
 *   每个维度使用不同的术语体系。
 * - 单查询检索 (topK=3) 最多覆盖 1-2 个维度，容易遗漏其他维度的信息。
 * - RewriteQueryTransformer 先将口语化查询改写为更清晰的表述，
 *   再由 MultiQueryExpander 生成多个角度的变体，并行检索后合并，
 *   覆盖面远超单查询。
 *
 * RetrievalAugmentationAdvisor 的 Modular RAG 流程：
 *   1. QueryTransformer   — 查询变换
 *   2. QueryExpander       — 查询扩展（单查询 → 多查询，并行检索）
 *   3. DocumentRetriever   — 文档检索
 *   4. DocumentJoiner      — 合并多路检索结果
 *   5. DocumentPostProcessor — 文档后处理
 *   6. QueryAugmenter      — 上下文增强，发给 LLM 生成回答
 */
public class RetrievalAugmentationAdvisorDemo {

    public static void main(String[] args) {
        // ================================================================
        // 1. 构建基础设施
        // ================================================================
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();
        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // ================================================================
        // 2. 添加文档到向量库（12 篇，分布在 4 个语义维度）。
        //
        //    维度1 — 认证与配置
        //    维度2 — 限流与错误处理
        //    维度3 — 成本与调用优化
        //    维度4 — 监控与排查
        //
        //    用户问题涵盖多个维度，单查询的 topK 无法覆盖全部。
        // ================================================================
        vectorStore.add(List.of(
                // 维度1: 认证与配置
                new Document("API 使用 Bearer Token 认证，在请求头中添加 Authorization: Bearer <token>"),
                new Document("API Key 可在控制台的'密钥管理'页面创建和删除，每个账号最多创建5个 Key"),
                new Document("所有 API 通信必须使用 HTTPS 协议，不支持 HTTP 明文传输，TLS 版本需 ≥ 1.2"),

                // 维度2: 限流与错误处理
                new Document("系统默认每用户每分钟最多100次请求，超出后返回 HTTP 429 状态码"),
                new Document("当收到 429 时，客户端应读取 Retry-After 响应头，等待指定秒数后重试"),
                new Document("高优先级用户可在控制台申请提高限流阈值，最高可提升至每分钟1000次请求"),

                // 维度3: 成本与调用优化
                new Document("批量处理场景建议使用异步批处理接口，将多个请求合并为一次调用，减少请求次数"),
                new Document("系统支持 Webhook 回调机制，任务完成后主动推送结果，避免轮询消耗配额"),
                new Document("每次 API 调用按 token 数量计费，输入和输出 token 分别计价，长文本场景需注意成本控制"),

                // 维度4: 监控与排查
                new Document("API 响应头中的 X-Request-Id 是请求唯一标识，用于追踪链路，排查问题时需提供此 ID"),
                new Document("控制台提供 API 调用量和错误率的实时监控面板，可按小时、天、周维度查看统计数据"),
                new Document("常见 HTTP 错误码：400 参数错误、401 认证失败、403 权限不足、429 限流、500 服务内部错误")
        ));

        // ================================================================
        // 3. 构建 ChatClient.Builder
        // ================================================================
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);

        // ================================================================
        // 4. 构建 QueryTransformer — 查询改写。
        //    使用默认 prompt，将口语化查询改写为更适合检索的表述。
        // ================================================================
        QueryTransformer queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();

        // ================================================================
        // 5. 构建 MultiQueryExpander — 多角度查询扩展。
        //    将（改写后的）单个查询扩展为多个变体，从不同角度检索，
        //    并行执行后合并结果，扩大覆盖面。
        // ================================================================
        QueryExpander queryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(3)
                .includeOriginal(true)
                .build();

        // ================================================================
        // 6. 演示查询变换 + 扩展效果
        // ================================================================
        String originalQuestion = "API调用老是出问题，我该怎么办？";
        Query originalQuery = new Query(originalQuestion);

        System.out.println("===== RetrievalAugmentationAdvisor 查询扩展演示 =====");
        System.out.println("文档库：12篇，分布在4个维度");
        System.out.println("  1) 认证与配置   2) 限流与错误处理");
        System.out.println("  3) 成本与优化   4) 监控与排查");
        System.out.println("单查询 topK=3 → 最多覆盖 1-2 个维度");
        System.out.println("多查询 4路×topK=3 → 可覆盖全部4个维度");
        System.out.println();

        System.out.println("原始查询: " + originalQuestion);

        // Step A: QueryTransformer 改写
        Query rewritten = queryTransformer.transform(originalQuery);
        System.out.println("QueryTransformer 改写: " + rewritten.text());
        System.out.println();

        // Step B: QueryExpander 多角度扩展
        List<Query> expandedQueries = queryExpander.expand(rewritten);
        System.out.println("--- MultiQueryExpander 扩展为 " + expandedQueries.size() + " 个查询，并行检索 ---");
        for (int i = 0; i < expandedQueries.size(); i++) {
            String label = i == 0 ? "改写" : "变体" + i;
            System.out.println("  " + label + ": " + expandedQueries.get(i).text());
        }
        System.out.println();

        // ================================================================
        // 7. 构建 RetrievalAugmentationAdvisor
        //    流程：transform → expand → retrieve → join → augment → generate
        // ================================================================
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(3)   // 每路只取 top 3，单查询不足以覆盖全部维度
                .build();

        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.initialize();

        RetrievalAugmentationAdvisor advisor = RetrievalAugmentationAdvisor.builder()
                .queryTransformers(queryTransformer)       // 查询改写
                .queryExpander(queryExpander)              // 查询扩展（多路并行检索）
                .documentRetriever(documentRetriever)
                .taskExecutor(taskExecutor)
                .build();

        // ================================================================
        // 8. 调用完整 RAG 流程
        // ================================================================
        ChatClient chatClient = chatClientBuilder.build();

        System.out.println(">>> 开始 RAG 检索与生成...\n");

        String response = chatClient.prompt()
                .user(originalQuestion)
                .advisors(advisor)
                .call()
                .content();

        System.out.println("=== RetrievalAugmentationAdvisor（多查询扩展）===");
        System.out.println(response);

        // ================================================================
        // 9. 对比：QuestionAnswerAdvisor（单查询检索，默认 topK=4）
        //    单查询最多命中 1-2 个维度，回答覆盖面明显不如多查询。
        // ================================================================
        System.out.println("\n\n--- 对比：QuestionAnswerAdvisor（单查询检索） ---");

        QuestionAnswerAdvisor qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore).build();

        String qaResponse = chatClient.prompt()
                .user(originalQuestion)
                .advisors(qaAdvisor)
                .call()
                .content();

        System.out.println("=== QuestionAnswerAdvisor（单查询）===");
        System.out.println(qaResponse);

        taskExecutor.shutdown();
    }
}
