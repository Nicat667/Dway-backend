package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.challenge.AlreadyJoinedException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeExpiredException;
import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.common.exception.challenge.NotJoinedException;
import com.dway.dwaybackend.dto.response.challenge.ChallengeResponse;
import com.dway.dwaybackend.entity.enums.Difficulty;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.ChallengeService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChallengeController.class)
@DisplayName("ChallengeController")
class ChallengeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ChallengeService challengeService;
    @MockitoBean RateLimitService rateLimitService;

    private static final UUID USER_ID      = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CHALLENGE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final String BASE       = "/api/v1/mobile/challenges";

    @BeforeEach
    void setUp() { when(rateLimitService.tryConsume(any(), any())).thenReturn(true); }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                USER_ID, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private ChallengeResponse stubChallenge(boolean isJoined, int progress) {
        return ChallengeResponse.builder()
                .id(CHALLENGE_ID).icon("🏆").title("7-Day Streak").description("Complete 7 tasks")
                .difficulty(Difficulty.MEDIUM).targetCount(7).rewardPoints(100)
                .participantCount(42).isJoined(isJoined).progress(progress)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested @DisplayName("GET / — getAllChallenges")
    class GetAllChallenges {

        @Test @DisplayName("200 — returns list of challenges")
        void success_returnsList() throws Exception {
            when(challengeService.getAllChallenges(USER_ID)).thenReturn(List.of(stubChallenge(false, 0)));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].title").value("7-Day Streak"))
                    .andExpect(jsonPath("$.data[0].targetCount").value(7))
                    .andExpect(jsonPath("$.data[0].participantCount").value(42));
        }

        @Test @DisplayName("200 — isJoined and progress present in each item")
        void success_includesJoinStatusAndProgress() throws Exception {
            when(challengeService.getAllChallenges(USER_ID)).thenReturn(List.of(stubChallenge(true, 4)));

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].isJoined").value(true))
                    .andExpect(jsonPath("$.data[0].progress").value(4));
        }

        @Test @DisplayName("200 — empty list when no challenges exist")
        void success_emptyList() throws Exception {
            when(challengeService.getAllChallenges(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get(BASE).with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isEmpty());
        }

        @Test @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
            verify(challengeService, never()).getAllChallenges(any());
        }
    }

    @Nested @DisplayName("POST /{id}/join — joinChallenge")
    class JoinChallenge {

        @Test @DisplayName("200 — joins challenge and returns isJoined=true")
        void success_returnsJoinedChallenge() throws Exception {
            when(challengeService.joinChallenge(USER_ID, CHALLENGE_ID)).thenReturn(stubChallenge(true, 0));

            mockMvc.perform(post(BASE + "/" + CHALLENGE_ID + "/join").with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Joined challenge"))
                    .andExpect(jsonPath("$.data.isJoined").value(true));
        }

        @Test @DisplayName("404 — challenge not found")
        void challengeNotFound_returns404() throws Exception {
            when(challengeService.joinChallenge(USER_ID, CHALLENGE_ID)).thenThrow(new ChallengeNotFoundException());

            mockMvc.perform(post(BASE + "/" + CHALLENGE_ID + "/join").with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("409 — already joined")
        void alreadyJoined_returns409() throws Exception {
            when(challengeService.joinChallenge(USER_ID, CHALLENGE_ID)).thenThrow(new AlreadyJoinedException());

            mockMvc.perform(post(BASE + "/" + CHALLENGE_ID + "/join").with(asUser()).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("410 — challenge expired")
        void challengeExpired_returns410() throws Exception {
            when(challengeService.joinChallenge(USER_ID, CHALLENGE_ID)).thenThrow(new ChallengeExpiredException());

            mockMvc.perform(post(BASE + "/" + CHALLENGE_ID + "/join").with(asUser()).with(csrf()))
                    .andExpect(status().isGone())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(post(BASE + "/" + CHALLENGE_ID + "/join").with(csrf()))
                    .andExpect(status().isUnauthorized());
            verify(challengeService, never()).joinChallenge(any(), any());
        }
    }

    @Nested @DisplayName("DELETE /{id}/leave — leaveChallenge")
    class LeaveChallenge {

        @Test @DisplayName("200 — leaves challenge with null data")
        void success_leavesChallenge() throws Exception {
            doNothing().when(challengeService).leaveChallenge(USER_ID, CHALLENGE_ID);

            mockMvc.perform(delete(BASE + "/" + CHALLENGE_ID + "/leave").with(asUser()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("Left challenge"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test @DisplayName("404 — challenge not found")
        void challengeNotFound_returns404() throws Exception {
            doThrow(new ChallengeNotFoundException()).when(challengeService).leaveChallenge(USER_ID, CHALLENGE_ID);

            mockMvc.perform(delete(BASE + "/" + CHALLENGE_ID + "/leave").with(asUser()).with(csrf()))
                    .andExpect(status().isNotFound());
        }

        @Test @DisplayName("400 — not joined")
        void notJoined_returns400() throws Exception {
            doThrow(new NotJoinedException()).when(challengeService).leaveChallenge(USER_ID, CHALLENGE_ID);

            mockMvc.perform(delete(BASE + "/" + CHALLENGE_ID + "/leave").with(asUser()).with(csrf()))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401 — unauthenticated")
        void unauthenticated_returns401() throws Exception {
            mockMvc.perform(delete(BASE + "/" + CHALLENGE_ID + "/leave").with(csrf()))
                    .andExpect(status().isUnauthorized());
            verify(challengeService, never()).leaveChallenge(any(), any());
        }
    }
}