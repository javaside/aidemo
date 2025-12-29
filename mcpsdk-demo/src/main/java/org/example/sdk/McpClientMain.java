package org.example.sdk;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

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

        Mono<McpSchema.CallToolResult> callToolResult = client.callTool(new McpSchema.CallToolRequest("calculate", Map.of("operation", "+", "a", 2, "b", 3)));

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
