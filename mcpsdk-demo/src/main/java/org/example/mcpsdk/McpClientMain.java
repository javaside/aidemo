package org.example.mcpsdk;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.example.commom.JsonSchemaArgumentGenerator;
import org.example.commom.SchemaObj;

public class McpClientMain {
    public static void main(String[] args) throws IOException {
        McpClientTransport transport = HttpClientStreamableHttpTransport
                .builder("http://localhost:8080/mcp")
                .build();
        // Create an async client with custom configuration
        McpAsyncClient client = McpClient.async(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .build();

        Mono<McpSchema.ListToolsResult> tools = client.listTools();
        tools.subscribe(tool -> System.out.println("tool: " + tool));

        client.listResources().subscribe(resource -> System.out.println("resource: " + resource));

        // 使用JsonSchema自动生成参数
        McpSchema.JsonSchema jsonSchema = new McpSchema.JsonSchema(
            "object", 
            Map.of(
                "operation", new SchemaObj("String", "Operator for calculation"),
                "a", new SchemaObj("number", "First number"),
                "b", new SchemaObj("number", "Second number")
            ), 
            List.of("operation", "a", "b"), 
            true, 
            null, 
            null
        );

        Map<String, Object> arguments = JsonSchemaArgumentGenerator.generateArgumentsFromSchema(jsonSchema);

        McpSchema.CallToolRequest toolRequest = McpSchema.CallToolRequest.builder().name("calculate").arguments(arguments).build();
        System.out.println("Generated tool request with arguments: " + toolRequest.arguments());
        
        Mono<McpSchema.CallToolResult> callToolResult = client.callTool(toolRequest);

        callToolResult.subscribe(result ->{
            System.out.println("result: " + result);
            result.content().forEach(System.out::println);
        });

        System.out.println("================");

        Mono<McpSchema.ReadResourceResult> docs = client.readResource(new McpSchema.ReadResourceRequest("docs"));

        docs.subscribe(result -> result.contents().forEach(System.out::println));
        System.in.read();
    }
}
