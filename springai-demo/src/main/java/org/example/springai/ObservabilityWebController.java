package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Spring AI 可观测性 Web 演示端点。
 *
 * <p>提供 /ai/chat（对话）、/ai/metrics（指标）、/ai/layers（五层说明）、/ai/demo（HTML 演示页）。
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

    /**
     * 对话端点 — 每次调用产生完整 5 层指标 + Trace + 日志。
     */
    @GetMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestParam("msg") String msg) {
        if (msg == null || msg.isBlank()) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", "msg parameter is required");
            return ResponseEntity.badRequest().body(error);
        }
        try {
            long start = System.currentTimeMillis();
            String reply = chatClient.prompt().user(msg).call().content();
            long cost = System.currentTimeMillis() - start;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("question", msg);
            result.put("answer", reply);
            result.put("cost_ms", cost);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new LinkedHashMap<>();
            error.put("error", e.getMessage());
            error.put("question", msg);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                <h2>5 层指标体系</h2>
                <table>
                    <tr><th>层</th><th>指标名</th><th>来源组件</th><th>说明</th></tr>
                    <tr><td>①</td><td>spring_ai_chat_client_seconds</td><td>ChatClient</td><td>包含 advisor 调用链</td></tr>
                    <tr><td>②</td><td>spring_ai_advisor_seconds</td><td>Advisor</td><td>拦截器层</td></tr>
                    <tr><td>③</td><td>spring_ai_tool_seconds</td><td>Tool Calling</td><td>工具调用层</td></tr>
                    <tr><td>④</td><td>gen_ai_client_operation_seconds</td><td>ChatModel</td><td>DeepSeek API 层</td></tr>
                    <tr><td>⑤</td><td>gen_ai_client_token_usage_total</td><td>Token 用量</td><td>input/output/total</td></tr>
                </table>
            </div>

            <div class="section">
                <h2>对话测试</h2>
                <div class="chat-area">
                    <input type="text" id="msgInput" placeholder="输入问题，如：Spring AI 是什么？" />
                    <button onclick="doChat()">发送</button>
                </div>
                <pre id="chatResult">等待输入...</pre>
            </div>

            <div class="section">
                <h2>实时指标</h2>
                <button onclick="loadMetrics()">刷新指标</button>
                <pre id="metricsResult">点击刷新以加载指标...</pre>
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
                document.getElementById('chatResult').textContent = '调用中...';
                fetch('/ai/chat?msg=' + encodeURIComponent(msg))
                    .then(r => r.json())
                    .then(d => {
                        document.getElementById('chatResult').textContent =
                            'Q: ' + d.question + '\\n\\nA: ' + d.answer + '\\n\\n耗时: ' + d.cost_ms + 'ms';
                        loadMetrics();
                    })
                    .catch(e => document.getElementById('chatResult').textContent = '错误: ' + e);
            }
            function loadMetrics() {
                fetch('/ai/metrics')
                    .then(r => r.json())
                    .then(data => {
                        var lines = data.map(m => m.name + ' | ' + m.type + ' | tags:' + m.tags +
                            (m.count !== undefined ? ' count=' + m.count : '') +
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
