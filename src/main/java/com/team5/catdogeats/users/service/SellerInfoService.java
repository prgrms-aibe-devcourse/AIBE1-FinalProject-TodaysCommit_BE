package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequestDTO;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponseDTO;

public interface SellerInfoService {

    /**
     * 판매자 정보 조회
     */
    SellerInfoResponseDTO getSellerInfo(UserPrincipal userPrincipal);

    /**
     * 판매자 정보 등록/수정
     */
    SellerInfoResponseDTO upsertSellerInfo(UserPrincipal userPrincipal, SellerInfoRequestDTO request);
}