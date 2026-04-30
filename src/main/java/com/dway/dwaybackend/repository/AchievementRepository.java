package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    Optional<Achievement> findByKey(String key);

    List<Achievement> findAllByOrderByThresholdAsc();

    boolean existsByKey(String key);
}