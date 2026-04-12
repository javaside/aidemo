package org.example.springai.mcp.client.spring;

import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpSchema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springaicommunity.mcp.annotation.McpElicitation;
import org.springaicommunity.mcp.annotation.McpLogging;
import org.springaicommunity.mcp.annotation.McpProgress;
import org.springaicommunity.mcp.annotation.McpSampling;
import org.springaicommunity.mcp.context.StructuredElicitResult;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class McpClientHandlerProviders {

	private static final Logger logger = LoggerFactory.getLogger(McpClientHandlerProviders.class);

	/**
	 * Handles progress notifications for the client identified by {@code clientId = "server1"}.
	 * <br>
	 * The {@code clientId} is configured via application properties, for example:
	 * <ul>
	 *   <li>{@code spring.ai.mcp.client.sse.connections.server1.url=...}</li>
	 *   <li>{@code spring.ai.mcp.client.streamable-http.connections.server1.url=...}</li>
	 * </ul>
	 * 
	 * The handler is assigned only to the client with ID "server1".
	 *
	 * @param progressNotification the progress notification received from the server
	 */
	@McpProgress(clients = "timemcp")
	public Mono<Void> progressHandler(ProgressNotification progressNotification) {
		logger.info("MCP PROGRESS: [{}] progress: {} total: {} message: {}",
				progressNotification.progressToken(), progressNotification.progress(),
				progressNotification.total(), progressNotification.message());
		return Mono.empty();
	}

	@McpLogging(clients = "timemcp")
	public Mono<Void> loggingHandler(LoggingMessageNotification loggingMessage) {
		logger.info("MCP LOGGING: [{}] {}", loggingMessage.level(), loggingMessage.data());
		return Mono.empty();
	}

	@McpSampling(clients = "timemcp")
	public Mono<CreateMessageResult> samplingHandler(CreateMessageRequest llmRequest) {
		logger.info("MCP SAMPLING: {}", llmRequest);

		String userPrompt = ((McpSchema.TextContent) llmRequest.messages().get(0).content()).text();
		String modelHint = llmRequest.modelPreferences().hints().get(0).name();

		return Mono.just(CreateMessageResult.builder()
				.content(new McpSchema.TextContent("Response " + userPrompt + " with model hint " + modelHint))
				.build());
	}

	public record Person(String name, Number age) {}

	@McpElicitation(clients = "timemcp")
	public Mono<StructuredElicitResult<Person>> elicitationHandler(McpSchema.ElicitRequest request) {
		logger.info("MCP ELICITATION: {}", request);
		return Mono.just(new StructuredElicitResult<>(ElicitResult.Action.ACCEPT, new Person("John Doe", 42), null));
	}

}
