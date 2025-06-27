package com.team5.catdogeats.support.domain.notice.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
public class NoticeListResponseDTO {

    private List<NoticeResponseDTO> notices;
    private int currentPage;
    private int totalPages;
    private long totalElements;
    private int size;
    private boolean hasNext;
    private boolean hasPrevious;

    public static NoticeListResponseDTO from(Page<NoticeResponseDTO> noticePage) {
        return NoticeListResponseDTO.builder()
                .notices(noticePage.getContent())
                .currentPage(noticePage.getNumber())
                .totalPages(noticePage.getTotalPages())
                .totalElements(noticePage.getTotalElements())
                .size(noticePage.getSize())
                .hasNext(noticePage.hasNext())
                .hasPrevious(noticePage.hasPrevious())
                .build();
    }
}
