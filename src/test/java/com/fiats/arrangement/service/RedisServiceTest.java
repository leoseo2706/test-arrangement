//package com.fiats.arrangement.service;
//
//import com.fiats.arrangement.constant.ArrConstant;
//import com.fiats.tmgcoreutils.utils.CommonUtils;
//import lombok.extern.slf4j.Slf4j;
//import org.junit.jupiter.api.DisplayName;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.data.redis.core.RedisTemplate;
//
//@SpringBootTest
//@Slf4j
//public class RedisServiceTest {
//
//    @Autowired
//    RedisTemplate<String, String> redisTemplate;
//
//    @Test
//    @DisplayName("Test RedisServiceTest")
//    void test_RedisServiceTest() {
//        redisTemplate.opsForValue().set(CommonUtils.format(ArrConstant.USER_KEY_FORMAT,
//                "123", "action"), "test_user1");
//        log.info("Done saving ...");
//
//        String user = redisTemplate.opsForValue().get(CommonUtils.format(ArrConstant.USER_KEY_FORMAT, "123", "action"));
//        log.info("Loading user : {}", user);
//
//        redisTemplate.delete(CommonUtils.format(ArrConstant.USER_KEY_FORMAT, "123", "action"));
//
//    }
//}
