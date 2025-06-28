package com.team5.catdogeats.chats.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record ChatMessagePageRequestDTO(String cursor,
                                        @Min(1) @Max(50) Integer size) {

    public ChatMessagePageRequestDTO {
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
