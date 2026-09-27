package com.starshop.repository;

import com.starshop.entity.ViewedProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ViewedProductRepository extends JpaRepository<ViewedProduct, Long> {

    Optional<ViewedProduct> findByUserIdAndProductId(Long userId, Long productId);
}
