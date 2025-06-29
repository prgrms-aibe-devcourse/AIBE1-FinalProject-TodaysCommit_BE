package com.team5.catdogeats.users.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.DayOfWeek;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import com.team5.catdogeats.users.domain.dto.SellerInfoRequestDTO;
import com.team5.catdogeats.users.domain.dto.SellerInfoResponseDTO;
import com.team5.catdogeats.users.repository.SellersRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import com.team5.catdogeats.users.service.SellerInfoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerInfoServiceImpl implements SellerInfoService {

    private final SellersRepository sellersRepository;
    private final UserRepository userRepository;

    @Override
    public SellerInfoResponseDTO getSellerInfo(UserPrincipal userPrincipal) {
        log.info("판매자 정보 조회 (JWT) - provider: {}, providerId: {}",
                userPrincipal.provider(), userPrincipal.providerId());

        // Users 조회
        Users user = findUserByPrincipal(userPrincipal);

        // 판매자 정보 조회
        return getSellerInfoInternal(user.getId());
    }
    @JpaTransactional
    @Override
    public SellerInfoResponseDTO upsertSellerInfo(UserPrincipal userPrincipal, SellerInfoRequestDTO request) {
        log.info("판매자 정보 등록/수정 (JWT) - provider: {}, providerId: {}, vendorName: {}",
                userPrincipal.provider(), userPrincipal.providerId(), request.vendorName());

        // Users 조회
        Users user = findUserByPrincipal(userPrincipal);


        // 운영시간, 휴무일 유효성 검증
        validateOperatingHours(request);
        validateClosedDays(request.closedDays());

        // 판매자 정보 등록/수정
        return upsertSellerInfoInternal(user, request);
    }



    // === 공통 헬퍼 메서드들 ===

    /**
     *  Users 엔티티 조회
     */
    private Users findUserByPrincipal(UserPrincipal userPrincipal) {
        return userRepository.findByProviderAndProviderId(
                userPrincipal.provider(),
                userPrincipal.providerId()
        ).orElseThrow(() -> new EntityNotFoundException(
                String.format("사용자를 찾을 수 없습니다 - provider: %s, providerId: %s",
                        userPrincipal.provider(), userPrincipal.providerId())));
    }


    /**
     * 판매자 정보 조회 로직 (권한 검증 분리)
     */
    private SellerInfoResponseDTO getSellerInfoInternal(String userId) {
        Optional<Sellers> sellerOpt = sellersRepository.findByUserId(userId);

        if (sellerOpt.isEmpty()) {
            log.info("등록된 판매자 정보가 없습니다 - userId: {}", userId);
            return null;
        }

        return SellerInfoResponseDTO.from(sellerOpt.get());
    }

    /**
     * 판매자 정보 등록/수정 로직
     */
    private SellerInfoResponseDTO upsertSellerInfoInternal(Users user, SellerInfoRequestDTO request) {
        String userId = user.getId();

        // 기존 판매자 정보 조회
        Optional<Sellers> existingSellerOpt = sellersRepository.findByUserId(userId);

        if (existingSellerOpt.isEmpty()) {
            // 신규 등록 - 필수 필드 체크 추가
            if (!request.isCreateRequest()) {
                throw new IllegalArgumentException("신규 등록 시 업체명,사업자 등록번호는 필수입니다.");
            }

            //이름 중복 검증
            validateVendorNameDuplication(userId, request.vendorName());
            //사업자 번호 중복 검증
            validateBusinessNumberDuplication(userId, request.businessNumber());

            // 신규 생성
            Sellers seller = createNewSeller(user, request);
            log.info("판매자 정보 신규 등록 완료 - userId: {}", userId);
            Sellers savedSeller = sellersRepository.save(seller);
            return SellerInfoResponseDTO.from(savedSeller);

        } else {
            Sellers seller = existingSellerOpt.get();
            updateSellerInfoPatch(seller, request, userId);
            log.info("판매자 정보 수정 완료 - userId: {}", userId);
            Sellers savedSeller = sellersRepository.save(seller);
            return SellerInfoResponseDTO.from(savedSeller);
        }
    }


    private void updateSellerInfoPatch(Sellers seller, SellerInfoRequestDTO request, String userId) {
        if (request.vendorName() != null && !request.vendorName().trim().isEmpty()) {
            validateVendorNameDuplication(userId, request.vendorName());
            seller.updateVendorName(request.vendorName());
        }


        if (request.businessNumber() != null && !request.businessNumber().trim().isEmpty()) {
            validateBusinessNumberDuplication(userId, request.businessNumber());
            seller.updateBusinessNumber(request.businessNumber());
        }

        if (request.settlementBank() != null) {
            seller.updateSettlementBank(request.settlementBank());
        }

        if (request.settlementAcc() != null) {
            seller.updateSettlementAcc(request.settlementAcc());
        }

        if (request.tags() != null) {
            seller.updateTags(request.tags());
        }

        if (request.operatingStartTime() != null) {
            seller.updateOperatingStartTime(request.operatingStartTime());
        }

        if (request.operatingEndTime() != null) {
            seller.updateOperatingEndTime(request.operatingEndTime());
        }

        if (request.closedDays() != null) {
            seller.updateClosedDays(request.closedDays());
        }
    }


    /**
     * 사업자 등록번호 중복 검증
     */
    private void validateBusinessNumberDuplication(String userId, String businessNumber) {
        Optional<Sellers> existingSeller = sellersRepository.findByBusinessNumber(businessNumber);

        if (existingSeller.isPresent()) {
            Sellers seller = existingSeller.get();
            String existingUserId = seller.getUserId();

            if (existingUserId == null && seller.getUser() != null) {
                existingUserId = seller.getUser().getId();
            }

            if (existingUserId != null && !existingUserId.equals(userId)) {
                log.warn("사업자 등록번호 중복 - businessNumber: {}, 요청자: {}, 기존사용자: {}",
                        businessNumber, userId, existingUserId);
                throw new DataIntegrityViolationException("이미 등록된 사업자 등록번호입니다: " + businessNumber);
            }
        }
    }

    /**
     * 운영시간 유효성 검증
     */
    private void validateOperatingHours(SellerInfoRequestDTO request) {
        if (request.operatingStartTime() != null && request.operatingEndTime() != null) {
            if (request.operatingStartTime().isAfter(request.operatingEndTime())) {
                throw new IllegalArgumentException("운영 시작 시간은 종료 시간보다 빠를 수 없습니다");
            }
        }

        if ((request.operatingStartTime() != null && request.operatingEndTime() == null) ||
                (request.operatingStartTime() == null && request.operatingEndTime() != null)) {
            throw new IllegalArgumentException("운영 시작 시간과 종료 시간은 모두 입력하거나 모두 입력하지 않아야 합니다.");
        }

        log.debug("운영시간 유효성 검증 완료 - start: {}, end: {}",
                request.operatingStartTime(), request.operatingEndTime());
    }

    /**
     * 휴무일 유효성 검증
     */
    private void validateClosedDays(String closedDays) {
        if (closedDays == null || closedDays.trim().isEmpty()) {
            log.debug("휴무일이 설정되지 않음 (null 또는 빈 값)");
            return;
        }

        try {
            List<DayOfWeek> days = DayOfWeek.parseFromString(closedDays);
            log.debug("휴무일 유효성 검증 완료 - closedDays: {}, parsed: {}", closedDays, days);
        } catch (IllegalArgumentException e) {
            log.warn("유효하지 않은 휴무일 입력 - closedDays: {}, error: {}", closedDays, e.getMessage());
            throw new IllegalArgumentException("유효하지 않은 요일이 포함되어 있습니다: " + closedDays, e);
        }
    }


    private void validateVendorNameDuplication(String userId, String vendorName) {
        sellersRepository.findByVendorName(vendorName)
                .filter(seller -> !userId.equals(seller.getUserId()))
                .ifPresent(seller -> {
                    log.warn("상점명 중복 시도 - vendorName: {}, 요청자: {}, 기존사용자: {}",
                            vendorName, userId, seller.getUserId());
                    throw new DataIntegrityViolationException("이미 사용 중인 상점명입니다: " + vendorName);
                });
    }
    /**
     * 새 판매자 정보 생성
     */
    private Sellers createNewSeller(Users user, SellerInfoRequestDTO request) {
        return Sellers.builder()
                .user(user)
                .vendorName(request.vendorName())
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