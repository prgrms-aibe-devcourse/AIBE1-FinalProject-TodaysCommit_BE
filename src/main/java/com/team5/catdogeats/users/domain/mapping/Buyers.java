package com.team5.catdogeats.users.domain.mapping;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Buyers extends BaseEntity {
    @Id
    @Column(name = "user_id", length = 36)
    private UUID userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_buyers_user_id"))
    private Users user;

    @Column(name = "name_masking_status")
    @Builder.Default
    private boolean nameMaskingStatus = true;

    @Column(nullable = false, name = "is_deleted")
    @Builder.Default
    private boolean isDeleted = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deledAt;


}
