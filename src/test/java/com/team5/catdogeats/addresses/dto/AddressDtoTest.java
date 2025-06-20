package com.team5.catdogeats.addresses.dto;

import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Address DTO 테스트")
class AddressDtoTest {

    private Validator validator;
    private Users testUser;
    private Addresses testAddress;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();

        testUser = Users.builder()
                .id(UUID.randomUUID())
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();

        testAddress = Addresses.builder()
                .id(UUID.randomUUID())
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
    @DisplayName("AddressRequestDto - 유효한 데이터로 생성 시 검증 오류가 없다")
    void addressRequestDto_validData_NoValidationErrors() {
        // given
        AddressRequestDto dto = AddressRequestDto.builder()
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

        // when
        Set<ConstraintViolation<AddressRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AddressRequestDto - 필수 필드가 null인 경우 검증 오류가 발생한다")
    void addressRequestDto_nullRequiredFields_ValidationErrors() {
        // given
        AddressRequestDto dto = AddressRequestDto.builder()
                .title(null)
                .city(null)
                .district(null)
                .neighborhood(null)
                .streetAddress(null)
                .postalCode(null)
                .detailAddress(null)
                .phoneNumber(null)
                .addressType(null)
                .build();

        // when
        Set<ConstraintViolation<AddressRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).hasSize(9); // 8개 NotBlank + 1개 NotNull
    }

    @Test
    @DisplayName("AddressUpdateRequestDto - 모든 필드가 null인 경우에도 검증 오류가 없다")
    void addressUpdateRequestDto_allNullFields_NoValidationErrors() {
        // given
        AddressUpdateRequestDto dto = AddressUpdateRequestDto.builder().build();

        // when
        Set<ConstraintViolation<AddressUpdateRequestDto>> violations = validator.validate(dto);

        // then
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("AddressUpdateRequestDto - 헬퍼 메서드들이 올바르게 동작한다")
    void addressUpdateRequestDto_helperMethods_WorkCorrectly() {
        // given
        AddressUpdateRequestDto dto = AddressUpdateRequestDto.builder()
                .title("제목")
                .city(null)
                .district("")
                .neighborhood("  ")
                .streetAddress("도로명")
                .phoneNumber("010-1234-5678")
                .isDefault(true)
                .build();

        // when & then
        assertThat(dto.hasTitle()).isTrue();
        assertThat(dto.hasCity()).isFalse();
        assertThat(dto.hasDistrict()).isFalse();
        assertThat(dto.hasNeighborhood()).isFalse();
        assertThat(dto.hasStreetAddress()).isTrue();
        assertThat(dto.hasPhoneNumber()).isTrue();
        assertThat(dto.hasIsDefault()).isTrue();
    }

    @Test
    @DisplayName("AddressResponseDto - Addresses 엔티티로부터 올바르게 생성된다")
    void addressResponseDto_fromEntity_CreatesCorrectDto() {
        // when
        AddressResponseDto dto = AddressResponseDto.from(testAddress);

        // then
        assertThat(dto.getId()).isEqualTo(testAddress.getId());
        assertThat(dto.getTitle()).isEqualTo(testAddress.getTitle());
        assertThat(dto.getCity()).isEqualTo(testAddress.getCity());
        assertThat(dto.getDistrict()).isEqualTo(testAddress.getDistrict());
        assertThat(dto.getNeighborhood()).isEqualTo(testAddress.getNeighborhood());
        assertThat(dto.getStreetAddress()).isEqualTo(testAddress.getStreetAddress());
        assertThat(dto.getPostalCode()).isEqualTo(testAddress.getPostalCode());
        assertThat(dto.getDetailAddress()).isEqualTo(testAddress.getDetailAddress());
        assertThat(dto.getPhoneNumber()).isEqualTo(testAddress.getPhoneNumber());
        assertThat(dto.getAddressType()).isEqualTo(testAddress.getAddressType());
        assertThat(dto.isDefault()).isEqualTo(testAddress.isDefault());
    }

    @Test
    @DisplayName("AddressResponseDto - 전체 주소 문자열이 올바르게 반환된다")
    void addressResponseDto_getFullAddress_ReturnsCorrectFormat() {
        // given
        AddressResponseDto dto = AddressResponseDto.from(testAddress);

        // when
        String fullAddress = dto.getFullAddress();

        // then
        assertThat(fullAddress).isEqualTo("서울특별시 강남구 역삼동 테헤란로 123 456호");
    }

    @Test
    @DisplayName("AddressListResponseDto - List 객체로부터 올바르게 생성된다")
    void addressListResponseDto_fromList_CreatesCorrectDto() {
        // given
        List<AddressResponseDto> addresses = Arrays.asList(
                AddressResponseDto.from(testAddress)
        );

        // when
        AddressListResponseDto dto = AddressListResponseDto.from(addresses);

        // then
        assertThat(dto.getAddresses()).hasSize(1);
        assertThat(dto.getTotalElements()).isEqualTo(1);
        assertThat(dto.getTotalPages()).isEqualTo(1);
        assertThat(dto.getCurrentPage()).isEqualTo(0);
        assertThat(dto.getPageSize()).isEqualTo(1);
        assertThat(dto.isHasNext()).isFalse();
        assertThat(dto.isHasPrevious()).isFalse();
    }
}