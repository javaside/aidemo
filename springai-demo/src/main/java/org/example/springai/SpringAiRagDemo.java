package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import java.util.List;

public class SpringAiRagDemo {
    public static void main(String[] args) {

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
                .baseUrl("https://api.deepseek.com")
                .apiKey("sk-f429667b2e4a4581bc1a3bb873ffa69f")
                .build();

        ChatModel chatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi)
                .build();

        ChatClient chatClient = ChatClient.builder(chatModel).build();

        OllamaApi ollamaApi = OllamaApi.builder().baseUrl("http://localhost:11434").build();
        OllamaEmbeddingOptions options = OllamaEmbeddingOptions.builder().model("Qwen3-Embedding").build();
        OllamaEmbeddingModel embeddingModel = OllamaEmbeddingModel.builder().ollamaApi(ollamaApi).defaultOptions(options).build();

        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        List<Document> documents = List.of(
                new Document("当前阶段，DeepSeek没有按照用户设置硬性并发上限。在系统总负载量较高时，基于系统负载和用户短时历史用量的动态限流模型可能会导致用户收到 503 或 429 错误码"));

        vectorStore.add(documents);

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore).build();

        ChatOptions chatOptions = ChatOptions.builder()
                .model("deepseek-reasoner").build();

        Prompt prompt = Prompt.builder().content("DeepSeek 调用模型时的并发限制是多少？").chatOptions(chatOptions).build();

        //非流式接口，请求大模型
        String res = chatClient.prompt(prompt).advisors(advisor).call().content();
        System.out.println("=======");
        System.out.println(res);
    }
}
