package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.entity.DailyMotivation;
import com.dway.dwaybackend.mapper.MotivationMapper;
import com.dway.dwaybackend.repository.DailyMotivationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class MotivationService {

    private final DailyMotivationRepository motivationRepository;
    private final MotivationMapper motivationMapper;

    // Cache key = today's date e.g. "2025-09-15".
    //
    // On cache MISS (first request of the day):
    //   1. Find the next motivation in the round-robin queue
    //      (NULL last_shown_date first, then oldest-shown, tiebreak by created_at).
    //   2. Stamp it with today's date so it moves to the back of the queue.
    //   3. Cache the result for 24h.
    //
    // On cache HIT (all subsequent requests that day):
    //   Method body never executes — DB is not touched.
    //
    // When all motivations have been shown, the one with the oldest
    // last_shown_date is picked next — the cycle starts over automatically.
    @Cacheable(value = "motivations", key = "T(java.time.LocalDate).now().toString()")
    @Transactional
    public MotivationResponse getTodayMotivation() {
        DailyMotivation motivation = motivationRepository.findNextInQueue().orElseThrow(MotivationNotFoundException::new);

        motivationRepository.updateLastShownDate(motivation.getId(), LocalDate.now());
        log.debug("Today's motivation: id={} last_shown_date set to {}", motivation.getId(), LocalDate.now());

        return motivationMapper.toResponse(motivation);
    }
}