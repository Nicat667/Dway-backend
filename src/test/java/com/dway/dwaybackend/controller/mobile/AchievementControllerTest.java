package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.config.SecurityConfig;
import com.dway.dwaybackend.dto.response.achievement.AchievementStatusResponse;
import com.dway.dwaybackend.entity.enums.AchievementType;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.security.JwtUtil;
import com.dway.dwaybackend.service.mobile.AchievementService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AchievementController.class)
@Import(SecurityConfig.class)
@DisplayName("AchievementController (mobile)")
class AchievementControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AchievementService achievementService;
    @MockitoBean RateLimitService rateLimitService;
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID   USER_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID   ACH_ID  = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String BASE    = "/api/v1/mobile/achievements";

    @BeforeEach
    void setUp() { when(rateLimitService.tryConsume(any(), any())).thenReturn(true); }

    private RequestPostProcessor asUser(UUID userId) {
        return authentication(new UsernamePasswordAuthenticationToken(userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(UUID.randomUUID(), null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private AchievementStatusResponse earnedStub() {
        return AchievementStatusResponse.builder()
                .id(ACH_ID).title("Task Starter").description("Complete 10 tasks")
                .icon("🏅").type(AchievementType.TASK_COUNT).threshold(10)
                .earned(true).unlockedAt(LocalDateTime.now().minusDays(1))
                .build();
    }

    private AchievementStatusResponse unearnedStub() {
        return AchievementStatusResponse.builder()
                .id(UUID.randomUUID()).title("Task Master").description("Complete 50 tasks")
                .icon("🏆").type(AchievementType.TASK_COUNT).threshold(50)
                .earned(false)
                .build();
    }

    @Nested @DisplayName("GET / — getAllAchievements")
    class GetAllAchievements {

        @Test @DisplayName("200 — returns achievements with earned flags")
        void success() throws Exception {
            when(achievementService.getAllAchievements(USER_ID)).thenReturn(List.of(earnedStub(), unearnedStub()));
            mockMvc.perform(get(BASE).with(asUser(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].earned").value(true))
                    .andExpect(jsonPath("$.data[0].unlockedAt").isNotEmpty())
                    .andExpect(jsonPath("$.data[1].earned").value(false));
        }

        @Test @DisplayName("200 — empty list")
        void empty() throws Exception {
            when(achievementService.getAllAchievements(USER_ID)).thenReturn(Collections.emptyList());
            mockMvc.perform(get(BASE).with(asUser(USER_ID))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test @DisplayName("passes correct userId to service")
        void passesUserId() throws Exception {
            when(achievementService.getAllAchievements(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get(BASE).with(asUser(USER_ID)));
            verify(achievementService).getAllAchievements(USER_ID);
        }

        @Test @DisplayName("401 — unauthenticated")
        void unauth() throws Exception {
            mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_ADMIN cannot access mobile route")
        void adminForbidden() throws Exception {
            mockMvc.perform(get(BASE).with(asAdmin())).andExpect(status().isForbidden());
        }
    }

    @Nested @DisplayName("GET /me — getMyAchievements")
    class GetMyAchievements {

        @Test @DisplayName("200 — returns earned achievements")
        void success() throws Exception {
            when(achievementService.getMyAchievements(USER_ID)).thenReturn(List.of(earnedStub()));
            mockMvc.perform(get(BASE + "/me").with(asUser(USER_ID)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].earned").value(true))
                    .andExpect(jsonPath("$.data[0].type").value("TASK_COUNT"));
        }

        @Test @DisplayName("200 — empty list")
        void empty() throws Exception {
            when(achievementService.getMyAchievements(USER_ID)).thenReturn(Collections.emptyList());
            mockMvc.perform(get(BASE + "/me").with(asUser(USER_ID))).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test @DisplayName("passes correct userId to service")
        void passesUserId() throws Exception {
            when(achievementService.getMyAchievements(USER_ID)).thenReturn(List.of());
            mockMvc.perform(get(BASE + "/me").with(asUser(USER_ID)));
            verify(achievementService).getMyAchievements(USER_ID);
        }

        @Test @DisplayName("401 — unauthenticated")
        void unauth() throws Exception {
            mockMvc.perform(get(BASE + "/me")).andExpect(status().isUnauthorized());
        }

        @Test @DisplayName("403 — ROLE_ADMIN cannot access mobile route")
        void adminForbidden() throws Exception {
            mockMvc.perform(get(BASE + "/me").with(asAdmin())).andExpect(status().isForbidden());
        }
    }
}