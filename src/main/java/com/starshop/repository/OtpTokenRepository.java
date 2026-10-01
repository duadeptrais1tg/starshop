package com.starshop.repository;

import com.starshop.entity.OtpToken;
import com.starshop.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /** OTP mới nhất của user theo loại (id tăng dần nên id lớn nhất là mới nhất). */
    Optional<OtpToken> findTopByUserIdAndTypeOrderByIdDesc(Long userId, OtpType type);

    /** Vô hiệu hóa các OTP cũ còn dùng được, để chỉ mã mới nhất có hiệu lực. */
    @Modifying(flushAutomatically = true)
    @Query("update OtpToken o set o.used = true where o.user.id = :userId and o.type = :type and o.used = false")
    int invalidateActive(@Param("userId") Long userId, @Param("type") OtpType type);
}
