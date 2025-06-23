package com.team5.catdogeats.payments.client;

import com.team5.catdogeats.global.config.TossPaymentsConfig;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Toss Payments OpenFeign 클라이언트 설정
 *
 * Toss Payments API 호출 시 필요한 인증 헤더와 공통 설정을 처리합니다.
 * 시크릿 키를 Base64로 인코딩하여 Authorization 헤더에 포함합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class TossPaymentsClientConfig {

    private final TossPaymentsConfig.TossPaymentsProperties tossPaymentsProperties;

    /**
     * Toss Payments API 인증 인터셉터
     *
     * 모든 요청에 다음 헤더들을 자동으로 추가합니다:
     * - Authorization: Basic [Base64 encoded secret key]
     * - Content-Type: application/json
     * - Toss-Payments-Version: API 버전
     *
     * @return RequestInterceptor 인스턴스
     */
    @Bean
    public RequestInterceptor tossPaymentsRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 1. Authorization 헤더 설정 (Basic Authentication)
                String secretKey = tossPaymentsProperties.getSecretKey();
                String credentials = secretKey + ":"; // 시크릿 키 뒤에 콜론 추가
                String encodedCredentials = Base64.getEncoder()
                        .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

                template.header("Authorization", "Basic " + encodedCredentials);

                // 2. Content-Type 헤더 설정
                template.header("Content-Type", "application/json");

                // 3. Toss Payments API 버전 헤더 설정
                String apiVersion = tossPaymentsProperties.getApi().getVersion();
                if (apiVersion != null && !apiVersion.isEmpty()) {
                    template.header("Toss-Payments-Version", apiVersion);
                }

                log.debug("Toss Payments API 요청 헤더 설정 완료: method={}, url={}",
                        template.method(), template.url());
            }
        };
    }
}