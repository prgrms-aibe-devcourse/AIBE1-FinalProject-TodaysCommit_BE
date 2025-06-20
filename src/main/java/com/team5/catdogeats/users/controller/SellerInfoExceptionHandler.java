package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.InvalidOperatingHoursException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import lombok.extern.slf4j.Slf4j;
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
    public ResponseEntity<ApiResponse<Object>> handleUserNotFound(UserNotFoundException e) {
        log.warn("사용자 없음 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.USER_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.USER_NOT_FOUND));
    }

    /**
     * 사업자 등록번호 중복 예외 처리
     */
    @ExceptionHandler(BusinessNumberDuplicateException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessNumberDuplicate(BusinessNumberDuplicateException e) {
        log.warn("사업자 등록번호 중복 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.BUSINESS_NUMBER_DUPLICATE.getStatus())
                .body(ApiResponse.error(ResponseCode.BUSINESS_NUMBER_DUPLICATE));
    }

    /**
     * 운영시간 잘못된 입력 예외 처리
     */
    @ExceptionHandler(InvalidOperatingHoursException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidOperatingHours(InvalidOperatingHoursException e) {
        log.warn("운영시간 유효성 검증 실패 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_OPERATING_HOURS.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_OPERATING_HOURS, e.getMessage()));
    }

    /**
     * 커스텀 판매자 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(SellerAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSellerAccessDenied(SellerAccessDeniedException e) {
        log.warn("판매자 접근 권한 없음 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.SELLER_ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ResponseCode.SELLER_ACCESS_DENIED));
    }

    /**
     * Spring Security 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleSpringAccessDenied(
            org.springframework.security.access.AccessDeniedException e) {
        log.warn("Spring Security 접근 권한 없음 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
    }

    /**
     * IllegalArgumentException 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * 기타 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception e) {
        log.error("예상치 못한 오류", e);
        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
    }
}