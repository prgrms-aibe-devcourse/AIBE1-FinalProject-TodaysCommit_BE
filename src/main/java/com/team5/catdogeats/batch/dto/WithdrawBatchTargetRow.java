package com.team5.catdogeats.batch.dto;

import java.time.OffsetDateTime;

public record WithdrawBatchTargetRow(String id,
                                     String role,
                                     OffsetDateTime deletedAt) {}

