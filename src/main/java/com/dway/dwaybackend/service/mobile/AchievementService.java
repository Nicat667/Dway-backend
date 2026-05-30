package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.dto.response.achievement.AchievementStatusResponse;
import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.entity.UserAchievement;
import com.dway.dwaybackend.mapper.AchievementMapper;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final AchievementMapper achievementMapper;

    /**
     * Returns all ACTIVE achievements, each enriched with whether the
     * requesting user has already earned it. Inactive achievements are
     * hidden from mobile users entirely.
     */
    @Transactional(readOnly = true)
    public List<AchievementStatusResponse> getAllAchievements(UUID userId) {
        List<Achievement> active = achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc();
        if (active.isEmpty()) return List.of();

        Map<UUID, LocalDateTime> earnedMap = buildEarnedMap(userId);

        return active.stream()
                .map(a -> achievementMapper.toStatusResponse(
                        a,
                        earnedMap.containsKey(a.getId()),
                        earnedMap.get(a.getId())))
                .toList();
    }

    /**
     * Returns only the achievements the requesting user has already earned,
     * ordered by unlock date descending (most recent first).
     * Includes achievements that may have been deactivated after being earned —
     * once earned it belongs to the user permanently.
     */
    @Transactional(readOnly = true)
    public List<AchievementStatusResponse> getMyAchievements(UUID userId) {
        List<UserAchievement> earned = userAchievementRepository.findByUserId(userId);
        if (earned.isEmpty()) return List.of();

        Set<UUID> achievementIds = earned.stream().map(UserAchievement::getAchievementId).collect(Collectors.toSet());

        Map<UUID, Achievement> achievementMap = achievementRepository.findAllById(achievementIds)
                .stream()
                .collect(Collectors.toMap(Achievement::getId, Function.identity()));

        return earned.stream()
                .filter(ua -> achievementMap.containsKey(ua.getAchievementId()))
                .sorted((a, b) -> b.getUnlockedAt().compareTo(a.getUnlockedAt()))
                .map(ua -> {
                    Achievement a = achievementMap.get(ua.getAchievementId());
                    return achievementMapper.toStatusResponse(a, true, ua.getUnlockedAt());
                })
                .toList();
    }


    private Map<UUID, LocalDateTime> buildEarnedMap(UUID userId) {
        return userAchievementRepository.findByUserId(userId)
                .stream()
                .collect(Collectors.toMap(UserAchievement::getAchievementId, UserAchievement::getUnlockedAt));
    }
}