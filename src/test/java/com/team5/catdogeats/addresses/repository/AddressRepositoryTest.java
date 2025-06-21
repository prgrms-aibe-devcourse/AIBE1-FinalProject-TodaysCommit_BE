package com.team5.catdogeats.addresses.repository;

import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("AddressRepository 테스트")
class AddressRepositoryTest {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    private Users testUser1;
    private Users testUser2;
    private Addresses personalAddress1;
    private Addresses personalAddress2;
    private Addresses businessAddress1;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .name("테스트사용자1")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test1")
                .provider("test")
                .providerId("test1")
                .build();

        testUser2 = Users.builder()
                .name("테스트사용자2")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .userNameAttribute("test2")
                .provider("test")
                .providerId("test2")
                .build();

        userRepository.save(testUser1);
        userRepository.save(testUser2);

        // 테스트 주소 생성
        personalAddress1 = Addresses.builder()
                .user(testUser1)
                .title("집")
                .city("서울특별시")
                .district("강남구")
                .neighborhood("역삼동")
                .streetAddress("테헤란로 123")
                .postalCode("06234")
                .detailAddress("456호")
                .phoneNumber("010-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(true)
                .build();

        personalAddress2 = Addresses.builder()
                .user(testUser1)
                .title("회사")
                .city("경기도")
                .district("성남시")
                .neighborhood("분당구")
                .streetAddress("판교역로 789")
                .postalCode("13494")
                .detailAddress("101동 202호")
                .phoneNumber("010-9876-5432")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();

        businessAddress1 = Addresses.builder()
                .user(testUser2)
                .title("본사")
                .city("서울특별시")
                .district("서초구")
                .neighborhood("서초동")
                .streetAddress("서초대로 333")
                .postalCode("06621")
                .detailAddress("A동 1층")
                .phoneNumber("02-1234-5678")
                .addressType(AddressType.BUSINESS)
                .isDefault(true)
                .build();

        addressRepository.saveAll(List.of(personalAddress1, personalAddress2, businessAddress1));
    }

    @Test
    @DisplayName("사용자별 주소 타입에 따른 주소 목록 조회 - 페이징")
    void findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc_WithPaging() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Addresses> result = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                testUser1.getId(), AddressType.PERSONAL, pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).isDefault()).isTrue(); // 기본 주소가 먼저
        assertThat(result.getContent().get(1).isDefault()).isFalse();
    }

    @Test
    @DisplayName("사용자별 주소 타입에 따른 주소 목록 조회 - 전체")
    void findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc_All() {
        // when
        List<Addresses> result = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                testUser1.getId(), AddressType.PERSONAL);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).isDefault()).isTrue(); // 기본 주소가 먼저
        assertThat(result.get(1).isDefault()).isFalse();
    }

    @Test
    @DisplayName("사용자의 기본 주소 조회")
    void findByUserIdAndAddressTypeAndIsDefaultTrue() {
        // when
        Optional<Addresses> result = addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(
                testUser1.getId(), AddressType.PERSONAL);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getTitle()).isEqualTo("집");
        assertThat(result.get().isDefault()).isTrue();
    }

    @Test
    @DisplayName("기본 주소가 없는 경우 빈 Optional 반환")
    void findByUserIdAndAddressTypeAndIsDefaultTrue_NoDefault() {
        // given
        personalAddress1.removeDefault();
        addressRepository.save(personalAddress1);

        // when
        Optional<Addresses> result = addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(
                testUser1.getId(), AddressType.PERSONAL);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("특정 주소가 해당 사용자의 것인지 확인")
    void existsByIdAndUserId() {
        // when & then
        assertThat(addressRepository.existsByIdAndUserId(personalAddress1.getId(), testUser1.getId())).isTrue();
        assertThat(addressRepository.existsByIdAndUserId(personalAddress1.getId(), testUser2.getId())).isFalse();
        assertThat(addressRepository.existsByIdAndUserId(UUID.randomUUID().toString(), testUser1.getId())).isFalse();
    }

    @Test
    @DisplayName("사용자의 주소 개수 조회")
    void countByUserIdAndAddressType() {
        // when
        long personalCount = addressRepository.countByUserIdAndAddressType(testUser1.getId(), AddressType.PERSONAL);
        long businessCount = addressRepository.countByUserIdAndAddressType(testUser1.getId(), AddressType.BUSINESS);
        long user2BusinessCount = addressRepository.countByUserIdAndAddressType(testUser2.getId(), AddressType.BUSINESS);

        // then
        assertThat(personalCount).isEqualTo(2);
        assertThat(businessCount).isEqualTo(0);
        assertThat(user2BusinessCount).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자의 모든 기본 주소를 비활성화")
    void clearDefaultAddresses() {
        // given
        assertThat(personalAddress1.isDefault()).isTrue();

        // when
        addressRepository.clearDefaultAddresses(testUser1.getId(), AddressType.PERSONAL);
        addressRepository.flush(); // 변경사항 즉시 반영

        // then
        Addresses updatedAddress = addressRepository.findById(UUID.fromString(personalAddress1.getId())).orElseThrow();
        assertThat(updatedAddress.isDefault()).isFalse();
    }

    @Test
    @DisplayName("특정 주소를 기본 주소로 설정")
    void setAsDefault() {
        // given
        assertThat(personalAddress2.isDefault()).isFalse();

        // when
        addressRepository.setAsDefault(personalAddress2.getId());
        addressRepository.flush(); // 변경사항 즉시 반영

        // then
        Addresses updatedAddress = addressRepository.findById(UUID.fromString(personalAddress2.getId())).orElseThrow();
        assertThat(updatedAddress.isDefault()).isTrue();
    }

    @Test
    @DisplayName("기본 주소 변경 시나리오 테스트")
    void defaultAddressChangeScenario() {
        // given - 초기 상태: personalAddress1이 기본 주소
        assertThat(personalAddress1.isDefault()).isTrue();
        assertThat(personalAddress2.isDefault()).isFalse();

        // when - personalAddress2를 기본 주소로 변경
        addressRepository.clearDefaultAddresses(testUser1.getId(), AddressType.PERSONAL);
        addressRepository.setAsDefault(personalAddress2.getId());
        addressRepository.flush();

        // then
        Addresses updatedAddress1 = addressRepository.findById(UUID.fromString(personalAddress1.getId())).orElseThrow();
        Addresses updatedAddress2 = addressRepository.findById(UUID.fromString(personalAddress2.getId())).orElseThrow();

        assertThat(updatedAddress1.isDefault()).isFalse();
        assertThat(updatedAddress2.isDefault()).isTrue();

        // 기본 주소 조회로 검증
        Optional<Addresses> defaultAddress = addressRepository.findByUserIdAndAddressTypeAndIsDefaultTrue(
                testUser1.getId(), AddressType.PERSONAL);

        assertThat(defaultAddress).isPresent();
        assertThat(defaultAddress.get().getId()).isEqualTo(personalAddress2.getId());
    }

    @Test
    @DisplayName("다른 주소 타입의 기본 주소는 영향받지 않음")
    void clearDefaultAddresses_DoesNotAffectDifferentAddressType() {
        // given
        assertThat(businessAddress1.isDefault()).isTrue();

        // when - PERSONAL 타입의 기본 주소만 해제
        addressRepository.clearDefaultAddresses(testUser2.getId(), AddressType.PERSONAL);
        addressRepository.flush();

        // then - BUSINESS 타입의 기본 주소는 유지
        Addresses updatedBusinessAddress = addressRepository.findById(UUID.fromString(businessAddress1.getId())).orElseThrow();
        assertThat(updatedBusinessAddress.isDefault()).isTrue();
    }

    @Test
    @DisplayName("주소 타입별 정렬 확인 - 기본주소 우선, 최신순")
    void orderByIsDefaultDescCreatedAtDesc() {
        // given - 추가 주소 생성 (기본주소 아님)
        Addresses additionalAddress = Addresses.builder()
                .user(testUser1)
                .title("부모님댁")
                .city("부산광역시")
                .district("해운대구")
                .neighborhood("우동")
                .streetAddress("해운대로 100")
                .postalCode("48094")
                .detailAddress("201호")
                .phoneNumber("051-1234-5678")
                .addressType(AddressType.PERSONAL)
                .isDefault(false)
                .build();
        addressRepository.save(additionalAddress);

        // when
        List<Addresses> addresses = addressRepository.findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
                testUser1.getId(), AddressType.PERSONAL);

        // then
        assertThat(addresses).hasSize(3);
        assertThat(addresses.get(0).isDefault()).isTrue(); // 기본 주소가 첫 번째
        assertThat(addresses.get(1).isDefault()).isFalse();
        assertThat(addresses.get(2).isDefault()).isFalse();

        // 생성 시간 확인 (최신순)
        assertThat(addresses.get(1).getCreatedAt()).isAfterOrEqualTo(addresses.get(2).getCreatedAt());
    }
}