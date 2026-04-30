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

    // Kept — used in analytics and achievement check (JOIN_CHALLENGE, ALL_CHALLENGES)
    @Query("""
            SELECT COUNT(uc) FROM UserChallenge uc
            WHERE uc.userId = :userId AND uc.completedAt IS NOT NULL
            """)
    long countCompletedByUserId(UUID userId);

    // Lightweight Set for isJoined computation in service
    @Query("SELECT uc.challengeId FROM UserChallenge uc WHERE uc.userId = :userId")
    Set<UUID> findChallengeIdsByUserId(UUID userId);
}
