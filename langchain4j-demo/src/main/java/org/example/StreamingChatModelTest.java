package org.example;

import dev.langchain4j.model.chat.response.*;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.io.IOException;

public class StreamingChatModelTest {
    public static void main(String[] args) throws IOException {
        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
//                .baseUrl("https://api.deepseek.com/v1")
//                .apiKey("sk-1fd65c8a9f3a430ba4ad87b440c041fe")
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey("sk-f126474509894a68acf2ff0140890279")
                //.modelName("deepseek-chat")
                .modelName("qwen-plus")
                .build();
        String userMessage = "你是谁,给我讲个笑话";

        model.chat(userMessage, new StreamingChatResponseHandler() {

            @Override
            public void onPartialResponse(String partialResponse) {
                System.out.println("onPartialResponse: " + partialResponse);
            }

            @Override
            public void onPartialThinking(PartialThinking partialThinking) {
                System.out.println("onPartialThinking: " + partialThinking);
            }

            @Override
            public void onPartialToolCall(PartialToolCall partialToolCall) {
                System.out.println("onPartialToolCall: " + partialToolCall);
            }

            @Override
            public void onCompleteToolCall(CompleteToolCall completeToolCall) {
                System.out.println("onCompleteToolCall: " + completeToolCall);
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

        System.in.read() ;
    }
}
