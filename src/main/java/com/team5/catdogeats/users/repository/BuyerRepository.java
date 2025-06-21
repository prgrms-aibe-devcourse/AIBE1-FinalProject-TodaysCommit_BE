package com.team5.catdogeats.users.repository;

import com.team5.catdogeats.users.domain.dto.BuyerDTO;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BuyerRepository extends JpaRepository<Buyers, UUID> {

    @Query("""
      SELECT new com.team5.catdogeats.users.domain.dto.BuyerDTO(
        b.userId,
        b.nameMaskingStatus,
        b.isDeleted,
        b.deledAt
      )
      FROM Buyers b
      JOIN b.user u
      WHERE u.provider = :provider
        AND u.providerId = :providerId
    """)
    Optional<BuyerDTO> findOnlyBuyerByProviderAndProviderId(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );
}
