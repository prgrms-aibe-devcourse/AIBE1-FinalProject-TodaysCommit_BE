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
 * 비동기 처리 설정 (이벤트 기반 아키텍처용)
 * OrderEventListener의 @Async 메서드들이 동작하도록 스레드 풀을 설정합니다.
 * 알림 발송, 로깅 등 메인 플로우에 영향을 주지 않는 작업들을 비동기로 처리합니다.
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업용 스레드 풀 설정
     * 이벤트 리스너의 알림 발송, 감사 로깅 등에 사용됩니다.
     */
    @Bean(name = "taskExecutor")
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 수 (항상 활성 상태로 유지)
        executor.setCorePoolSize(2);

        // 최대 스레드 수 (부하 증가 시 확장)
        executor.setMaxPoolSize(10);

        // 큐 용량 (대기 작업 수)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 (로깅 시 식별 용이)
        executor.setThreadNamePrefix("Event-Async-");

        // 종료 시 완료 대기 시간 (초)
        executor.setAwaitTerminationSeconds(60);

        // 종료 시 진행 중인 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 스레드 풀 초기화
        executor.initialize();

        log.info("비동기 스레드 풀 설정 완료: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 비동기 작업 예외 처리기
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) -> {
            log.error("비동기 작업 실행 중 예외 발생: method={}, error={}",
                    method.getName(), ex.getMessage(), ex);

            // 파라미터 정보 로깅 (디버깅용)
            for (int i = 0; i < params.length; i++) {
                log.debug("비동기 작업 파라미터[{}]: {}", i, params[i]);
            }

            // 중요한 비동기 작업 실패 시 추가 처리 로직
            // 예: 관리자 알림, 모니터링 시스템 연동 등
            if (method.getName().contains("Notification")) {
                log.warn("알림 발송 실패 - 수동 확인 필요: method={}", method.getName());
            }
        };
    }
}