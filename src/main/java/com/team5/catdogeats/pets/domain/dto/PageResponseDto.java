package com.team5.catdogeats.pets.domain.dto;

import java.util.List;

public record PageResponseDto<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last // 현재 페이지가 마지막 페이지인가?
) {
}
