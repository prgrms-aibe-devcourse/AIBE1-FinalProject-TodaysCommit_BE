package com.team5.catdogeats.global.config;

import com.team5.catdogeats.auth.handler.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true
)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;

    @Bean
    @Order(value = 1)
    public SecurityFilterChain securityFilterChainAdmin(HttpSecurity http) {
        try {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .securityMatcher("/v1/admin/**")
                    .csrf(AbstractHttpConfigurer::disable) // CSRF 비활성화
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/v1/admin/login").permitAll()
                            .requestMatchers("/v1/admin/**").hasRole("ADMIN")
                            .requestMatchers("/v1/sellers/info").permitAll()
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
                    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                    .csrf(AbstractHttpConfigurer::disable) // CSRF 완전 비활성화 (중복 제거)
                    .sessionManagement(session
                            -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize
                            -> authorize
                            .requestMatchers("/").permitAll()
                            .requestMatchers("/index.html").permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .requestMatchers("/v1/sellers/info").permitAll() // PATCH 요청도 포함
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
                            // OPTIONS 요청 허용 (preflight 요청)
                            .requestMatchers("OPTIONS", "/**").permitAll()
                            .requestMatchers("/v1/users/**").hasAnyRole("BUYER", "SELLER")
                            .requestMatchers("/v1/sellers/**").hasRole("SELLER")
                            .requestMatchers("/v1/buyers/**").hasRole("BUYER")
                            .anyRequest().authenticated())

                    .oauth2Login(oauth2 -> oauth2
                            .successHandler(oAuth2AuthenticationSuccessHandler)
                            .userInfoEndpoint(userInfo -> userInfo
                                    .userService(customOAuth2UserService)
                            )
                    )
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(logout -> logout
                            .logoutUrl("/logout")
                            .permitAll()
                    );
            return http.build();
        } catch (Exception e) {
            log.error("User SecurityFilterChain 설정 중 예외 발생: ", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경에서는 모든 Origin 허용, 프로덕션에서는 특정 도메인만 허용하도록 수정 필요
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 모든 HTTP 메서드 허용 (GET, POST, PUT, PATCH, DELETE, OPTIONS 등)
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));

        // credentials 허용 (쿠키, Authorization 헤더 등)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        // 클라이언트에서 접근할 수 있는 응답 헤더 설정
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}