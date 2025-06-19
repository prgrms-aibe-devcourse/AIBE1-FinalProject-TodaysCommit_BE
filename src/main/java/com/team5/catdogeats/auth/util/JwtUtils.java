package com.team5.catdogeats.auth.util;

import com.team5.catdogeats.global.config.JwtConfig;
import com.team5.catdogeats.global.exception.InvalidTokenException;
import com.team5.catdogeats.global.exception.TokenErrorException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {
    private final JwtConfig jwtConfig;

    public Claims parseToken(String token) {
        try {

            return Jwts.parser()
                    .verifyWith(jwtConfig.secretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (TokenErrorException e) {
            log.error("parse token error: {}", e.getMessage());
            throw new JwtException("parse token error");
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(jwtConfig.secretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (Exception e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        }
        return false;
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            Date expirationDate = claims.getExpiration();
            ZonedDateTime expiration = expirationDate.toInstant().atZone(ZoneOffset.UTC);
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            return expiration.isBefore(now);
        } catch (ExpiredJwtException e) {
            return true;
        } catch (JwtException e) {
            throw new InvalidTokenException();
        }
    }


    public String extractToken(HttpServletRequest request) {

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookie.getName().equals("token")) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}

