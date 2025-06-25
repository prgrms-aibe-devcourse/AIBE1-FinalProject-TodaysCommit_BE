package com.team5.catdogeats.users.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice(assignableTypes = SellerInfoController.class)
public class SellerInfoExceptionHandler {


    /**
     * 사용자 조회 실패
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


        // 벤더명 중복인지 확인
        if (e.getMessage() != null && e.getMessage().contains("상점명")) {
            return ResponseEntity.status(ResponseCode.VENDOR_NAME_DUPLICATE.getStatus())
                    .body(ApiResponse.error(ResponseCode.VENDOR_NAME_DUPLICATE));
        }

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
     * Bean Validation 예외 처리 (@Valid 어노테이션 검증 실패)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException e) {
        log.warn("입력값 검증 실패 - 필드 오류 개수: {}", e.getBindingResult().getFieldErrorCount());

        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String errorMessage = fieldErrors.stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("검증 실패 상세 - {}", errorMessage);

        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, errorMessage));
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
     * 이미지 업로드 크기 초과 예외 처리
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        log.warn("파일 업로드 크기 초과 - Message: {}", e.getMessage());
        return ResponseEntity.status(ResponseCode.INVALID_INPUT_VALUE.getStatus())
                .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "이미지 크기가 너무 큽니다. 최대 10MB까지 업로드 가능합니다."));
    }

    /**
     * 이미지 업로드 관련 I/O 예외 처리
     */
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiResponse<Object>> handleIOException(java.io.IOException e) {
        log.error("이미지 처리 중 I/O 오류", e);
        return ResponseEntity.status(ResponseCode.INTERNAL_SERVER_ERROR.getStatus())
                .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR, "이미지 처리 중 오류가 발생했습니다."));
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