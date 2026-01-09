package org.example.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.Duration;

public class BasicJoinExample {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== 基础时间窗口 JOIN ===");

        // 左流：用户登录事件
        Flux<String> userLogins = Flux.interval(Duration.ofMillis(200))
                .map(i -> "用户" + ((char)('A' + i)))
                .take(5);

        // 右流：系统事件
        Flux<String> systemEvents = Flux.interval(Duration.ofMillis(300))
                .map(i -> "事件" + (i + 1))
                .take(5);

        // 进行 JOIN：将同一时间窗口内的登录和事件配对
        Flux<String> joined = userLogins.join(
                systemEvents,
                login -> Mono.delay(Duration.ofMillis(250)), // 左窗口：250ms
                event -> Mono.delay(Duration.ofMillis(250)), // 右窗口：250ms
                (login, event) -> login + " 触发 " + event
        );

        joined.subscribe(
                result -> System.out.println("JOIN结果: " + result),
                error -> System.out.println("错误: " + error),
                () -> System.out.println("JOIN完成")
        );

        Thread.sleep(3000);

        System.out.println("\n=== 窗口大小的影响 ===");

        // 不同窗口大小的效果
        Flux<Integer> left = Flux.interval(Duration.ofMillis(100))
                .map(i -> (int)(i * 10))
                .take(5);

        Flux<Integer> right = Flux.interval(Duration.ofMillis(150))
                .map(i -> (int)(i * 10) + 1)
                .take(5);

        left.join(
                right,
                l -> Mono.delay(Duration.ofMillis(50)),   // 短窗口
                r -> Mono.delay(Duration.ofMillis(200)),  // 长窗口
                (l, r) -> l + " + " + r + " = " + (l + r)
        ).subscribe(System.out::println);

        Thread.sleep(2000);
    }
}