package com.team5.catdogeats.users.domain.dto;

import java.time.OffsetDateTime;

public record BuyerDTO(String userId,
                       boolean nameMaskingStatus,
                       boolean isDeleted,
                       OffsetDateTime deletedAt) {
}
