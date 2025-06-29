package com.team5.catdogeats.admins.domain.dto;

import lombok.Builder;

import java.util.List;

/**
 * 관리자 목록 조회 응답 DTO
 */
@Builder
public record AdminListResponseDTO(
        List<AdminInfoResponseDTO> admins,
        long totalElements,
        int totalPages,
        int currentPage,
        int pageSize,
        AdminStatsDTO stats
) {
}
