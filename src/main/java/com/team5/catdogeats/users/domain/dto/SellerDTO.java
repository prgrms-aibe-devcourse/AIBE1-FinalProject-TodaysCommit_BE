package com.team5.catdogeats.users.domain.dto;

import java.time.LocalTime;
import java.util.UUID;

public record SellerDTO(UUID userId,
                        String vendorName,
                        String vendorProfileImage,
                        String businessNumber,
                        String settlementBank,
                        String settlementAccount,
                        String tags,
                        LocalTime operatingStartTime,
                        LocalTime operatingEndTime,
                        String closedDays) {
}
