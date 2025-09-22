package com.wb.edutask;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WB Education Task Management System 메인 애플리케이션 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@SpringBootApplication
@EnableScheduling
public class WbEdutaskApplication {

    /**
     * 애플리케이션 진입점
     * 
     * @param args 명령행 인수
     */
    public static void main(String[] args) {
        SpringApplication.run(WbEdutaskApplication.class, args);
    }
}
