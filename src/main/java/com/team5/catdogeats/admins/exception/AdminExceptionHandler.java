package com.team5.catdogeats.admins.exception;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/**
 * 관리자 관련 전역 예외 처리기
 * Spring 표준 예외들을 사용하여 통합 처리합니다.
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.team5.catdogeats.admins.controller")
public class AdminExceptionHandler {

    /**
     * Spring Security 접근 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("접근 권한 없음: {}", e.getMessage());
        return ResponseEntity.status(403)
                .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));
    }

    /**
     * Spring Security 인증 실패 (로그인 필요, 잘못된 비밀번호 등)
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentials(BadCredentialsException e) {
        log.warn("인증 실패: {}", e.getMessage());
        return ResponseEntity.status(401)
                .body(ApiResponse.error(ResponseCode.UNAUTHORIZED, e.getMessage()));
    }

    /**
     * 유효성 검증 실패 (@Valid 어노테이션)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.isEmpty()
                ? "입력값이 올바르지 않습니다."
                : fieldErrors.get(0).getDefaultMessage();

        log.warn("유효성 검증 실패: {}", errorMessage);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, errorMessage));
    }

    /**
     * IllegalArgumentException (비밀번호 불일치, 이메일 중복 등)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 인수: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * IllegalStateException (계정 미활성화, 인증코드 만료 등)
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        log.warn("잘못된 상태: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(ResponseCode.ACCESS_DENIED, e.getMessage()));
    }

    /**
     * 기타 모든 예외 (예상치 못한 서버 오류)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception e) {
        log.error("예상치 못한 서버 오류: ", e);
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."));
    }
}