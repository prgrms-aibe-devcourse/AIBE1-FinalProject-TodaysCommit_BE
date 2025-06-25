package com.team5.catdogeats.users.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.users.domain.dto.SellerBrandImageResponseDTO;
import org.springframework.web.multipart.MultipartFile;

public interface SellerBrandImageService {
    /**
     * 판매자 브랜드 이미지 업로드/수정
     */
    SellerBrandImageResponseDTO uploadBrandImage(UserPrincipal userPrincipal, MultipartFile imageFile);
}