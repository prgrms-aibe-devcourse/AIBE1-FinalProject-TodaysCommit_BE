package com.team5.catdogeats.auth.util;

import com.team5.catdogeats.global.config.CookieProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtils {
    private final CookieProperties cookieProperties;

    public ResponseCookie createCookie(String name, String value) {
        return ResponseCookie.from(name, value)
                .sameSite(cookieProperties.getSameSite())
                .secure(cookieProperties.isSecure())
                .httpOnly(cookieProperties.isHttpOnly())
                .maxAge(cookieProperties.getMaxAge())
                .domain(cookieProperties.getDomain())
                .path(cookieProperties.getPath())
                .build();
    }
}
