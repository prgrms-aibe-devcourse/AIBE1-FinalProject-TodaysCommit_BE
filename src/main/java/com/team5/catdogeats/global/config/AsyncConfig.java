package com.team5.catdogeats.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * OrderEventListener의 알림 처리와 같은 비동기 작업을 위한 설정입니다.
 * 이벤트 기반 아키텍처에서 성능에 영향을 주지 않는 부가 작업들을
 * 별도의 스레드 풀에서 처리하도록 구성합니다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업용 스레드 풀 설정
     */
    @Bean(name = "orderAsyncExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 풀 크기 설정
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 설정 (로그 추적 용이)
        executor.setThreadNamePrefix("OrderEvent-Async-");

        // 애플리케이션 종료 시 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        // 스레드 풀 초기화
        executor.initialize();

        log.info("비동기 스레드 풀 초기화 완료: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 비동기 작업 중 발생한 예외 처리
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, objects) -> {
            log.error("비동기 작업 중 예외 발생: method={}, params={}, error={}",
                    method.getName(), objects, throwable.getMessage(), throwable);

            // 향후 확장 가능 영역:
            // - 알림 시스템 장애 알림
            // - 재시도 로직 적용
            // - 외부 모니터링 시스템 연동
        };
    }
}