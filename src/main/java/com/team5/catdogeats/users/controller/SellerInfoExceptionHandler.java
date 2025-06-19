package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.InvalidOperatingHoursException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(basePackages = "com.team5.catdogeats.users.controller")
public class SellerInfoExceptionHandler {

    /**
     * 사용자 없음 예외 처리
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(
            UserNotFoundException e, HttpServletRequest request) {

        log.warn("사용자 없음 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * 사업자 등록번호 중복 예외 처리
     */
    @ExceptionHandler(BusinessNumberDuplicateException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessNumberDuplicate(
            BusinessNumberDuplicateException e, HttpServletRequest request) {

        log.warn("사업자 등록번호 중복 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));
    }

    /**
     *  운영시간 잘못된 입력 예외 처리
     */
    @ExceptionHandler(InvalidOperatingHoursException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOperatingHours(
            InvalidOperatingHoursException e, HttpServletRequest request) {

        log.warn("운영시간 유효성 검증 실패 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));
    }


    /**
     * 커스텀 판매자 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(SellerAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSellerAccessDenied(
            SellerAccessDeniedException e, HttpServletRequest request) {

        log.warn("판매자 접근 권한 없음 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * Spring Security 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException e, HttpServletRequest request) {

        log.warn("Spring Security 접근 권한 없음 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("권한이 없습니다. 판매자만 접근 가능합니다.",
                        request.getRequestURI(), null));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(
            IllegalArgumentException e, HttpServletRequest request) {

        log.warn("잘못된 요청 - URI: {}, Message: {}", request.getRequestURI(), e.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage(), request.getRequestURI(), null));
    }

    /**
     * 기타 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(
            Exception e, HttpServletRequest request) {

        log.error("예상치 못한 오류 - URI: {}", request.getRequestURI(), e);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다.", request.getRequestURI(), null));
    }
}