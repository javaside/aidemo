# AI Demo 项目

一个展示各种 AI 框架和相关技术示例的 Java 项目集合。

## 配套视频

[观看抖音视频教程](https://www.douyin.com/user/MS4wLjABAAAAnJQkxRdJGBraMZ0SuEwyeEc6rs6tGxUaxfnRbQ3DI5hz7mGA4YDuyk6xeP1WVnZV)

## 项目概览

本项目是一个多模块 Maven 项目，演示了以下技术的使用：

- **LangChain4j** - Java AI 开发框架
- **Spring AI** - Spring 生态的 AI 集成
- **Spring AI Alibaba** - 阿里云 Spring AI 扩展
- **MCP (Model Context Protocol)** - 模型上下文协议
- **Project Reactor** - 响应式编程
- **Micrometer** - 应用观察性指标

## 技术栈

- Java 21
- Maven
- Spring Boot 3.5.6
- LangChain4j 1.9.1
- Spring AI 1.1.6
- Reactor 2025.0.1
- Micrometer 1.16.3

## 模块说明

### langchain4j-demo

LangChain4j 框架的各种功能演示：

| 示例 | 描述 |
|------|------|
| `Main.java` | 基础聊天模型和记忆机制使用 |
| `StreamingChatModelTest.java` | 流式聊天模型 |
| `WanxImageModelTest.java` | 图像生成模型 |
| `ToolDemo.java` | 工具调用演示 |
| `ToolSpecificationExample.java` | 工具规范示例 |
| `SequenceWorkFlowTest.java` | 顺序工作流 |
| `ParallelWorkFlowTest.java` | 并行工作流 |
| `LoopWorkFlowTest.java` | 循环工作流 |
| `EveningPlannerAgent.java` | Agent 并行模式（晚餐规划专家） |
| `proxy/ProxyDemo.java` | Java 代理模式集成 |

### springai-demo

Spring AI 框架的各种功能演示：

| 示例 | 描述 |
|------|------|
| `SpringAiDemoApplication.java` | ChatClient 流式调用 |
| `ObservabilityWebController.java` | 可观测性 Web 端点（/ai/demo） |
| `ObservationTraceService.java` | 5 层指标收集服务 |
| `ChatClientUserDemo.java` | ChatClient 用户模式 |
| `ChatClientObservabilityDemo.java` | ChatClient 可观测性演示 |
| `ChatMemoryDemo.java` | ChatMemory 记忆机制 |
| `DeepSeekChatModelDemo.java` | DeepSeek 模型集成 |
| `DeepSeekAiDemo.java` | DeepSeek AI 对接 |
| `DeepSeekApiDemo.java` | DeepSeek API 调用 |
| `DeepSeekMemoryDemo.java` | DeepSeek 记忆机制 |
| `SpringAiRagDemo.java` | RAG（检索增强生成）演示 |
| `RetrievalAugmentationAdvisorDemo.java` | RAG 查询变换与扩展 |
| `JedisVectorStoreDemo.java` | Jedis 向量存储 |
| `OllamaEmbeddingDemo.java` | Ollama Embedding |
| `SimpleVectorStoreDemo.java` | 简单内存向量存储（测试用） |
| `PgVectorStoreDemo.java` | PgVector 向量存储（编程方式配置） |
| `ToolsDemo.java` | 工具调用演示 |
| `ToolCallbackDemo.java` | ToolCallback 回调 |
| `AdvisorDemo.java` | Advisor 使用 |
| `StructuredOutPutDemo.java` | 结构化输出 |
| `PromptTemplateDemo.java` | 提示词模板 |
| `DateTimeTools.java` | 日期时间工具 |
| `MyController.java` | WebFlux 控制器示例 |

**可观测性演示页面**：`/ai/demo`
- 5 层指标：ChatClient → Advisor → Tool → ChatModel → Token
- 流式对话测试（SSE EventSource）
- 实时指标显示

### springai-alibaba-demo

阿里云 Spring AI 扩展演示：

| 示例 | 描述 |
|------|------|
| `StateGraphDemo.java` | 状态图（State Graph）使用，展示节点和边的工作流程 |
| `MemoryExample.java` | 记忆机制示例 |

### reactor-demo

Project Reactor 响应式编程演示：

| 示例 | 描述 |
|------|------|
| `BasicGenerateExample.java` | 基础生成器 - 创建计数器流 |
| `BasicHandleExample.java` | 基础处理器 - 数据转换和过滤 |
| `BasicJoinExample.java` | 基础合并操作 - zip 和 combineLatest |
| `BasicConcatExample.java` | 基础连接操作 - concat 和 merge |
| `FluxDeferExample.java` | defer 操作 - 延迟订阅 |
| `DeferVsJust.java` | defer 与 just 的区别 |
| `TimeSensitiveExample.java` | 时间敏感操作 |
| `DeferContextualBasic.java` | 上下文感知的 defer |
| `MultiLevelContext.java` | 多级上下文传递 |
| `FromPublisherExample.java` | 从 Publisher 创建流 |
| `FromStreamExample.java` | 从 Stream 创建流 |

### micrometer-demo

Micrometer 观察性演示：

| 示例 | 描述 |
|------|------|
| `Main.java` | 基础观察模式示例 |

### springai-mcp-server

MCP (Model Context Protocol) 服务器演示：

| 示例 | 描述 |
|------|------|
| `McpServerDemo.java` | MCP 服务器，提供工具和资源 |

### springai-mcp-client

MCP 客户端演示：

| 示例 | 描述 |
|------|------|
| `McpClientDemo.java` | MCP 客户端调用示例 |

### mcpsdk-demo

MCP SDK 演示（含 Spring Boot 集成）：

| 示例 | 描述 |
|------|------|
| `McpSdkApplication.java` | Spring Boot 应用入口 |
| `McpController.java` | MCP 控制器 |
| `McpConfig.java` | MCP 配置 |
| `McpClientMain.java` | MCP 客户端主程序 |

### mcpsdk-commom

MCP SDK 公共模块：

| 类 | 描述 |
|----|------|
| `SchemaObj.java` | Schema 对象定义 |
| `JsonSchemaArgumentGenerator.java` | JSON Schema 参数生成器 |

## 构建与运行

### 构建项目

```bash
mvn clean install
```

### 运行特定模块

```bash
# 运行 LangChain4j 示例
cd langchain4j-demo
mvn exec:java -Dexec.mainClass="org.example.langchain4j.Main"

# 运行 Spring AI 示例
cd springai-demo
mvn spring-boot:run

# 运行 Reactor 示例
cd reactor-demo
mvn exec:java -Dexec.mainClass="org.example.reactor.BasicGenerateExample"

# 运行 MCP 服务器
cd springai-mcp-server
mvn spring-boot:run

# 运行 MCP SDK 演示
cd mcpsdk-demo
mvn spring-boot:run
```

## 注意事项

1. 运行某些示例可能需要配置 API 密钥（如 OpenAI、DeepSeek 等）
2. 部分示例依赖于外部服务（如向量数据库、MCP 服务器等）
3. 确保已正确设置 Java 21 环境

## 联系作者
<img src="wechat.jpg" alt="描述" width="50%" height="50%">


## 许可证

MIT License
