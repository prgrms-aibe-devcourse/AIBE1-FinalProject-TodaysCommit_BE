package com.team5.catdogeats.chats.domain.dto;

import java.time.Instant;

public record ChatRoomResponseDTO(String roomId, Instant createdAt, Instant updatedAt) {
}
