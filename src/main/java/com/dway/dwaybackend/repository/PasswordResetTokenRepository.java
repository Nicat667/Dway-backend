package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    Optional<PasswordResetToken> findByUserIdAndCodeAndUsedFalse(UUID userId, String code);

    Optional<PasswordResetToken> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM PasswordResetToken t WHERE t.userId = :userId")
    void deleteAllByUserId(UUID userId);
}