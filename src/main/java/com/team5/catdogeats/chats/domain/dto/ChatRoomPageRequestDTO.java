package com.team5.catdogeats.chats.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;

import java.time.Instant;

@Builder
public record ChatRoomPageRequestDTO(
        String cursor,  // lastMessageAt의 ISO 문자열
        @Min(1) @Max(50) Integer size
) {
    public ChatRoomPageRequestDTO {
        if (size == null) {
            size = 20; // 기본값
        }
    }

    public Instant getCursorAsInstant() {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        try {
            return Instant.parse(cursor);
        } catch (Exception e) {
            throw new IllegalArgumentException("잘못된 커서 형식입니다: " + cursor);
        }
    }
}
