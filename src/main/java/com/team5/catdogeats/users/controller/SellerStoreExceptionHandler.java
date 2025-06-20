package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 스토어 관련 예외 처리 핸들러
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.team5.catdogeats.users.controller")
public class SellerStoreExceptionHandler {

    /**
     * 판매자를 찾을 수 없는 경우 예외 처리
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("판매자 스토어 조회 실패 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.SELLER_STORE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.SELLER_STORE_NOT_FOUND));
    }

    /**
     * 잘못된 파라미터 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 파라미터 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * 기타 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception e) {
        log.error("판매자 스토어 조회 중 예상치 못한 오류", e);
        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
    }
}