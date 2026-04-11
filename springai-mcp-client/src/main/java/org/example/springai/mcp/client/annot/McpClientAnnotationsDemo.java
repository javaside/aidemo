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
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * MCP Client 注解 Handler（@Service Bean）
 *
 * 这些方法由 Spring AI 自动配置注册到 MCP Client 的 SSE 监听器。
 * 当服务器通过 context.info()、context.progress()、context.sample() 发送通知时，
 * 对应的 handler 会被调用。
 *
 * 注意：handler 在 @Service bean 中（和 MCP Client 在同一 Spring 上下文），
 * 而不是独立的 @SpringBootApplication 中。
 */
@Service
public class McpClientAnnotationsDemo {

    private final ChatClient chatClient;

    public McpClientAnnotationsDemo(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * @McpLogging handler
     * 接收并打印服务器发来的 LoggingMessageNotification。
     *
     * 真实用途: 服务器向客户端发送结构化日志。
     * 典型场景:
     *   - 服务端审计追踪：记录用户操作
     *   - 服务端调试：日志实时推送到客户端
     *   - 用户通知：服务器重要事件实时展示
     */
    @McpLogging(clients = "timemcp")
    public void handleLogging(LoggingMessageNotification notification) {
        System.out.println("\n  [Logging handler] 收到日志通知:");
        System.out.println("    级别: " + notification.level());
        System.out.println("    logger: " + notification.logger());
        System.out.println("    数据: " + notification.data());
    }

    /**
     * @McpProgress handler
     * 接收并打印服务器发来的 ProgressNotification。
     *
     * 真实用途: 长时运行的批量任务向客户端推送进度。
     * 典型场景:
     *   - 批量数据导入/导出
     *   - 文件处理（视频转码、文档解析）
     *   - AI 推理任务进度
     */
    @McpProgress(clients = "timemcp")
    public void handleProgress(ProgressNotification notification) {
        double pct = notification.progress() * 100;
        System.out.println("\n  [Progress handler] 收到进度通知:");
        System.out.println("    进度: " + String.format("%.0f%%", pct));
        System.out.println("    消息: " + notification.message());
    }

    /**
     * @McpSampling handler
     * 接收服务器的 CreateMessageRequest，通过 DeepSeek LLM 生成回复，返回 CreateMessageResult。
     *
     * 真实用途: 服务器不持有 LLM API key，委托客户端调用 LLM。
     * 典型场景:
     *   - 多租户：客户端统一管理 LLM 访问权限
     *   - 隐私保护：敏感数据不离开客户端网络
     *   - LLM 能力复用：多个服务端共享同一 LLM 连接
     */
    @McpSampling(clients = "timemcp")
    public CreateMessageResult handleSampling(CreateMessageRequest request) {
        System.out.println("\n  [Sampling handler] 收到 LLM 采样请求:");
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
     *
     * 真实用途: 服务器通知客户端可用工具发生变化。
     * 典型场景:
     *   - 动态插件系统：加载/卸载插件后通知客户端
     *   - 权限变更：工具可用性变化
     *   - 工具版本更新：schema 变更后通知
     */
    @McpToolListChanged(clients = "timemcp")
    public void handleToolListChanged(List<McpSchema.Tool> tools) {
        System.out.println("\n  [ToolListChanged handler] 收到工具列表更新:");
        System.out.println("    当前工具数: " + tools.size());
        tools.forEach(t -> System.out.println("    - " + t.name() + ": " + t.description()));
    }

    private String extractText(List<SamplingMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        var content = messages.get(0).content();
        return content instanceof McpSchema.TextContent tc ? tc.text() : content.toString();
    }
}
