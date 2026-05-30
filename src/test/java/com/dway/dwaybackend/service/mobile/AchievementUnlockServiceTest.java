package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.entity.UserAchievement;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.TaskRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AchievementUnlockService Unit Tests")
class AchievementUnlockServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private TaskRepository taskRepository;
    @Mock private UserChallengeRepository userChallengeRepository;

    @InjectMocks private AchievementUnlockService service;

    private static final UUID USER_ID    = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ACH_TASK_10  = UUID.fromString("10000000-0000-0000-0000-000000000010");
    private static final UUID ACH_TASK_50  = UUID.fromString("10000000-0000-0000-0000-000000000050");
    private static final UUID ACH_CHAL_3   = UUID.fromString("20000000-0000-0000-0000-000000000003");
    private static final UUID ACH_CHAL_10  = UUID.fromString("20000000-0000-0000-0000-000000000010");

    private Achievement taskAch(UUID id, int threshold) {
        return Achievement.builder().id(id).title("T_" + threshold).description("d")
                .icon("🏅").type(AchievementType.TASK_COUNT)
                .threshold(threshold).isActive(true).build();
    }

    private Achievement chalAch(UUID id, int threshold) {
        return Achievement.builder().id(id).title("C_" + threshold).description("d")
                .icon("🏆").type(AchievementType.CHALLENGE_COUNT)
                .threshold(threshold).isActive(true).build();
    }

    // ── EarlyReturn ─────────────────────────────────────────────────────────

    @Nested @DisplayName("EarlyReturn")
    class EarlyReturn {

        @Test @DisplayName("returns early when no active achievements — never queries counts")
        void noActiveAchievements_noCountQueries() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of());

            service.checkAndUnlock(USER_ID);

            verify(taskRepository, never()).countCompletedByUserId(any());
            verify(userChallengeRepository, never()).countCompletedByUserId(any());
            verify(userAchievementRepository, never()).save(any());
        }
    }

    // ── TASK_COUNT ───────────────────────────────────────────────────────────

    @Nested @DisplayName("TASK_COUNT achievements")
    class TaskCountAchievements {

        @Test @DisplayName("unlocks when completed tasks reach threshold")
        void reachesThreshold_unlocks() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(10L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository).save(argThat(ua ->
                    ua.getUserId().equals(USER_ID) && ua.getAchievementId().equals(ACH_TASK_10)));
        }

        @Test @DisplayName("does not unlock when task count is below threshold")
        void belowThreshold_noUnlock() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(9L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, never()).save(any());
        }

        @Test @DisplayName("ignores challenge count for TASK_COUNT achievements")
        void ignoresChallengeCount() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(5L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(100L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, never()).save(any());
        }

        @Test @DisplayName("unlocks multiple when count crosses several thresholds")
        void multipleCrossed_unlocksAll() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10), taskAch(ACH_TASK_50, 50)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(50L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, times(2)).save(any());
        }
    }

    // ── CHALLENGE_COUNT ──────────────────────────────────────────────────────

    @Nested @DisplayName("CHALLENGE_COUNT achievements")
    class ChallengeCountAchievements {

        @Test @DisplayName("unlocks when completed challenges reach threshold")
        void reachesThreshold_unlocks() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(3L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository).save(argThat(ua ->
                    ua.getUserId().equals(USER_ID) && ua.getAchievementId().equals(ACH_CHAL_3)));
        }

        @Test @DisplayName("does not unlock when challenge count is below threshold")
        void belowThreshold_noUnlock() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(2L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, never()).save(any());
        }

        @Test @DisplayName("ignores task count for CHALLENGE_COUNT achievements")
        void ignoresTaskCount() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(100L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(2L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, never()).save(any());
        }
    }

    // ── MixedTypes ───────────────────────────────────────────────────────────

    @Nested @DisplayName("MixedTypes")
    class MixedTypes {

        @Test @DisplayName("unlocks correct achievement for each type independently")
        void mixed_unlocksCorrectly() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10), chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(10L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(3L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, times(2)).save(any());
        }

        @Test @DisplayName("unlocks task achievement but not challenge when challenge count is low")
        void taskMet_challengeNot() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10), chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(10L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(1L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, times(1)).save(argThat(ua ->
                    ua.getAchievementId().equals(ACH_TASK_10)));
        }

        @Test @DisplayName("fetches both counts exactly once regardless of how many achievements")
        void fetchesBothCountsOnce() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(
                            taskAch(ACH_TASK_10, 10), taskAch(ACH_TASK_50, 50),
                            chalAch(ACH_CHAL_3, 3), chalAch(ACH_CHAL_10, 10)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(50L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(10L);

            service.checkAndUnlock(USER_ID);

            verify(taskRepository, times(1)).countCompletedByUserId(USER_ID);
            verify(userChallengeRepository, times(1)).countCompletedByUserId(USER_ID);
        }
    }

    // ── AlreadyEarned ────────────────────────────────────────────────────────

    @Nested @DisplayName("AlreadyEarned")
    class AlreadyEarned {

        @Test @DisplayName("skips already earned achievement")
        void alreadyEarned_skips() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Set.of(ACH_TASK_10));
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(10L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, never()).save(any());
        }

        @Test @DisplayName("awards only the new one when one is already earned")
        void partiallyEarned_awardsOnlyNew() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(taskAch(ACH_TASK_10, 10), taskAch(ACH_TASK_50, 50)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Set.of(ACH_TASK_10));
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(50L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);

            service.checkAndUnlock(USER_ID);

            verify(userAchievementRepository, times(1)).save(argThat(ua ->
                    ua.getAchievementId().equals(ACH_TASK_50)));
        }
    }

    // ── EdgeCases ────────────────────────────────────────────────────────────

    @Nested @DisplayName("EdgeCases")
    class EdgeCases {

        @Test @DisplayName("persists correct userId and achievementId")
        void correctIdsPersisted() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc())
                    .thenReturn(List.of(chalAch(ACH_CHAL_3, 3)));
            when(userAchievementRepository.findAchievementIdsByUserId(USER_ID))
                    .thenReturn(Collections.emptySet());
            when(taskRepository.countCompletedByUserId(USER_ID)).thenReturn(0L);
            when(userChallengeRepository.countCompletedByUserId(USER_ID)).thenReturn(5L);

            service.checkAndUnlock(USER_ID);

            ArgumentCaptor<UserAchievement> captor = ArgumentCaptor.forClass(UserAchievement.class);
            verify(userAchievementRepository).save(captor.capture());
            assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
            assertThat(captor.getValue().getAchievementId()).isEqualTo(ACH_CHAL_3);
        }
    }
}