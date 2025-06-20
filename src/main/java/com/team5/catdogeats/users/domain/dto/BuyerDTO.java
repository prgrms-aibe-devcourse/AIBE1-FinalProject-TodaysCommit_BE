package com.team5.catdogeats.users.domain.dto;

import java.util.UUID;

public record BuyerDTO(UUID userId,
                       boolean nameMaskingStatus) {
}
