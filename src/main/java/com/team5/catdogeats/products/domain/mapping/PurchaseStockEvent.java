package com.team5.catdogeats.products.domain.mapping;

import com.team5.catdogeats.products.domain.enums.PurchaseEventStatus;
import com.team5.catdogeats.products.domain.enums.PurchaseEventType;
import jakarta.persistence.*;
import lombok.*;

import java.time.ZonedDateTime;
import java.util.UUID;

@Entity
@Table(name = "purchase_stock_events")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseStockEvent {
    @Id
    private UUID id;                       // RabbitMQ 메시지 eventId와 동일

    private UUID productId;
    private int quantity;

    @Enumerated(EnumType.STRING)
    private PurchaseEventType eventType;   // ORDER_CREATED, ORDER_CANCELLED 등

    @Enumerated(EnumType.STRING)
    private PurchaseEventStatus status;    // PENDING, SUCCESS, FAILED

    private ZonedDateTime processedAt;

}