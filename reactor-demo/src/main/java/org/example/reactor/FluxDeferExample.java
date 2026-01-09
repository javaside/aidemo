package org.example.reactor;

import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

public class FluxDeferExample {

    // 模拟动态数据源
    private static AtomicInteger counter = new AtomicInteger(0);

    public static void main(String[] args) throws InterruptedException {
        // 使用 defer 确保每次订阅获取最新值
        Flux<Integer> deferredFlux = Flux.defer(() -> {
            int currentValue = counter.incrementAndGet();
            System.out.println("创建 Flux，当前值: " + currentValue);
            return Flux.just(currentValue);
        });

        // 第一次订阅
        deferredFlux.subscribe(value ->
                System.out.println("订阅者1: " + value));

        // 修改数据源
        counter.set(10);

        // 第二次订阅 - 获取新值
        deferredFlux.subscribe(value ->
                System.out.println("订阅者2: " + value));
    }
}