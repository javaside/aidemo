# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 核心原则

1. **禁止臆想**：未提供的 API/密钥/路径/配置，必须明确说明"需补充"，不得编造虚假值。
2. **循证交付**：所有方案必须基于上下文与已知信息，不确定时主动追问关键信息。
3. **安全优先**：代码必做参数校验、错误处理、日志输出；敏感配置永不硬编码。
4. **简洁高效**：拒绝冗余代码与过度设计，优先主流成熟方案，必要时提供备选。

## 构建与运行

```bash
# 构建所有模块
mvn clean install -DskipTests

# 运行 Spring Boot 应用（server 端口 8080，client 端口 9090）
mvn -pl springai-mcp-server spring-boot:run
mvn -pl springai-mcp-client spring-boot:run

# 通过 exec 运行非 Spring 的 demo
mvn -pl reactor-demo exec:java -Dexec.mainClass="org.example.reactor.BasicGenerateExample"
mvn -pl langchain4j-demo exec:java -Dexec.mainClass="org.example.langchain4j.Main"
```

本项目没有测试——它是一个演示/教学项目，不是类库。

## 架构

**根 POM**（`pom.xml`）：聚合 POM，`<packaging>pom</packaging>`。所有依赖版本通过 `<dependencyManagement>` 中的 BOM 统一管理。子模块继承此 POM——它们不使用 `spring-boot-starter-parent`。

**模块一览：**

| 模块 | 用途 |
|---|---|
| `springai-mcp-server` | MCP 服务端——通过 `@McpTool` 暴露工具/资源。运行在 8080 |
| `springai-mcp-client` | MCP 客户端——连接服务端、调用工具、处理服务端→客户端通知。运行在 9090 |
| `mcpsdk-demo` | MCP SDK 演示，含 Spring Boot + Controller |
| `mcpsdk-commom` | MCP SDK 共享工具类（SchemaObj, JsonSchemaArgumentGenerator） |
| `springai-demo` | Spring AI 示例：ChatClient、RAG、工具调用、结构化输出、Advisor |
| `springai-alibaba-demo` | 阿里云 AI：StateGraph、记忆机制 |
| `langchain4j-demo` | LangChain4j：聊天、流式、工作流、Agent、工具 |
| `reactor-demo` | Project Reactor：Flux/Mono 操作 |
| `micrometer-demo` | Micrometer 可观测性 |

**关键依赖：** `springai-mcp-client` 依赖 `mcpsdk-commom` 以使用共享的 Schema 工具类。

## MCP 通信模式

`springai-mcp-server` 与 `springai-mcp-client` 组成 MCP streamable HTTP 协议的配对演示。服务端在方法上使用 `@McpTool` 注解；客户端在处理方法上使用 `@McpLogging`、`@McpProgress`、`@McpSampling`、`@McpElicitation`、`@McpToolListChanged` 注解。

**服务端→客户端通知流程：**
1. 客户端调用 `mcpClient.callTool(name, args, progressToken)`
2. 服务端工具方法接收 `McpAsyncRequestContext`，通过它向客户端推送通知
3. 客户端处理方法（带有 `@McpXxx(clients = "timemcp")` 注解）接收通知

**重要：** 所有 MCP 服务端通知调用（`ctx.info()`、`ctx.progress()`、`ctx.sample()` 等）返回的是冷 `Mono<Void>`——必须通过 `.then()` / `.flatMap()` 串联到响应式链中，否则通知不会被真正发送。

**服务端必须运行在 stateful 模式：** MCP 服务端的 `application.properties` 需要配置 `spring.ai.mcp.server.stateful=true`，否则通知上下文无法在工具调用之间保持。

**MCP 模块的包组织约定：**
- `*.basic.*`——较早期/底层的 MCP 演示，直接使用协议 API
- `*.annot.*`——注解风格演示，使用 `@McpTool` / `@McpXxx` 注解
- `*.spring.*`——Spring Boot 应用入口和 Provider 注册

## 配置

API 密钥和端点地址存放在各模块的 `src/main/resources/application.properties` 中。MCP 客户端通过以下配置连接服务端：
```properties
spring.ai.mcp.client.streamable-http.connections.timemcp.url=http://localhost:8080
spring.ai.mcp.client.streamable-http.connections.timemcp.endpoint=/mcp
```
连接名 `timemcp` 在客户端处理注解中被引用（`clients = "timemcp"`）。

## 提交规范

使用 conventional commits 格式，中英文描述均可：`type(scope): message`，其中 type 为 `feat`、`fix`、`refactor`、`docs` 之一。scope 与模块名对应（如 `mcp`、`mcp-client`、`rag`）。

## 设计文档

复杂功能的设计规范存放在 `docs/superpowers/specs/`，实现计划存放在 `docs/superpowers/plans/`。修改 MCP 注解基础设施之前请先查阅。
