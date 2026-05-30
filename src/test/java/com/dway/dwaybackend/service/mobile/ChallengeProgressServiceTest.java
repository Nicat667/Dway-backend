package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.entity.UserChallenge;
import com.dway.dwaybackend.entity.enums.Difficulty;
import com.dway.dwaybackend.repository.ChallengeRepository;
import com.dway.dwaybackend.repository.TaskRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import com.dway.dwaybackend.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeProgressService Unit Tests")
class ChallengeProgressServiceTest {

    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private ChallengeRepository     challengeRepository;
    @Mock private TaskRepository          taskRepository;
    @Mock private UserRepository          userRepository;

    @InjectMocks private ChallengeProgressService service;

    private static final UUID USER_ID      = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHALLENGE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Challenge challenge(UUID id, boolean active, int targetCount, int rewardPoints) {
        return Challenge.builder()
                .id(id).icon("🏆").title("Test Challenge").description("desc")
                .difficulty(Difficulty.MEDIUM).targetCount(targetCount).rewardPoints(rewardPoints)
                .isActive(active).participantCount(10)
                .build();
    }

    private UserChallenge userChallenge(UUID userId, UUID challengeId, int progress) {
        return UserChallenge.builder()
                .id(UUID.randomUUID()).userId(userId).challengeId(challengeId)
                .progress(progress).joinedAt(LocalDateTime.now().minusDays(3))
                .build();
    }

    // ── Early exit ────────────────────────────────────────────────────────────

    @Nested @DisplayName("Early exit")
    class EarlyExit {

        @Test @DisplayName("does nothing when user has no incomplete challenges")
        void whenNoIncomplete_noInteractionsWithOtherRepos() {
            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID))
                    .thenReturn(List.of());

            service.recalculateProgress(USER_ID);

            verifyNoInteractions(challengeRepository, taskRepository, userRepository);
        }
    }

    // ── Skip conditions ───────────────────────────────────────────────────────

    @Nested @DisplayName("Skip conditions")
    class SkipConditions {

        @Test @DisplayName("skips when challenge is not found in DB")
        void whenChallengeNotInMap_skipsWithoutSaving() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 0);
            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(Set.of(CHALLENGE_ID))).thenReturn(List.of());

            service.recalculateProgress(USER_ID);

            verify(taskRepository, never()).countCompletedByUserIdSince(any(), any());
            verify(userChallengeRepository, never()).save(any());
        }

        @Test @DisplayName("skips when challenge is inactive")
        void whenChallengeInactive_skipsWithoutSaving() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 0);
            Challenge inactive = challenge(CHALLENGE_ID, false, 10, 100);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(Set.of(CHALLENGE_ID))).thenReturn(List.of(inactive));

            service.recalculateProgress(USER_ID);

            verify(taskRepository, never()).countCompletedByUserIdSince(any(), any());
            verify(userChallengeRepository, never()).save(any());
        }

        @Test @DisplayName("skips save when computed progress is identical to stored progress")
        void whenProgressUnchanged_doesNotSave() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 3); // already 3
            Challenge c = challenge(CHALLENGE_ID, true, 10, 100);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(3L);

            service.recalculateProgress(USER_ID);

            verify(userChallengeRepository, never()).save(any());
            verify(userRepository, never()).incrementPoints(any(), anyInt());
        }
    }

    // ── Progress update (target not yet reached) ──────────────────────────────

    @Nested @DisplayName("Progress update — target not reached")
    class ProgressUpdate {

        @Test @DisplayName("saves UserChallenge with new progress value")
        void whenProgressIncreased_savesNewProgress() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 2);
            Challenge c = challenge(CHALLENGE_ID, true, 10, 100);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(5L);

            service.recalculateProgress(USER_ID);

            verify(userChallengeRepository).save(argThat(saved -> saved.getProgress() == 5));
        }

        @Test @DisplayName("completedAt remains null when target is not reached")
        void whenTargetNotReached_completedAtStaysNull() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 1);
            Challenge c = challenge(CHALLENGE_ID, true, 10, 100);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(4L);

            service.recalculateProgress(USER_ID);

            assertThat(uc.getCompletedAt()).isNull();
            verify(userRepository, never()).incrementPoints(any(), anyInt());
        }

        @Test @DisplayName("progress is capped at targetCount even when task count far exceeds it")
        void whenTaskCountExceedsTarget_progressCappedAtTargetCount() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 0);
            Challenge c = challenge(CHALLENGE_ID, true, 5, 50);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(99L);

            service.recalculateProgress(USER_ID);

            verify(userChallengeRepository).save(argThat(saved -> saved.getProgress() == 5));
        }
    }

    // ── Challenge completion ──────────────────────────────────────────────────

    @Nested @DisplayName("Challenge completion")
    class ChallengeCompletion {

        @Test @DisplayName("sets completedAt when progress exactly reaches targetCount")
        void whenTargetExactlyReached_setsCompletedAt() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 4);
            Challenge c = challenge(CHALLENGE_ID, true, 5, 100);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(5L);

            service.recalculateProgress(USER_ID);

            assertThat(uc.getCompletedAt()).isNotNull();
        }

        @Test @DisplayName("awards the correct reward points when challenge completes")
        void whenTargetReached_incrementsPointsByRewardAmount() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 4);
            Challenge c = challenge(CHALLENGE_ID, true, 5, 250);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(5L);

            service.recalculateProgress(USER_ID);

            verify(userRepository).incrementPoints(USER_ID, 250);
        }

        @Test @DisplayName("saves the completed UserChallenge with progress = targetCount and completedAt set")
        void whenTargetReached_savedRowHasCorrectState() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 0);
            Challenge c = challenge(CHALLENGE_ID, true, 3, 50);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID)).thenReturn(List.of(uc));
            when(challengeRepository.findAllById(any())).thenReturn(List.of(c));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, uc.getJoinedAt())).thenReturn(3L);

            service.recalculateProgress(USER_ID);

            verify(userChallengeRepository).save(argThat(saved ->
                    saved.getProgress() == 3 && saved.getCompletedAt() != null));
        }
    }

    // ── Batch behaviour ───────────────────────────────────────────────────────

    @Nested @DisplayName("Batch behaviour")
    class BatchBehaviour {

        @Test @DisplayName("resolves all challenge IDs in a single findAllById call — no N+1")
        void fetchesChallengesInOneBatch() {
            UUID idA = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID idB = UUID.fromString("22222222-2222-2222-2222-222222222222");
            UserChallenge ucA = userChallenge(USER_ID, idA, 0);
            UserChallenge ucB = userChallenge(USER_ID, idB, 0);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID))
                    .thenReturn(List.of(ucA, ucB));
            when(challengeRepository.findAllById(Set.of(idA, idB)))
                    .thenReturn(List.of(challenge(idA, true, 5, 50), challenge(idB, true, 5, 50)));
            when(taskRepository.countCompletedByUserIdSince(eq(USER_ID), any())).thenReturn(0L);

            service.recalculateProgress(USER_ID);

            verify(challengeRepository, times(1)).findAllById(any());
        }

        @Test @DisplayName("processes each challenge independently — partial completion does not block others")
        void multipleIncomplete_completionIsIndependent() {
            UUID idA = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID idB = UUID.fromString("22222222-2222-2222-2222-222222222222");
            LocalDateTime joinedA = LocalDateTime.now().minusDays(5);
            LocalDateTime joinedB = LocalDateTime.now().minusDays(2);

            UserChallenge ucA = UserChallenge.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).challengeId(idA)
                    .progress(2).joinedAt(joinedA).build();
            UserChallenge ucB = UserChallenge.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).challengeId(idB)
                    .progress(4).joinedAt(joinedB).build();

            Challenge cA = challenge(idA, true,  5, 100); // will complete  (5/5)
            Challenge cB = challenge(idB, true, 10, 200); // will not complete (6/10)

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID))
                    .thenReturn(List.of(ucA, ucB));
            when(challengeRepository.findAllById(any()))
                    .thenReturn(List.of(cA, cB));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, joinedA)).thenReturn(5L);
            when(taskRepository.countCompletedByUserIdSince(USER_ID, joinedB)).thenReturn(6L);

            service.recalculateProgress(USER_ID);

            assertThat(ucA.getCompletedAt()).isNotNull();
            assertThat(ucB.getCompletedAt()).isNull();
            verify(userRepository, times(1)).incrementPoints(USER_ID, 100); // cA only
            verify(userChallengeRepository, times(2)).save(any());           // both changed
        }

        @Test @DisplayName("skips inactive challenge while still processing active ones in the same batch")
        void mixedActiveInactive_skipsInactiveOnly() {
            UUID activeId   = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID inactiveId = UUID.fromString("22222222-2222-2222-2222-222222222222");

            UserChallenge ucActive   = userChallenge(USER_ID, activeId,   0);
            UserChallenge ucInactive = userChallenge(USER_ID, inactiveId, 0);

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID))
                    .thenReturn(List.of(ucActive, ucInactive));
            when(challengeRepository.findAllById(any()))
                    .thenReturn(List.of(challenge(activeId, true, 5, 100), challenge(inactiveId, false, 5, 100)));
            when(taskRepository.countCompletedByUserIdSince(USER_ID, ucActive.getJoinedAt())).thenReturn(3L);

            service.recalculateProgress(USER_ID);

            verify(userChallengeRepository, times(1)).save(any()); // active one only
        }

        @Test @DisplayName("uses joinedAt from each UserChallenge as the task-count window boundary")
        void usesCorrectJoinedAtPerChallenge() {
            UUID idA = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID idB = UUID.fromString("22222222-2222-2222-2222-222222222222");
            LocalDateTime joinedA = LocalDateTime.now().minusDays(10);
            LocalDateTime joinedB = LocalDateTime.now().minusDays(1);

            UserChallenge ucA = UserChallenge.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).challengeId(idA)
                    .progress(0).joinedAt(joinedA).build();
            UserChallenge ucB = UserChallenge.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).challengeId(idB)
                    .progress(0).joinedAt(joinedB).build();

            when(userChallengeRepository.findByUserIdAndCompletedAtIsNull(USER_ID))
                    .thenReturn(List.of(ucA, ucB));
            when(challengeRepository.findAllById(any()))
                    .thenReturn(List.of(challenge(idA, true, 5, 50), challenge(idB, true, 5, 50)));
            when(taskRepository.countCompletedByUserIdSince(eq(USER_ID), any())).thenReturn(0L);

            service.recalculateProgress(USER_ID);

            verify(taskRepository).countCompletedByUserIdSince(USER_ID, joinedA);
            verify(taskRepository).countCompletedByUserIdSince(USER_ID, joinedB);
        }
    }
}