package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellersRepository extends JpaRepository<Sellers, UUID> {

    /**
     * 사용자 ID로 판매자 정보 조회
     */
    Optional<Sellers> findByUserId(UUID userId);

    /**
     * 사업자 등록번호로 판매자 정보 조회 - 사업자 중복등록 검증용
     */
    Optional<Sellers> findByBusinessNumber(String businessNumber);


}
