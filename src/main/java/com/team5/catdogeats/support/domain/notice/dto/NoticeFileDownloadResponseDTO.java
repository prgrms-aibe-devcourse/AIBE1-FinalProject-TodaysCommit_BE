package com.team5.catdogeats.support.domain.notice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.io.Resource;

@Getter
@AllArgsConstructor
public class NoticeFileDownloadResponseDTO {
    private final Resource resource;
    private final String filename;
    private final String contentType;
}