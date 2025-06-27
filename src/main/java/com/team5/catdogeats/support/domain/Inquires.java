package com.team5.catdogeats.support.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.users.domain.Users;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "inquiries")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
public class Inquires extends BaseEntity {
    @Id
    @Column(length = 36)
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users users;

    // 자기참조 관계, 답글 기능
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Inquires parent;

    @OneToMany(mappedBy = "parent")
    @ToString.Exclude // 무한 루프 방지
    @JsonIgnore // 순환 참조 방지
    private List<Inquires> replies;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admins admins;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InquiryStatus inquiryStatus = InquiryStatus.PENDING;
}
