package org.example.mcpsdk;

import io.modelcontextprotocol.server.McpAsyncServer;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/mcpin")
public class McpController {

    @Autowired
    public void setMcpAsyncServer(McpAsyncServer mcpAsyncServer) {
        this.mcpAsyncServer = mcpAsyncServer;
    }

    McpAsyncServer mcpAsyncServer;

    @RequestMapping("/list")
    public Flux<McpSchema.Tool> list(){
        return this.mcpAsyncServer.listTools();
    }
}
