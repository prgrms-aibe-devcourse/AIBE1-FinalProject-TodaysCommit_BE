package com.team5.catdogeats.admins.util;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * 관리자 관련 공통 유틸리티 클래스
 * 모든 관리자 서비스에서 공통으로 사용하는 헬퍼 메서드들을 제공합니다.
 */
@Slf4j
@Component
public class AdminUtils {

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    @Getter
    @Value("${app.admin.base-url}")
    private String baseUrl;

    @Value("${admin.invitation.expiration-hours:1}")
    private int expirationHours;

    public AdminUtils(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ===== 코드 생성 관련 =====

    /**
     * 6자리 인증코드 생성
     */
    public String generateVerificationCode() {
        return String.format("%06d", secureRandom.nextInt(1000000));
    }

    /**
     * 초기 비밀번호 생성 (8자리 영문+숫자)
     */
    public String generateInitialPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder password = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            password.append(chars.charAt(secureRandom.nextInt(chars.length())));
        }
        return password.toString();
    }

    // ===== 이메일 발송 관련 =====

    /**
     * 일반 인증코드 이메일 발송
     */
    public void sendEmail(String to, String subject, String content) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            log.info("이메일 발송 완료: to={}, subject={}", to, subject);

        } catch (Exception e) {
            log.error("이메일 발송 실패: to={}, subject={}, error={}", to, subject, e.getMessage());
            throw new RuntimeException("이메일 발송에 실패했습니다.");
        }
    }

    /**
     * 관리자 초대 이메일 발송
     */
    public void sendInvitationEmail(String email, String name, String verificationCode, Department department) {
        String subject = "[CatDogEats] 관리자 계정 인증 안내";
        String content = buildInvitationEmailContent(name, verificationCode, department);
        sendEmail(email, subject, content);
    }

    /**
     * 인증코드 재발송 이메일 발송
     */
    public void sendResendVerificationEmail(String email, String name, String verificationCode) {
        String subject = "[CatDogEats] 인증코드 재발송";
        String content = buildResendEmailContent(name, verificationCode);
        sendEmail(email, subject, content);
    }

    /**
     * 비밀번호 초기화 이메일 발송
     */
    public void sendPasswordResetEmail(String email, String name, String verificationCode, String requestedBy) {
        String subject = "[CatDogEats] 관리자 계정 비밀번호 초기화 안내";
        String content = buildPasswordResetEmailContent(name, verificationCode, requestedBy);
        sendEmail(email, subject, content);
    }

    // ===== 이메일 템플릿  =====

    /**
     * 관리자 초대 이메일 내용 구성
     */
    public String buildInvitationEmailContent(String name, String verificationCode, Department department) {
        String departmentName = getDepartmentDisplayName(department);

        return String.format("""
            안녕하세요 %s님,
            
            CatDogEats %s 관리자 계정이 생성되었습니다.
            
            ===== 계정 정보 =====
            부서: %s
            인증코드: %s
            
            ===== 계정 활성화 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 인증코드를 입력해주세요.
            
            3. 활성화 완료 후 로그인하여 비밀번호를 변경해주세요.
            
            ※ 인증코드는 %d시간 동안만 유효합니다.
            ※ 문의사항이 있으시면 시스템 관리자에게 연락해주세요.
            
            CatDogEats 관리팀
            """, name, departmentName, departmentName, verificationCode, baseUrl, expirationHours);
    }

    /**
     * 인증코드 재발송 이메일 내용 구성
     */
    public String buildResendEmailContent(String name, String verificationCode) {
        return String.format("""
            안녕하세요 %s님,
            
            요청하신 인증코드를 재발송합니다.
            
            ===== 새로운 인증코드 =====
            %s
            
            ===== 계정 활성화 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 새로운 인증코드를 입력해주세요.
            
            ※ 인증코드는 %d시간 동안만 유효합니다.
            ※ 문의사항이 있으시면 개발팀에 연락해주세요.
            
            CatDogEats 관리팀
            """, name, verificationCode, baseUrl, expirationHours);
    }

    /**
     * 비밀번호 초기화 이메일 내용 구성
     */
    public String buildPasswordResetEmailContent(String name, String verificationCode, String requestedBy) {
        return String.format("""
            안녕하세요 %s님,
            
            관리자(%s)의 요청으로 귀하의 CatDogEats 관리자 계정 비밀번호가 초기화되었습니다.
            
            ===== 비밀번호 재설정 안내 =====
            인증코드: %s
            
            ===== 비밀번호 재설정 방법 =====
            1. 아래 링크로 접속해주세요.
            %s/v1/admin/verify
            
            2. 이메일과 인증코드를 입력해주세요.
            
            3. 새로운 비밀번호를 설정해주세요.
            
            ※ 인증코드는 %d시간 동안만 유효합니다.
            ※ 계정이 임시로 비활성화되었으며, 재설정 완료 후 다시 사용할 수 있습니다.
            ※ 문의사항이 있으시면 시스템 관리자에게 연락해주세요.
            
            CatDogEats 관리팀
            """, name, requestedBy, verificationCode, baseUrl, expirationHours);
    }

    // ===== 유틸리티 메서드 =====

    /**
     * 부서 표시명 반환
     */
    public String getDepartmentDisplayName(Department department) {
        return switch (department) {
            case DEVELOPMENT -> "개발팀";
            case CUSTOMER_SERVICE -> "고객서비스팀";
            case OPERATIONS -> "운영팀";
            case ADMIN -> "관리팀";
        };
    }

    /**
     * 만료 시간 계산 (현재 시간 + 설정된 시간)
     */
    public java.time.ZonedDateTime calculateExpiryTime() {
        return java.time.ZonedDateTime.now().plusHours(expirationHours);
    }

    /**
     * 로그인 리다이렉트 URL 생성
     */
    public String getLoginRedirectUrl() {
        return baseUrl + "/v1/admin/login";
    }

    /**
     * 인증 페이지 URL 생성
     */
    public String getVerifyPageUrl() {
        return baseUrl + "/v1/admin/verify";
    }
}