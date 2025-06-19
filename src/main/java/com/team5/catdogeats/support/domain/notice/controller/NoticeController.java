package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.exception.NoticeNotFoundException;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/notices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notice (Public)", description = "공지사항 공개 API - 모든 사용자 접근 가능")
public class NoticeController {

    private final NoticeService noticeService;

    // ========== 공지사항 목록 조회 (공통 기능) ==========
    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = "모든 사용자(판매자, 구매자, 관리자)가 공지사항 목록을 조회할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<NoticeListResponseDTO>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        log.info("공지사항 목록 조회 요청 - page: {}, size: {}, search: {}", page, size, search);

        NoticeListResponseDTO response = noticeService.getNotices(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 공지사항 상세 조회 (공통 기능) ==========
    @GetMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "모든 사용자(판매자, 구매자, 관리자)가 공지사항 상세 내용을 조회할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> getNotice(@PathVariable UUID noticeId) {
        log.info("공지사항 상세 조회 요청 - ID: {}", noticeId);

        NoticeResponseDTO response = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 파일 다운로드 (공통 기능) ==========
    @GetMapping("/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 다운로드",
            description = "모든 사용자가 공지사항의 첨부 파일을 다운로드할 수 있습니다."
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {

        log.info("파일 다운로드 요청 - 파일 ID: {}", fileId);

        try {
            Resource resource = noticeService.downloadFile(fileId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("파일 다운로드 실패 - 파일 ID: {}", fileId);
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 예외 처리 ==========
    @ExceptionHandler(NoticeNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoticeNotFoundException(NoticeNotFoundException ex) {
        log.error("Notice not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        log.error("Validation error: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(false)
                        .message("입력값 검증 오류")
                        .data(errors)
                        .timestamp(ZonedDateTime.now().toString())
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneralException(Exception ex) {
        log.error("Unexpected error: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("서버 내부 오류가 발생했습니다."));
    }

    // ========== 공통 응답 DTO ==========
    @lombok.Getter
    @lombok.Builder
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private String timestamp;

        public static <T> ApiResponse<T> success(T data) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message("성공")
                    .data(data)
                    .timestamp(ZonedDateTime.now().toString())
                    .build();
        }

        public static <T> ApiResponse<T> success(T data, String message) {
            return ApiResponse.<T>builder()
                    .success(true)
                    .message(message)
                    .data(data)
                    .timestamp(ZonedDateTime.now().toString())
                    .build();
        }

        public static <T> ApiResponse<T> error(String message) {
            return ApiResponse.<T>builder()
                    .success(false)
                    .message(message)
                    .data(null)
                    .timestamp(ZonedDateTime.now().toString())
                    .build();
        }
    }
}