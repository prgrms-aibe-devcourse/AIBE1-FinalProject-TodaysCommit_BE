package com.team5.catdogeats.admins.scheduler;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.List;

/**
 * 관리자 관련 정리 작업 스케줄러
 * 만료된 인증코드 정리 등의 작업을 수행합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminCleanupScheduler {

    private final AdminRepository adminRepository;

    /**
     * 만료된 인증코드 정리
     * 매일 새벽 2시에 실행
     */
    @Scheduled(cron = "0 0 2 * * *")
    @JpaTransactional
    public void cleanupExpiredVerificationCodes() {
        try {
            ZonedDateTime now = ZonedDateTime.now();
            List<Admins> expiredAdmins = adminRepository.findByVerificationCodeExpiryBefore(now);

            if (expiredAdmins.isEmpty()) {
                log.debug("정리할 만료된 인증코드가 없습니다.");
                return;
            }

            int cleanupCount = 0;
            for (Admins admin : expiredAdmins) {
                // 비활성화 상태이고 인증코드가 만료된 경우만 정리
                if (!admin.getIsActive() && admin.getVerificationCode() != null) {
                    admin.setVerificationCode(null, null);
                    adminRepository.save(admin);
                    cleanupCount++;
                }
            }

            log.info("만료된 인증코드 정리 완료: {}건", cleanupCount);

        } catch (Exception e) {
            log.error("인증코드 정리 작업 중 오류 발생: ", e);
        }
    }

    /**
     * 비활성화된 관리자 계정 정리 (선택사항)
     * 매주 일요일 새벽 3시에 실행
     * 30일 이상 비활성화 상태인 계정 삭제
     */
    @Scheduled(cron = "0 0 3 * * SUN")
    @JpaTransactional
    public void cleanupInactiveAdmins() {
        try {
            ZonedDateTime thirtyDaysAgo = ZonedDateTime.now().minusDays(30);

            // 30일 이상 비활성화 상태인 관리자 조회
            List<Admins> inactiveAdmins = adminRepository.findAll().stream()
                    .filter(admin -> !admin.getIsActive())
                    .filter(admin -> admin.getCreatedAt().isBefore(thirtyDaysAgo))
                    .filter(admin -> admin.getVerificationCode() == null) // 인증코드가 이미 정리된 계정
                    .toList();

            if (inactiveAdmins.isEmpty()) {
                log.debug("정리할 비활성화 관리자 계정이 없습니다.");
                return;
            }

            // 슈퍼관리자는 제외
            int deleteCount = 0;
            for (Admins admin : inactiveAdmins) {
                if (!"super@catdogeats.com".equals(admin.getEmail())) {
                    adminRepository.delete(admin);
                    deleteCount++;
                    log.info("비활성화 관리자 계정 삭제: email={}", admin.getEmail());
                }
            }

            log.info("비활성화 관리자 계정 정리 완료: {}건", deleteCount);

        } catch (Exception e) {
            log.error("비활성화 관리자 계정 정리 작업 중 오류 발생: ", e);
        }
    }
}