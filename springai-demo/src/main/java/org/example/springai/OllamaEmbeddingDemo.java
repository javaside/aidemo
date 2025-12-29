package org.example.springai;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;

public class OllamaEmbeddingDemo {
    public static void main(String[] args) {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(options).build();

        float[] helloWorlds = embeddingModel.embed("hello world");
        System.out.println(ToStringBuilder.reflectionToString(helloWorlds));
    }
}
