package org.example.springai.mcp.server;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.context.McpSyncRequestContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

/**
 * MCP Server Annotations Demo
 *
 * Demonstrates server-side functionality that triggers client handlers:
 * - @McpLogging / context.info()        → sends LoggingMessageNotification
 * - @McpProgress / context.progress()   → sends ProgressNotification
 * - @McpSampling / context.sample()     → sends CreateMessageRequest (LLM via client)
 * - @McpResource                        → static resource (annotations info)
 *
 * Server must be stateful (spring.ai.mcp.server.stateful=true) for context methods.
 */
@SpringBootApplication
public class McpServerAnnotationsDemo {

    public static void main(String[] args) {
        SpringApplication.run(McpServerAnnotationsDemo.class, args);
    }

    // ==================== Trigger Tools ====================

    /**
     * Tool: triggerLogging
     * Triggers a LoggingMessageNotification to the client via context.info().
     *
     * Real-world use: Server-side operations log structured messages to clients,
     * e.g., audit trails, debugging, displaying server activity to end users.
     */
    @McpTool(name = "triggerLogging", description = "触发日志通知演示")
    public Mono<String> triggerLogging(McpSyncRequestContext context) {
        context.info("这是一条来自服务器的 INFO 级别日志消息");
        context.info("服务器状态: 运行正常, 时间: " + java.time.LocalDateTime.now());
        return Mono.just("已向客户端发送 2 条日志通知 (INFO 级别)");
    }

    /**
     * Tool: triggerProgress
     * Sends three progress notifications (0%, 50%, 100%) to the client via context.progress().
     *
     * Real-world use: Long-running batch jobs (import, export, processing) report
     * progress so clients can display progress bars or percentage indicators.
     */
    @McpTool(name = "triggerProgress", description = "触发进度通知演示")
    public Mono<String> triggerProgress(McpSyncRequestContext context) {
        context.progress(p -> p.progress(0.0).total(1.0).message("任务开始: 准备数据..."));
        return Mono.just("step1")
            .delayElement(Duration.ofMillis(500))
            .doOnNext(s -> context.progress(p -> p.progress(0.5).total(1.0).message("任务进行中: 处理 50%...")))
            .delayElement(Duration.ofMillis(500))
            .doOnNext(s -> context.progress(p -> p.progress(1.0).total(1.0).message("任务完成: 100%")))
            .thenReturn("已向客户端发送 3 个进度通知 (0% → 50% → 100%)");
    }

    /**
     * Tool: triggerSampling
     * Sends a CreateMessageRequest to the client via context.sample().
     * The client forwards this to DeepSeek LLM and returns the result.
     *
     * Real-world use: Server acts as AI intermediary — the server doesn't need
     * its own LLM API key; instead it requests completions through the client
     * (multi-tenant scenarios, permission-controlled LLM access).
     */
    @McpTool(name = "triggerSampling", description = "触发采样请求演示")
    public Mono<String> triggerSampling(McpSyncRequestContext context) {
        if (!context.sampleEnabled()) {
            return Mono.just("采样未启用 (需要 stateful=true)");
        }
        var result = context.sample("请用一句话介绍 MCP (Model Context Protocol) 的作用");
        return Mono.just("采样结果: " + result.content());
    }

    /**
     * Tool: triggerToolListChanged
     * Reports the current tool list to the client.
     *
     * Note: The SDK does not expose a server-initiated tool list change notification API.
     * The @McpToolListChanged annotation on the client is for receiving notifications,
     * but the server must rely on the MCP protocol-level notifications/tools/changed
     * message which is not directly exposed via McpSyncRequestContext.
     *
     * Real-world use: Servers notify clients when available tools change —
     * e.g., after a plugin loads/unloads, or when user permissions change.
     * Clients keep their tool registry in sync by re-calling listTools().
     */
    @McpTool(name = "triggerToolListChanged", description = "触发工具列表变更通知演示")
    public Mono<String> triggerToolListChanged(McpSyncRequestContext context) {
        // Note: context.notifyToolListChanged() does not exist in the current SDK.
        // The server cannot proactively send tool list change notifications via the context API.
        // Clients should re-call listTools() to discover the current tool list.
        return Mono.just("SDK 不支持服务器主动发送工具列表变更通知。" +
            "客户端可通过调用 listTools() 获取当前工具列表。");
    }

    // ==================== Resource ====================

    /**
     * Resource: server://annotations/info
     * Static resource describing this demo's annotations.
     */
    @McpResource(uri = "server://annotations/info", name = "annotationsInfo", description = "本 demo 注解说明")
    public Mono<McpSchema.ReadResourceResult> getAnnotationsInfo() {
        String info = """
            MCP Annotations Demo — 服务器注解说明

            @McpLogging  → context.info()       服务器向客户端发送日志通知
            @McpProgress → context.progress()   服务器向客户端发送进度通知
            @McpSampling → context.sample()    服务器通过客户端请求 LLM 采样
            @McpResource → 静态资源             客户端可读取的服务端资源
            """;
        return Mono.just(new McpSchema.ReadResourceResult(List.of(
            new McpSchema.TextResourceContents("server://annotations/info", "text/plain", info)
        )));
    }
}
