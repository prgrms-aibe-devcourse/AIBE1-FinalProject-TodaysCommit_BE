package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.service.SellerStoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 스토어 관련 예외 처리 핸들러
 * - Service에서 발생하는 모든 예외를 적절한 HTTP 응답으로 변환
 * - 로깅과 에러 응답 표준화
 */
@Slf4j
@RestControllerAdvice(assignableTypes = SellerStoreController.class)
public class SellerStoreExceptionHandler {

    /**
     * 매자를 찾을 수 없는 경우 예외 처리
     * Service의 findSellerByVendorName()에서 발생
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("판매자 스토어 조회 실패 - Message: {}", e.getMessage());

        // 구체적인 에러 메시지로 응답
        return ResponseEntity.status(ResponseCode.SELLER_STORE_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.SELLER_STORE_NOT_FOUND, e.getMessage()));
    }

    /**
     * 잘못된 파라미터 예외 처리
     * Service의 validateRequestParameters()에서 발생
     * - 페이지 번호 < 1
     * - 페이지 크기 < 1 또는 > 100
     * - 유효하지 않은 필터 값
     * - 빈 판매자명 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("잘못된 요청 파라미터 - Message: {}", e.getMessage());

        // 파라미터별 구체적인 에러 응답
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }


    /**
     * 페이징 관련 오류 처리
     * Pageable 생성이나 처리 중 오류
     */
    @ExceptionHandler(org.springframework.data.mapping.PropertyReferenceException.class)
    public ResponseEntity<ApiResponse<Object>> handlePropertyReferenceException(
            org.springframework.data.mapping.PropertyReferenceException e) {
        log.warn("잘못된 정렬 속성 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE,
                        "잘못된 정렬 조건입니다: " + e.getPropertyName()));
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


    /**
     * Products 도메인 전용 예외
     */
    public static class ProductDataRetrievalException extends RuntimeException {
        public ProductDataRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }


    /**
     * Orders 도메인 전용 예외
     */
    public static class OrderStatsRetrievalException extends RuntimeException {
        public OrderStatsRetrievalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

}