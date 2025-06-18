package com.team5.catdogeats.users.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.*;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerInfoResponse {

    private String userId;
    private String vendorName;
    private String vendorProfileImage;
    private String businessNumber;
    private String settlementBank;
    private String settlementAcc;
    private String tags;
    private LocalTime operatingStartTime;
    private LocalTime operatingEndTime;
    private String closedDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity to Response DTO 변환 메서드

    //null-체크
    public static SellerInfoResponse from(Sellers seller) {
        if (seller == null) {
            return null;
        }

        return SellerInfoResponse.builder()
                .userId(seller.getUserId() != null ? seller.getUserId().toString() : null) // Null 체크
                .vendorName(seller.getVendorName())
                .vendorProfileImage(seller.getVendorProfileImage())
                .businessNumber(seller.getBusinessNumber())
                .settlementBank(seller.getSettlementBank())
                .settlementAcc(seller.getSettlementAccount())
                .tags(seller.getTags())
                .operatingStartTime(seller.getOperatingStartTime())
                .operatingEndTime(seller.getOperatingEndTime())
                .closedDays(seller.getClosedDays())
                .createdAt(seller.getCreatedAt() != null ? seller.getCreatedAt().toLocalDateTime() : null)
                .updatedAt(seller.getUpdatedAt() != null ? seller.getUpdatedAt().toLocalDateTime() : null)
                .build();
    }
}
