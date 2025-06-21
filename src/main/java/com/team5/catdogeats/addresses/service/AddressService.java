package com.team5.catdogeats.addresses.service;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.AddressListResponseDto;
import com.team5.catdogeats.addresses.dto.AddressRequestDto;
import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.addresses.dto.AddressUpdateRequestDto;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AddressService {

    // 사용자별 주소 목록 조회 (페이징)
    AddressListResponseDto getAddressesByUserAndType(String userId, AddressType addressType, Pageable pageable);

    // 사용자별 주소 목록 조회 (전체)
    List<AddressResponseDto> getAllAddressesByUserAndType(String userId, AddressType addressType);

    // 주소 상세 조회
    AddressResponseDto getAddressById(String addressId, String userId);

    // 주소 생성
    AddressResponseDto createAddress(AddressRequestDto requestDto, String userId);

    // 주소 수정
    AddressResponseDto updateAddress(String addressId, AddressUpdateRequestDto updateDto, String userId);

    // 주소 삭제
    void deleteAddress(String addressId, String userId);

    // 기본 주소 설정
    AddressResponseDto setDefaultAddress(String addressId, String userId);

    // 기본 주소 조회
    AddressResponseDto getDefaultAddress(String userId, AddressType addressType);
}