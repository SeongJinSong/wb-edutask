package com.wb.edutask.controller;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 간단한 컨트롤러 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
class SimpleControllerTest {

    @Autowired
    private MemberApiController memberApiController;

    @Test
    void contextLoads() {
        // Spring 컨텍스트가 정상적으로 로드되는지 확인
        assertNotNull(memberApiController);
    }
}
