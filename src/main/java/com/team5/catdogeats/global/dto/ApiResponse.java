package com.team5.catdogeats.global.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 전역 API 응답 포맷
 * 모든 REST API 응답에서 사용하는 공통 응답 구조
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(
                true,
                message,
                data,
                LocalDateTime.now(),
                null,
                null
        );
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> error(String message, String path, List<FieldError> errors) {
        return new ApiResponse<>(
                false,
                message,
                null,
                LocalDateTime.now(),
                path,
                errors
        );
    }

    /**
     * 단순 에러 응답 생성 (필드 에러 없이)
     */
    public static <T> ApiResponse<T> error(String message, String path) {
        return error(message, path, null);
    }

    /**
     * 필드 에러 정보
     */
    public record FieldError(
            String field,
            String message
    ) {}
}
