package com.team5.catdogeats.auth.handler;

import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.global.exception.TokenErrorException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            String url = request.getContextPath() + "/";
            ResponseCookie cookie = cookieUtils.createCookie("token", 0, null);
            ResponseCookie refreshIdCookie = cookieUtils.createCookie("refreshTokenId", 0, null);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Set-Cookie", cookie.toString());
            response.addHeader("Set-Cookie", refreshIdCookie.toString());
            response.sendRedirect(url);
        } catch (TokenErrorException e) {
            log.error("Error during logout: {}", e.getMessage());
            response.sendRedirect("/?logout-error=true");
        }
    }
}
