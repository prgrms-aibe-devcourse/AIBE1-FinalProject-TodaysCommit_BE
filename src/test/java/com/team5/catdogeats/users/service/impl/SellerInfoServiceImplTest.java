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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any; // ✅ 올바른 import
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerInfoServiceTest {

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

        // 테스트 판매자 정보 - userId 자동 설정됨
        testSeller = Sellers.builder()
                .user(testSellerUser)  // Users 설정하면 userId는 @MapsId로 자동 설정
                .vendorName("펫푸드 공방")
                .vendorProfileImage("https://example.com/logo.jpg")
                .businessNumber("123-45-67890")
                .settlementBank("신한은행")
                .settlementAcc("110-123-456789")
                .tags("수제간식")
                .build();

        // 다른 판매자 정보 (중복 테스트용) - ✅ Users 객체 포함
        otherSeller = Sellers.builder()
                .user(otherSellerUser)  // Users 설정하면 userId는 @MapsId로 자동 설정
                .vendorName("다른 공방")
                .businessNumber("123-45-67890") // 같은 사업자번호
                .build();

        // 요청 데이터
        testRequest = new SellerInfoRequest();
        testRequest.setVendorName("펫푸드 공방");
        testRequest.setVendorProfileImage("https://example.com/logo.jpg");
        testRequest.setBusinessNumber("123-45-67890");
        testRequest.setSettlementBank("신한은행");
        testRequest.setSettlementAcc("110-123-456789");
        testRequest.setTags("수제간식");
    }

    // ===== 권한 검증 포함 메서드 테스트 =====

    @Test
    @DisplayName("판매자 정보 조회 성공 (권한 검증 포함)")
    void getSellerInfo_Success() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
        given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));

        // when
        SellerInfoResponse result = sellerInfoService.getSellerInfo(testUserId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getVendorName()).isEqualTo("펫푸드 공방");
        assertThat(result.getBusinessNumber()).isEqualTo("123-45-67890");
        assertThat(result.getUserId()).isEqualTo(testUserId.toString());
    }

    @Test
    @DisplayName("판매자 정보 조회 실패 - 사용자 없음")
    void getSellerInfo_UserNotFound() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sellerInfoService.getSellerInfo(testUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    @Test
    @DisplayName("판매자 정보 조회 실패 - 판매자 권한 없음")
    void getSellerInfo_AccessDenied() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testBuyerUser));

        // when & then
        assertThatThrownBy(() -> sellerInfoService.getSellerInfo(testUserId))
                .isInstanceOf(SellerAccessDeniedException.class)
                .hasMessage("판매자 권한이 필요합니다.");
    }

    // ===== 권한 검증 없는 메서드 테스트 =====

    @Test
    @DisplayName("판매자 정보 조회 성공 (권한 검증 없음)")
    void getSellerInfoWithoutAuth_Success() {
        // given
        given(userRepository.existsById(testUserId)).willReturn(true);
        given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller));

        // when
        SellerInfoResponse result = sellerInfoService.getSellerInfoWithoutAuth(testUserId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getVendorName()).isEqualTo("펫푸드 공방");
        assertThat(result.getBusinessNumber()).isEqualTo("123-45-67890");
    }

    @Test
    @DisplayName("판매자 정보 조회 - 정보 없음 (권한 검증 없음)")
    void getSellerInfoWithoutAuth_NotFound() {
        // given
        given(userRepository.existsById(testUserId)).willReturn(true);
        given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty());

        // when
        SellerInfoResponse result = sellerInfoService.getSellerInfoWithoutAuth(testUserId);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("판매자 정보 조회 실패 - 사용자 없음 (권한 검증 없음)")
    void getSellerInfoWithoutAuth_UserNotFound() {
        // given
        given(userRepository.existsById(testUserId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> sellerInfoService.getSellerInfoWithoutAuth(testUserId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

    // ===== 등록/수정 테스트 =====

    @Test
    @DisplayName("판매자 정보 등록 성공 (권한 검증 없음)")
    void upsertSellerInfoWithoutAuth_Create_Success() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
        given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.empty()); // 기존 정보 없음
        given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.empty()); // 중복 없음
        given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller); // ✅ 올바른 any() 사용

        // when
        SellerInfoResponse result = sellerInfoService.upsertSellerInfoWithoutAuth(testUserId, testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getVendorName()).isEqualTo("펫푸드 공방");
        verify(sellersRepository).save(any(Sellers.class)); // ✅ 올바른 any() 사용
    }

    @Test
    @DisplayName("판매자 정보 수정 성공 (권한 검증 없음)")
    void upsertSellerInfoWithoutAuth_Update_Success() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
        given(sellersRepository.findByUserId(testUserId)).willReturn(Optional.of(testSeller)); // 기존 정보 있음
        given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(testSeller)); // 본인 것
        given(sellersRepository.save(any(Sellers.class))).willReturn(testSeller);

        // when
        SellerInfoResponse result = sellerInfoService.upsertSellerInfoWithoutAuth(testUserId, testRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getVendorName()).isEqualTo("펫푸드 공방");
        verify(sellersRepository).save(any(Sellers.class));
    }

    @Test
    @DisplayName("판매자 정보 등록 실패 - 사업자번호 중복")
    void upsertSellerInfoWithoutAuth_BusinessNumberDuplicate() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
        given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(otherSeller)); // 다른 사람이 사용중

        // when & then
        assertThatThrownBy(() -> sellerInfoService.upsertSellerInfoWithoutAuth(testUserId, testRequest))
                .isInstanceOf(BusinessNumberDuplicateException.class)
                .hasMessage("이미 등록된 사업자 등록번호입니다.");
    }

    @Test
    @DisplayName("판매자 정보 등록 실패 - 사용자 없음")
    void upsertSellerInfoWithoutAuth_UserNotFound() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sellerInfoService.upsertSellerInfoWithoutAuth(testUserId, testRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }
}