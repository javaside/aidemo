package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class DeepSeekAiDemo {
    public static void main(String[] args) throws IOException {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        ChatOptions chatOptions = ChatOptions.builder().model("deepseek-reasoner").build();
        Prompt prompt = Prompt.builder().content("你是什么版本的模型").chatOptions(chatOptions).build();

        //非流式接口，请求大模型
        String res = chatClient.prompt(prompt).call().content();
        System.out.println(res);

        //流式接口，请求大模型
        Flux<String> content = chatClient.prompt(prompt).stream().content();
        content.subscribe(string -> {
            System.out.println(string);
        });

        //阻塞进程退出
        System.in.read();

//        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
//        ChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(chatMemoryRepository)
//                .maxMessages(20)
//                .build();

        // 使用会话ID
//        String conversationId = "007";
//
//        chatClient.prompt()
//                .user("Do I have license to code?")
//                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
//                .call()
//                .content();


//        Flux<String> content = chatClient.prompt().user("我是tom").stream().content();
//
//        System.out.println("00000000000");
//        content.subscribe(string -> {
//            System.out.println(string);
//        });
//        System.out.println("------------");
//
//        String content2 = chatClient.prompt().user("现在是什么时间").tools(new DateTimeTools()).call().content();
//        System.out.println("====" + content2);
//
//        String content1 = chatClient.prompt().user("我是谁").call().content();
//        System.out.println(content1);
    }
}
