package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.dto.SellerInfoRequest;
import com.team5.catdogeats.users.dto.SellerInfoResponse;
import com.team5.catdogeats.users.exception.BusinessNumberDuplicateException;
import com.team5.catdogeats.users.exception.SellerAccessDeniedException;
import com.team5.catdogeats.users.exception.UserNotFoundException;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.SellerInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerInfoServiceImpl implements SellerInfoService {

    private final SellersRepository sellersRepository;
    private final UserRepository userRepository;

    @Override
    public SellerInfoResponse getSellerInfo(UUID userId) {
        log.info("판매자 정보 조회 (권한 검증 포함) - userId: {}", userId);

        // 사용자 존재 여부 및 판매자 권한 확인
        validateSellerUser(userId);

        return getSellerInfoInternal(userId);
    }

    @Override
    public SellerInfoResponse getSellerInfoWithoutAuth(UUID userId) {
        log.info("판매자 정보 조회 (권한 검증 없음) - userId: {}", userId);

        // 사용자 존재 여부만 확인 (권한 확인 안함)
        if (!userRepository.existsById(userId)) {
            throw new UserNotFoundException("존재하지 않는 사용자입니다.");
        }

        return getSellerInfoInternal(userId);
    }

    @Override
    @Transactional
    public SellerInfoResponse upsertSellerInfo(UUID userId, SellerInfoRequest request) {
        log.info("판매자 정보 등록/수정 (권한 검증 포함) - userId: {}, vendorName: {}", userId, request.getVendorName());

        // 사용자 존재 여부 및 판매자 권한 확인
        Users user = validateSellerUser(userId);

        return upsertSellerInfoInternal(user, userId, request);
    }

    @Override
    @Transactional
    public SellerInfoResponse upsertSellerInfoWithoutAuth(UUID userId, SellerInfoRequest request) {
        log.info("판매자 정보 등록/수정 (권한 검증 없음) - userId: {}, vendorName: {}", userId, request.getVendorName());

        // 사용자 존재 여부만 확인 (권한 확인 안함)
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        return upsertSellerInfoInternal(user, userId, request);
    }

    /**
     * 판매자 정보 조회 공통 로직 (권한 검증 분리)
     */
    private SellerInfoResponse getSellerInfoInternal(UUID userId) {
        Optional<Sellers> sellerOpt = sellersRepository.findByUserId(userId);

        if (sellerOpt.isEmpty()) {
            log.info("등록된 판매자 정보가 없습니다 - userId: {}", userId);
            return null;
        }

        return SellerInfoResponse.from(sellerOpt.get());
    }

    /**
     * 판매자 정보 등록/수정 공통 로직 (권한 검증 분리)
     */
    private SellerInfoResponse upsertSellerInfoInternal(Users user, UUID userId, SellerInfoRequest request) {
        // 사업자 등록번호 중복 체크
        validateBusinessNumberDuplication(userId, request.getBusinessNumber());

        // 기존 판매자 정보 조회
        Optional<Sellers> existingSellerOpt = sellersRepository.findByUserId(userId);

        Sellers seller;
        if (existingSellerOpt.isPresent()) {
            // 기존 정보 수정
            seller = existingSellerOpt.get();
            updateSellerInfo(seller, request);
            log.info("판매자 정보 수정 완료 - userId: {}", userId);
        } else {
            // 신규 정보 등록
            seller = createNewSeller(user, request);
            log.info("판매자 정보 신규 등록 완료 - userId: {}", userId);
        }

        Sellers savedSeller = sellersRepository.save(seller);
        return SellerInfoResponse.from(savedSeller);
    }

    /**
     * 판매자 사용자 검증 (존재 여부 + 판매자 권한)
     */
    private Users validateSellerUser(UUID userId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("존재하지 않는 사용자입니다."));

        if (user.getRole() != Role.ROLE_SELLER) {
            log.warn("판매자 권한이 없는 사용자의 접근 시도 - userId: {}, role: {}", userId, user.getRole());
            throw new SellerAccessDeniedException("판매자 권한이 필요합니다.");
        }

        log.info("판매자 권한 확인 완료 - userId: {}", userId);
        return user;
    }

    /**
     * 사업자 등록번호 중복 검증
     */
    private void validateBusinessNumberDuplication(UUID userId, String businessNumber) {
        Optional<Sellers> existingSeller = sellersRepository.findByBusinessNumber(businessNumber);

        if (existingSeller.isPresent() && !existingSeller.get().getUserId().equals(userId)) {
            log.warn("사업자 등록번호 중복 - businessNumber: {}, 요청자: {}, 기존사용자: {}",
                    businessNumber, userId, existingSeller.get().getUserId());
            throw new BusinessNumberDuplicateException("이미 등록된 사업자 등록번호입니다.");
        }
    }

    /**
     * 기존 판매자 정보 업데이트
     */
    private void updateSellerInfo(Sellers seller, SellerInfoRequest request) {
        seller.updateVendorName(request.getVendorName());
        seller.updateVendorProfileImage(request.getVendorProfileImage());
        seller.updateBusinessNumber(request.getBusinessNumber());
        seller.updateSettlementBank(request.getSettlementBank());
        seller.updateSettlementAcc(request.getSettlementAcc());
        seller.updateTags(request.getTags());
    }

    /**
     * 새 판매자 정보 생성
     */
    private Sellers createNewSeller(Users user, SellerInfoRequest request) {
        return Sellers.builder()
                .user(user)
                .vendorName(request.getVendorName())
                .vendorProfileImage(request.getVendorProfileImage())
                .businessNumber(request.getBusinessNumber())
                .settlementBank(request.getSettlementBank())
                .settlementAcc(request.getSettlementAcc())
                .tags(request.getTags())
                .build();
    }
}