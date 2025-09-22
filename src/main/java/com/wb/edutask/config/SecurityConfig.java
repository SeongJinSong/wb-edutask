package com.wb.edutask.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스
 * H2 콘솔 접근 및 개발 환경을 위한 보안 설정
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-22
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Spring Security 필터 체인 설정
     * H2 콘솔 접근을 위한 CSRF 및 Frame Options 설정
     * 
     * @param http HttpSecurity 객체
     * @return SecurityFilterChain
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // H2 콘솔과 API를 위한 CSRF 비활성화
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**", "/api/**"))
            // H2 콘솔 프레임 허용
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            // 인증 설정
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()  // H2 콘솔 접근 허용
                .requestMatchers("/api/**").permitAll()         // API 엔드포인트 허용
                .anyRequest().permitAll()                       // 모든 요청 허용 (개발용)
            );
        return http.build();
    }
}
