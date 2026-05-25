package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.UserChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserChallengeRepository extends JpaRepository<UserChallenge, UUID> {

    List<UserChallenge> findByUserId(UUID userId);

    Optional<UserChallenge> findByUserIdAndChallengeId(UUID userId, UUID challengeId);

    boolean existsByUserIdAndChallengeId(UUID userId, UUID challengeId);

    @Query("""
            SELECT COUNT(uc) FROM UserChallenge uc
            WHERE uc.userId = :userId AND uc.completedAt IS NOT NULL
            """)
    long countCompletedByUserId(UUID userId);

    List<UserChallenge> findByUserIdAndCompletedAtIsNull(UUID userId);

    void deleteAllByChallengeId(UUID challengeId);
}
