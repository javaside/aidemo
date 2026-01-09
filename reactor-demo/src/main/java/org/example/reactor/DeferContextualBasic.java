package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.util.context.Context;

public class DeferContextualBasic {

    public static void main(String[] args) {
        // 创建基于上下文的 Flux
        Flux<String> contextualFlux = Flux.deferContextual(ctx -> {
            // 从 Context 中获取值
            String user = ctx.getOrDefault("user", "anonymous");
            String traceId = ctx.getOrDefault("traceId", "unknown");

            System.out.println("创建 Flux - User: " + user + ", TraceId: " + traceId);

            // 根据上下文决定返回的数据
            return Flux.just(
                    "Hello, " + user,
                    "Trace: " + traceId,
                    "Request processed"
            );
        });

        // 订阅时传入 Context
        contextualFlux
                .contextWrite(Context.of("user", "Alice", "traceId", "TRACE-123"))
                .subscribe(System.out::println);

        System.out.println("\n--- 不同的 Context ---");

        // 使用不同的 Context 再次订阅
        contextualFlux
                .contextWrite(Context.of("user", "Bob", "traceId", "TRACE-456"))
                .subscribe(System.out::println);
    }
}