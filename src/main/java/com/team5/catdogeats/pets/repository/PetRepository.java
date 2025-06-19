package com.team5.catdogeats.pets.repository;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PetRepository extends JpaRepository<Pets, UUID> {
    List<Pets> findByBuyer(Buyers buyer);
}
