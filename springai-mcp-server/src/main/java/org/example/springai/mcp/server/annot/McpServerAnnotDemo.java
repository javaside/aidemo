package org.example.springai.mcp.server.annot;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springaicommunity.mcp.context.McpAsyncRequestContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Mono;

/**
 * MCP Server 注解演示 - 精简版
 *
 * <p>一个工具演示服务端向客户端发送 5 种通知（全部通过 McpAsyncRequestContext）：</p>
 * <ul>
 *   <li>context.info()      → 客户端 @McpLogging 收到日志</li>
 *   <li>context.progress(spec -> spec.progress(0.3))  → 客户端 @McpProgress 收到进度（需请求带 progressToken）</li>
 *   <li>context.sample()    → 客户端 @McpSampling 收到 LLM 采样请求</li>
 *   <li>context.elicit()    → 客户端 @McpElicitation 收到用户输入请求（结构化）</li>
 *   <li>context.roots()     → 获取客户端注册的根目录列表</li>
 * </ul>
 *
 * <p>注意事项：</p>
 * <ul>
 *   <li>服务端必须配置为 stateful（spring.ai.mcp.server.stateful=true）</li>
 *   <li>context.info/progress 等方法返回 cold {@link Mono}，必须纳入响应式链才会执行</li>
 *   <li>ctx.progress() 需要客户端请求携带 progressToken 才能将通知送达客户端</li>
 * </ul>
 *
 * @see McpAsyncRequestContext
 */
@SpringBootApplication
public class McpServerAnnotDemo {

    public static void main(String[] args) {
        SpringApplication.run(McpServerAnnotDemo.class, args);
    }

    /**
     * 用于 elicitation 演示的结构体。
     * 客户端收到 elicitation 请求后，可将此结构体作为表单让用户填写，
     * 然后通过 StructuredElicitResult 返回给服务端。
     */
    public record Person(String name, int age) {}

    /**
     * 演示全部 5 种服务端→客户端通知的工具。
     *
     * <p>参数：</p>
     * <ul>
     *   <li>input: 任意字符串，会原样显示在日志和返回结果中</li>
     * </ul>
     *
     * <p>执行顺序：logging → progress → sampling → elicitation → roots</p>
     *
     * <p>注意：所有 context.info/progress 等调用都必须用 .then() 串联进响应式链，
     * 否则返回的 Mono 不会被订阅，通知不会真正发送。</p>
     */
    @McpTool(name = "demoAll", description = "演示全部 5 种通知")
    public Mono<String> demoAll(McpAsyncRequestContext ctx,
                               @McpToolParam(description = "用户输入文本") String input) {

        // ===== 1. @McpLogging - 日志通知 =====
        // ctx.info() 返回 Mono<Void>，用 .then() 串联使其被执行
        return ctx.info("日志: 任务开始，input=" + input)
            .then(ctx.info("日志: 任务执行中..."))
            .thenReturn("1. @McpLogging: 已发送 2 条日志，input=\"" + input + "\"\n")

            // ===== 2. @McpProgress - 进度通知 =====
            // ctx.progress(spec -> spec.progress(0.3)) 发送 0.0~1.0 的进度
            // 注意：客户端请求必须携带 progressToken，否则服务端不知道往哪发
            .flatMap(result -> ctx.progress(spec -> spec.progress(0.3))  // 30% 进度
                .thenReturn(result + "2. @McpProgress: 已发送进度 30%\n"))

            // ===== 3. @McpSampling - LLM 采样请求 =====
            // 先检查客户端是否声明了 sampling 能力（@McpSampling handler）
            .flatMap(result -> ctx.sampleEnabled().flatMap(enabled -> {
                if (Boolean.TRUE.equals(enabled)) {
                    // 客户端 @McpSampling handler 会收到此请求，可让 LLM 生成回复
                    return ctx.sample(s -> s.message("请用一句话介绍 MCP"))
                        // 采样完成后记录日志（同样需要 .then() 执行）
                        .flatMap(r -> ctx.info("日志: 采样完成")
                            .thenReturn(result + "3. @McpSampling: OK\n"));
                }
                return Mono.just(result + "3. @McpSampling: 未启用\n");
            }))

            // ===== 4. @McpElicitation - 用户输入请求（结构化） =====
            // 用于服务端需要用户补充结构化数据时向客户端发起请求
            .flatMap(result -> ctx.elicitEnabled().flatMap(enabled -> {
                if (Boolean.TRUE.equals(enabled)) {
                    // 请求客户端返回一个 Person 类型的数据（实际场景可弹表单让用户填写）
                    return ctx.elicit(Person.class)
                        .flatMap(r -> ctx.info("日志: elicitation 完成")
                            .thenReturn(result + "4. @McpElicitation: OK\n"));
                }
                return Mono.just(result + "4. @McpElicitation: 未启用\n");
            }))

            // ===== 5. context.roots() - 获取客户端根目录 =====
            // 客户端通过 McpAsyncClientRootsCustomizer 设置了 roots 列表
            .flatMap(result -> ctx.rootsEnabled().flatMap(enabled -> {
                if (Boolean.TRUE.equals(enabled)) {
                    return ctx.roots().flatMap(r -> {
                        StringBuilder sb = new StringBuilder(result);
                        sb.append("5. context.roots(): 客户端根目录:\n");
                        r.roots().forEach(root ->
                            sb.append("   - ").append(root.name()).append(": ")
                              .append(root.uri()).append("\n"));
                        // roots 完成后记录日志并返回结果
                        return ctx.info("日志: roots 完成")
                            .thenReturn(sb.toString());
                    });
                }
                return Mono.just(result + "5. context.roots(): roots 未启用\n");
            }))

            // ===== 结束：发送最终进度和日志 =====
            .flatMap(result -> ctx.progress(spec -> spec.progress(1.0))  // 100% 完成
                .then(ctx.info("日志: 任务完成"))
                .thenReturn(result));
    }
}
