package org.example.springai;

import org.example.springai.config.ApiKeyConfig;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.ListOutputConverter;
import org.springframework.ai.converter.MapOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.convert.support.DefaultConversionService;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演示 Spring AI 结构化输出的高级特性。
 *
 * <p>补充基础示例未覆盖的场景：ListOutputConverter、MapOutputConverter、
 * 枚举类型、嵌套对象、错误处理、自定义转换逻辑等。</p>
 */
public class StructuredOutputAdvancedDemo {

    public static void main(String[] args) {
        try {
            ChatClient chatClient = createChatClient();

            demoListOutputConverter(chatClient);
            demoMapOutputConverter(chatClient);
            demoEnumInStructuredOutput(chatClient);
            demoNestedComplexObject(chatClient);
            demoErrorHandling(chatClient);
            demoResponseEntityWithParameterizedType(chatClient);
        }
        catch (Exception e) {
            System.err.println("高级结构化输出示例执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ChatClient createChatClient() {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        DeepSeekChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT)
                        .temperature(0.0)
                        .build())
                .build();

        return ChatClient.builder(chatModel).build();
    }

    /**
     * ListOutputConverter: 将模型回复转换成简单字符串列表。
     *
     * <p>当你只需要一个字符串列表（不是复杂对象列表）时，ListOutputConverter 比
     * ParameterizedTypeReference<List<String>> 更直接。它会生成简单的格式提示，
     * 要求模型返回逗号分隔或 JSON 数组格式的字符串列表。</p>
     *
     * <p>学习重点：ListOutputConverter 专门处理 List<String>，
     * 适合"给我一个关键词列表"、"列出所有城市名"这类简单列表场景。</p>
     */
    static void demoListOutputConverter(ChatClient chatClient) {
        System.out.println("\n=== 1. ListOutputConverter：返回简单字符串列表 ===");
        System.out.println("目的：当只需要字符串列表时，用它比泛型更简洁。");

        ListOutputConverter listOutputConverter = new ListOutputConverter(
                new DefaultConversionService());

        String format = listOutputConverter.getFormat();
        System.out.println("ListOutputConverter 生成的格式提示：");
        System.out.println(format);

        List<String> cities = chatClient.prompt()
                .user(u -> u.text("""
                        请列出中国最受欢迎的 5 个旅游城市。
                        只返回城市名称列表。
                        {format}
                        """)
                        .param("format", format))
                .call()
                .entity(listOutputConverter);

        Objects.requireNonNull(cities, "AI 未返回可解析的城市列表");
        System.out.println("热门旅游城市：" + cities);
    }

    /**
     * MapOutputConverter: 将模型回复转换成 Map<String, Object>。
     *
     * <p>当你不想定义 Java 类，只需要一个灵活的键值对结构时，MapOutputConverter 很有用。
     * 它会要求模型返回 JSON 对象，然后转换成 Map。</p>
     *
     * <p>学习重点：MapOutputConverter 适合动态结构、临时查询、或者字段不固定的场景。
     * 缺点是失去了类型安全，需要手动转换值类型。</p>
     */
    static void demoMapOutputConverter(ChatClient chatClient) {
        System.out.println("\n=== 2. MapOutputConverter：返回灵活的 Map 结构 ===");
        System.out.println("目的：当不想定义 Java 类，只需要键值对时使用。");

        MapOutputConverter mapOutputConverter = new MapOutputConverter();

        String format = mapOutputConverter.getFormat();
        System.out.println("MapOutputConverter 生成的格式提示：");
        System.out.println(format);

        Map<String, Object> productInfo = chatClient.prompt()
                .user(u -> u.text("""
                        请提供 iPhone 15 Pro 的基本信息：
                        - name（产品名称）
                        - price（价格，数字）
                        - features（特性列表）
                        {format}
                        """)
                        .param("format", format))
                .call()
                .entity(mapOutputConverter);

        Objects.requireNonNull(productInfo, "AI 未返回可解析的产品信息");
        System.out.println("产品信息 Map：" + productInfo);
        System.out.println("产品名称：" + productInfo.get("name"));
        System.out.println("价格：" + productInfo.get("price"));
    }

    /**
     * 枚举类型在结构化输出中的使用。
     *
     * <p>BeanOutputConverter 支持枚举类型，会在 JSON Schema 中生成 enum 约束，
     * 引导模型只返回预定义的枚举值。这对分类、状态、级别等场景很有用。</p>
     *
     * <p>学习重点：在 record/class 中使用枚举字段，BeanOutputConverter 会自动
     * 在格式提示中列出所有可选值，提高模型返回正确枚举值的概率。</p>
     */
    static void demoEnumInStructuredOutput(ChatClient chatClient) {
        System.out.println("\n=== 3. 枚举类型：在结构化输出中使用枚举 ===");
        System.out.println("目的：让 AI 返回预定义的分类、状态、级别等枚举值。");

        BeanOutputConverter<BookReview> converter = new BeanOutputConverter<>(BookReview.class);
        System.out.println("包含枚举的 JSON Schema：");
        System.out.println(converter.getFormat());

        BookReview review = chatClient.prompt()
                .user("""
                        请对《三体》这本书进行评价。
                        返回书名、评分（1-5星）、类型（必须是 SCIENCE_FICTION, FANTASY, MYSTERY, ROMANCE, THRILLER 之一）、
                        和简短评语。
                        """)
                .call()
                .entity(BookReview.class);

        Objects.requireNonNull(review, "AI 未返回可解析的书评");
        System.out.println("书名：" + review.title());
        System.out.println("评分：" + review.rating() + " 星");
        System.out.println("类型：" + review.genre());
        System.out.println("评语：" + review.comment());
    }

    /**
     * 嵌套复杂对象的结构化输出。
     *
     * <p>BeanOutputConverter 支持任意深度的嵌套对象，会递归生成完整的 JSON Schema。
     * 这对复杂业务场景（订单、用户画像、配置信息等）很有用。</p>
     *
     * <p>学习重点：定义嵌套的 record/class，BeanOutputConverter 会自动处理整个对象树，
     * 不需要为每一层单独写转换逻辑。</p>
     */
    static void demoNestedComplexObject(ChatClient chatClient) {
        System.out.println("\n=== 4. 嵌套对象：处理复杂的多层结构 ===");
        System.out.println("目的：演示如何让 AI 返回包含嵌套对象的复杂结构。");

        CompanyProfile profile = chatClient.prompt()
                .user("""
                        请提供阿里巴巴集团的公司信息：
                        - 公司名称
                        - 成立年份
                        - 总部地址（包含城市和国家）
                        - 主要产品列表（每个产品包含名称和描述）
                        """)
                .call()
                .entity(CompanyProfile.class);

        Objects.requireNonNull(profile, "AI 未返回可解析的公司信息");
        System.out.println("公司：" + profile.name());
        System.out.println("成立：" + profile.foundedYear());
        System.out.println("总部：" + profile.headquarters().city() + ", " + profile.headquarters().country());
        System.out.println("主要产品：");
        profile.products().forEach(p ->
                System.out.println("  - " + p.name() + ": " + p.description()));
    }

    /**
     * 错误处理：当 AI 返回格式不正确时的处理。
     *
     * <p>虽然 BeanOutputConverter 会生成格式提示，但模型仍可能返回无法解析的内容。
     * 实际应用中应该捕获转换异常，记录原始响应，并提供降级方案。</p>
     *
     * <p>学习重点：生产环境中不要假设 AI 一定返回正确格式，
     * 要有异常处理、日志记录、重试机制或人工介入流程。</p>
     */
    static void demoErrorHandling(ChatClient chatClient) {
        System.out.println("\n=== 5. 错误处理：处理格式不正确的响应 ===");
        System.out.println("目的：演示如何优雅地处理 AI 返回格式错误的情况。");

        try {
            // 故意使用一个可能导致格式错误的提示
            ActorInfo actorInfo = chatClient.prompt()
                    .user("""
                            请用自然语言描述一下演员刘德华的职业生涯。
                            （注意：这个提示故意不要求 JSON 格式，可能导致解析失败）
                            """)
                    .call()
                    .entity(ActorInfo.class);

            System.out.println("成功解析：" + actorInfo);
        }
        catch (Exception e) {
            System.err.println("解析失败（预期行为）：" + e.getMessage());
            System.out.println("建议：");
            System.out.println("1. 在提示词中明确要求 JSON 格式");
            System.out.println("2. 使用 temperature=0 提高格式稳定性");
            System.out.println("3. 捕获异常并记录原始响应用于调试");
            System.out.println("4. 实现重试机制或降级到非结构化输出");
        }
    }

    /**
     * responseEntity 的泛型重载：同时获取原始响应和泛型集合。
     *
     * <p>responseEntity 也支持 ParameterizedTypeReference，
     * 可以在获取 List<T> 的同时保留原始 ChatResponse。</p>
     *
     * <p>学习重点：当你需要列表数据，又需要查看 token 用量、metadata 等信息时，
     * 使用 responseEntity(ParameterizedTypeReference)。</p>
     */
    static void demoResponseEntityWithParameterizedType(ChatClient chatClient) {
        System.out.println("\n=== 6. responseEntity 泛型重载：获取列表和原始响应 ===");
        System.out.println("目的：在获取泛型集合的同时，保留原始 ChatResponse 信息。");

        ResponseEntity<ChatResponse, List<TechStack>> responseEntity = chatClient.prompt()
                .user("""
                        请列出 3 个流行的 Java Web 框架，每个包含名称和简短描述。
                        JSON 顶层必须是数组。
                        """)
                .call()
                .responseEntity(new ParameterizedTypeReference<List<TechStack>>() {
                });

        List<TechStack> techStacks = responseEntity.entity();
        ChatResponse chatResponse = responseEntity.response();

        Objects.requireNonNull(techStacks, "AI 未返回可解析的技术栈列表");
        Objects.requireNonNull(chatResponse, "AI 未返回原始 ChatResponse");

        System.out.println("技术栈列表：");
        techStacks.forEach(stack ->
                System.out.println("  - " + stack.name() + ": " + stack.description()));

        System.out.println("响应元数据：");
        System.out.println("  结果数量：" + chatResponse.getResults().size());
        if (!chatResponse.getResults().isEmpty()) {
            var metadata = chatResponse.getResults().get(0).getMetadata();
            System.out.println("  完成原因：" + metadata.get("finishReason"));
        }
    }
}

// ========== 数据模型定义 ==========

/**
 * 演员电影信息（从基础示例复用）
 */
record ActorInfo(String name, List<String> movies) {
}

/**
 * 书评信息，包含枚举类型
 */
record BookReview(String title, int rating, BookGenre genre, String comment) {
}

/**
 * 书籍类型枚举
 */
enum BookGenre {
    SCIENCE_FICTION,
    FANTASY,
    MYSTERY,
    ROMANCE,
    THRILLER,
    NON_FICTION,
    BIOGRAPHY
}

/**
 * 公司信息，包含嵌套对象
 */
record CompanyProfile(
        String name,
        int foundedYear,
        Address headquarters,
        List<Product> products
) {
}

/**
 * 地址信息（嵌套对象）
 */
record Address(String city, String country) {
}

/**
 * 产品信息（嵌套对象）
 */
record Product(String name, String description) {
}

/**
 * 技术栈信息
 */
record TechStack(String name, String description) {
}
