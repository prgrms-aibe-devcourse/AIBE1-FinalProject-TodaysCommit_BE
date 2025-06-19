package com.team5.catdogeats.global.exception;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.dto.ApiResponse.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException ex, HttpServletRequest request) {
        ApiResponse<Void> errorResponse = ApiResponse.error(ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    /**
     * @Valid 실패 (DTO 유효성 검증 실패) 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex,
                                                                       HttpServletRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiResponse<Void> errorResponse = ApiResponse.error("유효성 검사에 실패했습니다.", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 일반적인 바인딩 예외 처리 (예: 쿼리 파라미터, 폼 필드 등)
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<Void>> handleBindException(BindException ex,
                                                                 HttpServletRequest request) {
        List<FieldError> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .collect(Collectors.toList());

        ApiResponse<Void> errorResponse = ApiResponse.error("바인딩 오류가 발생했습니다.", request.getRequestURI(), errors);
        return ResponseEntity.badRequest().body(errorResponse);
    }

    /**
     * 알 수 없는 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        ApiResponse<Void> errorResponse = ApiResponse.error("서버 내부 오류가 발생했습니다.", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
