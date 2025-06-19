package com.team5.catdogeats.global.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Toss Payments 설정 구성 클래스
 *
 * application-dev.yml의 toss.payments 설정을 바인딩하고 관리합니다.
 * 기존의 @Value 어노테이션 방식보다 메모리 효율적이고 관리가 용이합니다.
 */
@Configuration
@EnableConfigurationProperties(TossPaymentsConfig.TossPaymentsProperties.class)
@RequiredArgsConstructor
public class TossPaymentsConfig {

    /**
     * Toss Payments 설정 프로퍼티 클래스
     */
    @ConfigurationProperties(prefix = "toss.payments")
    @Getter
    public static class TossPaymentsProperties {

        /**
         * 토스 페이먼츠 클라이언트 키
         */
        private String clientKey;

        /**
         * 토스 페이먼츠 시크릿 키
         */
        private String secretKey;

        /**
         * 결제 성공 시 리디렉션 URL
         */
        private String successUrl;

        /**
         * 결제 실패 시 리디렉션 URL
         */
        private String failUrl;

        /**
         * API 관련 설정
         */
        private Api api = new Api();

        /**
         * 주문 관련 설정
         */
        private Order order = new Order();

        /**
         * API 설정 중첩 클래스
         */
        @Getter
        public static class Api {
            /**
             * 토스 페이먼츠 API 기본 URL
             */
            private String baseUrl;

            /**
             * API 버전
             */
            private String version;

            public void setBaseUrl(String baseUrl) {
                this.baseUrl = baseUrl;
            }

            public void setVersion(String version) {
                this.version = version;
            }
        }

        /**
         * 주문 설정 중첩 클래스
         */
        @Getter
        public static class Order {
            /**
             * 주문번호 접두사
             */
            private String prefix;

            /**
             * 결제 타임아웃 (분 단위)
             */
            private int timeoutMinutes;

            public void setPrefix(String prefix) {
                this.prefix = prefix;
            }

            public void setTimeoutMinutes(int timeoutMinutes) {
                this.timeoutMinutes = timeoutMinutes;
            }
        }

        // Setters for Spring Boot Configuration Properties binding
        public void setClientKey(String clientKey) {
            this.clientKey = clientKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public void setSuccessUrl(String successUrl) {
            this.successUrl = successUrl;
        }

        public void setFailUrl(String failUrl) {
            this.failUrl = failUrl;
        }

        public void setApi(Api api) {
            this.api = api;
        }

        public void setOrder(Order order) {
            this.order = order;
        }
    }
}