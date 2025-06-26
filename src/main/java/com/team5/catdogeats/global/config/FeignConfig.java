package com.team5.catdogeats.global.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

/**
 * OpenFeign 설정
 * Toss Payments API 통신을 위한 설정입니다.
 */
@Configuration
@EnableFeignClients(basePackages = "com.team5.catdogeats")
public class FeignConfig {
    // OpenFeign 활성화를 위한 설정 클래스
    // 추가적인 설정이 필요할 경우 이곳에 Bean을 정의할 수 있습니다.
}