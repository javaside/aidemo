# Spring AI 可观测性 Web 演示实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 `springai-demo` 模块新增基于 Spring Boot 自动配置的 Web 演示页面，展示 Spring AI 的 5 层指标、OTLP Tracing 和内容日志。

**Architecture:** 通过 Spring Boot Actuator + Micrometer 暴露 Prometheus 指标；通过 OpenTelemetry OTLP 导出 Trace；通过 Logback 将 Prompt/Completion 内容写入独立日志文件；新增 `/ai/*` 系列端点统一展示所有可观测性数据。

**Tech Stack:** Spring Boot 3.x, spring-boot-starter-actuator, micrometer-registry-prometheus, opentelemetry-exporter-otlp, Spring AI ChatClient Auto-Configuration

---

## 文件结构

```
springai-demo/
├── pom.xml                                          [修改] 新增 4 个依赖
├── src/main/resources/
│   └── application.properties                      [修改] 追加 actuator/otel/ai.observability 配置
└── src/main/java/org/example/springai/
    ├── ObservabilityWebController.java              [新建] /ai/* 端点 + HTML 演示页
    ├── ObservationTraceService.java                 [新建] 封装 ObservationRegistry 查询
    └── SpringAiDemoApplication.java                 [保持不变]
```

---

## 任务清单

### Task 1: 更新 pom.xml — 新增 4 个依赖

**Files:**
- Modify: `springai-demo/pom.xml:20-73`

- [ ] **Step 1: 在 `</dependencies>` 前插入 4 个新依赖**

在 `</dependencies>` 前添加：

```xml
        <!-- Observability -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-exporter-otlp</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentelemetry</groupId>
            <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
        </dependency>
```

- [ ] **Step 2: Commit**

```bash
git add springai-demo/pom.xml
git commit -m "feat(observability): 新增 actuator/prometheus/otel 依赖"
```

---

### Task 2: 更新 application.properties — 追加可观测性配置

**Files:**
- Modify: `springai-demo/src/main/resources/application.properties`

- [ ] **Step 1: 追加以下配置到文件末尾**

```properties
# ===== Actuator 端点 =====
management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.prometheus.metrics.export.enabled=true

# ===== OpenTelemetry Trace（OTLP 导出到 localhost:4317）=====
management.otel.traces.export.enabled=true
management.otel.exporter.otlp.endpoint=http://localhost:4317

# ===== Prompt/Completion 内容日志 =====
spring.ai.chat.observations.log-prompt=true
spring.ai.chat.observations.log-completion=true
spring.ai.chat.client.observations.log-prompt=true
spring.ai.chat.client.observations.log-completion=true

# ===== Token 用量指标 =====
spring.ai.chat.observations.emit-tokens=true
```

- [ ] **Step 2: Commit**

```bash
git add springai-demo/src/main/resources/application.properties
git commit -m "feat(observability): 配置 actuator 端点和 ai 可观测性属性"
```

---

### Task 3: 创建 ObservationTraceService.java

**Files:**
- Create: `springai-demo/src/main/java/org/example/springai/ObservationTraceService.java`

- [ ] **Step 1: 创建文件**

```java
package org.example.springai;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 从 Micrometer MeterRegistry 收集并返回 Spring AI 5 层指标摘要。
 */
@Service
public class ObservationTraceService {

    private final MeterRegistry meterRegistry;

    public ObservationTraceService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * 返回所有 spring_ai 和 gen_ai 相关指标，供 /ai/metrics 端点展示。
     */
    public List<Map<String, Object>> getMetricsSummary() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (var meter : meterRegistry.getMeters()) {
            String name = meter.getId().getName();
            if (!name.startsWith("spring_ai") && !name.startsWith("gen_ai")) continue;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", name);
            entry.put("type", meter.getType().name());
            entry.put("tags", meter.getId().getTags().toString());

            if (meter instanceof Timer t) {
                entry.put("count", t.count());
                entry.put("mean_ms", t.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
                entry.put("max_ms", t.max(java.util.concurrent.TimeUnit.MILLISECONDS));
            } else if (meter instanceof Counter c) {
                entry.put("count", c.count());
            }
            result.add(entry);
        }
        return result;
    }

    /**
     * 返回 5 层指标名称列表，对应关系供演示页面使用。
     */
    public List<Map<String, String>> getLayersDescription() {
        return List.of(
            Map.of("layer", "① spring.ai.chat.client", "metric", "spring_ai_chat_client_operation_seconds", "description", "ChatClient 层，包含 advisor 调用"),
            Map.of("layer", "② spring.ai.advisor", "metric", "spring_ai_advisor_operation_seconds", "description", "Advisor 拦截层"),
            Map.of("layer", "③ spring.ai.tool", "metric", "spring_ai_tool_operation_seconds", "description", "Tool calling 层"),
            Map.of("layer", "④ gen_ai.client.operation", "metric", "gen_ai_client_operation_seconds", "description", "ChatModel 层（DeepSeek API）"),
            Map.of("layer", "⑤ gen_ai.client.token.usage", "metric", "gen_ai_client_token_usage_total", "description", "Token 用量统计")
        );
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add springai-demo/src/main/java/org/example/springai/ObservationTraceService.java
git commit -m "feat(observability): 新增 ObservationTraceService 指标收集服务"
```

---

### Task 4: 创建 ObservabilityWebController.java

**Files:**
- Create: `springai-demo/src/main/java/org/example/springai/ObservabilityWebController.java`

- [ ] **Step 1: 创建文件**

```java
package org.example.springai;

import org.springframework.ai.chat.client.ChatClient;
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
    public Map<String, Object> chat(@RequestParam("msg") String msg) {
        long start = System.currentTimeMillis();
        String reply = chatClient.prompt().user(msg).call().content();
        long cost = System.currentTimeMillis() - start;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("question", msg);
        result.put("answer", reply);
        result.put("cost_ms", cost);
        return result;
    }

    /**
     * 当前收集到的所有 Spring AI 指标（从 Micrometer MeterRegistry）。
     */
    @GetMapping("/metrics")
    public List<Map<String, Object>> metrics() {
        return traceService.getMetricsSummary();
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
                    <tr><td>①</td><td>spring_ai_chat_client_operation_seconds</td><td>ChatClient</td><td>包含 advisor 调用链</td></tr>
                    <tr><td>②</td><td>spring_ai_advisor_operation_seconds</td><td>Advisor</td><td>拦截器层</td></tr>
                    <tr><td>③</td><td>spring_ai_tool_operation_seconds</td><td>Tool Calling</td><td>工具调用层</td></tr>
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
```

- [ ] **Step 2: Commit**

```bash
git add springai-demo/src/main/java/org/example/springai/ObservabilityWebController.java
git commit -m "feat(observability): 新增 ObservabilityWebController 及 /ai/* 端点"
```

---

### Task 5: 验证构建

**Files:**
- (no changes)

- [ ] **Step 1: 运行 Maven 构建**

```bash
cd springai-demo && mvn clean compile -q
```

预期：无编译错误

- [ ] **Step 2: 提交验证 commit**

```bash
git add -A && git commit -m "chore: 验证 springai-demo 构建通过"
```

---

## 验收标准

- [ ] `pom.xml` 包含 actuator、prometheus、otel 共 4 个新依赖
- [ ] `application.properties` 包含 `management.endpoints.web.exposure.include` 和 `spring.ai.chat.observations.*` 配置
- [ ] `ObservationTraceService` 实现 `getMetricsSummary()` 和 `getLayersDescription()`
- [ ] `ObservabilityWebController` 提供 `/ai/chat`、`/ai/metrics`、`/ai/layers`、`/ai/demo` 四个端点
- [ ] `/ai/demo` 返回完整 HTML 页面，包含 5 层说明表格、对话输入框、实时指标刷新
- [ ] `mvn clean compile` 通过无错误