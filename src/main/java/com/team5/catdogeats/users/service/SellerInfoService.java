package com.team5.catdogeats.users.service;

import com.team5.catdogeats.users.domain.dto.SellerInfoRequest;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponse;

public interface SellerInfoService {

    /**
     * 판매자 정보 조회 (권한 검증 포함) - 정규 API용
     * JWT에서 추출한 userId와 일치하는 판매자 정보만 조회
     *
     * @param userId 현재 로그인한 판매자의 사용자 ID (UUID)
     * @return 판매자 정보 응답 DTO (없으면 null)
     */
    SellerInfoResponse getSellerInfo(String userId);



    /**
     * 판매자 정보 등록/수정 (권한 검증 포함) - 정규 API용
     * JWT에서 추출한 userId에 해당하는 판매자 정보만 등록/수정
     *
     * @param userId 현재 로그인한 판매자의 사용자 ID (UUID)
     * @param request 판매자 정보 요청 DTO
     * @return 저장된 판매자 정보 응답 DTO
     */
    SellerInfoResponse upsertSellerInfo(String userId, SellerInfoRequest request);

}