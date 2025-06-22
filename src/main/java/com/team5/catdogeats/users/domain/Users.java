package com.team5.catdogeats.users.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Table(uniqueConstraints = @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"}))
public class Users extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private String id;

    @Column(length = 36, updatable = false, nullable = false)
    private String provider;
    @Column(length = 100, nullable = false, name = "provider_id")
    private String providerId;
    @Column(length = 50, nullable = false, name = "user_name_attribute")
    private String userNameAttribute;

    @Column(length = 100, nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(length = 15, nullable = false)
    private Role role;

    @Column(nullable = false, name = "account_disable")
    @Builder.Default
    private boolean accountDisable = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;


    public void updateRole(Role role) {
        this.role = role;
    }

    public void reactivationAccount() {
        this.accountDisable = false;
        this.deletedAt = null;
    }

}
