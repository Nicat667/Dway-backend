package com.dway.dwaybackend.service.mobile;

import com.dway.dwaybackend.common.exception.challenge.AlreadyJoinedException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeExpiredException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.common.exception.challenge.NotJoinedException;
import com.dway.dwaybackend.dto.response.challenge.ChallengeResponse;
import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.entity.UserChallenge;
import com.dway.dwaybackend.entity.enums.Difficulty;
import com.dway.dwaybackend.mapper.ChallengeMapper;
import com.dway.dwaybackend.repository.ChallengeRepository;
import com.dway.dwaybackend.repository.UserChallengeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChallengeService Unit Tests")
class ChallengeServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private UserChallengeRepository userChallengeRepository;
    @Mock private ChallengeMapper challengeMapper;

    @InjectMocks private ChallengeService challengeService;

    private static final UUID USER_ID      = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHALLENGE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private Challenge challenge(UUID id, boolean active) {
        return Challenge.builder()
                .id(id).icon("🏆").title("7-Day Streak").description("Complete 7 tasks")
                .difficulty(Difficulty.MEDIUM).targetCount(7).rewardPoints(100)
                .isActive(active).participantCount(10)
                .build();
    }

    private Challenge challengeWithExpiry(UUID id, LocalDateTime expiresAt) {
        return Challenge.builder()
                .id(id).icon("🏆").title("Expiring Challenge").description("desc")
                .difficulty(Difficulty.EASY).targetCount(5).rewardPoints(50)
                .isActive(true).participantCount(0).expiresAt(expiresAt)
                .build();
    }

    private ChallengeResponse baseResponse(Challenge c) {
        return ChallengeResponse.builder()
                .id(c.getId()).icon(c.getIcon()).title(c.getTitle())
                .description(c.getDescription()).difficulty(c.getDifficulty())
                .targetCount(c.getTargetCount()).rewardPoints(c.getRewardPoints())
                .participantCount(c.getParticipantCount())
                .isJoined(false).progress(0)
                .createdAt(c.getCreatedAt())
                .build();
    }

    private UserChallenge userChallenge(UUID userId, UUID challengeId, int progress) {
        return UserChallenge.builder()
                .id(UUID.randomUUID()).userId(userId).challengeId(challengeId)
                .progress(progress).joinedAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    // ── getAllChallenges ───────────────────────────────────────────────────────

    @Nested @DisplayName("getAllChallenges()")
    class GetAllChallenges {

        @Test @DisplayName("returns empty list when no active challenges exist")
        void whenNoChallenges_returnsEmptyList() {
            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of());
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of());

            assertThat(challengeService.getAllChallenges(USER_ID)).isEmpty();
        }

        @Test @DisplayName("marks challenge as not joined when user has not joined")
        void whenUserHasNotJoined_isJoinedFalse() {
            Challenge c = challenge(CHALLENGE_ID, true);
            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(c));
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of());
            when(challengeMapper.toResponse(c)).thenReturn(baseResponse(c));

            List<ChallengeResponse> result = challengeService.getAllChallenges(USER_ID);

            assertThat(result.get(0).getIsJoined()).isFalse();
            assertThat(result.get(0).getProgress()).isZero();
            assertThat(result.get(0).getJoinedAt()).isNull();
        }

        @Test @DisplayName("marks challenge as joined with correct progress")
        void whenUserHasJoined_isJoinedTrueWithProgress() {
            Challenge c = challenge(CHALLENGE_ID, true);
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 4);
            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(c));
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of(uc));
            when(challengeMapper.toResponse(c)).thenReturn(baseResponse(c));

            List<ChallengeResponse> result = challengeService.getAllChallenges(USER_ID);

            assertThat(result.get(0).getIsJoined()).isTrue();
            assertThat(result.get(0).getProgress()).isEqualTo(4);
            assertThat(result.get(0).getJoinedAt()).isNotNull();
        }

        @Test @DisplayName("sets completedAt when challenge is completed")
        void whenChallengeCompleted_completedAtIsSet() {
            Challenge c = challenge(CHALLENGE_ID, true);
            UserChallenge uc = UserChallenge.builder()
                    .id(UUID.randomUUID()).userId(USER_ID).challengeId(CHALLENGE_ID)
                    .progress(7).joinedAt(LocalDateTime.now().minusDays(7))
                    .completedAt(LocalDateTime.now().minusDays(1)).build();
            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(c));
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of(uc));
            when(challengeMapper.toResponse(c)).thenReturn(baseResponse(c));

            assertThat(challengeService.getAllChallenges(USER_ID).get(0).getCompletedAt()).isNotNull();
        }

        @Test @DisplayName("maps progress to the correct challenge when multiple exist")
        void withMultipleChallenges_mapsProgressCorrectly() {
            UUID idA = UUID.fromString("11111111-1111-1111-1111-111111111111");
            UUID idB = UUID.fromString("22222222-2222-2222-2222-222222222222");
            Challenge cA = challenge(idA, true);
            Challenge cB = challenge(idB, true);
            UserChallenge ucA = userChallenge(USER_ID, idA, 3);

            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(cA, cB));
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of(ucA));
            when(challengeMapper.toResponse(cA)).thenReturn(baseResponse(cA));
            when(challengeMapper.toResponse(cB)).thenReturn(baseResponse(cB));

            List<ChallengeResponse> result = challengeService.getAllChallenges(USER_ID);

            ChallengeResponse rA = result.stream().filter(r -> r.getId().equals(idA)).findFirst().orElseThrow();
            ChallengeResponse rB = result.stream().filter(r -> r.getId().equals(idB)).findFirst().orElseThrow();

            assertThat(rA.getIsJoined()).isTrue();
            assertThat(rA.getProgress()).isEqualTo(3);
            assertThat(rB.getIsJoined()).isFalse();
            assertThat(rB.getProgress()).isZero();
        }

        @Test @DisplayName("does exactly 1 userChallenge query regardless of challenge count — no N+1")
        void noNPlusOneQueries() {
            List<Challenge> tenChallenges = new ArrayList<>();
            for (int i = 0; i < 10; i++) tenChallenges.add(challenge(UUID.randomUUID(), true));

            when(challengeRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(tenChallenges);
            when(userChallengeRepository.findByUserId(USER_ID)).thenReturn(List.of());
            tenChallenges.forEach(c -> when(challengeMapper.toResponse(c)).thenReturn(baseResponse(c)));

            challengeService.getAllChallenges(USER_ID);

            verify(userChallengeRepository, times(1)).findByUserId(USER_ID);
            verify(userChallengeRepository, never()).existsByUserIdAndChallengeId(any(), any());
        }
    }

    // ── joinChallenge ─────────────────────────────────────────────────────────

    @Nested @DisplayName("joinChallenge()")
    class JoinChallenge {

        @Test @DisplayName("saves a new UserChallenge row")
        void withValidRequest_savesUserChallenge() {
            Challenge c = challenge(CHALLENGE_ID, true);
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            challengeService.joinChallenge(USER_ID, CHALLENGE_ID);

            verify(userChallengeRepository).save(argThat(uc ->
                    uc.getUserId().equals(USER_ID) && uc.getChallengeId().equals(CHALLENGE_ID)));
        }

        @Test @DisplayName("increments participant count in the DB")
        void withValidRequest_incrementsParticipantCount() {
            Challenge c = challenge(CHALLENGE_ID, true);
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            challengeService.joinChallenge(USER_ID, CHALLENGE_ID);

            verify(challengeRepository).incrementParticipantCount(CHALLENGE_ID);
        }

        @Test @DisplayName("response has isJoined=true and progress=0")
        void withValidRequest_responseIsCorrect() {
            Challenge c = challenge(CHALLENGE_ID, true);
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            ChallengeResponse result = challengeService.joinChallenge(USER_ID, CHALLENGE_ID);

            assertThat(result.getIsJoined()).isTrue();
            assertThat(result.getProgress()).isZero();
            assertThat(result.getCompletedAt()).isNull();
            assertThat(result.getJoinedAt()).isNotNull();
        }

        @Test @DisplayName("response shows incremented participantCount")
        void withValidRequest_responseParticipantCountReflectsIncrement() {
            Challenge c = challenge(CHALLENGE_ID, true); // participantCount = 10
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            ChallengeResponse result = challengeService.joinChallenge(USER_ID, CHALLENGE_ID);

            assertThat(result.getParticipantCount()).isEqualTo(11);
        }

        @Test @DisplayName("succeeds when expiresAt is in the future")
        void whenExpiresAtIsInFuture_joinsSuccessfully() {
            Challenge c = challengeWithExpiry(CHALLENGE_ID, LocalDateTime.now().plusDays(3));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            assertDoesNotThrow(() -> challengeService.joinChallenge(USER_ID, CHALLENGE_ID));
            verify(userChallengeRepository).save(any());
        }

        @Test @DisplayName("throws ChallengeExpiredException when expiresAt is in the past")
        void whenChallengeExpired_throwsChallengeExpiredException() {
            Challenge c = challengeWithExpiry(CHALLENGE_ID, LocalDateTime.now().minusDays(1));
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));

            assertThrows(ChallengeExpiredException.class,
                    () -> challengeService.joinChallenge(USER_ID, CHALLENGE_ID));

            verify(userChallengeRepository, never()).save(any());
            verify(challengeRepository, never()).incrementParticipantCount(any());
        }

        @Test @DisplayName("succeeds when expiresAt is null — no deadline")
        void whenExpiresAtIsNull_noDeadlineEnforced() {
            Challenge c = challenge(CHALLENGE_ID, true); // expiresAt = null
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(false);

            assertDoesNotThrow(() -> challengeService.joinChallenge(USER_ID, CHALLENGE_ID));
        }

        @Test @DisplayName("throws ChallengeNotFoundException when challenge does not exist")
        void whenChallengeNotFound_throwsNotFoundException() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(ChallengeNotFoundException.class,
                    () -> challengeService.joinChallenge(USER_ID, CHALLENGE_ID));
            verify(userChallengeRepository, never()).save(any());
        }

        @Test @DisplayName("throws AlreadyJoinedException when user has already joined")
        void whenAlreadyJoined_throwsAlreadyJoinedException() {
            Challenge c = challenge(CHALLENGE_ID, true);
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(userChallengeRepository.existsByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(true);

            assertThrows(AlreadyJoinedException.class,
                    () -> challengeService.joinChallenge(USER_ID, CHALLENGE_ID));
            verify(userChallengeRepository, never()).save(any());
        }
    }

    // ── leaveChallenge ────────────────────────────────────────────────────────

    @Nested @DisplayName("leaveChallenge()")
    class LeaveChallenge {

        @Test @DisplayName("deletes the UserChallenge row")
        void withValidRequest_deletesUserChallenge() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 2);
            when(challengeRepository.existsById(CHALLENGE_ID)).thenReturn(true);
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(Optional.of(uc));

            challengeService.leaveChallenge(USER_ID, CHALLENGE_ID);

            verify(userChallengeRepository).delete(uc);
        }

        @Test @DisplayName("decrements participant count")
        void withValidRequest_decrementsParticipantCount() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 2);
            when(challengeRepository.existsById(CHALLENGE_ID)).thenReturn(true);
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(Optional.of(uc));

            challengeService.leaveChallenge(USER_ID, CHALLENGE_ID);

            verify(challengeRepository).decrementParticipantCount(CHALLENGE_ID);
        }

        @Test @DisplayName("deletes before decrementing — consistent ordering")
        void withValidRequest_deleteBeforeDecrement() {
            UserChallenge uc = userChallenge(USER_ID, CHALLENGE_ID, 2);
            when(challengeRepository.existsById(CHALLENGE_ID)).thenReturn(true);
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(Optional.of(uc));

            challengeService.leaveChallenge(USER_ID, CHALLENGE_ID);

            InOrder order = inOrder(userChallengeRepository, challengeRepository);
            order.verify(userChallengeRepository).delete(uc);
            order.verify(challengeRepository).decrementParticipantCount(CHALLENGE_ID);
        }

        @Test @DisplayName("throws ChallengeNotFoundException when challenge does not exist")
        void whenChallengeNotFound_throwsNotFoundException() {
            when(challengeRepository.existsById(CHALLENGE_ID)).thenReturn(false);

            assertThrows(ChallengeNotFoundException.class,
                    () -> challengeService.leaveChallenge(USER_ID, CHALLENGE_ID));
            verify(userChallengeRepository, never()).delete(any());
        }

        @Test @DisplayName("throws NotJoinedException when user never joined")
        void whenNotJoined_throwsNotJoinedException() {
            when(challengeRepository.existsById(CHALLENGE_ID)).thenReturn(true);
            when(userChallengeRepository.findByUserIdAndChallengeId(USER_ID, CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(NotJoinedException.class,
                    () -> challengeService.leaveChallenge(USER_ID, CHALLENGE_ID));
            verify(userChallengeRepository, never()).delete(any());
        }
    }
}