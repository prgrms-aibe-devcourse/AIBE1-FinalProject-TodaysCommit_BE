package com.team5.catdogeats.orders.repository;

import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * OrderItems 엔티티 Repository
 * 주문 상품 정보 관리를 위한 최소한의 데이터 접근 계층입니다.
 */
public interface OrderItemRepository extends JpaRepository<OrderItems, String> {

}