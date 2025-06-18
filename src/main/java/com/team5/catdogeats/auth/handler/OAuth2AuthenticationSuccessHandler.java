package com.team5.catdogeats.auth.handler;

import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.global.config.CookieProperties;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CookieUtils cookieUtils;
    private final JwtService jwtService;
    private final CookieProperties cookieProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        String token = jwtService.createAccessToken(authentication);

        ResponseCookie cookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), token);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Set-Cookie", cookie.toString());
        response.sendRedirect("/");
    }
}
