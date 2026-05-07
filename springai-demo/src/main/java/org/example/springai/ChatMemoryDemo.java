package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;

/**
 * Spring AI ChatMemoryAdvisor 三种实现方式的演示
 *
 * Spring AI 提供了不同的 Chat Memory Advisor 来管理对话历史：
 *
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │                    三种 Chat Memory Advisor 对比                                  │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │ 1. MessageChatMemoryAdvisor  (消息列表方式)                                      │
 * │    - 原理: 直接将历史消息插入到消息列表的适当位置                                  │
 * │    - 优点: 简单直接，保留原始消息结构                                            │
 * │    - 缺点: 不可自定义注入位置                                                   │
 * │    - 适用场景: 一般对话场景，不需要特殊格式                                        │
 * │    - 消息注入位置: 消息列表开头（在系统消息之后）                                 │
 * │                                                                                 │
 * │ 2. PromptChatMemoryAdvisor  (提示词注入方式)                                      │
 * │    - 原理: 将历史消息转换为文本，通过提示词模板注入到系统消息中                    │
 * │    - 优点: 灵活可控，可以自定义提示词模板                                          │
 * │    - 缺点: 需要额外的提示词模板配置                                                │
 * │    - 适用场景: 需要对历史消息有特殊格式要求的情况                                  │
 * │    - 消息注入位置: 通过提示词模板，通常作为系统消息的一部分                       │
 * │                                                                                 │
 * │ 3. VectorStoreChatMemoryAdvisor  (向量存储方式)                                    │
 * │    - 原理: 将历史消息存储在向量库中，通过语义检索获取相关历史                      │
 * │    - 优点: 可以检索最相关的历史消息，节省 token                                   │
 * │    - 缺点: 需要向量存储支持，检索可能不准确                                         │
 * │    - 适用场景: 长期对话、大量历史消息的场景                                        │
 * │    - 消息注入位置: 通过提示词模板注入相关消息                                      │
 * └─────────────────────────────────────────────────────────────────────────────────┘
 *
 * 如何选择：
 * ┌─────────────────────────────────────────────────────────────────────────────────┐
 * │ 选择建议                                                                         │
 * ├─────────────────────────────────────────────────────────────────────────────────┤
 * │ • MessageChatMemoryAdvisor:     默认选择，简单高效，适用于大多数场景             │
 * │ • PromptChatMemoryAdvisor:      需要自定义提示词格式时使用                        │
 * │ • VectorStoreChatMemoryAdvisor: 长期对话、大量历史消息、需要检索时使用           │
 * └─────────────────────────────────────────────────────────────────────────────────┘
 *
 * 消息注入顺序（多个 advisor 时）：
 * 系统消息 -> MessageChatMemoryAdvisor -> 用户消息 -> PromptChatMemoryAdvisor/其他
 */
public class ChatMemoryDemo {

    private static final String CONVERSATION_ID = "demo-conversation-001";

    public static void main(String[] args) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-7c7fc60f4b0747bf825dad131e03b2e1")
                .build();
        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        // ========================================================================
        // 演示 1: MessageChatMemoryAdvisor - 消息列表方式
        // ========================================================================
        System.out.println("========================================");
        System.out.println("演示 1: MessageChatMemoryAdvisor");
        System.out.println("========================================");
        demoMessageChatMemoryAdvisor(chatModel);

        // ========================================================================
        // 演示 2: PromptChatMemoryAdvisor - 提示词注入方式
        // ========================================================================
        System.out.println("\n========================================");
        System.out.println("演示 2: PromptChatMemoryAdvisor");
        System.out.println("========================================");
        demoPromptChatMemoryAdvisor(chatModel);

        // ========================================================================
        // 演示 3: VectorStoreChatMemoryAdvisor - 向量存储方式
        // ========================================================================
        System.out.println("\n========================================");
        System.out.println("演示 3: VectorStoreChatMemoryAdvisor");
        System.out.println("========================================");
        demoVectorStoreChatMemoryAdvisor(chatModel);
    }

    /**
     * 演示 MessageChatMemoryAdvisor
     *
     * 特点：
     * - 直接将历史消息插入到消息列表中
     * - 保留原始消息结构（包括角色、元数据等）
     * - 简单直接，易于使用
     *
     * 消息注入位置：消息列表开头（在系统消息之后）
     */
    private static void demoMessageChatMemoryAdvisor(ChatModel chatModel) {
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)  // 只保留最近 10 条消息
                .build();

        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(chatMemoryAdvisor)
                .build();

        // 第一轮对话
        String response1 = chatClient.prompt()
                .user("我叫上海哥，今年25岁，是一名程序员")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                .call()
                .content();
        System.out.println("用户: 我叫上海哥，今年25岁，是一名程序员");
        System.out.println("助手: " + response1);

        // 第二轮对话 - AI 应该记得用户的信息
        String response2 = chatClient.prompt()
                .user("我刚才说我今年多少岁？")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, CONVERSATION_ID))
                .call()
                .content();
        System.out.println("\n用户: 我刚才说我今年多少岁？");
        System.out.println("助手: " + response2);
    }

    /**
     * 演示 PromptChatMemoryAdvisor
     *
     * 特点：
     * - 将历史消息转换为文本，通过提示词模板注入
     * - 可以自定义历史消息的展示格式
     * - 灵活性高，但需要配置提示词模板
     *
     * 消息注入位置：通过提示词模板注入到系统消息中
     */
    private static void demoPromptChatMemoryAdvisor(ChatModel chatModel) {
        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(10)
                .build();

        PromptChatMemoryAdvisor promptChatMemoryAdvisor = PromptChatMemoryAdvisor.builder(chatMemory)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(promptChatMemoryAdvisor)
                .build();

        String customConversationId = "prompt-memory-demo-001";

        // 第一轮对话
        String response1 = chatClient.prompt()
                .user("我喜欢吃苹果和香蕉")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, customConversationId))
                .call()
                .content();
        System.out.println("用户: 我喜欢吃苹果和香蕉");
        System.out.println("助手: " + response1);

        // 第二轮对话
        String response2 = chatClient.prompt()
                .user("推荐一种水果给我")
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, customConversationId))
                .call()
                .content();
        System.out.println("\n用户: 推荐一种水果给我");
        System.out.println("助手: " + response2);
    }

    /**
     * 演示 VectorStoreChatMemoryAdvisor
     *
     * 特点：
     * - 将历史消息存储在向量库中
     * - 通过语义检索获取最相关的历史消息
     * - 适合长期对话和大量历史消息
     * - 可以节省 token（只检索相关消息）
     *
     * 注意：需要 embedding 模型和向量存储支持
     *
     * 消息注入位置：通过提示词模板注入相关消息
     */
    private static void demoVectorStoreChatMemoryAdvisor(ChatModel chatModel) {
        // 创建 Ollama embedding 模型
        // 注意：需要先启动 Ollama 服务，并拉取 embedding 模型
        // 例如：ollama pull Qwen3-Embedding
        // 默认模型名称可能需要根据实际情况调整
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(options)
                .build();

        // 创建内存向量存储
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 使用 VectorStoreChatMemoryAdvisor
        VectorStoreChatMemoryAdvisor vectorStoreMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(vectorStore)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(vectorStoreMemoryAdvisor)
                .build();

        String vectorConversationId = "vector-memory-demo-001";

        try {
            // 第一轮对话
            String response1 = chatClient.prompt()
                    .user("我在北京工作，做前端开发，使用 React 框架")
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, vectorConversationId))
                    .call()
                    .content();
            System.out.println("用户: 我在北京工作，做前端开发，使用 React 框架");
            System.out.println("助手: " + response1);

            // 第二轮对话 - AI 会检索相关的历史信息
            String response2 = chatClient.prompt()
                    .user("我使用什么技术栈？")
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, vectorConversationId))
                    .call()
                    .content();
            System.out.println("\n用户: 我使用什么技术栈？");
            System.out.println("助手: " + response2);
        } catch (Exception e) {
            System.out.println("向量存储方式运行出错：");
            System.out.println("请确保已启动 Ollama 服务并拉取 embedding 模型");
            System.out.println("1. 启动 Ollama: ollama serve");
            System.out.println("2. 拉取模型: ollama pull Qwen3-Embedding (或其他 embedding 模型)");
            System.out.println("3. 检查模型名称是否正确");
            e.printStackTrace();
        }
    }

}
