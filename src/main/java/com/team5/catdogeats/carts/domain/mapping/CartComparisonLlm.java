package com.team5.catdogeats.carts.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "orders_compare_llm")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class CartComparisonLlm extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private String buyerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_products_compare_llm_order_id"))
    private Orders orders;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private PetCategory petCategory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contents;

}
