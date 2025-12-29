package org.example.tools;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.internal.Json;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.CompleteToolCall;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ToolSpecificationExample {
    private static Map<String,String> citys = new HashMap<>();
    static {
        citys.put("北京","45°C");
        citys.put("上海","15°C");
    }

    public static void main(String[] args) throws IOException {
        
        // 1. 创建一个简单的工具规范 (ToolSpecification)
        ToolSpecification getCurrentWeatherTool = ToolSpecification.builder()
            .name("get_current_weather")
            .description("获取指定城市的当前天气")
            .parameters(JsonObjectSchema.builder()
                .addStringProperty("city", "城市名称，例如：北京、上海")
                .addStringProperty("country", "国家代码，例如：CN、US")
                .required("city")
                .build())
            .build();
            
        // 2. 创建另一个工具规范用于数学计算
        ToolSpecification calculatorTool = ToolSpecification.builder()
            .name("calculator")
            .description("执行基本数学计算")
            .parameters(JsonObjectSchema.builder()
                .addIntegerProperty("a", "第一个整数")
                .addIntegerProperty("b", "第二个整数")
                .addStringProperty("operation", "操作类型：add(加法)、subtract(减法)、multiply(乘法)、divide(除法)")
                .required("a", "b", "operation")
                .build())
            .build();

        // 4. 创建聊天请求，包含工具规范
        UserMessage userMessage = UserMessage.from("广州的天气怎么样？另外，计算一下 1134 + 28 等于多少？");

        sendStream(userMessage, getCurrentWeatherTool, calculatorTool);
        //send(userMessage, getCurrentWeatherTool, calculatorTool);
    }

    private static void sendStream(UserMessage userMessage, ToolSpecification getCurrentWeatherTool, ToolSpecification calculatorTool) throws IOException {
        // 3. 初始化 ChatModel (这里以 OpenAI 为例)
        OpenAiStreamingChatModel chatModel = OpenAiStreamingChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .logRequests(true)
                .logResponses(true)
                .modelName("gpt-4o-mini")
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(userMessage)
                .toolSpecifications(getCurrentWeatherTool, calculatorTool)
                .build();

        chatModel.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.println("onPartialResponse: " + partialResponse);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                System.out.println("onPartialToolCall: " + partialToolCall);
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                System.out.println("onCompleteToolCall: ================================================= ");
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                System.out.println("onCompleteResponse: " + completeResponse);
            }

            @Override
            public void onError(Throwable error) {
                error.printStackTrace();
            }
        });

        System.out.println("--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------");

        System.in.read();
    }

    private static void send(UserMessage userMessage, ToolSpecification getCurrentWeatherTool, ToolSpecification calculatorTool) {
        // 3. 初始化 ChatModel (这里以 OpenAI 为例)
        ChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .logRequests(true)
                .logResponses(true)
                .modelName("gpt-4o-mini")
                .build();

        ChatRequest chatRequest = ChatRequest.builder()
            .messages(userMessage)
            .toolSpecifications(getCurrentWeatherTool, calculatorTool)
            .build();

        // 5. 发送请求并获取响应
        ChatResponse response = chatModel.chat(chatRequest);
        AiMessage aiMessage = response.aiMessage();

        System.out.println("AI 回复: " + aiMessage);

        // 6. 检查 AI 是否需要调用工具
        if (aiMessage.hasToolExecutionRequests()) {
            System.out.println("AI 需要执行以下工具:");

            List<ChatMessage> messages  = new ArrayList<>();
            messages.add(userMessage);
            messages.add(aiMessage);

            // 7. 处理每个工具调用请求
            for (ToolExecutionRequest toolExecutionRequest : aiMessage.toolExecutionRequests()) {
                System.out.println("- 工具名称: " + toolExecutionRequest.name());
                System.out.println("  参数: " + toolExecutionRequest.arguments());

                // 8. 根据工具名称执行相应操作
                String toolResult = executeTool(toolExecutionRequest);

                // 9. 创建工具执行结果消息
                ToolExecutionResultMessage toolResultMessage =
                    ToolExecutionResultMessage.from(toolExecutionRequest, toolResult);
                messages.add(toolResultMessage);
            }
            // 10. 再次调用模型，传递工具执行结果
            ChatRequest followUpRequest = ChatRequest.builder()
                    .messages(messages)
                    .toolSpecifications(getCurrentWeatherTool, calculatorTool)
                    .build();

            ChatResponse followUpResponse = chatModel.chat(followUpRequest);
            System.out.println("最终回复: " + followUpResponse.aiMessage().text());
        }
    }

    /**
     * 根据工具执行请求执行相应的操作
     * 在实际应用中，这里会调用真实的API或执行具体业务逻辑
     */
    private static String executeTool(ToolExecutionRequest request) {
        Map<String, Object> map = Json.fromJson(request.arguments(), Map.class);
        map.forEach((key, value) -> System.out.println(key + ": " + value));

        switch (request.name()) {
            case "get_current_weather":

                // 模拟获取天气信息
                return citys.getOrDefault(map.get("city"),"12°C");
                
            case "calculator":
                // 解析参数并执行计算
                Integer a = (Integer) map.get("a");
                Integer b = (Integer) map.get("b");
                String operation = (String)map.get("operation");
                // 简化处理，实际应使用 JSON 解析库
                switch (operation){
                    case "add":
                        return String.valueOf(a + b);
                    case "subtract":
                        return String.valueOf(a - b);

                    case "multiply":
                        return String.valueOf(a * b);
                    case "divide":
                        return String.valueOf(a / b);
                }

                return "计算完成";
                
            default:
                return "未知工具";
        }
    }
}