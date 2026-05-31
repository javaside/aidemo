package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.List;

public class DeepSeekApiDemo {
    public static void main(String[] args) throws IOException {
        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey(ApiKeyConfig.getDeepSeekApiKey())
                .build();

        String message =  "你是谁";
        DeepSeekApi.ChatCompletionRequest request = createRequest(message);

        //非流式接口，请求大模型
        ResponseEntity<DeepSeekApi.ChatCompletion> chatCompletionResponseEntity = deepSeekApi.chatCompletionEntity(request);
        System.out.println(chatCompletionResponseEntity.getBody());

        //流式接口，请求大模型
        Flux<DeepSeekApi.ChatCompletionChunk> chatCompletionChunkFlux = deepSeekApi.chatCompletionStream(request);

        chatCompletionChunkFlux.subscribe(chunk -> {
            System.out.println(chunk);
        });

        //阻塞进程退出
        System.in.read();
    }

    private static DeepSeekApi.ChatCompletionRequest  createRequest(String messageStr) {
        DeepSeekApi.ChatCompletionMessage message = new DeepSeekApi.ChatCompletionMessage(messageStr, DeepSeekApi.ChatCompletionMessage.Role.USER);
        List<DeepSeekApi.ChatCompletionMessage> msgs = List.of(message);

        DeepSeekApi.ChatCompletionRequest request = new DeepSeekApi.ChatCompletionRequest(msgs, DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue(), 1.0);
        return request;
    }
}
