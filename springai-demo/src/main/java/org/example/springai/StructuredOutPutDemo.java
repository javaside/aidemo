package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 演示 Spring AI 结构化输出的基础用法。
 *
 * <p>结构化输出让 AI 返回的内容直接转换为 Java 对象，无需手动解析 JSON。
 * 这个示例覆盖 5 种核心场景，适合新手快速入门。</p>
 *
 * <p>进阶内容请参考 {@link StructuredOutputAdvancedDemo}。</p>
 *
 * <h3>核心概念</h3>
 * <ul>
 *     <li><b>entity()</b>：将 AI 响应转换为 Java 对象</li>
 *     <li><b>BeanOutputConverter</b>：自动生成 JSON Schema 并转换对象</li>
 *     <li><b>ParameterizedTypeReference</b>：保留泛型信息（如 List&lt;T&gt;）</li>
 *     <li><b>responseEntity()</b>：同时获取结构化对象和原始响应</li>
 * </ul>
 */
public class StructuredOutPutDemo {

    public static void main(String[] args) {
        try {
            ChatClient chatClient = createChatClient();

            demoEntityWithClass(chatClient);
            demoEntityWithParameterizedType(chatClient);
            demoEntityWithConverter(chatClient);
            demoBeanOutputConverter(chatClient);
            demoResponseEntity(chatClient);
        }
        catch (Exception e) {
            System.err.println("结构化输出示例执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ChatClient createChatClient() {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-f429667b2e4a4581bc1a3bb873ffa69f")
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
     * entity(Class<T>): 将模型回复直接转换成指定 Java 类型。
     *
     * <p>这个重载最适合“返回一个固定结构对象”的场景，例如用户画像、电影信息、商品信息等。
     * 调用 entity(ActorsFilms.class) 时，ChatClient 会内部创建 BeanOutputConverter，
     * 自动把 ActorsFilms 的 JSON Schema 作为格式要求追加到用户消息后面，
     * 再把模型返回的 JSON 反序列化为 ActorsFilms 对象。</p>
     *
     * <p>学习重点：业务代码不需要手写 JSON 解析，也不需要手动拼接格式说明；
     * 只要定义好 record/class，就可以直接拿到类型安全的 Java 对象。</p>
     */
    static void demoEntityWithClass(ChatClient chatClient) {
        System.out.println("\n=== 1. entity(Class<T>)：返回一个对象 ===");
        System.out.println("目的：当你只需要把 AI 回复直接转成一个 Java 对象时，用这个方法最简单。");

        ActorsFilms actorsFilms = chatClient.prompt()
                .user("""
                        请列出演员周星驰的 3 部代表电影。
                        只返回 actor 和 movies 两个字段。
                        """)
                .call()
                .entity(ActorsFilms.class);

        Objects.requireNonNull(actorsFilms, "AI 未返回可解析的演员电影信息");
        System.out.println("演员：" + actorsFilms.actor());
        System.out.println("电影：" + actorsFilms.movies());
    }

    /**
     * entity(ParameterizedTypeReference<T>): 将模型回复转换成带泛型的 Java 类型。
     *
     * <p>Java 运行时会有泛型擦除，直接传 List.class 只能表达“这是一个 List”，
     * 不能表达“这是 List<ActorsFilms>”。ParameterizedTypeReference 可以保留完整泛型信息，
     * 因此适合列表、Map、嵌套泛型等结构化输出。</p>
     *
     * <p>学习重点：当返回结果不是单个对象，而是 List<ActorsFilms> 这类集合时，
     * 使用 new ParameterizedTypeReference<List<ActorsFilms>>() {}，
     * ChatClient 才知道集合元素也应该按 ActorsFilms 的结构转换。</p>
     */
    static void demoEntityWithParameterizedType(ChatClient chatClient) {
        System.out.println("\n=== 2. entity(ParameterizedTypeReference<T>)：返回泛型集合 ===");
        System.out.println("目的：当返回值是 List<T>、Map<K,V> 这类泛型类型时，用它保留完整泛型信息。");

        List<ActorsFilms> actorsFilmsList = chatClient.prompt()
                .user("""
                        请列出 2 位华语演员，每位演员给出 2 部代表电影。
                        JSON 顶层必须是数组。
                        """)
                .call()
                .entity(new ParameterizedTypeReference<List<ActorsFilms>>() {
                });

        Objects.requireNonNull(actorsFilmsList, "AI 未返回可解析的演员电影列表");
        actorsFilmsList.forEach(item ->
                System.out.println(item.actor() + " -> " + item.movies()));
    }

    /**
     * entity(StructuredOutputConverter<T>): 显式传入结构化输出转换器。
     *
     * <p>entity(Class<T>) 和 entity(ParameterizedTypeReference<T>) 会帮你创建默认转换器；
     * 如果你希望复用同一个转换器、查看转换器生成的格式说明、或者未来替换成自定义转换器，
     * 就可以直接传入 StructuredOutputConverter。</p>
     *
     * <p>这里使用 BeanOutputConverter，它会根据 ActorsFilms 生成 JSON Schema，
     * 并负责把模型返回的 JSON 转成 ActorsFilms。这个写法更适合教学、调试格式提示，
     * 或者需要把 converter 单独抽出来复用的场景。</p>
     */
    static void demoEntityWithConverter(ChatClient chatClient) {
        System.out.println("\n=== 3. entity(StructuredOutputConverter<T>)：显式传入输出转换器 ===");
        System.out.println("目的：当你想复用或定制输出转换器时，用这个重载更清楚。");

        // BeanOutputConverter 会生成格式提示，也负责把模型返回的 JSON 转成 Java 对象。
        BeanOutputConverter<ActorsFilms> outputConverter = new BeanOutputConverter<>(ActorsFilms.class);

        ActorsFilms actorsFilms = chatClient.prompt()
                .user("""
                        请列出演员梁朝伟的 3 部代表电影。
                        只返回 actor 和 movies 两个字段。
                        """)
                .call()
                .entity(outputConverter);

        Objects.requireNonNull(actorsFilms, "AI 未返回可解析的演员电影信息");
        System.out.println("演员：" + actorsFilms.actor());
        System.out.println("电影：" + actorsFilms.movies());
    }

    /**
     * BeanOutputConverter: 手动生成格式提示，并手动把 JSON 转成 Java 对象。
     *
     * <p>前面的 entity(...) 示例是 ChatClient.CallResponseSpec 的快捷写法：
     * ChatClient 会自动创建或使用 converter，自动追加格式提示，并自动调用 convert()。
     * 这个示例把这些步骤拆开演示，便于理解结构化输出背后的工作流程。</p>
     *
     * <p>学习重点：</p>
     * <ol>
     *     <li>new BeanOutputConverter<>(ActorsFilms.class) 根据 Java 类型生成 JSON Schema。</li>
     *     <li>getFormat() 得到一段“请按这个 JSON Schema 返回”的格式说明。</li>
     *     <li>PromptTemplate 把 format 填进用户提示词，约束模型只返回 JSON。</li>
     *     <li>content() 先拿到模型返回的原始 JSON 字符串。</li>
     *     <li>convert(json) 再把 JSON 字符串转换为 ActorsFilms 对象。</li>
     * </ol>
     *
     * <p>适用场景：想看清楚格式提示长什么样、想调试模型原始 JSON、或者需要在 ChatClient
     * 之外复用同一个转换器时，可以使用这种显式写法。</p>
     */
    static void demoBeanOutputConverter(ChatClient chatClient) {
        System.out.println("\n=== 4. BeanOutputConverter：手动生成格式提示并转换结果 ===");
        System.out.println("目的：拆开演示 getFormat()、content()、convert() 三个步骤。");

        BeanOutputConverter<ActorsFilms> outputConverter = new BeanOutputConverter<>(ActorsFilms.class);
        String format = outputConverter.getFormat();
        System.out.println("BeanOutputConverter 生成的 format 信息：");
        System.out.println(format);

        String userInputTemplate = """
                请列出演员张曼玉的 3 部代表电影。
                只返回 actor 和 movies 两个字段。
                {format}
                """;

        Prompt prompt = new Prompt(
                PromptTemplate.builder()
                        .template(userInputTemplate)
                        .variables(Map.of("format", format))
                        .build()
                        .createMessage());

        String json = chatClient.prompt(prompt)
                .call()
                .content();

        Objects.requireNonNull(json, "AI 未返回可解析的 JSON 字符串");
        ActorsFilms actorsFilms = outputConverter.convert(json);

        System.out.println("原始 JSON：" + json);
        System.out.println("演员：" + actorsFilms.actor());
        System.out.println("电影：" + actorsFilms.movies());
    }

    /**
     * responseEntity(Class<T>): 同时获取原始 ChatResponse 和结构化对象。
     *
     * <p>entity(...) 只返回转换后的业务对象；responseEntity(...) 会返回一个 ResponseEntity，
     * 里面同时包含原始 ChatResponse 和转换后的 entity。</p>
     *
     * <p>学习重点：如果你只关心业务数据，用 entity(...) 更简洁；
     * 如果你还需要排查模型原始输出、读取 metadata、查看结果数量、token 用量等响应信息，
     * 就使用 responseEntity(...)。</p>
     */
    static void demoResponseEntity(ChatClient chatClient) {
        System.out.println("\n=== 5. responseEntity(Class<T>)：同时拿到原始 ChatResponse 和结构化对象 ===");
        System.out.println("目的：当你除了结构化对象，还要查看 token、metadata、原始结果等响应信息时，用它。");

        ResponseEntity<ChatResponse, MovieRecommendation> responseEntity = chatClient.prompt()
                .user("""
                        请推荐一部适合学习团队协作的电影。
                        只返回 title、reason 和 score 三个字段，score 是 1 到 10 的整数。
                        """)
                .call()
                .responseEntity(MovieRecommendation.class);

        MovieRecommendation recommendation = responseEntity.entity();
        ChatResponse chatResponse = responseEntity.response();

        Objects.requireNonNull(recommendation, "AI 未返回可解析的电影推荐");
        Objects.requireNonNull(chatResponse, "AI 未返回原始 ChatResponse");
        System.out.println("推荐：" + recommendation.title());
        System.out.println("理由：" + recommendation.reason());
        System.out.println("评分：" + recommendation.score());
        System.out.println("原始结果数量：" + chatResponse.getResults().size());
    }
}

record ActorsFilms(String actor, List<String> movies) {
}

record MovieRecommendation(String title, String reason, int score) {
}
