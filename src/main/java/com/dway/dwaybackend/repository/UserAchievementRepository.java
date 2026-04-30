package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, UUID> {

    List<UserAchievement> findByUserId(UUID userId);

    Optional<UserAchievement> findByUserIdAndAchievementId(UUID userId, UUID achievementId);

    boolean existsByUserIdAndAchievementId(UUID userId, UUID achievementId);

    @Query("SELECT ua.achievementId FROM UserAchievement ua WHERE ua.userId = :userId")
    Set<UUID> findAchievementIdsByUserId(UUID userId);

    long countByUserId(UUID userId);
}