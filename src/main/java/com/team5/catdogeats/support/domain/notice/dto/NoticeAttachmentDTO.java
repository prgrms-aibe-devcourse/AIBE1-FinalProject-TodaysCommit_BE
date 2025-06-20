package com.team5.catdogeats.support.domain.notice.dto;

import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Getter
@Builder
@Slf4j
public class NoticeAttachmentDTO {
    private String fileId;
    private String fileName;
    private String uploadedAt;

    public static NoticeAttachmentDTO from(NoticeFiles noticeFile) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // FileStorageService를 통해 파일명 추출 (정적 메소드로 처리)
        String fileUrl = noticeFile.getFiles().getFileUrl();
        String fileName = extractFileName(fileUrl);

        return NoticeAttachmentDTO.builder()
                .fileId(noticeFile.getFiles().getId())
                .fileName(fileName)
                .uploadedAt(noticeFile.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .build();
    }

    // 파일명 추출 (정적 메소드) - Windows/Linux 경로 모두 처리
    private static String extractFileName(String fileUrl) {
        try {
            log.debug("원본 파일 URL: {}", fileUrl);

            // 1. 파일명 추출 (Windows와 Linux 경로 모두 처리)
            String fileName = fileUrl;

            // Windows 경로 처리: C:\path\to\file
            int windowsIndex = fileName.lastIndexOf("\\");
            if (windowsIndex != -1) {
                fileName = fileName.substring(windowsIndex + 1);
            }

            // Linux/URL 경로 처리: /path/to/file 또는 http://domain/path/to/file
            int unixIndex = fileName.lastIndexOf("/");
            if (unixIndex != -1) {
                fileName = fileName.substring(unixIndex + 1);
            }

            log.debug("경로 제거 후 파일명: {}", fileName);

            // 2. 타임스탬프 패턴 제거

            // 패턴 1: YYYYMMDD_HHMMSS_원본파일명.확장자 (예: 20250620_164240_멤버쉽 아이콘.png)
            if (fileName.matches("\\d{8}_\\d{6}_.*")) {
                String[] parts = fileName.split("_", 3);
                if (parts.length == 3) {
                    String originalName = parts[2];
                    log.debug("YYYYMMDD_HHMMSS 패턴 제거 - 원본명: {}", originalName);
                    return originalName;
                }
            }

            // 패턴 2: 밀리초타임스탬프_원본파일명.확장자 (예: 1719121234567_문서.pdf)
            if (fileName.matches("\\d{13,}_.*")) {
                int underscoreIndex = fileName.indexOf('_');
                if (underscoreIndex != -1) {
                    String originalName = fileName.substring(underscoreIndex + 1);
                    log.debug("밀리초 타임스탬프 패턴 제거 - 원본명: {}", originalName);
                    return originalName;
                }
            }

            // 패턴 3: 일반 타임스탬프_원본파일명 형태 (기존 호환성)
            if (fileName.matches("\\d+_.*")) {
                int underscoreIndex = fileName.indexOf('_');
                if (underscoreIndex != -1) {
                    String originalName = fileName.substring(underscoreIndex + 1);
                    log.debug("일반 타임스탬프 패턴 제거 - 원본명: {}", originalName);
                    return originalName;
                }
            }

            // 3. 타임스탬프가 없는 경우 그대로 반환
            log.debug("타임스탬프 패턴 없음 - 파일명 그대로 반환: {}", fileName);
            return fileName;

        } catch (Exception e) {
            log.warn("파일명 추출 중 오류 발생 - URL: {}, 오류: {}", fileUrl, e.getMessage());
            return "첨부파일";
        }
    }
}