package com.team5.catdogeats.addresses.service;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface AddressService {

    // 사용자별 주소 목록 조회 (페이징)
    AddressListResponseDto getAddressesByUserAndType(UUID userId, AddressType addressType, Pageable pageable);

    // 사용자별 주소 목록 조회 (전체)
    List<AddressResponseDto> getAllAddressesByUserAndType(UUID userId, AddressType addressType);

    // 주소 상세 조회
    AddressResponseDto getAddressById(UUID addressId, UUID userId);

    // 주소 생성
    AddressResponseDto createAddress(AddressRequestDto requestDto, UUID userId);

    // 주소 수정
    AddressResponseDto updateAddress(UUID addressId, AddressUpdateRequestDto updateDto, UUID userId);

    // 주소 삭제
    void deleteAddress(UUID addressId, UUID userId);

    // 기본 주소 설정
    AddressResponseDto setDefaultAddress(UUID addressId, UUID userId);

    // 기본 주소 조회
    AddressResponseDto getDefaultAddress(UUID userId, AddressType addressType);
}