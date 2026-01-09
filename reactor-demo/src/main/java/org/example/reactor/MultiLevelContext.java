package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

public class MultiLevelContext {

    static Flux<String> createServiceFlux() {
        return Flux.deferContextual(ctx -> {
            String serviceName = ctx.get("service");
            String requestId = ctx.get("requestId");

            System.out.println("Service [" + serviceName + "] processing request: " + requestId);

            return Flux.range(1, 3)
                    .map(i -> String.format("[%s-%s] Item %d", serviceName, requestId, i));
        });
    }

    static Flux<String> processWithRetry() {
        return createServiceFlux()
                .doOnNext(item -> {
                    // 可以在这里访问 Context
                    String requestId = (String) Mono.deferContextual(
                            monoCtx -> Mono.just(monoCtx.get("requestId"))
                    ).block(); // 注意：实际中应避免阻塞
                    System.out.println("Processing item for request: " + requestId);
                })
                .retryWhen(reactor.util.retry.Retry.fixedDelay(2, java.time.Duration.ofSeconds(1)));
    }

    public static void main(String[] args) {
        processWithRetry()
                .contextWrite(Context.of(
                        "service", "OrderService",
                        "requestId", "REQ-001"
                ))
                .subscribe(
                        System.out::println,
                        error -> System.out.println("Error: " + error),
                        () -> System.out.println("Completed")
                );
    }
}