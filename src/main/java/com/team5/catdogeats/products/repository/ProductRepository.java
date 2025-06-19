package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * 상품 엔티티 Repository (1단계: 재고 차감 포함)
 *
 * 1단계에서는 주문 시 상품 검증과 재고 차감을 수행합니다.
 */
@Repository
public interface ProductRepository extends JpaRepository<Products, UUID> {

    /**
     * 상품 번호로 상품 조회
     *
     * @param productNumber 상품 번호
     * @return 상품 정보 (Optional)
     */
    Optional<Products> findByProductNumber(Long productNumber);

    /**
     * 원자적 재고 차감 (동시성 제어)
     *
     * 재고가 충분한 경우에만 차감을 수행하여 동시성 문제를 방지합니다.
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량 (양수)
     * @return 업데이트된 행 수 (0이면 재고 부족 또는 상품 없음)
     */
    @Modifying
    @Query("UPDATE Products p SET p.quantity = p.quantity - :quantity " +
            "WHERE p.id = :productId AND p.quantity >= :quantity")
    int decreaseQuantity(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    /**
     * 재고 복구 (결제 실패 시 사용 - 2단계에서 사용 예정)
     *
     * @param productId 상품 ID
     * @param quantity 복구할 수량 (양수)
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Products p SET p.quantity = p.quantity + :quantity " +
            "WHERE p.id = :productId")
    int increaseQuantity(@Param("productId") UUID productId, @Param("quantity") Integer quantity);
}