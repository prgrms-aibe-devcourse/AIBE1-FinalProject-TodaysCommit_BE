package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.*;
import com.team5.catdogeats.admins.domain.enums.AdminRole;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.util.AdminUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminPasswordResetService 단위 테스트")
class AdminPasswordResetServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminUtils adminUtils;

    @InjectMocks
    private AdminPasswordResetServiceImpl passwordResetService;

    private Admins targetAdmin;
    private Admins superAdmin;
    private Admins adminWithCode;

    @BeforeEach
    void setUp() {
        // 일반 관리자
        targetAdmin = Admins.builder()
                .id("admin-1")
                .email("target@test.com")
                .password("encoded-password")
                .name("대상관리자")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        // 슈퍼관리자
        superAdmin = Admins.builder()
                .id("super-admin")
                .email("super@catdogeats.com")
                .password("encoded-password")
                .name("슈퍼관리자")
                .department(Department.ADMIN)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        // 인증코드가 있는 관리자
        adminWithCode = Admins.builder()
                .id("admin-2")
                .email("withcode@test.com")
                .password("encoded-password")
                .name("코드있는관리자")
                .department(Department.OPERATIONS)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(false)
                .isFirstLogin(true)
                .verificationCode("123456")
                .verificationCodeExpiry(ZonedDateTime.now().plusHours(1))
                .build();
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 성공")
    void requestPasswordReset_Success() {
        // Given
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                "target@test.com", "requester@test.com");
        String verificationCode = "789012";
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(1);

        when(adminRepository.findByEmail("target@test.com")).thenReturn(Optional.of(targetAdmin));
        when(adminUtils.generateVerificationCode()).thenReturn(verificationCode);
        when(adminUtils.calculateExpiryTime()).thenReturn(expiry);

        // When
        AdminPasswordResetResponseDTO response = passwordResetService.requestPasswordReset(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("target@test.com");
        assertThat(response.name()).isEqualTo("대상관리자");
        assertThat(response.verificationCodeExpiry()).isEqualTo(expiry);
        assertThat(response.message()).isEqualTo("비밀번호 초기화 이메일이 발송되었습니다.");

        // 관리자 상태 변경 확인
        assertThat(targetAdmin.getIsActive()).isFalse();
        assertThat(targetAdmin.getIsFirstLogin()).isTrue();
        assertThat(targetAdmin.getVerificationCode()).isEqualTo(verificationCode);
        assertThat(targetAdmin.getVerificationCodeExpiry()).isEqualTo(expiry);

        verify(adminRepository).save(targetAdmin);
        verify(adminUtils).sendPasswordResetEmail(
                "target@test.com", "대상관리자", verificationCode, "requester@test.com");
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 존재하지 않는 관리자")
    void requestPasswordReset_Fail_AdminNotFound() {
        // Given
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                "notfound@test.com", "requester@test.com");

        when(adminRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 관리자입니다.");

        verify(adminUtils, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 슈퍼관리자")
    void requestPasswordReset_Fail_SuperAdmin() {
        // Given
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                "super@catdogeats.com", "requester@test.com");

        when(adminRepository.findByEmail("super@catdogeats.com")).thenReturn(Optional.of(superAdmin));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("슈퍼관리자 계정은 비밀번호를 초기화할 수 없습니다.");

        verify(adminUtils, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 초기화 요청 실패 - 자기 자신")
    void requestPasswordReset_Fail_SelfReset() {
        // Given
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                "target@test.com", "target@test.com");

        when(adminRepository.findByEmail("target@test.com")).thenReturn(Optional.of(targetAdmin));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.requestPasswordReset(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("자신의 비밀번호는 초기화할 수 없습니다. 비밀번호 변경 기능을 이용하세요.");

        verify(adminUtils, never()).sendPasswordResetEmail(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 성공")
    void verifyAndResetPassword_Success() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "123456", "newPassword123", "newPassword123");

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // When
        AdminVerificationResponseDTO response = passwordResetService.verifyAndResetPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("withcode@test.com");
        assertThat(response.name()).isEqualTo("코드있는관리자");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.initialPassword()).isNull();
        assertThat(response.message()).contains("비밀번호가 성공적으로 재설정되었습니다");
        assertThat(response.redirectUrl()).isEqualTo("http://localhost:8080/v1/admin/login");

        // 관리자 상태 변경 확인
        assertThat(adminWithCode.getIsActive()).isTrue();
        assertThat(adminWithCode.getIsFirstLogin()).isFalse();
        assertThat(adminWithCode.getPassword()).isEqualTo("encoded-new-password");
        assertThat(adminWithCode.getVerificationCode()).isNull();
        assertThat(adminWithCode.getVerificationCodeExpiry()).isNull();

        verify(adminRepository).save(adminWithCode);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 존재하지 않는 이메일")
    void verifyAndResetPassword_Fail_EmailNotFound() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "notfound@test.com", "123456", "newPassword123", "newPassword123");

        when(adminRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> passwordResetService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 잘못된 인증코드")
    void verifyAndResetPassword_Fail_WrongCode() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "wrong123", "newPassword123", "newPassword123");

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 만료된 인증코드")
    void verifyAndResetPassword_Fail_ExpiredCode() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "123456", "newPassword123", "newPassword123");

        // 인증코드 만료 설정
        adminWithCode.setVerificationCode("123456", ZonedDateTime.now().minusHours(1));

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("인증코드가 만료되었습니다. 관리자에게 새로운 초기화를 요청하세요.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 비밀번호 불일치")
    void verifyAndResetPassword_Fail_PasswordMismatch() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "123456", "newPassword123", "differentPassword");

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - null 인증코드")
    void verifyAndResetPassword_Fail_NullCode() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "123456", "newPassword123", "newPassword123");

        // 인증코드를 null로 설정
        adminWithCode.setVerificationCode(null, null);

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));

        // When & Then
        assertThatThrownBy(() -> passwordResetService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드입니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 계정 초기화 - 상태 변경 검증")
    void resetAdminAccount_StateChange() {
        // Given
        AdminPasswordResetRequestDTO request = new AdminPasswordResetRequestDTO(
                "target@test.com", "requester@test.com");
        String verificationCode = "789012";
        ZonedDateTime expiry = ZonedDateTime.now().plusHours(1);

        when(adminRepository.findByEmail("target@test.com")).thenReturn(Optional.of(targetAdmin));
        when(adminUtils.generateVerificationCode()).thenReturn(verificationCode);
        when(adminUtils.calculateExpiryTime()).thenReturn(expiry);

        // 초기 상태 확인
        assertThat(targetAdmin.getIsActive()).isTrue();
        assertThat(targetAdmin.getIsFirstLogin()).isFalse();

        // When
        passwordResetService.requestPasswordReset(request);

        // Then - 계정이 비활성화되고 첫 로그인 상태로 변경됨
        assertThat(targetAdmin.getIsActive()).isFalse();
        assertThat(targetAdmin.getIsFirstLogin()).isTrue();
        assertThat(targetAdmin.getVerificationCode()).isEqualTo(verificationCode);
        assertThat(targetAdmin.getVerificationCodeExpiry()).isEqualTo(expiry);

        verify(adminRepository).save(targetAdmin);
    }

    @Test
    @DisplayName("비밀번호 재설정 완료 - 상태 변경 검증")
    void resetPassword_StateChange() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "withcode@test.com", "123456", "newPassword123", "newPassword123");

        when(adminRepository.findByEmail("withcode@test.com")).thenReturn(Optional.of(adminWithCode));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // 초기 상태 확인
        assertThat(adminWithCode.getIsActive()).isFalse();
        assertThat(adminWithCode.getIsFirstLogin()).isTrue();
        assertThat(adminWithCode.getVerificationCode()).isNotNull();

        // When
        passwordResetService.verifyAndResetPassword(request);

        // Then - 계정이 활성화되고 첫 로그인 상태가 해제됨
        assertThat(adminWithCode.getIsActive()).isTrue();
        assertThat(adminWithCode.getIsFirstLogin()).isFalse();
        assertThat(adminWithCode.getPassword()).isEqualTo("encoded-new-password");
        assertThat(adminWithCode.getVerificationCode()).isNull();
        assertThat(adminWithCode.getVerificationCodeExpiry()).isNull();

        verify(adminRepository).save(adminWithCode);
    }
}