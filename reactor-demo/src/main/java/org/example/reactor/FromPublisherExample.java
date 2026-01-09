package org.example.reactor;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class FromPublisherExample {

    public static void main(String[] args) {
        System.out.println("=== 从 Mono 转换 ===");
        Mono<String> mono = Mono.just("Hello from Mono");
        Flux<String> fluxFromMono = Flux.from(mono);
        fluxFromMono.subscribe(System.out::println);

        System.out.println("\n=== 从另一个 Flux 转换 ===");
        Flux<Integer> sourceFlux = Flux.range(1, 3);
        Flux<Integer> fluxFromFlux = Flux.from(sourceFlux);
        fluxFromFlux
                .map(n -> n * 2)
                .subscribe(System.out::println);

        System.out.println("\n=== 从自定义 Publisher 转换 ===");
        // 自定义 Publisher 实现
        Publisher<String> customPublisher = new Publisher<>() {
            @Override
            public void subscribe(Subscriber<? super String> subscriber) {
                subscriber.onNext("Custom");
                subscriber.onNext("Publisher");
                subscriber.onComplete();
            }
        };

        Flux<String> fluxFromCustom = Flux.from(customPublisher);
        fluxFromCustom.subscribe(System.out::println);
    }
}