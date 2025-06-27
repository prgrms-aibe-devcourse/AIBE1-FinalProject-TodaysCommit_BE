package com.team5.catdogeats.pets.repository;

import com.team5.catdogeats.pets.domain.Pets;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pets, String> {
    Page<Pets> findByBuyer(Buyers buyer, Pageable pageable);

    Optional<Pets> findById(String id);

    void deleteById(String id);

    // 리뷰에서 작성자의 pet정보 추출 위해
    List<Pets> findByBuyer(Buyers buyer);
}
