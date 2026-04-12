package org.example.springai.mcp.client.annot;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.mcp.annotation.spring.ClientMcpAsyncHandlersRegistry;
import org.springframework.ai.mcp.customizer.McpAsyncClientCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 配置客户端 roots，使服务端可以通过 context.roots() 获取根目录。
 *
 * <p>仅追加 roots 能力，不覆盖注解框架已通过 @McpSampling / @McpElicitation
 * 自动配置的 sampling/elicitation 能力。</p>
 *
 * <p>触发时机：Spring AI MCP 自动配置创建 McpAsyncClient 时，在 spec 初始化
 * 之后调用此 customizer。</p>
 *
 * @see McpAsyncClientCustomizer
 */
@Component
public class McpAsyncClientRootsCustomizer implements McpAsyncClientCustomizer {

    /**
     * 注解框架的 handler 注册表，用于读取已由 @McpSampling/@McpElicitation
     * 自动生成的 capabilities（sampling、elicitation）。
     *
     * 可能为 null（如果完全没有使用注解风格的 handler）。
     */
    @Autowired(required = false)
    private ClientMcpAsyncHandlersRegistry handlersRegistry;

    @Override
    public void customize(String name, McpClient.AsyncSpec spec) {
        // 1. 构造 roots capability
        var rootCap = new McpSchema.ClientCapabilities.RootCapabilities(true);

        // 2. 读取注解框架已配置的 capabilities（含 sampling、elicitation）
        //    如果没有使用注解风格 handler，则为 null
        McpSchema.ClientCapabilities base = handlersRegistry != null
            ? handlersRegistry.getCapabilities(name) : null;

        // 3. 合并 capabilities：保留 sampling/elicitation，追加 roots
        //    McpSchema.ClientCapabilities 四个字段：
        //    (experimental, roots, sampling, elicitation)
        McpSchema.ClientCapabilities merged = (base != null)
            ? new McpSchema.ClientCapabilities(
                base.experimental(),   // 保留 experimental
                rootCap,               // 追加 roots
                base.sampling(),       // 保留 sampling
                base.elicitation()     // 保留 elicitation
              )
            : new McpSchema.ClientCapabilities(null, rootCap, null, null);

        spec.capabilities(merged);

        // 4. 设置初始根目录列表（供服务端通过 context.roots() 获取）
        spec.roots(List.of(
            new McpSchema.Root("file:///Users/demo/documents", "documents"),
            new McpSchema.Root("file:///Users/demo/downloads", "downloads")
        ));
    }
}
