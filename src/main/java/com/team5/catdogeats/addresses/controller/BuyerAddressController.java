package com.team5.catdogeats.addresses.controller;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.*;
import com.team5.catdogeats.addresses.service.AddressService;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.dto.ApiResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/v1/buyers")
@RequiredArgsConstructor
public class BuyerAddressController {

    private final AddressService addressService;

    // 구매자 주소 목록 조회 (페이징)
    @GetMapping("/address")
    public ResponseEntity<ApiResponse<AddressListResponseDto>> getAddresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        Pageable pageable = PageRequest.of(page, size);
        AddressListResponseDto response = addressService.getAddressesByUserAndType(
                userPrincipal, AddressType.PERSONAL, pageable);

        log.info("구매자 주소 목록 조회 - provider: {}, providerId: {}, page: {}, size: {}",
                userPrincipal.provider(), userPrincipal.providerId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // 구매자 주소 전체 목록 조회
    @GetMapping("/address/all")
    public ResponseEntity<ApiResponse<List<AddressResponseDto>>> getAllAddresses(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        List<AddressResponseDto> response = addressService.getAllAddressesByUserAndType(
                userPrincipal, AddressType.PERSONAL);

        log.info("구매자 주소 전체 목록 조회 - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // 구매자 주소 상세 조회
    @GetMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> getAddressById(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AddressResponseDto response = addressService.getAddressById(addressId, userPrincipal);

        log.info("구매자 주소 상세 조회 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // 구매자 주소 등록
    @PostMapping("/address")
    public ResponseEntity<ApiResponse<AddressResponseDto>> createAddress(
            @Valid @RequestBody AddressRequestDto requestDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        // 개인 주소 타입으로 설정
        AddressRequestDto personalAddressDto = AddressRequestDto.builder()
                .title(requestDto.getTitle())
                .city(requestDto.getCity())
                .district(requestDto.getDistrict())
                .neighborhood(requestDto.getNeighborhood())
                .streetAddress(requestDto.getStreetAddress())
                .postalCode(requestDto.getPostalCode())
                .detailAddress(requestDto.getDetailAddress())
                .phoneNumber(requestDto.getPhoneNumber())
                .addressType(AddressType.PERSONAL)  // 강제로 PERSONAL 설정
                .isDefault(requestDto.isDefault())
                .build();

        AddressResponseDto response = addressService.createAddress(personalAddressDto, userPrincipal);

        log.info("구매자 주소 등록 완료 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), response.getId());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.CREATED, response));
    }

    // 구매자 주소 수정
    @PatchMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<AddressResponseDto>> updateAddress(
            @PathVariable String addressId,
            @Valid @RequestBody AddressUpdateRequestDto updateDto,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AddressResponseDto response = addressService.updateAddress(addressId, updateDto, userPrincipal);

        log.info("구매자 주소 수정 완료 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // 구매자 주소 삭제
    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        addressService.deleteAddress(addressId, userPrincipal);

        log.info("구매자 주소 삭제 완료 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
    }

    // 기본 주소 설정
    @PatchMapping("/address/{addressId}/default")
    public ResponseEntity<ApiResponse<AddressResponseDto>> setDefaultAddress(
            @PathVariable String addressId,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AddressResponseDto response = addressService.setDefaultAddress(addressId, userPrincipal);

        log.info("기본 주소 설정 완료 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), addressId);
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }

    // 기본 주소 조회
    @GetMapping("/address/default")
    public ResponseEntity<ApiResponse<AddressResponseDto>> getDefaultAddress(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        AddressResponseDto response = addressService.getDefaultAddress(userPrincipal, AddressType.PERSONAL);

        if (response == null) {
            log.info("기본 주소 없음 - provider: {}, providerId: {}",
                    userPrincipal.provider(), userPrincipal.providerId());
            return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS));
        }

        log.info("기본 주소 조회 완료 - provider: {}, providerId: {}, addressId: {}",
                userPrincipal.provider(), userPrincipal.providerId(), response.getId());
        return ResponseEntity.ok(ApiResponse.success(ResponseCode.SUCCESS, response));
    }
}