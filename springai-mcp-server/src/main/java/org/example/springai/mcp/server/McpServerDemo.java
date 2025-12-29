package org.example.springai.mcp.server;

import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.i18n.LocaleContextHolder;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@SpringBootApplication
public class McpServerDemo {
    public static void main(String[] args) {
        SpringApplication.run(McpServerDemo.class, args);
    }

    @McpTool(description = "Get the current date and time in the user's timezone")
    public Mono<String> getCurrentDateTime() {
        return Mono.fromSupplier(()-> {return LocalDateTime.now().atZone(LocaleContextHolder.getTimeZone().toZoneId()).toString();});
    }

    @McpResource(uri = "config",name = "config",description = "Provides configuration data")
    public Mono<String> getConfig(){
        return Mono.fromSupplier(()-> {return "config";});
    }
}
