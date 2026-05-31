package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

public class ToolsDemo {
    public static void main(String[] args) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();


        ToolCallback[] callbacks = ToolCallbacks.from(new DateTimeTools());
        ChatOptions options = ToolCallingChatOptions.builder().toolCallbacks(callbacks).build();

        Prompt prompt = new Prompt("What is the current date and time?", options);

        ChatResponse response = model.call(prompt);
        System.out.println(response.getResult().getOutput().getText());

        System.out.println("=========");

        //使用ChatClient
        ChatClient.Builder builder = ChatClient.builder(model);
        ChatClient chatClient = builder.build();
        String content = chatClient.prompt()
                .user("What is the current date and time?")
                .tools(new DateTimeTools())
                .call()
                .content();

        System.out.println(content);
    }
}
