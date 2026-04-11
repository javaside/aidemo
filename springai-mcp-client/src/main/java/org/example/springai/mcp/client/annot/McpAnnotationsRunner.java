package org.example.springai.mcp.client.annot;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * MCP Client 注解演示启动器
 *
 * 运行方式:
 *   1. 先启动 springai-mcp-server (端口 8080)
 *   2. 再启动本模块 (端口 9090):
 *      cd springai-mcp-client && mvn spring-boot:run -Dspring.main.main-class=org.example.springai.mcp.client.annot.McpAnnotationsRunner
 *
 * 架构说明:
 *   - 本类是唯一的 @SpringBootApplication（统一 Spring 上下文）
 *   - McpClientAnnotationsDemo 是 @Service（handler 在同一上下文，能被自动配置扫描到）
 *   - McpClientDemo 是普通类（被本类调用，不创建独立上下文）
 */
@SpringBootApplication
@ComponentScan(
    basePackages = "org.example.springai.mcp.client",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = org.example.springai.mcp.client.basic.McpClientDemo.class
    )
)
public class McpAnnotationsRunner implements ApplicationRunner {

    private final McpAsyncClientRunner mcpRunner;

    public McpAnnotationsRunner(McpAsyncClientRunner mcpRunner) {
        this.mcpRunner = mcpRunner;
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpAnnotationsRunner.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        mcpRunner.run();
    }

    /**
     * 内部类：封装 MCP 客户端调用逻辑
     */
    @org.springframework.stereotype.Component
    public static class McpAsyncClientRunner {
        private final java.util.List<McpAsyncClient> mcpAsyncClients;

        public McpAsyncClientRunner(java.util.List<McpAsyncClient> mcpAsyncClients) {
            this.mcpAsyncClients = mcpAsyncClients;
        }

        public void run() throws Exception {
            mcpAsyncClients.forEach(mcpClient -> {

                // ==================== @McpLogging ====================
                System.out.println("\n========== @McpLogging 演示 ==========");
                System.out.println("调用 server triggerLogging() 工具...");
                System.out.println("预期: @McpLogging handler 会打印服务器发来的日志通知\n");
                callToolAndPrint(mcpClient, "triggerLogging", new java.util.HashMap<>());
                sleep(500); // 等待 SSE 通知传输

                // ==================== @McpProgress ====================
                System.out.println("\n========== @McpProgress 演示 ==========");
                System.out.println("调用 server triggerProgress() 工具...");
                System.out.println("预期: @McpProgress handler 会收到 0% → 50% → 100% 三个进度通知\n");
                callToolAndPrint(mcpClient, "triggerProgress", new java.util.HashMap<>());
                sleep(1500); // 等待所有进度通知传输

                // ==================== @McpSampling ====================
                System.out.println("\n========== @McpSampling 演示 ==========");
                System.out.println("调用 server triggerSampling() 工具...");
                System.out.println("预期: @McpSampling handler 收到服务器的 CreateMessageRequest，");
                System.out.println("     转发到 DeepSeek LLM，将结果返回给服务器\n");
                callToolAndPrint(mcpClient, "triggerSampling", new java.util.HashMap<>());
                sleep(500);

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

        private void sleep(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }
}
