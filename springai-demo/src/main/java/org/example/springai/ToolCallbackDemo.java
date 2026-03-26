package org.example.springai;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.ai.tool.resolution.StaticToolCallbackResolver;
import org.springframework.ai.tool.resolution.ToolCallbackResolver;

/**
 * 演示 ChatClient.ChatClientRequestSpec 的 toolNames、tools、toolCallbacks、toolContext 四个方法
 */
public class ToolCallbackDemo {

    public static void main(String[] args) {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .apiKey("sk-f429667b2e4a4581bc1a3bb873ffa69f")
                .build();

        // 创建 ToolCallbackResolver，用于根据工具名称解析 ToolCallback
        // StaticToolCallbackResolver 将 ToolCallback 按名称注册到静态注册表
        ToolCallbackResolver toolCallbackResolver = new StaticToolCallbackResolver(
                List.of(ToolCallbacks.from(new DateTimeTools())));

        // 创建 ToolCallingManager，注入 ToolCallbackResolver
        ToolCallingManager toolCallingManager = ToolCallingManager.builder()
                .toolCallbackResolver(toolCallbackResolver)
                .build();

        DeepSeekChatModel model = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .toolCallingManager(toolCallingManager)
                .build();

        ChatClient chatClient = ChatClient.builder(model).build();

        // 示例1: tools - 直接传入工具对象实例
        demoTools(chatClient);

        // 示例2: toolCallbacks - 传入 ToolCallback 数组
        demoToolCallbacks(chatClient);

        // 示例3: toolNames - 通过工具名称从 ToolCallbackResolver 解析工具
        demoToolNames(chatClient);

        // 示例4: toolContext - 传入工具上下文数据
        demoToolContext(chatClient);
    }

    /**
     * tools: 直接传入工具对象实例
     * Spring AI 会自动从对象的方法上读取 @Tool 注解来注册工具
     */
    static void demoTools(ChatClient chatClient) {
        System.out.println("=== demoTools ===");

        String content = chatClient.prompt()
                .user("What is today's date?")
                .tools(new DateTimeTools())
                .call()
                .content();

        System.out.println("Result: " + content);
        System.out.println();
    }

    /**
     * toolCallbacks: 传入 ToolCallback 数组
     * ToolCallback 提供了更细粒度的工具定义和控制
     * 与 tools() 不同，toolCallbacks 直接接收 ToolCallback 接口的实现
     */
    static void demoToolCallbacks(ChatClient chatClient) {
        System.out.println("=== demoToolCallbacks ===");

        // 使用 ToolCallbacks.from() 将工具类转换为 ToolCallback 数组
        ToolCallback[] callbacks = ToolCallbacks.from(new DateTimeTools());

        String content = chatClient.prompt()
                .user("What is the current date and time?")
                .toolCallbacks(callbacks)
                .call()
                .content();

        System.out.println("Result: " + content);
        System.out.println();
    }

    /**
     * toolNames: 通过工具名称字符串从 ToolCallbackResolver 解析工具
     *
     * 与 tools/toolCallbacks 不同，toolNames 只指定工具名称，
     * 实际的 ToolCallback 由 ToolCallbackResolver 根据名称解析获取
     *
     * 使用场景：工具已注册到 ToolCallbackResolver，只需通过名称引用
     */
    static void demoToolNames(ChatClient chatClient) {
        System.out.println("=== demoToolNames ===");

        // 通过 toolNames() 指定要使用的工具名称
        // 工具名称会被 ToolCallbackResolver（此处为 StaticToolCallbackResolver）解析为实际的 ToolCallback
        String content = chatClient.prompt()
                .user("What is the current date and time?")
                .toolNames("getCurrentDateTime")  // 通过名称解析工具，不需要传 tools/toolCallbacks
                .call()
                .content();

        System.out.println("Result: " + content);
        System.out.println();
    }

    /**
     * toolContext: 传入工具上下文数据
     * toolContext 是一个 Map，用于向工具传递共享数据
     *
     * 演示使用 MethodToolCallback 和 FunctionToolCallback 两种方式接收 toolContext
     */
    static void demoToolContext(ChatClient chatClient) {
        System.out.println("=== demoToolContext ===");

        // 传入 toolContext 数据 - 这些数据会传递给工具的 ToolContext
        Map<String, Object> context = Map.of("userLanguage", "EN", "userId", "12345");

        // 方式1: 使用 ToolCallbacks.from() 创建 MethodToolCallback
        // GreetTools.greet() 方法包含 ToolContext 参数，MethodToolCallback 会自动注入
        GreetTools greetTools = new GreetTools();
        ToolCallback[] methodToolCallbacks = ToolCallbacks.from(greetTools);

        System.out.println("--- MethodToolCallback ---");
        String content1 = chatClient.prompt()
                .user("Use the greet tool to greet Alice")
                .toolCallbacks(methodToolCallbacks)
                .toolContext(context)
                .call()
                .content();
        System.out.println("Result: " + content1);

        // 方式2: 使用 FunctionToolCallback - 通过函数式接口
        // BiFunction<I, ToolContext, O> 签名允许直接接收 ToolContext
        ToolCallback functionToolCallback = FunctionToolCallback.builder("greetWithFunction",
                        (GreetRequest request, ToolContext toolContext) -> {
                            // 从 toolContext 中获取传入的数据
                            String userLanguage = (String) toolContext.getContext().get("userLanguage");
                            System.out.println("FunctionToolCallback User language: " + userLanguage);
                            String greeting = "EN".equals(userLanguage) ? "Hello" : "你好";
                            return greeting + ", " + request.name() + "!";
                        })
                .description("Greet the user. Input is the name of the person to greet.")
                .inputType(GreetRequest.class)
                .build();

        System.out.println("--- FunctionToolCallback ---");
        String content2 = chatClient.prompt()
                .user("Use the greetWithFunction tool to greet Bob")
                .toolCallbacks(functionToolCallback)
                .toolContext(context)
                .call()
                .content();
        System.out.println("Result: " + content2);

        System.out.println();
    }

    /**
     * 用于 MethodToolCallback 演示的工具类
     * 方法包含 ToolContext 参数，MethodToolCallback 会自动注入
     */
    static class GreetTools {
        @Tool(description = "Greet a person. Input is the name of the person to greet.")
        public String greet(String name, ToolContext toolContext) {
            String userLanguage = (String) toolContext.getContext().get("userLanguage");
            System.out.println("MethodToolCallback User language: " + userLanguage);
            String greeting = "EN".equals(userLanguage) ? "Hello" : "你好";
            return greeting + ", " + name + "!";
        }
    }

    /**
     * 用于 FunctionToolCallback 演示的请求类型
     */
    record GreetRequest(String name) {}
}
