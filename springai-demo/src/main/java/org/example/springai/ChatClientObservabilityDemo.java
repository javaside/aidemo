package org.example.springai;

import org.example.springai.config.ApiKeyConfig;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.observation.DefaultMeterObservationHandler;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.observation.ChatClientCompletionObservationHandler;
import org.springframework.ai.chat.client.observation.ChatClientPromptContentObservationHandler;
import org.springframework.ai.chat.observation.ChatModelCompletionObservationHandler;
import org.springframework.ai.chat.observation.ChatModelMeterObservationHandler;
import org.springframework.ai.chat.observation.ChatModelPromptContentObservationHandler;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.tool.function.FunctionToolCallback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.FileAppender;

/**
 * ChatClient 可观测性入门演示 — 手动配置方式。
 *
 * <h3>产生的 5 层指标</h3>
 * <pre>
 *  ① spring.ai.chat.client      — ChatClient 层
 *  ② spring.ai.advisor          — Advisor 层
 *  ③ spring.ai.tool             — 工具调用层
 *  ④ gen_ai.client.operation    — ChatModel 层（模型厂商）
 *  ⑤ gen_ai.client.token.usage  — Token 用量
 * </pre>
 *
 * <h3>手动配置 vs 自动配置</h3>
 * Spring Boot 下只需引入 actuator 即可自动装配。本 demo 演示非 Boot 环境下的手动配置方式：
 *
 * <pre>
 *  ChatClient  → ChatClient.create(model, obsRegistry)
 *  Advisor    → （ChatClient 内部自动处理）
 *  Tool       → DefaultToolCallingManager.builder().observationRegistry(obsRegistry)
 *  ChatModel  → DeepSeekChatModel.builder().observationRegistry(obsRegistry)
 *  Token 用量  → new ChatModelMeterObservationHandler(meterRegistry)
 * </pre>
 *
 * <h3>Prompt / Completion 内容日志</h3>
 * 对应 Boot 下的以下属性（自动注册标准 Content Handler 将内容写入 Trace）：
 * <pre>
 *  spring.ai.chat.observations.log-prompt / log-completion         → ChatModel 层
 *  spring.ai.chat.client.observations.log-prompt / log-completion  → ChatClient 层
 * </pre>
 * 本 demo 注册 4 个标准 Content Handler，通过 logback 编程配置将其 SLF4J
 * 日志输出重定向到临时文件，避免控制台凌乱。
 *
 * @see <a href="https://docs.spring.io/spring-ai/reference/observability/index.html">官方文档</a>
 */
public class ChatClientObservabilityDemo {

    public static void main(String[] args) {

        // =====================================================================
        // 第 1 步：创建 MeterRegistry + ObservationRegistry + 注册 Handler
        // =====================================================================
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        ObservationRegistry obsRegistry = ObservationRegistry.create();

        // 自定义 Handler：收集高基数键值（Trace 数据），最后统一展示
        TraceCollector traceCollector = new TraceCollector();

        // 将 4 个标准 Content Handler 的日志输出重定向到临时文件
        String contentLogPath = redirectContentLoggers();

        obsRegistry.observationConfig()
            // ① 指标 Handler：将所有 Observation 转为 Meter（Timer / Counter）
            .observationHandler(new DefaultMeterObservationHandler(meterRegistry))
            // ② Token 用量 Handler：生成 gen_ai.client.token.usage Counter
            .observationHandler(new ChatModelMeterObservationHandler(meterRegistry))
            // ③ ChatClient 层 Prompt/Completion 内容 Handler（日志→临时文件）
            .observationHandler(new ChatClientPromptContentObservationHandler())
            .observationHandler(new ChatClientCompletionObservationHandler())
            // ④ ChatModel 层 Prompt/Completion 内容 Handler（日志→临时文件）
            .observationHandler(new ChatModelPromptContentObservationHandler())
            .observationHandler(new ChatModelCompletionObservationHandler())
            // ⑤ 自定义 Handler：收集高基数键值
            .observationHandler(traceCollector);

        // =====================================================================
        // 第 2 步：手动创建各组件，传入 ObservationRegistry
        // =====================================================================

        DeepSeekApi deepSeekApi = DeepSeekApi.builder()
            .apiKey(ApiKeyConfig.getDeepSeekApiKey())
            .baseUrl("https://api.deepseek.com")
            .build();

        // Tool 层：必须传入 observationRegistry 才会产生 spring.ai.tool 指标
        DefaultToolCallingManager toolCallingManager = DefaultToolCallingManager.builder()
            .observationRegistry(obsRegistry)
            .build();

        // ChatModel 层：必须传入 observationRegistry 才会产生 gen_ai.client.operation 指标
        DeepSeekChatModel model = DeepSeekChatModel.builder()
            .deepSeekApi(deepSeekApi)
            .toolCallingManager(toolCallingManager)
            .observationRegistry(obsRegistry)
            .build();

        // ChatClient 层：传入 observationRegistry，内部自动传递给 Advisor
        ChatClient chatClient = ChatClient.create(model, obsRegistry);

        // =====================================================================
        // 第 3 步：基本对话 — 产生 ①②④⑤ 层指标
        // =====================================================================
        separator("基本对话", "ChatClient / Advisor / ChatModel / Token");
        call(chatClient, 1, "请用一句话介绍 Spring AI");
        call(chatClient, 2, "用中文列出 Java 的三个优点");

        // =====================================================================
        // 第 4 步：工具调用 — 额外产生 ③ Tool 层指标
        // =====================================================================
        separator("工具调用", "额外产生 spring.ai.tool 指标");

        var weatherTool = FunctionToolCallback.<WeatherReq, String>builder(
                "getWeather", req -> req.city + "天气: 晴, 25°C, 湿度45%")
            .description("获取指定城市的天气信息")
            .inputType(WeatherReq.class)
            .build();

        callWithTool(chatClient, 3, "北京今天天气怎么样？", weatherTool);
        callWithTool(chatClient, 4, "上海今天天气怎么样？", weatherTool);

        // =====================================================================
        // 第 5 步：查看所有指标 + 高基数数据
        // =====================================================================
        separator("指标汇总", "各层 Observation 自动生成的 Meter");
        printMetrics(meterRegistry);

        traceCollector.printAll();

        System.out.println();
        System.out.println("  Prompt / Completion 内容日志: " + contentLogPath);
    }

    // ========================================================================
    // 将 4 个标准 Content Handler 的 SLF4J 日志重定向到临时文件
    // ========================================================================

    static String redirectContentLoggers() {
        try {
            LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(lc);
            encoder.setPattern("%msg%n");
            encoder.start();

            FileAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new FileAppender<>();
            appender.setContext(lc);
            appender.setEncoder(encoder);
            appender.setFile(java.nio.file.Files.createTempFile("spring-ai-content-", ".log").toString());
            appender.start();

            for (Class<?> clazz : new Class<?>[]{
                ChatClientPromptContentObservationHandler.class,
                ChatClientCompletionObservationHandler.class,
                ChatModelPromptContentObservationHandler.class,
                ChatModelCompletionObservationHandler.class
            }) {
                Logger logger = lc.getLogger(clazz);
                logger.setLevel(Level.INFO);
                logger.setAdditive(false);
                logger.addAppender(appender);
            }

            return appender.getFile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ========================================================================
    // 对话辅助方法
    // ========================================================================

    record WeatherReq(String city) {}

    static void call(ChatClient client, int n, String question) {
        try {
            String reply = client.prompt().user(question).call().content();
            System.out.printf("  [%d] Q: %s%n", n, question);
            System.out.printf("  [%d] A: %s%n%n", n, truncate(reply, 80));
        } catch (Exception e) {
            System.out.printf("  [%d] Q: %s%n", n, question);
            System.out.printf("  [%d] A: [%s]%n%n", n, e.getClass().getSimpleName());
        }
    }

    static void callWithTool(ChatClient client, int n, String question,
                             org.springframework.ai.tool.ToolCallback tool) {
        try {
            String reply = client.prompt().user(question)
                .toolCallbacks(tool).call().content();
            System.out.printf("  [%d] Q: %s%n", n, question);
            System.out.printf("  [%d] A: %s%n%n", n, truncate(reply, 80));
        } catch (Exception e) {
            System.out.printf("  [%d] Q: %s%n", n, question);
            System.out.printf("  [%d] A: [%s]%n%n", n, e.getClass().getSimpleName());
        }
    }

    // ========================================================================
    // 指标输出：直接遍历 MeterRegistry 中的所有 Meter
    // ========================================================================

    static void printMetrics(MeterRegistry registry) {
        registry.getMeters().stream()
            .sorted(java.util.Comparator.comparing(m -> m.getId().getName()))
            .forEach(m -> {
                if (m instanceof Timer t)
                    System.out.printf("  %-30s count=%-2d  mean=%-6.0fms  %s%n",
                        m.getId().getName(), t.count(),
                        t.mean(java.util.concurrent.TimeUnit.MILLISECONDS),
                        m.getId().getTags());
                else if (m instanceof Counter c)
                    System.out.printf("  %-30s count=%-6.0f  %s%n",
                        m.getId().getName(), c.count(), m.getId().getTags());
            });
    }

    // ========================================================================
    // TraceCollector：收集高基数键值（不进入指标标签的数据），最后统一展示
    // ========================================================================

    static class TraceCollector implements ObservationHandler<Observation.Context> {

        private final java.util.List<String> lines = new java.util.ArrayList<>();

        @Override
        public boolean supportsContext(Observation.Context ctx) {
            return true;
        }

        @Override
        public void onStop(Observation.Context ctx) {
            String obsName = ctx.getName();

            // 高基数键值：值无界（如 response.id 每次不同），只存在于 Trace，不进入指标标签
            var kvs = ctx.getHighCardinalityKeyValues();
            if (kvs != null) {
                for (var kv : kvs) {
                    String value = kv.getValue();
                    if (value != null && value.length() > 60)
                        value = value.substring(0, 60) + "...";
                    lines.add(String.format("  %-30s | %s = %s", obsName, kv.getKey(), value));
                }
            }
        }

        void printAll() {
            if (lines.isEmpty()) return;
            System.out.println();
            separator("高基数 Trace 数据", "不进入指标标签，防止 Prometheus 时序爆炸");
            for (var line : lines)
                System.out.println(line);
        }
    }

    // ========================================================================
    // 辅助
    // ========================================================================

    static String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "..." : s;
    }

    static void separator(String title, String subtitle) {
        String line = "=".repeat(60);
        System.out.println();
        System.out.println(line);
        System.out.println("  " + title +
            (subtitle.isEmpty() ? "" : "  (" + subtitle + ")"));
        System.out.println(line);
        System.out.println();
    }
}
