package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
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

    // 테스트용 데이터
    private String testUserId;
    private String otherUserId;
    private Users testSellerUser;
    private Users testBuyerUser;
    private Sellers testSeller;
    private Sellers otherSeller;
    private SellerInfoRequest testRequest;

    // JWT 테스트용 데이터
    private UserPrincipal sellerPrincipal;
    private UserPrincipal buyerPrincipal;
    private UserPrincipal nonExistentPrincipal;

    @BeforeEach
    void setUp() {
        testUserId = "11111111-1111-1111-1111-111111111111";
        otherUserId = "22222222-2222-2222-2222-222222222222";

        // JWT UserPrincipal 설정
        sellerPrincipal = new UserPrincipal("google", "113091084348977764576");
        buyerPrincipal = new UserPrincipal("google", "buyer123456789");
        nonExistentPrincipal = new UserPrincipal("google", "nonexistent123");

        // 판매자 사용자
        testSellerUser = Users.builder()
                .id(testUserId)
                .provider("google")
                .providerId("113091084348977764576")
                .userNameAttribute("sub")
                .name("테스트 판매자")
                .role(Role.ROLE_SELLER)
                .build();

        // 구매자 사용자 (권한 테스트용)
        testBuyerUser = Users.builder()
                .id(testUserId)
                .provider("google")
                .providerId("buyer123456789")
                .userNameAttribute("sub")
                .name("테스트 구매자")
                .role(Role.ROLE_BUYER)
                .build();

        // 다른 판매자 사용자 (중복 테스트용)
        Users otherSellerUser = Users.builder()
                .id(otherUserId)
                .provider("google")
                .providerId("seller002")
                .userNameAttribute("sub")
                .name("다른 판매자")
                .role(Role.ROLE_SELLER)
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
                "펫푸드 공방",
                "https://example.com/logo.jpg",
                "123-45-67890",
                "신한은행",
                "110-123-456789",
                "수제간식",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "월요일,화요일"
        );
    }

    @Nested
    @DisplayName("JWT 기반 판매자 정보 조회 테스트")
    class GetSellerInfoByUserPrincipalTests {

        @Test
        @DisplayName("성공 - JWT로 판매자 정보 조회")
        void getSellerInfoByUserPrincipal_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));

            // when
            SellerInfoResponse result = sellerInfoService.getSellerInfo(sellerPrincipal);

            // then
            assertThat(result).isNotNull();
            assertThat(result.vendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.businessNumber()).isEqualTo("123-45-67890");
            assertThat(result.userId()).isEqualTo(testUserId);

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("성공 - JWT로 조회했지만 판매자 정보가 없는 경우 (null 반환)")
        void getSellerInfoByUserPrincipal_NoSellerInfo_ReturnsNull() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());

            // when
            SellerInfoResponse result = sellerInfoService.getSellerInfo(sellerPrincipal);

            // then
            assertThat(result).isNull();

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository).findByUserId(testUserId);
        }

        @Test
        @DisplayName("실패 - JWT 정보로 사용자를 찾을 수 없음")
        void getSellerInfoByUserPrincipal_UserNotFound() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "nonexistent123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerInfoService.getSellerInfo(nonExistentPrincipal))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다")
                    .hasMessageContaining("provider: google")
                    .hasMessageContaining("providerId: nonexistent123");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "nonexistent123");
            verify(sellersRepository, never()).findByUserId(any());
        }

        @Test
        @DisplayName("실패 - 구매자가 판매자 정보 조회 시도")
        void getSellerInfoByUserPrincipal_BuyerAccessDenied() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "buyer123456789"))
                    .willReturn(Optional.of(testBuyerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.getSellerInfo(buyerPrincipal))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("판매자 권한이 필요합니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "buyer123456789");
            verify(sellersRepository, never()).findByUserId(any());
        }
    }

    @Nested
    @DisplayName("JWT 기반 판매자 정보 등록/수정 테스트")
    class upsertSellerInfoTests {

        @Test
        @DisplayName("성공 - JWT로 신규 판매자 정보 등록")
        void upsertSellerInfo_CreateNew_Success() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, testRequest);

            // then
            assertThat(result).isNotNull();
            assertThat(result.vendorName()).isEqualTo("펫푸드 공방");
            assertThat(result.businessNumber()).isEqualTo("123-45-67890");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository).findByUserId(testUserId);
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - JWT로 기존 판매자 정보 수정")
        void upsertSellerInfo_UpdateExisting_Success() {
            // given
            String newVendorName = "수정된 Vendor_name";
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(testSeller));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, updateRequest);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository).findByUserId(testUserId);
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("실패 - JWT로 다른 사용자 사업자번호 중복")
        void upsertSellerInfo_BusinessNumberDuplicate() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(otherSeller));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, testRequest))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("이미 등록된 사업자 등록번호입니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository).findByBusinessNumber("123-45-67890");
            verify(sellersRepository, never()).findByUserId(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - JWT 정보로 사용자를 찾을 수 없음")
        void upsertSellerInfo_UserNotFound() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "nonexistent123"))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(nonExistentPrincipal, testRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "nonexistent123");
            verify(sellersRepository, never()).findByBusinessNumber(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 구매자가 판매자 정보 등록 시도")
        void upsertSellerInfo_BuyerAccessDenied() {
            // given
            given(userRepository.findByProviderAndProviderId("google", "buyer123456789"))
                    .willReturn(Optional.of(testBuyerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(buyerPrincipal, testRequest))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("판매자 권한이 필요합니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "buyer123456789");
            verify(sellersRepository, never()).findByBusinessNumber(any());
            verify(sellersRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패 - 운영시간 유효성 검증 실패")
        void upsertSellerInfo_InvalidOperatingHours() {
            // given
            SellerInfoRequest invalidRequest = new SellerInfoRequest(
                    "펫푸드 공방",
                    "https://example.com/logo.jpg",
                    "987-65-43210",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(20, 0),  // 20:00 (시작)
                    LocalTime.of(9, 0),   // 09:00 (종료) - 잘못된 시간
                    "월요일,화요일"
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("운영 시작 시간은 종료 시간보다 빠를 수 없습니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository, never()).findByBusinessNumber(any());
        }

        @Test
        @DisplayName("실패 - 잘못된 휴무일")
        void upsertSellerInfo_InvalidClosedDays() {
            // given
            SellerInfoRequest invalidRequest = new SellerInfoRequest(
                    "펫푸드 공방",
                    "https://example.com/logo.jpg",
                    "987-65-43210",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),
                    LocalTime.of(18, 0),
                    "잘못된요일,화요일"  // 유효하지 않은 요일
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("유효하지 않은 요일이 포함되어 있습니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(sellerWithNullUserId));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, testRequest))
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(sellerWithNullBoth));
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, testRequest);

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
                    "펫푸드 공방",                          // 필수
                    "https://example.com/logo.jpg",        // 필수
                    "987-65-43210",                        // 필수 (다른 사업자번호)
                    null,                                  // settlementBank (선택)
                    null,                                  // settlementAcc (선택)
                    null,                                  // tags (선택)
                    null,                                  // operatingStartTime (선택)
                    null,                                  // operatingEndTime (선택)
                    null                                   // closedDays (선택)
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("987-65-43210")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, requestWithNulls);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("987-65-43210")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, requestWithEmptyStrings);

            // then
            assertThat(result).isNotNull();

            // verify
            verify(sellersRepository).save(any(Sellers.class));
        }
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("111-11-11111")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, weekendRequest);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("222-22-22222")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, singleDayRequest);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("333-33-33333")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, noClosedDaysRequest);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("444-44-44444")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, emptyClosedDaysRequest);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("555-55-55555")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, manyClosedDaysRequest);

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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, invalidRequest))
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, partiallyInvalidRequest))
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

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("777-77-77777")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, spacedDaysRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }
    }

    @Nested
    @DisplayName("비즈니스 로직 예외 상황 테스트")
    class BusinessLogicExceptionTests {

        @Test
        @DisplayName("실패 - 운영 시간 중 시작시간만 설정")
        void upsertSellerInfo_OnlyStartTimeSet() {
            // given
            SellerInfoRequest onlyStartTimeRequest = new SellerInfoRequest(
                    "시작시간만 설정",
                    "https://example.com/logo.jpg",
                    "888-88-88888",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    LocalTime.of(9, 0),   // 시작시간만 설정
                    null,                 // 종료시간 null
                    "월요일"
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, onlyStartTimeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("운영 시작 시간과 종료 시간은 모두 입력하거나 모두 입력하지 않아야 합니다");

            // verify
            verify(userRepository).findByProviderAndProviderId("google", "113091084348977764576");
            verify(sellersRepository, never()).findByBusinessNumber(any());
        }

        @Test
        @DisplayName("실패 - 운영 시간 중 종료시간만 설정")
        void upsertSellerInfo_OnlyEndTimeSet() {
            // given
            SellerInfoRequest onlyEndTimeRequest = new SellerInfoRequest(
                    "종료시간만 설정",
                    "https://example.com/logo.jpg",
                    "999-99-99999",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    null,                 // 시작시간 null
                    LocalTime.of(18, 0),  // 종료시간만 설정
                    "월요일"
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(sellerPrincipal, onlyEndTimeRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("운영 시작 시간과 종료 시간은 모두 입력하거나 모두 입력하지 않아야 합니다");
        }

        @Test
        @DisplayName("성공 - 운영시간 모두 null")
        void upsertSellerInfo_BothTimesNull_Success() {
            // given
            SellerInfoRequest bothTimesNullRequest = new SellerInfoRequest(
                    "시간 미설정",
                    "https://example.com/logo.jpg",
                    "000-00-00000",
                    "신한은행",
                    "110-123-456789",
                    "수제간식",
                    null,  // 시작시간 null
                    null,  // 종료시간 null
                    "월요일"
            );

            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("000-00-00000")).willReturn(Optional.empty());
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, bothTimesNullRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }

        @Test
        @DisplayName("성공 - 자신의 사업자번호로 중복체크 (수정 시나리오)")
        void upsertSellerInfo_SameUserBusinessNumber_Success() {
            // given - 자신의 사업자번호로 수정 시도
            given(userRepository.findByProviderAndProviderId("google", "113091084348977764576"))
                    .willReturn(Optional.of(testSellerUser));
            given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(testSeller)); // 자신의 정보
            given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));
            given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

            // when
            SellerInfoResponse result = sellerInfoService.upsertSellerInfo(sellerPrincipal, testRequest);

            // then
            assertThat(result).isNotNull();
            verify(sellersRepository).save(any(Sellers.class));
        }
    }

}