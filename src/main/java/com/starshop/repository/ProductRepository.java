package com.starshop.repository;

import com.starshop.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySlug(String slug);

    long countByCategoryId(Long categoryId);

    /**
     * Đếm sản phẩm theo từng danh mục trong một lần query (thay vì đếm từng danh mục).
     * Mỗi phần tử: [categoryId (Long), số sản phẩm (Long)].
     */
    @Query("select p.category.id, count(p) from Product p where p.category.id in :ids group by p.category.id")
    List<Object[]> countByCategoryIds(@Param("ids") Collection<Long> ids);

    /** Danh sách có phân trang, nạp sẵn shop để hiển thị tên shop trên card (tránh N+1). */
    @Override
    @EntityGraph(attributePaths = "shop")
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);
}
