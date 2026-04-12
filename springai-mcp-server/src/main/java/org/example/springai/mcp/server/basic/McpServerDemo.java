package org.example.springai.mcp.server.basic;

import io.modelcontextprotocol.spec.McpSchema;
import org.springaicommunity.mcp.annotation.McpArg;
import org.springaicommunity.mcp.annotation.McpComplete;
import org.springaicommunity.mcp.annotation.McpPrompt;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.i18n.LocaleContextHolder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring AI MCP Server 示例
 *
 * 支持的注解：
 * - @McpTool: 定义 MCP 工具，客户端可调用
 * - @McpResource: 定义资源，客户端可读取
 * - @McpComplete: 定义补全器，为参数提供自动补全建议
 * - @McpPrompt: 定义提示模板，客户端可获取完整提示词
 *
 * 注意：@McpLogging 需要 Spring AI 2.0.0+ (未发布)，本示例不包含
 */
@SpringBootApplication
public class McpServerDemo {
    public static void main(String[] args) {
        SpringApplication.run(McpServerDemo.class, args);
    }

    // ==================== @McpTool 示例 ====================
    @McpTool(description = "获取当前日期时间")
    public Mono<String> getCurrentDateTime() {
        return Mono.fromSupplier(() ->
            LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString());
    }

    // ==================== @McpResource 示例 ====================
    // 1. 固定资源 - 可被 listResources() 发现
    @McpResource(uri = "system://server/info", name = "serverInfo", description = "服务器信息")
    public Mono<McpSchema.ReadResourceResult> getServerInfo() {
        return Mono.just(new McpSchema.ReadResourceResult(List.of(
            new McpSchema.TextResourceContents("system://server/info", "application/json",
                "{\"name\":\"MCP Server\",\"version\":\"1.0.0\"}")
        )));
    }

    // 2. 资源模板 - URI 含变量 {key}
    @McpResource(uri = "config://{key}", name = "config", description = "配置资源")
    public Mono<McpSchema.ReadResourceResult> getConfig(String key) {
        var config = java.util.Map.of(
            "app.name", "Spring AI Demo",
            "app.version", "1.0.0",
            "db.url", "jdbc:postgresql://localhost:5432/demo"
        );
        String value = config.getOrDefault(key, "not found: " + key);
        return Mono.just(new McpSchema.ReadResourceResult(List.of(
            new McpSchema.TextResourceContents("config://" + key, "text/plain", key + "=" + value)
        )));
    }

    // ==================== @McpComplete 示例 ====================
    // MCP Complete 作用：为资源模板或提示的参数提供补全建议
    // 两种类型：
    // 1. ref/resource - 资源模板参数补全
    // 2. ref/prompt - 提示参数补全

    // 1. 资源模板参数补全：config://{key} 的 key 参数
    @McpComplete(uri = "config://{key}")
    public Mono<List<String>> completeConfigKey(String prefix) {
        System.out.println("  [Server] 资源模板补全，prefix=" + prefix);
        List<String> allKeys = List.of("app.name", "app.version", "app.description", "db.url", "db.user", "db.password");
        List<String> matches = allKeys.stream()
            .filter(key -> key.toLowerCase().startsWith(prefix.toLowerCase()))
            .toList();
        return Mono.just(matches);
    }

    // 2. 提示参数补全：sqlGenerator 提示的 tableName 参数
    @McpComplete(prompt = "sqlGenerator")
    public Mono<List<String>> completeSqlTableName(String prefix) {
        System.out.println("  [Server] 提示参数补全，prefix=" + prefix);
        List<String> allTables = List.of("users", "orders", "products", "categories", "reviews");
        List<String> matches = allTables.stream()
            .filter(table -> table.toLowerCase().startsWith(prefix.toLowerCase()))
            .toList();
        return Mono.just(matches);
    }

    // ==================== @McpPrompt 示例 ====================
    // MCP Prompts 作用：服务器统一管理提示模板，客户端传入参数获取完整提示
    // 工作流程：listPrompts() 发现 -> getPrompt(name, args) 传入参数 -> 返回完整提示

    @McpPrompt(name = "sqlGenerator", description = "根据表名和操作类型生成 SQL")
    public Mono<McpSchema.GetPromptResult> sqlGeneratorPrompt(
            @McpArg(name = "tableName", description = "表名") String tableName,
            @McpArg(name = "operation", description = "操作类型", required = false) String operation) {

        if (operation == null || operation.isEmpty()) {
            operation = "SELECT";
        }

        String prompt = "你是一个 SQL 专家。请为表 '" + tableName + "' 生成一条 " + operation + " 语句。";
        if ("INSERT".equals(operation)) {
            prompt += "\n要求：生成带所有列的完整 INSERT 语句，并包含几条示例数据。";
        } else if ("UPDATE".equals(operation)) {
            prompt += "\n要求：生成带有 WHERE 条件的 UPDATE 语句，确保安全更新。";
        } else if ("DELETE".equals(operation)) {
            prompt += "\n要求：生成带有 WHERE 条件的 DELETE 语句，防止误删。";
        }
        return Mono.just(new McpSchema.GetPromptResult("SQL 生成器", List.of(
            new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(prompt))
        )));
    }

    @McpPrompt(name = "meetingAgenda", description = "根据会议主题生成议程")
    public Mono<McpSchema.GetPromptResult> meetingAgendaPrompt(
            @McpArg(name = "topic", description = "会议主题") String topic,
            @McpArg(name = "duration", description = "会议时长(分钟)", required = false) Integer duration) {

        if (duration == null) {
            duration = 60;
        }

        String prompt = "你是一个会议组织专家。请为主题为 '" + topic + "' 的会议生成议程。";
        prompt += "\n会议时长：" + duration + " 分钟。";
        prompt += "\n要求：生成包含开场、主要讨论事项、总结的具体议程。";
        return Mono.just(new McpSchema.GetPromptResult("会议议程生成", List.of(
            new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(prompt))
        )));
    }
}
