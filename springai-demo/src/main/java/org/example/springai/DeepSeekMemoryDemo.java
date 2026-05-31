package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class DeepSeekMemoryDemo {
    public static void main(String[] args) throws IOException {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        InMemoryChatMemoryRepository chatMemoryRepository = new InMemoryChatMemoryRepository();
        ChatMemory chatMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(20)
                .build();

        MessageChatMemoryAdvisor chatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        ChatClient chatClient = ChatClient.builder(chatModel).defaultAdvisors(chatMemoryAdvisor).build();

        // 会话ID
        String conversationId = "007";

        ChatOptions chatOptions = ChatOptions.builder().model("deepseek-reasoner").build();
        Prompt prompt = Prompt.builder().content("我的名字叫上海哥，你叫什么名字").chatOptions(chatOptions).build();

        //非流式接口，请求大模型
        String res = chatClient.prompt(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();

        System.out.println(res);

        Prompt prompt2 = Prompt.builder().content("请问我叫什么名字").chatOptions(chatOptions).build();
        String res2 = chatClient.prompt(prompt2)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .content();
        System.out.println(res2);
    }
}
