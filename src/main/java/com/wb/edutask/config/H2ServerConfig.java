package com.wb.edutask.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import lombok.extern.slf4j.Slf4j;

/**
 * H2 데이터베이스 TCP 서버 설정 클래스
 * 외부에서 H2 DB에 접근할 수 있도록 TCP 서버를 구성
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-22
 */
@Slf4j
@Configuration
@Profile({"dev", "local"}) // 개발 환경에서만 활성화
public class H2ServerConfig {

    /**
     * H2 TCP 서버 빈 설정
     * 외부 접근을 허용하는 H2 TCP 서버를 9092 포트에서 실행
     * 
     * @return H2 TCP 서버 인스턴스
     * @throws java.sql.SQLException SQL 예외 발생 시
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    public Server h2TcpServer() throws java.sql.SQLException {
        log.info("H2 TCP 서버를 시작합니다. 포트: 9092");
        
        // 9092 포트에서 TCP 서버 시작, 외부 접근 허용
        Server server = Server.createTcpServer(
            "-tcp",              // TCP 서버 모드
            "-tcpAllowOthers",   // 외부 접근 허용
            "-tcpPort", "9092"   // 포트 번호 9092
        );
        
        log.info("H2 TCP 서버가 구성되었습니다.");
        log.info("외부 접속 URL: jdbc:h2:tcp://localhost:9092/mem:testdb");
        log.info("사용자명: sa, 비밀번호: (없음)");
        
        return server;
    }
}
