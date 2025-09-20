package com.wb.edutask;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * WB Education Task Management System 애플리케이션 테스트 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@SpringBootTest
@ActiveProfiles("test")
class WbEdutaskApplicationTests {

    /**
     * 애플리케이션 컨텍스트 로딩 테스트
     */
    @Test
    void contextLoads() {
        // Spring Boot 애플리케이션 컨텍스트가 정상적으로 로딩되는지 확인
    }
}
