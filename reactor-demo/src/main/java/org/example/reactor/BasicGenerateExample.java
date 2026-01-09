package org.example.reactor;

import reactor.core.publisher.Flux;

public class BasicGenerateExample {

    public static void main(String[] args) {
        System.out.println("=== 基础计数器 ===");

        Flux<Integer> counter = Flux.generate(
                () -> 0,  // 初始状态：计数器从0开始
                (state, sink) -> {
                    if (state < 5) {
                        sink.next(state);  // 发射当前状态
                        return state + 1;  // 返回新状态
                    } else {
                        sink.complete();   // 完成流
                        return state;      // 返回最终状态
                    }
                }
        );

        counter.subscribe(
                num -> System.out.println("接收: " + num),
                error -> System.out.println("错误: " + error),
                () -> System.out.println("计数器完成")
        );

    }
}