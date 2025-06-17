package com.team5.catdogeats.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true
)
@Slf4j
public class SecurityConfig {

    @Bean
    @Order(value = 1)
    public SecurityFilterChain securityFilterChainAdmin(HttpSecurity http) {
        try {
            http
                    .securityMatcher("/v1/admin/**") // 관리자 체인 설정 지정
                    .csrf(AbstractHttpConfigurer::disable) // 개발이 끝나면 반드시 활성화 시킬것!
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/v1/admin/login").permitAll()
                            .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated());
            return http.build();
        } catch (Exception e) {
            log.error("Admin SecurityFilterChain 설정 중 예외 발생: ", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    @Order(value = 2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .csrf(csrf -> csrf
                            .ignoringRequestMatchers("/swagger-ui/**", "/v3/api-docs/**"))
                    .sessionManagement(session
                            -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize
                            -> authorize
                            .requestMatchers("/").permitAll()
                            .requestMatchers("/index.html").permitAll() // 개발할때만 사용 로그인 페이지
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .requestMatchers("/oauth2/authorization/google/**").permitAll()
                            .requestMatchers("/oauth2/authorization/kakao/**").permitAll()
                            .requestMatchers("/oauth2/authorization/naver/**").permitAll()
                            .requestMatchers("/login/oauth2/code/google/**").permitAll()
                            .requestMatchers("/login/oauth2/code/naver/**").permitAll()
                            .requestMatchers("/login/oauth2/code/kakao/**").permitAll()
                            .requestMatchers("/v1/notices").permitAll()
                            .requestMatchers("/v1/faqs").permitAll()
                            .requestMatchers("/v1/login").permitAll()
                            .requestMatchers("/v1/buyers/products/list").permitAll()
                            .requestMatchers("/v1/buyers/products/{product-number}").permitAll()
                            .requestMatchers("/v1/buyers/reviews/{product-id}/list").permitAll()
                            .requestMatchers("/v1/buyers/reviews/{product-number}").permitAll()
                            .requestMatchers("/v1/users/**").hasAnyRole("BUYER", "SELLER")
                            .requestMatchers("/v1/sellers/**").hasRole("SELLER")
                            .requestMatchers("/v1/buyers/**").hasRole("BUYER")
                            .anyRequest().authenticated());
            return http.build();
        } catch (Exception e) {
            log.error("User SecurityFilterChain 설정 중 예외 발생: ", e);
            throw new RuntimeException(e);
        }
    }
}
