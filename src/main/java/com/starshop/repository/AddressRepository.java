package com.starshop.repository;

import com.starshop.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserId(Long userId);

    /** Địa chỉ của user: mặc định lên đầu, sau đó mới cập nhật gần nhất. */
    List<Address> findByUserIdOrderByDefaultAddressDescUpdatedAtDesc(Long userId);

    /** Chỉ tìm trong địa chỉ của chính user -> không thao tác được địa chỉ người khác. */
    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    /** Bỏ cờ mặc định của mọi địa chỉ khác (để chỉ còn 1 địa chỉ mặc định). */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Address a set a.defaultAddress = false where a.user.id = :userId and a.id <> :keepId")
    int clearDefaultExcept(@Param("userId") Long userId, @Param("keepId") Long keepId);
}
