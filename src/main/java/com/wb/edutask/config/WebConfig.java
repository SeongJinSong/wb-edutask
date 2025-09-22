package com.wb.edutask.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 설정을 위한 Configuration 클래스
 * 정적 리소스 및 CORS 설정을 담당합니다
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-21
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    
    /**
     * 정적 리소스 핸들러 설정
     * HTML, CSS, JavaScript 파일을 제공하기 위한 설정
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 정적 리소스 경로 설정
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCachePeriod(3600); // 1시간 캐시
        
        // 추가 리소스 경로 (필요시)
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCachePeriod(3600);
        
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCachePeriod(3600);
    }
    
    /**
     * CORS 설정
     * 프론트엔드에서 API 호출을 위한 CORS 허용 설정
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
