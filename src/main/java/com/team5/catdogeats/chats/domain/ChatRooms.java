package com.team5.catdogeats.chats.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "chat_rooms", uniqueConstraints = {
        @UniqueConstraint(name = "uk_chat_rooms", columnNames = {"buyer_id", "seller_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRooms extends BaseEntity {

    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_rooms_buyer"))
    private Buyers buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_chat_rooms_seller"))
    private Sellers seller;
}