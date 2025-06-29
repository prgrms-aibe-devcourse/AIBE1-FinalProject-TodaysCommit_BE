package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.domain.dto.AdminInfoResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.Department;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.admins.service.AdminManagementService;
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
                    .skip((long) page * size)
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
                    .skip((long) page * size)
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
