package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.AdminListResponseDTO;
import com.team5.catdogeats.admins.domain.dto.AdminStatsDTO;
import com.team5.catdogeats.admins.domain.enums.Department;

/**
 * 관리자 계정 관리 서비스 인터페이스
 */
public interface AdminManagementService {

    /**
     * 관리자 목록 조회 (페이지네이션, 필터링 지원)
     */
    AdminListResponseDTO getAdminList(int page, int size, String status, String search, Department department);


    /**
     * 관리자 통계 계산
     */
    AdminStatsDTO calculateAdminStats();
}