//package com.fiats.arrangement.utils;
//
//import com.fiats.arrangement.validator.ArrangementValidator;
//import com.fiats.tmgcoreutils.utils.DateHelper;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import reactor.core.publisher.Mono;
//
//import java.sql.Timestamp;
//import java.util.Optional;
//
//@SpringBootTest
//@Slf4j
//public class DateHelperTest {
//
//    @Autowired
//    ArrangementValidator arrangementValidator;
//
//    @Test
//    @DisplayName("Test DateHelperTest")
//    void test_DateHelperTest() {
//        String date = "2021-4-18";
//        Timestamp dateTs = DateHelper.parseTimestamp(date);
//        Assertions.assertTrue(arrangementValidator.isTradingDateToday(dateTs));
//    }
//
//
//    @Test
//    @DisplayName("Test Mono")
//    void test_Mono() {
//        Mono<String> mono1 = Mono.just("111")
//                .map(data -> {
//                    throw new RuntimeException("failed");
//                });
//        Mono<String> mono2 = Mono.just("999")
//                .doOnSuccess(data -> System.out.println("Getting: " + data));
//
//        Mono.justOrEmpty(Optional.ofNullable("test"))
//                .doOnNext(stringSignal -> mono1.subscribe())
//                .doOnNext(stringSignal -> mono2.subscribe())
//                .doOnNext(stringSignal -> mono2.subscribe())
//                .doOnNext(stringSignal -> mono2.subscribe())
//                .doOnNext(stringSignal -> mono2.subscribe())
//                .block();
//
//
//    }
//}
