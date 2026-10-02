package com.starshop.repository;

import com.starshop.entity.Shop;
import com.starshop.entity.enums.ShopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShopRepository extends JpaRepository<Shop, Long>, JpaSpecificationExecutor<Shop> {

    Optional<Shop> findBySlug(String slug);

    Optional<Shop> findByOwnerId(Long ownerId);

    boolean existsByName(String name);

    long countByStatus(ShopStatus status);

    /** Shop theo trạng thái, sắp theo tên (ví dụ danh sách shop đang hoạt động cho bộ lọc). */
    List<Shop> findByStatusOrderByNameAsc(ShopStatus status);

    /** Danh sách có phân trang, nạp sẵn chủ shop và chi nhánh (tránh N+1). */
    @Override
    @EntityGraph(attributePaths = {"owner", "store"})
    Page<Shop> findAll(Specification<Shop> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "store"})
    List<Shop> findTop5ByStatusOrderByCreatedAtAsc(ShopStatus status);
}
