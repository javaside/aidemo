/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.example.springai.mcp.client.spring;

import io.modelcontextprotocol.client.McpAsyncClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.*;
import io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteArgument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

@SpringBootApplication
public class McpClientApplication implements ApplicationRunner {

	private List<McpAsyncClient> mcpAsyncClients;

	public static void main(String[] args) {
		SpringApplication.run(McpClientApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		for (McpAsyncClient mcpClient : mcpAsyncClients) {
			System.out.println(">>> MCP Client: " + mcpClient.getClientInfo());

			mcpClient.listTools().block().tools().forEach(tool -> {
				System.out.println(">>> Tool: " + tool.name());
				System.out.println(">>> Input Schema: " + tool.inputSchema().toString());
			});

			// Call a tool that sends progress notifications
			CallToolRequest toolRequest = CallToolRequest.builder()
					.name("tool4")
					.arguments(Map.of("input", "test input"))
					.progressToken(666)
					.build();
			CallToolResult response = mcpClient.callTool(toolRequest).block();
			System.out.println("Tool response: " + response);
		}
	}

	@Autowired
	public void setMcpAsyncClients(List<McpAsyncClient> mcpAsyncClients) {
		this.mcpAsyncClients = mcpAsyncClients;
	}

}