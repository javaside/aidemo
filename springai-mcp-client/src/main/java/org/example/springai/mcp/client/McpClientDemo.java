package org.example.springai.mcp.client;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.example.commom.JsonSchemaArgumentGenerator;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

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

            McpSchema.ListToolsResult listToolsResult = mcpAsyncClient.listTools().block();
            listToolsResult.tools().forEach(tool -> {
                System.out.println("tool: " + tool);
                // 根据JsonSchema自动生成参数
                McpSchema.JsonSchema jsonSchema = tool.inputSchema();
                Map<String, Object> arguments = JsonSchemaArgumentGenerator.generateArgumentsFromSchema(jsonSchema);
                McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder().name(tool.name()).arguments(arguments).build();
                System.out.println("Generated tool request for " + tool.name() + " with arguments: " + toolRequest.arguments());
                mcpAsyncClient.callTool(toolRequest).block().content().forEach(System.out::println);
            });

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
