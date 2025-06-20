package com.team5.catdogeats.auth.filter;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.util.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String REFRESH_TOKEN_PATH = "/v1/auth/refresh";
    private final JwtUtils jwtUtils;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.equals(REFRESH_TOKEN_PATH)) {
            logger.debug("Skipping JWT validation for refresh token endpoint");
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtUtils.extractToken(request);

        if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
            try {
                Claims claims = jwtUtils.parseToken(token);
                String providerId = claims.getSubject();
                String provider = (String) claims.get("provider");
                String authorities = (String) claims.get("authorities");

                List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities.split(","))
                        .map(SimpleGrantedAuthority::new)
                        .toList();

                // UserPrincipal 생성
                UserPrincipal userPrincipal = new UserPrincipal(provider, providerId);

//                Authentication authentication = jwtService.getAuthentication(userPrincipal);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userPrincipal, token, grantedAuthorities);

                // SecurityContext에 Authentication 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("Authentication set for providerId: {}, authorities: {}",
                        providerId, authorities);

            } catch (Exception e) {
                log.error("Error setting authentication from token", e);
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
