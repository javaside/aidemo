package org.example.reactor;

import reactor.core.publisher.Flux;

import java.util.stream.Stream;

public class FromStreamExample {

    public static void main(String[] args) {
        System.out.println("=== 从 Stream 转换 ===");

        // 创建 Stream
        Stream<String> stream = Stream.of("A", "B", "C", "D");

        // 转换为 Flux
        Flux<String> fluxFromStream = Flux.fromStream(stream);

        fluxFromStream.subscribe(
                item -> System.out.println("接收到: " + item),
                error -> System.out.println("错误: " + error),
                () -> System.out.println("Stream 完成")
        );

        // 注意：Stream 只能消费一次
        System.out.println("\n=== Stream 只能消费一次 ===");
        try {
            Stream<String> onceStream = Stream.of("One", "Two");
            Flux<String> flux1 = Flux.fromStream(onceStream);
            flux1.subscribe(System.out::println);

            // 再次使用同一个 Stream 会报错
            Flux<String> flux2 = Flux.fromStream(onceStream);
            flux2.subscribe(System.out::println);
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
        }

        System.out.println("\n=== 使用 Stream 的 Supplier ===");
        // 解决方案：使用 Supplier 创建新的 Stream
        Flux<String> safeFlux = Flux.fromStream(() ->
                Stream.of("X", "Y", "Z")
        );

        // 可以多次订阅
        safeFlux.subscribe(item -> System.out.println("第一次: " + item));
        safeFlux.subscribe(item -> System.out.println("第二次: " + item));

        System.out.println("\n=== 无限 Stream ===");
        Stream<Double> infiniteStream = Stream.generate(Math::random).limit(5);
        Flux<Double> infiniteFlux = Flux.fromStream(infiniteStream);
        infiniteFlux.subscribe(System.out::println);
    }
}