package com.dway.dwaybackend.controller.mobile;

import com.dway.dwaybackend.common.exception.motivation.MotivationNotFoundException;
import com.dway.dwaybackend.dto.response.motivation.MotivationResponse;
import com.dway.dwaybackend.infrastructure.ratelimit.RateLimitService;
import com.dway.dwaybackend.service.mobile.MotivationService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MotivationController.class)
@DisplayName("MotivationController")
class MotivationControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean MotivationService motivationService;
    @MockitoBean RateLimitService rateLimitService;

    private static final UUID ID   = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final String BASE = "/api/v1/mobile/motivations";

    @BeforeEach
    void setUp() {
        when(rateLimitService.tryConsume(any(), any())).thenReturn(true);
    }

    private RequestPostProcessor asUser() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }

    private RequestPostProcessor asAdmin() {
        return authentication(new UsernamePasswordAuthenticationToken(
                UUID.randomUUID(), null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private MotivationResponse stub(String quote, String author) {
        return MotivationResponse.builder()
                .id(ID)
                .quote(quote)
                .author(author)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("GET /today")
    class GetTodayMotivation {

        @Test
        @DisplayName("200 — returns queue-selected motivation with full fields")
        void success_returnsMotivation() throws Exception {
            when(motivationService.getTodayMotivation())
                    .thenReturn(stub("Press forward.", "Lincoln"));

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(ID.toString()))
                    .andExpect(jsonPath("$.data.quote").value("Press forward."))
                    .andExpect(jsonPath("$.data.author").value("Lincoln"));
        }

        @Test
        @DisplayName("200 — success field is always true on 200")
        void success_successFieldIsTrue() throws Exception {
            when(motivationService.getTodayMotivation()).thenReturn(stub("Quote", "Author"));

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("200 — data is not null on success")
        void success_dataNotNull() throws Exception {
            when(motivationService.getTodayMotivation()).thenReturn(stub("Quote", "Author"));

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpect(jsonPath("$.data.quote").exists());
        }

        @Test
        @DisplayName("200 — author field absent from JSON when motivation has no author (@JsonInclude NON_NULL)")
        void success_authorAbsentWhenNull() throws Exception {
            when(motivationService.getTodayMotivation()).thenReturn(stub("Quote", null));

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.quote").value("Quote"))
                    .andExpect(jsonPath("$.data.author").doesNotExist());
        }

        @Test
        @DisplayName("200 — works when authenticated as ADMIN (mobile endpoint, any authenticated role is valid)")
        void success_adminCanAlsoAccess() throws Exception {
            when(motivationService.getTodayMotivation()).thenReturn(stub("Quote", "Author"));

            mockMvc.perform(get(BASE + "/today").with(asAdmin()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 — service is called exactly once per request")
        void success_serviceCalledOnce() throws Exception {
            when(motivationService.getTodayMotivation()).thenReturn(stub("Quote", "Author"));

            mockMvc.perform(get(BASE + "/today").with(asUser()));

            verify(motivationService, times(1)).getTodayMotivation();
        }

        @Test
        @DisplayName("401 — no auth header")
        void unauthenticated_noHeader_returns401() throws Exception {
            mockMvc.perform(get(BASE + "/today"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 — service is never called when unauthenticated")
        void unauthenticated_serviceNeverCalled() throws Exception {
            mockMvc.perform(get(BASE + "/today"))
                    .andExpect(status().isUnauthorized());

            verify(motivationService, never()).getTodayMotivation();
        }

        @Test
        @DisplayName("404 — when no motivations have been uploaded yet")
        void emptyPool_returns404() throws Exception {
            when(motivationService.getTodayMotivation())
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("404 — success field is false")
        void emptyPool_successIsFalse() throws Exception {
            when(motivationService.getTodayMotivation())
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("404 — message is present in response body")
        void emptyPool_messagePresent() throws Exception {
            when(motivationService.getTodayMotivation())
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }

        @Test
        @DisplayName("404 — data field is absent (not null, not empty — just absent)")
        void emptyPool_dataAbsent() throws Exception {
            when(motivationService.getTodayMotivation())
                    .thenThrow(new MotivationNotFoundException());

            mockMvc.perform(get(BASE + "/today").with(asUser()))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }
}