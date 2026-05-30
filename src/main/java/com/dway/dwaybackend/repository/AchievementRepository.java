package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.Achievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {

    Page<Achievement> findByIsActive(boolean isActive, Pageable pageable);

    List<Achievement> findAllByIsActiveTrueOrderByThresholdAsc();
}