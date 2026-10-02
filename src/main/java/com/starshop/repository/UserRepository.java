package com.starshop.repository;

import com.starshop.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Tìm kiếm có phân trang, nạp sẵn store/carrier để tránh N+1 query khi hiển thị danh sách. */
    @Override
    @EntityGraph(attributePaths = {"store", "carrier"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
