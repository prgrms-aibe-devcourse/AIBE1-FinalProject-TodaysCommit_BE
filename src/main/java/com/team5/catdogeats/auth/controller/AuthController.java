package com.team5.catdogeats.auth.controller;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.global.config.CookieProperties;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.dto.ModifyRoleRequestDTO;
import com.team5.catdogeats.users.service.ModifyUserRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
            }


            if (!jwtUtils.isTokenExpired(token)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ResponseCode.INVALID_TOKEN));
            }

            UUID id = UUID.fromString(refreshTokenId);
            RotateTokenDTO dto = rotateRefreshTokenService.RotateRefreshToken(id);
            ResponseCookie accessCookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), dto.newAccessToken());
            ResponseCookie refreshIdCookie = cookieUtils.createCookie("refreshTokenId", cookieProperties.getMaxAge(), dto.newRefreshToken().toString());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshIdCookie.toString())
                    .body(ApiResponse.success(ResponseCode.SUCCESS, dto));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping("/role")
    public ResponseEntity<ApiResponse<String>> modifyRole(@AuthenticationPrincipal UserPrincipal userPrincipal,
                                                          @RequestBody @Valid ModifyRoleRequestDTO roleRequestDTO) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        try {
            // 역할 변경 및 새로운 Authentication 객체 반환
            Authentication newAuthentication = modifyUserRoleService.modifyUserRole(userPrincipal, roleRequestDTO);

            // 업데이트된 Authentication으로 새 토큰 생성
            String newToken = jwtService.createAccessToken(newAuthentication);

            // 새 토큰으로 쿠키 생성
            ResponseCookie accessCookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), newToken);

            log.debug("New token created for user with updated role: {}", roleRequestDTO.role());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .body(ApiResponse.success(ResponseCode.SUCCESS, "Role updated successfully"));

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
        } catch (Exception e) {
            log.error("Error modifying user role", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(ResponseCode.UNAUTHORIZED));
        }

        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
    }
}
