package com.dway.dwaybackend.repository;

import com.dway.dwaybackend.entity.DailyMotivation;
import com.dway.dwaybackend.entity.enums.Language;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DailyMotivationRepository extends JpaRepository<DailyMotivation, UUID> {

    Optional<DailyMotivation> findByLanguageAndScheduledDate(Language language, LocalDate scheduledDate);

    Page<DailyMotivation> findAllByOrderByScheduledDateDesc(Pageable pageable);

    Page<DailyMotivation> findByLanguageOrderByScheduledDateDesc(Language language, Pageable pageable);

    List<DailyMotivation> findByScheduledDate(LocalDate scheduledDate);

    boolean existsByLanguageAndScheduledDate(Language language, LocalDate scheduledDate);
}