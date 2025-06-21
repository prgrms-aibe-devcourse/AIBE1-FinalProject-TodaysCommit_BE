package com.team5.catdogeats.addresses.domain;

import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Addresses 엔티티 테스트")
class AddressesTest {

    private Users testUser;
    private Addresses testAddress;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .id(UUID.randomUUID().toString())
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test")
                .build();

        testAddress = Addresses.builder()
                .id(UUID.randomUUID().toString())
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
    @DisplayName("주소 생성 시 모든 필드가 올바르게 설정된다")
    void createAddress_AllFieldsSetCorrectly() {
        // then
        assertThat(testAddress.getTitle()).isEqualTo("집");
        assertThat(testAddress.getCity()).isEqualTo("서울특별시");
        assertThat(testAddress.getDistrict()).isEqualTo("강남구");
        assertThat(testAddress.getNeighborhood()).isEqualTo("역삼동");
        assertThat(testAddress.getStreetAddress()).isEqualTo("테헤란로 123");
        assertThat(testAddress.getPostalCode()).isEqualTo("06234");
        assertThat(testAddress.getDetailAddress()).isEqualTo("456호");
        assertThat(testAddress.getPhoneNumber()).isEqualTo("010-1234-5678");
        assertThat(testAddress.getAddressType()).isEqualTo(AddressType.PERSONAL);
        assertThat(testAddress.isDefault()).isFalse();
        assertThat(testAddress.getUser()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("주소 정보 업데이트가 올바르게 동작한다")
    void updateAddress_AllFieldsUpdatedCorrectly() {
        // when
        testAddress.updateAddress(
                "회사",
                "경기도",
                "성남시",
                "분당구",
                "판교역로 789",
                "13494",
                "101동 202호",
                "010-9876-5432"
        );

        // then
        assertThat(testAddress.getTitle()).isEqualTo("회사");
        assertThat(testAddress.getCity()).isEqualTo("경기도");
        assertThat(testAddress.getDistrict()).isEqualTo("성남시");
        assertThat(testAddress.getNeighborhood()).isEqualTo("분당구");
        assertThat(testAddress.getStreetAddress()).isEqualTo("판교역로 789");
        assertThat(testAddress.getPostalCode()).isEqualTo("13494");
        assertThat(testAddress.getDetailAddress()).isEqualTo("101동 202호");
        assertThat(testAddress.getPhoneNumber()).isEqualTo("010-9876-5432");
    }

    @Test
    @DisplayName("null 값으로 업데이트 시 기존 값이 유지된다")
    void updateAddress_WithNullValues_PreservesExistingValues() {
        // given
        String originalTitle = testAddress.getTitle();
        String originalCity = testAddress.getCity();

        // when
        testAddress.updateAddress(null, null, "새로운구", null, null, null, null, null);

        // then
        assertThat(testAddress.getTitle()).isEqualTo(originalTitle);
        assertThat(testAddress.getCity()).isEqualTo(originalCity);
        assertThat(testAddress.getDistrict()).isEqualTo("새로운구");
    }

    @Test
    @DisplayName("빈 문자열로 업데이트 시 기존 값이 유지된다")
    void updateAddress_WithEmptyStrings_PreservesExistingValues() {
        // given
        String originalTitle = testAddress.getTitle();
        String originalCity = testAddress.getCity();

        // when
        testAddress.updateAddress("", "  ", "새로운구", "", null, null, null, null);

        // then
        assertThat(testAddress.getTitle()).isEqualTo(originalTitle);
        assertThat(testAddress.getCity()).isEqualTo(originalCity);
        assertThat(testAddress.getDistrict()).isEqualTo("새로운구");
    }

    @Test
    @DisplayName("기본 주소 설정이 올바르게 동작한다")
    void setAsDefault_SetsDefaultToTrue() {
        // given
        assertThat(testAddress.isDefault()).isFalse();

        // when
        testAddress.setAsDefault();

        // then
        assertThat(testAddress.isDefault()).isTrue();
    }

    @Test
    @DisplayName("기본 주소 해제가 올바르게 동작한다")
    void removeDefault_SetsDefaultToFalse() {
        // given
        testAddress.setAsDefault();
        assertThat(testAddress.isDefault()).isTrue();

        // when
        testAddress.removeDefault();

        // then
        assertThat(testAddress.isDefault()).isFalse();
    }

    @Test
    @DisplayName("주소 소유자 확인이 올바르게 동작한다")
    void isOwnedBy_WithCorrectUserId_ReturnsTrue() {
        // when & then
        assertThat(testAddress.isOwnedBy(UUID.fromString(testUser.getId()))).isTrue();
    }

    @Test
    @DisplayName("다른 사용자 ID로 소유자 확인 시 false를 반환한다")
    void isOwnedBy_WithDifferentUserId_ReturnsFalse() {
        // given
        UUID differentUserId = UUID.randomUUID();

        // when & then
        assertThat(testAddress.isOwnedBy(differentUserId)).isFalse();
    }

    @Test
    @DisplayName("사용자가 null인 경우 소유자 확인 시 false를 반환한다")
    void isOwnedBy_WithNullUser_ReturnsFalse() {
        // given
        Addresses addressWithNullUser = Addresses.builder()
                .id(UUID.randomUUID().toString())
                .user(null)
                .title("테스트")
                .city("서울")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테스트로 123")
                .postalCode("12345")
                .detailAddress("테스트")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();

        // when & then
        assertThat(addressWithNullUser.isOwnedBy(UUID.randomUUID())).isFalse();
    }

    @Test
    @DisplayName("전체 주소 문자열이 올바르게 반환된다")
    void getFullAddress_ReturnsCorrectFormat() {
        // when
        String fullAddress = testAddress.getFullAddress();

        // then
        assertThat(fullAddress).isEqualTo("서울특별시 강남구 역삼동 테헤란로 123 456호");
    }

    @Test
    @DisplayName("우편번호 포함 전체 주소 문자열이 올바르게 반환된다")
    void getFullAddressWithPostalCode_ReturnsCorrectFormat() {
        // when
        String fullAddressWithPostalCode = testAddress.getFullAddressWithPostalCode();

        // then
        assertThat(fullAddressWithPostalCode).isEqualTo("(06234) 서울특별시 강남구 역삼동 테헤란로 123 456호");
    }
}