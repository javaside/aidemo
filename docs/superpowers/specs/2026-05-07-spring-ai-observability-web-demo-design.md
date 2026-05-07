# Spring AI 可观测性 Web 演示设计

## 背景

在 `springai-demo` 模块已有 `ChatClientObservabilityDemo.java`（手动配置独立运行示例），
本次新增基于 Spring Boot 自动配置的 Web 服务演示，展示指标 + Tracing + 日志 3 种可观测性能力。

## 目标

在现有 `SpringAiDemoApplication` Web 服务基础上，增加可观测性端点和演示页面，
让用户通过浏览器即可查看 Spring AI 的 5 层指标、分布式 Trace 和内容日志。

## 架构

```
┌─────────────────────────────────────────────────────────┐
│  Spring Boot Web 服务（SpringAiDemoApplication）         │
│                                                          │
│  /ai/* 端点  ──→  ChatClient（自动 Instrumentation）       │
│                           ↓                              │
│              产生 5 层 Observation：                      │
│              ① spring.ai.chat.client                     │
│              ② spring.ai.advisor                         │
│              ③ spring.ai.tool                           │
│              ④ gen_ai.client.operation                   │
│              ⑤ gen_ai.client.token.usage                 │
└──────────┬──────────┬──────────┬──────────┬───────────┘
           ↓          ↓          ↓          ↓
    ┌──────────┐ ┌──────────┐ ┌──────────┐
    │ Micrometer│ │   OTLP   │ │  Logback │
    │ Prometheus│ │ Traces   │ │  Content │
    └──────────┘ └──────────┘ └──────────┘
           ↓          ↓          ↓
    /actuator/  /actuator/  logs/spring-ai-*.log
    prometheus  otlp
```

## 依赖变更

pom.xml 新增：

```xml
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

## 配置变更

application.properties 新增：

```properties
# Actuator 端点暴露
management.endpoints.web.exposure.include=health,prometheus,metrics,info
management.prometheus.metrics.export.enabled=true

# OpenTelemetry Trace（输出到 OTLP 兼容端点，如 Zipkin/Jaeger）
management.otel.traces.export.enabled=true
management.otel.exporter.otlp.endpoint=http://localhost:4317

# Prompt/Completion 内容日志
spring.ai.chat.observations.log-prompt=true
spring.ai.chat.observations.log-completion=true
spring.ai.chat.client.observations.log-prompt=true
spring.ai.chat.client.observations.log-completion=true

# Token 用量指标
spring.ai.chat.observations.emit-tokens=true
```

## 新增文件

### 1. ObservabilityWebController.java

新增 `/ai/*` 系列端点：

| 端点 | 功能 |
|------|------|
| `GET /ai/chat?msg=xxx` | 调用 ChatClient，产生完整 5 层指标 + Trace |
| `GET /ai/metrics` | 展示所有 Spring AI 层指标（JSON 格式） |
| `GET /ai/trace` | 展示最近 Trace 摘要（从 ObservationRegistry 收集） |
| `GET /ai/demo` | HTML 页面，同时触发 chat 并展示所有可观测性数据 |

### 2. ObservationTraceService.java

封装 ObservationRegistry 查询，将高基数 Trace 数据暴露为 REST 端点。

## 5 层指标对照表

| 层次 | 指标名 | 来源组件 | 关键标签 |
|------|--------|----------|----------|
| ① | `spring_ai_chat_client_operation_seconds` | ChatClient | `gen_ai.system=spring_ai`, `spring.ai.kind=chat_client` |
| ② | `spring_ai_advisor_operation_seconds` | Advisor | `advisor.name=xxx` |
| ③ | `spring_ai_tool_operation_seconds` | Tool Calling | `spring.ai.tool.definition.name=xxx` |
| ④ | `gen_ai_client_operation_seconds` | ChatModel | `gen_ai.system=deepseek`, `gen_ai.request.model=xxx` |
| ⑤ | `gen_ai_client_token_usage_total` | Token 用量 | `gen_ai_token_type=input/output/total` |

## 现有文件修改

- `SpringAiDemoApplication.java` — 保持不变，自动装配已处理一切
- `MyController.java` — 保持不变，已有 `/ai` 端点
- `application.properties` — 追加上述配置

## 验收标准

1. 启动应用后访问 `/ai/demo` 可看到 HTML 演示页面
2. 页面触发 chat 后，表格中展示 5 层指标
3. `/actuator/prometheus` 包含 `spring_ai_*` 和 `gen_ai_*` 指标
4. `/ai/trace` 端点返回高基数 Trace 数据（response.id、token 用量等）
5. 日志文件中包含 Prompt/Completion 内容