package com.team5.catdogeats.auth.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import org.springframework.security.core.Authentication;

public interface JwtService {
    String createAccessToken(Authentication authentication);
    Authentication getAuthentication(UserPrincipal userPrincipal);
}
