package com.team5.catdogeats.users.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        LocalDateTime timestamp,
        String path,
        List<FieldError> errors
) {
    // 정적 팩토리 메서드들은 그대로 유지
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

    // FieldError도 Record로 변환
    public record FieldError(
            String field,
            String message
    ) {}
}
