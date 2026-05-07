package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Spring AI 可观测性 Web 演示端点。
 */
@RestController
@RequestMapping("/ai")
public class ObservabilityWebController {

    private final ChatClient chatClient;
    private final ObservationTraceService traceService;

    public ObservabilityWebController(ChatClient.Builder chatClientBuilder,
                                       ObservationTraceService traceService) {
        this.chatClient = chatClientBuilder.build();
        this.traceService = traceService;
    }

    record WeatherReq(String city) {}

    private final FunctionToolCallback<WeatherReq, String> weatherTool =
        FunctionToolCallback.<WeatherReq, String>builder(
                "getWeather", req -> req.city + "天气: 晴, 25°C, 湿度45%")
            .description("获取指定城市的天气信息")
            .inputType(WeatherReq.class)
            .build();

    /**
     * 流式对话端点 — 返回 SSE 格式的流式响应。
     * 浏览器端可通过 EventSource 接收。
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestParam("msg") String msg) {
        if (msg == null || msg.isBlank()) {
            return Flux.just("data: {\"error\": \"msg parameter is required\"}\n\n");
        }
        long start = System.currentTimeMillis();
        return chatClient.prompt().user(msg)
            .toolCallbacks(weatherTool)
            .stream()
            .content()
            .map(content -> "data: {\"answer\": \"" + content.replace("\"", "\\\"") + "\"}\n\n")
            .startWith(Flux.just("data: {\"question\": \"" + msg.replace("\"", "\\\"") + "\"}\n\n"))
            .concatWith(Flux.defer(() -> {
                long cost = System.currentTimeMillis() - start;
                return Flux.just("data: {\"cost_ms\": " + cost + ", \"tool_used\": true}\n\n");
            }))
            .onErrorResume(ex -> Flux.just("data: {\"error\": \"" + ex.getMessage() + "\"}\n\n"));
    }

    /**
     * 当前收集到的所有 Spring AI 指标（从 Micrometer MeterRegistry）。
     */
    @GetMapping("/metrics")
    public List<Map<String, Object>> metrics() {
        try {
            return traceService.getMetricsSummary();
        } catch (Exception e) {
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 5 层指标对照说明。
     */
    @GetMapping("/layers")
    public List<Map<String, String>> layers() {
        return traceService.getLayersDescription();
    }

    /**
     * HTML 演示页面 — 从文件读取。
     */
    @GetMapping("/demo")
    public String demo() {
        try (var is = getClass().getResourceAsStream("/demo.html")) {
            return new String(is.readAllBytes());
        } catch (Exception e) {
            return "Error loading demo page: " + e.getMessage();
        }
    }
}