package org.example.springai.mcp.client.annot;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springaicommunity.mcp.annotation.McpToolListChanged;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;

/**
 * MCP Client 注解演示（独立运行）
 *
 * 演示客户端如何处理服务端发起的通知:
 * - @McpLogging       接收并打印服务器的日志消息
 * - @McpProgress     接收并打印服务器的进度通知
 * - @MMcpSampling     接收服务器的 LLM 采样请求，转发到 DeepSeek，返回结果
 * - @McpToolListChanged 接收服务器的工具列表变更通知
 *
 * 运行方式:
 *   1. 先启动 springai-mcp-server (端口 8080)
 *   2. 再启动本模块 (端口 9090): cd springai-mcp-client && mvn spring-boot:run -Dspring.main.main-class=org.example.springai.mcp.client.annot.McpClientAnnotationsDemo
 *   或者单独运行 main() 方法
 */
@SpringBootApplication
public class McpClientAnnotationsDemo implements ApplicationRunner {

    private final ChatClient chatClient;
    private List<McpAsyncClient> mcpAsyncClients;

    public McpClientAnnotationsDemo(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpClientAnnotationsDemo.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        mcpAsyncClients.forEach(mcpClient -> {

            // ==================== @McpLogging ====================
            System.out.println("\n========== @McpLogging 演示 ==========");
            System.out.println("调用 server triggerLogging() 工具...");
            System.out.println("预期: @McpLogging handler 会打印服务器发来的日志通知\n");
            callToolAndPrint(mcpClient, "triggerLogging", new java.util.HashMap<>());

            // ==================== @McpProgress ====================
            System.out.println("\n========== @McpProgress 演示 ==========");
            System.out.println("调用 server triggerProgress() 工具...");
            System.out.println("预期: @McpProgress handler 会收到 0% → 50% → 100% 三个进度通知\n");
            callToolAndPrint(mcpClient, "triggerProgress", new java.util.HashMap<>());

            // ==================== @McpSampling ====================
            System.out.println("\n========== @McpSampling 演示 ==========");
            System.out.println("调用 server triggerSampling() 工具...");
            System.out.println("预期: @McpSampling handler 收到服务器的 CreateMessageRequest，");
            System.out.println("     转发到 DeepSeek LLM，将结果返回给服务器\n");
            callToolAndPrint(mcpClient, "triggerSampling", new java.util.HashMap<>());

            // ==================== @McpToolListChanged ====================
            System.out.println("\n========== @McpToolListChanged 演示 ==========");
            System.out.println("调用 server triggerToolListChanged() 工具...\n");
            callToolAndPrint(mcpClient, "triggerToolListChanged", new java.util.HashMap<>());

            mcpClient.closeGracefully().block();
        });
    }

    private void callToolAndPrint(McpAsyncClient mcpClient, String toolName, java.util.Map<String, Object> arguments) {
        try {
            McpSchema.CallToolResult result = mcpClient.callTool(
                new McpSchema.CallToolRequest.Builder()
                    .name(toolName)
                    .arguments(new java.util.HashMap<>(arguments))
                    .build()
            ).block();
            if (result != null && result.content() != null) {
                result.content().forEach(c -> {
                    if (c instanceof McpSchema.TextContent tc) {
                        System.out.println("  工具返回: " + tc.text());
                    }
                });
            }
        } catch (Exception e) {
            System.out.println("  工具调用异常: " + e.getMessage());
        }
    }

    // ==================== Client Handlers ====================

    /**
     * @McpLogging handler
     * 接收并打印服务器发来的 LoggingMessageNotification。
     */
    @McpLogging(clients = "timemcp")
    public void handleLogging(LoggingMessageNotification notification) {
        System.out.println("  [Logging handler] 收到日志通知:");
        System.out.println("    级别: " + notification.level());
        System.out.println("    logger: " + notification.logger());
        System.out.println("    数据: " + notification.data());
    }

    /**
     * @McpProgress handler
     * 接收并打印服务器发来的 ProgressNotification。
     */
    @McpProgress(clients = "timemcp")
    public void handleProgress(ProgressNotification notification) {
        double pct = notification.progress() * 100;
        System.out.println("  [Progress handler] 收到进度通知:");
        System.out.println("    进度: " + String.format("%.0f%%", pct));
        System.out.println("    消息: " + notification.message());
    }

    /**
     * @McpSampling handler
     * 接收服务器的 CreateMessageRequest，通过 DeepSeek LLM 生成回复，返回 CreateMessageResult。
     */
    @McpSampling(clients = "timemcp")
    public CreateMessageResult handleSampling(CreateMessageRequest request) {
        System.out.println("  [Sampling handler] 收到 LLM 采样请求:");
        System.out.println("    请求内容: " + extractText(request.messages()));

        String userMessage = extractText(request.messages());
        String llmResponse = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        System.out.println("    DeepSeek 回复: " + llmResponse);

        return CreateMessageResult.builder()
            .role(McpSchema.Role.ASSISTANT)
            .content(new McpSchema.TextContent(llmResponse))
            .model("deepseek-chat")
            .build();
    }

    /**
     * @McpToolListChanged handler
     * 接收服务器发来的工具列表变更通知。
     */
    @McpToolListChanged(clients = "timemcp")
    public void handleToolListChanged(List<McpSchema.Tool> tools) {
        System.out.println("  [ToolListChanged handler] 收到工具列表更新:");
        System.out.println("    当前工具数: " + tools.size());
        tools.forEach(t -> System.out.println("    - " + t.name() + ": " + t.description()));
    }

    private String extractText(List<SamplingMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        var content = messages.get(0).content();
        return content instanceof McpSchema.TextContent tc ? tc.text() : content.toString();
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setMcpAsyncClients(List<McpAsyncClient> mcpAsyncClients) {
        this.mcpAsyncClients = mcpAsyncClients;
    }
}
