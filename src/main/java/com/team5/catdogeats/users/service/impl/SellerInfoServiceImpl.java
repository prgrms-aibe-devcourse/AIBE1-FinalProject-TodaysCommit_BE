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
        log.info("판매자 정보 조회 - userId: {}", userId);

        // 사용자 존재 여부 및 판매자 권한 확인
        validateSellerUser(userId);

        return getSellerInfoInternal(userId);
    }

    @Override
    @Transactional
    public SellerInfoResponse upsertSellerInfo(UUID userId, SellerInfoRequest request) {
        log.info("판매자 정보 등록/수정 - userId: {}, vendorName: {}", userId, request.vendorName());

        // 사용자 존재 여부 및 판매자 권한 확인
        Users user = validateSellerUser(userId);

        // 운영시간 유효성 검증
        validateOperatingHours(request);

        return upsertSellerInfoInternal(user, userId, request);
    }

    /**
     * 판매자 정보 조회 로직 (권한 검증 분리)
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
     * 판매자 정보 등록/수정 로직
     */
    private SellerInfoResponse upsertSellerInfoInternal(Users user, UUID userId, SellerInfoRequest request) {
        // 사업자 등록번호 중복 체크
        validateBusinessNumberDuplication(userId, request.businessNumber());

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

        if (existingSeller.isPresent()) {
            Sellers seller = existingSeller.get();

            // Null 체크
            UUID existingUserId = seller.getUserId();
            if (existingUserId == null) {
                // userId가 null인 경우 Users 관계에서 가져오기
                if (seller.getUser() != null) {
                    existingUserId = seller.getUser().getId();
                }
            }

            // 다른 사용자가 사용 중인지 확인
            if (existingUserId != null && !existingUserId.equals(userId)) {
                log.warn("사업자 등록번호 중복 - businessNumber: {}, 요청자: {}, 기존사용자: {}",
                        businessNumber, userId, existingUserId);
                throw new BusinessNumberDuplicateException("이미 등록된 사업자 등록번호입니다.");
            }
        }
    }

    /**
     * 운영시간 유효성 검증
     */
    private void validateOperatingHours(SellerInfoRequest request) {
        if (request.operatingStartTime() != null && request.operatingEndTime() != null) {
            if (request.operatingStartTime().toLocalTime()
                    .isAfter(request.operatingEndTime().toLocalTime())) {
                throw new IllegalArgumentException("운영 시작 시간은 종료 시간보다 빠를 수 없습니다.");
            }
        }

        // 시작 시간만 있고 종료 시간이 없는 경우 또는 그 반대인 경우 검증
        if ((request.operatingStartTime() != null && request.operatingEndTime() == null) ||
                (request.operatingStartTime() == null && request.operatingEndTime() != null)) {
            throw new IllegalArgumentException("운영 시작 시간과 종료 시간은 모두 입력하거나 모두 입력하지 않아야 합니다.");
        }

        log.info("운영시간 유효성 검증 완료 - start: {}, end: {}",
                request.operatingStartTime(), request.operatingEndTime());
    }

    /**
     * 기존 판매자 정보 업데이트
     */
    private void updateSellerInfo(Sellers seller, SellerInfoRequest request) {
        seller.updateVendorName(request.vendorName());
        seller.updateVendorProfileImage(request.vendorProfileImage());
        seller.updateBusinessNumber(request.businessNumber());
        seller.updateSettlementBank(request.settlementBank());
        seller.updateSettlementAcc(request.settlementAcc());
        seller.updateTags(request.tags());
        seller.updateOperatingStartTime(request.operatingStartTime());
        seller.updateOperatingEndTime(request.operatingEndTime());
        seller.updateClosedDays(request.closedDays());
    }

    /**
     * 새 판매자 정보 생성
     */
    private Sellers createNewSeller(Users user, SellerInfoRequest request) {
        return Sellers.builder()
                .user(user)
                .vendorName(request.vendorName())
                .vendorProfileImage(request.vendorProfileImage())
                .businessNumber(request.businessNumber())
                .settlementBank(request.settlementBank())
                .settlementAccount(request.settlementAcc())
                .tags(request.tags())
                .operatingStartTime(request.operatingStartTime())
                .operatingEndTime(request.operatingEndTime())
                .closedDays(request.closedDays())
                .build();
    }
}