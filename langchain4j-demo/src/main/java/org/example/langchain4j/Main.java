package org.example.langchain4j;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class Main {
    public static void main(String[] args) {
        OpenAiChatModel model = OpenAiChatModel.builder()
                .baseUrl("http://langchain4j.dev/demo/openai/v1")
                .apiKey("demo")
                .modelName("gpt-4o-mini")
                .build();
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(5)
                .id("my-chat-memory")
                .build();
        String answer = model.chat("你是谁,给我讲个笑话");
        System.out.println(answer); // Hello World

        UserMessage m1 = UserMessage.from("我是tom,你能做什么，你是谁");
        memory.add(m1);

        System.out.println(model.chat(memory.messages()).aiMessage().text());

        UserMessage m2 = UserMessage.from("我是谁");
        memory.add(m2);
        System.out.println(model.chat(memory.messages()).aiMessage().text());

    }
}