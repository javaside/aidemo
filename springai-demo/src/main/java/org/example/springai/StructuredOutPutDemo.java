package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.core.ParameterizedTypeReference;

import java.util.List;
import java.util.Objects;

/**
 * 演示 ChatClient.CallResponseSpec 的结构化输出方法。
 */
public class StructuredOutPutDemo {

    private static final String API_KEY_ENV = "DEEPSEEK_API_KEY";

    public static void main(String[] args) {
        String apiKey = System.getenv(API_KEY_ENV);
        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("请先配置环境变量 " + API_KEY_ENV + "，再运行本示例。");
            return;
        }

        try {
            ChatClient chatClient = createChatClient(apiKey);

            demoEntityWithClass(chatClient);
            demoEntityWithParameterizedType(chatClient);
            demoEntityWithConverter(chatClient);
            demoResponseEntity(chatClient);
        }
        catch (Exception e) {
            System.err.println("结构化输出示例执行失败：" + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ChatClient createChatClient(String apiKey) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(apiKey)
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

    private static void demoEntityWithClass(ChatClient chatClient) {
        System.out.println("\n=== 1. entity(Class<T>)：返回一个对象 ===");

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

    private static void demoEntityWithParameterizedType(ChatClient chatClient) {
        System.out.println("\n=== 2. entity(ParameterizedTypeReference<T>)：返回泛型集合 ===");

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

    private static void demoEntityWithConverter(ChatClient chatClient) {
        System.out.println("\n=== 3. entity(StructuredOutputConverter<T>)：显式传入输出转换器 ===");

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

    private static void demoResponseEntity(ChatClient chatClient) {
        System.out.println("\n=== 4. responseEntity(Class<T>)：同时拿到原始 ChatResponse 和结构化对象 ===");

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
