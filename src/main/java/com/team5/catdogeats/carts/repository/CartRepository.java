package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.carts.domain.Carts;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository <Carts, String> {
    Optional<Carts> findByUserId(String userId);

    boolean existsByUserId(String userId);
}
