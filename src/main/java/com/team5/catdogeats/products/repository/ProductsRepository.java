package com.team5.catdogeats.products.repository;


import com.team5.catdogeats.products.domain.Products;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


@Repository
public interface ProductsRepository extends JpaRepository<Products, String> {


    @Query("SELECT COUNT(p) FROM Products p WHERE p.seller.userId = :sellerId")
    Long countSellerActiveProducts(@Param("sellerId") String sellerId);

}