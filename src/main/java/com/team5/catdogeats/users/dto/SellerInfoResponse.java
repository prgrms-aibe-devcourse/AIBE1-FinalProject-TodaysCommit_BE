package com.team5.catdogeats.users.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import lombok.*;

import java.time.LocalDateTime;

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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Entity to Response DTO 변환 메서드
    public static SellerInfoResponse from(Sellers seller) {
        return SellerInfoResponse.builder()
                .userId(seller.getUserId().toString())
                .vendorName(seller.getVendorName())
                .vendorProfileImage(seller.getVendorProfileImage())
                .businessNumber(seller.getBusinessNumber())
                .settlementBank(seller.getSettlementBank())
                .settlementAcc(seller.getSettlementAcc())
                .tags(seller.getTags())
                .createdAt(seller.getCreatedAt().toLocalDateTime())
                .updatedAt(seller.getUpdatedAt().toLocalDateTime())
                .build();
    }
}
