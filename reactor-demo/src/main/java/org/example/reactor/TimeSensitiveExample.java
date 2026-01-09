package org.example.reactor;

import reactor.core.publisher.Flux;
import java.time.LocalTime;
import java.util.List;

public class TimeSensitiveExample {

    public static Flux<String> getCurrentTime() {
        return Flux.defer(() -> {
            String currentTime = LocalTime.now().toString();
            System.out.println("获取时间: " + currentTime);
            return Flux.just("当前时间", currentTime);
        });
    }

    public static void main(String[] args) throws InterruptedException {
        Flux<String> timeFlux = getCurrentTime();

        // 第一次订阅
        timeFlux.subscribe(System.out::println);

        Thread.sleep(1000);

        // 第二次订阅（时间已变化）
        timeFlux.subscribe(System.out::println);
    }
}