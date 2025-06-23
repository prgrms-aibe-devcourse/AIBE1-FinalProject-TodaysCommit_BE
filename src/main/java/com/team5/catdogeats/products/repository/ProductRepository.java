package com.team5.catdogeats.products.repository;

import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Products, String> {
    Optional<Products> findById(String productId);

    Boolean existsByProductNumber(Long productNumber);

    void deleteById(String productId);
}
