package com.team5.catdogeats.users.domain.dto;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalTime;

@Schema(description = "판매자 정보 응답 DTO")
public record SellerInfoResponseDTO(

        @Schema(description = "사용자 ID", example = "2ceb807f-586f-4450-b470-d1ece7173749")
        String userId,

        @Schema(description = "업체명", example = "멍멍이네 수제간식")
        String vendorName,

        @Schema(description = "업체 프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String vendorProfileImage,

        @Schema(description = "사업자 등록번호", example = "123-45-67890")
        String businessNumber,

        @Schema(description = "정산 은행명", example = "국민은행")
        String settlementBank,

        @Schema(description = "정산 계좌번호", example = "123456789012")
        String settlementAcc,

        @Schema(description = "태그", example = "수제간식,강아지")
        String tags,

        @Schema(description = "운영 시작 시간", example = "09:00:00")
        LocalTime operatingStartTime,

        @Schema(description = "운영 종료 시간", example = "18:00:00")
        LocalTime operatingEndTime,

        @Schema(description = "휴무일", example = "월요일,화요일")
        String closedDays,

        @Schema(description = "생성일시", example = "2024-01-15T10:30:00")
        LocalDateTime createdAt,

        @Schema(description = "수정일시", example = "2024-01-20T14:20:00")
        LocalDateTime updatedAt

) {
    public static SellerInfoResponseDTO from(Sellers seller) {
        if (seller == null) {
            return null;
        }

        return new SellerInfoResponseDTO(
                seller.getUserId() != null ? seller.getUserId() : null,
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