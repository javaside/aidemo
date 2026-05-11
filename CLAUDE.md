# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 在此仓库中工作时提供指导。

## 核心原则

1. **禁止臆想**：未提供的 API/密钥/路径/配置，必须明确说明"需补充"，不得编造虚假值。
2. **循证交付**：所有方案必须基于上下文与已知信息，不确定时主动追问关键信息。
3. **安全优先**：代码必做参数校验、错误处理、日志输出；敏感配置永不硬编码。
4. **简洁高效**：拒绝冗余代码与过度设计，优先主流成熟方案，必要时提供备选。
5. **编写目的**：示例代码都是为了新手快速入门使用，一眼就能学会，不要过渡复杂
6. **GIT管理**：每次增加修改都应commit，方便回退，但不要Push远程仓库

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
