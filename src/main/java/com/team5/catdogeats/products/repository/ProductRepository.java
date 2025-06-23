package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Products, UUID> {
    Optional<Products> findById(UUID productId);
    Boolean existsByProductNumber(Long productNumber);
}
