package com.team5.catdogeats.support.domain.notice.dto;

import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Getter
@Builder
public class NoticeAttachmentDTO {
    private UUID fileId;
    private String fileName;
    private String uploadedAt;

    public static NoticeAttachmentDTO from(NoticeFiles noticeFile) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // storage.domain의 Files 엔티티에서 정보 가져오기
        String fileUrl = noticeFile.getFiles().getFileUrl();
        String fileName = extractFileName(fileUrl);

        return NoticeAttachmentDTO.builder()
                .fileId(noticeFile.getFiles().getId())  // Files 엔티티의 ID
                .fileName(fileName)
                .uploadedAt(noticeFile.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .build();
    }

    private static String extractFileName(String fileUrl) {
        try {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
            // 타임스탬프_원본파일명 형태에서 원본파일명만 추출
            if (fileName.matches("\\d+_.*")) {
                return fileName.substring(fileName.indexOf('_') + 1);
            }
            return fileName;
        } catch (Exception e) {
            return "파일명 불명";
        }
    }
}