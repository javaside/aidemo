# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 核心原则

1. **禁止臆想**：未提供的 API/密钥/路径/配置，必须明确说明"需补充"，不得编造虚假值。
2. **循证交付**：所有方案必须基于上下文与已知信息，不确定时主动追问关键信息。
3. **安全优先**：代码必做参数校验、错误处理、日志输出；敏感配置永不硬编码。
4. **简洁高效**：拒绝冗余代码与过度设计，优先主流成熟方案，必要时提供备选。
5. **编写目的**：示例代码都是为了新手快速入门使用，一眼就能学会，不要过渡复杂。
6. **GIT管理**：每次增加修改都应commit，方便回退，但不要Push远程仓库。

## 项目概述

多模块 Maven 项目，演示 AI 框架的使用（Spring AI、LangChain4j、MCP 等）。每个模块可独立运行，都是新手入门的示例代码。

## 常用命令

```bash
# 构建项目
mvn clean install

# 运行指定模块的示例（需进入模块目录）
cd springai-demo
mvn compile exec:java -Dexec.mainClass="org.example.springai.类名"

# 运行 Spring AI Web 应用
cd springai-demo
mvn spring-boot:run
```

## 模块架构

### springai-demo（Spring AI 核心模块）
- `ChatClient` 是主要 API 入口，通过 `ChatClient.builder(model).build()` 创建
- API 密钥在代码中硬编码（演示用途），生产环境应使用 `application.properties`
- 向量存储：Redis、PgVector、Ollama 等多种配置示例
- RAG：支持 `RetrievalAugmentationAdvisor` 进行查询变换和扩展

### springai-mcp-server / springai-mcp-client
- MCP 协议实现，服务器提供工具和资源，客户端调用
- 连接配置在 `application.properties`，连接名 `timemcp`

### langchain4j-demo
- 独立的 main 方法入口，可直接运行
- 包含 Agent、Tool、Workflow 等模式演示

### reactor-demo
- Project Reactor 响应式编程示例
- 独立运行：`mvn exec:java -Dexec.mainClass="org.example.reactor.类名"`

## 核心模式

### ChatClient 调用模式
```java
// 基础调用
chatClient.prompt("指令").call().content();

// 带系统提示
chatClient.prompt()
    .system("你是一位...")
    .user("问题")
    .call().content();

// 带参数模板
chatClient.prompt()
    .user(u -> u.text("主题: {topic}").params(Map.of("topic", "AI")))
    .call().content();
```

### 独立运行（非 Spring Boot）
有些示例是独立运行的 Java 程序，不需要 Spring 容器：
```java
public static void main(String[] args) {
    DeepSeekApi api = DeepSeekApi.builder().apiKey("sk-...").build();
    ChatModel model = DeepSeekChatModel.builder().deepSeekApi(api).build();
    ChatClient client = ChatClient.builder(model).build();
    // 使用 client...
}
```

## 提交规范

使用 conventional commits：`type(scope): message`，type 为 `feat/fix/refactor/docs`。每次修改应 commit，但不 push 远程仓库。

## 配置注意

- API 密钥和端点地址存放在 `src/main/resources/application.properties`
- MCP 客户端配置：连接名 `timemcp` 在注解中引用（`clients = "timemcp"`）
- 复杂功能的设计文档在 `docs/superpowers/specs/`