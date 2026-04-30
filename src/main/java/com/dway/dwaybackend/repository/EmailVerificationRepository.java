package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findByUserIdAndCodeAndUsedFalse(UUID userId, String code);

    Optional<EmailVerification> findTopByUserIdOrderByCreatedAtDesc(UUID userId);

    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.userId = :userId")
    void deleteAllByUserId(UUID userId);
}