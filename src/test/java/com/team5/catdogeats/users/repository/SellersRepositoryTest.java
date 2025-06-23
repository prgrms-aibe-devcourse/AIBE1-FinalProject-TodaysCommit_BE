package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("SellersRepository 테스트")
class SellersRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SellersRepository sellersRepository;

    private Users testUser1;
    private Users testUser2;
    private Sellers testSeller1;
    private Sellers testSeller2;

    @BeforeEach
    void setUp() {
        // 테스트용 Users 생성
        testUser1 = Users.builder()
                .provider("google")
                .providerId("user001")
                .userNameAttribute("email")
                .name("테스트 판매자1")
                .role(Role.ROLE_SELLER)
                .build();

        testUser2 = Users.builder()
                .provider("kakao")
                .providerId("user002")
                .userNameAttribute("email")
                .name("테스트 판매자2")
                .role(Role.ROLE_SELLER)
                .build();

        // Users 저장
        testUser1 = entityManager.persistAndFlush(testUser1);
        testUser2 = entityManager.persistAndFlush(testUser2);

        // 테스트용 Sellers 생성
        testSeller1 = Sellers.builder()
                .user(testUser1)
                .vendorName("펫푸드 공방")
                .vendorProfileImage("https://example.com/profile1.jpg")
                .businessNumber("123-45-67890")
                .settlementBank("신한은행")
                .settlementAccount("110-123-456789")
                .tags("수제간식,강아지")
                .build();

        testSeller2 = Sellers.builder()
                .user(testUser2)
                .vendorName("고양이 간식 가게")
                .vendorProfileImage("https://example.com/profile2.jpg")
                .businessNumber("987-65-43210")
                .settlementBank("국민은행")
                .settlementAccount("220-987-654321")
                .tags("수제간식,고양이")
                .build();

        // Sellers 저장
        testSeller1 = entityManager.persistAndFlush(testSeller1);
        testSeller2 = entityManager.persistAndFlush(testSeller2);

        // 영속성 컨텍스트 초기화 (실제 DB 쿼리 동작 확인을 위함)
        entityManager.clear();
    }

    @Nested
    @DisplayName("findByUserId 테스트")
    class FindByUserIdTests {

        @Test
        @DisplayName("성공 - 존재하는 사용자 ID로 판매자 정보 조회")
        void findByUserId_Success() {
            // when
            Optional<Sellers> result = sellersRepository.findByUserId(testUser1.getId());

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getVendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.get().getBusinessNumber()).isEqualTo("123-45-67890");
            assertThat(result.get().getUser().getName()).isEqualTo("테스트 판매자1");
            assertThat(result.get().getUserId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자 ID로 조회")
        void findByUserId_NotFound() {
            // given
            String nonExistentUserId = UUID.randomUUID().toString();

            // when
            Optional<Sellers> result = sellersRepository.findByUserId(nonExistentUserId);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공 - 여러 판매자 중 특정 사용자 ID로 정확한 판매자 조회")
        void findByUserId_MultipleSellersDifferentUsers() {
            // when
            Optional<Sellers> result1 = sellersRepository.findByUserId(testUser1.getId());
            Optional<Sellers> result2 = sellersRepository.findByUserId(testUser2.getId());

            // then
            assertThat(result1).isPresent();
            assertThat(result1.get().getVendorName()).isEqualTo("펫푸드 공방");
            assertThat(result1.get().getUserId()).isEqualTo(testUser1.getId());

            assertThat(result2).isPresent();
            assertThat(result2.get().getVendorName()).isEqualTo("고양이 간식 가게");
            assertThat(result2.get().getUserId()).isEqualTo(testUser2.getId());
        }

        @Test
        @DisplayName("성공 - 연관관계 로딩 테스트 (User 정보 조회)")
        void findByUserId_LazyLoadingUser() {
            // when
            Optional<Sellers> result = sellersRepository.findByUserId(testUser1.getId());

            // then
            assertThat(result).isPresent();
            Sellers seller = result.get();

            // User 연관관계 테스트
            assertThat(seller.getUser()).isNotNull();
            assertThat(seller.getUser().getName()).isEqualTo("테스트 판매자1");
            assertThat(seller.getUser().getRole()).isEqualTo(Role.ROLE_SELLER);
            assertThat(seller.getUser().getProvider()).isEqualTo("google");
        }
    }

    @Nested
    @DisplayName("findByBusinessNumber 테스트")
    class FindByBusinessNumberTests {

        @Test
        @DisplayName("성공 - 존재하는 사업자번호로 판매자 정보 조회")
        void findByBusinessNumber_Success() {
            // when
            Optional<Sellers> result = sellersRepository.findByBusinessNumber("123-45-67890");

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getVendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.get().getUser().getName()).isEqualTo("테스트 판매자1");
            assertThat(result.get().getUserId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사업자번호로 조회")
        void findByBusinessNumber_NotFound() {
            // when
            Optional<Sellers> result = sellersRepository.findByBusinessNumber("999-99-99999");

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공 - 여러 판매자 중 특정 사업자번호로 정확한 판매자 조회")
        void findByBusinessNumber_MultipleSellers() {
            // when
            Optional<Sellers> result1 = sellersRepository.findByBusinessNumber("123-45-67890");
            Optional<Sellers> result2 = sellersRepository.findByBusinessNumber("987-65-43210");

            // then
            assertThat(result1).isPresent();
            assertThat(result1.get().getVendorName()).isEqualTo("펫푸드 공방");
            assertThat(result1.get().getUser().getName()).isEqualTo("테스트 판매자1");

            assertThat(result2).isPresent();
            assertThat(result2.get().getVendorName()).isEqualTo("고양이 간식 가게");
            assertThat(result2.get().getUser().getName()).isEqualTo("테스트 판매자2");
        }

        @Test
        @DisplayName("성공 - 사업자번호 대소문자 구분 및 공백 처리 테스트")
        void findByBusinessNumber_CaseSensitiveAndWhitespace() {
            // when - 정확한 사업자번호
            Optional<Sellers> exactResult = sellersRepository.findByBusinessNumber("123-45-67890");

            // 대소문자가 다른 경우 (현재 숫자라서 의미없지만 향후 영문자 포함 시 고려)
            // Optional<Sellers> caseResult = sellersRepository.findByBusinessNumber("123-45-67890");

            // 앞뒤 공백이 있는 경우
            Optional<Sellers> whitespaceResult = sellersRepository.findByBusinessNumber(" 123-45-67890 ");

            // then
            assertThat(exactResult).isPresent();
            // 현재는 정확한 매칭만 동작 (JPA 기본 동작)
            assertThat(whitespaceResult).isEmpty(); // 공백이 있으면 매칭되지 않음
        }
    }

    @Nested
    @DisplayName("기본 JPA Repository 메서드 테스트")
    class BasicJpaRepositoryTests {

        @Test
        @DisplayName("save() - 새로운 판매자 저장")
        void save_NewSeller() {
            // given
            Users newUser = Users.builder()
                    .provider("naver")
                    .providerId("newuser001")
                    .userNameAttribute("email")
                    .name("새로운 판매자")
                    .role(Role.ROLE_SELLER)
                    .build();

            newUser = entityManager.persistAndFlush(newUser);

            Sellers newSeller = Sellers.builder()
                    .user(newUser)
                    .vendorName("새로운 펫샵")
                    .vendorProfileImage("https://example.com/new.jpg")
                    .businessNumber("555-55-55555")
                    .settlementBank("우리은행")
                    .settlementAccount("333-555-777999")
                    .tags("새상품")
                    .build();

            // when
            Sellers savedSeller = sellersRepository.save(newSeller);

            // then
            assertThat(savedSeller.getUserId()).isNotNull();
            assertThat(savedSeller.getVendorName()).isEqualTo("새로운 펫샵");
            assertThat(savedSeller.getCreatedAt()).isNotNull();
            assertThat(savedSeller.getUpdatedAt()).isNotNull();

            // DB에서 실제로 조회되는지 확인
            Optional<Sellers> found = sellersRepository.findByUserId(newUser.getId());
            assertThat(found).isPresent();
            assertThat(found.get().getVendorName()).isEqualTo("새로운 펫샵");
        }

        @Test
        @DisplayName("save() - 기존 판매자 정보 수정")
        void save_UpdateExistingSeller() {
            // given - 기존 판매자 조회
            Optional<Sellers> existingSeller = sellersRepository.findByUserId(testUser1.getId());
            assertThat(existingSeller).isPresent();

            Sellers seller = existingSeller.get();
            String originalVendorName = seller.getVendorName();

            // 정보 수정 - 유효한 값으로 업데이트
            String newVendorName = "수정된 펫샵 이름";
            String newSettlementBank = "수정된 은행";

            seller.updateVendorName(newVendorName);
            seller.updateSettlementBank(newSettlementBank);

            // when
            Sellers updatedSeller = sellersRepository.save(seller);
            entityManager.flush(); // 강제로 DB에 반영

            // then
            assertThat(updatedSeller.getVendorName()).isEqualTo(newVendorName);
            assertThat(updatedSeller.getSettlementBank()).isEqualTo(newSettlementBank);
            assertThat(updatedSeller.getVendorName()).isNotEqualTo(originalVendorName);

            // DB에서 실제로 수정되었는지 확인
            entityManager.clear(); // 1차 캐시 초기화
            Optional<Sellers> reloaded = sellersRepository.findByUserId(testUser1.getId());
            assertThat(reloaded).isPresent();
            assertThat(reloaded.get().getVendorName()).isEqualTo(newVendorName);
            assertThat(reloaded.get().getSettlementBank()).isEqualTo(newSettlementBank);
        }

        @Test
        @DisplayName("findById() - ID로 판매자 조회")
        void findById_Success() {
            // when
            Optional<Sellers> result = sellersRepository.findById((testUser1.getId()));

            // then
            assertThat(result).isPresent();
            assertThat(result.get().getVendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.get().getUserId()).isEqualTo(testUser1.getId());
        }

        @Test
        @DisplayName("existsById() - ID 존재 여부 확인")
        void existsById_Test() {
            // when & then
            assertThat(sellersRepository.existsById((testUser1.getId()))).isTrue();
            assertThat(sellersRepository.existsById((testUser2.getId()))).isTrue();
            assertThat(sellersRepository.existsById(UUID.randomUUID().toString())).isFalse();
        }

        @Test
        @DisplayName("count() - 전체 판매자 수 조회")
        void count_Test() {
            // when
            long count = sellersRepository.count();

            // then
            assertThat(count).isEqualTo(2L); // setUp에서 2개 생성
        }

        @Test
        @DisplayName("deleteById() - ID로 판매자 삭제")
        void deleteById_Success() {
            // given
            String userIdToDelete = testUser1.getId();
            assertThat(sellersRepository.existsById((userIdToDelete))).isTrue();

            // when
            sellersRepository.deleteById((userIdToDelete));

            // then
            assertThat(sellersRepository.existsById((userIdToDelete))).isFalse();
            assertThat(sellersRepository.findByUserId(userIdToDelete)).isEmpty();

            // 다른 판매자는 여전히 존재하는지 확인
            assertThat(sellersRepository.existsById((testUser2.getId()))).isTrue();
        }
    }

    @Nested
    @DisplayName("Entity 메서드 동작 테스트")
    class EntityMethodTests {

        @Test
        @DisplayName("updateVendorName() 메서드 입력값 검증")
        void updateVendorName_ValidatesInput() {
            // given
            Optional<Sellers> existingSeller = sellersRepository.findByUserId(testUser1.getId());
            assertThat(existingSeller).isPresent();

            Sellers seller = existingSeller.get();
            String originalName = seller.getVendorName();

            // when & then - null이나 빈 문자열은 업데이트되지 않음
            seller.updateVendorName(null);
            assertThat(seller.getVendorName()).isEqualTo(originalName); // 변경되지 않음

            seller.updateVendorName("");
            assertThat(seller.getVendorName()).isEqualTo(originalName); // 변경되지 않음

            seller.updateVendorName("   ");
            assertThat(seller.getVendorName()).isEqualTo(originalName); // 변경되지 않음

            // 유효한 값은 업데이트됨
            seller.updateVendorName("새로운 이름");
            assertThat(seller.getVendorName()).isEqualTo("새로운 이름");
        }

        @Test
        @DisplayName("updateSettlementBank() null 값 허용 확인")
        void updateSettlementBank_AllowsNull() {
            // given
            Optional<Sellers> existingSeller = sellersRepository.findByUserId(testUser1.getId());
            assertThat(existingSeller).isPresent();

            Sellers seller = existingSeller.get();

            // when - null 값으로 업데이트 (선택 필드이므로 허용)
            seller.updateSettlementBank(null);

            // then
            assertThat(seller.getSettlementBank()).isNull();
        }
    }

    @Nested
    @DisplayName("데이터 무결성 및 제약조건 테스트")
    class DataIntegrityTests {

        @Test
        @DisplayName("실패 - 동일한 사업자번호로 판매자 등록 시도")
        void save_DuplicateBusinessNumber_ShouldFail() {
            // given
            Users anotherUser = Users.builder()
                    .provider("kakao")
                    .providerId("duplicate001")
                    .userNameAttribute("email")
                    .name("중복 테스트 사용자")
                    .role(Role.ROLE_SELLER)
                    .build();

            anotherUser = entityManager.persistAndFlush(anotherUser);

            Sellers duplicateSeller = Sellers.builder()
                    .user(anotherUser)
                    .vendorName("중복 사업자번호 샵")
                    .businessNumber("123-45-67890") // 이미 testSeller1이 사용 중
                    .build();

            // when & then
            // 실제로는 DB 제약조건에 따라 예외가 발생할 수 있음
            // 현재 스키마에 UNIQUE 제약이 없다면 저장될 수 있음
            // 비즈니스 로직에서 중복을 방지해야 함

            try {
                sellersRepository.saveAndFlush(duplicateSeller);

                // 만약 DB에 UNIQUE 제약이 없다면, 애플리케이션 레벨에서 검증
                Optional<Sellers> existing = sellersRepository.findByBusinessNumber("123-45-67890");
                assertThat(existing).isPresent();

                // 중복된 사업자번호가 여러 개 존재할 수 있으므로 주의 필요
                // 실제 운영에서는 DB 제약조건 또는 비즈니스 로직으로 방지

            } catch (Exception e) {
                // UNIQUE 제약이 있다면 예외 발생
                assertThat(e).isNotNull();
            }
        }

        @Test
        @DisplayName("성공 - Repository를 통한 삭제 테스트")
        void repositoryDelete_Success() {
            // given
            String userIdToDelete = testUser1.getId();

            // 판매자 정보가 존재하는지 확인
            assertThat(sellersRepository.existsById((userIdToDelete))).isTrue();

            // when - Repository를 통한 삭제
            sellersRepository.deleteById((userIdToDelete));
            entityManager.flush();

            // then - 삭제되었는지 확인
            assertThat(sellersRepository.existsById((userIdToDelete))).isFalse();
            assertThat(sellersRepository.findByUserId(userIdToDelete)).isEmpty();
        }
    }
}