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
            .map(content -> "data: " + content + "\n\n")
            .startWith(Flux.just("data: {\"question\": \"" + msg.replace("\"", "\\\"") + "\"}\n\n"))
            .concatWith(Flux.defer(() -> {
                long cost = System.currentTimeMillis() - start;
                return Flux.just("data: {\"cost_ms\": " + cost + ", \"tool_used\": true}\n\n");
            }))
            .onErrorResume(ex -> Flux.just("data: {\"error\": \"" + ex.getMessage() + "\"}\n\n"));
    }

    /**
     * 非流式对话端点 — 等待完整响应后返回 JSON。
     */
    @GetMapping("/chat")
    public Map<String, Object> chat(@RequestParam("msg") String msg,
                                    @RequestParam(value = "tool", required = false, defaultValue = "false") String withTool) {
        if (msg == null || msg.isBlank()) {
            return Map.of("error", "msg parameter is required");
        }
        try {
            long start = System.currentTimeMillis();
            String reply;
            if ("true".equalsIgnoreCase(withTool)) {
                reply = chatClient.prompt().user(msg).toolCallbacks(weatherTool).call().content();
            } else {
                reply = chatClient.prompt().user(msg).call().content();
            }
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", msg);
            result.put("answer", reply);
            result.put("cost_ms", cost);
            result.put("tool_used", "true".equalsIgnoreCase(withTool));
            return result;
        } catch (Exception e) {
            return Map.of("error", e.getMessage(), "question", msg);
        }
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
     * HTML 演示页面 — 展示 5 层说明 + 实时指标 + 调用入口。
     */
    @GetMapping("/demo")
    public String demo() {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Spring AI 可观测性演示</title>
            <style>
                body { font-family: sans-serif; max-width: 900px; margin: 2em auto; padding: 0 1em; }
                h1 { color: #2563eb; }
                h2 { color: #1e40af; margin-top: 1.5em; }
                table { border-collapse: collapse; width: 100%; margin-top: 0.5em; }
                th, td { border: 1px solid #e5e7eb; padding: 8px 12px; text-align: left; }
                th { background: #f3f4f6; }
                tr:nth-child(even) { background: #f9fafb; }
                .badge { display: inline-block; background: #2563eb; color: white; border-radius: 4px; padding: 2px 8px; font-size: 0.85em; }
                .section { margin: 1.5em 0; padding: 1em; border: 1px solid #e5e7eb; border-radius: 8px; }
                .chat-area { margin-top: 1em; }
                input[type=text] { width: 70%%; padding: 8px; font-size: 1em; }
                button { padding: 8px 16px; font-size: 1em; background: #2563eb; color: white; border: none; border-radius: 4px; cursor: pointer; }
                button:hover { background: #1e40af; }
                pre { background: #f3f4f6; padding: 1em; overflow-x: auto; border-radius: 4px; }
            </style>
        </head>
        <body>
            <h1>Spring AI 可观测性演示 <span class="badge">5 层指标</span></h1>

            <div class="section">
                <h2>对话测试</h2>
                <div class="chat-area">
                    <input type="text" id="msgInput" placeholder="输入问题，如：北京今天天气怎么样？" />
                    <label style="margin-left: 1em;">
                        <input type="checkbox" id="toolCheck" /> 使用工具（天气查询）
                    </label>
                    <button onclick="doChat()">发送</button>
                </div>
                <pre id="chatResult">等待输入...</pre>
            </div>

            <div class="section">
                <h2>实时指标 <button onclick="loadMetrics()">刷新</button></h2>
                <pre id="metricsResult">点击刷新以加载指标...</pre>
            </div>

            <div class="section">
                <h2>5 层指标说明</h2>
                <table>
                    <tr><th>层</th><th>指标名</th><th>来源</th><th>说明</th></tr>
                    <tr><td>①</td><td>spring.ai.chat.client</td><td>ChatClient</td><td>包含 advisor 调用链</td></tr>
                    <tr><td>②</td><td>spring.ai.advisor</td><td>Advisor</td><td>拦截器层</td></tr>
                    <tr><td>③</td><td>spring.ai.tool</td><td>Tool Calling</td><td>工具调用层（需开启工具）</td></tr>
                    <tr><td>④</td><td>gen_ai.client.operation</td><td>ChatModel</td><td>DeepSeek API 层</td></tr>
                    <tr><td>⑤</td><td>gen_ai.client.token.usage</td><td>Token 用量</td><td>input/output/total</td></tr>
                </table>
            </div>

            <div class="section">
                <h2>Actuator 端点</h2>
                <ul>
                    <li><a href="/actuator/prometheus">Prometheus 指标</a></li>
                    <li><a href="/actuator/metrics">Micrometer 指标列表</a></li>
                    <li><a href="/actuator/health">健康检查</a></li>
                </ul>
            </div>

            <script>
            function doChat() {
                var msg = document.getElementById('msgInput').value;
                if (!msg) return;
                var withTool = document.getElementById('toolCheck').checked;
                var url = '/ai/chat?msg=' + encodeURIComponent(msg);
                if (withTool) url += '&tool=true';
                document.getElementById('chatResult').textContent = '调用中...';
                fetch(url)
                    .then(r => r.json())
                    .then(d => {
                        var info = 'Q: ' + d.question + '\\n\\nA: ' + d.answer + '\\n\\n耗时: ' + d.cost_ms + 'ms';
                        if (d.tool_used) info += '\\n[使用工具: getWeather]';
                        document.getElementById('chatResult').textContent = info;
                        loadMetrics();
                    })
                    .catch(e => document.getElementById('chatResult').textContent = '错误: ' + e);
            }
            function loadMetrics() {
                fetch('/ai/metrics')
                    .then(r => r.json())
                    .then(data => {
                        var lines = data.map(m => m.name + ' | ' + m.type + ' | ' +
                            (m.count !== undefined ? 'count=' + m.count : '') +
                            (m.mean_ms !== undefined ? ' mean=' + m.mean_ms.toFixed(1) + 'ms' : ''));
                        document.getElementById('metricsResult').textContent = lines.join('\\n') || '暂无指标';
                    });
            }
            </script>
        </body>
        </html>
        """;
    }
}
