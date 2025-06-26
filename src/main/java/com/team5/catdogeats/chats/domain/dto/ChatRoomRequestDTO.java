package com.team5.catdogeats.chats.domain.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRoomRequestDTO(@NotBlank String vendorName) {
}

