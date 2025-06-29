package com.team5.catdogeats.admins.util;

import com.team5.catdogeats.admins.domain.enums.Department;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUtils 단위 테스트")
class AdminUtilsTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private AdminUtils adminUtils;

    @BeforeEach
    void setUp() {
        // ReflectionTestUtils를 사용하여 private 필드 설정
        ReflectionTestUtils.setField(adminUtils, "baseUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(adminUtils, "expirationHours", 1);
    }

    @Test
    @DisplayName("6자리 인증코드 생성 검증")
    void generateVerificationCode_Success() {
        // When
        String code = adminUtils.generateVerificationCode();

        // Then
        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}"); // 6자리 숫자인지 확인

        // 여러 번 생성하여 랜덤성 확인
        String code2 = adminUtils.generateVerificationCode();
        String code3 = adminUtils.generateVerificationCode();

        // 모두 다를 가능성이 높음 (완전히 같을 수도 있지만 매우 낮은 확률)
        assertThat(code).matches("\\d{6}");
        assertThat(code2).matches("\\d{6}");
        assertThat(code3).matches("\\d{6}");
    }

    @Test
    @DisplayName("초기 비밀번호 생성 검증")
    void generateInitialPassword_Success() {
        // When
        String password = adminUtils.generateInitialPassword();

        // Then
        assertThat(password).isNotNull();
        assertThat(password).hasSize(8);
        assertThat(password).matches("[A-Za-z0-9]{8}"); // 영문+숫자 8자리

        // 여러 번 생성하여 다양성 확인
        String password2 = adminUtils.generateInitialPassword();
        String password3 = adminUtils.generateInitialPassword();

        assertThat(password2).hasSize(8);
        assertThat(password3).hasSize(8);
        assertThat(password2).matches("[A-Za-z0-9]{8}");
        assertThat(password3).matches("[A-Za-z0-9]{8}");
    }

    @Test
    @DisplayName("이메일 발송 성공")
    void sendEmail_Success() {
        // Given
        String to = "test@example.com";
        String subject = "테스트 제목";
        String content = "테스트 내용";

        // When
        adminUtils.sendEmail(to, subject, content);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("이메일 발송 실패")
    void sendEmail_Failure() {
        // Given
        String to = "test@example.com";
        String subject = "테스트 제목";
        String content = "테스트 내용";

        doThrow(new RuntimeException("SMTP 서버 오류"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // When & Then
        assertThatThrownBy(() -> adminUtils.sendEmail(to, subject, content))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("이메일 발송에 실패했습니다.");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("관리자 초대 이메일 발송")
    void sendInvitationEmail_Success() {
        // Given
        String email = "newadmin@test.com";
        String name = "새관리자";
        String verificationCode = "123456";
        Department department = Department.DEVELOPMENT;

        // When
        adminUtils.sendInvitationEmail(email, name, verificationCode, department);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("인증코드 재발송 이메일 발송")
    void sendResendVerificationEmail_Success() {
        // Given
        String email = "admin@test.com";
        String name = "관리자";
        String verificationCode = "789012";

        // When
        adminUtils.sendResendVerificationEmail(email, name, verificationCode);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("비밀번호 초기화 이메일 발송")
    void sendPasswordResetEmail_Success() {
        // Given
        String email = "admin@test.com";
        String name = "관리자";
        String verificationCode = "456789";
        String requestedBy = "superadmin@test.com";

        // When
        adminUtils.sendPasswordResetEmail(email, name, verificationCode, requestedBy);

        // Then
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("관리자 초대 이메일 내용 구성")
    void buildInvitationEmailContent_Success() {
        // Given
        String name = "새관리자";
        String verificationCode = "123456";
        Department department = Department.DEVELOPMENT;

        // When
        String content = adminUtils.buildInvitationEmailContent(name, verificationCode, department);

        // Then
        assertThat(content).isNotNull();
        assertThat(content).contains(name);
        assertThat(content).contains(verificationCode);
        assertThat(content).contains("개발팀"); // 부서명 변환 확인
        assertThat(content).contains("http://localhost:8080/v1/admin/verify");
        assertThat(content).contains("1시간 동안만 유효");
    }

    @Test
    @DisplayName("인증코드 재발송 이메일 내용 구성")
    void buildResendEmailContent_Success() {
        // Given
        String name = "관리자";
        String verificationCode = "789012";

        // When
        String content = adminUtils.buildResendEmailContent(name, verificationCode);

        // Then
        assertThat(content).isNotNull();
        assertThat(content).contains(name);
        assertThat(content).contains(verificationCode);
        assertThat(content).contains("재발송");
        assertThat(content).contains("http://localhost:8080/v1/admin/verify");
    }

    @Test
    @DisplayName("비밀번호 초기화 이메일 내용 구성")
    void buildPasswordResetEmailContent_Success() {
        // Given
        String name = "관리자";
        String verificationCode = "456789";
        String requestedBy = "superadmin@test.com";

        // When
        String content = adminUtils.buildPasswordResetEmailContent(name, verificationCode, requestedBy);

        // Then
        assertThat(content).isNotNull();
        assertThat(content).contains(name);
        assertThat(content).contains(verificationCode);
        assertThat(content).contains(requestedBy);
        assertThat(content).contains("비밀번호가 초기화");
        assertThat(content).contains("http://localhost:8080/v1/admin/verify");
    }

    @Test
    @DisplayName("부서 표시명 반환 - 모든 부서")
    void getDepartmentDisplayName_AllDepartments() {
        // When & Then
        assertThat(adminUtils.getDepartmentDisplayName(Department.DEVELOPMENT))
                .isEqualTo("개발팀");
        assertThat(adminUtils.getDepartmentDisplayName(Department.CUSTOMER_SERVICE))
                .isEqualTo("고객서비스팀");
        assertThat(adminUtils.getDepartmentDisplayName(Department.OPERATIONS))
                .isEqualTo("운영팀");
        assertThat(adminUtils.getDepartmentDisplayName(Department.ADMIN))
                .isEqualTo("관리팀");
    }

    @Test
    @DisplayName("만료 시간 계산")
    void calculateExpiryTime_Success() {
        // Given
        ZonedDateTime before = ZonedDateTime.now();

        // When
        ZonedDateTime expiry = adminUtils.calculateExpiryTime();

        // Then
        ZonedDateTime after = ZonedDateTime.now().plusHours(1);

        assertThat(expiry).isNotNull();
        assertThat(expiry).isAfter(before);
        assertThat(expiry).isBeforeOrEqualTo(after);

        // 대략 1시간 후인지 확인 (오차 1분 허용)
        assertThat(expiry).isBetween(
                before.plusMinutes(59),
                before.plusMinutes(61)
        );
    }

    @Test
    @DisplayName("로그인 리다이렉트 URL 생성")
    void getLoginRedirectUrl_Success() {
        // When
        String url = adminUtils.getLoginRedirectUrl();

        // Then
        assertThat(url).isEqualTo("http://localhost:8080/v1/admin/login");
    }

    @Test
    @DisplayName("인증 페이지 URL 생성")
    void getVerifyPageUrl_Success() {
        // When
        String url = adminUtils.getVerifyPageUrl();

        // Then
        assertThat(url).isEqualTo("http://localhost:8080/v1/admin/verify");
    }

    @Test
    @DisplayName("baseUrl getter 테스트")
    void getBaseUrl_Success() {
        // When
        String baseUrl = adminUtils.getBaseUrl();

        // Then
        assertThat(baseUrl).isEqualTo("http://localhost:8080");
    }

    @Test
    @DisplayName("이메일 내용에 필수 정보 포함 확인")
    void emailContent_ContainsRequiredInfo() {
        // Given
        String name = "테스트관리자";
        String code = "123456";

        // When
        String invitationContent = adminUtils.buildInvitationEmailContent(
                name, code, Department.DEVELOPMENT);
        String resendContent = adminUtils.buildResendEmailContent(name, code);
        String resetContent = adminUtils.buildPasswordResetEmailContent(
                name, code, "requester@test.com");

        // Then - 모든 이메일에 공통으로 포함되어야 할 정보
        String[] contents = {invitationContent, resendContent, resetContent};

        for (String content : contents) {
            assertThat(content).contains("CatDogEats");
            assertThat(content).contains(name);
            assertThat(content).contains(code);
            assertThat(content).contains("시간 동안만 유효");
        }
    }

    @Test
    @DisplayName("인증코드 생성 - 패턴 일관성 확인")
    void generateVerificationCode_PatternConsistency() {
        // Given
        Pattern codePattern = Pattern.compile("^\\d{6}$");

        // When & Then - 100번 생성하여 모두 패턴에 맞는지 확인
        for (int i = 0; i < 100; i++) {
            String code = adminUtils.generateVerificationCode();
            assertThat(codePattern.matcher(code).matches())
                    .withFailMessage("생성된 코드 '%s'가 패턴에 맞지 않습니다", code)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("초기 비밀번호 생성 - 패턴 일관성 확인")
    void generateInitialPassword_PatternConsistency() {
        // Given
        Pattern passwordPattern = Pattern.compile("^[A-Za-z0-9]{8}$");

        // When & Then - 100번 생성하여 모두 패턴에 맞는지 확인
        for (int i = 0; i < 100; i++) {
            String password = adminUtils.generateInitialPassword();
            assertThat(passwordPattern.matcher(password).matches())
                    .withFailMessage("생성된 비밀번호 '%s'가 패턴에 맞지 않습니다", password)
                    .isTrue();
        }
    }

    @Test
    @DisplayName("이메일 발송 - SimpleMailMessage 설정 확인")
    void sendEmail_MessageConfiguration() {
        // Given
        String to = "test@example.com";
        String subject = "테스트 제목";
        String content = "테스트 내용";

        // When
        adminUtils.sendEmail(to, subject, content);

        // Then
        verify(mailSender).send(argThat((SimpleMailMessage message) -> {
            assertThat(message.getTo()).containsExactly(to);
            assertThat(message.getSubject()).isEqualTo(subject);
            assertThat(message.getText()).isEqualTo(content);
            return true;
        }));
    }
}