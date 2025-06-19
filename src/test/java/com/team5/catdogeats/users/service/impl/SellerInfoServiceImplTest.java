// 1. 수정된 SellerInfoServiceImplTest.java
package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerInfoServiceImplTest {

    @InjectMocks
    private SellerInfoServiceImpl sellerInfoService;

    @Mock
    private SellersRepository sellersRepository;

    @Mock
    private UserRepository userRepository;

    private UUID testUserId;
    private UUID otherUserId;
    private Users testSellerUser;
    private Users testBuyerUser;
    private Sellers testSeller;
    private Sellers otherSeller;
    private SellerInfoRequest testRequest;

    @BeforeEach
    void setUp() {
        testUserId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        otherUserId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        // 판매자 사용자
        testSellerUser = Users.builder()
                .id(testUserId)
                .provider("test")
                .providerId("seller001")
                .userNameAttribute("test")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();

        // 구매자 사용자 (권한 테스트용)
        testBuyerUser = Users.builder()
                .id(testUserId)
                .provider("test")
                .providerId("buyer001")
                .userNameAttribute("test")
                .name("테스트 구매자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .build();

        // 다른 판매자 사용자 (중복 테스트용)
        Users otherSellerUser = Users.builder()
                .id(otherUserId)
                .provider("test")
                .providerId("seller002")
                .userNameAttribute("test")
                .name("다른 판매자")
                .role(Role.ROLE_SELLER)
                .accountDisable(false)
                .build();

        // 테스트 판매자 정보
        testSeller = Sellers.builder()
                .userId(testUserId)
                .user(testSellerUser)
                .vendorName("펫푸드 공방")
                .vendorProfileImage("https://example.com/logo.jpg")
                .businessNumber("123-45-67890")
                .settlementBank("신한은행")
                .settlementAccount("110-123-456789")
                .tags("수제간식")
                .operatingStartTime(LocalTime.of(9, 0))
                .operatingEndTime(LocalTime.of(18, 0))
                .closedDays("월요일,화요일")
                .build();

        // 다른 판매자 정보
        otherSeller = Sellers.builder()
                .userId(otherUserId)
                .user(otherSellerUser)
                .vendorName("다른 공방")
                .businessNumber("123-45-67890") // 같은 사업자번호
                .build();

        testRequest = new SellerInfoRequest(
                "펫푸드 공방",                          // vendorName
                "https://example.com/logo.jpg",        // vendorProfileImage
                "123-45-67890",                        // businessNumber
                "신한은행",                             // settlementBank
                "110-123-456789",                      // settlementAcc
                "수제간식",                             // tags
                LocalTime.of(9, 0),                    // operatingStartTime
                LocalTime.of(18, 0),                   // operatingEndTime
                "월요일,화요일"                              // closedDays
        );
    }

    @Nested
    @DisplayName("판매자 정보 조회 테스트")
    class GetSellerInfoTests {

        @Test
        @DisplayName("성공 - 등록된 판매자 정보가 있는 경우")
        void getSellerInfo_Success() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));

            // when
            SellerInfoResponse result = sellerInfoService.getSellerInfo(testUserId);

            // then
            assertThat(result).isNotNull();
            // Record의 getter 메서드 사용 (vendorName() 방식)
            assertThat(result.vendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.businessNumber()).isEqualTo("123-45-67890");
            assertThat(result.userId()).isEqualTo(testUserId.toString());
            assertThat(result.vendorProfileImage()).isEqualTo("https://example.com/logo.jpg");
            assertThat(result.settlementBank()).isEqualTo("신한은행");
            assertThat(result.settlementAcc()).isEqualTo("110-123-456789");
            assertThat(result.tags()).isEqualTo("수제간식");
            assertThat(result.operatingStartTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.operatingEndTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(result.closedDays()).isEqualTo("월요일,화요일");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("성공 - 등록된 판매자 정보가 없는 경우 (null 반환)")
        void getSellerInfo_NoSellerInfo_ReturnsNull() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());

            // when
            SellerInfoResponse result = sellerInfoService.getSellerInfo(testUserId);

            // then
            assertThat(result).isNull();

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void getSellerInfo_UserNotFound() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerInfoService.getSellerInfo(testUserId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("실패 - 판매자 권한이 없는 사용자")
        void getSellerInfo_AccessDenied() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testBuyerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.getSellerInfo(testUserId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("판매자 권한이 필요합니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("판매자 정보 등록/수정 테스트")
    class UpsertSellerInfoTests {

        @Test
        @DisplayName("성공 - 신규 판매자 정보 등록")
        void upsertSellerInfo_CreateNew_Success() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, testRequest);

            // then
            assertThat(result).isNotNull();

            assertThat(result.vendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.businessNumber()).isEqualTo("123-45-67890");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository).findByUserId(testUserId);
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 기존 판매자 정보 수정")
        void upsertSellerInfo_UpdateExisting_Success() {
            // given
            String newVendorName = "수정된 공방명";
            SellerInfoRequest updateRequest = new SellerInfoRequest(
                    newVendorName,
                    testRequest.vendorProfileImage(),
                    testRequest.businessNumber(),
                    testRequest.settlementBank(),
                    testRequest.settlementAcc(),
                    testRequest.tags(),
                    testRequest.operatingStartTime(),
                    testRequest.operatingEndTime(),
                    testRequest.closedDays()
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(testSeller));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, updateRequest);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository).findByUserId(testUserId);
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 자신의 사업자번호로 다시 등록 (중복이 아님)")
        void upsertSellerInfo_SameUserBusinessNumber_Success() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(testSeller)); // 자신의 정보
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, testRequest);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository).findByUserId(testUserId);
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("실패 - 다른 사용자가 사용 중인 사업자번호")
        void upsertSellerInfo_BusinessNumberDuplicate() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(otherSeller));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("이미 등록된 사업자 등록번호입니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository, never()).findByUserId(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void upsertSellerInfo_UserNotFound() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByBusinessNumber(any());
            verify(sellersRepository, never()).findByUserId(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 판매자 권한이 없는 사용자")
        void upsertSellerInfo_AccessDenied() {
            // given
            given(userRepository.findById(testUserId)).willReturn(Optional.of(testBuyerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("판매자 권한이 필요합니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByBusinessNumber(any());
            verify(sellersRepository, never()).findByUserId(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 운영시간 유효성 검증 실패 (시작시간 > 종료시간)")
        void upsertSellerInfo_InvalidOperatingHours() {
            // given
            SellerInfoRequest invalidRequest = new SellerInfoRequest(
                    "펫푸드 공방",
                    "https://example.com/logo.jpg",
                    "987-65-43210", // 다른 사업자번호
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(20, 0),  // 20:00 (시작)
                    LocalTime.of(9, 0),   // 09:00 (종료) - 잘못된 시간
                    "월요일,화요일"
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("운영 시작 시간은 종료 시간보다 빠를 수 없습니다");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByBusinessNumber(any());
        }

        @Test
        @DisplayName("실패 - 운영시간 부분 입력 (시작시간만 있음)")
        void upsertSellerInfo_PartialOperatingHours() {
            // given
            SellerInfoRequest partialRequest = new SellerInfoRequest(
                    "펫푸드 공방",
                    "https://example.com/logo.jpg",
                    "987-65-43210",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),   // 시작시간만 있음
                    null,                 // 종료시간 없음
                    "MON,TUE"
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, partialRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("운영 시작 시간과 종료 시간은 모두 입력하거나 모두 입력하지 않아야 합니다.");

            // verify
            verify(userRepository).findById(testUserId);
            verify(sellersRepository, never()).findByBusinessNumber(any());
        }
    }

    @Nested
    @DisplayName("사업자번호 중복 검증 Edge Case 테스트")
    class BusinessNumberValidationEdgeCaseTests {

        @Test
        @DisplayName("성공 - userId가 null인 경우 User 관계에서 ID 조회")
        void upsertSellerInfo_BusinessNumberCheck_NullUserIdButUserExists() {
            // given
            Sellers sellerWithNullUserId = Sellers.builder()
                    .userId(null) // userId가 null
                    .user(Users.builder().id(otherUserId).build()) // 하지만 user는 존재
                    .businessNumber("123-45-67890")
                    .build();

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(sellerWithNullUserId));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("이미 등록된 사업자 등록번호입니다");
        }

        @Test
        @DisplayName("성공 - userId와 User 모두 null인 경우는 처리하지 않음")
        void upsertSellerInfo_BusinessNumberCheck_BothNull() {
            // given
            Sellers sellerWithNullBoth = Sellers.builder()
                    .userId(null)
                    .user(null)
                    .businessNumber("123-45-67890")
                    .build();

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(sellerWithNullBoth));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, testRequest);

            // then
            assertThat(result).isNotNull(); // 예외가 발생하지 않고 정상 처리됨
        }
    }

    @Nested
    @DisplayName("Null/Empty 값 처리 테스트")
    class NullEmptyValueTests {

        @Test
        @DisplayName("성공 - 선택 필드가 null인 요청")
        void upsertSellerInfo_OptionalFieldsNull_Success() {
            // given - Record 생성자로 null 값들 포함
            SellerInfoRequest requestWithNulls = new SellerInfoRequest(
                    "펫푸드 방",                          // 필수
                    "https://example.com/logo.jpg",        // 필수
                    "987-65-43210",                        // 필수 (다른 사업자번호)
                    null,                                  // settlementBank (선택)
                    null,                                  // settlementAcc (선택)
                    null,                                  // tags (선택)
                    null,                                  // operatingStartTime (선택)
                    null,                                  // operatingEndTime (선택)
                    null                                   // closedDays (선택)
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("987-65-43210")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, requestWithNulls);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 빈 문자열 필드 처리")
        void upsertSellerInfo_EmptyStringFields_Success() {
            // given
            SellerInfoRequest requestWithEmptyStrings = new SellerInfoRequest(
                    "펫푸드 공방",                          // 필수
                    "https://example.com/logo.jpg",        // 필수
                    "987-65-43210",                        // 필수
                    "",                                    // 빈 문자열
                    "",                                    // 빈 문자열
                    "",                                    // 빈 문자열
                    LocalTime.of(9, 0),                    // 유효한 시간
                    LocalTime.of(18, 0),                   // 유효한 시간
                    ""                                     // 빈 문자열
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("987-65-43210")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, requestWithEmptyStrings);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(sellersRepository).save(any(Sellers.class));
        }
    }

    // 테스트 유틸리티 메서드 추가 (Record 생성 편의 메서드)
    private SellerInfoRequest createTestRequest(String vendorName, String businessNumber) {
        return new SellerInfoRequest(
                vendorName,
                "https://example.com/logo.jpg",
                businessNumber,
                "신한은행",
                "110-123-456789",
                "수제간식",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "MON,TUE"
        );
    }

    private SellerInfoRequest createMinimalRequest(String vendorName, String profileImage, String businessNumber) {
        return new SellerInfoRequest(
                vendorName,
                profileImage,
                businessNumber,
                null,  // settlementBank
                null,  // settlementAcc
                null,  // tags
                null,  // operatingStartTime
                null,  // operatingEndTime
                null   // closedDays
        );
    }

    @Nested
    @DisplayName("휴무일 처리 테스트")
    class ClosedDaysTests {

        @Test
        @DisplayName("성공 - 다양한 휴무일 패턴 처리")
        void upsertSellerInfo_VariousClosedDaysPatterns() {
            // given - 주말 휴무
            SellerInfoRequest weekendRequest = new SellerInfoRequest(
                    "주말휴무 펫샵",
                    "https://example.com/logo.jpg",
                    "111-11-11111",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "토요일,일요일"
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("111-11-11111")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, weekendRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 단일 휴무일 처리")
        void upsertSellerInfo_SingleClosedDay() {
            // given
            SellerInfoRequest singleDayRequest = new SellerInfoRequest(
                    "월요일만 휴무",
                    "https://example.com/logo.jpg",
                    "222-22-22222",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "월요일"
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("222-22-22222")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, singleDayRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 휴무일 없음 (null)")
        void upsertSellerInfo_NoClosedDays_Null() {
            // given
            SellerInfoRequest noClosedDaysRequest = new SellerInfoRequest(
                    "무휴 펫샵",
                    "https://example.com/logo.jpg",
                    "333-33-33333",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    null  // 휴무일 없음
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("333-33-33333")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, noClosedDaysRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 휴무일 없음 (빈 문자열)")
        void upsertSellerInfo_NoClosedDays_EmptyString() {
            // given
            SellerInfoRequest emptyClosedDaysRequest = new SellerInfoRequest(
                    "무휴 펫샵2",
                    "https://example.com/logo.jpg",
                    "444-44-44444",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    ""  // 빈 문자열
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("444-44-44444")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, emptyClosedDaysRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 많은 휴무일 처리")
        void upsertSellerInfo_ManyClosedDays() {
            // given
            SellerInfoRequest manyClosedDaysRequest = new SellerInfoRequest(
                    "주 3일만 운영",
                    "https://example.com/logo.jpg",
                    "555-55-55555",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "월요일,화요일,수요일,목요일"  // 4일 휴무
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("555-55-55555")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, manyClosedDaysRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("실패 - 잘못된 요일명")
        void validateClosedDays_InvalidDayName_ThrowsException() {
            // given
            SellerInfoRequest invalidRequest = new SellerInfoRequest(
                    "잘못된 요일",
                    "https://example.com/logo.jpg",
                    "333-33-33333",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "잘못된요일,화요일"  // 유효하지 않은 요일명
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 요일이 포함되어 있습니다: 잘못된요일,화요일");

            // verify - 유효성 검증 실패로 인해 DB 작업이 수행되지 않음
            verify(sellersRepository, never()).findByBusinessNumber(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 부분적으로 잘못된 요일명")
        void validateClosedDays_PartiallyInvalid_ThrowsException() {
            // given
            SellerInfoRequest partiallyInvalidRequest = new SellerInfoRequest(
                    "부분 잘못",
                    "https://example.com/logo.jpg",
                    "444-44-44444",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "월요일,잘못된요일,수요일"  // 중간에 잘못된 요일
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, partiallyInvalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 요일이 포함되어 있습니다");
        }

        @Test
        @DisplayName("성공 - 공백이 포함된 휴무일 처리")
        void upsertSellerInfo_ClosedDaysWithSpaces() {
            // given
            SellerInfoRequest spacedDaysRequest = new SellerInfoRequest(
                    "공백 포함 요일",
                    "https://example.com/logo.jpg",
                    "777-77-77777",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "월요일, 화요일, 수요일"  // 공백 포함
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("777-77-77777")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(testUserId, spacedDaysRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }
    }

}