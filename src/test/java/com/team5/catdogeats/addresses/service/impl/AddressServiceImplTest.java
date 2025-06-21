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
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AddressService 테스트")
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AddressServiceImpl addressService;

    private Users testUser;
    private Addresses testAddress;
    private String userId;
    private String addressId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID().toString();
        addressId = UUID.randomUUID().toString();

        testUser = Users.builder()
                .id(userId)
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();

        testAddress = Addresses.builder()
                .id(addressId.toString())
                .user(testUser)
                .title("집")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();
    }

    @Test
    @DisplayName("주소 목록 조회(페이징) - 성공")
    void getAddressesByUserAndType_Success() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Page<Addresses> addressPage = new PageImpl<>(Arrays.asList(testAddress), pageable, 1);

        given(userRepository.existsById(UUID.fromString(userId))).willReturn(true);
        given(addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, AddressType.PERSONAL, pageable)).willReturn(addressPage);

        // when
        AddressListResponseDto result = addressService.getAddressesByUserAndType(
                userId, AddressType.PERSONAL, pageable);

        // then
        assertThat(result.getAddresses()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getAddresses().get(0).getTitle()).isEqualTo("집");
    }

    @Test
    @DisplayName("주소 목록 조회 - 사용자가 존재하지 않으면 예외 발생")
    void getAddressesByUserAndType_UserNotFound() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        given(userRepository.existsById(UUID.fromString(userId))).willReturn(false);

        // when & then
        assertThatThrownBy(() -> addressService.getAddressesByUserAndType(
                userId, AddressType.PERSONAL, pageable))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다: " + userId);
    }

    @Test
    @DisplayName("전체 주소 목록 조회 - 성공")
    void getAllAddressesByUserAndType_Success() {
        // given
        List<Addresses> addresses = Arrays.asList(testAddress);

        given(userRepository.existsById(UUID.fromString(userId))).willReturn(true);
        given(addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                userId, AddressType.PERSONAL)).willReturn(addresses);

        // when
        List<AddressResponseDto> result = addressService.getAllAddressesByUserAndType(
                userId, AddressType.PERSONAL);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("집");
    }

    @Test
    @DisplayName("주소 상세 조회 - 성공")
    void getAddressById_Success() {
        // given
        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.of(testAddress));

        // when
        AddressResponseDto result = addressService.getAddressById(addressId, userId);

        // then
        assertThat(result.getId()).isEqualTo(addressId);
        assertThat(result.getTitle()).isEqualTo("집");
    }

    @Test
    @DisplayName("주소 상세 조회 - 주소가 존재하지 않으면 예외 발생")
    void getAddressById_AddressNotFound() {
        // given
        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> addressService.getAddressById(addressId, userId))
                .isInstanceOf(AddressNotFoundException.class)
                .hasMessage("주소를 찾을 수 없습니다: " + addressId);
    }

    @Test
    @DisplayName("주소 상세 조회 - 다른 사용자의 주소에 접근 시 예외 발생")
    void getAddressById_AccessDenied() {
        // given
        String otherUserId = UUID.randomUUID().toString();
        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.of(testAddress));

        // when & then
        assertThatThrownBy(() -> addressService.getAddressById(addressId, otherUserId))
                .isInstanceOf(AddressAccessDeniedException.class)
                .hasMessage("해당 주소에 접근할 권한이 없습니다.");
    }

    @Test
    @DisplayName("주소 생성 - 성공")
    void createAddress_Success() {
        // given
        AddressRequestDto requestDto = AddressRequestDto.builder()
                .title("새주소")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 456")
                .postalCode("06234")
                .detailAddress("789호")
                .phoneNumber("010-9999-8888")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();

        given(userRepository.findById(UUID.fromString(userId))).willReturn(Optional.of(testUser));
        given(addressRepository.countByUserIdAndAddressType(userId, AddressType.PERSONAL)).willReturn(5L);
        given(addressRepository.save(any(Addresses.class))).willReturn(testAddress);

        // when
        AddressResponseDto result = addressService.createAddress(requestDto, userId);

        // then
        assertThat(result.getTitle()).isEqualTo("집");
        then(addressRepository).should().save(any(Addresses.class));
    }

    @Test
    @DisplayName("주소 생성 - 기본 주소로 생성 시 기존 기본 주소 해제")
    void createAddress_WithDefault_ClearsPreviousDefault() {
        // given
        AddressRequestDto requestDto = AddressRequestDto.builder()
                .title("새주소")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 456")
                .postalCode("06234")
                .detailAddress("789호")
                .phoneNumber("010-9999-8888")
                .addressType(AddressType.PERSONAL)
                .isDefault(true)
                .build();

        given(userRepository.findById(UUID.fromString(userId))).willReturn(Optional.of(testUser));
        given(addressRepository.countByUserIdAndAddressType(userId, AddressType.PERSONAL)).willReturn(5L);
        given(addressRepository.save(any(Addresses.class))).willReturn(testAddress);
        willDoNothing().given(addressRepository).clearDefaultAddresses(userId, AddressType.PERSONAL);

        // when
        addressService.createAddress(requestDto, userId);

        // then
        then(addressRepository).should().clearDefaultAddresses(userId, AddressType.PERSONAL);
        then(addressRepository).should().save(any(Addresses.class));
    }

    @Test
    @DisplayName("주소 생성 - 개인주소 개수 제한 초과 시 예외 발생")
    void createAddress_ExceedsPersonalLimit() {
        // given
        AddressRequestDto requestDto = AddressRequestDto.builder()
                .addressType(AddressType.PERSONAL)
                .title("새주소")
                .city("서울")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 456")
                .postalCode("06234")
                .detailAddress("789호")
                .phoneNumber("010-9999-8888")
                .build();

        given(userRepository.findById(UUID.fromString(userId))).willReturn(Optional.of(testUser));
        given(addressRepository.countByUserIdAndAddressType(userId, AddressType.PERSONAL)).willReturn(10L);

        // when & then
        assertThatThrownBy(() -> addressService.createAddress(requestDto, userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("개인주소은(는) 최대 10개까지만 등록할 수 있습니다.");
    }

    @Test
    @DisplayName("주소 수정 - 성공")
    void updateAddress_Success() {
        // given
        AddressUpdateRequestDto updateDto = AddressUpdateRequestDto.builder()
                .title("수정된 제목")
                .city("부산광역시")
                .district("해운대구")
                .neighborhood("우동")
                .streetAddress("해운대로 100")
                .postalCode("48094")
                .detailAddress("201호")
                .phoneNumber("051-1234-5678")
                .isDefault(false)
                .build();

        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.of(testAddress));

        // when
        AddressResponseDto result = addressService.updateAddress(addressId, updateDto, userId);

        // then
        assertThat(result.getId()).isEqualTo(addressId);
    }

    @Test
    @DisplayName("주소 삭제 - 성공")
    void deleteAddress_Success() {
        // given
        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.of(testAddress));
        willDoNothing().given(addressRepository).delete(testAddress);

        // when
        addressService.deleteAddress(addressId, userId);

        // then
        then(addressRepository).should().delete(testAddress);
    }

    @Test
    @DisplayName("기본 주소 설정 - 성공")
    void setDefaultAddress_Success() {
        // given
        given(addressRepository.findById(UUID.fromString(addressId))).willReturn(Optional.of(testAddress));
        willDoNothing().given(addressRepository).clearDefaultAddresses(userId, AddressType.PERSONAL);

        // when
        AddressResponseDto result = addressService.setDefaultAddress(addressId, userId);

        // then
        assertThat(result.getId()).isEqualTo(addressId);
        then(addressRepository).should().clearDefaultAddresses(userId, AddressType.PERSONAL);
    }

    @Test
    @DisplayName("기본 주소 조회 - 성공")
    void getDefaultAddress_Success() {
        // given
        given(userRepository.existsById(UUID.fromString(userId))).willReturn(true);
        given(addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(userId, AddressType.PERSONAL))
                .willReturn(Optional.of(testAddress));

        // when
        AddressResponseDto result = addressService.getDefaultAddress(userId, AddressType.PERSONAL);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(addressId);
        assertThat(result.getTitle()).isEqualTo("집");
    }

    @Test
    @DisplayName("기본 주소 조회 - 기본 주소가 없는 경우 null 반환")
    void getDefaultAddress_NotFound() {
        // given
        given(userRepository.existsById(UUID.fromString(userId))).willReturn(true);
        given(addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(userId, AddressType.PERSONAL))
                .willReturn(Optional.empty());

        // when
        AddressResponseDto result = addressService.getDefaultAddress(userId, AddressType.PERSONAL);

        // then
        assertThat(result).isNull();
    }
}