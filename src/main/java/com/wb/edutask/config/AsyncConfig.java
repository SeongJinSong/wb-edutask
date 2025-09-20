package com.wb.edutask.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리를 위한 설정 클래스
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2025-09-20
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 수강신청 처리를 위한 비동기 Executor
     * 
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "enrollmentTaskExecutor")
    public Executor enrollmentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 기본 스레드 수
        executor.setCorePoolSize(5);
        
        // 최대 스레드 수
        executor.setMaxPoolSize(20);
        
        // 큐 용량
        executor.setQueueCapacity(100);
        
        // 스레드 이름 접두사
        executor.setThreadNamePrefix("enrollment-async-");
        
        // 거부 정책 (큐가 가득 찰 때)
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 애플리케이션 종료 시 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        
        executor.initialize();
        return executor;
    }
    
    /**
     * 일반적인 비동기 작업을 위한 Executor
     * 
     * @return ThreadPoolTaskExecutor
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        
        executor.initialize();
        return executor;
    }
}
