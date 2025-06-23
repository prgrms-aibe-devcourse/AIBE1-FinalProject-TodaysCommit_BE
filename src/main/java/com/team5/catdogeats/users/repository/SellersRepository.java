package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.dto.SellerDTO;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellersRepository extends JpaRepository<Sellers, String> {

    /**
     * 사용자 ID로 판매자 정보 조회
     */
    Optional<Sellers> findByUserId(String userId);

    /**
     * 사업자 등록번호로 판매자 정보 조회 - 사업자 중복등록 검증용
     */
    Optional<Sellers> findByBusinessNumber(String businessNumber);



    @Query("""
        SELECT new com.team5.catdogeats.users.domain.dto.SellerDTO(
            s.userId,
            s.vendorName,
            s.vendorProfileImage,
            s.businessNumber,
            s.settlementBank,
            s.settlementAccount,
            s.tags,
            s.operatingStartTime,
            s.operatingEndTime,
            s.closedDays,
            s.isDeleted,
            s.deledAt
        )
        FROM Sellers s
        JOIN s.user u
        WHERE u.provider = :provider
        AND u.providerId = :providerId
    """)
    Optional<SellerDTO> findSellerDtoByProviderAndProviderId(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );
}
