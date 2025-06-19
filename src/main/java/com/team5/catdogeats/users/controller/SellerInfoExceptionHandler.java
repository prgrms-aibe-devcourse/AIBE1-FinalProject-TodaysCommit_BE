package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

@Slf4j
@RestControllerAdvice(basePackages = "com.team5.catdogeats.users.controller")
public class SellerInfoExceptionHandler {

    /**
     * JPA 엔티티 없음 예외 처리 (Spring Data JPA 표준)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("엔티티 없음 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.USER_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.USER_NOT_FOUND));
    }

    /**
     * 데이터 무결성 위반 예외 처리 (Spring Data 표준)
     * - 사업자 등록번호 중복 등
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("데이터 무결성 위반 - Message: {}", e.getMessage());

        // 사업자 등록번호 중복인지 확인
        if (e.getMessage() != null && e.getMessage().contains("사업자 등록번호")) {
            return ResponseEntity.status(ResponseCode.BUSINESS_NUMBER_DUPLICATE.getStatus())
                    .body(ApiResponse.error(ResponseCode.BUSINESS_NUMBER_DUPLICATE));
        }

        // 일반적인 데이터 무결성 위반
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "데이터 무결성 위반입니다"));
    }

    /**
     * Spring Security 접근 권한 없음 예외 처리
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException e) {
        log.warn("접근 권한 없음 - Message: {}", e.getMessage());

        // 판매자 권한 관련인지 확인
        if (e.getMessage() != null && e.getMessage().contains("판매자")) {
            return ResponseEntity.status(ResponseCode.SELLER_ACCESS_DENIED.getStatus())
                    .body(ApiResponse.error(ResponseCode.SELLER_ACCESS_DENIED));
        }

        // 일반적인 접근 권한 없음
        return ResponseEntity.status(ResponseCode.ACCESS_DENIED.getStatus())
                .body(ApiResponse.error(ResponseCode.ACCESS_DENIED));
    }

    /**
     * IllegalArgumentException 처리 (Java 표준)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 - Message: {}", e.getMessage());

        // 운영시간 관련 예외인지 확인
        if (e.getMessage() != null && e.getMessage().contains("운영")) {
            return ResponseEntity.status(ResponseCode.INVALID_OPERATING_HOURS.getStatus())
                    .body(ApiResponse.error(ResponseCode.INVALID_OPERATING_HOURS, e.getMessage()));
        }

        // 일반적인 잘못된 입력
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