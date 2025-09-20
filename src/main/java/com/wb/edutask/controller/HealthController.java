package com.wb.edutask.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 애플리케이션 상태 확인을 위한 Health Check Controller
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@RestController
@RequestMapping("/health")
public class HealthController {
    
    /**
     * 애플리케이션 상태 확인
     * 
     * @return 애플리케이션 상태 정보
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("timestamp", LocalDateTime.now());
        healthInfo.put("application", "WB Education Task Management System");
        healthInfo.put("version", "1.0.0");
        
        return ResponseEntity.ok(healthInfo);
    }
    
    /**
     * 간단한 상태 확인
     * 
     * @return 상태 메시지
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }
}
