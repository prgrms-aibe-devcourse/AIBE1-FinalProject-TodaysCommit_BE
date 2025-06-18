package com.team5.catdogeats.auth.util;

import com.team5.catdogeats.global.config.JwtConfig;
import com.team5.catdogeats.global.exception.TokenErrorException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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
}

