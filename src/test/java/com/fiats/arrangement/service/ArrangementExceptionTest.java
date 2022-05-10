package com.fiats.arrangement.service;

import com.fiats.arrangement.redis.entity.ArrangementException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Slf4j
public class ArrangementExceptionTest {

    @Autowired
    ArrangementExceptionService arrangementExceptionService;

    @Test
    void test_ApproveException() {
//        arrangementExceptionService.approveArrangementException("ec2b1885-fdb6-4b51-8f71-a002a94df6f5");
    }
}
