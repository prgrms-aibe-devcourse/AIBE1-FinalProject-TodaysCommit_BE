package com.team5.catdogeats.chats.domain.dto;

import java.util.List;

public record ChatRoomPageResponseDTO<T>(
        List<T> content,
        String nextCursor,  // 다음 페이지를 위한 커서
        boolean hasNext,    // 다음 페이지 존재 여부
        int size           // 현재 페이지 크기
) {
    public static <T> ChatRoomPageResponseDTO<T> of(
            List<T> content,
            String nextCursor,
            boolean hasNext,
            int size) {
        return new ChatRoomPageResponseDTO<>(
                content,
                nextCursor,
                hasNext,
                size
        );
    }
}
