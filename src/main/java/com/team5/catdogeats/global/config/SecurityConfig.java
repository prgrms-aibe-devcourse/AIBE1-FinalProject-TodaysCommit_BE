package com.team5.catdogeats.global.config;

import com.team5.catdogeats.auth.filter.JwtAuthenticationFilter;
import com.team5.catdogeats.auth.filter.PreventDuplicateLoginFilter;
import com.team5.catdogeats.auth.handler.CustomLogoutSuccessHandler;
import com.team5.catdogeats.auth.handler.OAuth2AuthenticationFailureHandler;
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
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PreventDuplicateLoginFilter preventDuplicateLoginFilter;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    @Bean
    @Order(value = 1)
    public SecurityFilterChain securityFilterChainAdmin(HttpSecurity http) {
        try {
            http
                    .securityMatcher("/v1/admin/**") // 관리자 체인 설정 지정
                    .csrf(AbstractHttpConfigurer::disable) // 개발이 끝나면 반드시 활성화 시킬것!
                    .sessionManagement(session ->
                            session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                                    .sessionFixation().migrateSession()
                                    .maximumSessions(1)
                                    .maxSessionsPreventsLogin(false))
                    .authorizeHttpRequests(authorize -> authorize
                            .requestMatchers("/v1/admin/login").permitAll()                           // 로그인 페이지
                            .requestMatchers("/v1/admin/verify").permitAll()                          // 계정 인증 페이지
                            .requestMatchers("/v1/admin/resend-code").permitAll()                     // 인증코드 재발송
                            .requestMatchers("/v1/admin/invite").hasAuthority("ADMIN")                // 초대 기능은 ADMIN 부서만
                            .requestMatchers("/v1/admin/account-management").hasAuthority("ADMIN")    // 계정 관리는 ADMIN 부서만
                            .requestMatchers("/v1/admin/accounts/**").hasAuthority("ADMIN")           // 계정 관리 API는 ADMIN 부서만
                            .requestMatchers("/v1/admin/**").authenticated()                          // 나머지는 세션 인증 필요
                            .anyRequest().authenticated())
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)                         // Spring Security 기본 로그인 비활성화
                    .logout(logout -> logout
                            .logoutUrl("/v1/admin/logout")
                            .logoutSuccessUrl("/v1/admin/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID")
                            .permitAll())
                    .securityContext(securityContext ->
                            securityContext.requireExplicitSave(false))                // 🔧 SecurityContext 자동 저장 활성화
                    .exceptionHandling(exceptions -> exceptions
                            .authenticationEntryPoint((request, response, authException) -> {
                                // 인증되지 않은 사용자를 로그인 페이지로 리다이렉트
                                if (request.getRequestURI().startsWith("/v1/admin/")) {
                                    response.sendRedirect("/v1/admin/login");
                                }
                            })
                            .accessDeniedHandler((request, response, accessDeniedException) -> {
                                // 권한이 없는 사용자를 대시보드로 리다이렉트
                                if (request.getRequestURI().startsWith("/v1/admin/")) {
                                    response.sendRedirect("/v1/admin/dashboard?error=access_denied");
                                }
                            }));
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
                    .sessionManagement(session
                            -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(authorize
                            -> authorize
                            .requestMatchers("/").permitAll()
                            .requestMatchers("/index.html").permitAll() // 개발할때만 사용 로그인 페이지
                            .requestMatchers("/withdraw").permitAll()
                            .requestMatchers("/error").permitAll()
                            .requestMatchers("/.well-known/**").permitAll()
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            .requestMatchers("/oauth2/authorization/google/**").permitAll()
                            .requestMatchers("/oauth2/authorization/kakao/**").permitAll()
                            .requestMatchers("/oauth2/authorization/naver/**").permitAll()
                            .requestMatchers("/login/oauth2/code/google/**").permitAll()
                            .requestMatchers("/login/oauth2/code/naver/**").permitAll()
                            .requestMatchers("/login/oauth2/code/kakao/**").permitAll()
                            .requestMatchers("/v1/auth/refresh").permitAll()
                            .requestMatchers("/v1/notices").permitAll()
                            .requestMatchers("/v1/faqs").permitAll()
                            .requestMatchers("/v1/buyers/products/list").permitAll()
                            .requestMatchers("/v1/buyers/products/{product-number}").permitAll()
                            .requestMatchers("/v1/buyers/reviews/{product-id}/list").permitAll()
                            .requestMatchers("/v1/buyers/reviews/{product-number}").permitAll()
                            .requestMatchers("/v1/users/{vendor-name}").permitAll()
                            .requestMatchers("/withdraw").permitAll()
                            .requestMatchers("/v1/users/**").hasAnyRole("BUYER", "SELLER")
                            .requestMatchers("/v1/sellers/**").hasRole("SELLER")
                            .requestMatchers("/v1/buyers/**").hasRole("BUYER")
                            .requestMatchers("/v1/auth/role").hasRole("TEMP")
                            .anyRequest().authenticated())

                    .oauth2Login(oauth2 -> oauth2
                            .successHandler(oAuth2AuthenticationSuccessHandler)
                            .failureHandler(oAuth2AuthenticationFailureHandler)
                            .userInfoEndpoint(userInfo -> userInfo
                                    .userService(customOAuth2UserService) // 여기에 커스텀 서비스 주입
                            )
                    )
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)

// .exceptionHandling(exception ->
// exception.authenticationEntryPoint(new OAuth2AuthenticationEntryPointHandler()))
                    .logout(logout -> logout
                            .logoutUrl("/v1/auth/logout")
                            .logoutSuccessHandler(customLogoutSuccessHandler)
                            .invalidateHttpSession(true)
                            .deleteCookies("token")
                            .permitAll()
                    )
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(preventDuplicateLoginFilter, OAuth2AuthorizationRequestRedirectFilter.class);
            return http.build();
        } catch (Exception e) {
            log.error("User SecurityFilterChain 설정 중 예외 발생: ", e);
            throw new RuntimeException(e);
        }
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
