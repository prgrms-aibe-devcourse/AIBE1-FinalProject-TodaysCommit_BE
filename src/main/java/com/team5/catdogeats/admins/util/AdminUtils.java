package com.team5.catdogeats.admins.util;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;


/**
 * 관리자 관련 공통 유틸리티 클래스
 * 중복 코드 제거를 위한 헬퍼 메서드들
 */
@Slf4j
@Component
public class AdminUtils {

    private final JavaMailSender mailSender;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * -- GETTER --
     *  기본 URL 반환
     */
    @Getter
    @Value("${app.admin.base-url}")
    private String baseUrl;

    public AdminUtils(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

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

    /**
     * 일반 이메일 발송
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
     * 부서 표시명 반환
     */
    public String getDepartmentDisplayName(com.team5.catdogeats.admins.domain.enums.Department department) {
        return switch (department) {
            case DEVELOPMENT -> "개발팀";
            case CUSTOMER_SERVICE -> "고객서비스팀";
            case OPERATIONS -> "운영팀";
            case ADMIN -> "관리팀";
        };
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
            
            ※ 인증코드는 1시간 동안만 유효합니다.
            ※ 문의사항이 있으시면 개발팀에 연락해주세요.
            
            CatDogEats 관리팀
            """, name, verificationCode, baseUrl);
    }
}
