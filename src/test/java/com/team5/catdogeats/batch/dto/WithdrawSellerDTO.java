package com.team5.catdogeats.batch.dto;

import java.time.OffsetDateTime;


// 테스트용 입니다
public record WithdrawSellerDTO(String userId,
                                String role,
                                boolean accountDisable,
                                OffsetDateTime deletedAt,
                                String sellerId,
                                boolean isDeleted,
                                OffsetDateTime sellerDeletedAt) {
}
