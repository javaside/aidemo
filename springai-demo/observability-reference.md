# Spring AI 可观测性完整参考

## 架构总览

Spring AI 的可观测性体系分为 **两层**：

```
┌─────────────────────────────────────────────────────────────┐
│  第一层：命名约定 (spring-ai-commons)                          │
│  org.springframework.ai.observation.conventions               │
│  定义 OpenTelemetry 标准标签 + Spring AI 自定义扩展             │
└──────────────────────────┬──────────────────────────────────┘
                           │ 引用
┌──────────────────────────▼──────────────────────────────────┐
│  第二层：各组件实现                                            │
│  每个组件 = Convention + Context + Documentation + Handler    │
└─────────────────────────────────────────────────────────────┘
```

---

## 第一层：命名约定类 (spring-ai-commons)

所有类在 `org.springframework.ai.observation.conventions` 包下。

### AiObservationMetricNames — 指标名称

| 枚举常量 | 值 | 说明 |
|----------|---|------|
| `OPERATION_DURATION` | `gen_ai.client.operation.duration` | 操作耗时 Timer |
| `TOKEN_USAGE` | `gen_ai.client.token.usage` | Token 用量 Counter |

### AiObservationMetricAttributes — 指标属性

| 枚举常量 | 值 | 说明 |
|----------|---|------|
| `TOKEN_TYPE` | `gen_ai.token.type` | Token 类型标签 |

### AiObservationAttributes — 标签 key（低基数，会出现在指标维度中）

| 枚举常量 | 值 | 说明 |
|----------|---|------|
| `AI_OPERATION_TYPE` | `gen_ai.operation.name` | 操作类型 (framework/embeddings 等) |
| `AI_PROVIDER` | `gen_ai.system` | 提供商 (spring_ai) |
| `REQUEST_MODEL` | `gen_ai.request.model` | 请求模型名 |
| `REQUEST_FREQUENCY_PENALTY` | `gen_ai.request.frequency_penalty` | 频率惩罚 |
| `REQUEST_MAX_TOKENS` | `gen_ai.request.max_tokens` | 最大 Token |
| `REQUEST_PRESENCE_PENALTY` | `gen_ai.request.presence_penalty` | 存在惩罚 |
| `REQUEST_STOP_SEQUENCES` | `gen_ai.request.stop_sequences` | 停止序列 |
| `REQUEST_TEMPERATURE` | `gen_ai.request.temperature` | 温度参数 |
| `REQUEST_TOP_K` | `gen_ai.request.top_k` | Top K 参数 |
| `REQUEST_TOP_P` | `gen_ai.request.top_p` | Top P 参数 |
| `REQUEST_TOOL_NAMES` | `spring.ai.model.request.tool.names` | 工具名列表 |
| `REQUEST_EMBEDDING_DIMENSIONS` | `gen_ai.request.embedding.dimensions` | 嵌入维度 |
| `REQUEST_IMAGE_RESPONSE_FORMAT` | `gen_ai.request.image.response_format` | 图像响应格式 |
| `REQUEST_IMAGE_SIZE` | `gen_ai.request.image.size` | 图像尺寸 |
| `REQUEST_IMAGE_STYLE` | `gen_ai.request.image.style` | 图像风格 |
| `RESPONSE_FINISH_REASONS` | `gen_ai.response.finish_reasons` | 完成原因 |
| `RESPONSE_ID` | `gen_ai.response.id` | 响应 ID |
| `RESPONSE_MODEL` | `gen_ai.response.model` | 响应模型名 |
| `USAGE_INPUT_TOKENS` | `gen_ai.usage.input_tokens` | 输入 Token 数 |
| `USAGE_OUTPUT_TOKENS` | `gen_ai.usage.output_tokens` | 输出 Token 数 |
| `USAGE_TOTAL_TOKENS` | `gen_ai.usage.total_tokens` | 总 Token 数 |

### AiOperationType — 操作类型

| 枚举常量 | 值 |
|----------|---|
| `FRAMEWORK` | `framework` |
| `EMBEDDING` | `embeddings` |

### AiProvider — 提供商

| 枚举常量 | 值 |
|----------|---|
| `SPRING_AI` | `spring_ai` |
| 各模型厂商枚举 | `anthropic`、`openai`、`ollama` 等 |

### SpringAiKind — Spring AI 组件类型

| 枚举常量 | 值 | 对应组件 |
|----------|---|---------|
| `CHAT_CLIENT` | `chat_client` | ChatClient |
| `ADVISOR` | `adviser` | Advisor |
| `TOOL_CALL` | `tool_call` | Tool Calling |

---

## 第二层：各组件实现

每个组件遵循统一模式，由 4 个核心类组成：

```
┌─────────────────────────────────────┐
│  Default*ObservationConvention      │  → getName() 决定指标名称
│  *ObservationContext                │  → 携带组件特有的业务数据
│  *ObservationDocumentation          │  → 定义该组件的 KeyName 常量
│  *ObservationHandler (可选)         │  → 生命周期回调，生成额外指标
└─────────────────────────────────────┘
```

### 1. ChatModel — 对话模型

**Observation 名称**: `gen_ai.client.operation`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.chat.observation.DefaultChatModelObservationConvention` |
| Context | `o.s.a.chat.observation.ChatModelObservationContext` |
| Documentation | `o.s.a.chat.observation.ChatModelObservationDocumentation` |
| Handler | `o.s.a.chat.observation.ChatModelMeterObservationHandler`（生成 token 用量指标） |
| Handler | `o.s.a.chat.observation.ChatModelCompletionObservationHandler`（生成 completion 内容） |

**标签（低基数）**:
- `gen_ai.operation.name` = `framework`
- `gen_ai.system` = 模型提供商
- `gen_ai.request.model` = 模型名
- `gen_ai.response.model` = 实际响应模型

**JAR**: `spring-ai-model`

### 2. EmbeddingModel — 嵌入模型

**Observation 名称**: `gen_ai.client.operation`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.embedding.observation.DefaultEmbeddingModelObservationConvention` |
| Context | `o.s.a.embedding.observation.EmbeddingModelObservationContext` |
| Documentation | `o.s.a.embedding.observation.EmbeddingModelObservationDocumentation` |
| Handler | `o.s.a.embedding.observation.EmbeddingModelMeterObservationHandler` |

**JAR**: `spring-ai-model`

### 3. ImageModel — 图像模型

**Observation 名称**: `gen_ai.client.operation`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.image.observation.DefaultImageModelObservationConvention` |
| Context | `o.s.a.image.observation.ImageModelObservationContext` |
| Documentation | `o.s.a.image.observation.ImageModelObservationDocumentation` |
| Handler | `o.s.a.image.observation.ImageModelPromptContentObservationHandler` |

**JAR**: `spring-ai-model`

### 4. Tool Calling — 工具调用

**Observation 名称**: `spring.ai.tool`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.tool.observation.DefaultToolCallingObservationConvention` |
| Context | `o.s.a.tool.observation.ToolCallingObservationContext` |
| Documentation | `o.s.a.tool.observation.ToolCallingObservationDocumentation` |
| Filter | `o.s.a.tool.observation.ToolCallingContentObservationFilter` |

**JAR**: `spring-ai-model`

### 5. ChatClient — 聊天客户端

**Observation 名称**: `spring.ai.chat.client`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.chat.client.observation.DefaultChatClientObservationConvention` |
| Context | `o.s.a.chat.client.observation.ChatClientObservationContext` |
| Documentation | `o.s.a.chat.client.observation.ChatClientObservationDocumentation` |
| Handler | `o.s.a.chat.client.observation.ChatClientCompletionObservationHandler` |
| Handler | `o.s.a.chat.client.observation.ChatClientPromptContentObservationHandler` |

**标签（低基数）**:
- `gen_ai.operation.name` = `framework`
- `gen_ai.system` = `spring_ai`
- `spring.ai.kind` = `chat_client`
- `spring.ai.chat.client.stream` = `true`/`false`

**标签（高基数，仅 Trace）**:
- `spring.ai.chat.client.advisors` = advisor 列表
- `spring.ai.chat.client.conversation_id` = 会话 ID
- `spring.ai.chat.client.tools` = 工具名列表

**JAR**: `spring-ai-client-chat`

### 6. Advisor — 顾问

**Observation 名称**: `spring.ai.advisor`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.chat.client.advisor.observation.DefaultAdvisorObservationConvention` |
| Context | `o.s.a.chat.client.advisor.observation.AdvisorObservationContext` |
| Documentation | `o.s.a.chat.client.advisor.observation.AdvisorObservationDocumentation` |

**标签（低基数）**:
- `gen_ai.operation.name` = `framework`
- `gen_ai.system` = `spring_ai`
- `spring.ai.kind` = `adviser`
- `advisor.name` = advisor 名称（如 `call`）

**JAR**: `spring-ai-client-chat`

### 7. VectorStore — 向量存储

**Observation 名称**: `db.vector.client.operation`

| 类 | 完整路径 |
|---|---------|
| Convention | `o.s.a.vectorstore.observation.DefaultVectorStoreObservationConvention` |
| Context | `o.s.a.vectorstore.observation.VectorStoreObservationContext` |
| Documentation | `o.s.a.vectorstore.observation.VectorStoreObservationDocumentation` |
| Handler | `o.s.a.vectorstore.observation.VectorStoreQueryResponseObservationHandler` |

**标签（低基数）**:
- `db.operation.name` = 操作名（add/delete/get/query）
- `db.system` = 向量数据库名（如 `redis`）
- `db.index.operation` = 索引操作

**JAR**: `spring-ai-vector-store`

---

## 自动装配

| AutoConfiguration | 所在 JAR | 作用 |
|---|---|---|
| `ChatObservationAutoConfiguration` | `spring-ai-autoconfigure-model-chat-observation` | 注册 ChatModel 的 Convention + Handler |
| `EmbeddingObservationAutoConfiguration` | `spring-ai-autoconfigure-model-embedding-observation` | 注册 EmbeddingModel 的 Convention + Handler |
| `ImageObservationAutoConfiguration` | `spring-ai-autoconfigure-model-image-observation` | 注册 ImageModel 的 Convention + Handler |
| `ChatClientAutoConfiguration` | `spring-ai-autoconfigure-model-chat-client` | 注册 ChatClient + Advisor 的 Convention + Handler |

---

## 如何自定义指标名称

### ChatClient

使用 4 参数工厂方法，传入自定义 Convention：

```java
ChatClient chatClient = ChatClient.create(
    model,
    obsRegistry,
    new ChatClientObservationConvention() {
        @Override
        public String getName() { return "my.custom.metric.name"; }
        @Override
        public KeyValues getLowCardinalityKeyValues(ChatClientObservationContext ctx) {
            return KeyValues.of(KeyValue.of("my.tag", "value"));
        }
        @Override
        public KeyValues getHighCardinalityKeyValues(ChatClientObservationContext ctx) {
            return KeyValues.empty();
        }
        @Override
        public String getContextualName(ChatClientObservationContext ctx) {
            return "my custom chat client";
        }
        @Override
        public boolean supportsContext(Context ctx) {
            return ctx instanceof ChatClientObservationContext;
        }
    },
    null  // 或自定义 AdvisorObservationConvention
);
```

### ChatModel

重写 `DefaultChatModelObservationConvention.getName()`，或注册自定义 Convention 到 `ObservationRegistry`：

```java
obsRegistry.observationConfig()
    .observationHandler(new DefaultMeterObservationHandler(meterRegistry) {
        @Override
        public String getName() { return "custom.name"; }
    });
```

### 通用方法：GlobalObservationConvention

注册一个全局 Convention，覆盖所有同名 Observation 的标签：

```java
obsRegistry.observationConfig()
    .observationConvention(new MyGlobalConvention());
```

---

## 指标 vs Trace 的标签分流

- **lowCardinalityKeyValue** → 同时出现在 **指标标签** 和 **Trace 属性** 中 → 可按此维度过滤/聚合指标
- **highCardinalityKeyValue** → 只出现在 **Trace 属性** 中 → 不污染指标维度（防止 Prometheus 时序爆炸）

例如 ChatClient 的 `spring.ai.chat.client.tools` 是高基数标签，只存在于 Trace 中，
而 `gen_ai.system` 是低基数标签，既在指标中也在 Trace 中。
