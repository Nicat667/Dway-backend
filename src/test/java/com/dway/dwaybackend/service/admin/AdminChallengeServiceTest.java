package com.dway.dwaybackend.service.admin;

import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.dto.request.challenge.CreateChallengeRequest;
import com.dway.dwaybackend.dto.request.challenge.UpdateChallengeRequest;
import com.dway.dwaybackend.dto.response.challenge.AdminChallengeResponse;
import com.dway.dwaybackend.entity.Challenge;
import com.dway.dwaybackend.entity.enums.Difficulty;
import com.dway.dwaybackend.mapper.ChallengeMapper;
import com.dway.dwaybackend.repository.ChallengeRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminChallengeService Unit Tests")
class AdminChallengeServiceTest {

    @Mock private ChallengeRepository challengeRepository;
    @Mock private ChallengeMapper challengeMapper;

    @InjectMocks private AdminChallengeService adminChallengeService;

    private static final UUID CHALLENGE_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private Challenge activeChallenge() {
        return Challenge.builder()
                .id(CHALLENGE_ID).icon("🏆").title("7-Day Streak")
                .description("Complete 7 tasks")
                .difficulty(Difficulty.MEDIUM).targetCount(7).rewardPoints(100)
                .isActive(true).participantCount(5)
                .build();
    }

    private Challenge inactiveChallenge() {
        Challenge c = activeChallenge();
        c.setActive(false);
        return c;
    }

    private AdminChallengeResponse adminResponse(Challenge c) {
        return AdminChallengeResponse.builder()
                .id(c.getId()).icon(c.getIcon()).title(c.getTitle())
                .description(c.getDescription()).difficulty(c.getDifficulty())
                .targetCount(c.getTargetCount()).rewardPoints(c.getRewardPoints())
                .isActive(c.isActive()).participantCount(c.getParticipantCount())
                .expiresAt(c.getExpiresAt())
                .build();
    }

    private CreateChallengeRequest createRequest() {
        CreateChallengeRequest r = new CreateChallengeRequest();
        r.setIcon("🏅"); r.setTitle("New Challenge"); r.setDescription("Description");
        r.setDifficulty(Difficulty.HARD); r.setTargetCount(14); r.setRewardPoints(200);
        return r;
    }

    private UpdateChallengeRequest updateRequest(String title, Difficulty difficulty) {
        UpdateChallengeRequest r = new UpdateChallengeRequest();
        r.setTitle(title); r.setDifficulty(difficulty);
        return r;
    }

    // ── getAllChallenges ───────────────────────────────────────────────────────

    @Nested @DisplayName("getAllChallenges()")
    class GetAllChallenges {

        @Test @DisplayName("returns all challenges when isActive filter is null")
        void whenNoFilter_returnsAll() {
            Challenge c = activeChallenge();
            Page<Challenge> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
            when(challengeRepository.findAll(any(Pageable.class))).thenReturn(page);
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            Page<AdminChallengeResponse> result = adminChallengeService.getAllChallenges(null, PageRequest.of(0, 20));

            assertThat(result.getTotalElements()).isEqualTo(1);
            verify(challengeRepository).findAll(any(Pageable.class));
            verify(challengeRepository, never()).findByIsActive(anyBoolean(), any());
        }

        @Test @DisplayName("filters by isActive=true")
        void whenFilterTrue_callsFindByIsActiveTrue() {
            Challenge c = activeChallenge();
            Page<Challenge> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
            when(challengeRepository.findByIsActive(true, PageRequest.of(0, 20))).thenReturn(page);
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            Page<AdminChallengeResponse> result = adminChallengeService.getAllChallenges(true, PageRequest.of(0, 20));

            assertThat(result.getContent().get(0).getIsActive()).isTrue();
            verify(challengeRepository).findByIsActive(true, PageRequest.of(0, 20));
            verify(challengeRepository, never()).findAll(any(Pageable.class));
        }

        @Test @DisplayName("filters by isActive=false")
        void whenFilterFalse_callsFindByIsActiveFalse() {
            Challenge c = inactiveChallenge();
            Page<Challenge> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
            when(challengeRepository.findByIsActive(false, PageRequest.of(0, 20))).thenReturn(page);
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            Page<AdminChallengeResponse> result = adminChallengeService.getAllChallenges(false, PageRequest.of(0, 20));

            assertThat(result.getContent().get(0).getIsActive()).isFalse();
        }

        @Test @DisplayName("returns empty page when no challenges exist")
        void whenNoChallenges_returnsEmptyPage() {
            when(challengeRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            Page<AdminChallengeResponse> result = adminChallengeService.getAllChallenges(null, PageRequest.of(0, 20));

            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ── getChallengeById ──────────────────────────────────────────────────────

    @Nested @DisplayName("getChallengeById()")
    class GetChallengeById {

        @Test @DisplayName("returns admin response for existing challenge")
        void withValidId_returnsAdminResponse() {
            Challenge c = activeChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            AdminChallengeResponse result = adminChallengeService.getChallengeById(CHALLENGE_ID);

            assertThat(result.getId()).isEqualTo(CHALLENGE_ID);
            assertThat(result.getTitle()).isEqualTo("7-Day Streak");
        }

        @Test @DisplayName("throws ChallengeNotFoundException when not found")
        void whenNotFound_throwsChallengeNotFoundException() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(ChallengeNotFoundException.class, () -> adminChallengeService.getChallengeById(CHALLENGE_ID));
            verify(challengeMapper, never()).toAdminResponse(any());
        }
    }

    // ── createChallenge ───────────────────────────────────────────────────────

    @Nested @DisplayName("createChallenge()")
    class CreateChallenge {

        @Test @DisplayName("saves the challenge entity")
        void withValidRequest_savesChallenge() {
            CreateChallengeRequest req = createRequest();
            Challenge entity = activeChallenge();
            when(challengeMapper.toEntity(req)).thenReturn(entity);
            when(challengeMapper.toAdminResponse(entity)).thenReturn(adminResponse(entity));

            adminChallengeService.createChallenge(req);

            verify(challengeRepository).save(entity);
        }

        @Test @DisplayName("applies isActive override when provided")
        void whenRequestHasIsActive_appliesIt() {
            CreateChallengeRequest req = createRequest();
            req.setIsActive(false);
            Challenge entity = activeChallenge();
            when(challengeMapper.toEntity(req)).thenReturn(entity);
            when(challengeMapper.toAdminResponse(entity)).thenReturn(adminResponse(entity));

            adminChallengeService.createChallenge(req);

            assertThat(entity.isActive()).isFalse();
        }

        @Test @DisplayName("applies expiresAt when provided")
        void whenRequestHasExpiresAt_appliesIt() {
            CreateChallengeRequest req = createRequest();
            LocalDateTime expiry = LocalDateTime.now().plusDays(30);
            req.setExpiresAt(expiry);
            Challenge entity = activeChallenge();
            when(challengeMapper.toEntity(req)).thenReturn(entity);
            when(challengeMapper.toAdminResponse(entity)).thenReturn(adminResponse(entity));

            adminChallengeService.createChallenge(req);

            assertThat(entity.getExpiresAt()).isEqualTo(expiry);
        }

        @Test @DisplayName("leaves expiresAt null when not provided")
        void whenRequestOmitsExpiresAt_entityExpiresAtIsNull() {
            CreateChallengeRequest req = createRequest(); // expiresAt = null
            Challenge entity = activeChallenge();
            when(challengeMapper.toEntity(req)).thenReturn(entity);
            when(challengeMapper.toAdminResponse(entity)).thenReturn(adminResponse(entity));

            adminChallengeService.createChallenge(req);

            assertThat(entity.getExpiresAt()).isNull();
        }

        @Test @DisplayName("does not touch isActive when request omits it")
        void whenRequestOmitsIsActive_keepsMapperDefault() {
            CreateChallengeRequest req = createRequest();
            Challenge entity = activeChallenge();
            when(challengeMapper.toEntity(req)).thenReturn(entity);
            when(challengeMapper.toAdminResponse(entity)).thenReturn(adminResponse(entity));

            adminChallengeService.createChallenge(req);

            assertThat(entity.isActive()).isTrue();
        }
    }

    // ── updateChallenge ───────────────────────────────────────────────────────

    @Nested @DisplayName("updateChallenge()")
    class UpdateChallenge {

        @Test @DisplayName("applies non-null title from request")
        void withNewTitle_updatesTitle() {
            Challenge c = activeChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.updateChallenge(CHALLENGE_ID, updateRequest("Updated Title", null));

            assertThat(c.getTitle()).isEqualTo("Updated Title");
            verify(challengeRepository).save(c);
        }

        @Test @DisplayName("applies expiresAt when provided")
        void withExpiresAt_updatesExpiresAt() {
            Challenge c = activeChallenge();
            LocalDateTime expiry = LocalDateTime.now().plusDays(14);
            UpdateChallengeRequest req = new UpdateChallengeRequest();
            req.setExpiresAt(expiry);
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.updateChallenge(CHALLENGE_ID, req);

            assertThat(c.getExpiresAt()).isEqualTo(expiry);
        }

        @Test @DisplayName("does not overwrite fields when null — PATCH semantics")
        void withNullFields_leavesExistingValuesUntouched() {
            Challenge c = activeChallenge();
            String originalTitle = c.getTitle();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.updateChallenge(CHALLENGE_ID, updateRequest(null, null));

            assertThat(c.getTitle()).isEqualTo(originalTitle);
            assertThat(c.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        }

        @Test @DisplayName("throws ChallengeNotFoundException when not found")
        void whenNotFound_throwsChallengeNotFoundException() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(ChallengeNotFoundException.class,
                    () -> adminChallengeService.updateChallenge(CHALLENGE_ID, updateRequest("x", null)));
            verify(challengeRepository, never()).save(any());
        }
    }

    // ── toggleActive ──────────────────────────────────────────────────────────

    @Nested @DisplayName("toggleActive()")
    class ToggleActive {

        @Test @DisplayName("flips isActive from true to false")
        void whenCurrentlyActive_becomesInactive() {
            Challenge c = activeChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.toggleActive(CHALLENGE_ID);

            assertThat(c.isActive()).isFalse();
        }

        @Test @DisplayName("flips isActive from false to true")
        void whenCurrentlyInactive_becomesActive() {
            Challenge c = inactiveChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.toggleActive(CHALLENGE_ID);

            assertThat(c.isActive()).isTrue();
        }

        @Test @DisplayName("calling twice restores original state")
        void calledTwice_restoresOriginalState() {
            Challenge c = activeChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));
            when(challengeMapper.toAdminResponse(c)).thenReturn(adminResponse(c));

            adminChallengeService.toggleActive(CHALLENGE_ID);
            adminChallengeService.toggleActive(CHALLENGE_ID);

            assertThat(c.isActive()).isTrue();
        }

        @Test @DisplayName("throws ChallengeNotFoundException when not found")
        void whenNotFound_throwsChallengeNotFoundException() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(ChallengeNotFoundException.class, () -> adminChallengeService.toggleActive(CHALLENGE_ID));
            verify(challengeRepository, never()).save(any());
        }
    }

    // ── deleteChallenge ───────────────────────────────────────────────────────

    @Nested @DisplayName("deleteChallenge()")
    class DeleteChallenge {

        @Test @DisplayName("deletes the challenge entity")
        void withValidId_deletesChallenge() {
            Challenge c = activeChallenge();
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(c));

            adminChallengeService.deleteChallenge(CHALLENGE_ID);

            verify(challengeRepository).delete(c);
        }

        @Test @DisplayName("throws ChallengeNotFoundException when not found")
        void whenNotFound_throwsChallengeNotFoundException() {
            when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

            assertThrows(ChallengeNotFoundException.class, () -> adminChallengeService.deleteChallenge(CHALLENGE_ID));
            verify(challengeRepository, never()).delete(any());
        }
    }
}