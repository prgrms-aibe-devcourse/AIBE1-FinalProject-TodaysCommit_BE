package com.team5.catdogeats.orders.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.orders.domain.enums.OrderStatus;
import com.team5.catdogeats.orders.domain.mapping.OrderItems;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Orders extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "order_number", nullable = false, unique = true)
    private Long orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_orders_user_id"))
    private Users user;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_status")
    private OrderStatus orderStatus;

    @Column(name = "total_price", nullable = false)
    private Long totalPrice;

    // ===== 배송지 정보 필드 추가 =====
    /**
     * 받는 사람 이름
     * 주문 시점에 입력된 실제 수령인 이름
     */
    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    /**
     * 받는 사람 연락처
     * 주문 시점에 입력된 실제 수령인 연락처
     */
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    /**
     * 우편번호
     */
    @Column(name = "postal_code", length = 10)
    private String postalCode;

    /**
     * 배송 주소
     * 주문 시점에 입력된 전체 배송 주소
     */
    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    /**
     * 상세 주소
     */
    @Column(name = "detail_address", length = 200)
    private String detailAddress;

    /**
     * 배송 요청사항
     * 주문 시점에 입력된 배송 관련 요청사항
     */
    @Column(name = "delivery_note", length = 500)
    private String deliveryNote;

    // 주문 상품 목록
    @OneToMany(mappedBy = "orders", fetch = FetchType.LAZY)
    private List<OrderItems> orderItems;

    // ===== 배송지 정보 설정을 위한 편의 메서드 =====
    /**
     * 주문 시 배송지 정보를 한 번에 설정하는 편의 메서드
     * @param recipientName 받는 사람 이름
     * @param recipientPhone 받는 사람 연락처
     * @param postalCode 우편번호
     * @param streetAddress 기본 주소
     * @param detailAddress 상세 주소
     * @param deliveryNote 배송 요청사항
     */
    public void setShippingInfo(String recipientName, String recipientPhone, String postalCode,
                                String streetAddress, String detailAddress, String deliveryNote) {
        this.recipientName = recipientName;
        this.recipientPhone = recipientPhone;
        this.postalCode = postalCode;
        this.shippingAddress = streetAddress;
        this.detailAddress = detailAddress;
        this.deliveryNote = deliveryNote;
    }

    /**
     * 전체 배송 주소를 조합해서 반환하는 편의 메서드
     * @return 전체 배송 주소 문자열
     */
    public String getFullShippingAddress() {
        if (shippingAddress == null) {
            return "";
        }
        StringBuilder fullAddress = new StringBuilder(shippingAddress);
        if (detailAddress != null && !detailAddress.trim().isEmpty()) {
            fullAddress.append(" ").append(detailAddress);
        }
        return fullAddress.toString().trim();
    }
}