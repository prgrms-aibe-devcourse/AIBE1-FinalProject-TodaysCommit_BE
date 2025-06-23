package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.exception.WithdrawnAccountDomainException;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.UserDuplicateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserDuplicateServiceImpl implements UserDuplicateService {
    private final UserRepository userRepository;

    @JpaTransactional
    public Users isDuplicate(Users users) {
            return userRepository.findByProviderAndProviderId(users.getProvider(),
                                                            users.getProviderId())
                    .map(existingUser -> {

                        // 계정이 비활성화된 경우 재활성화
                        OffsetDateTime deletedAt = existingUser.getDeletedAt();
                        OffsetDateTime sevenDaysAgo = OffsetDateTime.now(ZoneOffset.UTC).minusDays(7);
                        if(existingUser.getRole() == Role.ROLE_WITHDRAWN && existingUser.isAccountDisable() &&
                        deletedAt != null && !deletedAt.isAfter(sevenDaysAgo)) {
                            log.error("7일 이후 틸퇴한 계정 차단");
                            throw new WithdrawnAccountDomainException("이미 탈퇴한 유저입니다.");
                        }
                        if (existingUser.isAccountDisable() && deletedAt != null &&
                                deletedAt.isAfter(sevenDaysAgo)) {

                            existingUser.reactivationAccount();
                        }
                        existingUser.preUpdate();
                        return userRepository.save(existingUser); // 기존 유저 저장 (업데이트)
                    })
                    .orElseGet(() -> {
                        log.info("신규 유저입니다: {}", users.getProviderId());
                        return userRepository.save(users); // 신규 유저 저장
                    });

    }
}
