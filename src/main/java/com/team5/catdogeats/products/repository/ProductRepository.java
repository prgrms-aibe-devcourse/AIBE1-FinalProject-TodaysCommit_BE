package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.domain.enums.StockStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 상품 엔티티 Repository (업데이트된 Products 엔티티 적용)
 *
 * Products 엔티티의 quantity 필드가 stock으로 변경됨에 따라
 * 모든 재고 관련 쿼리를 stock 필드로 수정하였습니다.
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
     * 원자적 재고 차감 (동시성 제어) - stock 필드 사용
     *
     * 재고가 충분한 경우에만 차감을 수행하여 동시성 문제를 방지합니다.
     * quantity 필드가 stock 필드로 변경됨에 따라 쿼리를 수정했습니다.
     *
     * @param productId 상품 ID
     * @param quantity 차감할 수량 (양수)
     * @return 업데이트된 행 수 (0이면 재고 부족 또는 상품 없음)
     */
    @Modifying
    @Query("UPDATE Products p SET p.stock = p.stock - :quantity " +
            "WHERE p.id = :productId AND p.stock >= :quantity")
    int decreaseStock(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    /**
     * 재고 복구 (결제 실패 시 사용) - stock 필드 사용
     *
     * @param productId 상품 ID
     * @param quantity 복구할 수량 (양수)
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Products p SET p.stock = p.stock + :quantity " +
            "WHERE p.id = :productId")
    int increaseStock(@Param("productId") UUID productId, @Param("quantity") Integer quantity);

    /**
     * 재고 상태별 상품 조회
     *
     * 새로 추가된 StockStatus enum을 활용한 조회 메서드입니다.
     *
     * @param stockStatus 재고 상태
     * @return 해당 상태의 상품 목록
     */
    List<Products> findByStockStatus(StockStatus stockStatus);

    /**
     * 재고가 특정 수량 이하인 상품 조회
     *
     * 재고 부족 상품 관리를 위한 메서드입니다.
     *
     * @param threshold 기준 재고 수량
     * @return 재고가 기준 이하인 상품 목록
     */
    @Query("SELECT p FROM Products p WHERE p.stock <= :threshold")
    List<Products> findLowStockProducts(@Param("threshold") Integer threshold);

    /**
     * 특정 판매자의 상품 중 재고가 있는 상품만 조회
     *
     * @param sellerId 판매자 ID
     * @return 재고가 있는 상품 목록
     */
    @Query("SELECT p FROM Products p WHERE p.seller.user.id = :sellerId AND p.stock > 0")
    List<Products> findAvailableProductsBySeller(@Param("sellerId") UUID sellerId);

    /**
     * 할인 중인 상품 조회
     *
     * 새로 추가된 할인 관련 필드를 활용한 조회입니다.
     *
     * @return 할인 중인 상품 목록
     */
    @Query("SELECT p FROM Products p WHERE p.isDiscounted = true AND p.discountRate > 0")
    List<Products> findDiscountedProducts();

    /**
     * 재고 상태를 자동으로 업데이트
     *
     * 재고 수량에 따라 재고 상태를 자동으로 설정합니다.
     *
     * @return 업데이트된 행 수
     */
    @Modifying
    @Query("UPDATE Products p SET p.stockStatus = " +
            "CASE WHEN p.stock > 0 THEN 'IN_STOCK' ELSE 'OUT_OF_STOCK' END")
    int updateStockStatusByQuantity();

    /**
     * 상품 재고와 함께 기본 정보 조회 (성능 최적화)
     *
     * 페치 조인을 사용하여 N+1 문제를 방지합니다.
     *
     * @param productId 상품 ID
     * @return 상품 정보 (Optional)
     */
    @Query("SELECT p FROM Products p " +
            "LEFT JOIN FETCH p.seller s " +
            "LEFT JOIN FETCH s.user u " +
            "WHERE p.id = :productId")
    Optional<Products> findByIdWithSeller(@Param("productId") UUID productId);
}