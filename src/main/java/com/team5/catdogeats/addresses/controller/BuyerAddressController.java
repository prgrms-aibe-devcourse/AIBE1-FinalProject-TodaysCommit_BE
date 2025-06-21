package com.team5.catdogeats.addresses.controller;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.AddressListResponseDto;
import com.team5.catdogeats.addresses.dto.AddressRequestDto;
import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.addresses.dto.AddressUpdateRequestDto;
import com.team5.catdogeats.addresses.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<AddressListResponseDto> getAddresses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestHeader("X-User-Id") String userId) {

        Pageable pageable = PageRequest.of(page, size);
        AddressListResponseDto response = addressService.getAddressesByUserAndType(
                userId, AddressType.PERSONAL, pageable);

        log.info("구매자 주소 목록 조회 - userId: {}, page: {}, size: {}", userId, page, size);
        return ResponseEntity.ok(response);
    }

    // 구매자 주소 전체 목록 조회
    @GetMapping("/address/all")
    public ResponseEntity<List<AddressResponseDto>> getAllAddresses(
            @RequestHeader("X-User-Id") String userId) {

        List<AddressResponseDto> response = addressService.getAllAddressesByUserAndType(
                userId, AddressType.PERSONAL);

        log.info("구매자 주소 전체 목록 조회 - userId: {}", userId);
        return ResponseEntity.ok(response);
    }

    // 구매자 주소 상세 조회
    @GetMapping("/address/{addressId}")
    public ResponseEntity<AddressResponseDto> getAddressById(
            @PathVariable String addressId,
            @RequestHeader("X-User-Id") String userId) {

        AddressResponseDto response = addressService.getAddressById(addressId, userId);

        log.info("구매자 주소 상세 조회 - userId: {}, addressId: {}", userId, addressId);
        return ResponseEntity.ok(response);
    }

    // 구매자 주소 등록
    @PostMapping("/address")
    public ResponseEntity<AddressResponseDto> createAddress(
            @Valid @RequestBody AddressRequestDto requestDto,
            @RequestHeader("X-User-Id") String userId) {

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

        AddressResponseDto response = addressService.createAddress(personalAddressDto, userId);

        log.info("구매자 주소 등록 완료 - userId: {}, addressId: {}", userId, response.getId());
        return ResponseEntity.ok(response);
    }

    // 구매자 주소 수정
    @PatchMapping("/address/{addressId}")
    public ResponseEntity<AddressResponseDto> updateAddress(
            @PathVariable String addressId,
            @Valid @RequestBody AddressUpdateRequestDto updateDto,
            @RequestHeader("X-User-Id") String userId) {

        AddressResponseDto response = addressService.updateAddress(addressId, updateDto, userId);

        log.info("구매자 주소 수정 완료 - userId: {}, addressId: {}", userId, addressId);
        return ResponseEntity.ok(response);
    }

    // 구매자 주소 삭제
    @DeleteMapping("/address/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable String addressId,
            @RequestHeader("X-User-Id") String userId) {

        addressService.deleteAddress(addressId, userId);

        log.info("구매자 주소 삭제 완료 - userId: {}, addressId: {}", userId, addressId);
        return ResponseEntity.noContent().build();
    }

    // 기본 주소 설정
    @PatchMapping("/address/{addressId}/default")
    public ResponseEntity<AddressResponseDto> setDefaultAddress(
            @PathVariable String addressId,
            @RequestHeader("X-User-Id") String userId) {

        AddressResponseDto response = addressService.setDefaultAddress(addressId, userId);

        log.info("기본 주소 설정 완료 - userId: {}, addressId: {}", userId, addressId);
        return ResponseEntity.ok(response);
    }

    // 기본 주소 조회
    @GetMapping("/address/default")
    public ResponseEntity<AddressResponseDto> getDefaultAddress(
            @RequestHeader("X-User-Id") String userId) {

        AddressResponseDto response = addressService.getDefaultAddress(userId, AddressType.PERSONAL);

        if (response == null) {
            log.info("기본 주소 없음 - userId: {}", userId);
            return ResponseEntity.noContent().build();
        }

        log.info("기본 주소 조회 완료 - userId: {}, addressId: {}", userId, response.getId());
        return ResponseEntity.ok(response);
    }
}