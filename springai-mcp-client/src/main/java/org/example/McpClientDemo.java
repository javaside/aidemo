package org.example;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 演示如何使用springai-mcp-server
 * 运行前，请先运行springai-mcp-server模块
 */
@SpringBootApplication
public class McpClientDemo implements ApplicationRunner {

    private final ChatClient chatClient;

    private List<McpAsyncClient> mcpAsyncClients;
    private AsyncMcpToolCallbackProvider  asyncMcpToolCallbackProvider;

    public McpClientDemo(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(McpClientDemo.class);
        //配置非web项目
        //application.setWebApplicationType(WebApplicationType.NONE);
        application.run(args);
    }


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("=====");

        //通过mcpClient获取tool,resource
        mcpAsyncClients.forEach(mcpAsyncClient -> {
            McpSchema.ListToolsResult listToolsResultMono = mcpAsyncClient.listTools().block();
            listToolsResultMono.tools().forEach(tool -> System.out.println("tool: " + tool));
            mcpAsyncClient.listResources().block().resources().forEach(resource -> System.out.println("resource: " + resource));
        });

        System.out.println("=== without tools  ==");
        System.out.println(chatClient.prompt()
                .user("What is the current date and time?")
                .call()
                .content());

        System.out.println("=== tools ==");
        System.out.println(chatClient.prompt()
                .user("What is the current date and time?")
                .toolCallbacks(this.asyncMcpToolCallbackProvider.getToolCallbacks())
                .call()
                .content());
    }

    @Autowired
    public void setMcpAsyncClients(List<McpAsyncClient> mcpAsyncClients) {
        this.mcpAsyncClients = mcpAsyncClients;
    }

    @Autowired
    public void setAsyncMcpToolCallbackProvider(AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider) {
        this.asyncMcpToolCallbackProvider = asyncMcpToolCallbackProvider;
    }
}
