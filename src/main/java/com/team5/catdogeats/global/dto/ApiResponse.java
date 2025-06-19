package com.team5.catdogeats.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.team5.catdogeats.global.enums.ResponseCode;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 전역 API 응답 포맷
 * 모든 REST API 응답에서 사용하는 공통 응답 구조
 */
@Getter
public class ApiResponse<T> {

    private final boolean success; // 성공 여부
    private final int status; // HTTP 상태 코드
    private final String code; // ResponseCode의 이름 (e.g., "SUCCESS", "USER_NOT_FOUND")
    private final String message; // 응답 메시지
    private final LocalDateTime timestamp; // 응답 시간

    @JsonInclude(JsonInclude.Include.NON_NULL) // data가 null이면 응답 JSON에서 제외
    private final T data; // 실제 응답 데이터

    // 성공 시 (데이터 포함)
    private ApiResponse(ResponseCode responseCode, T data) {
        this.success = true;
        this.status = responseCode.getStatus().value();
        this.code = responseCode.name();
        this.message = responseCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    // 성공 시 (데이터 없음)
    private ApiResponse(ResponseCode responseCode) {
        this.success = true;
        this.status = responseCode.getStatus().value();
        this.code = responseCode.name();
        this.message = responseCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.data = null;
    }

    // 성공 시 (커스텀 메시지)
    private ApiResponse(ResponseCode responseCode, String customMessage, T data) {
        this.success = true;
        this.status = responseCode.getStatus().value();
        this.code = responseCode.name();
        this.message = customMessage;
        this.timestamp = LocalDateTime.now();
        this.data = data;
    }

    // 실패 시 (기본 메시지)
    private ApiResponse(ResponseCode responseCode, boolean success) {
        this.success = success;
        this.status = responseCode.getStatus().value();
        this.code = responseCode.name();
        this.message = responseCode.getMessage();
        this.timestamp = LocalDateTime.now();
        this.data = null;
    }

    // 실패 시 (커스텀 메시지)
    private ApiResponse(ResponseCode responseCode, String customMessage, boolean success) {
        this.success = success;
        this.status = responseCode.getStatus().value();
        this.code = responseCode.name();
        this.message = customMessage;
        this.timestamp = LocalDateTime.now();
        this.data = null;
    }

    // === 정적 팩토리 메서드 ===

    // 성공 (데이터 포함)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, data);
    }

    // 성공 (데이터 없음)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(ResponseCode.SUCCESS);
    }

    // 성공 (특정 ResponseCode 사용, 데이터 포함)
    public static <T> ApiResponse<T> success(ResponseCode responseCode, T data) {
        return new ApiResponse<>(responseCode, data);
    }

    // 성공 (특정 ResponseCode 사용, 데이터 없음)
    public static <T> ApiResponse<T> success(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode);
    }

    // 성공 (커스텀 메시지 사용, 데이터 포함)
    public static <T> ApiResponse<T> success(ResponseCode responseCode, String message, T data) {
        return new ApiResponse<>(responseCode, message, data);
    }

    // 성공 (커스텀 메시지 사용, 데이터 없음)
    public static <T> ApiResponse<T> success(ResponseCode responseCode, String message) {
        return new ApiResponse<>(responseCode, message, null);
    }

    // 성공 (CREATED 상태 코드 사용, 데이터 포함)
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(ResponseCode.CREATED, data);
    }

    // 실패 (기본 메시지 사용)
    public static <T> ApiResponse<T> error(ResponseCode responseCode) {
        return new ApiResponse<>(responseCode, false);
    }

    // 실패 (커스텀 메시지 사용)
    public static <T> ApiResponse<T> error(ResponseCode responseCode, String message) {
        return new ApiResponse<>(responseCode, message, false);
    }

    // === 기존 호환성을 위한 메서드들 (deprecated 처리 예정) ===

    /**
     * @deprecated ResponseCode를 사용하는 방식으로 변경해주세요
     */
    @Deprecated
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(ResponseCode.SUCCESS, message, data);
    }

    /**
     * @deprecated ResponseCode를 사용하는 방식으로 변경해주세요
     */
    @Deprecated
    public static <T> ApiResponse<T> error(String message, String path, java.util.List<FieldError> errors) {
        return new ApiResponse<>(ResponseCode.INVALID_INPUT_VALUE, message, false);
    }

    /**
     * @deprecated ResponseCode를 사용하는 방식으로 변경해주세요
     */
    @Deprecated
    public static <T> ApiResponse<T> error(String message, String path) {
        return new ApiResponse<>(ResponseCode.INTERNAL_SERVER_ERROR, message, false);
    }

    /**
     * 필드 에러 정보 (기존 호환성 유지)
     */
    public record FieldError(
            String field,
            String message
    ) {}
}