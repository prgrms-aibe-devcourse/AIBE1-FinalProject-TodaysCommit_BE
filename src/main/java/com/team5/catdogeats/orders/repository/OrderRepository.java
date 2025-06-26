package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.users.domain.Users;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 주문 엔티티 Repository (타입 수정됨)
 * JPA를 사용한 주문 관련 데이터 접근 계층입니다.
 * 기본 CRUD 기능과 주문 관련 조회 기능을 제공합니다.
 * Orders 엔티티의 실제 ID 타입(String)에 맞게 수정되었습니다.
 */
public interface OrderRepository extends JpaRepository<Orders, String> {

    /**
     * 주문 번호로 주문 조회
     * @param orderNumber 주문 번호
     * @return 주문 정보 (Optional)
     */
    Optional<Orders> findByOrderNumber(Long orderNumber);

    /**
     * 사용자 ID와 주문 번호로 주문 조회
     * (본인의 주문만 조회하도록 보안 강화)
     * @param user 사용자 엔티티
     * @param orderNumber 주문 번호
     * @return 주문 정보 (Optional)
     */
    Optional<Orders> findByUserAndOrderNumber(Users user, Long orderNumber);

    /**
     * 특정 사용자의 주문 목록 조회 (페이징)
     * 최신 주문부터 정렬
     * @param user 사용자 엔티티
     * @param pageable 페이징 정보
     * @return 주문 목록 (페이징)
     */
    @Query("SELECT o FROM Orders o WHERE o.user = :user ORDER BY o.createdAt DESC")
    Page<Orders> findByUserOrderByCreatedAtDesc(@Param("user") Users user, Pageable pageable);

    /**
     * 특정 사용자의 특정 상태 주문 목록 조회
     * @param user 사용자 엔티티
     * @param orderStatus 주문 상태
     * @param pageable 페이징 정보
     * @return 특정 상태의 주문 목록
     */
    Page<Orders> findByUserAndOrderStatusOrderByCreatedAtDesc(
            Users user, OrderStatus orderStatus, Pageable pageable);

    /**
     * 특정 사용자의 주문 개수 조회
     * @param user 사용자 엔티티
     * @return 해당 사용자의 총 주문 개수
     */
    long countByUser(Users user);

    /**
     * 주문 상태별 개수 조회 (관리자용)
     * @param orderStatus 주문 상태
     * @return 해당 상태의 주문 개수
     */
    long countByOrderStatus(OrderStatus orderStatus);

    /**
     * 주문이 존재하는지 확인
     * @param user 사용자 엔티티
     * @param orderNumber 주문 번호
     * @return 존재 여부
     */
    boolean existsByUserAndOrderNumber(Users user, Long orderNumber);

}