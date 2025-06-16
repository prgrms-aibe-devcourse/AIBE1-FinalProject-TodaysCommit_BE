package com.team5.catdogeats.orders.domain.mapping;

import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.products.domain.Products;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;


@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItems {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Orders orders;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Products products;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private Long price; // 주문 시점 상품 가격
}
