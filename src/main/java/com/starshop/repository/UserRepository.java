package com.starshop.repository;

import com.starshop.entity.User;
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
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByCarrierId(Long carrierId);

    /** Số shipper theo từng nhà vận chuyển trong 1 query. Mỗi phần tử: [carrierId (Long), số lượng (Long)]. */
    @Query("select u.carrier.id, count(u) from User u where u.carrier.id in :carrierIds group by u.carrier.id")
    List<Object[]> countByCarrierIds(@Param("carrierIds") Collection<Long> carrierIds);

    /** Tìm kiếm có phân trang, nạp sẵn store/carrier để tránh N+1 query khi hiển thị danh sách. */
    @Override
    @EntityGraph(attributePaths = {"store", "carrier"})
    Page<User> findAll(Specification<User> spec, Pageable pageable);
}
