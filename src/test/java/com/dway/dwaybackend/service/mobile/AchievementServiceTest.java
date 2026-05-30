package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.dto.response.achievement.AchievementStatusResponse;
import com.dway.dwaybackend.entity.Achievement;
import com.dway.dwaybackend.entity.UserAchievement;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.mapper.AchievementMapper;
import com.dway.dwaybackend.repository.AchievementRepository;
import com.dway.dwaybackend.repository.UserAchievementRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AchievementService Unit Tests")
class AchievementServiceTest {

    @Mock private AchievementRepository achievementRepository;
    @Mock private UserAchievementRepository userAchievementRepository;
    @Mock private AchievementMapper achievementMapper;

    @InjectMocks private AchievementService service;

    private static final UUID USER_ID   = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID ACH_ID_5  = UUID.fromString("10000000-0000-0000-0000-000000000005");
    private static final UUID ACH_ID_10 = UUID.fromString("10000000-0000-0000-0000-000000000010");

    private Achievement ach(UUID id, int threshold) {
        return Achievement.builder().id(id).title("T_" + threshold).description("d")
                .icon("🏅").type(AchievementType.TASK_COUNT)
                .threshold(threshold).isActive(true).createdAt(LocalDateTime.now()).build();
    }

    private UserAchievement ua(UUID achievementId, LocalDateTime unlockedAt) {
        return UserAchievement.builder().id(UUID.randomUUID()).userId(USER_ID)
                .achievementId(achievementId).unlockedAt(unlockedAt).build();
    }

    private AchievementStatusResponse statusResp(Achievement a, boolean earned, LocalDateTime unlockedAt) {
        return AchievementStatusResponse.builder()
                .id(a.getId()).title(a.getTitle()).description(a.getDescription())
                .icon(a.getIcon()).type(a.getType()).threshold(a.getThreshold())
                .earned(earned).unlockedAt(unlockedAt).build();
    }

    @Nested @DisplayName("getAllAchievements()")
    class GetAllAchievements {

        @Test @DisplayName("returns active achievements with earned flags")
        void mixedEarned_enrichedCorrectly() {
            Achievement a5  = ach(ACH_ID_5, 5);
            Achievement a10 = ach(ACH_ID_10, 10);
            LocalDateTime unlockedAt = LocalDateTime.now().minusDays(1);

            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc()).thenReturn(List.of(a5, a10));
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(ua(ACH_ID_5, unlockedAt)));
            when(achievementMapper.toStatusResponse(a5, true, unlockedAt)).thenReturn(statusResp(a5, true, unlockedAt));
            when(achievementMapper.toStatusResponse(a10, false, null)).thenReturn(statusResp(a10, false, null));

            List<AchievementStatusResponse> result = service.getAllAchievements(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).isEarned()).isTrue();
            assertThat(result.get(0).getUnlockedAt()).isEqualTo(unlockedAt);
            assertThat(result.get(1).isEarned()).isFalse();
            assertThat(result.get(1).getUnlockedAt()).isNull();
        }

        @Test @DisplayName("returns empty list when no active achievements")
        void noActive_returnsEmpty() {
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc()).thenReturn(List.of());
            assertThat(service.getAllAchievements(USER_ID)).isEmpty();
            verify(userAchievementRepository, never()).findByUserId(any());
        }

        @Test @DisplayName("all earned — all flagged true")
        void allEarned_allTrue() {
            Achievement a5 = ach(ACH_ID_5, 5);
            Achievement a10 = ach(ACH_ID_10, 10);
            LocalDateTime t1 = LocalDateTime.now().minusDays(2);
            LocalDateTime t2 = LocalDateTime.now().minusDays(1);
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc()).thenReturn(List.of(a5, a10));
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(ua(ACH_ID_5, t1), ua(ACH_ID_10, t2)));
            when(achievementMapper.toStatusResponse(a5, true, t1)).thenReturn(statusResp(a5, true, t1));
            when(achievementMapper.toStatusResponse(a10, true, t2)).thenReturn(statusResp(a10, true, t2));
            assertThat(service.getAllAchievements(USER_ID)).allMatch(AchievementStatusResponse::isEarned);
        }

        @Test @DisplayName("none earned — all flagged false")
        void noneEarned_allFalse() {
            Achievement a5 = ach(ACH_ID_5, 5);
            when(achievementRepository.findAllByIsActiveTrueOrderByThresholdAsc()).thenReturn(List.of(a5));
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(achievementMapper.toStatusResponse(a5, false, null)).thenReturn(statusResp(a5, false, null));
            List<AchievementStatusResponse> result = service.getAllAchievements(USER_ID);
            assertThat(result.get(0).isEarned()).isFalse();
            assertThat(result.get(0).getUnlockedAt()).isNull();
        }
    }

    @Nested @DisplayName("getMyAchievements()")
    class GetMyAchievements {

        @Test @DisplayName("returns earned achievements sorted newest first")
        void sorted_newestFirst() {
            LocalDateTime older = LocalDateTime.now().minusDays(5);
            LocalDateTime newer = LocalDateTime.now().minusDays(1);
            Achievement a5 = ach(ACH_ID_5, 5);
            Achievement a10 = ach(ACH_ID_10, 10);
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(ua(ACH_ID_5, older), ua(ACH_ID_10, newer)));
            when(achievementRepository.findAllById(Set.of(ACH_ID_5, ACH_ID_10))).thenReturn(List.of(a5, a10));
            when(achievementMapper.toStatusResponse(a5, true, older)).thenReturn(statusResp(a5, true, older));
            when(achievementMapper.toStatusResponse(a10, true, newer)).thenReturn(statusResp(a10, true, newer));

            List<AchievementStatusResponse> result = service.getMyAchievements(USER_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUnlockedAt()).isEqualTo(newer);
            assertThat(result.get(1).getUnlockedAt()).isEqualTo(older);
        }

        @Test @DisplayName("returns empty list when user has nothing earned")
        void noEarned_returnsEmpty() {
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of());
            assertThat(service.getMyAchievements(USER_ID)).isEmpty();
            verify(achievementRepository, never()).findAllById(any());
        }

        @Test @DisplayName("all results have earned=true")
        void allEarnedTrue() {
            UserAchievement ua = ua(ACH_ID_5, LocalDateTime.now());
            Achievement a5 = ach(ACH_ID_5, 5);
            when(userAchievementRepository.findByUserId(USER_ID)).thenReturn(List.of(ua));
            when(achievementRepository.findAllById(any())).thenReturn(List.of(a5));
            when(achievementMapper.toStatusResponse(eq(a5), eq(true), any()))
                    .thenReturn(statusResp(a5, true, ua.getUnlockedAt()));
            assertThat(service.getMyAchievements(USER_ID)).allMatch(AchievementStatusResponse::isEarned);
        }
    }
}