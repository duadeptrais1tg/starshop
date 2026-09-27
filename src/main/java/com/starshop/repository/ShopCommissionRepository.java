package com.starshop.repository;

import com.starshop.entity.ShopCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShopCommissionRepository extends JpaRepository<ShopCommission, Long> {
}
