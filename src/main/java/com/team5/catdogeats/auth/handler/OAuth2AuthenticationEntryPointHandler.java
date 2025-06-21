//package com.team5.catdogeats.auth.handler;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.AnonymousAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.web.AuthenticationEntryPoint;
//
//import java.io.IOException;
//
//@Slf4j
//public class OAuth2AuthenticationEntryPointHandler implements AuthenticationEntryPoint {
//
//    @Override
//    public void commence(HttpServletRequest request, HttpServletResponse response,
//                         AuthenticationException authException) throws IOException {
//
//        log.info("=== OAuth2AuthenticationEntryPointHandler 호출됨 ===");
//        log.info("요청 URI: {}", request.getRequestURI());
//        log.info("예외 타입: {}", authException.getClass().getSimpleName());
//        log.info("예외 메시지: {}", authException.getMessage());
//
//        // 이미 인증된 사용자인지 확인
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//        if (auth != null && auth.isAuthenticated() &&
//                !(auth instanceof AnonymousAuthenticationToken)) {
//            log.info("이미 인증된 사용자 - 홈페이지로 리다이렉트");
//            response.sendRedirect("/");
//            return;
//        }
//
//        // 미인증 사용자는 기본 동작 수행
//        log.info("미인증 사용자 - 401 Unauthorized 반환");
//        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
//    }
//}