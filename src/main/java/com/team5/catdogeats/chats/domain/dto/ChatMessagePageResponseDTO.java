package com.team5.catdogeats.chats.domain.dto;

import java.util.List;

public record ChatMessagePageResponseDTO<T>(
        List<T> contents,
        String nextCursor,
        boolean hasNext,
        int size
) {
    public static <T> ChatMessagePageResponseDTO<T> of(
            List<T> content,
            String nextCursor,
            boolean hasNext,
            int size) {
        return new ChatMessagePageResponseDTO<>(
                content,
                nextCursor,
                hasNext,
                size
        );
    }
}


