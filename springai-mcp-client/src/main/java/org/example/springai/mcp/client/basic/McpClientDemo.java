package org.example.springai.mcp.client.basic;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.List;

/**
 * MCP 客户端演示：McpAsyncClient vs ChatClient 两种调用方式
 *
 * @McpTool      工具调用
 * @McpResource  资源读取
 * @McpComplete  URI 补全
 * @McpPrompt   提示模板
 */
@SpringBootApplication
public class McpClientDemo implements ApplicationRunner {

    private final ChatClient chatClient;
    private List<McpAsyncClient> mcpAsyncClients;
    private AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider;

    public McpClientDemo(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(McpClientDemo.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        mcpAsyncClients.forEach(mcpClient -> {

            // ==================== @McpTool 演示 ====================
            System.out.println("\n========== @McpTool ==========");
            System.out.println("【McpAsyncClient】listTools() 发现工具:");
            McpSchema.ListToolsResult toolsResult = mcpClient.listTools().block();
            if (toolsResult != null) {
                toolsResult.tools().forEach(t -> System.out.println("  - " + t.name() + ": " + t.description()));
            }

            System.out.println("\n【McpAsyncClient】直接调用工具:");
            McpSchema.CallToolRequest toolRequest = new McpSchema.CallToolRequest.Builder()
                .name("getCurrentDateTime")
                .arguments(new HashMap<>())
                .build();
            mcpClient.callTool(toolRequest).block().content().forEach(c ->
                System.out.println("  工具返回: " + ((McpSchema.TextContent) c).text()));

            System.out.println("\n【ChatClient】通过 toolCallbacks 调用工具:");
            String response = chatClient.prompt()
                    .user("当前时间是多少？")
                    .toolCallbacks(this.asyncMcpToolCallbackProvider.getToolCallbacks())
                    .call()
                    .content();
            System.out.println("  AI 回复: " + response);

            // ==================== @McpResource 演示 ====================
            /*
             * MCP Resources 两类：
             * 1. 固定资源 - URI 固定，如 system://server/info，可被 listResources() 发现
             * 2. 资源模板 - URI 含变量，如 config://{key}，listResources() 返回空
             */
            System.out.println("\n========== @McpResource ==========");

            // 1. 固定资源
            System.out.println("\n【类型1】固定资源 (system://server/info):");
            System.out.println("  listResources() 可发现:");
            McpSchema.ListResourcesResult resourcesResult = mcpClient.listResources().block();
            if (resourcesResult != null) {
                resourcesResult.resources().forEach(r ->
                    System.out.println("    - " + r.uri()));
            }
            System.out.println("  readResource() 读取:");
            resourcesResult.resources().stream()
                .filter(r -> r.uri().equals("system://server/info"))
                .findFirst()
                .ifPresent(r -> {
                    McpSchema.ReadResourceResult readResult = mcpClient.readResource(r).block();
                    if (readResult != null) {
                        readResult.contents().forEach(c -> {
                            if (c instanceof McpSchema.TextResourceContents tc) {
                                System.out.println("    " + tc.text());
                            }
                        });
                    }
                });

            // 2. 资源模板
            System.out.println("\n【类型2】资源模板 (config://{key}):");
            System.out.println("  listResources() 只能发现固定资源:");
            System.out.println("    资源模板 config://{key} 不会出现");
            System.out.println("  listResourceTemplates() 发现资源模板:");
            McpSchema.ListResourceTemplatesResult templatesResult = mcpClient.listResourceTemplates().block();
            if (templatesResult != null) {
                templatesResult.resourceTemplates().forEach(t ->
                    System.out.println("    - " + t.uriTemplate() + " (" + t.description() + ")"));
            }

            McpSchema.Resource configResource = new McpSchema.Resource(
                "config://app.name",    // uri - 资源标识符
                "app-config",           // name - 资源显示名称
                null,                   // title - 标题
                null,                   // description - 描述
                null);                  // mimeType - 媒体类型
            McpSchema.ReadResourceResult configResult = mcpClient.readResource(configResource).block();
            if (configResult != null) {
                configResult.contents().forEach(c -> {
                    if (c instanceof McpSchema.TextResourceContents tc) {
                        System.out.println("    " + tc.text());
                    }
                });
            }

            // ==================== @McpComplete 演示 ====================
            /*
             * MCP Complete 两种补全类型：
             * 1. ref/resource - 资源模板参数补全
             * 2. ref/prompt - 提示参数补全
             */
            System.out.println("\n========== @McpComplete ==========");

            // 1. 资源模板参数补全：config://{key} 的 key 参数
            System.out.println("\n【类型1】资源模板参数补全:");
            System.out.println("  用户输入 key='app'，请求补全:");
            // ref.type=ref/resource, ref.uri=资源模板, argument.name=参数名, argument.value=用户输入
            McpSchema.CompleteRequest configComplete = new McpSchema.CompleteRequest(
                new McpSchema.ResourceReference("config://{key}"),
                new McpSchema.CompleteRequest.CompleteArgument("key", "app"),
                new HashMap<>()
            );
            McpSchema.CompleteResult completeResult = mcpClient.completeCompletion(configComplete).block();
            if (completeResult != null) {
                System.out.println("  补全结果: " + completeResult.completion().values());
            }

            // 2. 提示参数补全：sqlGenerator 提示的 tableName 参数
            System.out.println("\n【类型2】提示参数补全:");
            System.out.println("  用户输入 tableName='u'，请求补全:");
            McpSchema.PromptReference promptRef = new McpSchema.PromptReference("sqlGenerator");
            McpSchema.CompleteRequest promptComplete = new McpSchema.CompleteRequest(
                promptRef,
                new McpSchema.CompleteRequest.CompleteArgument("tableName", "u"),
                new HashMap<>()
            );
            McpSchema.CompleteResult promptResult = mcpClient.completeCompletion(promptComplete).block();
            if (promptResult != null) {
                System.out.println("  补全结果: " + promptResult.completion().values());
            }

            // ==================== @McpPrompt 演示 ====================
            /*
             * MCP Prompts 作用：客户端传入参数，服务器返回完整提示词
             * 工作流程：listPrompts() 发现模板 -> getPrompt(name, args) 传入参数 -> 获取完整提示
             */
            System.out.println("\n========== @McpPrompt ==========");

            System.out.println("【步骤1】listPrompts() 发现可用提示:");
            McpSchema.ListPromptsResult promptsResult = mcpClient.listPrompts().block();
            if (promptsResult != null) {
                promptsResult.prompts().forEach(p ->
                    System.out.println("  - " + p.name() + ": " + p.description()));
            }

            System.out.println("\n【步骤2】getPrompt() 传入参数，获取完整提示:");
            System.out.println("  请求 sqlGenerator 提示，参数 tableName=users, operation=INSERT");
            HashMap<String, Object> sqlArgs = new HashMap<>();
            sqlArgs.put("tableName", "users");
            sqlArgs.put("operation", "INSERT");
            McpSchema.GetPromptResult sqlResult = mcpClient.getPrompt(
                new McpSchema.GetPromptRequest("sqlGenerator", sqlArgs)).block();
            if (sqlResult != null) {
                String promptText = extractText(sqlResult.messages().get(0).content());
                System.out.println("  完整提示: " + promptText);
            }

            System.out.println("\n【步骤3】getPrompt() 请求 meetingAgenda 提示:");
            HashMap<String, Object> meetingArgs = new HashMap<>();
            meetingArgs.put("topic", "Q2 季度复盘");
            meetingArgs.put("duration", 45);
            McpSchema.GetPromptResult meetingResult = mcpClient.getPrompt(
                new McpSchema.GetPromptRequest("meetingAgenda", meetingArgs)).block();
            if (meetingResult != null) {
                String promptText = extractText(meetingResult.messages().get(0).content());
                System.out.println("  完整提示: " + promptText);
            }

            mcpClient.closeGracefully().block();
        });
    }

    private String extractText(McpSchema.Content content) {
        return content instanceof McpSchema.TextContent tc ? tc.text() : content.toString();
    }

    @Autowired
    public void setMcpAsyncClients(List<McpAsyncClient> mcpAsyncClients) {
        this.mcpAsyncClients = mcpAsyncClients;
    }

    @Autowired
    public void setAsyncMcpToolCallbackProvider(AsyncMcpToolCallbackProvider provider) {
        this.asyncMcpToolCallbackProvider = provider;
    }
}
