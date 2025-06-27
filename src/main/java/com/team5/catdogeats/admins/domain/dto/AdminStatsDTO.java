package com.team5.catdogeats.admins.domain.dto;

import lombok.Builder;

/**
 * 관리자 통계 정보 DTO
 */
@Builder
public record AdminStatsDTO(
        long totalCount,
        long activeCount,
        long pendingCount,
        long inactiveCount
) {
}
