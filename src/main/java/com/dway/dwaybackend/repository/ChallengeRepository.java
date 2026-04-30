package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Challenge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChallengeRepository extends JpaRepository<Challenge, UUID> {

    List<Challenge> findByIsActiveTrueOrderByCreatedAtDesc();

    Page<Challenge> findByIsActiveTrue(Pageable pageable);

    boolean existsByTitle(String title);

    @Modifying
    @Query("UPDATE Challenge c SET c.participantCount = c.participantCount + 1 WHERE c.id = :challengeId")
    void incrementParticipantCount(UUID challengeId);

    @Modifying
    @Query("UPDATE Challenge c SET c.participantCount = c.participantCount - 1 WHERE c.id = :challengeId AND c.participantCount > 0")
    void decrementParticipantCount(UUID challengeId);
}