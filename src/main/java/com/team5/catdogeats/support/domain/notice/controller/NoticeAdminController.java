package com.team5.catdogeats.support.domain.notice.controller;

import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.support.domain.notice.dto.*;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.*;

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
            description = "관리자 페이지에서 공지사항 목록을 조회합니다. " +
                    "sortBy: latest(최신순, 기본값), oldest(오래된순), views(조회순)"
    )
    public ResponseEntity<ApiResponse<NoticeListResponseDTO>> getNotices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "latest") String sortBy) {

        NoticeListResponseDTO response = noticeService.getNotices(page, size, search, sortBy);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // ========== 공지사항 상세 조회 ==========
    @GetMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 상세 조회",
            description = "관리자 페이지에서 공지사항 상세 내용을 조회합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> getNotice(@PathVariable String noticeId) {

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
                .body(ApiResponse.success(ResponseCode.CREATED, response));
    }

    // ========== 공지사항 수정 ==========
    @PatchMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 수정",
            description = "관리자가 공지사항을 수정합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> updateNotice(
            @PathVariable String noticeId,
            @Valid @RequestBody NoticeUpdateRequestDTO requestDto) {

        log.info("[관리자] 공지사항 수정 요청 - ID: {}, 제목: {}", noticeId, requestDto.getTitle());

        NoticeResponseDTO response = noticeService.updateNotice(noticeId, requestDto);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // ========== 공지사항 삭제 ==========
    @DeleteMapping("/{noticeId}")
    @Operation(
            summary = "공지사항 삭제",
            description = "관리자가 공지사항을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteNotice(@PathVariable String noticeId) {
        log.info("[관리자] 공지사항 삭제 요청 - ID: {}", noticeId);

        try {
            noticeService.deleteNotice(noticeId);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
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

    // ========== 파일 업로드 ==========
    @PostMapping(value = "/{noticeId}/files")
    @Operation(
            summary = "공지사항 첨부파일 업로드",
            description = "관리자가 공지사항의 첨부파일을 업로드합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> uploadFile(
            @PathVariable String noticeId,
            @RequestParam("file") MultipartFile file) {

        // 기본적인 파일 존재 여부만 Controller에서 체크
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "업로드할 파일을 선택해주세요."));
        }

        try {
            // 파일 검증과 비즈니스 로직은 서비스에서 처리
            NoticeResponseDTO response = noticeService.uploadFile(noticeId, file);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (IllegalArgumentException e) {
            log.error("[관리자] 파일 업로드 검증 실패 - ID: {}, 오류: {}", noticeId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("[관리자] 파일 업로드 실패 - ID: {}, 오류: {}", noticeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 다운로드 ==========
    @GetMapping("/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 다운로드",
            description = "관리자 페이지에서 공지사항의 첨부파일을 다운로드합니다."
    )
    public ResponseEntity<Resource> downloadFile(@PathVariable String noticeId, @PathVariable String fileId) {

        try {
            // 서비스에서 DTO로 받아오기
            NoticeFileDownloadResponseDTO downloadResponse = noticeService.downloadFile(fileId);

            // DTO에서 필요한 정보 추출해서 ResponseEntity 구성
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(downloadResponse.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + downloadResponse.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(downloadResponse.getResource()); // ✅ Resource만 body에 넣기

        } catch (Exception e) {
            log.error("파일 다운로드 실패 - 파일 ID: {}, 오류: {}", fileId, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 파일 삭제 ==========
    @DeleteMapping("/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 삭제",
            description = "관리자가 공지사항의 첨부파일을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable String noticeId,
            @PathVariable String fileId) {

        try {
            noticeService.deleteFile(noticeId, fileId);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        } catch (NoSuchElementException e) {
            log.error("File or Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid file deletion request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during file deletion: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    // ========== 파일 수정(교체) ==========
    @PutMapping(value = "/{noticeId}/files/{fileId}")
    @Operation(
            summary = "공지사항 첨부파일 수정(교체)",
            description = "관리자가 공지사항의 첨부파일을 새 파일로 수정(교체)합니다."
    )
    public ResponseEntity<ApiResponse<NoticeResponseDTO>> replaceFile(
            @PathVariable String noticeId,
            @PathVariable String fileId,
            @RequestParam("file") MultipartFile newFile) {

        // 기본적인 파일 존재 여부만 Controller에서 체크
        if (newFile.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, "수정(교체)할 파일을 선택해주세요."));
        }

        try {
            // 파일 검증과 비즈니스 로직은 서비스에서 처리
            NoticeResponseDTO response = noticeService.replaceFile(noticeId, fileId, newFile);
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
        } catch (NoSuchElementException e) {
            log.error("File or Notice not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ResponseCode.ENTITY_NOT_FOUND, e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.error("Invalid file replacement request: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error(ResponseCode.INVALID_INPUT_VALUE, e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during file replacement: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }
}