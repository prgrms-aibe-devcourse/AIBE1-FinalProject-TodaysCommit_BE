package com.team5.catdogeats.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 처리 설정
 *
 * Spring의 @Async 어노테이션을 사용한 비동기 메서드 실행을 위한 설정입니다.
 * 주요 용도:
 * - 주문 생성 후 사용자 알림 발송 (OrderEventListener.handleUserNotification)
 * - 외부 서비스 호출로 인한 지연이 메인 플로우에 영향을 주지 않도록 격리
 * - 시스템 성능 최적화 및 사용자 경험 향상
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 비동기 작업용 ThreadPool 설정
     *
     * 알림, 로깅 등 부가적인 작업들을 처리하기 위한 전용 스레드풀입니다.
     * 메인 비즈니스 로직(주문 생성, 결제 처리)과 분리하여 안정성을 확보합니다.
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 기본 스레드 개수 (항상 유지되는 스레드 수)
        executor.setCorePoolSize(2);

        // 최대 스레드 개수 (부하 증가 시 확장 가능한 최대 스레드 수)
        executor.setMaxPoolSize(10);

        // 큐 용량 (대기 중인 작업을 저장할 큐 크기)
        executor.setQueueCapacity(100);

        // 스레드 이름 접두사 (로그 추적 시 구분하기 쉽도록)
        executor.setThreadNamePrefix("CatDogEats-Async-");

        // 유휴 스레드 대기 시간 (60초간 작업이 없으면 스레드 종료)
        executor.setKeepAliveSeconds(60);

        // 애플리케이션 종료 시 작업 완료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);

        // 거부된 작업 처리 정책 설정
        executor.setRejectedExecutionHandler(new CustomRejectedExecutionHandler());

        // ThreadPoolTaskExecutor 초기화
        executor.initialize();

        log.info("비동기 TaskExecutor 초기화 완료: corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
    }

    /**
     * 비동기 작업 중 발생한 예외 처리
     *
     * @Async 메서드에서 발생한 예외들을 중앙에서 처리합니다.
     * 알림 발송 실패 등이 메인 시스템에 영향을 주지 않도록 격리하되,
     * 로그는 남겨서 모니터링이 가능하도록 합니다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            log.error("비동기 작업 실행 중 예외 발생: method={}, params={}, error={}",
                    method.getName(),
                    params != null ? params.length : 0,
                    throwable.getMessage(),
                    throwable);

            // TODO: 심각한 오류의 경우 관리자 알림 발송
            // 예: 슬랙 알림, 이메일 발송 등
        };
    }

    /**
     * 커스텀 거부 작업 처리 핸들러
     *
     * 스레드풀이 포화상태일 때 새로운 작업이 거부되는 경우를 처리합니다.
     * 알림 발송과 같은 부가 기능이므로 작업 손실을 허용하되 로그는 남깁니다.
     */
    private static class CustomRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("비동기 작업이 거부되었습니다. 스레드풀 상태: active={}, poolSize={}, queueSize={}",
                    executor.getActiveCount(),
                    executor.getPoolSize(),
                    executor.getQueue().size());

            // 거부된 작업에 대한 대체 처리 로직
            // 예: 중요하지 않은 알림은 무시, 중요한 작업은 동기 실행 등

            // 현재는 로그만 남기고 작업을 포기
            // 추후 필요에 따라 대체 처리 로직 구현 가능
        }
    }
}