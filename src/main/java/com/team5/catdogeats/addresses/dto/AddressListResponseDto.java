package com.team5.catdogeats.addresses.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressListResponseDto {

    private List<AddressResponseDto> addresses;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
    private boolean hasNext;
    private boolean hasPrevious;

    // Page 객체로부터 생성
    public static AddressListResponseDto from(Page<AddressResponseDto> addressPage) {
        return AddressListResponseDto.builder()
                .addresses(addressPage.getContent())
                .totalElements(addressPage.getTotalElements())
                .totalPages(addressPage.getTotalPages())
                .currentPage(addressPage.getNumber())
                .pageSize(addressPage.getSize())
                .hasNext(addressPage.hasNext())
                .hasPrevious(addressPage.hasPrevious())
                .build();
    }

    // List 에서 생성 (페이징 없는 경우)
    public static AddressListResponseDto from(List<AddressResponseDto> addresses) {
        return AddressListResponseDto.builder()
                .addresses(addresses)
                .totalElements(addresses.size())
                .totalPages(1)
                .currentPage(0)
                .pageSize(addresses.size())
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }
}