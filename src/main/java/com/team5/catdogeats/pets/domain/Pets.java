package com.team5.catdogeats.pets.domain;

import com.team5.catdogeats.baseEntity.BaseEntity;
import com.team5.catdogeats.pets.domain.dto.PetCreateRequestDto;
import com.team5.catdogeats.pets.domain.dto.PetUpdateRequestDto;
import com.team5.catdogeats.pets.domain.enums.Gender;
import com.team5.catdogeats.pets.domain.enums.PetCategory;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Pets extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", referencedColumnName = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_pets_buyers_id"))
    private Buyers buyer;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    private PetCategory petCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 1)
    private Gender gender;

    @Column(length = 100)
    private String breed;

    @Column
    private Short age;

    public static Pets fromDto(PetCreateRequestDto dto, Buyers buyer) {
        return Pets.builder()
                .buyer(buyer)
                .name(dto.name())
                .petCategory(dto.petCategory())
                .gender(dto.gender())
                .breed(dto.breed())
                .age(dto.age())
                .build();
    }

    public void updateFromDto(PetUpdateRequestDto dto) {
        if (dto.name() != null) this.name = dto.name();
        if (dto.petCategory() != null) this.petCategory = dto.petCategory();
        if (dto.gender() != null) this.gender = dto.gender();
        if (dto.breed() != null) this.breed = dto.breed();
        if (dto.age() != null) this.age = dto.age();
    }
}
