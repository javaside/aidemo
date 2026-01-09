package org.example.reactor;

import reactor.core.publisher.Flux;

public class BasicHandleExample {

    public static void main(String[] args) {
        System.out.println("=== 基础过滤和转换 ===");

        Flux<Integer> numbers = Flux.range(1, 10);

        Flux<String> result = numbers.handle((number, sink) -> {
            if (number % 2 == 0) {  // 过滤：只处理偶数
                String transformed = "偶数: " + number;  // 转换
                sink.next(transformed);
            }
            // 奇数被静默忽略
        });

        result.subscribe(System.out::println);

        System.out.println("\n=== 复杂条件处理 ===");

        Flux.range(1, 10)
                .handle((n, sink) -> {
                    if (n % 3 == 0) {
                        sink.next("Fizz");
                    } else if (n % 5 == 0) {
                        sink.next("Buzz");
                    } else if (n % 15 == 0) {
                        sink.next("FizzBuzz");
                    } else if (n % 2 == 0) {
                        sink.next("偶数: " + n);
                    }
                    // 其他情况不发射
                })
                .subscribe(System.out::println);
    }
}