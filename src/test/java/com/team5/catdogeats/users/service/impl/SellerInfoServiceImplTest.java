// 1. 수정된 SellerInfoServiceImplTest.java
package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.dto.SellerInfoRequest;
import com.team5.catdogeats.users.dto.SellerInfoResponse;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.time.ZonedDateTime;
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
                .operatingStartTime(ZonedDateTime.now().with(LocalTime.of(9, 0)))
                .operatingEndTime(ZonedDateTime.now().with(LocalTime.of(18, 0)))
                .closedDays("MON,TUE")
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
                ZonedDateTime.now().with(LocalTime.of(9, 0)),
                ZonedDateTime.now().with(LocalTime.of(18, 0)),                // operatingEndTime
                "MON,TUE"                              // closedDays
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
            assertThat(result.operatingStartTime().toLocalTime()).isEqualTo(LocalTime.of(9, 0));
            assertThat(result.operatingEndTime().toLocalTime()).isEqualTo(LocalTime.of(18, 0));
            assertThat(result.closedDays()).isEqualTo("MON,TUE");

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
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");

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
                    .isInstanceOf(SellerAccessDeniedException.class)
                    .hasMessage("판매자 권한이 필요합니다.");

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
                    .isInstanceOf(BusinessNumberDuplicateException.class)
                    .hasMessage("이미 등록된 사업자 등록번호입니다.");

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
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessage("존재하지 않는 사용자입니다.");

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
                    .isInstanceOf(SellerAccessDeniedException.class)
                    .hasMessage("판매자 권한이 필요합니다.");

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
                    ZonedDateTime.now().with(LocalTime.of(20, 0)), // 20:00 (시작)
                    ZonedDateTime.now().with(LocalTime.of(9, 0)), // 09:00 (종료)
                    "MON,TUE"
            );

            given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));

            // when & then
            assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, invalidRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("운영 시작 시간은 종료 시간보다 빠를 수 없습니다.");

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
                    ZonedDateTime.now().with(LocalTime.of(9, 0)),   // 시작시간만 있음
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
                    .isInstanceOf(BusinessNumberDuplicateException.class)
                    .hasMessage("이미 등록된 사업자 등록번호입니다.");
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
                    ZonedDateTime.now().with(LocalTime.of(9, 0)),// 유효한 시간
                    ZonedDateTime.now().with(LocalTime.of(18, 0)),// 유효한 시간
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
                ZonedDateTime.now().with(LocalTime.of(9, 0)),
                ZonedDateTime.now().with(LocalTime.of(18, 0)),
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
}