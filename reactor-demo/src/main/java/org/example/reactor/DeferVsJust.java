package org.example.reactor;

import reactor.core.publisher.Flux;
import java.time.Instant;

public class DeferVsJust {

    public static void main(String[] args) throws InterruptedException {
        // just - 创建时就确定值
        Instant justTime = Instant.now();
        Flux<Instant> fluxJust = Flux.just(justTime);

        // defer - 订阅时才确定值
        Flux<Instant> fluxDefer = Flux.defer(() ->
                Flux.just(Instant.now()));

        System.out.println("=== Flux.just ===");
        fluxJust.subscribe(time ->
                System.out.println("订阅时间1: " + time));
        Thread.sleep(100);
        fluxJust.subscribe(time ->
                System.out.println("订阅时间2: " + time));

        Thread.sleep(100);

        System.out.println("\n=== Flux.defer ===");
        fluxDefer.subscribe(time ->
                System.out.println("订阅时间1: " + time));
        Thread.sleep(100);
        fluxDefer.subscribe(time ->
                System.out.println("订阅时间2: " + time));
    }
}