package com.team5.catdogeats.pets.repository;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pets, String> {
    List<Pets> findByBuyer(Buyers buyer);

    Optional<Pets> findById(String id);

    void deleteById(String id);
}
