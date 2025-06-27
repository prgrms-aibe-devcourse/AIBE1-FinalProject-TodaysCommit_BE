package com.team5.catdogeats.storage.service.impl;

import com.team5.catdogeats.storage.domain.Files;
import com.team5.catdogeats.storage.repository.FilesRepository;
import com.team5.catdogeats.storage.service.NoticeFileManagementService;
import com.team5.catdogeats.storage.service.ObjectStorageService;
import com.team5.catdogeats.support.domain.notice.dto.NoticeFileDownloadResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NoticeFileManagementServiceImpl implements NoticeFileManagementService {

    private final FilesRepository filesRepository;
    private final ObjectStorageService objectStorageService;

    @Override
    public Files uploadNoticeFile(MultipartFile file) {
        try {
            // 공지사항 전용 파일명 생성
            String fileName = generateNoticeFileName(file.getOriginalFilename());

            String fileUrl = objectStorageService.uploadFile(
                    fileName,
                    file.getInputStream(),
                    file.getSize(),
                    file.getContentType()
            );

            log.info("공지사항 파일 업로드 완료: {}", fileUrl);

            Files fileEntity = Files.builder()
                    .fileUrl(fileUrl)
                    .build();

            return filesRepository.save(fileEntity);

        } catch (IOException e) {
            log.error("공지사항 파일 업로드 실패 - 파일명: {}", file.getOriginalFilename(), e);
            throw new RuntimeException("공지사항 파일 업로드 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public NoticeFileDownloadResponseDTO downloadNoticeFile(String fileId) {
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다: " + fileId));

        try {
            String fileUrl = fileEntity.getFileUrl();

            if (!fileUrl.startsWith("http")) {
                fileUrl = "https://" + fileUrl;
            }

            int lastSlash = fileUrl.lastIndexOf('/');
            String basePath = fileUrl.substring(0, lastSlash + 1);
            String fileName = fileUrl.substring(lastSlash + 1);

            String encodedFileName = java.net.URLEncoder.encode(fileName, "UTF-8");
            String finalUrl = basePath + encodedFileName;

            log.info("공지사항 파일 다운로드 최종 URL: {}", finalUrl);

            Resource resource = new UrlResource(finalUrl);
            String smartFilename = generateSmartFilename(fileName);
            String contentType = determineContentType(fileName);

            return new NoticeFileDownloadResponseDTO(resource, smartFilename, contentType);

        } catch (Exception e) {
            log.error("공지사항 파일 다운로드 실패 - 파일 ID: {}", fileId, e);
            throw new RuntimeException("공지사항 파일 다운로드 중 오류가 발생했습니다: " + fileId, e);
        }
    }

    @Override
    public void replaceNoticeFile(String fileId, MultipartFile newFile) {
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다: " + fileId));

        try {
            String oldFileUrl = fileEntity.getFileUrl();

            // 새 공지사항 파일 업로드
            String newFileName = generateNoticeFileName(newFile.getOriginalFilename());
            String newFileUrl = objectStorageService.uploadFile(
                    newFileName,
                    newFile.getInputStream(),
                    newFile.getSize(),
                    newFile.getContentType()
            );

            // 기존 파일 삭제
            deleteNoticeFileFromStorageOnly(oldFileUrl);

            // DB 업데이트
            fileEntity.setFileUrl(newFileUrl);
            filesRepository.save(fileEntity);

            log.info("공지사항 파일 교체 완료: {} -> {}", oldFileUrl, newFileUrl);

        } catch (IOException e) {
            log.error("공지사항 파일 교체 실패 - 파일 ID: {}", fileId, e);
            throw new RuntimeException("공지사항 파일 교체 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    public void deleteNoticeFileCompletely(String fileId) {
        Files fileEntity = filesRepository.findById(fileId)
                .orElseThrow(() -> new NoSuchElementException("파일을 찾을 수 없습니다: " + fileId));

        // Storage에서 삭제
        deleteNoticeFileFromStorageOnly(fileEntity.getFileUrl());

        // DB에서 삭제
        filesRepository.deleteById(fileId);

        log.info("공지사항 파일 완전 삭제 완료 - 파일 ID: {}", fileId);
    }

    @Override
    public void deleteNoticeFileFromStorageOnly(String fileUrl) {
        try {
            String key = extractKeyFromUrl(fileUrl);
            objectStorageService.deleteFile(key);
            log.info("공지사항 파일 Storage 삭제 완료: {}", fileUrl);
        } catch (Exception e) {
            log.warn("공지사항 파일 Storage 삭제 실패 (무시 가능): {}", e.getMessage());
        }
    }

    // ========== Private 헬퍼 메서드들 ==========
    // 공지사항 전용 파일명 생성
    // 형식: notice_UUID_타임스탬프_원본파일명
    private String generateNoticeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 올바르지 않습니다.");
        }

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        return "notice_" + uuid + "_" + timestamp + "_" + originalFileName;
    }

    //S3에서 파일 삭제
    private void deleteFileFromS3(String fileUrl) {
        // CloudFront URL에서 key 추출
        String key = extractKeyFromUrl(fileUrl);
        objectStorageService.deleteFile(key);
    }

    private String extractKeyFromUrl(String fileUrl) {
        int filesIndex = fileUrl.indexOf("files/");
        if (filesIndex != -1) {
            // "files/" 이후의 파일명만 반환 (prefix 제거)
            return fileUrl.substring(filesIndex + 6); // "files/" 길이만큼 스킵
        }

        // fallback: 마지막 슬래시 이후 파일명만 반환
        return fileUrl.substring(fileUrl.lastIndexOf('/') + 1);
    }

    //  파일 확장자 추출
    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }

    // MIME 타입 결정
    private String determineContentType(String filename) {
        if (filename == null) return "application/octet-stream";

        String extension = extractFileExtension(filename);

        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default -> "application/octet-stream";
        };
    }

    //스마트 파일명 생성
    private String generateSmartFilename(String originalFilename) {
        if (originalFilename == null || originalFilename.isEmpty()) {
            return "notice_attachment_" + System.currentTimeMillis();
        }

        String extension = "";
        int lastDotIndex = originalFilename.lastIndexOf(".");
        if (lastDotIndex > 0 && lastDotIndex < originalFilename.length() - 1) {
            extension = originalFilename.substring(lastDotIndex);
        }

        String baseFilename = generateBaseFilename(extension);
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
            case ".pdf" -> "notice_document";
            case ".doc", ".docx" -> "notice_word_document";
            case ".xls", ".xlsx" -> "notice_excel_document";
            case ".ppt", ".pptx" -> "notice_presentation";
            default -> "notice_file";
        };
    }
}