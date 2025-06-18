package com.team5.catdogeats.auth.filter;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String REFRESH_TOKEN_PATH = "/v1/auth/refresh";
    private final JwtUtils jwtUtils;

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
            Claims claims = jwtUtils.parseToken(token);
            String providerId = claims.getSubject();
            String authorities = (String) claims.get("authorities");
            String provider = (String) claims.get("provider");

            List<SimpleGrantedAuthority> grantedAuthorities = Arrays.stream(authorities.split(","))
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            UserPrincipal userPrincipal = new UserPrincipal(providerId, provider);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userPrincipal, token, grantedAuthorities);

            //이 인증 객체를 시큐리티 컨텍스트에 등록하면, 이후 컨트롤러 등에서 @AuthenticationPrincipal을 통해 유저 정보를 가져올 수 있음.
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
