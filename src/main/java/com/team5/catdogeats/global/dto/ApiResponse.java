package com.team5.catdogeats.global.dto;

import com.team5.catdogeats.global.enums.ResponseCode;
import org.springframework.validation.FieldError;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 전역 API 응답 포맷
 * 모든 REST API 응답에서 사용하는 공통 응답 구조
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        ZonedDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    // === ResponseCode 기반 새로운 팩토리 메서드들 ===

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return new ApiResponse<>(
                true,
                responseCode.getMessage(),
                data,
                ZonedDateTime.now(),
                null,
                null
        );
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> success(ResponseCode responseCode) {
        return new ApiResponse<>(
                true,
                responseCode.getMessage(),
                null,
                ZonedDateTime.now(),
                null,
                null
        );
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(
                false,
                responseCode.getMessage(),
                null,
                ZonedDateTime.now(),
                null,
                null
        );
    }

    /**
     * 실패 응답 생성 (커스텀 메시지)
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String customMessage) {
        return new ApiResponse<>(
                false,
                customMessage,
                null,
                ZonedDateTime.now(),
                null,
                null
        );
    }

    /**
     * 실패 응답 생성 (path)
     */
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String path, List<FieldError> errors) {
        return new ApiResponse<>(
                false,
                responseCode.getMessage(),
                null,
                ZonedDateTime.now(),
                path,
                errors
        );
    }

}