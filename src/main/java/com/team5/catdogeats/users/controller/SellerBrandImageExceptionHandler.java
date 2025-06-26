package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.persistence.EntityNotFoundException;

/**
 * 판매자 브랜드 이미지 관련 예외 처리 핸들러
 * SellerBrandImageController에서 발생하는 예외를 처리
 */
@Slf4j
@RestControllerAdvice(assignableTypes = SellerBrandImageController.class)
public class SellerBrandImageExceptionHandler {

    /**
     * 사용자 또는 판매자 정보를 찾을 수 없는 경우
     * Service의 findUserByPrincipal(), findSellerByUserId()에서 발생
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleEntityNotFound(EntityNotFoundException e) {
        log.warn("사용자/판매자 정보 조회 실패 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.USER_NOT_FOUND.getStatus())
                .body(ApiResponse.error(ResponseCode.USER_NOT_FOUND, e.getMessage()));
    }

    /**
     * 파일 업로드 관련 파라미터 오류 처리
     * Service의 validateImageFile()에서 발생하는 IllegalArgumentException
     * - 파일이 null이거나 비어있음
     * - 파일크기 검증
     * - 지원하지 않는 파일 형식
     * - Content-Type 오류 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("파일 업로드 파라미터 오류 - Message: {}", e.getMessage());

        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
    }

    /**
     * 런타임 예외 처리
     * Service의 uploadNewImage(), deleteBrandImage()에서 발생
     * - "이미지 업로드 중 오류가 발생했습니다."
     * - "이미지 업로드 실패"
     * - "브랜드 이미지 삭제에 실패했습니다."
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(RuntimeException e) {
        log.error("브랜드 이미지 처리 중 런타임 오류 - Message: {}", e.getMessage(), e);

        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
    }


    /**
     * 기타 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneral(Exception e) {
        log.error("브랜드 이미지 처리 중 예상치 못한 오류", e);

        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
    }
}