package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.notice.dto.*;
import com.team5.catdogeats.support.domain.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

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
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // ========== 공지사항 상세 조회 (공통 기능) ==========
    @GetMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "모든 사용자(판매자, 구매자, 관리자)가 공지사항 상세 내용을 조회할 수 있습니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> getNotice(@PathVariable String noticeId) {
        log.info("공지사항 상세 조회 요청 - ID: {}", noticeId);

        try {
            NoticeResponseDTO response = noticeService.getNotice(noticeId);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            log.error("Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 다운로드 (공통 기능) ==========
    @GetMapping("/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 다운로드",
            description = "모든 사용자가 공지사항의 첨부파일을 다운로드할 수 있습니다."
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable String noticeId, @PathVariable String fileId) {

        log.info("파일 다운로드 요청 - 공지사항 ID: {}, 파일 ID: {}", noticeId, fileId);

        try {
            Resource resource = noticeService.downloadFile(fileId);

            // 원본 파일명 추출
            String originalFilename = resource.getFilename();

            // 스마트 파일명 생성
            String smartFilename = generateSmartFilename(originalFilename);

            // MIME 타입 결정
            String contentType = determineContentType(originalFilename);

            log.info("파일 다운로드 성공 - 원본명: {}, 다운로드명: {}, 타입: {}",
                    originalFilename, smartFilename, contentType);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + smartFilename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (Exception e) {
            log.error("[관리자] 파일 다운로드 실패 - 파일 ID: {}, 오류: {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // 스마트 파일명 생성 메서드
    private String generateSmartFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "notice_attachment_" + System.currentTimeMillis();
        }

        // 확장자 추출
        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex);
        }

        // 파일 타입별 기본 이름 생성
        String baseFilename = generateBaseFilename(extension);

        // 현재 시간을 추가하여 중복 방지
        String timestamp = String.valueOf(System.currentTimeMillis());

        return baseFilename + "_" + timestamp + extension;
    }

    // 파일 타입별 기본 이름 생성
    private String generateBaseFilename(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "notice_file";
        }

        String ext = extension.toLowerCase();

        return switch (ext) {
            // 이미지 파일
            case ".jpg", ".jpeg", ".png", ".gif", ".bmp", ".webp" -> "notice_image";

            // 문서 파일
            case ".pdf" -> "notice_document";
            case ".doc", ".docx" -> "notice_word_document";
            case ".xls", ".xlsx" -> "notice_excel_document";
            case ".ppt", ".pptx" -> "notice_presentation";
            case ".txt" -> "notice_text_file";

            // 압축 파일
            case ".zip", ".rar", ".7z" -> "notice_archive";

            // 기타
            default -> "notice_file";
        };
    }

    // MIME 타입 결정 헬퍼 메서드 (기존과 동일)
    private String determineContentType(String filename) {
        if (filename == null) return "application/octet-stream";

        String extension = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "txt" -> "text/plain";
            default -> "application/octet-stream";
        };
    }
}