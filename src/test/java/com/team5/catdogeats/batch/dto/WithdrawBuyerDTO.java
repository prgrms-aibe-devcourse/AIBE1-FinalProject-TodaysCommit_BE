package com.team5.catdogeats.batch.dto;

import java.time.OffsetDateTime;

public record WithdrawBuyerDTO(String userId,
                               String role,
                               boolean accountDisable,
                               OffsetDateTime deletedAt,
                               String buyerId, boolean isDeleted,
                               OffsetDateTime buyerDeletedAt) {}
