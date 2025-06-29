package com.team5.catdogeats.admins.domain.dto;

import com.team5.catdogeats.admins.domain.enums.Department;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 관리자 세션 정보
 * Redis 세션에 저장될 관리자 정보
 *
 * ※ Record가 아닌 Class인 이유:
 * 1. 가변성 필요 (setFirstLogin 등)
 * 2. Redis 직렬화 호환성
 * 3. 복잡한 비즈니스 로직 포함
 */
@Data
@Builder
public class AdminSessionInfo implements Serializable {

    private String adminId;
    private String email;
    private String name;
    private Department department;
    private ZonedDateTime loginTime;
    private boolean isFirstLogin;

    /**
     * 세션 유효성 검증
     */
    public boolean isValid() {
        return adminId != null && email != null && name != null;
    }

    /**
     * 세션 만료 여부 확인 (30분)
     */
    public boolean isExpired() {
        if (loginTime == null) return true;
        return loginTime.plusMinutes(30).isBefore(ZonedDateTime.now());
    }
}