package com.team5.catdogeats.addresses.repository;

import com.team5.catdogeats.addresses.domain.Addresses;
import com.team5.catdogeats.addresses.domain.enums.AddressType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Addresses, UUID> {

    // 사용자별 주소 타입에 따른 주소 목록 조회 (페이징)
    Page<Addresses> findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
            UUID userId, AddressType addressType, Pageable pageable);

    // 사용자별 주소 타입에 따른 주소 목록 조회 (전체)

    List<Addresses> findByUserIdAndAddressTypeOrderByIsDefaultDescCreatedAtDesc(
            UUID userId, AddressType addressType);

    // 사용자의 기본 주소 조회
    Optional<Addresses> findByUserIdAndAddressTypeAndIsDefaultTrue(
            UUID userId, AddressType addressType);

    // 특정 주소가 해당 사용자의 것인지 확인
    boolean existsByIdAndUserId(UUID addressId, UUID userId);

    // 사용자의 주소 개수 조회
    long countByUserIdAndAddressType(UUID userId, AddressType addressType);

    // 사용자의 모든 기본 주소를 비활성화 (기본 주소 변경 시 사용)
    @Modifying
    @Query("UPDATE Addresses a SET a.isDefault = false WHERE a.user.id = :userId AND a.addressType = :addressType")
    void clearDefaultAddresses(@Param("userId") UUID userId, @Param("addressType") AddressType addressType);

    // 특정 주소를 기본 주소로 설정
    @Modifying
    @Query("UPDATE Addresses a SET a.isDefault = true WHERE a.id = :addressId")
    void setAsDefault(@Param("addressId") UUID addressId);
}