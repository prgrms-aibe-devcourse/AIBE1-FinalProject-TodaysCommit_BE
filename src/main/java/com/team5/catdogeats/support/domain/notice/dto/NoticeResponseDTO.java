package com.team5.catdogeats.support.domain.notice.dto;

import com.team5.catdogeats.storage.domain.mapping.NoticeFiles;
import com.team5.catdogeats.support.domain.Notices;
import lombok.Builder;
import lombok.Getter;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class NoticeResponseDTO {

    private String id;
    private String title;
    private String content;
    private String createdAt;
    private String updatedAt;
    private Long viewCount;
    private List<NoticeAttachmentDTO> attachments;

    public static NoticeResponseDTO from(Notices notices) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 첨부파일 없이
        return NoticeResponseDTO.builder()
                .id(notices.getId())
                .title(notices.getTitle())
                .content(notices.getContent())
                .createdAt(notices.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .updatedAt(notices.getUpdatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .viewCount(notices.getViewCount())
                .attachments(new ArrayList<>())
                .build();
    }

    // 첨부파일 포함
    public static NoticeResponseDTO fromWithAttachments(Notices notices, List<NoticeFiles> noticeFiles) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // NoticeFiles에서 AttachmentDTO로 변환
        List<NoticeAttachmentDTO> attachments = noticeFiles.stream()
                .map(NoticeAttachmentDTO::from)
                .collect(Collectors.toList());

        return NoticeResponseDTO.builder()
                .id(notices.getId())
                .title(notices.getTitle())
                .content(notices.getContent())
                .createdAt(notices.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .updatedAt(notices.getUpdatedAt().withZoneSameInstant(koreaZone).format(formatter))
                .viewCount(notices.getViewCount())
                .attachments(attachments)  // 실제 첨부파일 목록
                .build();
    }
}
