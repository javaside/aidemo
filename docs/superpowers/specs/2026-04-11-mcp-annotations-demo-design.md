# MCP Annotations Demo — Design Spec

## Overview

Add comprehensive MCP annotation demos to the existing `springai-mcp-client` and `springai-mcp-server` modules. Two new demo classes (one per module) demonstrate the full lifecycle of server-initiated notifications and client-side handlers.

## Goals

- Demonstrate `@McpLogging`, `@McpSampling`, `@McpProgress`, `@McpToolListChanged` on the client side with clear explanations of their real-world purpose
- Demonstrate the server-side counterparts that trigger each notification
- Also include `@McpResource` on the server side (as requested: use `@McpResource` alongside other annotations)

## Architecture

Two separate processes communicate over SSE (Streamable HTTP):

```
Client (port 9090)                    Server (port 8080)
─────────────────                    ─────────────────
@McpLogging      ◄── info() ────    triggerLogging()
@McpSampling     ◄── sample() ────  triggerSampling()
@McpProgress      ◄── progress() ──  triggerProgress()
@McpToolListChanged ◄── notify ───  triggerToolListChanged()

DeepSeek LLM ◄─── @McpSampling ─────  (server requests LLM via client)
```

## Module 1: springai-mcp-server

### New Class: `McpServerAnnotationsDemo`

**Package:** `org.example.springai.mcp.server`

**Server must be stateful** (`spring.ai.mcp.server.stateful=true`) because notifications use `McpSyncRequestContext`.

#### Tools

| Tool Name | Description | Notification Triggered |
|---|---|---|
| `triggerLogging` | Triggers a logging notification | `context.info("...")` → `LoggingMessageNotification` |
| `triggerProgress` | Sends 0%, 50%, 100% progress | `context.progress(...)` → `ProgressNotification` |
| `triggerSampling` | Requests LLM sampling via client | `context.sample(...)` → `CreateMessageRequest` |
| `triggerToolListChanged` | Notifies current tool list | Server change-notification protocol |

#### Resource

| URI | Name | Description |
|---|---|---|
| `server://annotations/info` | `annotationInfo` | Static resource describing the demo |

#### Config

**`src/main/resources/application.properties`:**
```properties
spring.ai.mcp.server.type=async
spring.ai.mcp.server.stateful=true
spring.ai.mcp.server.protocol=streamable
spring.ai.mcp.server.annotation-scanner.enabled=true
```

## Module 2: springai-mcp-client

### New Class: `McpClientAnnotationsDemo`

**Package:** `org.example.springai.mcp.client`

**Runs as a non-web (`WebApplicationType.NONE`) `ApplicationRunner`.**

#### Handler Methods

| Annotation | Method | Purpose |
|---|---|---|
| `@McpLogging` | `handleLogging(LoggingMessageNotification)` | Prints server log messages with level and data |
| `@McpSampling` | `handleSampling(CreateMessageRequest)` | Forwards request to DeepSeek, returns `CreateMessageResult` |
| `@McpProgress` | `handleProgress(ProgressNotification)` | Prints progress percentage and message |
| `@McpToolListChanged` | `handleToolListChanged(List<Tool>)` | Prints the updated tool list |

#### Execution Flow (in `run()`)

1. Call `triggerLogging` → observe `@McpLogging` handler fires
2. Call `triggerProgress` → observe `@McpProgress` handler fires (0% → 50% → 100%)
3. Call `triggerSampling` → observe `@McpSampling` handler fires, DeepSeek is called, result returned
4. Call `triggerToolListChanged` → observe `@McpToolListChanged` handler fires
5. Each step prints explanatory text describing what happened and why it matters

#### Config

**`src/main/resources/application.properties`:**
```properties
server.port=9090
spring.ai.deepseek.api-key=...
spring.ai.deepseek.base-url=https://api.deepseek.com
spring.ai.mcp.client.streamable-http.connections.timemcp.url=http://localhost:8080
spring.ai.mcp.client.streamable-http.connections.timemcp.endpoint=/mcp
spring.ai.mcp.client.type=async
```

## Annotation Purpose Explanations (to include in code comments and console output)

- **@McpLogging**: Servers send structured log messages to clients. Use case: server-side debugging, audit trails, displaying server activity to users.
- **@McpSampling**: Servers request LLM completions through clients without needing their own API key. Use case: servers act as AI intermediaries; multi-tenant scenarios where the client controls LLM access.
- **@McpProgress**: Long-running server operations report progress to clients. Use case: batch processing, file uploads, data imports — clients can show progress bars.
- **@McpToolListChanged**: Servers notify clients when available tools change. Use case: dynamic tool registration (plugins, permissions changes); clients keep their tool registry up to date.

## Files to Create/Modify

### New Files
- `springai-mcp-server/src/main/java/org/example/springai/mcp/server/McpServerAnnotationsDemo.java`
- `springai-mcp-client/src/main/java/org/example/springai/mcp/client/McpClientAnnotationsDemo.java`

### Modified Files
- `springai-mcp-server/src/main/resources/application.properties` — add `spring.ai.mcp.server.stateful=true`

## Verification

1. Start server: `cd springai-mcp-server && mvn spring-boot:run`
2. Start client: `cd springai-mcp-client && mvn spring-boot:run`
3. Observe console output for each handler invocation
4. Confirm DeepSeek response appears for the sampling demo
