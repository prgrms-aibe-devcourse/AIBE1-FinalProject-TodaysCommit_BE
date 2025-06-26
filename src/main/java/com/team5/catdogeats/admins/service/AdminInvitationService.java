package com.team5.catdogeats.admins.service;

import com.team5.catdogeats.admins.domain.dto.AdminInvitationRequestDTO;
import com.team5.catdogeats.admins.domain.dto.AdminInvitationResponseDTO;

/**
 * 관리자 초대 서비스 인터페이스
 */
public interface AdminInvitationService {
    /**
     * 관리자 초대
     */
    AdminInvitationResponseDTO inviteAdmin(AdminInvitationRequestDTO request);
}