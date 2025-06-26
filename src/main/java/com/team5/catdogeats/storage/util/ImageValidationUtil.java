package com.team5.catdogeats.storage.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 이미지 파일 검증을 위한 공용 유틸리티 클래스
 * Storage 도메인에서 사용하는 이미지 검증 로직을 통합 관리
 */
@Slf4j
@Component
public class ImageValidationUtil {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final int MAX_FILENAME_LENGTH = 255;

    /**
     * 통합 이미지 파일 검증
     * - 기본 속성 검사 (크기, NULL 등)
     * - 실제 파일 내용 검증 (Magic Number)
     * - 보안 강화된 단일 검증 메서드
     *
     * @param imageFile 검증할 이미지 파일
     * @throws IllegalArgumentException 검증 실패 시
     */
    public void validateImageFile(MultipartFile imageFile) {
        // 기본 검사
        validateBasicProperties(imageFile);

        // MIME Type 검증
        validateMimeType(imageFile);

        // 파일명 보안 검증
        validateFileName(imageFile.getOriginalFilename());

        // 스크립트 공격 방지
        validateNoScriptContent(imageFile);

        // 실제 파일 내용 검증
        validateFileSignature(imageFile);

        log.debug("이미지 파일 검증 완료 - fileName: {}, size: {}",
                imageFile.getOriginalFilename(), imageFile.getSize());
    }

    /**
     * 기본 속성 검증
     */
    private void validateBasicProperties(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 비어있습니다.");
        }

        if (imageFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("이미지 파일 크기는 10MB를 초과할 수 없습니다.");
        }
    }

    /**
     * MIME Type 검증
     */
    private void validateMimeType(MultipartFile file) {
        String contentType = file.getContentType();

        if (contentType == null) {
            throw new IllegalArgumentException("파일의 Content-Type을 확인할 수 없습니다.");
        }

        if (!contentType.matches("^image/(jpeg|jpg|png|webp)$")) {
            throw new IllegalArgumentException(
                    String.format("허용되지 않은 MIME 타입입니다: %s", contentType));
        }
    }

    /**
     * 파일명 보안 검증
     */
    private void validateFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("파일명이 비어있습니다.");
        }

        if (fileName.length() > MAX_FILENAME_LENGTH) {
            throw new IllegalArgumentException("파일명이 너무 깁니다. (최대 255자)");
        }

        // 경로 순회 공격 방지
        if (fileName.contains("..") || fileName.contains("./") || fileName.contains(".\\")) {
            throw new IllegalArgumentException("파일명에 상대경로가 포함될 수 없습니다.");
        }

        // 위험한 확장자 차단
        if (fileName.toLowerCase().matches(".*\\.(js|html|htm|php|jsp|asp|exe|bat|cmd).*")) {
            throw new IllegalArgumentException("실행 가능한 파일 확장자는 업로드할 수 없습니다.");
        }
    }

    /**
     * 스크립트 공격 방지
     */
    private void validateNoScriptContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] buffer = new byte[2048];
            int bytesRead = is.read(buffer);

            if (bytesRead > 0) {
                String content = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8).toLowerCase();

                String[] dangerousPatterns = {
                        "<script", "javascript:", "onload=", "onerror=",
                        "onclick=", "eval(", "document.", "alert("
                };

                for (String pattern : dangerousPatterns) {
                    if (content.contains(pattern)) {
                        throw new IllegalArgumentException(
                                "보안상 위험한 스크립트가 포함된 파일은 업로드할 수 없습니다.");
                    }
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("파일 내용 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 파일 시그니처 검증 (Magic Number)
     * - JPEG, PNG, WebP 실제 파일 형식 확인
     * - Content-Type 조작 공격 방어
     */
    private void validateFileSignature(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = new byte[12]; // WebP 확인을 위해 12바이트
            int bytesRead = is.read(header);

            if (bytesRead < 4) {
                throw new IllegalArgumentException("파일 형식을 확인할 수 없습니다.");
            }

            // JPEG: FF D8 FF
            if (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF) {
                return;
            }

            // PNG: 89 50 4E 47
            if (header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return;
            }

            // WebP: RIFF....WEBP
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 &&
                    header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return;
            }

            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다. (JPEG, PNG, WebP만 지원)");

        } catch (IOException e) {
            throw new IllegalArgumentException("파일 형식 검증 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 안전한 파일 확장자 추출
     */
    public String getFileExtension(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return "jpg";
        }

        String safeName = fileName.replaceAll("[^a-zA-Z0-9._-]", "");
        int lastDotIndex = safeName.lastIndexOf('.');

        if (lastDotIndex != -1 && lastDotIndex < safeName.length() - 1) {
            String ext = safeName.substring(lastDotIndex + 1).toLowerCase();
            if (ext.matches("^(jpg|jpeg|png|webp)$")) {
                return ext;
            }
        }

        return "jpg";
    }
}