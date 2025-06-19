package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.exception.NoticeNotFoundException;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/notices")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notice (Admin)", description = "공지사항 관리자 API - 관리자만 접근 가능")
public class NoticeAdminController {

    private final NoticeService noticeService;

    // ========== 공지사항 목록 조회 ==========
    @GetMapping
    @Operation(
            summary = "공지사항 목록 조회",
            description = "관리자 페이지에서 공지사항 목록을 조회합니다."
            )
    public ResponseEntity<ApiResponse<NoticeListResponseDTO>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {

        log.info("[관리자] 공지사항 목록 조회 요청 - page: {}, size: {}, search: {}", page, size, search);

        NoticeListResponseDTO response = noticeService.getNotices(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 공지사항 상세 조회 ==========
    @GetMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "관리자 페이지에서 공지사항 상세 내용을 조회합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> getNotice(@PathVariable UUID noticeId) {
        log.info("[관리자] 공지사항 상세 조회 요청 - ID: {}", noticeId);

        NoticeResponseDTO response = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 공지사항 생성 ==========
    @PostMapping
    @Operation(
            summary = "공지사항 등록",
            description = "관리자가 공지사항을 등록합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> createNotice(
            @Valid @RequestBody NoticeCreateRequestDTO requestDto) {

        log.info("[관리자] 공지사항 생성 요청 - 제목: {}", requestDto.getTitle());

        NoticeResponseDTO response = noticeService.createNotice(requestDto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "공지사항이 성공적으로 생성되었습니다."));
    }

    // ========== 공지사항 수정 ==========
    @PatchMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 수정",
            description = "관리자가 공지사항을 수정합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> updateNotice(
            @PathVariable UUID noticeId,
            @Valid @RequestBody NoticeUpdateRequestDTO requestDto) {

        log.info("[관리자] 공지사항 수정 요청 - ID: {}, 제목: {}", noticeId, requestDto.getTitle());

        requestDto.setId(noticeId);
        NoticeResponseDTO response = noticeService.updateNotice(requestDto);
        return ResponseEntity.ok(ApiResponse.success(response, "공지사항이 성공적으로 수정되었습니다."));
    }

    // ========== 공지사항 삭제 ==========
    @DeleteMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 삭제",
            description = "관리자가 공지사항을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable UUID noticeId) {
        log.info("[관리자] 공지사항 삭제 요청 - ID: {}", noticeId);

        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(ApiResponse.success(null, "공지사항이 성공적으로 삭제되었습니다."));
    }

    // ========== 파일 업로드 ==========
    @PostMapping(value = "/{noticeId}/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "공지사항 첨부파일 업로드",
            description = "관리자가 공지사항의 첨부파일을 업로드합니다."

    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> uploadFile(
            @PathVariable UUID noticeId,
            @RequestParam("file") MultipartFile file) {

        log.info("[관리자] 파일 업로드 요청 - ID: {}, 파일명: {}", noticeId, file.getOriginalFilename());

        // 파일 검증
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("업로드할 파일을 선택해주세요."));
        }

        // 파일 크기 제한 (10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("파일 크기는 10MB를 초과할 수 없습니다."));
        }

        // 허용된 파일 확장자 검사
        String fileName = file.getOriginalFilename();
        if (!isAllowedFileType(fileName)) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("허용되지 않는 파일 형식입니다. (jpg, png, pdf, docx, xlsx 만 가능)"));
        }

        try {
            // Service에서 NoticeResponseDTO 반환 (공지사항 정보)
            NoticeResponseDTO response = noticeService.uploadFile(noticeId, file);
            return ResponseEntity.ok(ApiResponse.success(response, "파일이 성공적으로 업로드되었습니다."));
        } catch (Exception e) {
            log.error("[관리자] 파일 업로드 실패 - ID: {}, 오류: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("파일 업로드 중 오류가 발생했습니다."));
        }
    }

    // ========== 파일 다운로드 ==========
    @GetMapping("/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 다운로드",
            description = "관리자 페이지에서 공지사항의 첨부파일을 다운로드합니다."
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable UUID fileId) {

        log.info("[관리자] 파일 다운로드 요청 - 파일 ID: {}", fileId);

        try {
            // Service에서 fileId로 직접 다운로드
            Resource resource = noticeService.downloadFile(fileId);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("[관리자] 파일 다운로드 실패 - 파일 ID: {}", fileId);
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 헬퍼 메서드 ==========
    private boolean isAllowedFileType(String fileName) {
        if (fileName == null) return false;

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return List.of("jpg", "jpeg", "png", "pdf", "doc", "docx", "xls", "xlsx").contains(extension);
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