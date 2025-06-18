package com.team5.catdogeats.users.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

public record SellerInfoResponse(
        String userId,
        String vendorName,
        String vendorProfileImage,
        String businessNumber,
        String settlementBank,
        String settlementAcc,
        String tags,
        LocalTime operatingStartTime,
        LocalTime operatingEndTime,
        String closedDays,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 정적 팩토리 메서드는 그대로 유지 가능
    public static SellerInfoResponse from(Sellers seller) {
        if (seller == null) {
            return null;
        }

        return new SellerInfoResponse(
                seller.getUserId() != null ? seller.getUserId().toString() : null,
                seller.getVendorName(),
                seller.getVendorProfileImage(),
                seller.getBusinessNumber(),
                seller.getSettlementBank(),
                seller.getSettlementAccount(),
                seller.getTags(),
                seller.getOperatingStartTime(),
                seller.getOperatingEndTime(),
                seller.getClosedDays(),
                seller.getCreatedAt() != null ? seller.getCreatedAt().toLocalDateTime() : null,
                seller.getUpdatedAt() != null ? seller.getUpdatedAt().toLocalDateTime() : null
        );
    }
}
