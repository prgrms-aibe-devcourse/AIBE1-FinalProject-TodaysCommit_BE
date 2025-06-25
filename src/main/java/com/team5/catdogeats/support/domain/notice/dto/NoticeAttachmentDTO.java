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
    private String fileUrl;  // 🆕 추가!
    private String uploadedAt;

    public static NoticeAttachmentDTO from(NoticeFiles noticeFile) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        String fileUrl = noticeFile.getFiles().getFileUrl();
        String fileName = extractFileName(fileUrl);
        String correctedFileUrl = ensureHttpsUrl(fileUrl); // URL 보정 추가

        return NoticeAttachmentDTO.builder()
                .fileId(noticeFile.getFiles().getId())
                .fileName(fileName)
                .fileUrl(correctedFileUrl)  // 보정된 URL 사용
                .uploadedAt(noticeFile.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .build();
    }

    // URL 보정 메서드 추가
    private static String ensureHttpsUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return fileUrl;
        }

        // 이미 프로토콜이 있으면 그대로 반환
        if (fileUrl.startsWith("http://") || fileUrl.startsWith("https://")) {
            return fileUrl;
        }

        // 프로토콜이 없으면 https:// 추가
        return "https://" + fileUrl;
    }

    // 파일명 추출 (S3 URL 처리)
    private static String extractFileName(String fileUrl) {
        try {
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

            // 2. S3/CloudFront URL에서 파일명 추출 (우선 처리!)
            // notice_a58ad2a3_20250625_142224_0. 스프링 입문.pdf
            // -> 0. 스프링 입문.pdf
            if (fileName.startsWith("notice_")) {
                // "notice_"를 제거
                String withoutPrefix = fileName.substring(7); // "notice_" 제거

                // UUID 부분 제거 (첫 번째 _까지)
                int firstUnderscore = withoutPrefix.indexOf('_');
                if (firstUnderscore != -1) {
                    String afterUuid = withoutPrefix.substring(firstUnderscore + 1);

                    // 날짜 부분 제거 (두 번째 _까지) - YYYYMMDD
                    int secondUnderscore = afterUuid.indexOf('_');
                    if (secondUnderscore != -1) {
                        String afterDate = afterUuid.substring(secondUnderscore + 1);

                        // 시간 부분 제거 (세 번째 _까지) - HHMMSS
                        int thirdUnderscore = afterDate.indexOf('_');
                        if (thirdUnderscore != -1) {
                            return afterDate.substring(thirdUnderscore + 1); // 원본 파일명
                        }
                    }
                }
            }

            // 3. 기존 타임스탬프 패턴 제거 (기존 로직 유지)

            // 패턴 1: YYYYMMDD_HHMMSS_원본파일명.확장자 (예: 20250620_164240_멤버쉽 아이콘.png)
            if (fileName.matches("\\d{8}_\\d{6}_.*")) {
                String[] parts = fileName.split("_", 3);
                if (parts.length == 3) {
                    return parts[2];
                }
            }

            // 패턴 2: 밀리초타임스탬프_원본파일명.확장자 (예: 1719121234567_문서.pdf)
            if (fileName.matches("\\d{13,}_.*")) {
                int underscoreIndex = fileName.indexOf('_');
                if (underscoreIndex != -1) {
                    return fileName.substring(underscoreIndex + 1);
                }
            }

            // 패턴 3: 일반 타임스탬프_원본파일명 형태 (기존 호환성)
            if (fileName.matches("\\d+_.*")) {
                int underscoreIndex = fileName.indexOf('_');
                if (underscoreIndex != -1) {
                    return fileName.substring(underscoreIndex + 1);
                }
            }

            // 4. 타임스탬프가 없는 경우 그대로 반환
            return fileName;

        } catch (Exception e) {
            log.warn("파일명 추출 실패: {}", e.getMessage());
            return "첨부파일";
        }
    }
}