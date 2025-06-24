package com.team5.catdogeats.carts.repository;

import com.team5.catdogeats.carts.domain.mapping.CartItems;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItems, String> {

    List<CartItems> findByCartsId(String cartId);

    Optional<CartItems> findByCartsIdAndProductId(String cartId, String productId);

    @Query("SELECT ci FROM CartItems ci JOIN FETCH ci.product WHERE ci.carts.id = :cartId")
    List<CartItems> findByCartsIdWithProduct(@Param("cartId") String cartId);

    void deleteByCartsIdAndId(String cartId, String cartItemId);

    long countByCartsId(String cartId);
}