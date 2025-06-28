package com.team5.catdogeats.chats.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ReadReceiptDTO(@NotBlank String roomId) {
}
