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
import static org.mockito.ArgumentMatchers.any;
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

        // 테스트 판매자 정보 - userId 명시적 설정
        testSeller = Sellers.builder()
                .userId(testUserId)  //
                .user(testSellerUser)
                .vendorName("펫푸드 공방")
                .vendorProfileImage("https://example.com/logo.jpg")
                .businessNumber("123-45-67890")
                .settlementBank("신한은행")
                .settlementAcc("110-123-456789")
                .tags("수제간식")
                .build();

        //  다른 판매자 정보 - userId 명시적 설정
        otherSeller = Sellers.builder()
                .userId(otherUserId)  // 직접 설정
                .user(otherSellerUser)
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
    @DisplayName("판매자 정보 조회 성공")
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


    // ===== 등록/수정 테스트 =====

    @Test
    @DisplayName("판매자 정보 등록 실패 - 사업자번호 중복")
    void upsertSellerInfoWithoutAuth_BusinessNumberDuplicate() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.of(testSellerUser));
        given(sellersRepository.findByBusinessNumber("123-45-67890")).willReturn(Optional.of(otherSeller)); // 다른 사람이 사용중

        // when & then
        assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                .isInstanceOf(BusinessNumberDuplicateException.class)
                .hasMessage("이미 등록된 사업자 등록번호입니다.");
    }

    @Test
    @DisplayName("판매자 정보 등록 실패 - 사용자 없음")
    void upsertSellerInfoWithoutAuth_UserNotFound() {
        // given
        given(userRepository.findById(testUserId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> sellerInfoService.upsertSellerInfo(testUserId, testRequest))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }
}