package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminPasswordResetVerificationDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminVerificationResponseDTO;
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
@DisplayName("AdminVerificationService 단위 테스트")
class AdminVerificationServiceImplTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AdminUtils adminUtils;

    @InjectMocks
    private AdminVerificationServiceImpl verificationService;

    private Admins pendingAdmin;
    private Admins activeAdmin;
    private Admins expiredCodeAdmin;

    @BeforeEach
    void setUp() {
        // 인증 대기중인 관리자
        pendingAdmin = Admins.builder()
                .id("admin-1")
                .email("pending@test.com")
                .password("encoded-temp-password")
                .name("대기관리자")
                .department(Department.DEVELOPMENT)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(false)
                .isFirstLogin(true)
                .verificationCode("123456")
                .verificationCodeExpiry(ZonedDateTime.now().plusHours(1))
                .build();

        // 이미 활성화된 관리자
        activeAdmin = Admins.builder()
                .id("admin-2")
                .email("active@test.com")
                .password("encoded-password")
                .name("활성관리자")
                .department(Department.ADMIN)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(true)
                .isFirstLogin(false)
                .build();

        // 인증코드가 만료된 관리자
        expiredCodeAdmin = Admins.builder()
                .id("admin-3")
                .email("expired@test.com")
                .password("encoded-temp-password")
                .name("만료관리자")
                .department(Department.OPERATIONS)
                .adminRole(AdminRole.ROLE_ADMIN)
                .isActive(false)
                .isFirstLogin(true)
                .verificationCode("654321")
                .verificationCodeExpiry(ZonedDateTime.now().minusHours(1))
                .build();
    }

    @Test
    @DisplayName("관리자 계정 인증 성공")
    void verifyAdmin_Success() {
        // Given
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "pending@test.com", "123456");
        String initialPassword = "initial123";

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));
        when(adminUtils.generateInitialPassword()).thenReturn(initialPassword);
        when(passwordEncoder.encode(initialPassword)).thenReturn("encoded-initial123");
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // When
        AdminVerificationResponseDTO response = verificationService.verifyAdmin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("pending@test.com");
        assertThat(response.name()).isEqualTo("대기관리자");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.initialPassword()).isEqualTo(initialPassword);
        assertThat(response.redirectUrl()).isEqualTo("http://localhost:8080/v1/admin/login");
        assertThat(response.message()).contains("계정이 성공적으로 활성화되었습니다");

        // 관리자 상태 변경 확인
        assertThat(pendingAdmin.getIsActive()).isTrue();
        assertThat(pendingAdmin.getIsFirstLogin()).isTrue();
        assertThat(pendingAdmin.getVerificationCode()).isNull();
        assertThat(pendingAdmin.getVerificationCodeExpiry()).isNull();
        assertThat(pendingAdmin.getPassword()).isEqualTo("encoded-initial123");

        verify(adminRepository).save(pendingAdmin);
    }

    @Test
    @DisplayName("관리자 계정 인증 - 이미 활성화된 계정")
    void verifyAdmin_AlreadyActive() {
        // Given
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "active@test.com", "123456");

        when(adminRepository.findByEmail("active@test.com")).thenReturn(Optional.of(activeAdmin));
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // When
        AdminVerificationResponseDTO response = verificationService.verifyAdmin(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("active@test.com");
        assertThat(response.name()).isEqualTo("활성관리자");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.initialPassword()).isNull();
        assertThat(response.message()).isEqualTo("이미 활성화된 계정입니다.");

        verify(adminUtils, never()).generateInitialPassword();
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 계정 인증 실패 - 존재하지 않는 이메일")
    void verifyAdmin_Fail_EmailNotFound() {
        // Given
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "notfound@test.com", "123456");

        when(adminRepository.findByEmail("notfound@test.com")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> verificationService.verifyAdmin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 이메일입니다.");
    }

    @Test
    @DisplayName("관리자 계정 인증 실패 - 잘못된 인증코드")
    void verifyAdmin_Fail_WrongCode() {
        // Given
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "pending@test.com", "wrong123");

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));

        // When & Then
        assertThatThrownBy(() -> verificationService.verifyAdmin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드입니다.");
    }

    @Test
    @DisplayName("관리자 계정 인증 실패 - 만료된 인증코드")
    void verifyAdmin_Fail_ExpiredCode() {
        // Given
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "expired@test.com", "654321");

        when(adminRepository.findByEmail("expired@test.com")).thenReturn(Optional.of(expiredCodeAdmin));

        // When & Then
        assertThatThrownBy(() -> verificationService.verifyAdmin(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("인증코드가 만료되었습니다. 새로운 인증코드를 요청해주세요.");
    }

    @Test
    @DisplayName("인증코드 재발송 성공")
    void resendVerificationCode_Success() {
        // Given
        String newCode = "789012";
        ZonedDateTime newExpiry = ZonedDateTime.now().plusHours(1);

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));
        when(adminUtils.generateVerificationCode()).thenReturn(newCode);
        when(adminUtils.calculateExpiryTime()).thenReturn(newExpiry);

        // When
        String result = verificationService.resendVerificationCode("pending@test.com");

        // Then
        assertThat(result).isEqualTo(newCode);
        assertThat(pendingAdmin.getVerificationCode()).isEqualTo(newCode);
        assertThat(pendingAdmin.getVerificationCodeExpiry()).isEqualTo(newExpiry);

        verify(adminRepository).save(pendingAdmin);
        verify(adminUtils).sendResendVerificationEmail("pending@test.com", "대기관리자", newCode);
    }

    @Test
    @DisplayName("인증코드 재발송 실패 - 이미 활성화된 계정")
    void resendVerificationCode_Fail_AlreadyActive() {
        // Given
        when(adminRepository.findByEmail("active@test.com")).thenReturn(Optional.of(activeAdmin));

        // When & Then
        assertThatThrownBy(() -> verificationService.resendVerificationCode("active@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 활성화된 계정입니다.");

        verify(adminUtils, never()).generateVerificationCode();
        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("비밀번호 재설정 인증 성공")
    void verifyAndResetPassword_Success() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "pending@test.com", "123456", "newPassword123", "newPassword123");

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(adminUtils.getLoginRedirectUrl()).thenReturn("http://localhost:8080/v1/admin/login");

        // When
        AdminVerificationResponseDTO response = verificationService.verifyAndResetPassword(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.email()).isEqualTo("pending@test.com");
        assertThat(response.name()).isEqualTo("대기관리자");
        assertThat(response.isVerified()).isTrue();
        assertThat(response.initialPassword()).isNull();
        assertThat(response.message()).contains("비밀번호가 성공적으로 재설정되었습니다");

        // 관리자 상태 변경 확인
        assertThat(pendingAdmin.getIsActive()).isTrue();
        assertThat(pendingAdmin.getIsFirstLogin()).isFalse();
        assertThat(pendingAdmin.getPassword()).isEqualTo("encoded-new-password");
        assertThat(pendingAdmin.getVerificationCode()).isNull();
        assertThat(pendingAdmin.getVerificationCodeExpiry()).isNull();

        verify(adminRepository).save(pendingAdmin);
    }

    @Test
    @DisplayName("비밀번호 재설정 실패 - 비밀번호 불일치")
    void verifyAndResetPassword_Fail_PasswordMismatch() {
        // Given
        AdminPasswordResetVerificationDTO request = new AdminPasswordResetVerificationDTO(
                "pending@test.com", "123456", "newPassword123", "differentPassword");

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));

        // When & Then
        assertThatThrownBy(() -> verificationService.verifyAndResetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("새 비밀번호와 확인 비밀번호가 일치하지 않습니다.");

        verify(adminRepository, never()).save(any());
    }

    @Test
    @DisplayName("인증코드 검증 - null 인증코드")
    void validateVerificationCode_NullCode() {
        // Given
        pendingAdmin.setVerificationCode(null, null);
        AdminVerificationRequestDTO request = new AdminVerificationRequestDTO(
                "pending@test.com", "123456");

        when(adminRepository.findByEmail("pending@test.com")).thenReturn(Optional.of(pendingAdmin));

        // When & Then
        assertThatThrownBy(() -> verificationService.verifyAdmin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("잘못된 인증코드입니다.");
    }
}