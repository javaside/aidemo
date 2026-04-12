package org.example.springai.mcp.client.annot;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest;
import io.modelcontextprotocol.spec.McpSchema.CreateMessageResult;
import io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification;
import io.modelcontextprotocol.spec.McpSchema.ProgressNotification;
import io.modelcontextprotocol.spec.McpSchema.Role;
import io.modelcontextprotocol.spec.McpSchema.SamplingMessage;
import org.springaicommunity.mcp.annotation.McpElicitation;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springaicommunity.mcp.annotation.McpToolListChanged;
import org.springaicommunity.mcp.context.StructuredElicitResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * MCP Client 注解 Handler - 接收服务端发来的 5 种通知
 *
 * <p>这 5 个 handler 对应服务端通过 McpAsyncRequestContext 发出的 5 种通知：</p>
 * <ul>
 *   <li>{@code @McpLogging}       ← 服务端 context.info/warn/error/log() 日志</li>
 *   <li>{@code @McpProgress}      ← 服务端 context.progress() 进度</li>
 *   <li>{@code @McpSampling}      ← 服务端 context.sample() LLM 采样请求</li>
 *   <li>{@code @McpElicitation}   ← 服务端 context.elicit() 用户输入请求</li>
 *   <li>{@code @McpToolListChanged} ← 工具列表变更通知</li>
 * </ul>
 *
 * <p>所有 handler 均返回 {@link Mono}，由 Spring AI MCP 框架负责订阅。
 * 注意：handler 中应避免阻塞调用（如 .block()），否则会阻塞响应式线程。</p>
 */
@Service
public class McpClientAnnotProviders {

    private final ChatClient chatClient;

    public McpClientAnnotProviders(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    // ==================== @McpLogging ====================

    /**
     * 接收服务端发来的日志通知。
     *
     * <p>当服务端调用 context.info/warn/error/log() 时，此 handler 会被触发。
     * 注意：Spring AI 1.1.x 中 @McpLogging 仅接收通过 context.log() 发送的日志，
     * context.info/warn/error 这些便捷方法在某些版本中不走此通道。</p>
     *
     * @param notification 包含日志级别 (level) 和数据 (data)
     */
    @McpLogging(clients = "timemcp")
    public Mono<Void> handleLogging(LoggingMessageNotification notification) {
        System.out.println("\n  [@McpLogging] 收到日志通知:");
        System.out.println("    级别: " + notification.level());
        System.out.println("    数据: " + notification.data());
        return Mono.empty();
    }

    // ==================== @McpProgress ====================

    /**
     * 接收服务端发来的进度通知。
     *
     * <p>当服务端调用 context.progress(spec -> spec.progress(0.3)) 时触发，参数为 0.0~1.0 的进度值。
     * 此 handler 之所以能收到通知，是因为客户端请求中携带了 progressToken，
     * 服务端通过该 token 将通知发送回对应的客户端。</p>
     *
     * @param notification 包含 progressToken、进度值 (0.0~1.0)、总进度 (可选) 和消息
     */
    @McpProgress(clients = "timemcp")
    public Mono<Void> handleProgress(ProgressNotification notification) {
        double pct = notification.progress() * 100;
        System.out.println("\n  [@McpProgress] 收到进度通知:");
        System.out.println("    进度: " + String.format("%.0f%%", pct));
        System.out.println("    消息: " + notification.message());
        return Mono.empty();
    }

    // ==================== @McpSampling ====================

    /**
     * 接收服务端发来的 LLM 采样请求。
     *
     * <p>当服务端调用 context.sample() 时触发，请求中包含需要 LLM 处理的消息列表。
     * 此 handler 使用 {@code ChatClient.prompt().stream()} 异步调用大模型，
     * 将流式响应聚合成一个完整的回复后返回 {@link CreateMessageResult}。</p>
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>将 MCP 的 {@link SamplingMessage} 列表转换为 Spring AI 的 {@link Message} 列表</li>
     *   <li>使用 {@code chatClient.prompt().messages(...).stream()} 获取流式响应</li>
     *   <li>将所有文本块聚合成完整回复</li>
     *   <li>封装为 {@link CreateMessageResult} 返回给服务端</li>
     * </ol>
     *
     * @param request 服务端发来的采样请求，包含消息列表、模型偏好等
     * @return LLM 生成的回复
     */
    @McpSampling(clients = "timemcp")
    public Mono<CreateMessageResult> handleSampling(CreateMessageRequest request) {
        System.out.println("\n  [@McpSampling] 收到 LLM 采样请求:");
        System.out.println("    请求内容: " + extractText(request.messages()));

        // 1. 将 MCP SamplingMessage 列表转换为 Spring AI Message 列表
        List<Message> chatMessages = request.messages().stream()
            .map(this::toSpringAiMessage)
            .toList();

        // 2. 使用 stream().content() 获取流式文本响应
        //    chatClient.prompt().messages(...).stream().content() 返回 Flux<String>
        //    每个 String 是一个文本块，直接收集并拼接即可
        return chatClient.prompt()
            .messages(chatMessages)
            .stream()
            .content()
            // 3. 收集所有文本块并拼接成完整回复
            .collectList()
            .map(chunks -> String.join("", chunks))
            // 4. 封装为 CreateMessageResult 返回
            .map(text -> CreateMessageResult.builder()
                .role(Role.ASSISTANT)
                .content(new McpSchema.TextContent(text))
                .model(request.modelPreferences() != null
                    ? request.modelPreferences().toString() : "deepseek")
                .build())
            .doOnNext(result -> System.out.println("    LLM 回复: " + result.content()));
    }

    // ==================== @McpElicitation ====================

    /**
     * 接收服务端发来的用户输入请求（结构化）。
     *
     * <p>当服务端调用 context.elicit(Person.class) 请求用户补充结构化数据时触发。
     * 返回的 StructuredElicitResult 包含：</p>
     * <ul>
     *   <li>{@code action}: ACCEPT（接受）/ REJECT（拒绝）/ CANCEL（取消）</li>
     *   <li>{@code data}: 用户提供的结构化数据（本例为 Person）</li>
     *   <li>{@code partialMessage}: 可选的中间消息（可省略）</li>
     * </ul>
     *
     * <p>实际应用场景：可在此处弹出 UI 表单让用户填写数据，然后将结果封装为
     * StructuredElicitResult 返回给服务端，服务端收到后继续执行。</p>
     *
     * @param request 服务端发来的 elicitation 请求
     * @return 用户输入的结果（含接受/拒绝动作和结构化数据）
     */
    public record Person(String name, int age) {}

    @McpElicitation(clients = "timemcp")
    public Mono<StructuredElicitResult<Person>> handleElicitation(McpSchema.ElicitRequest request) {
        System.out.println("\n  [@McpElicitation] 收到 elicitation 请求:");
        System.out.println("    请求: " + request);
        // 本例直接返回模拟数据。实际场景应弹出 UI 表单让用户填写
        return Mono.just(new StructuredElicitResult<>(
            McpSchema.ElicitResult.Action.ACCEPT,  // 接受此请求
            new Person("张三", 30),                  // 用户输入的结构化数据
            null                                    // 无中间消息
        ));
    }

    // ==================== @McpToolListChanged ====================

    /**
     * 接收工具列表变更通知。
     *
     * <p>当服务端的工具列表发生变化（新增/删除/修改工具）时触发。
     * 可用于动态更新客户端支持的工具缓存。</p>
     *
     * @param tools 当前可用的工具列表
     */
    @McpToolListChanged(clients = "timemcp")
    public Mono<Void> handleToolListChanged(List<McpSchema.Tool> tools) {
        System.out.println("\n  [@McpToolListChanged] 收到工具列表更新:");
        System.out.println("    当前工具数: " + tools.size());
        tools.forEach(t -> System.out.println("    - " + t.name()));
        return Mono.empty();
    }

    /**
     * 将 MCP SamplingMessage 转换为 Spring AI Message。
     *
     * <p>MCP 消息角色：USER / ASSISTANT / SYSTEM<br>
     * Spring AI 消息类型：UserMessage / AssistantMessage / SystemMessage</p>
     */
    private Message toSpringAiMessage(SamplingMessage sm) {
        String text = extractContentText(sm.content());
        Role role = sm.role();
        return switch (role) {
            case USER    -> new UserMessage(text);
            case ASSISTANT -> new AssistantMessage(text);
            // MCP SamplingMessage 的 Role 只有 USER 和 ASSISTANT，
            // SYSTEM 角色在 MCP 协议中不属于 Sampling 范畴，理论上不会走到这里
            default      -> new UserMessage(text); // 兜底当作用户消息
        };
    }

    /**
     * 从 MCP Content 中提取纯文本。
     * 兼容 TextContent、ImageContent 等多种类型。
     */
    private String extractContentText(io.modelcontextprotocol.spec.McpSchema.Content content) {
        if (content instanceof McpSchema.TextContent tc) {
            return tc.text();
        }
        return content.toString();
    }

    /**
     * 从 SamplingMessage 列表中提取文本内容。
     * MCP 协议中消息内容可能是 TextContent 或其他类型，此处做简单兼容。
     */
    private String extractText(List<SamplingMessage> messages) {
        if (messages == null || messages.isEmpty()) return "";
        return extractContentText(messages.get(0).content());
    }
}
