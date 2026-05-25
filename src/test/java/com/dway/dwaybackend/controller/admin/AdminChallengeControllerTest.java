package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.challenge.ChallengeNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.challenge.AdminChallengeResponse;
import com.dway.dwaybackend.entity.enums.Difficulty;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminChallengeService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.http.MediaType;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminChallengeController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminChallengeController")
class AdminChallengeControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminChallengeService adminChallengeService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID   ID   = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final String BASE = "/api/v1/admin/challenges";

    @BeforeEach
    void setUp() { when(rateLimitService.tryConsume(any(), any())).thenReturn(true); }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private AdminChallengeResponse stub(boolean isActive) {
        return AdminChallengeResponse.builder()
                .id(ID).icon("🏆").title("7-Day Streak").description("Complete 7 tasks")
                .difficulty(Difficulty.MEDIUM).targetCount(7).rewardPoints(100)
                .isActive(isActive).participantCount(5)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    private Page<AdminChallengeResponse> page(List<AdminChallengeResponse> items) {
        return new PageImpl<>(items, PageRequest.of(0, 20), items.size());
    }

    // ── GET / ─────────────────────────────────────────────────────────────────

    @Nested @DisplayName("GET / — getAllChallenges")
    class GetAllChallenges {

        @Test @DisplayName("200 — returns paginated list")
        void success_returnsList() throws Exception {
            when(adminChallengeService.getAllChallenges(any(), any())).thenReturn(page(List.of(stub(true))));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content.length()").value(1))
                    .andExpect(jsonPath("$.data.content[0].targetCount").value(7))
                    .andExpect(jsonPath("$.data.content[0].isActive").value(true));
        }

        @Test @DisplayName("200 — with isActive=true filter")
        void success_withIsActiveTrueFilter() throws Exception {
            when(adminChallengeService.getAllChallenges(eq(true), any())).thenReturn(page(List.of(stub(true))));

            mockMvc.perform(get(BASE).param("isActive", "true").with(asAdmin()))
                    .andExpect(status().isOk());

            verify(adminChallengeService).getAllChallenges(eq(true), any());
        }

        @Test @DisplayName("200 — with isActive=false filter")
        void success_withIsActiveFalseFilter() throws Exception {
            when(adminChallengeService.getAllChallenges(eq(false), any())).thenReturn(page(List.of(stub(false))));

            mockMvc.perform(get(BASE).param("isActive", "false").with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].isActive").value(false));
        }

        @Test @DisplayName("200 — empty page")
        void success_emptyPage() throws Exception {
            when(adminChallengeService.getAllChallenges(any(), any())).thenReturn(page(Collections.emptyList()));

            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot access") void regularUser() throws Exception {
            mockMvc.perform(get(BASE).with(asUser())).andExpect(status().isForbidden());
        }
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("GET /{id} — getChallengeById")
    class GetChallengeById {

        @Test @DisplayName("200 — returns challenge with all fields")
        void success_returnsChallenge() throws Exception {
            when(adminChallengeService.getChallengeById(ID)).thenReturn(stub(true));

            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.targetCount").value(7))
                    .andExpect(jsonPath("$.data.isActive").value(true));
        }

        @Test @DisplayName("404 — not found") void notFound() throws Exception {
            when(adminChallengeService.getChallengeById(ID)).thenThrow(new ChallengeNotFoundException());
            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot access") void regularUser() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID).with(asUser())).andExpect(status().isForbidden());
        }
    }

    // ── POST / ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("POST / — createChallenge")
    class CreateChallenge {

        private static final String VALID_JSON = """
                {
                  "icon": "🏆",
                  "title": "7-Day Streak",
                  "description": "Complete 7 tasks",
                  "difficulty": "MEDIUM",
                  "targetCount": 7,
                  "rewardPoints": 100
                }
                """;

        @Test @DisplayName("200 — creates challenge")
        void success_returnsCreatedChallenge() throws Exception {
            when(adminChallengeService.createChallenge(any())).thenReturn(stub(true));

            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Challenge created"))
                    .andExpect(jsonPath("$.data.targetCount").value(7));
        }

        @Test @DisplayName("400 — missing title")
        void validation_missingTitle_returns400() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"icon":"🏆","description":"d","difficulty":"MEDIUM","targetCount":7,"rewardPoints":100}
                                    """))
                    .andExpect(status().isBadRequest());
            verify(adminChallengeService, never()).createChallenge(any());
        }

        @Test @DisplayName("400 — missing difficulty")
        void validation_missingDifficulty_returns400() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"icon":"🏆","title":"t","description":"d","targetCount":7,"rewardPoints":100}
                                    """))
                    .andExpect(status().isBadRequest());
            verify(adminChallengeService, never()).createChallenge(any());
        }

        @Test @DisplayName("400 — targetCount less than 1")
        void validation_targetCountBelowMin_returns400() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"icon":"🏆","title":"t","description":"d","difficulty":"EASY","targetCount":0,"rewardPoints":100}
                                    """))
                    .andExpect(status().isBadRequest());
            verify(adminChallengeService, never()).createChallenge(any());
        }

        @Test @DisplayName("400 — negative rewardPoints")
        void validation_negativeRewardPoints_returns400() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("""
                                    {"icon":"🏆","title":"t","description":"d","difficulty":"EASY","targetCount":7,"rewardPoints":-1}
                                    """))
                    .andExpect(status().isBadRequest());
            verify(adminChallengeService, never()).createChallenge(any());
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot create") void regularUser() throws Exception {
            mockMvc.perform(post(BASE).with(asUser()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID_JSON))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PATCH /{id} ───────────────────────────────────────────────────────────

    @Nested @DisplayName("PATCH /{id} — updateChallenge")
    class UpdateChallenge {

        @Test @DisplayName("200 — updates and returns response")
        void success_returnsUpdatedChallenge() throws Exception {
            when(adminChallengeService.updateChallenge(eq(ID), any())).thenReturn(stub(true));

            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"title\":\"Updated\",\"difficulty\":\"HARD\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Challenge updated"));
        }

        @Test @DisplayName("200 — empty body accepted (PATCH semantics)")
        void success_emptyBodyAccepted() throws Exception {
            when(adminChallengeService.updateChallenge(eq(ID), any())).thenReturn(stub(true));

            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isOk());
        }

        @Test @DisplayName("404 — not found") void notFound() throws Exception {
            when(adminChallengeService.updateChallenge(eq(ID), any())).thenThrow(new ChallengeNotFoundException());

            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}"))
                    .andExpect(status().isNotFound());
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot update") void regularUser() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID).with(asUser()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── PATCH /{id}/toggle ────────────────────────────────────────────────────

    @Nested @DisplayName("PATCH /{id}/toggle — toggleActive")
    class ToggleActive {

        @Test @DisplayName("200 — returns toggled challenge")
        void success_returnsToggledChallenge() throws Exception {
            when(adminChallengeService.toggleActive(ID)).thenReturn(stub(false));

            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Challenge status toggled"))
                    .andExpect(jsonPath("$.data.isActive").value(false));
        }

        @Test @DisplayName("404 — not found") void notFound() throws Exception {
            when(adminChallengeService.toggleActive(ID)).thenThrow(new ChallengeNotFoundException());
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asAdmin()).with(csrf())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(csrf())).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot toggle") void regularUser() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asUser()).with(csrf())).andExpect(status().isForbidden());
        }
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────

    @Nested @DisplayName("DELETE /{id} — deleteChallenge")
    class DeleteChallenge {

        @Test @DisplayName("200 — deletes challenge")
        void success_deletesChallenge() throws Exception {
            doNothing().when(adminChallengeService).deleteChallenge(ID);

            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Challenge deleted"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }

        @Test @DisplayName("404 — not found") void notFound() throws Exception {
            doThrow(new ChallengeNotFoundException()).when(adminChallengeService).deleteChallenge(ID);
            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401 — unauthenticated") void unauthenticated() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(csrf())).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_USER cannot delete") void regularUser() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(asUser()).with(csrf())).andExpect(status().isForbidden());
        }
    }
}