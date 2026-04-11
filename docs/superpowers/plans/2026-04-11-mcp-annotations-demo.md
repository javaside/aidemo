# MCP Annotations Demo — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `McpServerAnnotationsDemo` (server) and `McpClientAnnotationsDemo` (client) demonstrating @McpLogging, @McpSampling, @McpProgress, @McpToolListChanged with explanations of their real-world purpose.

**Architecture:** Two-process demo — server exposes trigger tools that send notifications via `McpSyncRequestContext`; client registers handler methods annotated with the corresponding `@Mcp*` annotations. DeepSeek handles LLM sampling.

**Tech Stack:** Spring AI MCP Server/Client Boot Starters, Spring WebFlux, Java 21, `McpSyncRequestContext`, `CreateMessageRequest`/`CreateMessageResult`

---

## File Map

| File | Action | Responsibility |
|---|---|---|
| `springai-mcp-server/src/main/java/.../McpServerAnnotationsDemo.java` | Create | Server: 4 trigger tools, 1 resource, stateful context |
| `springai-mcp-server/src/main/resources/application.properties` | Modify | Add `spring.ai.mcp.server.stateful=true` |
| `springai-mcp-client/src/main/java/.../McpClientAnnotationsDemo.java` | Create | Client: 4 handler methods, trigger calls, DeepSeek sampling |
| `springai-mcp-client/src/main/resources/application.properties` | Read | Already configured for SSE connection |

---

## Task 1: Configure server as stateful

**Files:**
- Modify: `springai-mcp-server/src/main/resources/application.properties`

- [ ] **Step 1: Add stateful flag**

Add this line to `springai-mcp-server/src/main/resources/application.properties`:
```properties
spring.ai.mcp.server.stateful=true
```

The file will look like:
```properties
spring.ai.mcp.server.type=async
spring.ai.mcp.server.stateful=true
spring.ai.mcp.server.protocol=streamable
spring.ai.mcp.server.annotation-scanner.enabled=true
```

---

## Task 2: Create `McpServerAnnotationsDemo` on the server

**Files:**
- Create: `springai-mcp-server/src/main/java/org/example/springai/mcp/server/McpServerAnnotationsDemo.java`

The class uses `@McpTool` for 4 trigger tools, `@McpResource` for one resource, and injects `McpSyncRequestContext` to send notifications. The class is also a `@SpringBootApplication`.

**Full source code:**

```java
package org.example.springai.mcp.server;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

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
    public Mono<String> triggerLogging(io.modelcontextprotocol.server.McpSyncRequestContext context) {
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
    public Mono<String> triggerProgress(io.modelcontextprotocol.server.McpSyncRequestContext context) {
        context.progress(p -> p.progress(0.0).total(1.0).message("任务开始: 准备数据..."));
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        context.progress(p -> p.progress(0.5).total(1.0).message("任务进行中: 处理 50%..."));
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        context.progress(p -> p.progress(1.0).total(1.0).message("任务完成: 100%"));
        return Mono.just("已向客户端发送 3 个进度通知 (0% → 50% → 100%)");
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
    public Mono<String> triggerSampling(io.modelcontextprotocol.server.McpSyncRequestContext context) {
        if (!context.sampleEnabled()) {
            return Mono.just("采样未启用 (需要 stateful=true)");
        }
        var result = context.sample("请用一句话介绍 MCP (Model Context Protocol) 的作用");
        return Mono.just("采样结果: " + result.content());
    }

    /**
     * Tool: triggerToolListChanged
     * Notifies the client of the current tool list via server change-notification protocol.
     *
     * Real-world use: Servers notify clients when available tools change —
     * e.g., after a plugin loads/unloads, or when user permissions change.
     * Clients keep their tool registry in sync.
     */
    @McpTool(name = "triggerToolListChanged", description = "触发工具列表变更通知演示")
    public Mono<String> triggerToolListChanged(io.modelcontextprotocol.server.McpSyncRequestContext context) {
        // Report the current tool list — tools added/removed would be reflected here
        List<McpSchema.Tool> tools = List.of(
            new McpSchema.Tool("triggerLogging", "触发日志通知演示", null, List.of()),
            new McpSchema.Tool("triggerProgress", "触发进度通知演示", null, List.of()),
            new McpSchema.Tool("triggerSampling", "触发采样请求演示", null, List.of()),
            new McpSchema.Tool("triggerToolListChanged", "触发工具列表变更通知演示", null, List.of())
        );
        context.notifyToolListChanged(tools);
        return Mono.just("已向客户端发送工具列表变更通知，当前共 " + tools.size() + " 个工具");
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
```

---

## Task 3: Create `McpClientAnnotationsDemo` on the client

**Files:**
- Create: `springai-mcp-client/src/main/java/org/example/springai/mcp/client/McpClientAnnotationsDemo.java`

The class uses `@McpLogging`, `@McpSampling`, `@McpProgress`, `@McpToolListChanged` handler methods. It also injects `ChatClient` for the sampling handler (which calls DeepSeek).

**Full source code:**

```java
package org.example.springai.mcp.client;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springaicommunity.mcp.annotation.McpToolListChanged;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.AsyncMcpToolCallbackProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP Client Annotations Demo
 *
 * Demonstrates client-side handlers for server-initiated notifications:
 * - @McpLogging       接收并打印服务器的日志消息
 * - @McpProgress     接收并打印服务器的进度通知
 * - @McpSampling     接收服务器的 LLM 采样请求，转发到 DeepSeek，返回结果
 * - @McpToolListChanged 接收服务器的工具列表变更通知
 *
 * 运行方式:
 *   1. 先启动 springai-mcp-server (端口 8080)
 *   2. 再启动本模块 (端口 9090): cd springai-mcp-client && mvn spring-boot:run
 */
@SpringBootApplication
public class McpClientAnnotationsDemo implements ApplicationRunner {

    private final ChatClient chatClient;
    private List<McpAsyncClient> mcpAsyncClients;
    private AsyncMcpToolCallbackProvider asyncMcpToolCallbackProvider;

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
            /*
             * @McpLogging 的真实用途:
             * 服务器向客户端发送结构化日志消息。
             * 典型场景:
             *   1. 服务器端调试 — 将服务端日志实时推送客户端，开发者无需登录服务器查看
             *   2. 审计追踪 — 记录用户在服务端执行的操作，实时展示给客户端
             *   3. 用户通知 — 服务器的重要事件（如任务完成、告警）推送给客户端显示
             */
            System.out.println("\n========== @McpLogging 演示 ==========");
            System.out.println("调用 server triggerLogging() 工具...");
            System.out.println("预期: @McpLogging handler 会打印服务器发来的日志通知\n");
            callToolAndPrint(mcpClient, "triggerLogging", Map.of());

            // ==================== @McpProgress ====================
            /*
             * @McpProgress 的真实用途:
             * 服务器向客户端发送长时间运行任务的进度通知。
             * 典型场景:
             *   1. 批量数据导入/导出 — 服务器处理大量数据，实时推送进度
             *   2. 文件处理 — 视频转码、文档解析等耗时操作
             *   3. AI 推理 — 复杂推理任务的中间状态推送
             * 客户端可以用这个来显示进度条、改善用户体验。
             */
            System.out.println("\n========== @McpProgress 演示 ==========");
            System.out.println("调用 server triggerProgress() 工具...");
            System.out.println("预期: @McpProgress handler 会收到 0% → 50% → 100% 三个进度通知\n");
            callToolAndPrint(mcpClient, "triggerProgress", Map.of());

            // ==================== @McpSampling ====================
            /*
             * @McpSampling 的真实用途:
             * 服务器通过客户端向 LLM 发起采样请求，客户端负责实际调用 LLM。
             * 关键优势: 服务器不需要 LLM API key，所有 LLM 调用由客户端代理。
             * 典型场景:
             *   1. 多租户场景 — 客户端统一管理 LLM 访问权限，服务器按需请求
             *   2. 隐私保护 — 敏感数据不离开客户端网络，由客户端负责 LLM 调用
             *   3. LLM 能力复用 — 多个服务端共享同一个客户端的 LLM 连接
             * 以下 handler 收到服务器的采样请求后，自动调用 DeepSeek LLM。
             */
            System.out.println("\n========== @McpSampling 演示 ==========");
            System.out.println("调用 server triggerSampling() 工具...");
            System.out.println("预期: @McpSampling handler 收到服务器的 CreateMessageRequest，");
            System.out.println("     转发到 DeepSeek LLM，将结果返回给服务器\n");
            callToolAndPrint(mcpClient, "triggerSampling", Map.of());

            // ==================== @McpToolListChanged ====================
            /*
             * @McpToolListChanged 的真实用途:
             * 服务器通知客户端可用工具列表发生了变化。
             * 典型场景:
             *   1. 动态插件系统 — 加载/卸载插件后，通知客户端工具列表已更新
             *   2. 权限变更 — 用户权限变化后，服务器通知客户端哪些工具可用/不可用
             *   3. 工具版本更新 — 工具 schema 变更后，通知客户端重新发现工具
             * 客户端收到通知后通常会重新调用 listTools() 更新本地注册表。
             */
            System.out.println("\n========== @McpToolListChanged 演示 ==========");
            System.out.println("调用 server triggerToolListChanged() 工具...");
            System.out.println("预期: @McpToolListChanged handler 收到当前工具列表\n");
            callToolAndPrint(mcpClient, "triggerToolListChanged", Map.of());

            mcpClient.closeGracefully().block();
        });
    }

    private void callToolAndPrint(McpAsyncClient mcpClient, String toolName, Map<String, Object> arguments) {
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
    public void handleLogging(io.modelcontextprotocol.spec.LoggingMessageNotification notification) {
        System.out.println("  [Logging handler] 收到日志通知:");
        System.out.println("    级别: " + notification.level());
        System.out.println("    数据: " + notification.data());
        System.out.println("    源: " + notification.relatesTo());
    }

    /**
     * @McpProgress handler
     * 接收并打印服务器发来的 ProgressNotification。
     */
    @McpProgress(clients = "timemcp")
    public void handleProgress(io.modelcontextprotocol.spec.ProgressNotification notification) {
        double pct = notification.progress() * 100;
        System.out.println("  [Progress handler] 收到进度通知:");
        System.out.println("    进度: " + String.format("%.0f%%", pct));
        System.out.println("    消息: " + notification.message());
    }

    /**
     * @McpSampling handler
     * 接收服务器的 CreateMessageRequest，通过 DeepSeek LLM 生成回复，返回 CreateMessageResult。
     *
     * 这里使用 Spring AI ChatClient 调用 DeepSeek。
     * 实际应用中可以根据请求的 model 参数选择不同的 LLM。
     */
    @McpSampling(clients = "timemcp")
    public io.modelcontextprotocol.spec.CreateMessageResult handleSampling(
            io.modelcontextprotocol.spec.CreateMessageRequest request) {

        System.out.println("  [Sampling handler] 收到 LLM 采样请求:");
        System.out.println("    模型偏好: " + request.modelPreferences());
        System.out.println    ("    请求内容: " + extractText(request.messages()));

        // 调用 DeepSeek LLM
        String userMessage = extractText(request.messages());
        String llmResponse = chatClient.prompt()
            .user(userMessage)
            .call()
            .content();

        System.out.println("    DeepSeek 回复: " + llmResponse);

        return io.modelcontextprotocol.spec.CreateMessageResult.builder()
            .role(io.modelcontextprotocol.spec.CreateMessageResult.Role.ASSISTANT)
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

    private String extractText(List<io.modelcontextprotocol.spec.PromptMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        var content = messages.get(0).content();
        return content instanceof McpSchema.TextContent tc ? tc.text() : content.toString();
    }

    // ==================== Setters for autowiring ====================

    @org.springframework.beans.factory.annotation.Autowired
    public void setMcpAsyncClients(List<McpAsyncClient> mcpAsyncClients) {
        this.mcpAsyncClients = mcpAsyncClients;
    }

    @org.springframework.beans.factory.annotation.Autowired
    public void setAsyncMcpToolCallbackProvider(AsyncMcpToolCallbackProvider provider) {
        this.asyncMcpToolCallbackProvider = provider;
    }
}
```

---

## Task 4: Verify and run

**Files:**
- None (verification only)

- [ ] **Step 1: Build the project**

Run: `cd /Users/zxh/IdeaProjects/aidemo && mvn clean compile -pl springai-mcp-server,springai-mcp-client -am`

Expected: BUILD SUCCESS

- [ ] **Step 2: Start the server (terminal 1)**

Run: `cd /Users/zxh/IdeaProjects/aidemo/springai-mcp-server && mvn spring-boot:run`

Expected: Server starts on port 8080

- [ ] **Step 3: Start the client (terminal 2)**

Run: `cd /Users/zxh/IdeaProjects/aidemo/springai-mcp-client && mvn spring-boot:run`

Expected: Client runs, console shows handler invocations for each of the 4 demos

---

## Self-Review Checklist

- [x] Spec coverage: All 4 annotations covered (logging, progress, sampling, toolListChanged)
- [x] Spec coverage: `@McpResource` included on server
- [x] Placeholder scan: No TODOs, no TBDs
- [x] Type consistency: `McpSyncRequestContext` methods match (`info()`, `progress(...)`, `sample()`, `notifyToolListChanged(...)`)
- [x] Type consistency: `CreateMessageRequest`/`CreateMessageResult` imports correct packages
- [x] Task independence: Each task is self-contained
