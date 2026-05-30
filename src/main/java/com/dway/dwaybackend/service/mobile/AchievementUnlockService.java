package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.entity.UserAchievement;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.TaskRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementUnlockService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final TaskRepository taskRepository;
    private final UserChallengeRepository userChallengeRepository;

    @Transactional
    public void checkAndUnlock(UUID userId) {
        List<Achievement> active = achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc();

        if (active.isEmpty()) return;

        Set<UUID> alreadyEarned = userAchievementRepository.findAchievementIdsByUserId(userId);

        long completedTasks      = taskRepository.countCompletedByUserId(userId);
        long completedChallenges = userChallengeRepository.countCompletedByUserId(userId);

        for (Achievement achievement : active) {
            if (alreadyEarned.contains(achievement.getId())) continue;

            long count = achievement.getType() == AchievementType.TASK_COUNT
                    ? completedTasks
                    : completedChallenges;

            if (count >= achievement.getThreshold()) {
                userAchievementRepository.save(
                        UserAchievement.builder()
                                .userId(userId)
                                .achievementId(achievement.getId())
                                .build());

                log.info("User {} unlocked '{}' (type={} threshold={})", userId, achievement.getTitle(), achievement.getType(), achievement.getThreshold());
            }
        }
    }
}