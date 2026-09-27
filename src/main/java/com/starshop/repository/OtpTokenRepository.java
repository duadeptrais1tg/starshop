package com.starshop.repository;

import com.starshop.entity.OtpToken;
import com.starshop.entity.enums.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    /** OTP mới nhất của user theo loại. */
    Optional<OtpToken> findTopByUserIdAndTypeOrderByCreatedAtDesc(Long userId, OtpType type);
}
