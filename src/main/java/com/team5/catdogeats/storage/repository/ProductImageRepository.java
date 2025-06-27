package com.team5.catdogeats.storage.repository;

import com.team5.catdogeats.storage.domain.mapping.ProductsImages;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductImageRepository extends JpaRepository<ProductsImages, String> {
    Optional<ProductsImages> findByProductsIdAndImagesId(String productId, String imageId);

    List<ProductsImages> findAllByProductsId(String productId);

    @Query("""
        select ri
        from ProductsImages ri
        join fetch ri.images
        where ri.products.id = :productId
    """)
    List<ProductsImages> findAllByProductsIdWithImages(@Param("productId") String productId);

}
