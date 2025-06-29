package com.team5.catdogeats.addresses.service.impl;

import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.addresses.dto.AddressListResponseDto;
import com.team5.catdogeats.addresses.dto.AddressRequestDto;
import com.team5.catdogeats.addresses.dto.AddressResponseDto;
import com.team5.catdogeats.addresses.dto.AddressUpdateRequestDto;
import com.team5.catdogeats.addresses.exception.AddressAccessDeniedException;
import com.team5.catdogeats.addresses.exception.AddressNotFoundException;
import com.team5.catdogeats.addresses.exception.UserNotFoundException;
import com.team5.catdogeats.addresses.repository.AddressRepository;
import com.team5.catdogeats.addresses.service.AddressService;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final BuyerRepository buyerRepository;

    @Override
    public AddressListResponseDto getAddressesByUserAndType(UserPrincipal userPrincipal, AddressType addressType, Pageable pageable) {
        String userId = findUserIdByPrincipal(userPrincipal);

        Page<Addresses> addressPage = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, addressType, pageable);

        Page<AddressResponseDto> dtoPage = addressPage.map(AddressResponseDto::from);
        return AddressListResponseDto.from(dtoPage);
    }

    @Override
    public List<AddressResponseDto> getAllAddressesByUserAndType(UserPrincipal userPrincipal, AddressType addressType) {
        String userId = findUserIdByPrincipal(userPrincipal);

        List<Addresses> addresses = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, addressType);

        return addresses.stream()
                .map(AddressResponseDto::from)
                .toList();
    }

    @Override
    public AddressResponseDto getAddressById(String addressId, UserPrincipal userPrincipal) {
        String userId = findUserIdByPrincipal(userPrincipal);

        Addresses address = findAddressById((addressId));
        validateAddressOwnership(address, userId);
        return AddressResponseDto.from(address);
    }

    @Override
    @JpaTransactional
    public AddressResponseDto createAddress(AddressRequestDto requestDto, UserPrincipal userPrincipal) {
        String userId = findUserIdByPrincipal(userPrincipal);

        // 주소 개수 제한 검증
        validateAddressLimit(userId, requestDto.getAddressType());

        // 기본 주소 설정 시 기존 기본 주소 해제
        if (requestDto.isDefault()) {
            addressRepository.clearDefaultAddresses(userId, requestDto.getAddressType());
        }

        // Users 엔티티는 JPA에서 필요하므로 lazy loading을 위한 reference 생성
        Users userReference = userRepository.getReferenceById(userId);

        Addresses address = Addresses.builder()
                .user(userReference)
                .title(requestDto.getTitle())
                .city(requestDto.getCity())
                .district(requestDto.getDistrict())
                .neighborhood(requestDto.getNeighborhood())
                .streetAddress(requestDto.getStreetAddress())
                .postalCode(requestDto.getPostalCode())
                .detailAddress(requestDto.getDetailAddress())
                .phoneNumber(requestDto.getPhoneNumber())
                .addressType(requestDto.getAddressType())
                .isDefault(requestDto.isDefault())
                .build();

        Addresses savedAddress = addressRepository.save(address);
        log.info("주소 생성 완료 - userId: {}, addressId: {}, type: {}",
                userId, savedAddress.getId(), requestDto.getAddressType());

        return AddressResponseDto.from(savedAddress);
    }

    @Override
    @JpaTransactional
    public AddressResponseDto updateAddress(String addressId, AddressUpdateRequestDto updateDto, UserPrincipal userPrincipal) {
        String userId = findUserIdByPrincipal(userPrincipal);

        Addresses address = findAddressById((addressId));
        validateAddressOwnership(address, userId);

        // 주소 정보 업데이트
        address.updateAddress(
                updateDto.getTitle(),
                updateDto.getCity(),
                updateDto.getDistrict(),
                updateDto.getNeighborhood(),
                updateDto.getStreetAddress(),
                updateDto.getPostalCode(),
                updateDto.getDetailAddress(),
                updateDto.getPhoneNumber()
        );

        // 기본 주소 설정 변경
        if (updateDto.hasIsDefault() && updateDto.getIsDefault()) {
            addressRepository.clearDefaultAddresses(userId, address.getAddressType());
            address.setAsDefault();
        } else if (updateDto.hasIsDefault() && !updateDto.getIsDefault()) {
            address.removeDefault();
        }

        log.info("주소 수정 완료 - userId: {}, addressId: {}", userId, addressId);
        return AddressResponseDto.from(address);
    }

    @Override
    @JpaTransactional
    public void deleteAddress(String addressId, UserPrincipal userPrincipal) {
        // UserPrincipal에서 userId 조회
        String userId = findUserIdByPrincipal(userPrincipal);

        Addresses address = findAddressById((addressId));
        validateAddressOwnership(address, userId);

        addressRepository.delete(address);
        log.info("주소 삭제 완료 - userId: {}, addressId: {}", userId, addressId);
    }

    @Override
    @JpaTransactional
    public AddressResponseDto setDefaultAddress(String addressId, UserPrincipal userPrincipal) {
        // UserPrincipal에서 userId 조회
        String userId = findUserIdByPrincipal(userPrincipal);

        Addresses address = findAddressById((addressId));
        validateAddressOwnership(address, userId);

        // 기존 기본 주소 해제
        addressRepository.clearDefaultAddresses(userId, address.getAddressType());

        // 새로운 기본 주소 설정
        address.setAsDefault();

        log.info("기본 주소 설정 완료 - userId: {}, addressId: {}", userId, addressId);
        return AddressResponseDto.from(address);
    }

    @Override
    public AddressResponseDto getDefaultAddress(UserPrincipal userPrincipal, AddressType addressType) {
        // UserPrincipal에서 userId 조회
        String userId = findUserIdByPrincipal(userPrincipal);

        return addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(userId, addressType)
                .map(AddressResponseDto::from)
                .orElse(null);
    }

    // Private 헬퍼 메서드
    // UserPrincipal에서 userId 조회
    private String findUserIdByPrincipal(UserPrincipal userPrincipal) {
        BuyerDTO buyerDTO = buyerRepository.findOnlyBuyerByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> new UserNotFoundException("해당 유저 정보를 찾을 수 없습니다."));

        return buyerDTO.userId();
    }

    private Addresses findAddressById(String addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new AddressNotFoundException("주소를 찾을 수 없습니다: " + addressId));
    }

    private void validateAddressOwnership(Addresses address, String userId) {
        if (!address.isOwnedBy(userId)) {
            throw new AddressAccessDeniedException("해당 주소에 접근할 권한이 없습니다.");
        }
    }

    private void validateAddressLimit(String userId, AddressType addressType) {
        long addressCount = addressRepository.countByUserIdAndAddressType(userId, addressType);

        // 주소 제한 (개인 주소: 10개, 사업자 주소: 1개)
        int maxAddresses = (addressType == AddressType.PERSONAL) ? 10 : 1;

        if (addressCount >= maxAddresses) {
            String message = String.format("%s은(는) 최대 %d개까지만 등록할 수 있습니다.",
                    addressType.getDescription(), maxAddresses);
            throw new IllegalArgumentException(message);
        }
    }
}