package org.example.mcpsdk;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.WebFluxStreamableServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.example.commom.SchemaObj;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Configuration
class McpConfig {
    @Bean
    WebFluxStreamableServerTransportProvider webFluxStreamableServerTransportProvider() {
        return WebFluxStreamableServerTransportProvider.builder()
                .build();
    }

    @Bean
    RouterFunction<?> mcpRouterFunction(WebFluxStreamableServerTransportProvider transportProvider) {
        return transportProvider.getRouterFunction();
    }

    @Bean
    McpAsyncServer mcpAsyncServer(WebFluxStreamableServerTransportProvider transportProvider) {


        return McpServer.async(transportProvider)
                .tools(tools())
                .resources(resources())
                .prompts(prompts())
                .build();
    }

    private static McpServerFeatures.AsyncResourceSpecification resources(){
        return new McpServerFeatures.AsyncResourceSpecification(
                new McpSchema.Resource("docs","doc","doc", "Documentation files", "text/markdown", 200L, null, null),
                (exchange, request) -> Mono.fromSupplier(() -> new McpSchema.ReadResourceResult(List.of(new McpSchema.TextResourceContents("","","testcontent")))));
    }

    private static McpServerFeatures.AsyncToolSpecification tools(){
        McpSchema.Tool calculate = McpSchema.Tool.builder()
                .name("calculate")
                .description("Basic calculator")
                .inputSchema(new McpSchema.JsonSchema("object", Map.of("operation",new SchemaObj("String","Operator"),"a",new SchemaObj("number","Number"),"b",new SchemaObj("number","Number")), List.of("operation","a","b"), true, null, null))
                .build();

        McpServerFeatures.AsyncToolSpecification calculateToolSpecification = McpServerFeatures.AsyncToolSpecification.builder()
                .tool(calculate)
                .callHandler((exchange, request) -> {
                    String operation = (String) request.arguments().get("operation");
                    int a = (int) request.arguments().get("a");
                    int b = (int) request.arguments().get("b");

                    int result = -1;
                    switch ( operation){
                        case "+" -> {
                            result = a + b;
                        }
                        case "-" -> {
                            result = a - b;
                        }
                        case "*" -> {
                            result = a * b;
                        }
                        case "/" -> {
                            result = a / b;
                        }
                    }

                    return Mono.just(new McpSchema.CallToolResult(""+result, false));
                })
                .build();

        return calculateToolSpecification;
    }
    private static McpServerFeatures.AsyncPromptSpecification prompts(){
        // Async prompt specification
        return new McpServerFeatures.AsyncPromptSpecification(
                new McpSchema.Prompt("greeting", "description", List.of(
                        new McpSchema.PromptArgument("name", "description", true)
                )),
                (exchange, request) -> {
                    request.arguments();
                    request.name();
                    McpSchema.PromptMessage message = new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(null,   ""));
                    // Prompt implementation
                    return Mono.just(new McpSchema.GetPromptResult("description", List.of(message)));
                }
        );
    }
}