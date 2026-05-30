package com.dway.dwaybackend.controller.admin;

import com.dway.dwaybackend.common.exception.achievement.AchievementNotFoundException;
import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.achievement.AchievementResponse;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.admin.AdminAchievementService;
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

@WebMvcTest(AdminAchievementController.class)
@Import(SecurityConfig.class)
@DisplayName("AdminAchievementController")
class AdminAchievementControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AdminAchievementService adminAchievementService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID   ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String BASE = "/api/v1/admin/achievements";

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

    private AchievementResponse stub(boolean isActive) {
        return AchievementResponse.builder()
                .id(ID).title("Task Starter").description("Complete 10 tasks")
                .icon("🏅").type(AchievementType.TASK_COUNT).threshold(10)
                .isActive(isActive).createdAt(LocalDateTime.now())
                .build();
    }

    private Page<AchievementResponse> page(List<AchievementResponse> items) {
        return new PageImpl<>(items, PageRequest.of(0, 20), items.size());
    }

    // ── GET / ─────────────────────────────────────────────────────────────────

    @Nested @DisplayName("GET /")
    class GetAll {

        @Test @DisplayName("200 — returns page") void success() throws Exception {
            when(adminAchievementService.getAllAchievements(any(), any()))
                    .thenReturn(page(List.of(stub(false))));
            mockMvc.perform(get(BASE).with(asAdmin()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].type").value("TASK_COUNT"))
                    .andExpect(jsonPath("$.data.content[0].isActive").value(false));
        }

        @Test @DisplayName("200 — isActive=true filter") void filterActive() throws Exception {
            when(adminAchievementService.getAllAchievements(eq(true), any())).thenReturn(page(List.of(stub(true))));
            mockMvc.perform(get(BASE).param("isActive", "true").with(asAdmin())).andExpect(status().isOk());
            verify(adminAchievementService).getAllAchievements(eq(true), any());
        }

        @Test @DisplayName("200 — empty page") void empty() throws Exception {
            when(adminAchievementService.getAllAchievements(any(), any())).thenReturn(page(Collections.emptyList()));
            mockMvc.perform(get(BASE).with(asAdmin())).andExpect(jsonPath("$.data.totalElements").value(0));
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403") void forbidden() throws Exception {
            mockMvc.perform(get(BASE).with(asUser())).andExpect(status().isForbidden());
        }
    }

    // ── GET /{id} ─────────────────────────────────────────────────────────────

    @Nested @DisplayName("GET /{id}")
    class GetById {

        @Test @DisplayName("200") void success() throws Exception {
            when(adminAchievementService.getAchievementById(ID)).thenReturn(stub(false));
            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin())).andExpect(status().isOk()).andExpect(jsonPath("$.data.type").value("TASK_COUNT"));
        }

        @Test @DisplayName("404") void notFound() throws Exception {
            when(adminAchievementService.getAchievementById(ID)).thenThrow(new AchievementNotFoundException());
            mockMvc.perform(get(BASE + "/" + ID).with(asAdmin())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(get(BASE + "/" + ID)).andExpect(status().isUnauthorized());
        }
    }

    // ── POST / ────────────────────────────────────────────────────────────────

    @Nested @DisplayName("POST /")
    class Create {

        private static final String VALID =
                "{\"title\":\"Task Starter\",\"description\":\"Complete 10 tasks\",\"icon\":\"🏅\",\"type\":\"TASK_COUNT\",\"threshold\":10}";

        @Test @DisplayName("200 — creates inactive achievement") void success() throws Exception {
            when(adminAchievementService.createAchievement(any())).thenReturn(stub(false));
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(VALID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("TASK_COUNT"))
                    .andExpect(jsonPath("$.data.isActive").value(false));
        }

        @Test @DisplayName("200 — CHALLENGE_COUNT type") void challengeType() throws Exception {
            String json = "{\"title\":\"Challenger\",\"description\":\"Complete 3 challenges\",\"icon\":\"🏆\",\"type\":\"CHALLENGE_COUNT\",\"threshold\":3}";
            when(adminAchievementService.createAchievement(any())).thenReturn(
                    AchievementResponse.builder().id(ID).title("Challenger")
                            .type(AchievementType.CHALLENGE_COUNT).threshold(3).isActive(false).build());
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.type").value("CHALLENGE_COUNT"));
        }

        @Test @DisplayName("400 — missing type") void missingType() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(
                                    "{\"title\":\"T\",\"description\":\"d\",\"icon\":\"🏅\",\"threshold\":10}"))
                    .andExpect(status().isBadRequest());
            verify(adminAchievementService, never()).createAchievement(any());
        }

        @Test @DisplayName("400 — missing title") void missingTitle() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(
                                    "{\"description\":\"d\",\"icon\":\"🏅\",\"type\":\"TASK_COUNT\",\"threshold\":10}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("400 — threshold below 1") void badThreshold() throws Exception {
            mockMvc.perform(post(BASE).with(asAdmin()).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(
                                    "{\"title\":\"T\",\"description\":\"d\",\"icon\":\"🏅\",\"type\":\"TASK_COUNT\",\"threshold\":0}"))
                    .andExpect(status().isBadRequest());
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(post(BASE).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403") void forbidden() throws Exception {
            mockMvc.perform(post(BASE).with(asUser()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
        }
    }

    // ── PATCH /{id} ───────────────────────────────────────────────────────────

    @Nested @DisplayName("PATCH /{id}")
    class Update {

        @Test @DisplayName("200") void success() throws Exception {
            when(adminAchievementService.updateAchievement(eq(ID), any())).thenReturn(stub(false));
            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Updated\"}")).andExpect(status().isOk());
        }

        @Test @DisplayName("200 — empty body") void emptyBody() throws Exception {
            when(adminAchievementService.updateAchievement(eq(ID), any())).thenReturn(stub(false));
            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isOk());
        }

        @Test @DisplayName("400 — threshold 0") void badThreshold() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"threshold\":0}")).andExpect(status().isBadRequest());
        }

        @Test @DisplayName("404") void notFound() throws Exception {
            when(adminAchievementService.updateAchievement(eq(ID), any())).thenThrow(new AchievementNotFoundException());
            mockMvc.perform(patch(BASE + "/" + ID).with(asAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"x\"}")).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        }
    }

    // ── PATCH /{id}/toggle ────────────────────────────────────────────────────

    @Nested @DisplayName("PATCH /{id}/toggle")
    class Toggle {

        @Test @DisplayName("200 — activates") void activates() throws Exception {
            when(adminAchievementService.toggleActive(ID)).thenReturn(stub(true));
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asAdmin()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.data.isActive").value(true));
        }

        @Test @DisplayName("200 — deactivates") void deactivates() throws Exception {
            when(adminAchievementService.toggleActive(ID)).thenReturn(stub(false));
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asAdmin()).with(csrf())).andExpect(jsonPath("$.data.isActive").value(false));
        }

        @Test @DisplayName("404") void notFound() throws Exception {
            when(adminAchievementService.toggleActive(ID)).thenThrow(new AchievementNotFoundException());
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asAdmin()).with(csrf())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(csrf())).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403") void forbidden() throws Exception {
            mockMvc.perform(patch(BASE + "/" + ID + "/toggle").with(asUser()).with(csrf())).andExpect(status().isForbidden());
        }
    }

    // ── DELETE /{id} ──────────────────────────────────────────────────────────

    @Nested @DisplayName("DELETE /{id}")
    class Delete {

        @Test @DisplayName("200") void success() throws Exception {
            doNothing().when(adminAchievementService).deleteAchievement(ID);
            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.message").value("Achievement deleted"));
        }

        @Test @DisplayName("404") void notFound() throws Exception {
            doThrow(new AchievementNotFoundException()).when(adminAchievementService).deleteAchievement(ID);
            mockMvc.perform(delete(BASE + "/" + ID).with(asAdmin()).with(csrf())).andExpect(status().isNotFound());
        }

        @Test @DisplayName("401") void unauth() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(csrf())).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403") void forbidden() throws Exception {
            mockMvc.perform(delete(BASE + "/" + ID).with(asUser()).with(csrf())).andExpect(status().isForbidden());
        }
    }
}