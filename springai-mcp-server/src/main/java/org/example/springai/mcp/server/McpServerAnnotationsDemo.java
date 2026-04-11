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
 * MCP Server 注解演示
 *
 * 演示服务端触发客户端处理器的能力:
 * - @McpLogging / context.info()        → 发送 LoggingMessageNotification
 * - @McpProgress / context.progress()   → 发送 ProgressNotification
 * - @McpSampling / context.sample()     → 发送 CreateMessageRequest (通过客户端调用 LLM)
 * - @McpResource                        → 静态资源 (本 demo 注解说明)
 *
 * 注意: 服务端必须配置为 stateful (spring.ai.mcp.server.stateful=true)
 *       才能使用 context.info()、context.progress()、context.sample() 等方法。
 */
@SpringBootApplication
public class McpServerAnnotationsDemo {

    public static void main(String[] args) {
        SpringApplication.run(McpServerAnnotationsDemo.class, args);
    }

    // ==================== 触发工具 ====================

    /**
     * 工具: triggerLogging
     * 通过 context.info() 向客户端发送 LoggingMessageNotification。
     *
     * 真实用途: 服务端向客户端发送结构化日志消息，例如:
     *   - 审计追踪: 记录用户在服务端执行的操作
     *   - 服务端调试: 实时推送日志到客户端，无需登录服务器
     *   - 用户通知: 服务器重要事件实时展示给客户端
     */
    @McpTool(name = "triggerLogging", description = "触发日志通知演示")
    public Mono<String> triggerLogging(McpSyncRequestContext context) {
        context.info("这是一条来自服务器的 INFO 级别日志消息");
        context.info("服务器状态: 运行正常, 时间: " + java.time.LocalDateTime.now());
        return Mono.just("已向客户端发送 2 条日志通知 (INFO 级别)");
    }

    /**
     * 工具: triggerProgress
     * 通过 context.progress() 向客户端发送 0% → 50% → 100% 三个进度通知。
     *
     * 真实用途: 长时运行的批量任务（导入/导出/数据处理）向客户端推送进度，
     * 客户端可据此显示进度条，改善用户体验。
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
     * 工具: triggerSampling
     * 通过 context.sample() 向客户端发送 CreateMessageRequest，
     * 客户端将请求转发给 DeepSeek LLM，将结果返回给服务端。
     *
     * 真实用途: 服务端作为 AI 中介 — 服务端不需要 LLM API key，
     * 所有 LLM 调用由客户端代理，适合多租户场景或权限受控的 LLM 访问。
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
     * 工具: triggerToolListChanged
     * 向客户端报告当前工具列表。
     *
     * 注意: 当前 SDK 未暴露服务器主动发送工具列表变更通知的 API。
     * 客户端的 @McpToolListChanged 注解用于接收通知，
     * 但服务端的 MCP 协议层 notifications/tools/changed 消息
     * 未通过 McpSyncRequestContext 直接暴露。
     *
     * 真实用途: 服务器通知客户端可用工具发生变化时 —
     * 例如插件加载/卸载、用户权限变更后，
     * 客户端重新调用 listTools() 同步工具注册表。
     */
    @McpTool(name = "triggerToolListChanged", description = "触发工具列表变更通知演示")
    public Mono<String> triggerToolListChanged(McpSyncRequestContext context) {
        // 注意: 当前 SDK 中 context.notifyToolListChanged() 方法不存在，
        // 服务器无法通过 context API 主动发送工具列表变更通知。
        // 客户端需要主动调用 listTools() 来发现当前工具列表。
        return Mono.just("SDK 不支持服务器主动发送工具列表变更通知。" +
            "客户端可通过调用 listTools() 获取当前工具列表。");
    }

    // ==================== 资源 ====================

    /**
     * 资源: server://annotations/info
     * 静态资源，描述本 demo 的注解功能。
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
