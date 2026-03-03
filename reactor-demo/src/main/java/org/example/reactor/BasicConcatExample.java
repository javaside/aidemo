package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class BasicConcatExample {

    public static void main(String[] args) {
        System.out.println("=== 基础 concatWith 使用 ===");

        Flux<String> flux1 = Flux.just("A", "B", "C");
        Flux<String> flux2 = Flux.just("D", "E", "F");
        Flux<String> flux3 = Flux.just("G", "H", "I");

        // 使用 concatWith 顺序连接
        Flux<String> concatenated = flux1
                .concatWith(flux2)
                .concatWith(flux3);

        concatenated.subscribe(
                item -> System.out.println("收到: " + item),
                error -> System.out.println("错误: " + error),
                () -> System.out.println("所有流完成")
        );

        System.out.println("\n=== 与 Mono 连接 ===");

        Mono<String> mono = Mono.just("单值数据");
        Flux<String> flux = Flux.just("流数据1", "流数据2");

        mono.concatWith(flux)
                .subscribe(System.out::println);

        Flux<Integer> numbers1 = Flux.range(1, 3);
        Flux<Integer> numbers2 = Flux.range(4, 3);
        Flux<Integer> numbers3 = Flux.range(7, 3);

        // 链式连接多个流
        numbers1.concatWith(numbers2)
                .concatWith(numbers3)
                .subscribe(n -> System.out.print(n + " "));
    }
}