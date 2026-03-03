package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;

import java.util.List;

/**
 * 演示 ChatClientRequestSpec 接口的三个 advisors 重载方法：
 *
 * 1. advisors(Consumer<AdvisorSpec> consumer) - 使用函数式接口配置 advisors
 * 2. advisors(Advisor... advisors) - 传入可变参数的 Advisor 数组
 * 3. advisors(List<Advisor> advisors) - 传入 Advisor 列表
 */
public class AdvisorDemo {
    public static void main(String[] args) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-21c106ac47104557a449fd02607319f8")
                .build();

        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        // 自定义 CallAdvisor: 添加响应前缀，演示如何获取参数
        CallAdvisor prefixAdvisor = new CallAdvisor() {
            @Override
            public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
                // 从 context 中获取参数
                var context = chatClientRequest.context();
                System.out.println("[PrefixAdvisor] 获取到参数 custom_param = " + context.get("custom_param"));

                ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
                System.out.println("[PrefixAdvisor] 响应内容: " + response.chatResponse().getResult().getOutput().getText());
                return response;
            }

            @Override
            public String getName() {
                return "PrefixAdvisor";
            }

            @Override
            public int getOrder() {
                return 0;
            }
        };

        // 自定义 CallAdvisor: 记录调用时间
        CallAdvisor timingAdvisor = new CallAdvisor() {
            @Override
            public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
                long startTime = System.currentTimeMillis();
                System.out.println("[TimingAdvisor] 开始调用: " + startTime);
                ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
                long endTime = System.currentTimeMillis();
                System.out.println("[TimingAdvisor] 调用结束: " + endTime + ", 耗时: " + (endTime - startTime) + "ms");
                return response;
            }

            @Override
            public String getName() {
                return "TimingAdvisor";
            }

            @Override
            public int getOrder() {
                return 10;
            }
        };

        // 自定义 CallAdvisor: 添加请求/响应计数器
        CallAdvisor counterAdvisor = new CallAdvisor() {
            private static int requestCount = 0;
            private static int responseCount = 0;

            @Override
            public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
                requestCount++;
                System.out.println("[CounterAdvisor] 请求计数: " + requestCount);
                ChatClientResponse response = callAdvisorChain.nextCall(chatClientRequest);
                responseCount++;
                System.out.println("[CounterAdvisor] 响应计数: " + responseCount);
                return response;
            }

            @Override
            public String getName() {
                return "CounterAdvisor";
            }

            @Override
            public int getOrder() {
                return 20;
            }
        };

        System.out.println("========== 方法 1: advisors(Consumer<AdvisorSpec> consumer) ==========");
        // 方法 1: 使用 Consumer<AdvisorSpec> 函数式接口配置 advisors
        // 这种方式可以在配置链中动态添加 advisor 并设置参数
        String result1 = chatClient.prompt()
                .user("请简短介绍一下自己")
                .advisors(a -> {
                    a.advisors(prefixAdvisor);          // 添加单个 advisor
                    a.param("custom_param", "my-value"); // 可以设置额外的参数
                    a.param("prefix", "[AI响应]");       // 可以设置多个参数
                    a.param("suffix", "[END]");
                })
                .call()
                .content();
        System.out.println(result1);
        System.out.println();

        System.out.println("========== 方法 2: advisors(Advisor... advisors) ==========");
        // 方法 2: 传入可变参数的 Advisor 数组
        // 这种方式适合直接传入多个 advisor 实例
        String result2 = chatClient.prompt()
                .user("请简短介绍一下自己")
                .advisors(prefixAdvisor, timingAdvisor, counterAdvisor)  // 传入多个 advisor
                .call()
                .content();
        System.out.println(result2);
        System.out.println();

        System.out.println("========== 方法 3: advisors(List<Advisor> advisors) ==========");
        // 方法 3: 传入 Advisor 列表
        // 这种方式适合从外部获取或动态构建的 advisor 列表
        List<Advisor> advisorList = List.of(prefixAdvisor, timingAdvisor, counterAdvisor);
        String result3 = chatClient.prompt()
                .user("请简短介绍一下自己")
                .advisors(advisorList)  // 传入 advisor 列表
                .call()
                .content();
        System.out.println(result3);
    }
}
