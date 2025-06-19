package com.team5.catdogeats.auth.controller;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.global.config.CookieProperties;
import com.team5.catdogeats.users.domain.dto.ModifyRoleRequestDTO;
import com.team5.catdogeats.users.service.ModifyUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {
    private final JwtUtils jwtUtils;
    private final CookieUtils cookieUtils;
    private final RotateRefreshTokenService rotateRefreshTokenService;
    private final CookieProperties cookieProperties;
    private final ModifyUserRoleService modifyUserRoleService;
    private final JwtService jwtService;

    @PostMapping("/refresh")
    @Operation(summary = "토큰 갱신", description = "리프레시 토큰을 사용하여 액세스 토큰과 리프레시 토큰을 갱신합니다.")
    public ResponseEntity<?> rotateRefreshToken(@CookieValue(value = "refreshTokenId", required = false) String refreshTokenId,
                                                @CookieValue(value = "token", required = false) String token) {
        try {
            if (refreshTokenId == null || token == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
            }


            if (!jwtUtils.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("엑세스 토큰이 만료되지 않았습니다.");
            }

            UUID id = UUID.fromString(refreshTokenId);
            RotateTokenDTO dto = rotateRefreshTokenService.RotateRefreshToken(id);
            ResponseCookie accessCookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), dto.newAccessToken());
            ResponseCookie refreshIdCookie = cookieUtils.createCookie("refreshTokenId", cookieProperties.getMaxAge(), dto.newRefreshToken().toString());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshIdCookie.toString())
                    .body(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/role")
    public ResponseEntity<?> modifyRole(@RequestBody @Valid @AuthenticationPrincipal UserPrincipal userPrincipal, ModifyRoleRequestDTO roleRequestDTO) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        try {
            Authentication authentication = modifyUserRoleService.modifyUserRole(userPrincipal, roleRequestDTO);
            String token = jwtService.createAccessToken(authentication);
            ResponseCookie accessCookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), token);

            return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, accessCookie.toString()).body(token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("잘못된 접근입니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증되지 않은 사용자입니다.");
        }

        return ResponseEntity.ok().build();
    }
}
