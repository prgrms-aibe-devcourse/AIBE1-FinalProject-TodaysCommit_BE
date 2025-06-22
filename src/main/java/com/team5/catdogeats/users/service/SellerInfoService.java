package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;

public interface SellerInfoService {

    /**
     * 판매자 정보 조회 (JWT 기반 인증)
     * UserPrincipal을 통해 사용자를 식별하고 해당 판매자 정보를 조회
     *
     * @param userPrincipal JWT에서 추출한 사용자 정보 (provider, providerId)
     * @return 판매자 정보 응답 DTO (없으면 null)
     */
    SellerInfoResponse getSellerInfo(UserPrincipal userPrincipal);

    /**
     * 판매자 정보 등록/수정 (JWT 기반 인증)
     * UserPrincipal을 통해 사용자를 식별하고 해당 판매자 정보를 등록/수정
     *
     * @param userPrincipal JWT에서 추출한 사용자 정보 (provider, providerId)
     * @param request 판매자 정보 요청 DTO
     * @return 저장된 판매자 정보 응답 DTO
     */
    SellerInfoResponse upsertSellerInfo(UserPrincipal userPrincipal, SellerInfoRequest request);

}