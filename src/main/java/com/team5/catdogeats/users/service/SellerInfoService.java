package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

public interface SellerInfoService {

    /**
     * 판매자 정보 조회
     */
    SellerInfoResponse getSellerInfo(UserPrincipal userPrincipal);

    /**
     * 판매자 정보 등록/수정
     */
    SellerInfoResponse upsertSellerInfo(UserPrincipal userPrincipal, SellerInfoRequest request);
}