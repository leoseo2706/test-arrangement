package com.fiats.arrangement.utils;

import com.fiats.tmgcoreutils.constant.Constant;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskExecutor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@SpringBootTest
@Slf4j
public class ReactiveTest {

    @Autowired
    @Qualifier(Constant.DEFAULT_THREAD_POOL)
    TaskExecutor executor;

    @Test
    @DisplayName("Test ReactiveTest")
    void test_ReactiveTest() {

        Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {

            log.info("thread: {}", Thread.currentThread().getName());

            try {
                Thread.sleep(12000);
            } catch (InterruptedException e) {
            }

            return "123";
        })).subscribe(data -> log.info("test# {} - thread {}",
                        data, Thread.currentThread().getName()));
        log.info("Done? - {}", Thread.currentThread().getName());
    }

    @Test
    @DisplayName("Test ReactiveTestException")
    void test_ReactiveTestException() {

        Mono.fromFuture(() -> CompletableFuture.supplyAsync(() -> {

            log.info("thread: {}", Thread.currentThread().getName());

            throw new RuntimeException("123");
        })).publishOn(Schedulers.fromExecutor(executor))
                .subscribe(data -> log.info("test# {} - thread {}",
                        data, Thread.currentThread().getName()));
        log.info("Done? - {}", Thread.currentThread().getName());
    }

    @Test
    @DisplayName("Test ReactiveChain")
    void test_ReactiveChain() {

        Mono<String> test = Mono.just("2");

        Mono<String> combine = Mono.fromCallable(() -> "1")
                .zipWith(test, (data1, data2) -> data1 + data2);

        combine.doOnSuccess(data -> log.info("test# {}", data)).subscribe();
    }
}
