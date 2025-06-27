package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInfoResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminManagementService;
import com.team5.catdogeats.global.config.JpaTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 관리자 계정 관리 서비스 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminManagementServiceImpl implements AdminManagementService {

    private final AdminRepository adminRepository;

    @Override
    public AdminListResponseDTO getAdminList(int page, int size, String status, String search, Department department) {
        // 페이지네이션 설정
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // 필터링 조건에 따라 조회
        List<Admins> adminList;
        long totalElements;

        if (search != null && !search.trim().isEmpty()) {
            // 검색어가 있는 경우
            adminList = adminRepository.findAll().stream()
                    .filter(admin ->
                            admin.getName().toLowerCase().contains(search.toLowerCase()) ||
                                    admin.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .skip(page * size)
                    .limit(size)
                    .toList();
            totalElements = adminRepository.findAll().stream()
                    .filter(admin ->
                            admin.getName().toLowerCase().contains(search.toLowerCase()) ||
                                    admin.getEmail().toLowerCase().contains(search.toLowerCase()))
                    .count();
        } else if (status != null && !status.equals("all")) {
            // 상태별 필터링
            adminList = filterByStatus(status).stream()
                    .skip(page * size)
                    .limit(size)
                    .toList();
            totalElements = filterByStatus(status).size();
        } else if (department != null) {
            // 부서별 필터링
            adminList = adminRepository.findByDepartment(department);
            totalElements = adminList.size();
        } else {
            // 전체 조회
            Page<Admins> adminPage = adminRepository.findAll(pageable);
            adminList = adminPage.getContent();
            totalElements = adminPage.getTotalElements();
        }

        // DTO 변환
        List<AdminInfoResponseDTO> adminDTOs = adminList.stream()
                .map(this::convertToDTO)
                .toList();

        // 통계 정보 계산
        AdminStatsDTO stats = calculateAdminStats();

        return AdminListResponseDTO.builder()
                .admins(adminDTOs)
                .totalElements(totalElements)
                .totalPages((int) Math.ceil((double) totalElements / size))
                .currentPage(page)
                .pageSize(size)
                .stats(stats)
                .build();
    }

    @Override
    @JpaTransactional
    public String toggleAdminStatus(String adminEmail, String currentUserEmail) {
        Admins admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 슈퍼관리자 계정은 비활성화 불가
        if (admin.getEmail().equals("super@catdogeats.com")) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 상태를 변경할 수 없습니다.");
        }

        // 상태 토글
        admin.setIsActive(!admin.getIsActive());
        adminRepository.save(admin);

        String statusMessage = admin.getIsActive() ? "활성화" : "비활성화";

        log.info("관리자 상태 변경: email={}, newStatus={}, changedBy={}",
                admin.getEmail(), statusMessage, currentUserEmail);

        return String.format("%s님의 계정이 %s되었습니다.", admin.getName(), statusMessage);
    }

    @Override
    @JpaTransactional
    public String deleteAdmin(String adminEmail, String currentUserEmail) {
        Admins admin = adminRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 슈퍼관리자 계정은 삭제 불가
        if (admin.getEmail().equals("super@catdogeats.com")) {
            throw new IllegalArgumentException("슈퍼관리자 계정은 삭제할 수 없습니다.");
        }

        // 자기 자신은 삭제 불가
        if (admin.getEmail().equals(currentUserEmail)) {
            throw new IllegalArgumentException("자신의 계정은 삭제할 수 없습니다.");
        }

        String deletedAdminName = admin.getName();
        adminRepository.delete(admin);

        log.info("관리자 계정 삭제: email={}, name={}, deletedBy={}",
                admin.getEmail(), admin.getName(), currentUserEmail);

        return String.format("%s님의 계정이 삭제되었습니다.", deletedAdminName);
    }

    @Override
    public AdminStatsDTO calculateAdminStats() {
        List<Admins> allAdmins = adminRepository.findAll();

        long totalCount = allAdmins.size();
        long activeCount = allAdmins.stream().filter(Admins::getIsActive).count();
        long pendingCount = allAdmins.stream()
                .filter(admin -> !admin.getIsActive() && admin.getVerificationCode() != null)
                .count();
        long inactiveCount = totalCount - activeCount;

        return AdminStatsDTO.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .pendingCount(pendingCount)
                .inactiveCount(inactiveCount)
                .build();
    }

    // === Private Helper Methods ===

    /**
     * 상태별 필터링
     */
    private List<Admins> filterByStatus(String status) {
        List<Admins> allAdmins = adminRepository.findAll();

        return switch (status.toLowerCase()) {
            case "active" -> allAdmins.stream()
                    .filter(admin -> admin.getIsActive() && !admin.getIsFirstLogin())
                    .toList();
            case "pending" -> allAdmins.stream()
                    .filter(admin -> !admin.getIsActive() && admin.getVerificationCode() != null)
                    .toList();
            case "inactive" -> allAdmins.stream()
                    .filter(admin -> !admin.getIsActive())
                    .toList();
            default -> allAdmins;
        };
    }

    /**
     * Admin 엔티티를 DTO로 변환
     */
    private AdminInfoResponseDTO convertToDTO(Admins admin) {
        return AdminInfoResponseDTO.builder()
                .id(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .department(admin.getDepartment())
                .isActive(admin.getIsActive())
                .isFirstLogin(admin.getIsFirstLogin())
                .verificationCode(admin.getVerificationCode())
                .verificationCodeExpiry(admin.getVerificationCodeExpiry())
                .lastLoginAt(admin.getLastLoginAt())
                .createdAt(admin.getCreatedAt())
                .build();
    }
}
