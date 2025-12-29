package org.example.langchain4j.tools;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.util.List;

public class ToolDemo {
    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .logRequests(true)
                .logResponses(true)
                .build();

        ToolSpecification toolSpecification = ToolSpecification.builder()
                .name("getWeather")
                .description("Returns the weather forecast for a given city")
                .parameters(JsonObjectSchema.builder()
                        .addStringProperty("city", "The city for which the weather forecast should be returned")
                        .addEnumProperty("temperatureUnit", List.of("CELSIUS", "FAHRENHEIT"))
                        .required("city") // the required properties should be specified explicitly
                        .build())
                .build();

        ChatRequest request = ChatRequest.builder()
                .messages(UserMessage.from("What will the weather be like in London tomorrow?"))
                .toolSpecifications(toolSpecification)
                .build();
        ChatResponse response = model.chat(request);
        AiMessage aiMessage = response.aiMessage();
        System.out.println(aiMessage.text());
    }
}
