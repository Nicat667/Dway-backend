package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.DailyMotivation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMotivationRepository extends JpaRepository<DailyMotivation, UUID> {

    Page<DailyMotivation> findAllByOrderByLastShownDateAscCreatedAtAsc(Pageable pageable);

    // Round-robin queue: never-shown (NULL) entries come first,
    // then oldest-shown entries, with created_at as tiebreaker.
    // Called once per day on cache miss — result is cached 24h.
    @Query(
            value = "SELECT * FROM daily_motivations ORDER BY last_shown_date ASC NULLS FIRST, created_at ASC LIMIT 1",
            nativeQuery = true
    )
    Optional<DailyMotivation> findNextInQueue();

    // Marks a motivation as shown today. Called immediately after findNextInQueue().
    @Modifying
    @Query("UPDATE DailyMotivation m SET m.lastShownDate = :date WHERE m.id = :id")
    void updateLastShownDate(@Param("id") UUID id, @Param("date") LocalDate date);
}