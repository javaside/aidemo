package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Map;

/**
 * ChatClientUserDemo - 演示 ChatClientRequestSpec.user() 的重载方法
 */
public class ChatClientUserDemo {
    public static void main(String[] args) throws IOException, InterruptedException {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey("sk-21c106ac47104557a449fd02607319f8")
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(model).build();

        System.out.println("=== 演示 ChatClientRequestSpec.user() 的重载方法 ===\n");

        // 1. user(String message) - 最基本的形式，传入用户消息字符串
        System.out.println("1. user(String message):");
        String response1 = chatClient.prompt()
                .user("请用中文简单介绍一下Spring AI，不超过30字")
                .call()
                .content();
        System.out.println("回复: " + response1 + "\n");

        // 2. user(Resource resource) - 从资源文件加载用户消息
        System.out.println("2. user(Resource resource):");
        Resource messageResource = new ClassPathResource("user-message.txt");
        String response2 = chatClient.prompt()
                .user(messageResource)
                .call()
                .content();
        System.out.println("回复: " + response2.substring(0, Math.min(100, response2.length())) + "...\n");

        // 3. user(Consumer<ChatClient.PromptUserSpec> spec) - 使用 Consumer 配置 PromptUserSpec
        // 这个重载允许更细粒度地配置用户消息，包括文本、参数等
        System.out.println("3. user(Consumer<ChatClient.PromptUserSpec> spec):");
        String response3 = chatClient.prompt()
                .user(userSpec -> {
                    // 使用 text() 方法设置消息文本
                    userSpec.text("请用中文简单介绍一下 {topic}，不超过30字");

                    // 使用 params() 方法设置模板参数
                    userSpec.params(Map.of("topic", "人工智能"));

                    // 也可以组合使用其他配置方法
                    // 例如添加媒体、设置元数据等（视具体 API 版本而定）
                })
                .call()
                .content();
        System.out.println("回复: " + response3 + "\n");
        // 4. 实际上只有三个重载：user(String), user(Resource), user(Consumer<PromptUserSpec>)
        // 没有 user(Message) 或 user(String, Consumer) 重载
        System.out.println("总结：ChatClientRequestSpec.user() 有三个重载方法：");
        System.out.println("1. user(String message) - 直接传入字符串消息");
        System.out.println("2. user(Resource resource) - 从资源文件加载消息");
        System.out.println("3. user(Consumer<ChatClient.PromptUserSpec> spec) - 使用 Consumer 进行配置");

        System.out.println("\n=== 演示完成 ===");
    }
}
