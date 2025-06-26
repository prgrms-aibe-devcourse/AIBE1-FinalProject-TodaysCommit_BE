package com.team5.catdogeats.admins.repository;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.enums.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AdminRepository extends JpaRepository<Admins, String> {

    /**
     * 이메일로 관리자 조회
     */
    Optional<Admins> findByEmail(String email);

    /**
     * 이메일과 부서로 관리자 조회
     */
    Optional<Admins> findByEmailAndDepartment(String email, Department department);

    /**
     * 인증코드로 관리자 조회
     */
    Optional<Admins> findByVerificationCode(String verificationCode);

    /**
     * 활성화된 관리자 목록 조회
     */
    List<Admins> findByIsActiveTrue();

    /**
     * 부서별 관리자 목록 조회
     */
    List<Admins> findByDepartment(Department department);

    /**
     * 만료된 인증코드를 가진 관리자 목록 조회 (정리용)
     */
    List<Admins> findByVerificationCodeExpiryBefore(ZonedDateTime expiry);

    /**
     * 이메일 중복 체크
     */
    boolean existsByEmail(String email);
}