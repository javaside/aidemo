package org.example.springai.mcp.client.annot;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * MCP Client 注解演示启动器 - 精简版
 *
 * <p>此模块演示 Spring AI MCP 注解风格（@McpLogging / @McpProgress /
 * @McpSampling / @McpElicitation / @McpToolListChanged）下服务端→客户端
 * 的通知推送能力。</p>
 *
 * <p>运行步骤：</p>
 * <ol>
 *   <li>先启动 springai-mcp-server（端口 8080）：运行 {@code McpServerAnnotDemo}</li>
 *   <li>再启动本模块（端口 9090）：运行 {@code McpClientAnnotDemo}</li>
 * </ol>
 *
 * <p>预期效果：客户端收到服务端的 5 种通知，输出到控制台。</p>
 *
 * @see McpClientAnnotProviders
 */
@SpringBootApplication
public class McpClientAnnotDemo {

    /** 注入所有已注册的 McpAsyncClient（每个连接一个，本例为 timemcp） */
    private final java.util.List<McpAsyncClient> mcpAsyncClients;

    public McpClientAnnotDemo(java.util.List<McpAsyncClient> mcpAsyncClients) {
        this.mcpAsyncClients = mcpAsyncClients;
    }

    public static void main(String[] args) {
        SpringApplication.run(McpClientAnnotDemo.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        mcpAsyncClients.forEach(mcpClient -> {
            System.out.println("\n========== 调用 demoAll ==========");
            System.out.println("调用 server demoAll() 工具...");
            System.out.println("预期: 4 个 @Mcp* handler 会依次收到通知\n");

            try {
                /**
                 * 调用服务端的 demoAll 工具。
                 *
                 * 关键参数：
                 * - progressToken: 用于接收服务端 @McpProgress 通知。
                 *   若不设置，服务端的 context.progress() 无法将通知送达客户端。
                 *   值可任意，本例使用字符串 "demo-progress-token"。
                 */
                McpSchema.CallToolResult result = mcpClient.callTool(
                    new McpSchema.CallToolRequest.Builder()
                        .name("demoAll")
                        .arguments(new java.util.HashMap<>() {{ put("input", "测试输入内容"); }})
                        .progressToken("demo-progress-token")  // ← 必需：接收 @McpProgress 通知
                        .build()
                ).block();

                if (result != null && result.content() != null) {
                    result.content().forEach(c -> {
                        if (c instanceof McpSchema.TextContent tc) {
                            System.out.println("\n  工具返回:\n" + tc.text());
                        }
                    });
                }
            } catch (Exception e) {
                System.out.println("  工具调用异常: " + e.getMessage());
            }

            // 添加 Root - 客户端连接就绪后再调用
            // addRoot() 返回 Mono<Void>（内部会发送 rootsListChangedNotification），必须订阅才执行
            mcpClient.addRoot(new McpSchema.Root("file:///Users/demo/Desktop","Desktop")).subscribe();
        });
    }
}
