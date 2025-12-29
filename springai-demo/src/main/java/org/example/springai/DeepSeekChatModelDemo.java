package org.example.springai;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import reactor.core.publisher.Flux;

import java.io.IOException;

public class DeepSeekChatModelDemo {
    public static void main(String[] args) throws IOException {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-c5d30650dd584c0e8fadb1821441e169")
                .build();

        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatOptions chatOptions = DeepSeekChatOptions.builder().model(DeepSeekApi.ChatModel.DEEPSEEK_CHAT.value).build();
        Prompt prompt = Prompt.builder().content("你是什么版本的模型").chatOptions(chatOptions).build();

        //非流式接口，请求大模型
        ChatResponse response = chatModel.call(prompt);
        System.out.println(response);

        //流式接口，请求大模型
        Flux<ChatResponse> stream = chatModel.stream(prompt);
        stream.subscribe(string -> {
            System.out.println(string);
        });

        //阻塞进程退出
        System.in.read();
    }
}
