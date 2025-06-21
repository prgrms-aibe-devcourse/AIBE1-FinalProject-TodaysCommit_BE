package com.team5.catdogeats.users.domain.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public record SellerDTO(String userId,
                        String vendorName,
                        String vendorProfileImage,
                        String businessNumber,
                        String settlementBank,
                        String settlementAccount,
                        String tags,
                        LocalTime operatingStartTime,
                        LocalTime operatingEndTime,
                        String closedDays,
                        boolean isDeleted,
                        OffsetDateTime deletedAt) {
}
