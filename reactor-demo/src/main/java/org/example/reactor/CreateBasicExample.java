package org.example.reactor;

import reactor.core.publisher.Flux;

public class CreateBasicExample {

    public static void main(String[] args) {
        System.out.println("=== 基础 Flux.create ===");

        Flux<String> flux = Flux.create(sink -> {
            // 手动控制发射
            sink.next("第一个元素");
            sink.next("第二个元素");
            sink.next("第三个元素");
            sink.complete(); // 必须调用 complete 或 error
        });

        flux.subscribe(
                item -> System.out.println("接收到: " + item),
                error -> System.out.println("错误: " + error),
                () -> System.out.println("流完成")
        );

        System.out.println("\n=== 带错误的流 ===");
        Flux<Integer> errorFlux = Flux.create(sink -> {
            sink.next(1);
            sink.next(2);
            sink.error(new RuntimeException("自定义错误"));
        });

        errorFlux.subscribe(
                System.out::println,
                error -> System.out.println("捕获错误: " + error.getMessage())
        );
    }
}
